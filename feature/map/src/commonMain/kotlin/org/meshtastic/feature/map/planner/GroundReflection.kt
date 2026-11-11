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

package org.meshtastic.feature.map.planner

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The reflection from the ground under a link: the wave that reaches the receiver straight and the one that bounces off
 * the ground arrive almost together and add up or cancel each other, depending on the heights, the distance and the
 * wavelength (the "two-ray" model). Over open flat ground it can add a few dB or take away much more; the terrain model
 * of the planner only partly covers this, so it is an option.
 */
object GroundReflection {
    /** Typical strength of the ground reflection at a flat grazing angle. */
    const val DEFAULT_COEFFICIENT = 0.7
    private const val LIGHT_M_MHZ = 299.792458
    private const val MIN_DISTANCE_M = 10.0

    /**
     * Change of the received level in dB (positive: stronger) at [distM] between antennas [h1M] and [h2M] above the
     * reflecting ground, for [frequencyMHz]. The distance is limited so a very short link stays in range.
     */
    fun gainDb(distM: Double, h1M: Double, h2M: Double, frequencyMHz: Double, coefficient: Double): Double {
        if (frequencyMHz <= 0.0) return 0.0
        val lambda = LIGHT_M_MHZ / frequencyMHz
        val d = max(MIN_DISTANCE_M, distM)
        val delta = 2.0 * max(0.0, h1M) * max(0.0, h2M) / d
        val phase = 2.0 * PI * delta / lambda
        val field = sqrt(max(1.0e-6, 1.0 + coefficient * coefficient - 2.0 * coefficient * cos(phase)))
        return 20.0 * log10(field)
    }
}
