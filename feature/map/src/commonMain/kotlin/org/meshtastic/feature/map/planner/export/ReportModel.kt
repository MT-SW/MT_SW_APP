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
package org.meshtastic.feature.map.planner.export

/** One "label: value" row; both strings are already localized by the caller. */
data class ReportKeyValue(val label: String, val value: String)

/** A titled two-column table. */
data class ReportSection(val title: String, val rows: List<ReportKeyValue>)

/**
 * Terrain profile. All arrays are in metres (distance from A, heights above the same datum) and should have equal
 * length (the shortest one wins). Axis tick values are printed as plain numbers in these units, so the axis labels
 * supplied by the caller should name metres.
 */
data class ReportProfile(
    val distancesM: DoubleArray,
    val groundM: DoubleArray,
    val losM: DoubleArray,
    val fresnelUpperM: DoubleArray,
    val fresnelLowerM: DoubleArray,
    val labelA: String,
    val labelB: String,
    val xAxisLabel: String,
    val yAxisLabel: String,
)

data class ReportPoint(val name: String, val lat: Double, val lon: Double, val description: String = "")

/** One coverage sample (grid cell centre). */
data class ReportCoverageCell(val lat: Double, val lon: Double, val marginDb: Float)

/**
 * Coverage plan. [legend] is ordered from the best (highest margin) class to the worst. A sample with margin m falls
 * into legend entry i, where i is the first index with `m >= thresholdsDb[i]` (the last entry catches everything
 * below). When [thresholdsDb] has fewer than `legend.size - 1` values the defaults 20, 10, 0, -10 ... dB are used.
 * Legend colours are ARGB; alpha 0 is treated as opaque.
 */
data class ReportCoverage(
    val centerLat: Double,
    val centerLon: Double,
    val samples: List<ReportCoverageCell>,
    val maxRangeKm: Double,
    val legend: List<Pair<String, Int>>,
    val title: String,
    val thresholdsDb: List<Float> = emptyList(),
)

data class PlannerReport(
    val title: String,
    val subtitle: String,
    val generatedAt: String,
    val sections: List<ReportSection>,
    val profile: ReportProfile?,
    val coverage: ReportCoverage?,
    val points: List<ReportPoint>,
    /** Disclaimer, licences, weather date etc. */
    val notes: List<String>,
    val footer: String,
)
