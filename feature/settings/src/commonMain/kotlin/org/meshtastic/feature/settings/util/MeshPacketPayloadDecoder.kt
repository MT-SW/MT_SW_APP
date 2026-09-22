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
package org.meshtastic.feature.settings.util

import org.meshtastic.core.common.crypto.ChannelCrypto
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.model.MeshLog
import org.meshtastic.core.model.getTracerouteResponse
import org.meshtastic.core.model.util.decodeOrNull
import org.meshtastic.core.model.util.toReadableString
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.proto.AdminMessage
import org.meshtastic.proto.Data
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.NeighborInfo
import org.meshtastic.proto.Paxcount
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.Position
import org.meshtastic.proto.RouteDiscovery
import org.meshtastic.proto.Routing
import org.meshtastic.proto.StoreAndForward
import org.meshtastic.proto.StoreForwardPlusPlus
import org.meshtastic.proto.Telemetry
import org.meshtastic.proto.User
import org.meshtastic.proto.Waypoint

/**
 * Decodes a [MeshPacket]'s payload into a human-readable string, shared between the Debug Panel and the Sniffer Log
 * screens so both show packets the same way.
 *
 * For known portnums, the payload is parsed into its corresponding proto message and returned as a string. For text and
 * alert messages, the payload is interpreted as UTF-8 text. For unknown portnums, the payload is shown as a hex string.
 *
 * @param log The MeshLog containing the packet and payload to decode.
 * @return A human-readable string representation of the decoded payload, or an error message if decoding fails, or null
 *   if the log does not contain a decodable packet.
 */
fun decodePayloadFromMeshLog(log: MeshLog, nodeRepository: NodeRepository): String? {
    val packet = log.meshPacket ?: return null
    return decodePayloadFromPacket(packet, nodeRepository)
}

/**
 * Decodes a [MeshPacket]'s payload the same way [decodePayloadFromMeshLog] does, but from a bare packet -- used by the
 * MQTT Sniffer, which sources packets from MQTT-published `ServiceEnvelope`s rather than a [MeshLog] row.
 *
 * If the packet is still channel-encrypted, first tries [ChannelCrypto] against [knownChannelPsks] (channels this app
 * already holds the key for -- the same channels firmware itself would be able to decrypt) before giving up and
 * returning null (hex display) for anything it truly can't read.
 */
@Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
fun decodePayloadFromPacket(
    packet: MeshPacket,
    nodeRepository: NodeRepository,
    knownChannelPsks: List<ByteArray> = emptyList(),
): String? {
    val decoded = packet.decoded ?: decryptWithKnownChannels(packet, knownChannelPsks) ?: return null

    val portnumValue = decoded.portnum.value
    val payload = decoded.payload.toByteArray()
    return try {
        when (portnumValue) {
            PortNum.TEXT_MESSAGE_APP.value,
            PortNum.ALERT_APP.value,
            -> payload.decodeToString()

            PortNum.POSITION_APP.value ->
                Position.ADAPTER.decodeOrNull(payload)?.let { Position.ADAPTER.toReadableString(it) }
                    ?: "Failed to decode Position"

            PortNum.WAYPOINT_APP.value ->
                Waypoint.ADAPTER.decodeOrNull(payload)?.let { Waypoint.ADAPTER.toReadableString(it) }
                    ?: "Failed to decode Waypoint"

            PortNum.NODEINFO_APP.value ->
                User.ADAPTER.decodeOrNull(payload)?.let { User.ADAPTER.toReadableString(it) } ?: "Failed to decode User"

            PortNum.TELEMETRY_APP.value ->
                Telemetry.ADAPTER.decodeOrNull(payload)?.let { Telemetry.ADAPTER.toReadableString(it) }
                    ?: "Failed to decode Telemetry"

            PortNum.ROUTING_APP.value ->
                Routing.ADAPTER.decodeOrNull(payload)?.let { Routing.ADAPTER.toReadableString(it) }
                    ?: "Failed to decode Routing"

            PortNum.ADMIN_APP.value ->
                AdminMessage.ADAPTER.decodeOrNull(payload)?.let { AdminMessage.ADAPTER.toReadableString(it) }
                    ?: "Failed to decode AdminMessage"

            PortNum.PAXCOUNTER_APP.value ->
                Paxcount.ADAPTER.decodeOrNull(payload)?.let { Paxcount.ADAPTER.toReadableString(it) }
                    ?: "Failed to decode Paxcount"

            PortNum.STORE_FORWARD_APP.value ->
                StoreAndForward.ADAPTER.decodeOrNull(payload)?.let { StoreAndForward.ADAPTER.toReadableString(it) }
                    ?: "Failed to decode StoreAndForward"

            PortNum.STORE_FORWARD_PLUSPLUS_APP.value ->
                StoreForwardPlusPlus.ADAPTER.decodeOrNull(payload)?.let {
                    StoreForwardPlusPlus.ADAPTER.toReadableString(it)
                } ?: "Failed to decode StoreForwardPlusPlus"

            PortNum.NEIGHBORINFO_APP.value -> decodeNeighborInfoPayload(payload, nodeRepository)

            PortNum.TRACEROUTE_APP.value -> decodeTraceroutePayload(packet, payload, nodeRepository)

            else -> payload.joinToString(" ") { it.toPayloadHex() }
        }
    } catch (e: Exception) {
        "Failed to decode payload: ${e.message}"
    }
}

/**
 * Tries each of [knownChannelPsks] against [packet]'s `encrypted` bytes (AES-CTR, see [ChannelCrypto]), returning the
 * first successfully-decoded [Data] submessage, or null if none of them fit -- either because this app doesn't hold the
 * right channel's key, or the packet isn't encrypted at all.
 */
private fun decryptWithKnownChannels(packet: MeshPacket, knownChannelPsks: List<ByteArray>): Data? {
    val encrypted = packet.encrypted?.toByteArray() ?: return null
    for (psk in knownChannelPsks) {
        val plain =
            ChannelCrypto.decrypt(psk, packetId = packet.id, fromNode = packet.from, data = encrypted) ?: continue
        val data = Data.ADAPTER.decodeOrNull(plain) ?: continue
        return data
    }
    return null
}

private fun Byte.toPayloadHex(): String = this.toUByte().toString(16).padStart(2, '0')

private fun formatNodeWithShortNameForPayload(nodeNum: Int, nodeRepository: NodeRepository): String {
    val user = nodeRepository.nodeDBbyNum.value[nodeNum]?.user
    val shortName = user?.short_name?.takeIf { it.isNotEmpty() } ?: ""
    val nodeId = "!${nodeNum.toUInt().toString(16).padStart(8, '0')}"
    return if (shortName.isNotEmpty()) "$nodeId ($shortName)" else nodeId
}

private fun decodeNeighborInfoPayload(payload: ByteArray, nodeRepository: NodeRepository): String {
    val info = NeighborInfo.ADAPTER.decode(payload)
    return buildString {
        appendLine("NeighborInfo:")
        appendLine("  node_id: ${formatNodeWithShortNameForPayload(info.node_id, nodeRepository)}")
        appendLine("  last_sent_by_id: ${formatNodeWithShortNameForPayload(info.last_sent_by_id, nodeRepository)}")
        appendLine("  node_broadcast_interval_secs: ${info.node_broadcast_interval_secs}")
        if (info.neighbors.isNotEmpty()) {
            appendLine("  neighbors:")
            info.neighbors.forEach {
                appendLine(
                    "    - node_id: ${formatNodeWithShortNameForPayload(it.node_id, nodeRepository)} " +
                        "snr: ${MetricFormatter.snr(it.snr)}",
                )
            }
        }
    }
}

private fun decodeTraceroutePayload(packet: MeshPacket, payload: ByteArray, nodeRepository: NodeRepository): String {
    val getUsername: (Int) -> String = { nodeNum -> formatNodeWithShortNameForPayload(nodeNum, nodeRepository) }
    return packet.getTracerouteResponse(getUsername)
        ?: runCatching { RouteDiscovery.ADAPTER.decode(payload).toString() }.getOrNull()
        ?: payload.joinToString(" ") { it.toPayloadHex() }
}
