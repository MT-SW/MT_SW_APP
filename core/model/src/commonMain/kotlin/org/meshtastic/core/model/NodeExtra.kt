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
        fun fromName(name: String): NodeExtraMode = entries.firstOrNull { it.name == name } ?: OFF
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
    val heapFreePercent: Float? = null,
    val flashUsedPercent: Float? = null,
    val psramFreePercent: Float? = null,
) {
    val hasTraffic: Boolean
        get() = packetsTx != null || packetsRx != null || packetsRxBad != null || rxDupe != null || txRelay != null

    val hasResources: Boolean
        get() = cpuPercent != null || heapFreePercent != null || flashUsedPercent != null || psramFreePercent != null
}
