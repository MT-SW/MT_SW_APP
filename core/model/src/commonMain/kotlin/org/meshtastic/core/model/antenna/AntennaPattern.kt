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

package org.meshtastic.core.model.antenna

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** The kinds of antenna the planner can tell apart by how they radiate around the horizon. */
enum class PatternKind(val directional: Boolean) {
    /** Same in every direction. */
    OMNI(false),
    YAGI(true),
    MOXON(true),
    PANEL(true),
    BIQUAD(true),
    HELIX(true),
    DISH(true),

    /** A wide sector antenna of a base station (90 to 120 degrees). */
    SECTOR(true),
    ;

    companion object {
        /** The kind that matches an antenna type of the calculator. */
        fun of(type: AntennaType): PatternKind = when (type) {
            AntennaType.YAGI -> YAGI
            AntennaType.MOXON -> MOXON
            AntennaType.BIQUAD -> BIQUAD
            AntennaType.HELIX -> HELIX
            AntennaType.PATCH -> PANEL
            else -> OMNI
        }
    }
}

/**
 * Typical numbers for the horizontal pattern of each [PatternKind], and the loss off the main direction.
 *
 * These are textbook approximations (a parabolic main lobe that is 3 dB down at half the beamwidth and flattens out at
 * the front-to-back ratio), good for planning how much a link or a coverage area changes when an antenna is turned, not
 * a replacement for a measured pattern.
 */
object AntennaPatterns {
    const val MIN_ELEMENTS = 2
    const val MAX_ELEMENTS = 30
    const val DEFAULT_ELEMENTS = 5

    /** Whether the number of elements changes the antenna (only a Yagi). */
    fun hasElements(kind: PatternKind): Boolean = kind == PatternKind.YAGI

    /** Typical gain (dBi) of the kind; a Yagi grows with its number of [elements]. */
    fun presetGainDbi(kind: PatternKind, elements: Int): Double = when (kind) {
        PatternKind.OMNI -> 2.15
        PatternKind.YAGI -> 1.95 + 5.23 * ln(elements.coerceIn(MIN_ELEMENTS, MAX_ELEMENTS).toDouble())
        PatternKind.MOXON -> 6.0
        PatternKind.PANEL -> 9.0
        PatternKind.BIQUAD -> 10.0
        PatternKind.HELIX -> 12.0
        PatternKind.DISH -> 20.0
        PatternKind.SECTOR -> 14.0
    }

    /** The beamwidth (degrees, 3 dB down) of the kind, from its gain: the higher the gain, the narrower the beam. */
    fun presetBeamwidthDeg(kind: PatternKind, gainDbi: Double): Double = when (kind) {
        PatternKind.OMNI -> 360.0
        PatternKind.MOXON -> 70.0
        PatternKind.SECTOR -> 90.0
        else -> (0.85 * sqrt(41253.0 / 10.0.pow(gainDbi / 10.0))).coerceIn(5.0, 120.0)
    }

    /** The front-to-back ratio (dB) of the kind; more elements of a Yagi suppress the back better. */
    fun presetFrontToBackDb(kind: PatternKind, elements: Int): Double = when (kind) {
        PatternKind.OMNI -> 0.0
        PatternKind.YAGI -> min(25.0, 10.0 + 1.2 * elements.coerceIn(MIN_ELEMENTS, MAX_ELEMENTS))
        PatternKind.MOXON -> 25.0
        PatternKind.PANEL -> 22.0
        PatternKind.BIQUAD -> 15.0
        PatternKind.HELIX -> 15.0
        PatternKind.DISH -> 28.0
        PatternKind.SECTOR -> 25.0
    }

    /** The angle (0 to 180 degrees) between two compass bearings. */
    fun angleBetweenDeg(a: Double, b: Double): Double {
        val d = abs(((a - b) % 360.0 + 540.0) % 360.0 - 180.0)
        return min(180.0, d)
    }

    /**
     * How many dB the antenna radiates less than in its main direction, [offAxisDeg] away from it: 0 on the axis, 3 dB
     * at half the [beamwidthDeg], never more than [frontToBackDb].
     */
    fun lossDb(offAxisDeg: Double, beamwidthDeg: Double, frontToBackDb: Double): Double {
        if (beamwidthDeg >= 360.0 || frontToBackDb <= 0.0) return 0.0
        val x = min(180.0, abs(offAxisDeg)) / max(1.0, beamwidthDeg)
        return min(frontToBackDb, 12.0 * x * x)
    }

    /** The loss in 1-degree steps around the full circle (index = degrees from the main direction). */
    fun lossAround(beamwidthDeg: Double, frontToBackDb: Double): DoubleArray =
        DoubleArray(360) { lossDb(angleBetweenDeg(it.toDouble(), 0.0), beamwidthDeg, frontToBackDb) }

    /** Gain relative to the main direction as a fraction of the field strength (0..1), for drawing a pattern. */
    fun relativeField(lossDb: Double): Double = 10.0.pow(-lossDb / 20.0)
}
