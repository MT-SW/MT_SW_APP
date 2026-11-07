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

/** How the extra line of data under a node on the node list is shown. */
enum class NodeExtraMode {
    /** No extra line. */
    OFF,

    /** One page at a time; pages change by themselves and by swiping. */
    PAGED,

    /** Everything at once. */
    ALL,

    ;

    companion object {
        // The line is no longer switched on by this mode (that is done by the separate options); OFF now means "all at
        // once".
        fun fromName(name: String): NodeExtraMode = if (name == PAGED.name) PAGED else ALL
    }
}

/**
 * The newest traffic and resource readings of one node, taken from its latest local-stats telemetry. Only the latest
 * reading is kept -- no history.
 */
data class NodeExtraStats(
    val packetsTx: Int? = null,
    val packetsRx: Int? = null,
    val packetsRxBad: Int? = null,
    val rxDupe: Int? = null,
    val txRelay: Int? = null,
    val cpuPercent: Int? = null,
    val heapUsedPercent: Float? = null,
    val flashUsedPercent: Float? = null,
    val psramUsedPercent: Float? = null,
    /** Wi-Fi signal of the node in dBm, exactly as the node sent it in its last packet. */
    val wifiRssi: Int? = null,
    /** Bluetooth signal in dBm, as the node hears the phone connected to it, exactly as the node sent it. */
    val bleRssi: Int? = null,
    /** Noise floor in dBm as the node reported it (before any LNA correction). */
    val noiseFloor: Int? = null,
) {
    val hasTraffic: Boolean
        get() = packetsTx != null || packetsRx != null || packetsRxBad != null || rxDupe != null || txRelay != null

    val hasResources: Boolean
        get() =
            cpuPercent != null ||
                heapUsedPercent != null ||
                flashUsedPercent != null ||
                psramUsedPercent != null ||
                wifiRssi != null ||
                bleRssi != null

    /** The node reported the signal of one of its links (WiFi or Bluetooth). */
    val hasLinkSignal: Boolean
        get() = wifiRssi != null || bleRssi != null

    /** Every reading a node can send is present, so older packets cannot add anything. */
    val isComplete: Boolean
        get() =
            packetsTx != null &&
                cpuPercent != null &&
                heapUsedPercent != null &&
                flashUsedPercent != null &&
                psramUsedPercent != null &&
                hasLinkSignal &&
                noiseFloor != null

    /** This reading, with every gap filled from an [older] one. */
    fun orElse(older: NodeExtraStats): NodeExtraStats = NodeExtraStats(
        packetsTx = packetsTx ?: older.packetsTx,
        packetsRx = packetsRx ?: older.packetsRx,
        packetsRxBad = packetsRxBad ?: older.packetsRxBad,
        rxDupe = rxDupe ?: older.rxDupe,
        txRelay = txRelay ?: older.txRelay,
        cpuPercent = cpuPercent ?: older.cpuPercent,
        heapUsedPercent = heapUsedPercent ?: older.heapUsedPercent,
        flashUsedPercent = flashUsedPercent ?: older.flashUsedPercent,
        psramUsedPercent = psramUsedPercent ?: older.psramUsedPercent,
        // WiFi and Bluetooth replace each other: the newest reading of either one wins, and the older ones are kept
        // only
        // while no newer packet reported any of the two.
        wifiRssi = if (hasLinkSignal) wifiRssi else older.wifiRssi,
        bleRssi = if (hasLinkSignal) bleRssi else older.bleRssi,
        noiseFloor = noiseFloor ?: older.noiseFloor,
    )
}
