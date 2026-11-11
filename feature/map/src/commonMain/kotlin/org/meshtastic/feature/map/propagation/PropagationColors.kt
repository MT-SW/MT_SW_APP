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
@file:Suppress("MagicNumber")

package org.meshtastic.feature.map.propagation

import org.meshtastic.feature.map.planner.Atmosphere

/** What the propagation layer paints: the picture's kind, chosen in the layers sheet. */
enum class PropagationKind {
    /** The whole scale, from normal air to a duct. */
    GRADIENT,

    /** Only the places where the signal carries further than usual. */
    ENHANCED,

    /** The effective earth radius factor k. */
    K_FACTOR,

    ;

    companion object {
        fun of(index: Int): PropagationKind = entries.getOrElse(index) { GRADIENT }
    }
}

/** Colours of the propagation layer, as 0xAARRGGBB; the layer's own opacity slider scales them further. */
object PropagationColors {
    private class Stop(val at: Double, val argb: Int)

    /** Stops by how much the refractivity drops (N-units per km, a positive number), ascending. */
    private val gradientStops =
        listOf(
            Stop(0.0, 0x603080E0),
            Stop(40.0, 0xB040C040.toInt()),
            Stop(79.0, 0xFFF0D020.toInt()),
            Stop(118.0, 0xFFF08020.toInt()),
            Stop(157.0, 0xFFE02020.toInt()),
            Stop(300.0, 0xFFB000B0.toInt()),
        )

    private val kStops =
        listOf(
            Stop(1.0, 0x603080E0),
            Stop(1.33, 0xB040C040.toInt()),
            Stop(2.0, 0xFFF0D020.toInt()),
            Stop(3.0, 0xFFF08020.toInt()),
            Stop(5.0, 0xFFE02020.toInt()),
            Stop(10.0, 0xFFB000B0.toInt()),
        )

    /** The colour for a refractivity gradient (N-units per km) in the chosen kind. */
    fun argb(kind: PropagationKind, gradient: Double): Int {
        val drop = (-gradient).coerceIn(0.0, 300.0)
        return when (kind) {
            PropagationKind.GRADIENT -> interpolate(gradientStops, drop)
            PropagationKind.ENHANCED -> if (drop < 79.0) 0 else interpolate(gradientStops, drop)
            PropagationKind.K_FACTOR -> interpolate(kStops, Atmosphere.kFactorFromGradient(gradient))
        }
    }

    @Suppress("ReturnCount")
    private fun interpolate(stops: List<Stop>, x: Double): Int {
        if (x <= stops.first().at) return stops.first().argb
        if (x >= stops.last().at) return stops.last().argb
        val i = stops.indexOfLast { it.at <= x }
        val a = stops[i]
        val b = stops[i + 1]
        val t = ((x - a.at) / (b.at - a.at)).toFloat()
        return lerp(a.argb, b.argb, t)
    }

    private fun lerp(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = (((a ushr shift) and 0xFF) * (1 - t) + ((b ushr shift) and 0xFF) * t).toInt() and 0xFF
        return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
