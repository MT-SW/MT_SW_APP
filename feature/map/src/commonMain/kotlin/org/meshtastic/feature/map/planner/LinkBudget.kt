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

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

data class LinkEnd(
    val point: GeoPoint,
    val groundAltM: Double,
    val antennaHeightM: Double,
    val txPowerDbm: Double,
    val antennaGainDbi: Double,
    val feederLossDb: Double,
)

data class LinkInput(
    val a: LinkEnd,
    val b: LinkEnd,
    val frequencyMHz: Double,
    val bandwidthKhz: Double,
    val spreadingFactor: Int,
    val noiseFigureDb: Double = 6.0,
    val kFactor: Double = 4.0 / 3.0,
    val surfaceRefractivity: Double = 301.0,
    /** Clutter, gas, rain etc. */
    val extraLossDb: Double = 0.0,
    val timePct: Double = 50.0,
    val locationPct: Double = 50.0,
    val situationPct: Double = 50.0,
    val fadeMarginDb: Double = 0.0,
)

/** Margin >= 20 dB, >= 10, >= 3, >= 0, otherwise NO_LINK. */
enum class LinkVerdict {
    EXCELLENT,
    GOOD,
    MARGINAL,
    WEAK,
    NO_LINK,
    ;

    companion object {
        fun fromMargin(marginDb: Double): LinkVerdict = when {
            marginDb.isNaN() -> NO_LINK
            marginDb >= 20.0 -> EXCELLENT
            marginDb >= 10.0 -> GOOD
            marginDb >= 3.0 -> MARGINAL
            marginDb >= 0.0 -> WEAK
            else -> NO_LINK
        }
    }
}

data class DirectionResult(val rxPowerDbm: Double, val marginDb: Double, val verdict: LinkVerdict)

@Suppress("ArrayInDataClass")
data class LinkResult(
    val distanceM: Double,
    val bearingAToBDeg: Double,
    val bearingBToADeg: Double,
    val elevationAAngleDeg: Double,
    val elevationBAngleDeg: Double,
    val freeSpaceLossDb: Double,
    val itmLossDb: Double,
    /** itm + extra */
    val totalPathLossDb: Double,
    val sensitivityDbm: Double,
    val aToB: DirectionResult,
    val bToA: DirectionResult,
    val itmMode: ItmMode,
    val itmWarnings: Int,
    val lineOfSightClear: Boolean,
    val worstFresnelClearanceM: Double,
    /** clearance / first Fresnel radius at the worst point; < 0 means blocked. */
    val worstFresnelRatio: Double,
    val profile: PathProfile,
)

/** Chart data: ground with earth bulge (relative to the ground chord), LOS line and first Fresnel zone. */
@Suppress("ArrayInDataClass")
class ProfileSeries(
    val distancesM: DoubleArray,
    val groundM: DoubleArray,
    val losM: DoubleArray,
    val fresnelUpperM: DoubleArray,
    val fresnelLowerM: DoubleArray,
)

object LinkBudget {
    /** Loss reported when ITM fails (error return code). */
    const val ITM_ERROR_LOSS_DB = 999.0

    fun freeSpaceLossDb(distanceM: Double, fMHz: Double): Double {
        if (distanceM <= 0.0) return 0.0
        return 32.44 + 20.0 * log10(distanceM / 1000.0) + 20.0 * log10(fMHz)
    }

    /**
     * Analyse a link over [profile]. Antenna heights are taken above the profile's terrain at each end (the profile
     * is authoritative for ground elevation; [LinkEnd.groundAltM] is informational). ITM loss is reciprocal.
     */
    fun analyze(input: LinkInput, profile: PathProfile): LinkResult {
        val a = input.a
        val b = input.b
        val dist = profile.distanceM
        val fsl = freeSpaceLossDb(dist, input.frequencyMHz)

        val itm = Itm.pointToPoint(
            elevationsM = profile.groundM,
            stepM = profile.stepM,
            txHeightM = a.antennaHeightM,
            rxHeightM = b.antennaHeightM,
            frequencyMHz = input.frequencyMHz,
            surfaceRefractivityNUnits = input.surfaceRefractivity,
            timePct = input.timePct,
            locationPct = input.locationPct,
            situationPct = input.situationPct,
        )
        val itmOk = itm.returnCode <= 1 && !itm.lossDb.isNaN()
        val itmLoss = if (itmOk) itm.lossDb else ITM_ERROR_LOSS_DB
        val total = itmLoss + input.extraLossDb
        val sens = Sensitivity.dbm(input.bandwidthKhz, input.spreadingFactor, input.noiseFigureDb)

        fun dir(tx: LinkEnd, rx: LinkEnd): DirectionResult {
            val rxP = tx.txPowerDbm + tx.antennaGainDbi - tx.feederLossDb - total + rx.antennaGainDbi - rx.feederLossDb
            val margin = rxP - sens - input.fadeMarginDb
            return DirectionResult(rxP, margin, if (itmOk) LinkVerdict.fromMargin(margin) else LinkVerdict.NO_LINK)
        }

        // Line of sight and first Fresnel zone with earth bulge.
        val g = profile.groundM
        val n = profile.intervals
        val zA = g[0] + a.antennaHeightM
        val zB = g[n] + b.antennaHeightM
        var worstClear = min(a.antennaHeightM, b.antennaHeightM)
        var worstRatio = 99.0
        var minClear = Double.MAX_VALUE
        for (i in 1 until n) {
            val d1 = profile.distanceAt(i)
            val d2 = dist - d1
            val los = profile.losHeightM(i, zA, zB)
            val clear = los - (g[i] + profile.bulgeM(i, input.kFactor))
            val r1 = fresnelRadiusM(1, d1, d2, input.frequencyMHz)
            val ratio = if (r1 > 0) clear / r1 else 99.0
            if (clear < minClear) minClear = clear
            if (ratio < worstRatio) {
                worstRatio = ratio
                worstClear = clear
            }
        }
        val clearLos = n < 2 || minClear > 0.0

        val altA = zA
        val altB = zB
        return LinkResult(
            distanceM = dist,
            bearingAToBDeg = Geodesy.bearingDeg(a.point, b.point),
            bearingBToADeg = Geodesy.bearingDeg(b.point, a.point),
            elevationAAngleDeg = Geodesy.elevationAngleDeg(altA, altB, dist, input.kFactor),
            elevationBAngleDeg = Geodesy.elevationAngleDeg(altB, altA, dist, input.kFactor),
            freeSpaceLossDb = fsl,
            itmLossDb = itmLoss,
            totalPathLossDb = total,
            sensitivityDbm = sens,
            aToB = dir(a, b),
            bToA = dir(b, a),
            itmMode = itm.mode,
            itmWarnings = itm.warnings,
            lineOfSightClear = clearLos,
            worstFresnelClearanceM = worstClear,
            worstFresnelRatio = worstRatio,
            profile = profile,
        )
    }

    /** Series for the profile chart. */
    fun series(input: LinkInput, profile: PathProfile): ProfileSeries {
        val n = profile.intervals
        val g = profile.groundM
        val zA = g[0] + input.a.antennaHeightM
        val zB = g[n] + input.b.antennaHeightM
        val dist = profile.distanceM
        val ground = DoubleArray(n + 1)
        val los = DoubleArray(n + 1)
        val up = DoubleArray(n + 1)
        val lo = DoubleArray(n + 1)
        val d = DoubleArray(n + 1)
        for (i in 0..n) {
            d[i] = profile.distanceAt(i)
            ground[i] = g[i] + profile.bulgeM(i, input.kFactor)
            los[i] = profile.losHeightM(i, zA, zB)
            val r = fresnelRadiusM(1, d[i], dist - d[i], input.frequencyMHz)
            up[i] = los[i] + r
            lo[i] = los[i] - r
        }
        return ProfileSeries(d, ground, los, up, lo)
    }
}
