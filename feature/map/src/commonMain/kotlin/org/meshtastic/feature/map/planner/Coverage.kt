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
 *
 */
package org.meshtastic.feature.map.planner

import org.meshtastic.feature.map.planner.data.PlannerClutter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class CoverageInput(
    val center: GeoPoint,
    val groundAltM: Double,
    val antennaHeightM: Double,
    val txPowerDbm: Double,
    val antennaGainDbi: Double,
    val feederLossDb: Double,
    val rxAntennaHeightM: Double = 2.0,
    val rxGainDbi: Double = 0.0,
    val frequencyMHz: Double,
    val bandwidthKhz: Double,
    val spreadingFactor: Int,
    val noiseFigureDb: Double = 6.0,
    val kFactor: Double,
    val surfaceRefractivity: Double,
    val extraLossDb: Double = 0.0,
    val maxRangeKm: Double,
    val radials: Int = 72,
    val stepM: Double = 200.0,
    /** Number of ring distances evaluated per radial (default 40). */
    val rangeSteps: Int? = null,
)

/**
 * Coverage prediction on a polar grid. Radial i has bearing 360*i/radials; [ringsM] are the evaluated distances.
 * [marginDb] is margin versus receiver sensitivity (dB).
 */
@Suppress("ArrayInDataClass")
class CoverageResult(
    val radials: Int,
    val ringsM: DoubleArray,
    val marginDb: Array<FloatArray>,
    val center: GeoPoint,
    /** Per radial: distance of the farthest ring with margin >= 0 (0 if none). */
    val maxReachM: DoubleArray,
)

object Coverage {
    private const val MAX_PROFILE_SAMPLES = 600
    private const val DEFAULT_RINGS = 40
    private const val ERROR_MARGIN_DB = -200f

    /**
     * @param clutterAt height (m above ground) of buildings and trees at a point, or null for bare terrain. It is added
     *   to the terrain of the in-between samples only: the centre's neighbourhood ([PlannerClutter.CLEAR_AROUND_ANTENNA_M])
     *   and the receiving point itself stay on the ground.
     */
    fun compute(
        input: CoverageInput,
        elevationAt: (lat: Double, lon: Double) -> Double,
        clutterAt: ((lat: Double, lon: Double) -> Double)? = null,
        onProgress: ((Float) -> Unit)? = null,
    ): CoverageResult {
        val radials = max(1, input.radials)
        val maxRangeM = max(1.0, input.maxRangeKm * 1000.0)
        val n = min(MAX_PROFILE_SAMPLES, max(3, ceil(maxRangeM / max(1.0, input.stepM)).toInt()))
        val step = maxRangeM / n
        val rings = min(max(1, input.rangeSteps ?: DEFAULT_RINGS), n - 1)
        val ringIdx = IntArray(rings) { j -> max(2, ((j + 1).toDouble() * n / rings).roundToInt()) }
        val ringsM = DoubleArray(rings) { ringIdx[it] * step }
        val sens = Sensitivity.dbm(input.bandwidthKhz, input.spreadingFactor, input.noiseFigureDb)
        val eirpMinusLosses =
            input.txPowerDbm + input.antennaGainDbi - input.feederLossDb + input.rxGainDbi - input.extraLossDb

        val margin = Array(radials) { FloatArray(rings) }
        val reach = DoubleArray(radials)
        for (r in 0 until radials) {
            val bearing = 360.0 * r / radials
            val ground = DoubleArray(n + 1)
            val obstacle = DoubleArray(n + 1)
            for (i in 0..n) {
                val p = if (i == 0) input.center else Geodesy.destination(input.center, bearing, i * step)
                ground[i] = elevationAt(p.lat, p.lon)
                if (clutterAt != null && i > 0 && i * step >= PlannerClutter.CLEAR_AROUND_ANTENNA_M) {
                    obstacle[i] = clutterAt(p.lat, p.lon)
                }
            }
            for (j in 0 until rings) {
                val idx = ringIdx[j]
                val sub = DoubleArray(idx + 1) { ground[it] + obstacle[it] }
                sub[0] = ground[0]
                sub[idx] = ground[idx]
                val res = Itm.pointToPoint(
                    elevationsM = sub,
                    stepM = step,
                    txHeightM = input.antennaHeightM,
                    rxHeightM = input.rxAntennaHeightM,
                    frequencyMHz = input.frequencyMHz,
                    surfaceRefractivityNUnits = input.surfaceRefractivity,
                )
                val m = if (res.returnCode <= 1 && !res.lossDb.isNaN()) {
                    (eirpMinusLosses - res.lossDb - sens).toFloat()
                } else {
                    ERROR_MARGIN_DB
                }
                margin[r][j] = m
                if (m >= 0f) reach[r] = ringsM[j]
            }
            onProgress?.invoke((r + 1).toFloat() / radials)
        }
        return CoverageResult(radials, ringsM, margin, input.center, reach)
    }
}

/**
 * Margin (dB) at a geographic point, bilinear between the neighbouring radials and rings; null outside the computed
 * range. Inside the first ring the first ring's value is returned.
 */
fun CoverageResult.marginAt(lat: Double, lon: Double): Float? {
    if (ringsM.isEmpty() || radials <= 0) return null
    val p = GeoPoint(lat, lon)
    val d = Geodesy.distanceM(center, p)
    if (d > ringsM.last()) return null
    val bearing = Geodesy.bearingDeg(center, p)
    val rf = bearing / 360.0 * radials
    val r0 = floor(rf).toInt()
    val tr = (rf - r0).toFloat()
    val ra = ((r0 % radials) + radials) % radials
    val rb = (ra + 1) % radials

    val j1 = ringsM.indexOfFirst { it >= d }.let { if (it < 0) ringsM.size - 1 else it }
    val j0 = max(0, j1 - 1)
    val td = if (j1 == j0 || d <= ringsM[j0]) 0f else ((d - ringsM[j0]) / (ringsM[j1] - ringsM[j0])).toFloat()
    val jj0 = if (d < ringsM[0]) 0 else j0
    val jj1 = if (d < ringsM[0]) 0 else j1
    fun at(r: Int) = marginDb[r][jj0] * (1 - td) + marginDb[r][jj1] * td
    return at(ra) * (1 - tr) + at(rb) * tr
}
