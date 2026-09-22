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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.model.Channel
import org.meshtastic.core.model.MeshLog
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.feature.settings.debugging.sanitizeForExport
import org.meshtastic.feature.settings.util.decodePayloadFromPacket
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
data class GroupedSniffedPacket(val packet: SniffedPacket, val relayIds: List<String>)

/**
 * Groups packets sharing the same [SniffedPacket.packetId] -- i.e. the same over-the-air transmission re-heard through
 * several relaying nodes -- into a single row per packet, newest first. Entries with no packetId are never merged and
 * each get their own single-relay row. Pure and stateless, mirroring
 * [org.meshtastic.feature.settings.sniffer.mqtt.groupedByGateway]: called from the display layer only when the user has
 * grouping enabled -- the underlying live packet list itself always stays ungrouped.
 */
fun List<SniffedPacket>.groupedByRelay(): List<GroupedSniffedPacket> {
    val (groupable, ungroupable) = partition { it.packetId != null }
    val grouped =
        groupable
            .groupBy { it.packetId }
            .values
            .map { packets ->
                val newest = packets.maxBy { it.receivedAtMillis }
                val relayIds = packets.mapNotNull { it.relayId }.distinct()
                GroupedSniffedPacket(newest, relayIds)
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
) : ViewModel() {

    val sniffedPackets: StateFlow<List<SniffedPacket>> =
        meshLogRepository
            .getAllLogs()
            .map { logs -> logs.mapNotNull(::toSniffedPacket).sortedByDescending { it.receivedAtMillis } }
            .stateInWhileSubscribed(initialValue = emptyList())

    private val channelSet = radioConfigRepository.channelSetFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Every channel this app currently holds an (already-expanded, see [Channel.psk]) key for. */
    private fun knownChannelPsks(): List<ByteArray> {
        val set = channelSet.value ?: return emptyList()
        val loraConfig = set.lora_config ?: LoRaConfig.Builder().build()
        return set.settings.map { Channel(it, loraConfig).psk.toByteArray() }
    }

    private fun toSniffedPacket(log: MeshLog): SniffedPacket? {
        val packet = log.meshPacket ?: return null
        val myNodeNum = nodeRepository.myNodeInfo.value?.myNodeNum ?: return null

        val nodeMap = nodeRepository.nodeDBbyNum.value
        val decodedText = decodePayloadFromPacket(packet, nodeRepository, knownChannelPsks())
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
            portNum = packet.decoded?.portnum?.value,
            isEncrypted = decodedText == null,
            payloadHex = rawBytes?.hex().orEmpty(),
            decodedPayload = decodedText,
            receivedAtMillis = log.received_date,
            packetId = packet.id,
            relayId = resolveRelayId(packet.relay_node, nodeMap, myNodeNum),
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
