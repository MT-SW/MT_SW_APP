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

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** One radiosonde / forecast level. */
data class AtmosphereSample(val heightM: Double, val tempC: Double, val pressureHpa: Double, val rhPct: Double)

/** Ducting classification derived from the vertical refractivity gradient. */
enum class DuctingLevel { NORMAL, ELEVATED, POSSIBLE_DUCT }

/**
 * Result of [Atmosphere.analyzeProfile]. [surfaceN] is the refractivity at the lowest sample, [seaLevelN] the same
 * reduced to sea level (ITM input, clamped 250..400), [gradientNPerKm] the vertical gradient dN/dh.
 */
data class AtmosphereAnalysis(
    val surfaceN: Double,
    val seaLevelN: Double,
    val gradientNPerKm: Double,
    val kFactor: Double,
    val level: DuctingLevel,
)

/** Radio-meteorology helpers (ITU-R P.453, P.676, P.838). */
object Atmosphere {
    private const val EARTH_RADIUS_KM = 6371.0

    /** Radio refractivity N (N-units), ITU-R P.453: N = 77.6/T * (P + 4810 e / T). */
    fun refractivityN(tempC: Double, pressureHpa: Double, relHumidityPct: Double): Double {
        val t = tempC + 273.15
        val e = vapourPressureHpa(tempC, relHumidityPct)
        return 77.6 / t * (pressureHpa + 4810.0 * e / t)
    }

    /** Water vapour partial pressure (hPa) from relative humidity (Magnus saturation, P.453). */
    fun vapourPressureHpa(tempC: Double, rhPct: Double): Double {
        val es = 6.1121 * exp((18.678 - tempC / 234.5) * tempC / (257.14 + tempC))
        return rhPct.coerceIn(0.0, 100.0) / 100.0 * es
    }

    /** Gradient (N-units per km) between two levels. */
    fun gradientNPerKm(n1: Double, h1M: Double, n2: Double, h2M: Double): Double {
        val dh = (h2M - h1M) / 1000.0
        if (dh == 0.0) return 0.0
        return (n2 - n1) / dh
    }

    /** Effective earth radius factor k = 1 / (1 + R * dN/dh * 1e-6), R = 6371 km, clamped 0.5..20. */
    fun kFactorFromGradient(dnDhPerKm: Double): Double {
        val denom = 1.0 + EARTH_RADIUS_KM * dnDhPerKm * 1e-6
        if (denom <= 0.05) return 20.0
        return (1.0 / denom).coerceIn(0.5, 20.0)
    }

    /**
     * Reduce station refractivity [stationN] at [stationHeightM] to sea level (N0 = Ns * exp(h_km / 7.35)) and clamp
     * to the 250..400 range ITM accepts.
     */
    fun surfaceRefractivityForItm(stationN: Double, stationHeightM: Double = 0.0): Double =
        (stationN * exp(stationHeightM / 1000.0 / 7.35)).coerceIn(250.0, 400.0)

    /**
     * dN/dh > -40: normal / sub-refraction; -40..-79: normal; -79..-157: super-refraction (ELEVATED);
     * < -157: trapping (POSSIBLE_DUCT).
     */
    fun classify(dnDhPerKm: Double): DuctingLevel = when {
        dnDhPerKm < -157.0 -> DuctingLevel.POSSIBLE_DUCT
        dnDhPerKm < -79.0 -> DuctingLevel.ELEVATED
        else -> DuctingLevel.NORMAL
    }

    /**
     * Analyse a vertical profile. The gradient is taken between the lowest sample and the sample closest to 1 km above
     * it (ignoring samples less than 100 m above the lowest; if there is none, the highest sample above that is used).
     * With fewer than two usable levels the standard atmosphere (-39.2 N/km) is assumed.
     */
    fun analyzeProfile(samples: List<AtmosphereSample>): AtmosphereAnalysis {
        if (samples.isEmpty()) {
            return AtmosphereAnalysis(315.0, 315.0, -39.2, kFactorFromGradient(-39.2), DuctingLevel.NORMAL)
        }
        val low = samples.minByOrNull { it.heightM }!!
        val n1 = refractivityN(low.tempC, low.pressureHpa, low.rhPct)
        val sea = surfaceRefractivityForItm(n1, low.heightM)
        val upper = samples.filter { it.heightM - low.heightM >= 100.0 }
        val grad = if (upper.isEmpty()) {
            -39.2
        } else {
            val hi = upper.minByOrNull { abs(it.heightM - low.heightM - 1000.0) }!!
            gradientNPerKm(n1, low.heightM, refractivityN(hi.tempC, hi.pressureHpa, hi.rhPct), hi.heightM)
        }
        return AtmosphereAnalysis(n1, sea, grad, kFactorFromGradient(grad), classify(grad))
    }

    // ---- gaseous attenuation (simplified ITU-R P.676 Annex 2) ----

    private fun phi(rp: Double, rt: Double, a: Double, b: Double, c: Double, d: Double) =
        rp.pow(a) * rt.pow(b) * exp(c * (1 - rp) + d * (1 - rt))

    // Oxygen specific attenuation (dB/km, sea level standard) around the 60 GHz complex, 54..100 GHz.
    private val oxyF = doubleArrayOf(54.0, 56.0, 58.0, 60.0, 62.0, 64.0, 66.0, 68.0, 70.0, 80.0, 100.0)
    private val oxyA = doubleArrayOf(4.5, 13.0, 15.5, 15.0, 14.0, 9.0, 2.7, 1.1, 0.5, 0.1, 0.1)

    /** Specific gaseous attenuation (oxygen + water vapour) in dB/km; 0 below 1 GHz. */
    fun gasAttenuationDbPerKm(fMHz: Double, tempC: Double, pressureHpa: Double, rhPct: Double): Double {
        if (fMHz < 1000.0) return 0.0
        val f = min(fMHz / 1000.0, 100.0)
        val rp = pressureHpa / 1013.0
        val rt = 288.0 / (273.0 + tempC)
        val e = vapourPressureHpa(tempC, rhPct)
        val rho = 216.7 * e / (273.15 + tempC)

        val oxygen = if (f <= 54.0) {
            val x1 = phi(rp, rt, 0.0717, -1.8132, 0.0156, -1.6515)
            val x2 = phi(rp, rt, 0.5146, -4.6368, -0.1921, -5.7416)
            val x3 = phi(rp, rt, 0.3414, -6.5851, 0.2130, -8.5854)
            (7.2 * rt.pow(2.8) / (f * f + 0.34 * rp * rp * rt.pow(1.6)) +
                0.62 * x3 / ((54.0 - f).pow(1.16 * x1) + 0.83 * x2)) * f * f * rp * rp * 1e-3
        } else {
            var i = 0
            while (i < oxyF.size - 2 && f > oxyF[i + 1]) i++
            val t = ((f - oxyF[i]) / (oxyF[i + 1] - oxyF[i])).coerceIn(0.0, 1.0)
            (oxyA[i] + t * (oxyA[i + 1] - oxyA[i])) * rp * rp
        }

        val eta1 = 0.955 * rp * rt.pow(0.68) + 0.006 * rho
        val g22 = 1.0 + ((f - 22.0) / (f + 22.0)).let { it * it }
        val wTerms =
            3.98 * eta1 * exp(2.23 * (1 - rt)) / ((f - 22.235).pow(2) + 9.42 * eta1 * eta1) * g22 +
                11.96 * eta1 * exp(0.7 * (1 - rt)) / ((f - 183.31).pow(2) + 11.14 * eta1 * eta1) +
                0.081 * eta1 * exp(6.44 * (1 - rt)) / ((f - 321.226).pow(2) + 6.29 * eta1 * eta1)
        val water = wTerms * f * f * rt.pow(2.5) * rho * 1e-4
        return max(0.0, oxygen) + max(0.0, water)
    }

    // ---- rain (ITU-R P.838-3 power law gamma = k R^alpha) ----

    private val rainF = doubleArrayOf(1.0, 2.0, 4.0, 6.0, 7.0, 8.0, 10.0, 12.0, 15.0, 20.0, 25.0, 30.0, 40.0, 50.0)
    private val kH = doubleArrayOf(
        0.0000259, 0.0000847, 0.0001071, 0.0007056, 0.001915, 0.004115, 0.01217, 0.02386, 0.04481, 0.09164,
        0.1571, 0.2403, 0.4431, 0.5911,
    )
    private val aH = doubleArrayOf(
        0.9691, 1.0664, 1.6009, 1.5900, 1.4810, 1.3905, 1.2571, 1.1825, 1.1233, 1.0568, 0.9991, 0.9485, 0.8673,
        0.8355,
    )
    private val kV = doubleArrayOf(
        0.0000308, 0.0000998, 0.0002461, 0.0004878, 0.001425, 0.003450, 0.01129, 0.02455, 0.05008, 0.09611,
        0.1533, 0.2291, 0.4274, 0.5796,
    )
    private val aV = doubleArrayOf(
        0.8592, 0.9490, 1.2476, 1.5728, 1.4745, 1.3797, 1.2156, 1.1216, 1.0440, 0.9847, 0.9491, 0.9129, 0.8421,
        0.8130,
    )

    /** Rain attenuation in dB/km for [rainRateMmH]; 0 below 1 GHz or without rain. Vertical polarization by default. */
    fun rainAttenuationDbPerKm(fMHz: Double, rainRateMmH: Double, vertical: Boolean = true): Double {
        if (fMHz < 1000.0 || rainRateMmH <= 0.0) return 0.0
        val f = (fMHz / 1000.0).coerceIn(rainF.first(), rainF.last())
        var i = 0
        while (i < rainF.size - 2 && f > rainF[i + 1]) i++
        val t = (ln(f) - ln(rainF[i])) / (ln(rainF[i + 1]) - ln(rainF[i]))
        val ks = if (vertical) kV else kH
        val al = if (vertical) aV else aH
        val k = exp(ln(ks[i]) + t * (ln(ks[i + 1]) - ln(ks[i])))
        val a = al[i] + t * (al[i + 1] - al[i])
        return k * rainRateMmH.pow(a)
    }
}
