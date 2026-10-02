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
 * Represents a traceroute result with forward and return routes as ordered lists of node nums.
 *
 * @property requestId The mesh packet request ID that initiated this traceroute.
 * @property forwardRoute Ordered node nums along the path towards the destination.
 * @property returnRoute Ordered node nums along the return path back to the originator.
 */
data class TracerouteOverlay(
    val requestId: Int,
    val forwardRoute: List<Int> = emptyList(),
    val returnRoute: List<Int> = emptyList(),
    /** SNR of each forward hop in quarter dB, as the radio reports it (-128 = unknown); hop i is route[i] to route[i+1]. */
    val forwardSnr: List<Int> = emptyList(),
    /** Same as [forwardSnr] for the return route. */
    val returnSnr: List<Int> = emptyList(),
) {
    /** All unique node nums involved in either route direction. */
    val relatedNodeNums: Set<Int> = (forwardRoute + returnRoute).toSet()

    /** True if at least one route direction contains nodes. */
    val hasRoutes: Boolean
        get() = forwardRoute.isNotEmpty() || returnRoute.isNotEmpty()
}
