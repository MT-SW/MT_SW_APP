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
package org.meshtastic.core.model

/**
 * A single point-in-time telemetry snapshot for a node, used to render trend charts on the Network Health screen.
 *
 * This is the platform-agnostic external model; see `NodeMetricsHistoryEntity` in `core/database` for the persisted
 * Room representation.
 */
data class NodeMetricsHistoryEntry(
    val num: Int,
    val timestamp: Long,
    val batteryLevel: Int? = null,
    val voltage: Float? = null,
    val snr: Float? = null,
    val rssi: Int? = null,
    val channelUtilization: Float? = null,
    val airUtilTx: Float? = null,
    val temperature: Float? = null,
    val relativeHumidity: Float? = null,
    val barometricPressure: Float? = null,
    val current: Float? = null,
    val noiseFloor: Int? = null,
    val packetsTx: Int? = null,
    val packetsRx: Int? = null,
    val rxDupe: Int? = null,
    val txRelay: Int? = null,
    val packetsRxBad: Int? = null,
    val uptimeSeconds: Int? = null,
    val cpuUsagePercent: Int? = null,
    val heapFreePercent: Float? = null,
    val flashUsedPercent: Float? = null,
    val psramFreePercent: Float? = null,
)
