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

    /** Rotation for an arrowhead glyph that points right ("→") at rest, so it points along the hop. */
    val arrowRotationDeg: Double
        get() = bearingDeg - QUARTER_CIRCLE

    /** True when the text had to be turned half a circle to stay upright, which swaps its left and right. */
    val labelFlipped: Boolean
        get() = arrowRotationDeg > QUARTER_CIRCLE

    /** Rotation for the SNR text so it runs along the hop but is never upside down. */
    val labelRotationDeg: Double
        get() = if (labelFlipped) arrowRotationDeg - HALF_CIRCLE else arrowRotationDeg

    /** Where the drawn line begins: a little past the sending node, so the node's marker stays clear. */
    val lineStart: Position
        get() = along(LINE_START_FRACTION)

    /** Where the drawn line, and its arrowhead, end: a little short of the receiving node. */
    val lineEnd: Position
        get() = along(LINE_END_FRACTION)

    private fun along(fraction: Double) = Position(
        longitude = from.longitude + (to.longitude - from.longitude) * fraction,
        latitude = from.latitude + (to.latitude - from.latitude) * fraction,
    )

    val midpoint: Position
        get() = Position(longitude = (from.longitude + to.longitude) / 2.0, latitude = (from.latitude + to.latitude) / 2.0)

    /** "-3.5 dB", or "?" when the radio did not report a value. */
    val label: String
        get() = snrDb?.let { NumberFormatter.format(it.toDouble(), 1) + " dB" } ?: "?"
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
private const val LINE_START_FRACTION = 0.04
private const val LINE_END_FRACTION = 0.93
private const val HALF_CIRCLE = 180.0
private const val QUARTER_CIRCLE = 90.0
