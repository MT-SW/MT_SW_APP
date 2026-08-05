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
package org.meshtastic.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * A single point-in-time snapshot of a node's key telemetry values, appended on every received telemetry packet.
 *
 * Unlike [NodeEntity], which only holds the latest live value per field, this table accumulates history so trend charts
 * (battery, SNR, channel utilization) can be rendered on the Network Health screen.
 */
@Entity(tableName = "node_metrics_history", indices = [Index(value = ["num", "timestamp"])])
data class NodeMetricsHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val num: Int,
    val timestamp: Long,
    @ColumnInfo(name = "battery_level") val batteryLevel: Int? = null,
    val voltage: Float? = null,
    val snr: Float? = null,
    val rssi: Int? = null,
    @ColumnInfo(name = "channel_utilization") val channelUtilization: Float? = null,
    @ColumnInfo(name = "air_util_tx") val airUtilTx: Float? = null,
    val temperature: Float? = null,
    @ColumnInfo(name = "relative_humidity") val relativeHumidity: Float? = null,
    @ColumnInfo(name = "barometric_pressure") val barometricPressure: Float? = null,
    /** Current in amps, from device_metrics or the ch3 power sensor — whichever the node reported. */
    val current: Float? = null,
    @ColumnInfo(name = "noise_floor") val noiseFloor: Int? = null,
    @ColumnInfo(name = "packets_tx") val packetsTx: Int? = null,
    @ColumnInfo(name = "packets_rx") val packetsRx: Int? = null,
    @ColumnInfo(name = "rx_dupe") val rxDupe: Int? = null,
    @ColumnInfo(name = "tx_relay") val txRelay: Int? = null,
    @ColumnInfo(name = "packets_rx_bad") val packetsRxBad: Int? = null,
    @ColumnInfo(name = "uptime_seconds") val uptimeSeconds: Int? = null,
)

fun NodeMetricsHistoryEntity.asExternalModel() = org.meshtastic.core.model.NodeMetricsHistoryEntry(
    num = num,
    timestamp = timestamp,
    batteryLevel = batteryLevel,
    voltage = voltage,
    snr = snr,
    rssi = rssi,
    channelUtilization = channelUtilization,
    airUtilTx = airUtilTx,
    temperature = temperature,
    relativeHumidity = relativeHumidity,
    barometricPressure = barometricPressure,
    current = current,
    noiseFloor = noiseFloor,
    packetsTx = packetsTx,
    packetsRx = packetsRx,
    rxDupe = rxDupe,
    txRelay = txRelay,
    packetsRxBad = packetsRxBad,
    uptimeSeconds = uptimeSeconds,
)

fun org.meshtastic.core.model.NodeMetricsHistoryEntry.asEntity() = NodeMetricsHistoryEntity(
    num = num,
    timestamp = timestamp,
    batteryLevel = batteryLevel,
    voltage = voltage,
    snr = snr,
    rssi = rssi,
    channelUtilization = channelUtilization,
    airUtilTx = airUtilTx,
    temperature = temperature,
    relativeHumidity = relativeHumidity,
    barometricPressure = barometricPressure,
    current = current,
    noiseFloor = noiseFloor,
    packetsTx = packetsTx,
    packetsRx = packetsRx,
    rxDupe = rxDupe,
    txRelay = txRelay,
    packetsRxBad = packetsRxBad,
    uptimeSeconds = uptimeSeconds,
)
