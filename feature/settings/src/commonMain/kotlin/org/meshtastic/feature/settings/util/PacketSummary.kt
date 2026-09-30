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

/**
 * A structured, source-agnostic summary of a decoded packet's content, built once from the parsed proto at decode time
 * (see [summarizePacketPayload]) and formatted into a localized display string only at the Compose layer (see
 * SnifferPanelComponents.kt's `render()`) -- no user-facing text lives in this non-Composable module. This is the
 * always-visible, single-line counterpart to [decodePayloadFromPacket]'s fuller multi-line dump, which is still what a
 * Sniffer Log card shows once expanded.
 */
sealed interface PacketSummary {
    data class Text(val text: String) : PacketSummary

    /** [summary] plus the timestamp (Unix seconds) the sender wrote into the frame itself (Position / Telemetry). */
    data class Timed(val summary: PacketSummary, val frameTimeSeconds: Int) : PacketSummary

    data class PositionSummary(val latitude: Double?, val longitude: Double?, val altitudeMeters: Int?) : PacketSummary

    data class NodeInfoSummary(val longName: String?, val shortName: String?) : PacketSummary

    data class TelemetrySummary(
        val temperatureCelsius: Float?,
        val humidityPercent: Float?,
        val pressureHpa: Float?,
        val voltage: Float?,
        val currentMilliAmps: Float?,
        val batteryPercent: Int?,
        val uptimeSeconds: Int?,
        val channelUtilizationPercent: Float?,
        val airUtilTxPercent: Float?,
    ) : PacketSummary

    /**
     * The `host_metrics` Telemetry variant (Linux-native / Station G2-class nodes) -- CPU load and memory, not sensor
     * readings, so it gets its own case rather than being folded into [TelemetrySummary]'s device/environment fields.
     */
    data class HostMetricsSummary(
        val uptimeSeconds: Int?,
        val freeMemBytes: Long?,
        val load1: Int?,
        val load5: Int?,
        val load15: Int?,
    ) : PacketSummary

    /**
     * One [org.meshtastic.proto.PowerMetrics] channel with a non-NaN reading -- built only from channels that carry a
     * voltage and/or current, so an unpopulated multi-channel power monitor doesn't pad the summary with empty
     * channels. Channel numbers are 1-based, matching the physical CH1..CH8 labeling on the hardware and the
     * PowerMetrics screen ([org.meshtastic.feature.node.metrics.PowerMetricsCard]).
     */
    data class PowerChannelReading(val channel: Int, val voltage: Float?, val currentMilliAmps: Float?)

    /**
     * The `power_metrics` Telemetry variant -- one or more [PowerChannelReading]s from a multi-channel power monitor.
     */
    data class PowerMetricsSummary(val channels: List<PowerChannelReading>) : PacketSummary

    /**
     * The `local_stats` Telemetry variant -- this node's own runtime/link counters (as opposed to [TelemetrySummary]'s
     * sensor/battery readings from itself or a remote node). Previously fell through to no summary at all, so this
     * frame's content was invisible until the card was expanded.
     */
    data class LocalStatsSummary(
        val uptimeSeconds: Int?,
        val channelUtilizationPercent: Float?,
        val airUtilTxPercent: Float?,
        val numPacketsTx: Int?,
        val numPacketsRx: Int?,
        val numOnlineNodes: Int?,
        val numTotalNodes: Int?,
        val heapFreeBytes: Int?,
        val heapTotalBytes: Int?,
    ) : PacketSummary

    /**
     * Just the neighbor count -- the per-neighbor breakdown (id, short name, SNR) stays in [decodePayloadFromPacket]'s
     * expanded text, already one row per neighbor there. Keeping the always-visible summary to a single count is the
     * fix for a specific complaint about the HA integration this screen's redesign is otherwise modeled on: it crams
     * every neighbor into one long wrapped line, which reads poorly with more than a couple of neighbors.
     */
    data class NeighborCount(val count: Int) : PacketSummary
}

/** Attaches the sender-supplied frame time when the frame carries one (0 means "not set" and is skipped). */
internal fun PacketSummary.withFrameTime(seconds: Int): PacketSummary =
    if (seconds > 0) PacketSummary.Timed(this, seconds) else this
