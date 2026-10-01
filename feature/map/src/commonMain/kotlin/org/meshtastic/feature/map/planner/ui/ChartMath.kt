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
package org.meshtastic.feature.map.planner.ui

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Pure helpers of the chart drawing code (unit tested). */
internal object ChartMath {
    private const val NORM_1 = 1.5
    private const val NORM_2 = 3.0
    private const val NORM_5 = 7.0
    private const val MAX_DECIMALS = 4

    /** "Nice" axis tick positions (1, 2, 5 x 10^k steps) inside [[lo], [hi]], about [target] of them. */
    fun niceTicks(lo: Double, hi: Double, target: Int): List<Double> {
        val span = hi - lo
        if (span.isNaN() || span <= 0.0 || target < 1) return listOf(lo)
        val step = niceStep(span / target)
        val out = ArrayList<Double>()
        var t = ceil(lo / step) * step
        var guard = 0
        while (t <= hi + step * 1.0e-9 && guard < 1000) {
            out += if (abs(t) < step * 1.0e-9) 0.0 else t
            t += step
            guard++
        }
        return out
    }

    /** The 1-2-5 step nearest to [raw]. */
    fun niceStep(raw: Double): Double {
        if (raw.isNaN() || raw <= 0.0) return 1.0
        val mag = 10.0.pow(floor(log10(raw)))
        val norm = raw / mag
        val factor =
            when {
                norm < NORM_1 -> 1.0
                norm < NORM_2 -> 2.0
                norm < NORM_5 -> 5.0
                else -> 10.0
            }
        return factor * mag
    }

    /** Decimals needed to print multiples of [step] exactly. */
    fun decimalsFor(step: Double): Int {
        if (step.isNaN() || step <= 0.0 || step >= 1.0) return 0
        return ceil(-log10(step) - 1.0e-9).toInt().coerceIn(0, MAX_DECIMALS)
    }

    /** Index of the sample of ascending [distances] nearest to [x] (0 for an empty array). */
    fun nearestIndex(distances: DoubleArray, x: Double): Int {
        if (distances.isEmpty()) return 0
        var best = 0
        var bestDiff = abs(distances[0] - x)
        for (i in 1 until distances.size) {
            val diff = abs(distances[i] - x)
            if (diff < bestDiff) {
                best = i
                bestDiff = diff
            }
        }
        return best
    }
}
