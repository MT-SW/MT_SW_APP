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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.sniffer_chip_hops
import org.meshtastic.core.resources.sniffer_chip_received
import org.meshtastic.core.resources.sniffer_chip_source_count
import org.meshtastic.core.resources.sniffer_packet_metadata_channel
import org.meshtastic.core.resources.sniffer_packet_metadata_id
import org.meshtastic.core.resources.sniffer_receipt_direct
import org.meshtastic.core.resources.sniffer_source_mqtt_chip
import org.meshtastic.core.resources.sniffer_source_radio_chip
import org.meshtastic.feature.settings.sniffer.mqtt.GroupedMqttSniffedPacket

/** The chip row for a Radio sniffer card -- source/relay count, hop count, signal, and the "received N×" highlight. */
@Composable
private fun RadioChipsRow(grouped: GroupedSniffedPacket, onClick: () -> Unit) {
    val packet = grouped.packet
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AssistChip(
            onClick = onClick,
            label = {
                val label = stringResource(Res.string.sniffer_source_radio_chip)
                Text(
                    if (grouped.relayIds.size > 1) {
                        stringResource(Res.string.sniffer_chip_source_count, label, grouped.relayIds.size)
                    } else {
                        label
                    },
                )
            },
        )
        AssistChip(
            onClick = onClick,
            label = {
                Text(stringResource(Res.string.sniffer_chip_hops, packet.hopStart - packet.hopLimit, packet.hopStart))
            },
        )
        AssistChip(
            onClick = onClick,
            label = {
                val signal = listOfNotNull(MetricFormatter.snr(packet.snr), MetricFormatter.rssi(packet.rssi))
                Text(signal.joinToString(" • "))
            },
        )
        if (grouped.receipts.size > 1) {
            AssistChip(
                onClick = onClick,
                colors =
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                label = { Text(stringResource(Res.string.sniffer_chip_received, grouped.receipts.size)) },
            )
        }
    }
}

/** The chip row for an MQTT sniffer card -- source/gateway count, signal (when known), and "received N×". */
@Composable
private fun MqttChipsRow(grouped: GroupedMqttSniffedPacket, onClick: () -> Unit) {
    val packet = grouped.packet
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AssistChip(
            onClick = onClick,
            label = {
                val label = stringResource(Res.string.sniffer_source_mqtt_chip)
                Text(
                    if (grouped.gatewayIds.size > 1) {
                        stringResource(Res.string.sniffer_chip_source_count, label, grouped.gatewayIds.size)
                    } else {
                        label
                    },
                )
            },
        )
        if (packet.snr != null || packet.rssi != null) {
            AssistChip(
                onClick = onClick,
                label = {
                    val signal = listOfNotNull(MetricFormatter.snr(packet.snr), MetricFormatter.rssi(packet.rssi))
                    Text(signal.joinToString(" • "))
                },
            )
        }
        if (grouped.receipts.size > 1) {
            AssistChip(
                onClick = onClick,
                colors =
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                label = { Text(stringResource(Res.string.sniffer_chip_received, grouped.receipts.size)) },
            )
        }
    }
}

@Composable
internal fun GroupedRadioPacketCard(
    grouped: GroupedSniffedPacket,
    decryptPayloads: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val packet = grouped.packet
    PacketCardShell(portNum = packet.portNum, isEncrypted = packet.isEncrypted, onClick = onClick) {
        PacketCardHeader(
            label = packetCategoryLabel(packet.portNum, packet.isEncrypted),
            color = packetCategoryColor(packet.portNum, packet.isEncrypted),
            receivedAtMillis = packet.receivedAtMillis,
        )
        val fromLabel = packet.fromShortName?.let { "${packet.fromId} ($it)" } ?: packet.fromId
        val toLabel = packet.toShortName?.let { "${packet.toId} ($it)" } ?: packet.toId
        Text(text = "$fromLabel → $toLabel", style = MaterialTheme.typography.titleSmall)
        val summaryText = packet.summary?.render()
        if (summaryText != null) {
            Text(text = summaryText, style = MaterialTheme.typography.bodyMedium)
        }
        RadioChipsRow(grouped, onClick)
        if (isExpanded) {
            val directLabel = stringResource(Res.string.sniffer_receipt_direct)
            val metadataLines =
                buildList {
                    add(stringResource(Res.string.sniffer_packet_metadata_channel, packet.channel))
                    packet.packetId?.let { add(stringResource(Res.string.sniffer_packet_metadata_id, it.toString())) }
                }
            val receipts =
                grouped.receipts.map {
                    DisplayReceipt(it.relayId ?: directLabel, it.snr, it.rssi, it.receivedAtMillis)
                }
            PacketExpandedDetails(
                receipts = receipts,
                decodedText = if (decryptPayloads) packet.decodedPayload ?: packet.payloadHex else packet.payloadHex,
                metadataLines = metadataLines,
                copyText = packet.copyText,
            )
        }
    }
}

@Composable
internal fun GroupedMqttPacketCard(
    grouped: GroupedMqttSniffedPacket,
    decryptPayloads: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val packet = grouped.packet
    PacketCardShell(portNum = packet.portNum, isEncrypted = packet.isEncrypted, onClick = onClick) {
        PacketCardHeader(
            label = if (packet.isJson) packet.topic else packetCategoryLabel(packet.portNum, packet.isEncrypted),
            color = packetCategoryColor(packet.portNum, packet.isEncrypted),
            receivedAtMillis = packet.receivedAtMillis,
        )
        if (!packet.isJson && packet.fromId.isNotEmpty()) {
            val fromLabel = packet.fromShortName?.let { "${packet.fromId} ($it)" } ?: packet.fromId
            val toLabel = packet.toShortName?.let { "${packet.toId} ($it)" } ?: packet.toId
            Text(
                text = "$fromLabel → $toLabel" + (packet.channelId?.let { " • $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val summaryText = packet.summary?.render()
        when {
            summaryText != null -> Text(text = summaryText, style = MaterialTheme.typography.bodyMedium)

            packet.isJson ->
                Text(text = packet.decodedPayload ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
        MqttChipsRow(grouped, onClick)
        if (isExpanded) {
            val directLabel = stringResource(Res.string.sniffer_receipt_direct)
            val receipts =
                grouped.receipts.map {
                    DisplayReceipt(it.gatewayId ?: directLabel, it.snr, it.rssi, it.receivedAtMillis)
                }
            PacketExpandedDetails(
                receipts = receipts,
                decodedText = if (decryptPayloads) packet.decodedPayload ?: packet.payloadHex else packet.payloadHex,
                metadataLines = emptyList(),
                copyText = packet.copyText,
            )
        }
    }
}
