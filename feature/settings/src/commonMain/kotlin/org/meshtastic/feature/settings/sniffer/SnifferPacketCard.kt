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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.air_utilization
import org.meshtastic.core.resources.channel_utilization
import org.meshtastic.core.resources.discovery_stat_packets_rx
import org.meshtastic.core.resources.discovery_stat_packets_tx
import org.meshtastic.core.resources.free_memory
import org.meshtastic.core.resources.load_indexed
import org.meshtastic.core.resources.local_stats_heap
import org.meshtastic.core.resources.sniffer_category_admin
import org.meshtastic.core.resources.sniffer_category_alert
import org.meshtastic.core.resources.sniffer_category_encrypted
import org.meshtastic.core.resources.sniffer_category_neighborinfo
import org.meshtastic.core.resources.sniffer_category_nodeinfo
import org.meshtastic.core.resources.sniffer_category_paxcounter
import org.meshtastic.core.resources.sniffer_category_position
import org.meshtastic.core.resources.sniffer_category_routing
import org.meshtastic.core.resources.sniffer_category_storeforward
import org.meshtastic.core.resources.sniffer_category_telemetry
import org.meshtastic.core.resources.sniffer_category_text_message
import org.meshtastic.core.resources.sniffer_category_traceroute
import org.meshtastic.core.resources.sniffer_category_unknown
import org.meshtastic.core.resources.sniffer_category_waypoint
import org.meshtastic.core.resources.sniffer_content_title
import org.meshtastic.core.resources.sniffer_packet_metadata_title
import org.meshtastic.core.resources.sniffer_receipts_title
import org.meshtastic.core.resources.sniffer_summary_neighbor_count
import org.meshtastic.core.resources.sniffer_summary_nodeinfo_unknown
import org.meshtastic.core.resources.sniffer_summary_position_unknown
import org.meshtastic.core.resources.sniffer_summary_telemetry_unknown
import org.meshtastic.core.ui.component.CopyIconButton
import org.meshtastic.feature.settings.util.PacketSummary
import org.meshtastic.proto.PortNum

/**
 * The Sniffer Log's per-packet card, for both the Radio ([GroupedRadioPacketCard]) and MQTT ([GroupedMqttPacketCard])
 * sources. Split out of SnifferPanelComponents.kt because the cards' own rendering logic (category colors/labels, the
 * summary line, the expandable receipts list) is a self-contained concern distinct from that file's gear-menu/settings
 * composables.
 */
@Composable
// A lookup table of fixed category colors -- named constants would be noisier here than the literals.
@Suppress("MagicNumber")
internal fun packetCategoryColor(portNum: Int?, isEncrypted: Boolean): Color =
    when {
        isEncrypted -> Color(0xFF9E9E9E)
        portNum == PortNum.POSITION_APP.value -> Color(0xFF4CAF50)
        portNum == PortNum.NODEINFO_APP.value -> Color(0xFF9C27B0)
        portNum == PortNum.NEIGHBORINFO_APP.value -> Color(0xFF00BCD4)
        portNum == PortNum.TELEMETRY_APP.value -> Color(0xFFFF9800)
        portNum == PortNum.TEXT_MESSAGE_APP.value -> Color(0xFF2196F3)
        portNum == PortNum.ALERT_APP.value -> Color(0xFFF44336)
        portNum == PortNum.TRACEROUTE_APP.value -> Color(0xFFFFC107)
        portNum == PortNum.ROUTING_APP.value -> Color(0xFF607D8B)
        portNum == PortNum.ADMIN_APP.value -> Color(0xFFFF5722)
        portNum == PortNum.WAYPOINT_APP.value -> Color(0xFF8BC34A)
        portNum == PortNum.PAXCOUNTER_APP.value -> Color(0xFF795548)
        portNum == PortNum.STORE_FORWARD_APP.value -> Color(0xFF3F51B5)
        portNum == PortNum.STORE_FORWARD_PLUSPLUS_APP.value -> Color(0xFF3F51B5)
        else -> Color(0xFF757575)
    }

@Composable
internal fun packetCategoryLabel(portNum: Int?, isEncrypted: Boolean): String = when {
    isEncrypted -> stringResource(Res.string.sniffer_category_encrypted)
    portNum == PortNum.POSITION_APP.value -> stringResource(Res.string.sniffer_category_position)
    portNum == PortNum.NODEINFO_APP.value -> stringResource(Res.string.sniffer_category_nodeinfo)
    portNum == PortNum.NEIGHBORINFO_APP.value -> stringResource(Res.string.sniffer_category_neighborinfo)
    portNum == PortNum.TELEMETRY_APP.value -> stringResource(Res.string.sniffer_category_telemetry)
    portNum == PortNum.TEXT_MESSAGE_APP.value -> stringResource(Res.string.sniffer_category_text_message)
    portNum == PortNum.ALERT_APP.value -> stringResource(Res.string.sniffer_category_alert)
    portNum == PortNum.TRACEROUTE_APP.value -> stringResource(Res.string.sniffer_category_traceroute)
    portNum == PortNum.ROUTING_APP.value -> stringResource(Res.string.sniffer_category_routing)
    portNum == PortNum.ADMIN_APP.value -> stringResource(Res.string.sniffer_category_admin)
    portNum == PortNum.WAYPOINT_APP.value -> stringResource(Res.string.sniffer_category_waypoint)
    portNum == PortNum.PAXCOUNTER_APP.value -> stringResource(Res.string.sniffer_category_paxcounter)
    portNum == PortNum.STORE_FORWARD_APP.value -> stringResource(Res.string.sniffer_category_storeforward)
    portNum == PortNum.STORE_FORWARD_PLUSPLUS_APP.value -> stringResource(Res.string.sniffer_category_storeforward)
    else -> stringResource(Res.string.sniffer_category_unknown)
}

private const val POSITION_DECIMAL_PLACES = 5

/** Localizes a [PacketSummary] into the card's always-visible content line -- null when there is nothing to show. */
@Composable
internal fun PacketSummary.render(): String? = when (this) {
    is PacketSummary.Text -> text

    is PacketSummary.PositionSummary ->
        if (latitude != null && longitude != null) {
            val coords =
                "${NumberFormatter.format(latitude, POSITION_DECIMAL_PLACES)}, " +
                    NumberFormatter.format(longitude, POSITION_DECIMAL_PLACES)
            altitudeMeters?.let { "$coords ($it m)" } ?: coords
        } else {
            stringResource(Res.string.sniffer_summary_position_unknown)
        }

    is PacketSummary.NodeInfoSummary ->
        listOfNotNull(longName, shortName?.let { "($it)" }).joinToString(" ").ifBlank {
            stringResource(Res.string.sniffer_summary_nodeinfo_unknown)
        }

    is PacketSummary.TelemetrySummary -> {
        val chUtilLabel = stringResource(Res.string.channel_utilization)
        val airUtilLabel = stringResource(Res.string.air_utilization)
        val parts = buildList {
            temperatureCelsius?.let { add(MetricFormatter.temperature(it, isFahrenheit = false)) }
            humidityPercent?.let { add(MetricFormatter.humidity(it)) }
            pressureHpa?.let { add(MetricFormatter.pressure(it)) }
            voltage?.let { add(MetricFormatter.voltage(it)) }
            currentMilliAmps?.let { add(MetricFormatter.current(it, decimalPlaces = 2)) }
            batteryPercent?.let { add(MetricFormatter.percent(it)) }
            uptimeSeconds?.let { add(formatUptimeShort(it)) }
            // Labelled, unlike the metrics above: with battery/humidity already on the same line, a bare "%" is
            // ambiguous between four different percentages. ChUtil/AirUtil reuse the app's existing short labels.
            channelUtilizationPercent?.let { add("$chUtilLabel ${MetricFormatter.percent(it)}") }
            airUtilTxPercent?.let { add("$airUtilLabel ${MetricFormatter.percent(it)}") }
        }
        parts.joinToString(" • ").ifBlank { stringResource(Res.string.sniffer_summary_telemetry_unknown) }
    }

    is PacketSummary.HostMetricsSummary -> {
        val loadLabel = stringResource(Res.string.load_indexed, 1)
        val freeMemLabel = stringResource(Res.string.free_memory)
        val parts = buildList {
            uptimeSeconds?.let { add(formatUptimeShort(it)) }
            freeMemBytes?.let { add("$freeMemLabel ${formatBytesShort(it)}") }
            load1?.let { add("$loadLabel ${NumberFormatter.format(it / LOAD_SCALE, 2)}") }
        }
        parts.joinToString(" • ").ifBlank { stringResource(Res.string.sniffer_summary_telemetry_unknown) }
    }

    is PacketSummary.PowerMetricsSummary ->
        channels
            .joinToString(" \u2022 ") { ch ->
                val readings =
                    buildList {
                        ch.voltage?.let { add(MetricFormatter.voltage(it)) }
                        ch.currentMilliAmps?.let { add(MetricFormatter.current(it, decimalPlaces = 1)) }
                    }
                "CH${ch.channel} ${readings.joinToString(" ")}"
            }
            .ifBlank { stringResource(Res.string.sniffer_summary_telemetry_unknown) }

    is PacketSummary.LocalStatsSummary -> {
        val txLabel = stringResource(Res.string.discovery_stat_packets_tx)
        val rxLabel = stringResource(Res.string.discovery_stat_packets_rx)
        val heapLabel = stringResource(Res.string.local_stats_heap)
        val chUtilLabel = stringResource(Res.string.channel_utilization)
        val airUtilLabel = stringResource(Res.string.air_utilization)
        val parts = buildList {
            uptimeSeconds?.let { add(formatUptimeShort(it)) }
            if (numOnlineNodes != null && numTotalNodes != null) add("$numOnlineNodes/$numTotalNodes")
            numPacketsTx?.let { add("$txLabel $it") }
            numPacketsRx?.let { add("$rxLabel $it") }
            if (heapFreeBytes != null && heapTotalBytes != null) {
                val free = formatBytesShort(heapFreeBytes.toLong())
                val total = formatBytesShort(heapTotalBytes.toLong())
                add("$heapLabel $free/$total")
            }
            channelUtilizationPercent?.let { add("$chUtilLabel ${MetricFormatter.percent(it)}") }
            airUtilTxPercent?.let { add("$airUtilLabel ${MetricFormatter.percent(it)}") }
        }
        parts.joinToString(" \u2022 ").ifBlank { stringResource(Res.string.sniffer_summary_telemetry_unknown) }
    }

    is PacketSummary.NeighborCount -> stringResource(Res.string.sniffer_summary_neighbor_count, count)
}

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_HOUR = 3600
private const val HOURS_PER_DAY = 24
private const val BYTES_PER_KB = 1024.0
private const val LOAD_SCALE = 100f

/** "2d 3h" once past a day, else just "Xh" -- mirrors the app's other uptime display (NetworkSummaryScreen). */
private fun formatUptimeShort(seconds: Int): String {
    val hours = seconds / SECONDS_PER_HOUR
    val days = hours / HOURS_PER_DAY
    return if (days > 0) "${days}d ${hours % HOURS_PER_DAY}h" else "${hours}h"
}

/** Compact free-memory reading for the summary line -- KB below 1 MB, MB above. */
private fun formatBytesShort(bytes: Long): String {
    val kb = bytes / BYTES_PER_KB
    return if (kb < BYTES_PER_KB) {
        "${NumberFormatter.format(kb, 0)} KB"
    } else {
        "${NumberFormatter.format(kb / BYTES_PER_KB, 1)} MB"
    }
}

/** Timestamp for the first (oldest) receipt, absolute; every later one as a delta from it -- "+32ms" / "+2.4s". */
private fun formatReceiptTime(receivedAtMillis: Long, firstReceivedAtMillis: Long): String {
    if (receivedAtMillis <= firstReceivedAtMillis) {
        return "${DateFormatter.formatDate(receivedAtMillis)} ${DateFormatter.formatTimeWithSeconds(receivedAtMillis)}"
    }
    val deltaMs = receivedAtMillis - firstReceivedAtMillis
    return if (deltaMs < MILLIS_PER_SECOND) {
        "+${deltaMs}ms"
    } else {
        "+${NumberFormatter.format(deltaMs / MILLIS_PER_SECOND.toDouble(), 1)}s"
    }
}

/** One row of the expandable "receipts" list -- who this copy came through, its signal, and when. */
@Composable
private fun ReceiptRow(label: String, snr: Float?, rssi: Int?, timeLabel: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            text = listOfNotNull(MetricFormatter.snr(snr), MetricFormatter.rssi(rssi)).joinToString(" • "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Shared visual shell for both card types: a colored left strip keyed to the packet's category, dark card body. */
@Composable
internal fun PacketCardShell(
    portNum: Int?,
    isEncrypted: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(packetCategoryColor(portNum, isEncrypted)))
            Column(
                modifier = Modifier.weight(1f).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

/** The card's top row: colored category label on the left, absolute timestamp on the right. */
@Composable
internal fun PacketCardHeader(label: String, color: Color, receivedAtMillis: Long) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = color)
        Text(
            text =
            "${DateFormatter.formatDate(receivedAtMillis)} " +
                DateFormatter.formatTimeWithSeconds(receivedAtMillis),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Source-agnostic view of one physical reception, for the shared expanded "receipts" list below. */
internal data class DisplayReceipt(val label: String, val snr: Float?, val rssi: Int?, val receivedAtMillis: Long)

/** The card's expanded content: the receipts list (when there's more than one), the decoded text, and metadata. */
@Composable
internal fun PacketExpandedDetails(
    receipts: List<DisplayReceipt>,
    decodedText: String,
    metadataLines: List<String>,
    copyText: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (receipts.size > 1) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(Res.string.sniffer_receipts_title, receipts.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val firstAt = receipts.minOf { it.receivedAtMillis }
            receipts.forEach { receipt ->
                ReceiptRow(
                    label = receipt.label,
                    snr = receipt.snr,
                    rssi = receipt.rssi,
                    timeLabel = formatReceiptTime(receipt.receivedAtMillis, firstAt),
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = stringResource(Res.string.sniffer_content_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = decodedText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
        if (metadataLines.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(Res.string.sniffer_packet_metadata_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            metadataLines.forEach { line -> Text(text = line, style = MaterialTheme.typography.bodySmall) }
        }
        CopyIconButton(valueToCopy = copyText, modifier = Modifier.padding(top = 4.dp))
    }
}
