/*
 * Copyright (c) 2025 MT_SW contributors
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
package org.meshtastic.feature.map.planner

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Terrain profile along a great circle with uniform sampling (both end points included). */
@Suppress("ArrayInDataClass")
class PathProfile(val stepM: Double, val groundM: DoubleArray) {
    /** Number of intervals (samples - 1). */
    val intervals: Int get() = groundM.size - 1

    val distanceM: Double get() = stepM * intervals

    fun distanceAt(i: Int): Double = stepM * i

    companion object {
        /**
         * Sample [elevationAt] uniformly between [a] and [b]: step is at least [minStepM], the number of samples at
         * most [maxSamples] (at least 3 samples are always produced).
         */
        fun fromSampler(
            a: GeoPoint,
            b: GeoPoint,
            maxSamples: Int = 400,
            minStepM: Double = 30.0,
            elevationAt: (lat: Double, lon: Double) -> Double,
        ): PathProfile {
            val d = Geodesy.distanceM(a, b)
            val n = min(max(2, maxSamples - 1), max(2, ceil(d / minStepM).toInt()))
            val step = d / n
            val g = DoubleArray(n + 1) { i ->
                val p = when (i) {
                    0 -> a
                    n -> b
                    else -> Geodesy.interpolate(a, b, i.toDouble() / n)
                }
                elevationAt(p.lat, p.lon)
            }
            return PathProfile(step, g)
        }
    }
}

/** Earth bulge (m) at sample [i] relative to the chord between the end points, effective earth radius factor [k]. */
fun PathProfile.bulgeM(i: Int, k: Double): Double {
    val d1 = distanceAt(i)
    val d2 = distanceM - d1
    return d1 * d2 / (2.0 * k * Geodesy.EARTH_RADIUS_M)
}

/** Straight line-of-sight height (m ASL) at sample [i] between antenna heights [startAslM] and [endAslM]. */
fun PathProfile.losHeightM(i: Int, startAslM: Double, endAslM: Double): Double {
    val n = intervals
    if (n <= 0) return startAslM
    return startAslM + (endAslM - startAslM) * i / n
}

/** n-th Fresnel zone radius (m) at distances [d1M] / [d2M] from the two ends. */
fun fresnelRadiusM(n: Int = 1, d1M: Double, d2M: Double, fMHz: Double): Double {
    val total = d1M + d2M
    if (total <= 0.0 || d1M <= 0.0 || d2M <= 0.0) return 0.0
    val lambda = 299.792458 / fMHz
    return sqrt(n * lambda * d1M * d2M / total)
}
