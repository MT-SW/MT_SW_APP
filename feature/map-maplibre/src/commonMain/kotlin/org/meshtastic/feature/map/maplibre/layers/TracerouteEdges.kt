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
package org.meshtastic.feature.map.maplibre.layers

import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.Node
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos

/** One hop of a traceroute: from one node to the next, with the SNR the receiving node measured (null = unknown). */
internal data class TracerouteEdge(val from: Position, val to: Position, val snrDb: Float?) {
    /** Compass bearing of the hop in degrees, 0 = north, clockwise. Good enough for an arrow glyph. */
    val bearingDeg: Double
        get() {
            val dLat = to.latitude - from.latitude
            val dLon = (to.longitude - from.longitude) * cos((from.latitude + to.latitude) / 2.0 * RAD_PER_DEG)
            val deg = atan2(dLon, dLat) / RAD_PER_DEG
            return if (deg < 0.0) deg + FULL_CIRCLE else deg
        }

    /** True when the hop runs mostly east-west on a north-up map, so a copy of it is separated vertically. */
    val isMostlyEastWest: Boolean
        get() = abs(kotlin.math.sin(bearingDeg * RAD_PER_DEG)) >= HALF_DIAGONAL

    val midpoint: Position
        get() = Position(longitude = (from.longitude + to.longitude) / 2.0, latitude = (from.latitude + to.latitude) / 2.0)

    /** The arrow pointing the way the packet travelled, e.g. "↗". */
    val arrow: String
        get() = ARROWS[(((bearingDeg + EIGHTH_CIRCLE / 2.0) / EIGHTH_CIRCLE).toInt()) % ARROWS.size]

    /** "↗ -3.5 dB", or "↗ ?" when the radio did not report a value. */
    val label: String
        get() = arrow + " " + (snrDb?.let { NumberFormatter.format(it.toDouble(), 1) + " dB" } ?: "?")
}

/**
 * The hops of [route], in the order the list gives them (hop `i` runs from `route[i]` to `route[i+1]`), paired with the
 * SNR the response carries for them. [snr] is in quarter dB; -128 means unknown, and a list of the wrong length means
 * every hop is unknown, exactly as the traceroute text treats it. Hops touching a node with no position are left out.
 */
internal fun tracerouteEdges(route: List<Int>, snr: List<Int>, nodeLookup: Map<Int, Node>): List<TracerouteEdge> {
    val snrUsable = snr.size == route.size - 1
    val edges = ArrayList<TracerouteEdge>(route.size)
    for (i in 0 until route.size - 1) {
        val a = nodeLookup[route[i]]
        val b = nodeLookup[route[i + 1]]
        if (a == null || b == null || a.validPosition == null || b.validPosition == null) continue
        val raw = if (snrUsable) snr[i] else UNKNOWN_SNR
        edges +=
            TracerouteEdge(
                from = Position(longitude = a.longitude, latitude = a.latitude),
                to = Position(longitude = b.longitude, latitude = b.latitude),
                snrDb = if (raw == UNKNOWN_SNR) null else raw / SNR_UNITS_PER_DB,
            )
    }
    return edges
}

internal const val UNKNOWN_SNR = -128
private const val SNR_UNITS_PER_DB = 4f
private const val RAD_PER_DEG = kotlin.math.PI / 180.0
private const val FULL_CIRCLE = 360.0
private const val EIGHTH_CIRCLE = 45.0
private const val HALF_DIAGONAL = 0.70710678
private val ARROWS = listOf("↑", "↗", "→", "↘", "↓", "↙", "←", "↖")
