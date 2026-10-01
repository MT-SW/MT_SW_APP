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

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/** One attenuation sample: [dbPer100m] decibels per 100 metres at [fMHz]. */
data class AttenPoint(val fMHz: Double, val dbPer100m: Double)

/**
 * A coaxial cable with manufacturer attenuation data.
 *
 * Unit convention: datasheet values given per 100 ft are converted to dB/100 m by
 * `dB/100m = dB/100ft * 100 / 30.48` (see [CableDb]); values already published per 100 m or per m are
 * stored verbatim (per m values multiplied by 100).
 *
 * [approximate] is true when the data is second-hand, rounded, 75-ohm/typical, sparse, or does not reach 2.4 GHz
 * (so the 2.4 GHz figure is extrapolated).
 */
data class CableType(
    val id: String,
    val name: String,
    val diameterMm: Double?,
    val points: List<AttenPoint>,
    val approximate: Boolean,
    val source: String,
    val velocityFactor: Double? = null,
) {
    /**
     * Attenuation in dB per metre at [fMHz] (clamped to 20 MHz..20 GHz).
     *
     * - Inside the datasheet range: log-log (power law) interpolation between neighbouring points, so the
     *   datasheet values are reproduced exactly and the result is monotonic.
     * - Below the first point: pure sqrt(f) scaling (skin-effect dominated).
     * - Above the last point: fit `a*sqrt(f) + b*f` (skin + dielectric loss) through the last two points when
     *   both coefficients are non-negative, bounded between sqrt(f) scaling (lower) and linear-in-f scaling
     *   (upper); otherwise plain sqrt(f) scaling.
     */
    fun lossDbPerM(fMHz: Double): Double {
        val pts = points.sortedBy { it.fMHz }
        if (pts.isEmpty()) return 0.0
        val f = (if (fMHz.isNaN()) MIN_F else fMHz).coerceIn(MIN_F, MAX_F)
        val first = pts.first()
        val last = pts.last()
        val per100 =
            when {
                f <= first.fMHz -> first.dbPer100m * sqrt(f / first.fMHz)
                f >= last.fMHz -> extrapolateUp(pts, f)
                else -> {
                    var i = 0
                    while (i < pts.size - 2 && pts[i + 1].fMHz <= f) i++
                    val p0 = pts[i]
                    val p1 = pts[i + 1]
                    val t = ln(f / p0.fMHz) / ln(p1.fMHz / p0.fMHz)
                    exp(ln(p0.dbPer100m) + t * (ln(p1.dbPer100m) - ln(p0.dbPer100m)))
                }
            }
        return per100 / 100.0
    }

    private fun extrapolateUp(pts: List<AttenPoint>, f: Double): Double {
        val last = pts.last()
        val sqrtScaled = last.dbPer100m * sqrt(f / last.fMHz)
        if (f == last.fMHz || pts.size < 2) return sqrtScaled
        val prev = pts[pts.size - 2]
        val s1 = sqrt(prev.fMHz)
        val s2 = sqrt(last.fMHz)
        val det = s1 * last.fMHz - s2 * prev.fMHz
        if (det == 0.0) return sqrtScaled
        val a = (prev.dbPer100m * last.fMHz - last.dbPer100m * prev.fMHz) / det
        val b = (s1 * last.dbPer100m - s2 * prev.dbPer100m) / det
        if (a < 0.0 || b < 0.0) return sqrtScaled
        val fit = a * sqrt(f) + b * f
        val linear = last.dbPer100m * f / last.fMHz
        return fit.coerceIn(sqrtScaled, linear)
    }

    companion object {
        const val MIN_F = 20.0
        const val MAX_F = 20000.0
    }
}

/**
 * RF connector with typical insertion loss at 900 MHz and 2.4 GHz. Values are typical figures
 * (resistive plus mismatch loss of a mated pair); [approximate] marks the ones that are not well established.
 */
data class ConnectorType(
    val id: String,
    val name: String,
    val lossDb900: Double,
    val lossDb2400: Double,
    val approximate: Boolean,
) {
    /** Loss in dB at [fMHz]: power-law through the two anchor points, exponent limited to 0..1.5; never negative. */
    fun lossDb(fMHz: Double): Double {
        val f = (if (fMHz.isNaN()) CableType.MIN_F else fMHz).coerceIn(CableType.MIN_F, CableType.MAX_F)
        val l9 = lossDb900.coerceAtLeast(0.0)
        val l24 = lossDb2400.coerceAtLeast(0.0)
        if (l9 <= 0.0 || l24 <= 0.0) {
            // Degenerate anchors: linear in f through the non-zero one (or zero).
            val k = if (l24 > 0.0) l24 / 2400.0 else l9 / 900.0
            return (k * f).coerceAtLeast(0.0)
        }
        val exponent = (ln(l24 / l9) / ln(2400.0 / 900.0)).coerceIn(0.0, 1.5)
        return (l9 * exp(exponent * ln(f / 900.0))).coerceAtLeast(0.0)
    }
}

/**
 * Cable and connector database. Cable attenuation comes from manufacturer datasheets (see CABLE_SOURCES.md).
 */
object CableDb {
    /** dB/100 ft -> dB/100 m: multiply by 100 / 30.48. */
    private const val DB100FT_TO_DB100M = 100.0 / 30.48

    private fun ft(fMHz: Double, dbPer100ft: Double) = AttenPoint(fMHz, dbPer100ft * DB100FT_TO_DB100M)

    private fun m(fMHz: Double, dbPer100m: Double) = AttenPoint(fMHz, dbPer100m)

    private fun pts(vararg fv: Double): List<AttenPoint> = fv.toList().chunked(2).map { AttenPoint(it[0], it[1]) }

    /** Huber+Suhner RG213 datasheet formula: dB/m = a*sqrt(fGHz) + b*fGHz, a=0.1679, b=0.0585 (to 1 GHz). */
    private fun rg213Points(): List<AttenPoint> =
        listOf(50.0, 100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0, 1000.0).map {
            val g = it / 1000.0
            AttenPoint(it, (0.1679 * sqrt(g) + 0.0585 * g) * 100.0)
        }

    val cables: List<CableType> =
        listOf(
            CableType(
                "rg174", "RG174", 2.55,
                pts(100.0, 28.4, 200.0, 40.4, 300.0, 49.7, 400.0, 57.5, 600.0, 70.8, 800.0, 82.1, 1000.0, 92.2),
                approximate = true, source = "Huber+Suhner RG_174/U data sheet (data to 1 GHz, 2.4 GHz extrapolated)",
                velocityFactor = 0.66,
            ),
            CableType(
                "rg178", "RG178", 1.8,
                pts(
                    200.0, 66.7, 400.0, 96.3, 600.0, 119.9, 800.0, 140.3, 1000.0, 158.7, 1400.0, 191.5,
                    2000.0, 234.6, 3000.0, 296.8,
                ),
                approximate = false, source = "Huber+Suhner RG_178_B/U data sheet", velocityFactor = 0.69,
            ),
            CableType(
                "rg316", "RG316", 2.5,
                pts(
                    200.0, 36.5, 400.0, 52.8, 600.0, 65.7, 800.0, 76.9, 1000.0, 87.0, 1400.0, 105.0,
                    2000.0, 128.7, 3000.0, 163.0,
                ),
                approximate = false, source = "Huber+Suhner RG_316/U data sheet", velocityFactor = 0.69,
            ),
            CableType(
                "rg58", "RG58", 4.95,
                pts(
                    50.0, 9.0, 100.0, 13.0, 200.0, 20.0, 300.0, 26.0, 400.0, 31.0, 500.0, 36.0, 700.0, 46.0,
                    1000.0, 58.0,
                ),
                approximate = true,
                source = "Huber+Suhner RG_58_C/U data sheet (rounded to 0.01 dB/m, data to 1 GHz, 2.4 GHz extrapolated)",
                velocityFactor = 0.66,
            ),
            CableType(
                "rg59", "RG59 (75 ohm)", 6.1,
                listOf(
                    ft(100.0, 3.4), ft(200.0, 4.9), ft(400.0, 7.0), ft(700.0, 9.7), ft(900.0, 11.1), ft(1000.0, 12.0),
                ),
                approximate = true,
                source = "Belden 8241 (RG-59/U type, 75 ohm) data sheet, per 100 ft converted (data to 1 GHz)",
                velocityFactor = 0.66,
            ),
            CableType(
                "rg8x", "RG8X", 6.15,
                listOf(
                    ft(50.0, 2.1), ft(100.0, 3.1), ft(200.0, 4.5), ft(400.0, 6.6), ft(700.0, 9.1), ft(900.0, 10.7),
                    ft(1000.0, 11.2),
                ),
                approximate = true,
                source = "Belden 9258 (RG-8X type) data sheet, per 100 ft converted (data to 1 GHz)",
                velocityFactor = 0.82,
            ),
            CableType(
                "rg213", "RG213", 10.3, rg213Points(),
                approximate = true,
                source = "Huber+Suhner RG_213/U data sheet formula a*sqrt(f)+b*f (valid to 1 GHz, 2.4 GHz extrapolated)",
                velocityFactor = 0.66,
            ),
            CableType(
                "lmr100", "LMR-100A", 2.79,
                pts(
                    30.0, 12.9, 50.0, 16.7, 150.0, 29.4, 220.0, 35.8, 450.0, 51.9, 900.0, 74.9, 1500.0, 98.7,
                    1800.0, 109.0, 2000.0, 115.5, 2500.0, 130.6, 5800.0, 210.3,
                ),
                approximate = false, source = "Times Microwave LMR-100A data sheet", velocityFactor = 0.66,
            ),
            CableType(
                "lmr195", "LMR-195", 4.95,
                pts(
                    30.0, 6.5, 50.0, 8.4, 150.0, 14.6, 220.0, 17.7, 450.0, 25.5, 900.0, 36.5, 1500.0, 47.7,
                    1800.0, 52.5, 2000.0, 55.4, 2500.0, 62.4, 5800.0, 98.1,
                ),
                approximate = false, source = "Times Microwave LMR-195 data sheet", velocityFactor = 0.80,
            ),
            CableType(
                "lmr200", "LMR-200", 4.95,
                pts(
                    30.0, 5.8, 50.0, 7.5, 150.0, 13.1, 220.0, 15.9, 450.0, 22.8, 900.0, 32.6, 1500.0, 42.4,
                    1800.0, 46.6, 2000.0, 49.3, 2500.0, 55.4, 5800.0, 86.5, 8000.0, 102.8,
                ),
                approximate = false, source = "Times Microwave LMR-200 data sheet", velocityFactor = 0.83,
            ),
            CableType(
                "lmr240", "LMR-240", 6.10,
                pts(
                    30.0, 4.4, 50.0, 5.7, 150.0, 9.9, 220.0, 12.0, 450.0, 17.3, 900.0, 24.8, 1500.0, 32.4,
                    1800.0, 35.6, 2000.0, 37.7, 2500.0, 42.4, 5800.0, 66.8, 8000.0, 79.7,
                ),
                approximate = false, source = "Times Microwave LMR-240 data sheet", velocityFactor = 0.83,
            ),
            CableType(
                "lmr400", "LMR-400", 10.29,
                pts(
                    50.0, 2.95, 150.0, 4.92, 220.0, 6.23, 450.0, 8.86, 900.0, 12.8, 1500.0, 16.73, 1800.0, 18.7,
                    2000.0, 19.69, 2500.0, 22.31, 6000.0, 35.43,
                ),
                approximate = false, source = "Times Microwave LMR-400 data sheet", velocityFactor = 0.85,
            ),
            CableType(
                "lmr600", "LMR-600", 14.99,
                pts(
                    30.0, 1.4, 50.0, 1.8, 150.0, 3.2, 220.0, 3.9, 450.0, 5.6, 900.0, 8.2, 1500.0, 10.9,
                    1800.0, 12.1, 2000.0, 12.8, 2500.0, 14.5, 5800.0, 23.8,
                ),
                approximate = false, source = "Times Microwave LMR-600 data sheet", velocityFactor = 0.87,
            ),
            CableType(
                "h155", "H155", 5.4,
                pts(
                    50.0, 6.5, 100.0, 9.3, 230.0, 14.2, 300.0, 16.3, 400.0, 19.0, 470.0, 20.7, 860.0, 28.5,
                    1000.0, 30.9, 1350.0, 36.4, 1750.0, 41.9, 2050.0, 45.8,
                ),
                approximate = false, source = "Belden H155 PE data sheet (data to 2.05 GHz)", velocityFactor = 0.81,
            ),
            CableType(
                "h1000", "H1000", 10.3,
                pts(
                    50.0, 3.0, 100.0, 4.3, 230.0, 6.8, 300.0, 7.7, 400.0, 9.1, 470.0, 10.0, 860.0, 14.1,
                    1000.0, 15.3, 1350.0, 18.3, 1750.0, 21.3, 2050.0, 23.4,
                ),
                approximate = false, source = "Belden H1000 PE data sheet (data to 2.05 GHz)", velocityFactor = 0.83,
            ),
            CableType(
                "aircell5", "Aircell 5", 5.0,
                pts(
                    50.0, 6.61, 100.0, 9.40, 144.0, 11.33, 200.0, 13.41, 300.0, 16.53, 432.0, 19.99, 500.0, 21.57,
                    800.0, 27.62, 1000.0, 31.09, 1296.0, 35.71, 1500.0, 38.63, 1800.0, 42.63, 2000.0, 45.14,
                    2400.0, 49.87, 3000.0, 56.39, 4000.0, 66.19, 5000.0, 75.05, 10000.0, 112.0,
                ),
                approximate = false, source = "SSB-Electronic Aircell 5 data sheet", velocityFactor = 0.82,
            ),
            CableType(
                "aircell7", "Aircell 7", 7.3,
                pts(
                    50.0, 4.52, 100.0, 6.28, 144.0, 7.6, 200.0, 9.04, 300.0, 11.2, 432.0, 13.6, 500.0, 14.72,
                    800.0, 19.0, 1000.0, 21.52, 1296.0, 24.84, 1500.0, 27.08, 1800.0, 30.0, 2000.0, 31.88,
                    2400.0, 35.6, 3000.0, 40.88, 4000.0, 49.12, 5000.0, 57.04, 6000.0, 64.9,
                ),
                approximate = false, source = "SSB-Electronic Aircell 7 data sheet", velocityFactor = 0.83,
            ),
            CableType(
                "ecoflex10", "Ecoflex 10", 10.2,
                pts(
                    50.0, 2.8, 100.0, 4.0, 144.0, 4.9, 200.0, 5.8, 300.0, 7.3, 432.0, 8.9, 500.0, 9.6, 800.0, 12.5,
                    1000.0, 14.2, 1296.0, 16.5, 1500.0, 17.9, 1800.0, 19.9, 2000.0, 21.2, 2400.0, 23.6, 3000.0, 27.0,
                    4000.0, 32.2, 5000.0, 37.0, 6000.0, 41.5,
                ),
                approximate = false, source = "SSB-Electronic Ecoflex 10 data sheet", velocityFactor = 0.85,
            ),
            CableType(
                "ecoflex15", "Ecoflex 15", 14.6,
                pts(
                    50.0, 1.96, 100.0, 2.81, 144.0, 3.4, 200.0, 4.05, 300.0, 5.0, 432.0, 6.1, 500.0, 6.7, 800.0, 8.6,
                    1000.0, 9.8, 1296.0, 11.4, 1500.0, 12.4, 1800.0, 13.8, 2000.0, 14.7, 2400.0, 16.3, 3000.0, 18.7,
                    4000.0, 22.3, 5000.0, 25.7, 6000.0, 28.8,
                ),
                approximate = false, source = "SSB-Electronic Ecoflex 15 data sheet", velocityFactor = 0.86,
            ),
            CableType(
                "semirigid085", "Semi-rigid .085", 2.2,
                listOf(ft(1000.0, 22.0), ft(10000.0, 80.0), ft(20000.0, 120.0)),
                approximate = true,
                source = "RG405 type .086 semi-rigid (Pasternack/Fairview data sheet), only 3 points: 1, 10, 20 GHz",
                velocityFactor = 0.70,
            ),
            CableType(
                "semirigid141", "Semi-rigid .141", 3.58,
                listOf(ft(1000.0, 12.0), ft(10000.0, 45.0), ft(20000.0, 70.0)),
                approximate = true,
                source = "Fairview Microwave FM-SR141CU (RG402 type .141) data sheet, only 3 points: 1, 10, 20 GHz",
                velocityFactor = 0.70,
            ),
            CableType(
                "ldf4_50a", "LDF4-50A 1/2\" Heliax", 15.875,
                pts(
                    50.0, 1.521, 100.0, 2.169, 500.0, 5.021, 1000.0, 7.284, 2000.0, 10.666, 5000.0, 18.01,
                    8800.0, 25.244,
                ),
                approximate = false, source = "CommScope/Andrew HELIAX LDF4-50A product specification",
                velocityFactor = 0.88,
            ),
        )

    val connectors: List<ConnectorType> =
        listOf(
            ConnectorType("ufl", "U.FL", 0.10, 0.20, true),
            ConnectorType("mhf4", "MHF4", 0.12, 0.25, true),
            ConnectorType("mmcx", "MMCX", 0.05, 0.10, true),
            ConnectorType("sma", "SMA", 0.03, 0.05, false),
            ConnectorType("rpsma", "RP-SMA", 0.03, 0.05, false),
            ConnectorType("n", "N", 0.03, 0.05, false),
            ConnectorType("bnc", "BNC", 0.05, 0.10, false),
            ConnectorType("tnc", "TNC", 0.04, 0.08, false),
            ConnectorType("uhf", "UHF (PL-259)", 0.10, 0.40, true),
            ConnectorType("din716", "7/16 DIN", 0.02, 0.04, false),
            ConnectorType("adapter", "Adapter", 0.10, 0.20, true),
        )

    fun cable(id: String): CableType? = cables.firstOrNull { it.id == id }

    fun connector(id: String): ConnectorType? = connectors.firstOrNull { it.id == id }
}
