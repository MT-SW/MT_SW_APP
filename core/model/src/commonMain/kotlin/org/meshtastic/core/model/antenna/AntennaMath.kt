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
@file:Suppress("MagicNumber", "TooManyFunctions")

package org.meshtastic.core.model.antenna

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/*
 * Antenna and feed-line calculations of the MT_SW antenna calculator (and of the assistant, so both always agree).
 * Sources of the formulas:
 *  - thickness of the conductor: resonant-length shortening p = 0.2257 / ln(L / 2a) (Stutzman & Thiele);
 *  - J-pole and Slim Jim: M0UKD calculator (3/4, 1/2, 1/4 wave sections, feed λ/40, gap λ/100);
 *  - coaxial collinear: half-wave sections × velocity factor of the cable, a quarter-wave whip on top;
 *  - Yagi-Uda: Rothammel / DL6WU tables (as in the changpuak.ch generator), with boom correction;
 *  - Moxon rectangle: L. B. Cebik's regression equations (MOXGEN), valid for 1e-5 to 1e-2 λ wire;
 *  - axial-mode helix: J. D. Kraus (C = λ, S = λ/4, gain 15·(C/λ)²·N·S/λ, Z = 140·C/λ);
 *  - rectangular patch: transmission-line model (Balanis), 50 Ω microstrip feed (Hammerstad);
 *  - cables: attenuation at two frequencies from manufacturers' tables, fitted as a·√f + b·f.
 */

/** The kinds of antenna the calculator draws and sizes. */
enum class AntennaType { GROUND_PLANE, DIPOLE, FOLDED_DIPOLE, FIVE_EIGHTHS, J_POLE, SLIM_JIM, COLLINEAR, YAGI, MOXON, BIQUAD, HELIX, PATCH, COAX_STUB }

/** Insulation on a wire element: it slows the wave a little, so the element gets a bit shorter (typical values). */
enum class Insulation(val factor: Double) { NONE(1.0), THIN(0.97), THICK(0.95) }

/** Coaxial cables with their velocity factor and attenuation (dB per 100 m) at two frequencies (MHz). */
enum class Cable(val label: String, val vf: Double, val f1: Double, val a1: Double, val f2: Double, val a2: Double) {
    RG174("RG-174", 0.66, 432.0, 60.0, 1296.0, 110.0),
    RG316("RG-316", 0.695, 400.0, 54.8, 1000.0, 101.7),
    RG58("RG-58", 0.66, 432.0, 40.0, 1296.0, 90.0),
    RG8X("RG-8X (Mini-8)", 0.82, 400.0, 25.9, 1000.0, 42.7),
    RG213("RG-213", 0.66, 432.0, 15.0, 1296.0, 26.0),
    H155("H-155", 0.79, 432.0, 19.8, 1296.0, 34.9),
    AIRCELL7("Aircell 7", 0.83, 432.0, 14.1, 1296.0, 26.1),
    ECOFLEX10("Ecoflex 10", 0.86, 432.0, 8.9, 1296.0, 16.5),
    ECOFLEX15("Ecoflex 15", 0.86, 432.0, 6.1, 1296.0, 11.4),
    H2000("H-2000 Flex", 0.83, 432.0, 8.5, 1296.0, 15.7),
    LMR240("LMR-240", 0.84, 400.0, 16.1, 1000.0, 25.9),
    LMR400("LMR-400", 0.85, 400.0, 8.2, 1000.0, 14.1),
    LMR600("LMR-600", 0.87, 400.0, 4.6, 1000.0, 8.9),
    ;

    /** Attenuation in dB per 100 m at [mhz]: a·√f + b·f through the two table points. */
    fun lossPer100m(mhz: Double): Double {
        val s1 = sqrt(f1)
        val s2 = sqrt(f2)
        val det = s1 * f2 - s2 * f1
        val a = (a1 * f2 - a2 * f1) / det
        val b = (s1 * a2 - s2 * a1) / det
        return max(0.0, a * sqrt(mhz) + b * mhz)
    }
}

/** Connectors and other parts in the feed line, with a typical insertion loss near 1 GHz. */
enum class Connector(val label: String, val lossDb: Double) {
    N("N", 0.05),
    SMA("SMA", 0.05),
    RP_SMA("RP-SMA", 0.05),
    BNC("BNC", 0.1),
    UHF("UHF (PL-259/SO-239)", 0.2),
    UFL("U.FL / IPEX", 0.2),
    ADAPTER("adapter", 0.1),
    ARRESTER("odgromnik / lightning arrester", 0.2),
}

/** What a dimension of a drawing stands for. */
enum class DimKind {
    TOTAL, RADIATOR, RADIAL, ARM, GAP, SPACING, STUB, FEED, SECTION, WHIP, REFLECTOR, DRIVEN, DIRECTOR, BOOM, POSITION,
    WIDTH, DEPTH, TAIL_DRIVER, TAIL_REFLECTOR, SIDE, REFLECTOR_PLATE, DIAMETER, PITCH, TURN_WIRE, LENGTH, GROUND,
    PATCH_W, PATCH_L, INSET, FEED_WIDTH, QUARTER, HALF, WAVELENGTH,
}

/** One dimension: its letter on the drawing, what it is, its length in metres and the element number (Yagi). */
data class Dim(val letter: String, val kind: DimKind, val meters: Double, val index: Int = 0)

/** Notes the calculator shows under a design. */
enum class Note { TUNE_SWR, RADIALS_ANGLE, BALUN_4_1, COIL_MATCH, CHOKE, FEED_TAP, YAGI_DRIVEN, MOXON_RANGE, HELIX_MATCH, HELIX_GAIN, PATCH_FR4, GAIN_APPROX, YAGI_BOOM, COAX_VF, HALF_WAVE_BALUN }

data class AntennaDesign(
    val dims: List<Dim>,
    val impedanceOhm: Double?,
    val gainDbi: Double,
    val polarization: Polarization,
    val notes: List<Note>,
    /** The factor actually used for wire elements (thickness and insulation). */
    val shortening: Double,
)

enum class Polarization { VERTICAL, HORIZONTAL_OR_VERTICAL, CIRCULAR, LINEAR }

/** Everything the calculator needs; the defaults are a 1.5 mm copper wire on our network's frequency. */
data class AntennaInput(
    val type: AntennaType = AntennaType.GROUND_PLANE,
    val mhz: Double = AntennaMath.OUR_MHZ,
    val diameterMm: Double = 1.5,
    val insulation: Insulation = Insulation.NONE,
    val radialAngleDeg: Double = 45.0,
    val radials: Int = 4,
    val elements: Int = 5,
    val boomMm: Double = 20.0,
    val metalBoom: Boolean = true,
    val cable: Cable = Cable.RG58,
    val sections: Int = 4,
    val turns: Int = 8,
    val epsR: Double = 4.4,
    val substrateMm: Double = 1.6,
    val foldedSpacingMm: Double = 0.0,
)

object AntennaMath {
    const val OUR_MHZ = 869.44165
    const val C = 299_792_458.0
    private const val DIPOLE_GAIN = 2.15

    /** Free-space wavelength in metres. */
    fun wavelength(mhz: Double) = C / (mhz * 1e6)

    /**
     * Shortening of a half-wave element made of a conductor [diameterM] thick (Stutzman & Thiele):
     * p = 0.2257 / ln(L / 2a) with L ≈ λ/2 and a the radius; thin wire ≈ 0.97–0.98, thick tube ≈ 0.92–0.94.
     */
    fun thicknessFactor(lambda: Double, diameterM: Double): Double {
        if (diameterM <= 0) return 0.98
        val ratio = (lambda / 2) / diameterM
        if (ratio <= 3) return 0.85
        return (1 - 0.2257 / ln(ratio)).coerceIn(0.85, 0.995)
    }

    fun design(input: AntennaInput): AntennaDesign {
        val f = input.mhz.coerceIn(1.0, 100_000.0)
        val l = wavelength(f)
        val d = input.diameterMm / 1000
        val k = thicknessFactor(l, d) * input.insulation.factor
        return when (input.type) {
            AntennaType.GROUND_PLANE -> groundPlane(l, k, input)
            AntennaType.DIPOLE -> dipole(l, k)
            AntennaType.FOLDED_DIPOLE -> folded(l, k, input)
            AntennaType.FIVE_EIGHTHS -> fiveEighths(l, k, input)
            AntennaType.J_POLE -> jPole(l, k, slim = false)
            AntennaType.SLIM_JIM -> jPole(l, k, slim = true)
            AntennaType.COLLINEAR -> collinear(l, k, input)
            AntennaType.YAGI -> yagi(l, k, input)
            AntennaType.MOXON -> moxon(l, d, k)
            AntennaType.BIQUAD -> biquad(l, k)
            AntennaType.HELIX -> helix(l, input)
            AntennaType.PATCH -> patch(f, input)
            AntennaType.COAX_STUB -> stub(l, input)
        }
    }

    private fun groundPlane(l: Double, k: Double, i: AntennaInput): AntennaDesign {
        val angle = i.radialAngleDeg.coerceIn(0.0, 90.0)
        // 36 Ω with flat radials, about 50 Ω at 45° drooping.
        val z = 36 + 14 * min(angle, 45.0) / 45 + max(0.0, angle - 45) * 0.4
        return AntennaDesign(
            listOf(Dim("A", DimKind.RADIATOR, l / 4 * k), Dim("B", DimKind.RADIAL, l / 4 * k)),
            z, DIPOLE_GAIN, Polarization.VERTICAL, listOf(Note.RADIALS_ANGLE, Note.TUNE_SWR), k,
        )
    }

    private fun dipole(l: Double, k: Double): AntennaDesign {
        val total = l / 2 * k
        val gap = l / 100
        return AntennaDesign(
            listOf(Dim("A", DimKind.TOTAL, total), Dim("B", DimKind.ARM, (total - gap) / 2), Dim("C", DimKind.GAP, gap)),
            73.0, DIPOLE_GAIN, Polarization.HORIZONTAL_OR_VERTICAL, listOf(Note.TUNE_SWR), k,
        )
    }

    private fun folded(l: Double, k: Double, i: AntennaInput): AntennaDesign {
        val spacing = if (i.foldedSpacingMm > 0) i.foldedSpacingMm / 1000 else l / 50
        return AntennaDesign(
            listOf(
                Dim("A", DimKind.TOTAL, l / 2 * k),
                Dim("B", DimKind.SPACING, spacing),
                Dim("C", DimKind.GAP, l / 100),
                Dim("D", DimKind.HALF, l / 2 * i.cable.vf),
            ),
            292.0, DIPOLE_GAIN, Polarization.HORIZONTAL_OR_VERTICAL, listOf(Note.BALUN_4_1, Note.HALF_WAVE_BALUN, Note.TUNE_SWR), k,
        )
    }

    private fun fiveEighths(l: Double, k: Double, i: AntennaInput): AntennaDesign = AntennaDesign(
        listOf(Dim("A", DimKind.RADIATOR, l * 0.625 * k), Dim("B", DimKind.RADIAL, l / 4 * k)),
        50.0, 4.0, Polarization.VERTICAL, listOf(Note.COIL_MATCH, Note.RADIALS_ANGLE, Note.GAIN_APPROX, Note.TUNE_SWR), k,
    )

    private fun jPole(l: Double, k: Double, slim: Boolean): AntennaDesign {
        val gap = l / 100
        val dims =
            listOf(
                Dim("A", DimKind.TOTAL, l * 0.75 * k + if (slim) gap else 0.0),
                Dim("B", DimKind.RADIATOR, l / 2 * k),
                Dim("C", DimKind.STUB, l / 4 * k),
                Dim("D", DimKind.FEED, l / 40 * k),
            ) + (if (slim) listOf(Dim("E", DimKind.GAP, gap)) else emptyList()) + Dim(if (slim) "F" else "E", DimKind.SPACING, l / 50)
        return AntennaDesign(dims, 50.0, DIPOLE_GAIN, Polarization.VERTICAL, listOf(Note.FEED_TAP, Note.CHOKE, Note.TUNE_SWR), k)
    }

    /** Typical gain of a coaxial collinear with [n] sections (cable losses included). */
    fun collinearGain(n: Int): Double = when {
        n <= 1 -> DIPOLE_GAIN
        n <= 8 -> doubleArrayOf(2.15, 3.5, 5.0, 6.0, 6.8, 7.5, 8.2, 8.7)[n - 1]
        else -> min(10.0, 8.7 + 0.3 * (n - 8))
    }

    private fun collinear(l: Double, k: Double, i: AntennaInput): AntennaDesign {
        val n = i.sections.coerceIn(2, 16)
        val section = l / 2 * i.cable.vf
        val whip = l / 4 * k
        return AntennaDesign(
            listOf(Dim("A", DimKind.SECTION, section), Dim("B", DimKind.WHIP, whip), Dim("C", DimKind.TOTAL, n * section + whip)),
            50.0, collinearGain(n), Polarization.VERTICAL, listOf(Note.COAX_VF, Note.CHOKE, Note.GAIN_APPROX, Note.TUNE_SWR), k,
        )
    }

    // Rothammel / DL6WU spacings (λ): reflector–driven, driven–D1, D1–D2, …
    private val YAGI_SPACING = doubleArrayOf(
        0.240, 0.075, 0.180, 0.215, 0.250, 0.280, 0.300, 0.315, 0.330, 0.345, 0.360, 0.375, 0.385, 0.390, 0.395,
        0.400, 0.400, 0.400, 0.400, 0.400,
    )

    private fun yagi(l: Double, k: Double, i: AntennaInput): AntennaDesign {
        val n = i.elements.coerceIn(3, 20)
        // Correction for elements mounted through a metal boom (boom diameter / λ between 0.01 and 0.05).
        val boom = (i.boomMm / 1000 / l).coerceIn(0.01, 0.05)
        val corr = if (i.metalBoom) -416.86 * boom.pow(3) + 46.183 * boom * boom - 0.6834 * boom + 0.0059 else 0.0
        val dl = (i.diameterMm / 1000 / l).coerceIn(0.002, 0.01)
        val t = (dl - 0.002) / 0.008
        val dims = ArrayList<Dim>()
        dims.add(Dim("R", DimKind.REFLECTOR, l * (0.482 + corr)))
        dims.add(Dim("Z", DimKind.DRIVEN, l / 2 * k))
        val length = (0 until n - 1).sumOf { YAGI_SPACING[it] }
        var position = YAGI_SPACING[0]
        dims.add(Dim("S0", DimKind.POSITION, YAGI_SPACING[0] * l, 1))
        for (j in 1..n - 2) {
            val big = -7e-6 * j * j * j + 0.0003 * j * j - 0.0073 * j + 0.433 // d/λ = 0.01
            val small = -3e-6 * j * j * j + 0.0002 * j * j - 0.0049 * j + 0.4595 // d/λ = 0.002
            dims.add(Dim("D$j", DimKind.DIRECTOR, l * (corr + small + (big - small) * t), j))
            position += YAGI_SPACING[j]
            dims.add(Dim("S$j", DimKind.POSITION, position * l, j + 1))
        }
        dims.add(Dim("L", DimKind.BOOM, length * l))
        val gainDbd = 3.39 * ln(length) + 9.15
        return AntennaDesign(dims, 50.0, gainDbd + DIPOLE_GAIN, Polarization.HORIZONTAL_OR_VERTICAL, listOf(Note.YAGI_DRIVEN, Note.YAGI_BOOM, Note.GAIN_APPROX, Note.TUNE_SWR), k)
    }

    private fun moxon(l: Double, d: Double, k: Double): AntennaDesign {
        val dw = (d / l).coerceIn(1e-5, 1e-2)
        val d1 = log10(dw)
        val a = -0.0008571428571 * d1 * d1 - 0.009571428571 * d1 + 0.3398571429
        val b = -0.002142857143 * d1 * d1 - 0.02035714286 * d1 + 0.008285714286
        val c = 0.001809523381 * d1 * d1 + 0.01780952381 * d1 + 0.05164285714
        val dd = 0.001 * d1 + 0.07178571429
        return AntennaDesign(
            listOf(
                Dim("A", DimKind.WIDTH, a * l),
                Dim("B", DimKind.TAIL_DRIVER, b * l),
                Dim("C", DimKind.GAP, c * l),
                Dim("D", DimKind.TAIL_REFLECTOR, dd * l),
                Dim("E", DimKind.DEPTH, (b + c + dd) * l),
            ),
            50.0, 5.9, Polarization.HORIZONTAL_OR_VERTICAL, listOf(Note.MOXON_RANGE, Note.GAIN_APPROX, Note.TUNE_SWR), k,
        )
    }

    private fun biquad(l: Double, k: Double): AntennaDesign = AntennaDesign(
        listOf(Dim("A", DimKind.SIDE, l / 4), Dim("B", DimKind.SPACING, l * 0.12), Dim("C", DimKind.REFLECTOR_PLATE, l)),
        50.0, 10.5, Polarization.LINEAR, listOf(Note.GAIN_APPROX, Note.TUNE_SWR), k,
    )

    private fun helix(l: Double, i: AntennaInput): AntennaDesign {
        val n = i.turns.coerceIn(3, 40)
        val circ = l
        val s = l / 4
        val gain = 10 * log10(15.0 * n * (s / l))
        return AntennaDesign(
            listOf(
                Dim("A", DimKind.DIAMETER, circ / PI),
                Dim("B", DimKind.PITCH, s),
                Dim("C", DimKind.LENGTH, n * s),
                Dim("D", DimKind.GROUND, l * 0.75),
                Dim("E", DimKind.TURN_WIRE, sqrt(circ * circ + s * s)),
            ),
            140.0, gain, Polarization.CIRCULAR, listOf(Note.HELIX_MATCH, Note.HELIX_GAIN, Note.TUNE_SWR), 1.0,
        )
    }

    private fun patch(mhz: Double, i: AntennaInput): AntennaDesign {
        val er = i.epsR.coerceIn(1.0, 20.0)
        val h = i.substrateMm.coerceIn(0.1, 20.0) / 1000
        val f = mhz * 1e6
        val w = C / (2 * f) * sqrt(2 / (er + 1))
        val eff = (er + 1) / 2 + (er - 1) / 2 / sqrt(1 + 12 * h / w)
        val dl = 0.412 * h * (eff + 0.3) * (w / h + 0.264) / ((eff - 0.258) * (w / h + 0.8))
        val len = C / (2 * f * sqrt(eff)) - 2 * dl
        val l0 = C / f
        val k0h = 2 * PI / l0 * h
        val g1 = w / (120 * l0) * (1 - k0h * k0h / 24)
        val rEdge = 1 / (2 * g1)
        val inset = if (rEdge > 50) len / PI * acos(sqrt(50 / rEdge)) else 0.0
        return AntennaDesign(
            listOf(
                Dim("A", DimKind.PATCH_W, w),
                Dim("B", DimKind.PATCH_L, len),
                Dim("C", DimKind.INSET, inset),
                Dim("D", DimKind.FEED_WIDTH, microstripWidth(50.0, er, h)),
                Dim("E", DimKind.GROUND, w + 6 * h),
                Dim("F", DimKind.GROUND, len + 6 * h),
            ),
            50.0, if (er > 3) 5.0 else 7.0, Polarization.LINEAR, listOf(Note.PATCH_FR4, Note.GAIN_APPROX, Note.TUNE_SWR), 1.0,
        )
    }

    /** Width of a microstrip line of impedance [z0] on a substrate [er] and [h] metres thick (Hammerstad). */
    fun microstripWidth(z0: Double, er: Double, h: Double): Double {
        val a = z0 / 60 * sqrt((er + 1) / 2) + (er - 1) / (er + 1) * (0.23 + 0.11 / er)
        val narrow = 8 * exp(a) / (exp(2 * a) - 2)
        if (narrow < 2) return narrow * h
        val b = 377 * PI / (2 * z0 * sqrt(er))
        return h * 2 / PI * (b - 1 - ln(2 * b - 1) + (er - 1) / (2 * er) * (ln(b - 1) + 0.39 - 0.61 / er))
    }

    private fun stub(l: Double, i: AntennaInput): AntennaDesign = AntennaDesign(
        listOf(Dim("A", DimKind.QUARTER, l / 4 * i.cable.vf), Dim("B", DimKind.HALF, l / 2 * i.cable.vf), Dim("λ", DimKind.WAVELENGTH, l)),
        null, 0.0, Polarization.LINEAR, listOf(Note.COAX_VF), 1.0,
    )

    // ------------------------------------------------------------ feed line and power

    /** Loss of a mismatch with this SWR, in dB. */
    fun mismatchLossDb(swr: Double): Double {
        if (swr <= 1.0) return 0.0
        val g = (swr - 1) / (swr + 1)
        return -10 * log10(1 - g * g)
    }

    data class Band(val from: Double, val to: Double, val erpDbm: Double?, val eirpDbm: Double?, val dutyPercent: Double?, val name: String)

    /** Power limits of the usual licence-free bands (EU short-range devices, US ISM, 2.4 GHz). */
    val BANDS = listOf(
        Band(433.05, 434.79, 10.0, null, 10.0, "433 MHz (EU)"),
        Band(863.0, 868.0, 14.0, null, 1.0, "863–868 MHz (EU)"),
        Band(868.0, 868.6, 14.0, null, 1.0, "868,0–868,6 MHz (EU)"),
        Band(868.7, 869.2, 14.0, null, 0.1, "868,7–869,2 MHz (EU)"),
        Band(869.4, 869.65, 27.0, null, 10.0, "869,4–869,65 MHz (EU)"),
        Band(869.7, 870.0, 14.0, null, 1.0, "869,7–870 MHz (EU)"),
        Band(902.0, 928.0, null, 36.0, null, "902–928 MHz (US)"),
        Band(2400.0, 2483.5, null, 20.0, null, "2,4 GHz (EU)"),
    )

    fun bandFor(mhz: Double): Band? = BANDS.firstOrNull { mhz >= it.from && mhz <= it.to }

    data class FeedResult(
        val cableDb: Double,
        val connectorsDb: Double,
        val mismatchDb: Double,
        val totalDb: Double,
        val atAntennaDbm: Double,
        val eirpDbm: Double,
        val erpDbm: Double,
        /** How far a link reaches compared with no losses and a dipole (free space), as a fraction. */
        val rangeFactor: Double,
        val band: Band?,
        val overLimit: Boolean?,
    )

    fun feed(mhz: Double, txDbm: Double, cable: Cable?, cableM: Double, connectors: Map<Connector, Int>, swr: Double, gainDbi: Double, customLossPer100m: Double? = null): FeedResult {
        val per100 = customLossPer100m ?: cable?.lossPer100m(mhz) ?: 0.0
        val cableDb = per100 * cableM / 100
        val connDb = connectors.entries.sumOf { it.key.lossDb * it.value }
        val mm = mismatchLossDb(swr)
        val total = cableDb + connDb + mm
        val at = txDbm - total
        val eirp = at + gainDbi
        val erp = eirp - DIPOLE_GAIN
        val band = bandFor(mhz)
        val over = band?.let { b -> (b.erpDbm?.let { erp > it + 0.05 }) ?: (b.eirpDbm?.let { eirp > it + 0.05 }) }
        // Both ends' losses and gains count once on the way out; range scales with 10^(dB / 20) in free space.
        val range = 10.0.pow((gainDbi - DIPOLE_GAIN - total) / 20)
        return FeedResult(cableDb, connDb, mm, total, at, eirp, erp, range, band, over)
    }

    /**
     * The same power calculation for a feed line whose loss is already known ([feederDb] in total, of which
     * [connectorsDb] is connectors), for example from [FeederBuilder.compute] with any number of cables and connectors.
     */
    fun feedLoss(mhz: Double, txDbm: Double, feederDb: Double, connectorsDb: Double, swr: Double, gainDbi: Double): FeedResult {
        val mm = mismatchLossDb(swr)
        val total = feederDb + mm
        val at = txDbm - total
        val eirp = at + gainDbi
        val erp = eirp - DIPOLE_GAIN
        val band = bandFor(mhz)
        val over = band?.let { b -> (b.erpDbm?.let { erp > it + 0.05 }) ?: (b.eirpDbm?.let { eirp > it + 0.05 }) }
        val range = 10.0.pow((gainDbi - DIPOLE_GAIN - total) / 20)
        return FeedResult(feederDb - connectorsDb, connectorsDb, mm, total, at, eirp, erp, range, band, over)
    }

    fun mw(dbm: Double) = 10.0.pow(dbm / 10)
}

/** What the assistant computed last, so its button opens the calculator on the same antenna. */
object AntennaPrefill {
    var mhz: Double? = null
    var type: AntennaType? = null
    var txDbm: Double? = null

    /** Takes the values once: a calculator opened later from the settings starts fresh. */
    fun consume(): Triple<Double?, AntennaType?, Double?> = Triple(mhz, type, txDbm).also {
        mhz = null
        type = null
        txDbm = null
    }
}
