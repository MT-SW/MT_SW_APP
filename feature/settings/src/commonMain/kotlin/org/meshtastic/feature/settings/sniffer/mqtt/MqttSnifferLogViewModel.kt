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
package org.meshtastic.feature.settings.sniffer.mqtt

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import okio.ByteString
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ServiceScope
import org.meshtastic.core.datastore.SnifferBufferOverflowPolicy
import org.meshtastic.core.datastore.SnifferLogPrefs
import org.meshtastic.core.model.Channel
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.network.repository.MQTTRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.feature.settings.debugging.sanitizeForExport
import org.meshtastic.feature.settings.util.decodePayloadFromPacket
import org.meshtastic.mqtt.ConnectionState
import org.meshtastic.proto.Config.LoRaConfig
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.MqttClientProxyMessage
import org.meshtastic.proto.ServiceEnvelope

/**
 * One packet observed on the MQTT broker's mesh topics -- the MQTT counterpart to `SniffedPacket` (Radio Sniffer).
 * `isEncrypted` reflects whether this app could decode it: either the publishing node had encryption disabled for this
 * channel, or the app already holds that channel's key (see [MqttSnifferManager]).
 */
data class MqttSniffedPacket(
    val topic: String,
    val channelId: String?,
    val fromId: String,
    val toId: String,
    val fromShortName: String?,
    val toShortName: String?,
    val portNum: Int?,
    val isEncrypted: Boolean,
    val payloadHex: String,
    val decodedPayload: String?,
    val isJson: Boolean,
    val receivedAtMillis: Long,
    /**
     * The originating MeshPacket's own id -- lets several gateway copies of the same over-the-air transmission be
     * spotted and grouped together. Null for JSON entries, which carry no MeshPacket to identify.
     */
    val packetId: Int? = null,
    /**
     * The single gateway (formatted "!hex" or "!hex (SHORTNAME)") this particular broker message came through. Grouping
     * several same-[packetId] entries into one displayed row is done downstream, not here.
     */
    val gatewayId: String? = null,
) {
    /** Multi-line, redacted representation for the per-packet copy action -- reuses the Debug Panel's redaction. */
    val copyText: String
        get() =
            sanitizeForExport(
                buildString {
                    appendLine(topic)
                    appendLine("$fromId -> $toId" + (channelId?.let { " (ch $it)" } ?: ""))
                    appendLine(if (isEncrypted) "encrypted" else portNum?.let { "port $it" } ?: "unknown port")
                    gatewayId?.let { appendLine("via $it") }
                    append(decodedPayload ?: payloadHex)
                },
            )
}

/**
 * One row in the combined Sniffer panel's grouped MQTT view (see [groupedByGateway]): one over-the-air packet
 * ([MqttSniffedPacket.packetId]), possibly relayed to the broker by several gateways. [gatewayIds] lists every gateway
 * this packet was seen from, most-recently-seen first, deduplicated. [packet] is the most recently received copy -- its
 * own [MqttSniffedPacket.gatewayId] is superseded by [gatewayIds] for display.
 */
data class GroupedMqttSniffedPacket(val packet: MqttSniffedPacket, val gatewayIds: List<String>)

/**
 * Groups packets sharing the same [MqttSniffedPacket.packetId] -- i.e. the same over-the-air transmission relayed by
 * several gateways -- into a single row per packet, newest first. Entries with no packetId (JSON entries, or anything
 * whose originating MeshPacket id wasn't available) are never merged and each get their own single-gateway row. Pure
 * and stateless: called from the display layer only when the user has grouping enabled -- the underlying
 * [MqttSnifferManager.packets] list itself always stays ungrouped.
 */
fun List<MqttSniffedPacket>.groupedByGateway(): List<GroupedMqttSniffedPacket> {
    val (groupable, ungroupable) = partition { it.packetId != null }
    val grouped =
        groupable
            .groupBy { it.packetId }
            .values
            .map { packets ->
                val newest = packets.maxBy { it.receivedAtMillis }
                val gatewayIds = packets.mapNotNull { it.gatewayId }.distinct()
                GroupedMqttSniffedPacket(newest, gatewayIds)
            }
    val singles = ungroupable.map { GroupedMqttSniffedPacket(it, listOfNotNull(it.gatewayId)) }
    return (grouped + singles).sortedByDescending { it.packet.receivedAtMillis }
}

/**
 * Owns the MQTT Sniffer's live state as an app-wide singleton -- not a ViewModel -- so the toggle on the combined
 * Sniffer settings screen and the live log screen (two separate navigation entries, each with its own
 * [MqttSnifferLogViewModel] instance) both observe and control the *same* connection and packet buffer, the same way
 * [org.meshtastic.core.data.manager.MqttManagerImpl] backs the MQTT Client Proxy feature across screens.
 *
 * This app's MQTT client library allows only one active broker session at a time (see [MQTTRepository]) -- the same
 * session "MQTT Client Proxy" uses to relay packets to the connected node. Turning this sniffer on takes over that
 * session for as long as it stays active (the user is warned about this before enabling it, same as the Radio Sniffer's
 * own queue-contention warning); turning it off releases the session back for the proxy to reclaim on its next
 * collection.
 *
 * Nothing here is persisted -- packets live only in [packets] for as long as the app process does, same rationale as
 * the Radio Sniffer having no separate persistence beyond the shared [org.meshtastic.core.model.MeshLog] table.
 */
@Single
class MqttSnifferManager(
    private val mqttRepository: MQTTRepository,
    private val nodeRepository: NodeRepository,
    radioConfigRepository: RadioConfigRepository,
    private val prefs: SnifferLogPrefs,
    private val scope: ServiceScope,
) {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _packets = MutableStateFlow<List<MqttSniffedPacket>>(emptyList())
    val packets: StateFlow<List<MqttSniffedPacket>> = _packets.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = mqttRepository.connectionState

    private val channelSet = radioConfigRepository.channelSetFlow.stateIn(scope, SharingStarted.Eagerly, null)

    private var collectJob: Job? = null

    // Mirrors the Radio Sniffer's same setting (SnifferLogPrefs.bufferOverflowPolicy) -- one shared preference for
    // both sniffer sources' buffers, read here as a plain field since onMessage() isn't itself a Flow collector.
    private var overflowPolicy = SnifferBufferOverflowPolicy.STOP

    init {
        scope.launch { prefs.bufferOverflowPolicy.collect { overflowPolicy = it } }
    }

    /** Starts or stops the MQTT connection for this sniffer. See the class doc for the session-sharing caveat. */
    fun setActive(enabled: Boolean) {
        if (enabled == _active.value) return
        _active.value = enabled
        if (enabled) {
            collectJob =
                scope.launch {
                    mqttRepository
                        .proxyMessageFlow(subscribeAllChannels = true)
                        .catch { /* connectionState already reflects the failure; nothing else to surface here */ }
                        .collect(::onMessage)
                }
        } else {
            collectJob?.cancel()
            collectJob = null
        }
    }

    fun clear() {
        _packets.value = emptyList()
    }

    private fun onMessage(message: MqttClientProxyMessage) {
        val entry = decodeMessage(message) ?: return
        _packets.update { current ->
            when (overflowPolicy) {
                // Full: stop buffering entirely, keep whatever was captured first, same as the Radio Sniffer.
                SnifferBufferOverflowPolicy.STOP ->
                    if (current.size >= SnifferLogPrefs.MAX_BUFFERED_PACKETS) current else listOf(entry) + current

                SnifferBufferOverflowPolicy.OVERWRITE ->
                    (listOf(entry) + current).take(SnifferLogPrefs.MAX_BUFFERED_PACKETS)
            }
        }
    }

    /** Every channel this app currently holds an (already-expanded, see [Channel.psk]) key for. */
    private fun knownChannelPsks(): List<ByteArray> {
        val set = channelSet.value ?: return emptyList()
        val loraConfig = set.lora_config ?: LoRaConfig.Builder().build()
        return set.settings.map { Channel(it, loraConfig).psk.toByteArray() }
    }

    private fun decodeMessage(message: MqttClientProxyMessage): MqttSniffedPacket? {
        val now = Clock.System.now().toEpochMilliseconds()
        val jsonText = message.text
        if (jsonText != null) {
            return MqttSniffedPacket(
                topic = message.topic,
                channelId = null,
                fromId = "",
                toId = "",
                fromShortName = null,
                toShortName = null,
                portNum = null,
                isEncrypted = false,
                payloadHex = "",
                decodedPayload = jsonText,
                isJson = true,
                receivedAtMillis = now,
            )
        }

        val bytes = message.data_?.toByteArray() ?: return null
        val envelope = runCatching { ServiceEnvelope.ADAPTER.decode(bytes) }.getOrNull() ?: return null
        val packet = envelope.packet ?: return null
        return toSniffedPacket(message.topic, envelope.channel_id, packet, envelope.gateway_id, now)
    }

    private fun toSniffedPacket(
        topic: String,
        channelId: String?,
        packet: MeshPacket,
        gatewayId: String?,
        receivedAtMillis: Long,
    ): MqttSniffedPacket {
        val nodeMap = nodeRepository.nodeDBbyNum.value
        val decodedFromWire = packet.decoded?.payload
        val decodedPayload = decodePayloadFromPacket(packet, nodeRepository, knownChannelPsks())
        val rawBytes: ByteString? = decodedFromWire ?: packet.encrypted
        return MqttSniffedPacket(
            topic = topic,
            channelId = channelId,
            fromId = NodeAddress.numToDefaultId(packet.from),
            toId = NodeAddress.numToDefaultId(packet.to),
            fromShortName = nodeMap[packet.from]?.user?.short_name?.takeIf { it.isNotBlank() },
            toShortName = nodeMap[packet.to]?.user?.short_name?.takeIf { it.isNotBlank() },
            portNum = packet.decoded?.portnum?.value,
            isEncrypted = decodedPayload == null,
            payloadHex = rawBytes?.hex().orEmpty(),
            decodedPayload = decodedPayload,
            isJson = false,
            receivedAtMillis = receivedAtMillis,
            packetId = packet.id,
            gatewayId = gatewayId?.let { id -> formatGatewayLabel(id, nodeMap) },
        )
    }

    /** Formats a gateway node id as "!hex" or "!hex (SHORTNAME)" when this app already knows that node. */
    private fun formatGatewayLabel(gatewayId: String, nodeMap: Map<Int, org.meshtastic.core.model.Node>): String {
        val shortName =
            NodeAddress.idToNum(gatewayId)?.let { num -> nodeMap[num]?.user?.short_name?.takeIf(String::isNotBlank) }
        return shortName?.let { "$gatewayId ($it)" } ?: gatewayId
    }
}

/** Thin per-screen façade over the shared [MqttSnifferManager] -- see its doc for why this isn't stateful itself. */
@KoinViewModel
class MqttSnifferLogViewModel(private val manager: MqttSnifferManager) : ViewModel() {
    val active: StateFlow<Boolean> = manager.active
    val packets: StateFlow<List<MqttSniffedPacket>> = manager.packets
    val connectionState: StateFlow<ConnectionState> = manager.connectionState

    fun setActive(enabled: Boolean) = manager.setActive(enabled)

    fun clear() = manager.clear()
}
