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

import org.meshtastic.proto.DeviceMetrics
import org.meshtastic.proto.EnvironmentMetrics
import org.meshtastic.proto.HostMetrics
import org.meshtastic.proto.LocalStats
import org.meshtastic.proto.PowerMetrics
import org.meshtastic.proto.Telemetry

/**
 * Builds a [PacketSummary] from a decoded [Telemetry] proto -- split out of MeshPacketPayloadDecoder.kt (which was over
 * this repo's detekt TooManyFunctions limit once this grew past a single function) and further split, one
 * expression-bodied function per Telemetry oneof variant, to stay under the ReturnCount limit a chain of early
 * `return`s in a single function would have hit.
 *
 * Null when none of the Telemetry oneof's variants this screen understands are present -- an empty/unknown Telemetry
 * packet, or one carrying a variant (air_quality_metrics, health_metrics) this summary doesn't break out yet, has
 * nothing worth summarizing; the raw decode in the expanded card still shows it in full.
 */
internal fun summarizeTelemetry(telemetry: Telemetry): PacketSummary? = summarizeHostMetrics(telemetry.host_metrics)
    ?: summarizeLocalStats(telemetry.local_stats)
    ?: summarizePowerMetrics(telemetry.power_metrics)
    ?: summarizeDeviceOrEnvironmentMetrics(telemetry.environment_metrics, telemetry.device_metrics)

private fun summarizeHostMetrics(host: HostMetrics?): PacketSummary.HostMetricsSummary? = host?.let {
    PacketSummary.HostMetricsSummary(
        uptimeSeconds = it.uptime_seconds,
        freeMemBytes = it.freemem_bytes,
        load1 = it.load1,
        load5 = it.load5,
        load15 = it.load15,
    )
}

private fun summarizeLocalStats(stats: LocalStats?): PacketSummary.LocalStatsSummary? = stats?.let {
    PacketSummary.LocalStatsSummary(
        uptimeSeconds = it.uptime_seconds,
        channelUtilizationPercent = it.channel_utilization?.takeIf { u -> !u.isNaN() },
        airUtilTxPercent = it.air_util_tx?.takeIf { u -> !u.isNaN() },
        numPacketsTx = it.num_packets_tx,
        numPacketsRx = it.num_packets_rx,
        numOnlineNodes = it.num_online_nodes,
        numTotalNodes = it.num_total_nodes,
        heapFreeBytes = it.heap_free_bytes,
        heapTotalBytes = it.heap_total_bytes,
    )
}

private fun summarizePowerMetrics(pm: PowerMetrics?): PacketSummary.PowerMetricsSummary? =
    pm?.let(::powerChannelReadings)?.takeIf { it.isNotEmpty() }?.let(PacketSummary::PowerMetricsSummary)

/** One [PacketSummary.PowerChannelReading] per CH1..CH8 that carries a non-NaN voltage and/or current reading. */
@Suppress("MagicNumber") // 1..8 are channel labels here, not magic values.
private fun powerChannelReadings(pm: PowerMetrics): List<PacketSummary.PowerChannelReading> =
    listOf(
        Triple(1, pm.ch1_voltage, pm.ch1_current),
        Triple(2, pm.ch2_voltage, pm.ch2_current),
        Triple(3, pm.ch3_voltage, pm.ch3_current),
        Triple(4, pm.ch4_voltage, pm.ch4_current),
        Triple(5, pm.ch5_voltage, pm.ch5_current),
        Triple(6, pm.ch6_voltage, pm.ch6_current),
        Triple(7, pm.ch7_voltage, pm.ch7_current),
        Triple(8, pm.ch8_voltage, pm.ch8_current),
    )
        .mapNotNull { (channel, voltage, current) ->
            val validVoltage = voltage?.takeIf { !it.isNaN() }
            val validCurrent = current?.takeIf { !it.isNaN() }
            if (validVoltage == null && validCurrent == null) {
                null
            } else {
                PacketSummary.PowerChannelReading(channel, validVoltage, validCurrent)
            }
        }

private fun summarizeDeviceOrEnvironmentMetrics(
    env: EnvironmentMetrics?,
    device: DeviceMetrics?,
): PacketSummary.TelemetrySummary? = if (env == null && device == null) {
    null
} else {
    PacketSummary.TelemetrySummary(
        temperatureCelsius = env?.temperature?.takeIf { !it.isNaN() },
        humidityPercent = env?.relative_humidity?.takeIf { !it.isNaN() },
        pressureHpa = env?.barometric_pressure?.takeIf { !it.isNaN() },
        voltage = (env?.voltage ?: device?.voltage)?.takeIf { !it.isNaN() },
        currentMilliAmps = env?.current?.takeIf { !it.isNaN() },
        batteryPercent = device?.battery_level,
        uptimeSeconds = device?.uptime_seconds,
        channelUtilizationPercent = device?.channel_utilization?.takeIf { !it.isNaN() },
        airUtilTxPercent = device?.air_util_tx?.takeIf { !it.isNaN() },
    )
}
