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
 * Derived from the NTIA/ITS Irregular Terrain Model (public domain,
 * https://github.com/NTIA/itm); modified: ported from C++ to Kotlin.
 *
 * NTIA/ITS disclaimer (short): the original software is provided "as is" by the
 * U.S. Government, which makes no warranty of any kind and assumes no liability
 * for its use. It is not subject to copyright protection in the United States.
 */
package org.meshtastic.feature.map.planner

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Antenna polarization. */
enum class ItmPolarization(val code: Int) {
    HORIZONTAL(0),
    VERTICAL(1),
}

/** Dominant propagation mode reported by the model. */
enum class ItmMode { NOT_SET, LINE_OF_SIGHT, DIFFRACTION, TROPOSCATTER }

/**
 * Result of a point-to-point ITM prediction.
 *
 * [returnCode]: 0 = success, 1 = success with warnings ([warnings] bit flags), other values are errors
 * (then [lossDb] is NaN).
 */
@Suppress("ArrayInDataClass")
data class ItmResult(
    val returnCode: Int,
    val lossDb: Double,
    val mode: ItmMode,
    val warnings: Int,
    val freeSpaceLossDb: Double,
    val referenceAttenuationDb: Double,
    val terrainIrregularityM: Double,
    val effectiveHeightsM: DoubleArray,
    val horizonDistancesM: DoubleArray,
    val horizonAnglesRad: DoubleArray,
)

/** Longley-Rice / ITM point-to-point model (time/location/situation variability). */
object Itm {
    // Error codes
    private const val ERROR_TX_TERMINAL_HEIGHT = 1000
    private const val ERROR_RX_TERMINAL_HEIGHT = 1001
    private const val ERROR_INVALID_RADIO_CLIMATE = 1002
    private const val ERROR_INVALID_TIME = 1003
    private const val ERROR_INVALID_LOCATION = 1004
    private const val ERROR_INVALID_SITUATION = 1005
    private const val ERROR_REFRACTIVITY = 1008
    private const val ERROR_FREQUENCY = 1009
    private const val ERROR_POLARIZATION = 1010
    private const val ERROR_EPSILON = 1011
    private const val ERROR_SIGMA = 1012
    private const val ERROR_GROUND_IMPEDANCE = 1013
    private const val ERROR_MDVAR = 1014
    private const val ERROR_EFFECTIVE_EARTH = 1016
    private const val ERROR_PATH_DISTANCE = 1017
    private const val ERROR_SURFACE_REFRACTIVITY_SMALL = 1021
    private const val ERROR_SURFACE_REFRACTIVITY_LARGE = 1022

    // Warning flags
    private const val WARN_TX_TERMINAL_HEIGHT = 0x0001
    private const val WARN_RX_TERMINAL_HEIGHT = 0x0002
    private const val WARN_FREQUENCY = 0x0004
    private const val WARN_PATH_DISTANCE_TOO_BIG_1 = 0x0008
    private const val WARN_PATH_DISTANCE_TOO_BIG_2 = 0x0010
    private const val WARN_PATH_DISTANCE_TOO_SMALL_1 = 0x0020
    private const val WARN_PATH_DISTANCE_TOO_SMALL_2 = 0x0040
    private const val WARN_TX_HORIZON_ANGLE = 0x0080
    private const val WARN_RX_HORIZON_ANGLE = 0x0100
    private const val WARN_TX_HORIZON_DISTANCE_1 = 0x0200
    private const val WARN_RX_HORIZON_DISTANCE_1 = 0x0400
    private const val WARN_TX_HORIZON_DISTANCE_2 = 0x0800
    private const val WARN_RX_HORIZON_DISTANCE_2 = 0x1000
    private const val WARN_EXTREME_VARIABILITIES = 0x2000
    private const val WARN_SURFACE_REFRACTIVITY = 0x4000

    private const val A_0_METER = 6370e3
    private const val A_9000_METER = 9000e3
    private const val THIRD = 1.0 / 3.0
    private val SQRT2 = sqrt(2.0)

    private const val MODE_P2P = 0

    /** Mutable warning-flag holder (stands in for the C++ `long *warnings`). */
    private class Warn(var flags: Int = 0)

    // ---- minimal complex arithmetic (no std::complex in Kotlin) ----

    private class Cx(val re: Double, val im: Double)

    private fun cMinus(a: Cx, b: Cx) = Cx(a.re - b.re, a.im - b.im)

    private fun cPlus(a: Cx, b: Cx) = Cx(a.re + b.re, a.im + b.im)

    private fun cMul(a: Cx, b: Cx) = Cx(a.re * b.re - a.im * b.im, a.re * b.im + a.im * b.re)

    private fun cScale(a: Cx, s: Double) = Cx(a.re * s, a.im * s)

    private fun cDiv(a: Cx, b: Cx): Cx {
        val d = b.re * b.re + b.im * b.im
        return Cx((a.re * b.re + a.im * b.im) / d, (a.im * b.re - a.re * b.im) / d)
    }

    private fun cAbs(a: Cx) = hypot(a.re, a.im)

    /** Principal square root. */
    private fun cSqrt(a: Cx): Cx {
        if (a.re == 0.0 && a.im == 0.0) return Cx(0.0, a.im)
        val m = hypot(a.re, a.im)
        val r = sqrt((m + abs(a.re)) / 2.0)
        return if (a.re >= 0.0) {
            Cx(r, a.im / (2.0 * r))
        } else {
            val im = a.im.let { if (it < 0.0 || (it == 0.0 && 1.0 / it < 0.0)) -r else r }
            Cx(abs(a.im) / (2.0 * r), im)
        }
    }

    private fun fdim(x: Double, y: Double): Double = if (x > y) x - y else 0.0

    // ---- public entry ----

    /**
     * ITM point-to-point prediction with time/location/situation variability (mirrors ITM_P2P_TLS_Ex).
     *
     * @param elevationsM terrain samples including both end points (n+1 samples, evenly spaced by [stepM])
     * @param timePct time percentage, 0 < x < 100 (passed through to the reference algorithm unchanged)
     * @param locationPct location percentage, 0 < x < 100
     * @param situationPct situation percentage, 0 < x < 100
     */
    fun pointToPoint(
        elevationsM: DoubleArray,
        stepM: Double,
        txHeightM: Double,
        rxHeightM: Double,
        frequencyMHz: Double,
        surfaceRefractivityNUnits: Double = 301.0,
        climate: Int = 5,
        polarization: ItmPolarization = ItmPolarization.VERTICAL,
        epsilon: Double = 15.0,
        sigma: Double = 0.005,
        mdvar: Int = 12,
        timePct: Double = 50.0,
        locationPct: Double = 50.0,
        situationPct: Double = 50.0,
    ): ItmResult {
        val warn = Warn()

        fun error(code: Int) = ItmResult(
            code, Double.NaN, ItmMode.NOT_SET, warn.flags, Double.NaN, Double.NaN, Double.NaN,
            doubleArrayOf(Double.NaN, Double.NaN), doubleArrayOf(Double.NaN, Double.NaN),
            doubleArrayOf(Double.NaN, Double.NaN),
        )

        var rtn = validateInputs(
            txHeightM, rxHeightM, climate, timePct, locationPct, situationPct, surfaceRefractivityNUnits,
            frequencyMHz, polarization.code, epsilon, sigma, mdvar, warn,
        )
        if (rtn != 0) return error(rtn)

        // not part of the reference implementation: guard against unusable profiles
        if (elevationsM.size < 2 || !(stepM > 0.0)) return error(ERROR_PATH_DISTANCE)

        val np = elevationsM.size - 1
        // PFL layout: [0] = np, [1] = step, [2..] = elevations
        val pfl = DoubleArray(np + 3)
        pfl[0] = np.toDouble()
        pfl[1] = stepM
        for (i in 0..np) pfl[i + 2] = elevationsM[i]

        // average path height, ignoring first and last 10%
        val p10 = (0.1 * np).toInt()
        var hSys = 0.0
        for (i in p10..(np - p10)) hSys += pfl[i + 2]
        hSys /= (np - 2 * p10 + 1)

        // InitializePointToPoint
        val gammaA = 157e-9
        val nS = if (hSys == 0.0) surfaceRefractivityNUnits else surfaceRefractivityNUnits * exp(-hSys / 9460.0)
        val gammaE = gammaA * (1.0 - 0.04665 * exp(nS / 179.3))
        val epR = Cx(epsilon, 18000 * sigma / frequencyMHz)
        var zG = cSqrt(cMinus(epR, Cx(1.0, 0.0)))
        if (polarization == ItmPolarization.VERTICAL) zG = cDiv(zG, epR)

        val h = doubleArrayOf(txHeightM, rxHeightM)
        val thetaHzn = DoubleArray(2)
        val dHzn = DoubleArray(2)
        val hE = DoubleArray(2)
        val dh = DoubleArray(1)
        val dist = DoubleArray(1)
        quickPfl(pfl, gammaE, h, thetaHzn, dHzn, hE, dh, dist)
        val deltaH = dh[0]
        val d = dist[0]

        val aRef = DoubleArray(1)
        val propMode = IntArray(1)
        rtn = longleyRice(
            thetaHzn, frequencyMHz, zG, dHzn, hE, gammaE, nS, deltaH, h, d, MODE_P2P, aRef, warn, propMode,
        )
        if (rtn != 0) return error(rtn)

        val aFs = freeSpaceLoss(d, frequencyMHz)
        val loss = variability(
            timePct, locationPct, situationPct, hE, deltaH, frequencyMHz, d, aRef[0], climate, mdvar, warn,
        ) + aFs

        val mode = when (propMode[0]) {
            1 -> ItmMode.LINE_OF_SIGHT
            2 -> ItmMode.DIFFRACTION
            3 -> ItmMode.TROPOSCATTER
            else -> ItmMode.NOT_SET
        }
        return ItmResult(
            returnCode = if (warn.flags != 0) 1 else 0,
            lossDb = loss,
            mode = mode,
            warnings = warn.flags,
            freeSpaceLossDb = aFs,
            referenceAttenuationDb = aRef[0],
            terrainIrregularityM = deltaH,
            effectiveHeightsM = hE.copyOf(),
            horizonDistancesM = dHzn.copyOf(),
            horizonAnglesRad = thetaHzn.copyOf(),
        )
    }

    // ---- ValidateInputs ----

    private fun validateInputs(
        hTx: Double, hRx: Double, climate: Int, time: Double, location: Double, situation: Double,
        n0: Double, fMhz: Double, pol: Int, epsilon: Double, sigma: Double, mdvar: Int, warn: Warn,
    ): Int {
        if (hTx < 1.0 || hTx > 1000.0) warn.flags = warn.flags or WARN_TX_TERMINAL_HEIGHT
        if (hTx < 0.5 || hTx > 3000.0) return ERROR_TX_TERMINAL_HEIGHT

        if (hRx < 1.0 || hRx > 1000.0) warn.flags = warn.flags or WARN_RX_TERMINAL_HEIGHT
        if (hRx < 0.5 || hRx > 3000.0) return ERROR_RX_TERMINAL_HEIGHT

        if (climate !in 1..7) return ERROR_INVALID_RADIO_CLIMATE

        if (n0 < 250 || n0 > 400) return ERROR_REFRACTIVITY

        if (fMhz < 40.0 || fMhz > 10000.0) warn.flags = warn.flags or WARN_FREQUENCY
        if (fMhz < 20 || fMhz > 20000) return ERROR_FREQUENCY

        if (pol != 0 && pol != 1) return ERROR_POLARIZATION

        if (epsilon < 1) return ERROR_EPSILON
        if (sigma <= 0) return ERROR_SIGMA

        if (mdvar < 0 || (mdvar > 3 && mdvar < 10) || (mdvar > 13 && mdvar < 20) ||
            (mdvar > 23 && mdvar < 30) || mdvar > 33
        ) {
            return ERROR_MDVAR
        }

        if (situation <= 0 || situation >= 100) return ERROR_INVALID_SITUATION
        if (time <= 0 || time >= 100) return ERROR_INVALID_TIME
        if (location <= 0 || location >= 100) return ERROR_INVALID_LOCATION
        return 0
    }

    // ---- QuickPfl ----

    private fun quickPfl(
        pfl: DoubleArray, gammaE: Double, h: DoubleArray, thetaHzn: DoubleArray, dHzn: DoubleArray,
        hE: DoubleArray, deltaHOut: DoubleArray, dOut: DoubleArray,
    ) {
        val d = pfl[0] * pfl[1]
        dOut[0] = d
        val np = pfl[0].toInt()
        val aE = 1 / gammaE

        findHorizons(pfl, aE, h, thetaHzn, dHzn)

        val dStart = min(15.0 * h[0], 0.1 * dHzn[0])
        val dEnd = d - min(15.0 * h[1], 0.1 * dHzn[1])

        val deltaH = computeDeltaH(pfl, dStart, dEnd)
        deltaHOut[0] = deltaH

        if (dHzn[0] + dHzn[1] > 1.5 * d) {
            val fit = DoubleArray(2)
            linearLeastSquaresFit(pfl, dStart, dEnd, fit)
            val fitTx = fit[0]
            val fitRx = fit[1]

            hE[0] = h[0] + fdim(pfl[2], fitTx)
            hE[1] = h[1] + fdim(pfl[np + 2], fitRx)

            for (i in 0..1) {
                dHzn[i] = sqrt(2.0 * hE[i] * aE) * exp(-0.07 * sqrt(deltaH / max(hE[i], 5.0)))
            }

            val combined = dHzn[0] + dHzn[1]
            if (combined <= d) {
                val q = (d / combined).pow(2.0)
                for (i in 0..1) {
                    hE[i] = hE[i] * q
                    dHzn[i] = sqrt(2.0 * hE[i] * aE) * exp(-0.07 * sqrt(deltaH / max(hE[i], 5.0)))
                }
            }

            for (i in 0..1) {
                val q = sqrt(2.0 * hE[i] * aE)
                thetaHzn[i] = (0.65 * deltaH * (q / dHzn[i] - 1.0) - 2.0 * hE[i]) / q
            }
        } else {
            val fit = DoubleArray(2)
            linearLeastSquaresFit(pfl, dStart, 0.9 * dHzn[0], fit)
            hE[0] = h[0] + fdim(pfl[2], fit[0])

            linearLeastSquaresFit(pfl, d - 0.9 * dHzn[1], dEnd, fit)
            hE[1] = h[1] + fdim(pfl[np + 2], fit[1])
        }
    }

    // ---- ComputeDeltaH ----

    private fun computeDeltaH(pfl: DoubleArray, dStart: Double, dEnd: Double): Double {
        val s = DoubleArray(247)
        val np = pfl[0].toInt()
        var xStart = dStart / pfl[1]
        var xEnd = dEnd / pfl[1]

        if (xEnd - xStart < 2.0) return 0.0

        var p10 = (0.1 * (xEnd - xStart + 8.0)).toInt()
        p10 = min(max(4, p10), 25)

        val n = 10 * p10 - 5
        val p90 = n - p10

        val npS = (n - 1).toDouble()
        s[0] = npS
        s[1] = 1.0

        xEnd = (xEnd - xStart) / npS
        var i = xStart.toInt()
        xStart -= (i + 1.0).toFloat().toDouble()

        for (j in 0 until n) {
            while (xStart > 0.0 && (i + 1) < np) {
                xStart--
                i++
            }
            s[j + 2] = pfl[i + 3] + (pfl[i + 3] - pfl[i + 2]) * xStart
            xStart += xEnd
        }

        val fit = DoubleArray(2)
        linearLeastSquaresFit(s, 0.0, npS, fit)
        var fitY1 = fit[0]
        val fitY2 = (fit[1] - fit[0]) / npS

        val diffs = DoubleArray(n)
        for (j in 0 until n) {
            diffs[j] = s[j + 2] - fitY1
            fitY1 += fitY2
        }

        // descending order statistics (equivalent to nth_element with std::greater)
        diffs.sort()
        val q10 = diffs[n - p10]
        val q90 = diffs[n - 1 - p90]

        val deltaHD = q10 - q90
        return deltaHD / (1.0 - 0.8 * exp(-(dEnd - dStart) / 50e3))
    }

    // ---- FindHorizons ----

    private fun findHorizons(pfl: DoubleArray, aE: Double, h: DoubleArray, thetaHzn: DoubleArray, dHzn: DoubleArray) {
        val np = pfl[0].toInt()
        val xi = pfl[1]
        val d = pfl[0] * pfl[1]

        val zTx = pfl[2] + h[0]
        val zRx = pfl[np + 2] + h[1]

        thetaHzn[0] = (zRx - zTx) / d - d / (2 * aE)
        thetaHzn[1] = -(zRx - zTx) / d - d / (2 * aE)

        dHzn[0] = d
        dHzn[1] = d

        var dTx = 0.0
        var dRx = d

        for (i in 1 until np) {
            dTx += xi
            dRx -= xi

            val thetaTx = (pfl[i + 2] - zTx) / dTx - dTx / (2 * aE)
            val thetaRx = -(zRx - pfl[i + 2]) / dRx - dRx / (2 * aE)

            if (thetaTx > thetaHzn[0]) {
                thetaHzn[0] = thetaTx
                dHzn[0] = dTx
            }
            if (thetaRx > thetaHzn[1]) {
                thetaHzn[1] = thetaRx
                dHzn[1] = dRx
            }
        }
    }

    // ---- LinearLeastSquaresFit ----

    /** Writes fitted y1 to out[0] and y2 to out[1]. */
    private fun linearLeastSquaresFit(pfl: DoubleArray, dStart: Double, dEnd: Double, out: DoubleArray) {
        val np = pfl[0].toInt()

        var iStart = fdim(dStart / pfl[1], 0.0).toInt()
        var iEnd = np - fdim(np.toDouble(), dEnd / pfl[1]).toInt()

        if (iEnd <= iStart) {
            iStart = fdim(iStart.toDouble(), 1.0).toInt()
            iEnd = np - fdim(np.toDouble(), iEnd + 1.0).toInt()
        }

        val xLength = (iEnd - iStart).toDouble()

        var midShiftedIndex = -0.5 * xLength
        val midShiftedEnd = iEnd + midShiftedIndex

        var sumY = 0.5 * (pfl[iStart + 2] + pfl[iEnd + 2])
        var scaledSumY = 0.5 * (pfl[iStart + 2] - pfl[iEnd + 2]) * midShiftedIndex

        var i = 2
        while (i <= xLength) {
            iStart++
            midShiftedIndex++
            sumY += pfl[iStart + 2]
            scaledSumY += pfl[iStart + 2] * midShiftedIndex
            i++
        }

        sumY /= xLength
        scaledSumY = scaledSumY * 12.0 / ((xLength * xLength + 2.0) * xLength)

        out[0] = sumY - scaledSumY * midShiftedEnd
        out[1] = sumY + scaledSumY * (np - midShiftedEnd)
    }

    // ---- LongleyRice (lrprop) ----

    private fun longleyRice(
        thetaHzn: DoubleArray, fMhz: Double, zG: Cx, dHzn: DoubleArray, hE: DoubleArray, gammaE: Double,
        nS: Double, deltaH: Double, h: DoubleArray, d: Double, mode: Int, aRefOut: DoubleArray,
        warn: Warn, propMode: IntArray,
    ): Int {
        val aE = 1 / gammaE

        val dHznS = DoubleArray(2)
        for (i in 0..1) dHznS[i] = sqrt(2.0 * hE[i] * aE)

        val dSML = dHznS[0] + dHznS[1]
        val dML = dHzn[0] + dHzn[1]
        val thetaLos = -max(thetaHzn[0] + thetaHzn[1], -dML / aE)

        if (abs(thetaHzn[0]) > 200e-3) warn.flags = warn.flags or WARN_TX_HORIZON_ANGLE
        if (abs(thetaHzn[1]) > 200e-3) warn.flags = warn.flags or WARN_RX_HORIZON_ANGLE

        if (dHzn[0] < 0.1 * dHznS[0]) warn.flags = warn.flags or WARN_TX_HORIZON_DISTANCE_1
        if (dHzn[1] < 0.1 * dHznS[1]) warn.flags = warn.flags or WARN_RX_HORIZON_DISTANCE_1

        if (dHzn[0] > 3.0 * dHznS[0]) warn.flags = warn.flags or WARN_TX_HORIZON_DISTANCE_2
        if (dHzn[1] > 3.0 * dHznS[1]) warn.flags = warn.flags or WARN_RX_HORIZON_DISTANCE_2

        if (nS < 150) return ERROR_SURFACE_REFRACTIVITY_SMALL
        if (nS > 400) return ERROR_SURFACE_REFRACTIVITY_LARGE
        if (nS < 250) warn.flags = warn.flags or WARN_SURFACE_REFRACTIVITY

        if (aE < 4000000 || aE > 13333333) return ERROR_EFFECTIVE_EARTH

        if (zG.re <= abs(zG.im)) return ERROR_GROUND_IMPEDANCE

        val cube = (aE.pow(2.0) / fMhz).pow(1.0 / 3.0)
        val d3 = max(dSML, dML + 5.0 * cube)
        val d4 = d3 + 10.0 * cube

        val a3 = diffractionLoss(d3, dHzn, hE, zG, aE, deltaH, h, mode, thetaLos, dSML, fMhz)
        val a4 = diffractionLoss(d4, dHzn, hE, zG, aE, deltaH, h, mode, thetaLos, dSML, fMhz)

        val mD = (a4 - a3) / (d4 - d3)
        val aD0 = a3 - mD * d3

        val dMin = abs(hE[0] - hE[1]) / 200e-3

        if (d < dMin) warn.flags = warn.flags or WARN_PATH_DISTANCE_TOO_SMALL_1
        if (d < 1e3) warn.flags = warn.flags or WARN_PATH_DISTANCE_TOO_SMALL_2
        if (d > 1000e3) warn.flags = warn.flags or WARN_PATH_DISTANCE_TOO_BIG_1
        if (d > 2000e3) warn.flags = warn.flags or WARN_PATH_DISTANCE_TOO_BIG_2

        var aRef: Double
        if (d < dSML) {
            val aSML = dSML * mD + aD0

            var d0 = 0.04 * fMhz * hE[0] * hE[1]

            val d1: Double
            if (aD0 >= 0.0) {
                d0 = min(d0, 0.5 * dML)
                d1 = d0 + 0.25 * (dML - d0)
            } else {
                d1 = max(-aD0 / mD, 0.25 * dML)
            }

            val a1 = lineOfSightLoss(d1, hE, zG, deltaH, mD, aD0, dSML, fMhz)

            var flag = false
            var k1 = 0.0
            var k2 = 0.0

            if (d0 < d1) {
                val a0 = lineOfSightLoss(d0, hE, zG, deltaH, mD, aD0, dSML, fMhz)
                val q = ln(dSML / d0)

                k2 = max(
                    0.0,
                    ((dSML - d0) * (a1 - a0) - (d1 - d0) * (aSML - a0)) /
                        ((dSML - d0) * ln(d1 / d0) - (d1 - d0) * q),
                )

                flag = aD0 > 0.0 || k2 > 0.0

                if (flag) {
                    k1 = (aSML - a0 - k2 * q) / (dSML - d0)
                    if (k1 < 0.0) {
                        k1 = 0.0
                        k2 = dim(aSML, a0) / q
                        if (k2 == 0.0) k1 = mD
                    }
                }
            }

            if (!flag) {
                k1 = dim(aSML, a1) / (dSML - d1)
                k2 = 0.0
                if (k1 == 0.0) k1 = mD
            }

            val aO = aSML - k1 * dSML - k2 * ln(dSML)
            aRef = aO + k1 * d + k2 * ln(d)
            propMode[0] = 1
        } else {
            val d5 = dML + 200e3
            val d6 = dML + 400e3

            val h0 = doubleArrayOf(-1.0)
            val a6 = troposcatterLoss(d6, thetaHzn, dHzn, hE, aE, nS, fMhz, thetaLos, h0)
            val a5 = troposcatterLoss(d5, thetaHzn, dHzn, hE, aE, nS, fMhz, thetaLos, h0)

            val mS: Double
            val aS0: Double
            val dX: Double

            if (a5 < 1000.0) {
                mS = (a6 - a5) / 200e3
                dX = max(
                    max(dSML, dML + 1.088 * (aE.pow(2.0) / fMhz).pow(1.0 / 3.0) * ln(fMhz)),
                    (a5 - aD0 - mS * d5) / (mD - mS),
                )
                aS0 = (mD - mS) * dX + aD0
            } else {
                mS = mD
                aS0 = aD0
                dX = 10e6
            }

            if (d > dX) {
                aRef = mS * d + aS0
                propMode[0] = 3
            } else {
                aRef = mD * d + aD0
                propMode[0] = 2
            }
        }

        aRef = max(aRef, 0.0)
        aRefOut[0] = aRef
        return 0
    }

    private fun dim(x: Double, y: Double): Double = if (x > y) x - y else 0.0

    // ---- Diffraction ----

    private fun diffractionLoss(
        d: Double, dHzn: DoubleArray, hE: DoubleArray, zG: Cx, aE: Double, deltaH: Double,
        h: DoubleArray, mode: Int, thetaLos: Double, dSML: Double, fMhz: Double,
    ): Double {
        val aK = knifeEdgeDiffraction(d, fMhz, aE, thetaLos, dHzn)
        val aSe = smoothEarthDiffraction(d, fMhz, aE, thetaLos, dHzn, hE, zG)

        val deltaHDsML = terrainRoughness(dSML, deltaH)
        val sigmaHD = sigmaH(deltaHDsML)
        val aFo = min(15.0, 5 * log10(1.0 + 1e-5 * h[0] * h[1] * fMhz * sigmaHD))

        val deltaHD = terrainRoughness(d, deltaH)

        var q = h[0] * h[1]
        val qk = hE[0] * hE[1] - q

        if (mode == MODE_P2P) q += 10.0

        val term1 = sqrt(1.0 + qk / q)

        val dML = dHzn[0] + dHzn[1]
        q = (term1 + (-thetaLos * aE + dML) / d) * min(deltaHD * fMhz / 47.7, 6283.2)

        val w = 25.1 / (25.1 + sqrt(q))

        return w * aSe + (1.0 - w) * aK + aFo
    }

    private fun smoothEarthDiffraction(
        d: Double, fMhz: Double, aE: Double, thetaLos: Double, dHzn: DoubleArray, hE: DoubleArray, zG: Cx,
    ): Double {
        val a = DoubleArray(3)
        val dKm = DoubleArray(3)
        val k = DoubleArray(3)
        val b0 = DoubleArray(3)
        val x = DoubleArray(3)
        val c0 = DoubleArray(3)

        val thetaNlos = d / aE - thetaLos
        val dML = dHzn[0] + dHzn[1]

        a[0] = (d - dML) / (d / aE - thetaLos)
        a[1] = 0.5 * dHzn[0].pow(2.0) / hE[0]
        a[2] = 0.5 * dHzn[1].pow(2.0) / hE[1]

        dKm[0] = (a[0] * thetaNlos) / 1000.0
        dKm[1] = dHzn[0] / 1000.0
        dKm[2] = dHzn[1] / 1000.0

        for (i in 0..2) {
            c0[i] = ((4.0 / 3.0) * A_0_METER / a[i]).pow(THIRD)
            k[i] = 0.017778 * c0[i] * fMhz.pow(-THIRD) / cAbs(zG)
            b0[i] = 1.607 - k[i]
        }

        x[1] = b0[1] * c0[1].pow(2.0) * fMhz.pow(THIRD) * dKm[1]
        x[2] = b0[2] * c0[2].pow(2.0) * fMhz.pow(THIRD) * dKm[2]
        x[0] = b0[0] * c0[0].pow(2.0) * fMhz.pow(THIRD) * dKm[0] + x[1] + x[2]

        val f0 = heightFunction(x[1], k[1])
        val f1 = heightFunction(x[2], k[2])

        val gX = 0.05751 * x[0] - 10.0 * log10(x[0])

        return gX - f0 - f1 - 20
    }

    private fun heightFunction(xKm: Double, k: Double): Double {
        var result: Double
        if (xKm < 200.0) {
            val w = -ln(k)
            if (k < 1e-5 || xKm * w.pow(3.0) > 5495.0) {
                result = -117.0
                if (xKm > 1.0) result = 17.372 * ln(xKm) + result
            } else {
                result = 2.5e-5 * xKm.pow(2.0) / k - 8.686 * w - 15.0
            }
        } else {
            result = 0.05751 * xKm - 4.343 * ln(xKm)
            if (xKm < 2000) {
                val w = 0.0134 * xKm * exp(-0.005 * xKm)
                result = (1.0 - w) * result + w * (17.372 * ln(xKm) - 117.0)
            }
        }
        return result
    }

    private fun knifeEdgeDiffraction(d: Double, fMhz: Double, aE: Double, thetaLos: Double, dHzn: DoubleArray): Double {
        val dML = dHzn[0] + dHzn[1]
        val thetaNlos = d / aE - thetaLos
        val dNlos = d - dML

        val v1 = 0.0795775 * (fMhz / 47.7) * thetaNlos.pow(2.0) * dHzn[0] * dNlos / (dNlos + dHzn[0])
        val v2 = 0.0795775 * (fMhz / 47.7) * thetaNlos.pow(2.0) * dHzn[1] * dNlos / (dNlos + dHzn[1])

        return fresnelIntegral(v1) + fresnelIntegral(v2)
    }

    private fun fresnelIntegral(v2: Double): Double =
        if (v2 < 5.76) 6.02 + 9.11 * sqrt(v2) - 1.27 * v2 else 12.953 + 10 * log10(v2)

    // ---- Line of sight ----

    private fun lineOfSightLoss(
        d: Double, hE: DoubleArray, zG: Cx, deltaH: Double, mD: Double, aD0: Double, dSML: Double, fMhz: Double,
    ): Double {
        val deltaHD = terrainRoughness(d, deltaH)
        val sigmaHD = sigmaH(deltaHD)

        val wn = fMhz / 47.7

        val sinPsi = (hE[0] + hE[1]) / sqrt(d.pow(2.0) + (hE[0] + hE[1]).pow(2.0))

        val sp = Cx(sinPsi, 0.0)
        var rE = cScale(cDiv(cMinus(sp, zG), cPlus(sp, zG)), exp(-min(10.0, wn * sigmaHD * sinPsi)))

        val q = rE.re.pow(2.0) + rE.im.pow(2.0)
        if (q < 0.25 || q < sinPsi) rE = cScale(rE, sqrt(sinPsi / q))

        var deltaPhi = wn * 2.0 * hE[0] * hE[1] / d

        if (deltaPhi > PI / 2.0) deltaPhi = PI - (PI / 2.0).pow(2.0) / deltaPhi

        val rr = cPlus(Cx(cos(deltaPhi), -sin(deltaPhi)), rE)
        val aT = -10 * log10(rr.re.pow(2.0) + rr.im.pow(2.0))

        val aD = mD * d + aD0

        val w = 1 / (1 + fMhz * deltaH / max(10e3, dSML))

        return w * aT + (1 - w) * aD
    }

    // ---- Troposcatter ----

    private fun fFunction(td: Double): Double {
        val a = doubleArrayOf(133.4, 104.6, 71.8)
        val b = doubleArrayOf(0.332e-3, 0.212e-3, 0.157e-3)
        val c = doubleArrayOf(-10.0, -2.5, 5.0)

        val i = if (td <= 10e3) 0 else if (td <= 70e3) 1 else 2
        return a[i] + b[i] * td + c[i] * log10(td)
    }

    private fun troposcatterLoss(
        d: Double, thetaHzn: DoubleArray, dHzn: DoubleArray, hE: DoubleArray, aE: Double, nS: Double,
        fMhz: Double, thetaLos: Double, h0: DoubleArray,
    ): Double {
        var hZero: Double
        val wn = fMhz / 47.7

        if (h0[0] > 15.0) {
            hZero = h0[0]
        } else {
            var ad = dHzn[0] - dHzn[1]
            var rr = hE[1] / hE[0]

            if (ad < 0.0) {
                ad = -ad
                rr = 1.0 / rr
            }

            val theta = thetaHzn[0] + thetaHzn[1] + d / aE

            val r1 = 2.0 * wn * theta * hE[0]
            val r2 = 2.0 * wn * theta * hE[1]

            if (r1 < 0.2 && r2 < 0.2) return 1001.0

            var s = (d - ad) / (d + ad)

            val q = min(max(0.1, rr / s), 10.0)
            s = max(0.1, s)

            val h0Meter = (d - ad) * (d + ad) * theta * 0.25 / d

            val z0 = 1.7556e3
            val z1 = 8.0e3
            val etaS = (h0Meter / z0) *
                (1.0 + (0.031 - nS * 2.32e-3 + nS.pow(2.0) * 5.67e-6) * exp(-min(1.7, h0Meter / z1).pow(6.0)))

            val hH00 = (h0Function(r1, etaS) + h0Function(r2, etaS)) / 2
            val deltaH0 = min(hH00, 6.0 * (0.6 - log10(max(etaS, 1.0))) * log10(s) * log10(q))

            hZero = hH00 + deltaH0
            hZero = max(hZero, 0.0)

            if (etaS < 1.0) {
                hZero = etaS * hZero + (1.0 - etaS) * 10 *
                    log10(
                        ((1.0 + SQRT2 / r1) * (1.0 + SQRT2 / r2)).pow(2.0) * (r1 + r2) / (r1 + r2 + 2 * SQRT2),
                    )
            }

            if (hZero > 15.0 && h0[0] >= 0.0) hZero = h0[0]
        }

        h0[0] = hZero
        val th = d / aE - thetaLos

        val d0 = 40e3
        val hMeter = 47.7
        return fFunction(th * d) + 10 * log10(wn * hMeter * th.pow(4.0)) -
            0.1 * (nS - 301.0) * exp(-th * d / d0) + hZero
    }

    private fun h0Curve(j: Int, r: Double): Double {
        val a = doubleArrayOf(25.0, 80.0, 177.0, 395.0, 705.0)
        val b = doubleArrayOf(24.0, 45.0, 68.0, 80.0, 105.0)
        return 10 * log10(1 + a[j] * (1 / r).pow(4.0) + b[j] * (1.0 / r).pow(2.0))
    }

    private fun h0Function(r: Double, etaIn: Double): Double {
        val eta = min(max(etaIn, 1.0), 5.0)
        val i = eta.toInt()
        val q = eta - i

        var result = h0Curve(i - 1, r)
        if (q != 0.0) result = (1.0 - q) * result + q * h0Curve(i, r)
        return result
    }

    // ---- small helpers ----

    private fun sigmaH(deltaH: Double): Double = 0.78 * deltaH * exp(-0.5 * deltaH.pow(0.25))

    private fun terrainRoughness(d: Double, deltaH: Double): Double = deltaH * (1.0 - 0.8 * exp(-d / 50e3))

    private fun freeSpaceLoss(d: Double, fMhz: Double): Double =
        32.45 + 20.0 * log10(fMhz) + 20.0 * log10(d / 1000.0)

    private fun inverseCcdf(q: Double): Double {
        val c0 = 2.515516
        val c1 = 0.802853
        val c2 = 0.010328
        val d1 = 1.432788
        val d2 = 0.189269
        val d3 = 0.001308

        var x = q
        if (q > 0.5) x = 1.0 - x

        val t = sqrt(-2.0 * ln(x))
        val zeta = ((c2 * t + c1) * t + c0) / (((d3 * t + d2) * t + d1) * t + 1.0)

        var res = t - zeta
        if (q > 0.5) res = -res
        return res
    }

    // ---- Variability ----

    private val allYear = arrayOf(
        doubleArrayOf(-9.67, -0.62, 1.26, -9.21, -0.62, -0.39, 3.15),
        doubleArrayOf(12.7, 9.19, 15.5, 9.05, 9.19, 2.86, 857.9),
        doubleArrayOf(144.9e3, 228.9e3, 262.6e3, 84.1e3, 228.9e3, 141.7e3, 2222.0e3),
        doubleArrayOf(190.3e3, 205.2e3, 185.2e3, 101.1e3, 205.2e3, 315.9e3, 164.8e3),
        doubleArrayOf(133.8e3, 143.6e3, 99.8e3, 98.6e3, 143.6e3, 167.4e3, 116.3e3),
    )
    private val bsm1 = doubleArrayOf(2.13, 2.66, 6.11, 1.98, 2.68, 6.86, 8.51)
    private val bsm2 = doubleArrayOf(159.5, 7.67, 6.65, 13.11, 7.16, 10.38, 169.8)
    private val xsm1 = doubleArrayOf(762.2e3, 100.4e3, 138.2e3, 139.1e3, 93.7e3, 187.8e3, 609.8e3)
    private val xsm2 = doubleArrayOf(123.6e3, 172.5e3, 242.2e3, 132.7e3, 186.8e3, 169.6e3, 119.9e3)
    private val xsm3 = doubleArrayOf(94.5e3, 136.4e3, 178.6e3, 193.5e3, 133.5e3, 108.9e3, 106.6e3)
    private val bsp1 = doubleArrayOf(2.11, 6.87, 10.08, 3.68, 4.75, 8.58, 8.43)
    private val bsp2 = doubleArrayOf(102.3, 15.53, 9.60, 159.3, 8.12, 13.97, 8.19)
    private val xsp1 = doubleArrayOf(636.9e3, 138.7e3, 165.3e3, 464.4e3, 93.2e3, 216.0e3, 136.2e3)
    private val xsp2 = doubleArrayOf(134.8e3, 143.7e3, 225.7e3, 93.1e3, 135.9e3, 152.0e3, 188.5e3)
    private val xsp3 = doubleArrayOf(95.6e3, 98.6e3, 129.7e3, 94.2e3, 113.4e3, 122.7e3, 122.9e3)
    private val cD = doubleArrayOf(1.224, 0.801, 1.380, 1.000, 1.224, 1.518, 1.518)
    private val zD = doubleArrayOf(1.282, 2.161, 1.282, 20.0, 1.282, 1.282, 1.282)
    private val bfm1 = doubleArrayOf(1.0, 1.0, 1.0, 1.0, 0.92, 1.0, 1.0)
    private val bfm2 = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.25, 0.0, 0.0)
    private val bfm3 = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.77, 0.0, 0.0)
    private val bfp1 = doubleArrayOf(1.0, 0.93, 1.0, 0.93, 0.93, 1.0, 1.0)
    private val bfp2 = doubleArrayOf(0.0, 0.31, 0.0, 0.19, 0.31, 0.0, 0.0)
    private val bfp3 = doubleArrayOf(0.0, 2.00, 0.0, 1.79, 2.00, 0.0, 0.0)

    private fun curve(c1: Double, c2: Double, x1: Double, x2: Double, x3: Double, de: Double): Double =
        (c1 + c2 / (1.0 + ((de - x2) / x3).pow(2.0))) * (de / x1).pow(2.0) / (1.0 + (de / x1).pow(2.0))

    private fun variability(
        time: Double, location: Double, situation: Double, hE: DoubleArray, deltaH: Double, fMhz: Double,
        d: Double, aRef: Double, climate: Int, mdvar: Int, warn: Warn,
    ): Double {
        var zT = inverseCcdf(time / 100)
        var zL = inverseCcdf(location / 100)
        val zS = inverseCcdf(situation / 100)

        val ci = climate - 1

        val wn = fMhz / 47.7

        val dEx = sqrt(2 * A_9000_METER * hE[0]) + sqrt(2 * A_9000_METER * hE[1]) + (575.7e12 / wn).pow(THIRD)

        val dE = if (d < dEx) 130e3 * d / dEx else 130e3 + d - dEx

        var mv = mdvar
        val plus20 = mv >= 20
        if (plus20) mv -= 20

        val sigmaS = if (plus20) 0.0 else 5.0 + 3.0 * exp(-dE / 100e3)

        val plus10 = mv >= 10
        if (plus10) mv -= 10

        val vMed = curve(allYear[0][ci], allYear[1][ci], allYear[2][ci], allYear[3][ci], allYear[4][ci], dE)

        when (mv) {
            0 -> {
                zT = zS
                zL = zS
            }
            1 -> zL = zS
            2 -> zL = zT
        }

        if (abs(zT) > 3.10 || abs(zL) > 3.10 || abs(zS) > 3.10) {
            warn.flags = warn.flags or WARN_EXTREME_VARIABILITIES
        }

        val sigmaL: Double
        if (plus10) {
            sigmaL = 0.0
        } else {
            val deltaHD = terrainRoughness(d, deltaH)
            sigmaL = 10.0 * wn * deltaHD / (wn * deltaHD + 13.0)
        }
        val yL = sigmaL * zL

        val q = ln(0.133 * wn)
        val gMinus = bfm1[ci] + bfm2[ci] / ((bfm3[ci] * q).pow(2.0) + 1.0)
        val gPlus = bfp1[ci] + bfp2[ci] / ((bfp3[ci] * q).pow(2.0) + 1.0)

        val sigmaTMinus = curve(bsm1[ci], bsm2[ci], xsm1[ci], xsm2[ci], xsm3[ci], dE) * gMinus
        val sigmaTPlus = curve(bsp1[ci], bsp2[ci], xsp1[ci], xsp2[ci], xsp3[ci], dE) * gPlus

        val sigmaTD = cD[ci] * sigmaTPlus
        val tgtd = (sigmaTPlus - sigmaTD) * zD[ci]

        val sigmaT = if (zT < 0.0) {
            sigmaTMinus
        } else if (zT <= zD[ci]) {
            sigmaTPlus
        } else {
            sigmaTD + tgtd / zT
        }
        val yT = sigmaT * zT

        val ySTemp = sigmaS.pow(2.0) + yT.pow(2.0) / (7.8 + zS.pow(2.0)) + yL.pow(2.0) / (24.0 + zS.pow(2.0))

        val yR: Double
        val yS: Double
        when (mv) {
            0 -> {
                yR = 0.0
                yS = sqrt(sigmaT.pow(2.0) + sigmaL.pow(2.0) + ySTemp) * zS
            }
            1 -> {
                yR = yT
                yS = sqrt(sigmaL.pow(2.0) + ySTemp) * zS
            }
            2 -> {
                yR = sqrt(sigmaT.pow(2.0) + sigmaL.pow(2.0)) * zT
                yS = sqrt(ySTemp) * zS
            }
            else -> {
                yR = yT + yL
                yS = sqrt(ySTemp) * zS
            }
        }

        var result = aRef - vMed - yR - yS

        if (result < 0.0) result = result * (29.0 - result) / (29.0 - 10.0 * result)

        return result
    }
}
