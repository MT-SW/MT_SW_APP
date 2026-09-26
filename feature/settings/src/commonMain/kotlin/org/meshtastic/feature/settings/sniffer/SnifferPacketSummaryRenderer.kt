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

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
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
import org.meshtastic.core.resources.sniffer_summary_neighbor_count
import org.meshtastic.core.resources.sniffer_summary_nodeinfo_unknown
import org.meshtastic.core.resources.sniffer_summary_position_unknown
import org.meshtastic.core.resources.sniffer_summary_telemetry_unknown
import org.meshtastic.feature.settings.util.PacketSummary

/**
 * Renders a [PacketSummary] into the Sniffer Log card's always-visible content line. Split out of
 * SnifferPacketCard.kt, whose `render()` plus its per-variant helpers had grown past this repo's detekt
 * CyclomaticComplexMethod/LongMethod limits as a single function, and once split into helpers, past its
 * TooManyFunctions-per-file limit alongside that file's other card-chrome composables.
 */
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

    is PacketSummary.TelemetrySummary -> renderTelemetrySummary(this)

    is PacketSummary.HostMetricsSummary -> renderHostMetricsSummary(this)

    is PacketSummary.PowerMetricsSummary -> renderPowerMetricsSummary(this)

    is PacketSummary.LocalStatsSummary -> renderLocalStatsSummary(this)

    is PacketSummary.NeighborCount -> stringResource(Res.string.sniffer_summary_neighbor_count, count)
}

@Composable
private fun renderTelemetrySummary(summary: PacketSummary.TelemetrySummary): String {
    val chUtilLabel = stringResource(Res.string.channel_utilization)
    val airUtilLabel = stringResource(Res.string.air_utilization)
    val parts = buildList {
        summary.temperatureCelsius?.let { add(MetricFormatter.temperature(it, isFahrenheit = false)) }
        summary.humidityPercent?.let { add(MetricFormatter.humidity(it)) }
        summary.pressureHpa?.let { add(MetricFormatter.pressure(it)) }
        summary.voltage?.let { add(MetricFormatter.voltage(it)) }
        summary.currentMilliAmps?.let { add(MetricFormatter.current(it, decimalPlaces = 2)) }
        summary.batteryPercent?.let { add(MetricFormatter.percent(it)) }
        summary.uptimeSeconds?.let { add(formatUptimeShort(it)) }
        // Labelled, unlike the metrics above: with battery/humidity already on the same line, a bare "%" is
        // ambiguous between four different percentages. ChUtil/AirUtil reuse the app's existing short labels.
        summary.channelUtilizationPercent?.let { add("$chUtilLabel ${MetricFormatter.percent(it)}") }
        summary.airUtilTxPercent?.let { add("$airUtilLabel ${MetricFormatter.percent(it)}") }
    }
    return parts.joinToString(" \u2022 ").ifBlank { stringResource(Res.string.sniffer_summary_telemetry_unknown) }
}

@Composable
private fun renderHostMetricsSummary(summary: PacketSummary.HostMetricsSummary): String {
    val loadLabel = stringResource(Res.string.load_indexed, 1)
    val freeMemLabel = stringResource(Res.string.free_memory)
    val parts = buildList {
        summary.uptimeSeconds?.let { add(formatUptimeShort(it)) }
        summary.freeMemBytes?.let { add("$freeMemLabel ${formatBytesShort(it)}") }
        summary.load1?.let { add("$loadLabel ${NumberFormatter.format(it / LOAD_SCALE, 2)}") }
    }
    return parts.joinToString(" \u2022 ").ifBlank { stringResource(Res.string.sniffer_summary_telemetry_unknown) }
}

@Composable
private fun renderPowerMetricsSummary(summary: PacketSummary.PowerMetricsSummary): String =
    summary.channels
        .joinToString(" \u2022 ") { ch ->
            val readings = buildList {
                ch.voltage?.let { add(MetricFormatter.voltage(it)) }
                ch.currentMilliAmps?.let { add(MetricFormatter.current(it, decimalPlaces = 1)) }
            }
            "CH${ch.channel} ${readings.joinToString(" ")}"
        }
        .ifBlank { stringResource(Res.string.sniffer_summary_telemetry_unknown) }

@Composable
private fun renderLocalStatsSummary(summary: PacketSummary.LocalStatsSummary): String {
    val txLabel = stringResource(Res.string.discovery_stat_packets_tx)
    val rxLabel = stringResource(Res.string.discovery_stat_packets_rx)
    val heapLabel = stringResource(Res.string.local_stats_heap)
    val chUtilLabel = stringResource(Res.string.channel_utilization)
    val airUtilLabel = stringResource(Res.string.air_utilization)
    val parts = buildList {
        summary.uptimeSeconds?.let { add(formatUptimeShort(it)) }
        val onlineNodes = summary.numOnlineNodes
        val totalNodes = summary.numTotalNodes
        if (onlineNodes != null && totalNodes != null) add("$onlineNodes/$totalNodes")
        summary.numPacketsTx?.let { add("$txLabel $it") }
        summary.numPacketsRx?.let { add("$rxLabel $it") }
        val heapFree = summary.heapFreeBytes
        val heapTotal = summary.heapTotalBytes
        if (heapFree != null && heapTotal != null) {
            add("$heapLabel ${formatBytesShort(heapFree.toLong())}/${formatBytesShort(heapTotal.toLong())}")
        }
        summary.channelUtilizationPercent?.let { add("$chUtilLabel ${MetricFormatter.percent(it)}") }
        summary.airUtilTxPercent?.let { add("$airUtilLabel ${MetricFormatter.percent(it)}") }
    }
    return parts.joinToString(" \u2022 ").ifBlank { stringResource(Res.string.sniffer_summary_telemetry_unknown) }
}

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
