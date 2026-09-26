/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.feature.settings.sniffer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.datastore.SnifferBufferOverflowPolicy
import org.meshtastic.core.datastore.SnifferLogPrefs
import org.meshtastic.core.datastore.SnifferSource
import org.meshtastic.core.model.Channel
import org.meshtastic.core.model.MeshLog
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.util.ON_DEMAND_PORT_NUM
import org.meshtastic.core.model.util.effectivePortNum
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.feature.settings.debugging.sanitizeForExport
import org.meshtastic.feature.settings.util.PacketSummary
import org.meshtastic.feature.settings.util.decodePayloadFromPacket
import org.meshtastic.feature.settings.util.summarizePacketPayload
import org.meshtastic.proto.Config.LoRaConfig

/** One packet the locally connected node overheard but which was not addressed to it. */
data class SniffedPacket(
    val fromId: String,
    val toId: String,
    val fromShortName: String?,
    val toShortName: String?,
    val channel: Int,
    val hopStart: Int,
    val hopLimit: Int,
    val rssi: Int?,
    val snr: Float,
    val portNum: Int?,
    val isEncrypted: Boolean,
    val payloadHex: String,
    val decodedPayload: String?,
    val receivedAtMillis: Long,
    /**
     * The originating MeshPacket's own id -- lets a saved-and-reloaded log retry decryption the same way the MQTT
     * Sniffer's [org.meshtastic.feature.settings.sniffer.mqtt.MqttSniffedPacket.packetId] does. Also lets several
     * relayed copies of the same over-the-air transmission be spotted and grouped -- see [groupedByRelay].
     */
    val packetId: Int? = null,
    /**
     * The single relay node (formatted "!hex" or "!hex (SHORTNAME)") this particular copy of the packet was last heard
     * from, resolved from the wire's truncated [org.meshtastic.proto.MeshPacket.relay_node] byte via
     * [org.meshtastic.core.model.Node.getRelayNode]. Null when unset (a direct/0-hop packet) or unresolvable against
     * the current node database. Grouping several same-[packetId] entries into one row is done downstream (see
     * [groupedByRelay]), not here -- mirrors MQTT's own gatewayId field.
     */
    val relayId: String? = null,
    /** Structured summary of the decoded content, for the always-visible card content line -- see [PacketSummary]. */
    val summary: PacketSummary? = null,
) {
    /** Multi-line, redacted representation for the per-packet copy action — reuses the Debug Panel's redaction. */
    val copyText: String
        get() =
            sanitizeForExport(
                buildString {
                    appendLine("$fromId → $toId")
                    appendLine("ch $channel • hops ${hopStart - hopLimit}/$hopStart")
                    appendLine(listOfNotNull(rssi?.let { "RSSI $it" }, "SNR $snr").joinToString(" • "))
                    relayId?.let { appendLine("via $it") }
                    appendLine(if (isEncrypted) "encrypted" else portNum?.let { "port $it" } ?: "unknown port")
                    append(decodedPayload ?: payloadHex)
                },
            )
}

/**
 * One row in the combined Sniffer panel's grouped Radio view (see [groupedByRelay]): one over-the-air packet
 * ([SniffedPacket.packetId]), possibly re-heard through several relaying nodes. [relayIds] lists every relay this
 * packet was seen from, most-recently-seen first, deduplicated. [packet] is the most recently received copy -- its own
 * [SniffedPacket.relayId] is superseded by [relayIds] for display. Mirrors
 * [org.meshtastic.feature.settings.sniffer.mqtt.GroupedMqttSniffedPacket].
 */
data class GroupedSniffedPacket(
    val packet: SniffedPacket,
    val relayIds: List<String>,
    /** One entry per underlying reception, oldest first -- the Sniffer Log card's expandable "receipts" list. */
    val receipts: List<SniffedPacketReceipt> =
        listOf(SniffedPacketReceipt(packet.relayId, packet.snr, packet.rssi, packet.receivedAtMillis)),
    /**
     * Every underlying reception this row groups, oldest first -- not just the chosen [packet] -- so the copy action
     * (see [copyText]) can hand over all of them at once. Several relayed copies of the same over-the-air transmission
     * can decode differently from each other (see [groupedByRelay]'s kdoc); showing only the one this card picked hides
     * exactly the discrepancy someone copying a packet out to diagnose a decode problem would need to see.
     */
    val allPackets: List<SniffedPacket> = listOf(packet),
)

/**
 * Combined, redacted copy-action text for a grouped row: every underlying reception's own [SniffedPacket.copyText] from
 * [GroupedSniffedPacket.allPackets], oldest first -- not just the single copy [GroupedSniffedPacket.packet] shows on
 * the card.
 */
val GroupedSniffedPacket.copyText: String
    get() =
        if (allPackets.size <= 1) {
            packet.copyText
        } else {
            allPackets
                .mapIndexed { index, p -> "--- Copy ${index + 1}/${allPackets.size} ---\n${p.copyText}" }
                .joinToString("\n\n")
        }

/** One physical reception of a grouped packet -- who it came through, its signal, and when. */
data class SniffedPacketReceipt(val relayId: String?, val snr: Float, val rssi: Int?, val receivedAtMillis: Long)

/**
 * Groups packets sharing the same [SniffedPacket.packetId] *and* the same [SniffedPacket.fromId] -- i.e. the same
 * over-the-air transmission re-heard through several relaying nodes -- into a single row per packet, newest first.
 * Entries with no packetId are never merged and each get their own single-relay row. Pure and stateless, mirroring
 * [org.meshtastic.feature.settings.sniffer.mqtt.groupedByGateway] -- see its kdoc for why packetId alone (a per-node
 * rolling id, not a global one) isn't a safe grouping key by itself: called from the display layer only when the user
 * has grouping enabled -- the underlying live packet list itself always stays ungrouped.
 */
fun List<SniffedPacket>.groupedByRelay(): List<GroupedSniffedPacket> {
    val (groupable, ungroupable) = partition { it.packetId != null }
    val grouped =
        groupable
            .groupBy { it.fromId to it.packetId }
            .values
            .map { packets ->
                // Prefer the newest copy that actually decoded: several relayed copies of the same over-the-air
                // transmission can arrive with different decode outcomes (e.g. one relay's copy fails a signature
                // or arrives before this app's channel keys were ready), and picking by timestamp alone let a
                // later still-encrypted copy blank out content this same row had already shown -- and among
                // decoded copies, prefer the longest payload over the most recently received one, for the same
                // reason as the mirrored MQTT version (groupedByGateway): TRACEROUTE_APP's route_back/snr_back
                // grow with every hop it's relayed through, so the relay with the longest raw payload has
                // necessarily seen the most complete picture so far -- when this node happens to hear a relay's
                // still-partial copy after an already-more-complete one, "most recently heard" is no guarantee of
                // "traveled the furthest", and could otherwise show fewer hops and "?" SNR than the same response's
                // own record elsewhere in the app.
                val newest =
                    packets
                        .filterNot { it.isEncrypted }
                        .maxWithOrNull(compareBy({ it.payloadHex.length }, { it.receivedAtMillis }))
                        ?: packets.maxBy { it.receivedAtMillis }
                val relayIds = packets.mapNotNull { it.relayId }.distinct()
                val receipts =
                    packets
                        .sortedBy { it.receivedAtMillis }
                        .map { SniffedPacketReceipt(it.relayId, it.snr, it.rssi, it.receivedAtMillis) }
                GroupedSniffedPacket(newest, relayIds, receipts, packets.sortedBy { it.receivedAtMillis })
            }
    val singles = ungroupable.map { GroupedSniffedPacket(it, listOfNotNull(it.relayId)) }
    return (grouped + singles).sortedByDescending { it.packet.receivedAtMillis }
}

/**
 * Sniffer Log screen ViewModel, reached from Settings → Advanced. Sniffer mode has no distinguishing marker on the wire
 * — firmware's `sendPacketToPhoneRaw()` forwards a sniffed packet through the exact same `FromRadio.packet` channel as
 * ordinary traffic. So this decodes live from the same [MeshLog] stream as everything else and applies a heuristic: any
 * packet not addressed to the locally connected node was not meant for us, so it must have been sniffed. By request,
 * this screen shows the complete unfiltered packet log instead — including our own outgoing requests and their
 * responses — so a full request/response pair (e.g. traceroute) can be inspected together here. Always reflects the
 * locally connected node — there is no per-node variant of this screen.
 */
@KoinViewModel
class SnifferLogViewModel(
    private val meshLogRepository: MeshLogRepository,
    private val nodeRepository: NodeRepository,
    radioConfigRepository: RadioConfigRepository,
    private val prefs: SnifferLogPrefs,
) : ViewModel() {

    /**
     * Bound to [SnifferLogPrefs.clearedAtMillis] rather than an unbounded/most-recent-N query over the whole shared
     * MeshLog table: that table holds every packet the app has ever logged, sniffed or not, so querying it without a
     * lower bound would surface a sniffing run's own history mixed in with days of unrelated ordinary traffic -- and do
     * the decode/heuristic work for all of it on every collection. 0L (nothing captured since the last clear, or the
     * sniffer has never been activated yet) means "show nothing" rather than "show everything ever logged" -- see
     * [SnifferPanelViewModel]'s init, which guarantees this is never left at 0L once a real source has been selected.
     */
    val sniffedPackets: StateFlow<List<SniffedPacket>> =
        combine(prefs.activeSource, prefs.clearedAtMillis) { source, clearedAtMillis -> source to clearedAtMillis }
            .flatMapLatest { (source, clearedAtMillis) ->
                // Off truly means off: no query, no decoding, nothing buffered -- not just hidden at the display
                // layer. A stale RADIO selection left over from before a disconnect/reconnect is what used to make
                // this look like it kept "filling up" while the sniffer toggle showed off; see SnifferSettingsScreen's
                // state.snifferEnabled reconciliation, which is what keeps activeSource itself honest.
                if (source != SnifferSource.RADIO || clearedAtMillis <= 0L) {
                    flowOf(emptyList())
                } else {
                    meshLogRepository.getAllLogsSince(clearedAtMillis)
                }
            }
            .combine(prefs.bufferOverflowPolicy) { logs, policy -> logs to policy }
            .combine(prefs.hideOnDemandChannel0) { (logs, policy), hideOnDemand -> Triple(logs, policy, hideOnDemand) }
            .map { (logs, policy, hideOnDemand) ->
                val packets =
                    logs.mapNotNull(::toSniffedPacket).filterNot { packet ->
                        // OnDemand (port 354) on the primary channel is the phone app's own diagnostic/control
                        // chatter with the connected node, not mesh traffic -- see hideOnDemandChannel0's doc.
                        hideOnDemand && packet.channel == 0 && packet.portNum == ON_DEMAND_PORT_NUM
                    }
                capBuffer(packets, policy)
            }
            .stateInWhileSubscribed(initialValue = emptyList())

    private val channelSet = radioConfigRepository.channelSetFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Applies [SnifferLogPrefs.MAX_BUFFERED_PACKETS] according to [policy]: [SnifferBufferOverflowPolicy.STOP] keeps
     * whichever [SnifferLogPrefs.MAX_BUFFERED_PACKETS] packets were captured first (oldest since the last clear) and
     * ignores anything beyond that until cleared; [SnifferBufferOverflowPolicy.OVERWRITE] keeps the newest
     * [SnifferLogPrefs.MAX_BUFFERED_PACKETS], dropping older ones as new packets arrive. Always returned newest-first.
     */
    private fun capBuffer(packets: List<SniffedPacket>, policy: SnifferBufferOverflowPolicy): List<SniffedPacket> {
        val capped =
            when (policy) {
                SnifferBufferOverflowPolicy.STOP ->
                    packets.sortedBy { it.receivedAtMillis }.take(SnifferLogPrefs.MAX_BUFFERED_PACKETS)

                SnifferBufferOverflowPolicy.OVERWRITE ->
                    packets.sortedByDescending { it.receivedAtMillis }.take(SnifferLogPrefs.MAX_BUFFERED_PACKETS)
            }
        return capped.sortedByDescending { it.receivedAtMillis }
    }

    /** Every channel this app currently holds an (already-expanded, see [Channel.psk]) key for. */
    private fun knownChannels(): List<Channel> {
        val set = channelSet.value ?: return emptyList()
        val loraConfig = set.lora_config ?: LoRaConfig.Builder().build()
        return set.settings.map { Channel(it, loraConfig) }
    }

    private fun toSniffedPacket(log: MeshLog): SniffedPacket? {
        val packet = log.meshPacket ?: return null
        val myNodeNum = nodeRepository.myNodeInfo.value?.myNodeNum ?: return null

        val nodeMap = nodeRepository.nodeDBbyNum.value
        val decodedText = decodePayloadFromPacket(packet, nodeRepository, knownChannels())
        val rawBytes = packet.decoded?.payload ?: packet.encrypted
        return SniffedPacket(
            fromId = NodeAddress.numToDefaultId(packet.from),
            toId = NodeAddress.numToDefaultId(packet.to),
            fromShortName = nodeMap[packet.from]?.user?.short_name?.takeIf { it.isNotBlank() },
            toShortName = nodeMap[packet.to]?.user?.short_name?.takeIf { it.isNotBlank() },
            channel = packet.channel,
            hopStart = packet.hop_start,
            hopLimit = packet.hop_limit,
            rssi = packet.rx_rssi,
            snr = packet.rx_snr,
            portNum = packet.decoded?.effectivePortNum(),
            isEncrypted = decodedText == null,
            payloadHex = rawBytes?.hex().orEmpty(),
            decodedPayload = decodedText,
            receivedAtMillis = log.received_date,
            packetId = packet.id,
            relayId = resolveRelayId(packet.relay_node, nodeMap, myNodeNum),
            summary = summarizePacketPayload(packet, knownChannels()),
        )
    }

    /**
     * Resolves [relayNode] (the wire's truncated last-byte relay id) against [nodeMap] via [Node.getRelayNode]. Returns
     * null for an unset (0) or unresolvable relay rather than showing a misleading partial id.
     */
    private fun resolveRelayId(relayNode: Int, nodeMap: Map<Int, Node>, ourNodeNum: Int?): String? {
        if (relayNode == 0) return null
        return Node.getRelayNode(relayNode, nodeMap.values.toList(), ourNodeNum)?.let { relay ->
            val shortName = relay.user?.short_name?.takeIf { it.isNotBlank() }
            val id = NodeAddress.numToDefaultId(relay.num)
            shortName?.let { "$id ($it)" } ?: id
        }
    }
}
