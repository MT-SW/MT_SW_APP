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

import kotlinx.serialization.Serializable
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.meshtastic.core.model.Channel
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.feature.settings.sniffer.mqtt.MqttSniffedPacket
import org.meshtastic.feature.settings.util.decodePayloadFromPacket
import org.meshtastic.proto.MeshPacket

/**
 * Source-agnostic snapshot of one displayed row, shared by export (txt/JSON/CSV, see [buildSnifferExport] in
 * SnifferLogExport.kt) and log loading (JSON only, see [parseSnifferExportJson]) so the combined Sniffer panel doesn't
 * need two parallel sets of file-format code for [SniffedPacket] (Radio) and [MqttSniffedPacket] (MQTT). JSON is the
 * only format read back in -- txt and CSV are for humans, not for this app. `@Serializable` defaults keep old and new
 * saved files forward/backward compatible as fields get added.
 */
@Serializable
data class SnifferExportRow(
    val source: String,
    val receivedAtMillis: Long,
    val fromId: String,
    val toId: String,
    val fromShortName: String? = null,
    val toShortName: String? = null,
    /** Radio: the channel index, as a string. MQTT: the channel id (name). */
    val channel: String? = null,
    val portNum: Int? = null,
    val isEncrypted: Boolean = false,
    val payloadHex: String = "",
    val decodedPayload: String? = null,
    val isJson: Boolean = false,
    /** The originating MeshPacket's id -- lets [reattemptDecryption] retry decoding after a reload. */
    val packetId: Int? = null,
    /** MQTT only: every gateway this packet was seen relayed from (see [groupedByGateway]). */
    val gatewayIds: List<String> = emptyList(),
    /** MQTT only: the broker topic this packet was published on. */
    val topic: String? = null,
    val rssi: Int? = null,
    val snr: Float? = null,
    val hopStart: Int? = null,
    val hopLimit: Int? = null,
)

private const val SOURCE_RADIO = "radio"
private const val SOURCE_MQTT = "mqtt"

fun SniffedPacket.toExportRow(): SnifferExportRow = SnifferExportRow(
    source = SOURCE_RADIO,
    receivedAtMillis = receivedAtMillis,
    fromId = fromId,
    toId = toId,
    fromShortName = fromShortName,
    toShortName = toShortName,
    channel = channel.toString(),
    portNum = portNum,
    isEncrypted = isEncrypted,
    payloadHex = payloadHex,
    decodedPayload = decodedPayload,
    packetId = packetId,
    rssi = rssi,
    snr = snr,
    hopStart = hopStart,
    hopLimit = hopLimit,
)

/** [gatewayIds] defaults to this packet's own single gateway; pass a group's full list when grouping is enabled. */
fun MqttSniffedPacket.toExportRow(gatewayIds: List<String> = listOfNotNull(gatewayId)): SnifferExportRow =
    SnifferExportRow(
        source = SOURCE_MQTT,
        receivedAtMillis = receivedAtMillis,
        fromId = fromId,
        toId = toId,
        fromShortName = fromShortName,
        toShortName = toShortName,
        channel = channelId,
        portNum = portNum,
        isEncrypted = isEncrypted,
        payloadHex = payloadHex,
        decodedPayload = decodedPayload,
        isJson = isJson,
        packetId = packetId,
        gatewayIds = gatewayIds,
        topic = topic,
    )

/** Reconstructs the Radio packet a loaded row described, or null if it wasn't one ([SnifferExportRow.source]). */
fun SnifferExportRow.toSniffedPacket(): SniffedPacket? {
    if (source != SOURCE_RADIO) return null
    return SniffedPacket(
        fromId = fromId,
        toId = toId,
        fromShortName = fromShortName,
        toShortName = toShortName,
        channel = channel?.toIntOrNull() ?: 0,
        hopStart = hopStart ?: 0,
        hopLimit = hopLimit ?: 0,
        rssi = rssi,
        snr = snr ?: 0f,
        portNum = portNum,
        isEncrypted = isEncrypted,
        payloadHex = payloadHex,
        decodedPayload = decodedPayload,
        receivedAtMillis = receivedAtMillis,
        packetId = packetId,
    )
}

/**
 * Reconstructs the MQTT packet(s) a loaded row described, or empty if it wasn't one. A grouped row with several
 * [SnifferExportRow.gatewayIds] expands back into one [MqttSniffedPacket] per gateway -- the same shape the live MQTT
 * Sniffer would have produced -- so [groupedByGateway] can re-group them for display exactly as it does live.
 */
fun SnifferExportRow.toMqttSniffedPackets(): List<MqttSniffedPacket> {
    if (source != SOURCE_MQTT) return emptyList()
    val gateways = gatewayIds.ifEmpty { listOf(null) }
    return gateways.map { gatewayId ->
        MqttSniffedPacket(
            topic = topic.orEmpty(),
            channelId = channel,
            fromId = fromId,
            toId = toId,
            fromShortName = fromShortName,
            toShortName = toShortName,
            portNum = portNum,
            isEncrypted = isEncrypted,
            payloadHex = payloadHex,
            decodedPayload = decodedPayload,
            isJson = isJson,
            receivedAtMillis = receivedAtMillis,
            packetId = packetId,
            gatewayId = gatewayId,
        )
    }
}

/**
 * Retries decoding this row's [payloadHex] against every channel key this app currently holds, the same way live
 * sniffing does. Returns this row unchanged if it wasn't encrypted, already had a decoded payload, or doesn't carry
 * enough saved metadata (packetId, a parseable fromId, valid hex) to retry.
 */
fun SnifferExportRow.reattemptDecryption(
    nodeRepository: NodeRepository,
    knownChannels: List<Channel>,
): SnifferExportRow {
    if (!isEncrypted || decodedPayload != null || knownChannels.isEmpty()) return this
    val decoded = decodeRetriedPayload(nodeRepository, knownChannels)
    return decoded?.let { copy(decodedPayload = it, isEncrypted = false) } ?: this
}

/**
 * Reconstructs just enough of a [MeshPacket] (id, from, the encrypted bytes) for [decodePayloadFromPacket] to attempt
 * [knownChannels] against it, then reuses its normal portnum-aware decoding so a reloaded log can show newly-readable
 * payloads for channels added/joined since the log was saved. Split out of [reattemptDecryption] to keep each
 * function's guard clauses within detekt's ReturnCount limit.
 */
private fun SnifferExportRow.decodeRetriedPayload(
    nodeRepository: NodeRepository,
    knownChannels: List<Channel>,
): String? {
    val id = packetId
    val from = NodeAddress.idToNum(fromId)
    val encrypted = payloadHex.decodeHexOrNull()
    if (id == null || from == null || encrypted == null) return null
    val packet =
        MeshPacket.Builder()
            .also { wb ->
                wb.id = id
                wb.from = from
                wb.encrypted = encrypted
            }
            .build()
    return decodePayloadFromPacket(packet, nodeRepository, knownChannels)
}

private const val HEX_SHIFT_HIGH_NIBBLE = 4
private const val HEX_ALPHA_DIGIT_OFFSET = 10

/**
 * Pure-Kotlin hex decode -- avoids depending on okio's `String.decodeHex()` top-level extension, which turned out not
 * to resolve on this module's classpath. Returns null for an odd-length or non-hex string rather than throwing.
 */
private fun String.decodeHexOrNull(): ByteString? {
    if (length % 2 != 0) return null
    return runCatching {
        ByteArray(length / 2) { i ->
            val hi = hexDigit(this[i * 2])
            val lo = hexDigit(this[i * 2 + 1])
            ((hi shl HEX_SHIFT_HIGH_NIBBLE) or lo).toByte()
        }
            .toByteString()
    }
        .getOrNull()
}

/** Throws for a non-hex character, letting [decodeHexOrNull]'s `runCatching` fold that into a null result. */
private fun hexDigit(c: Char): Int = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + HEX_ALPHA_DIGIT_OFFSET
    in 'A'..'F' -> c - 'A' + HEX_ALPHA_DIGIT_OFFSET
    else -> throw NumberFormatException("Invalid hex digit: $c")
}
