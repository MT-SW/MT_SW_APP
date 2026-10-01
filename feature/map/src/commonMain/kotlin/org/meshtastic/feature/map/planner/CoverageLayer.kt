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

import org.meshtastic.feature.map.planner.export.Num
import kotlin.math.PI
import kotlin.math.cos

/**
 * Map-layer GeoJSON of a coverage prediction: a fine grid of tiny semi-transparent dots (small squares with gaps between them) (simplestyle `fill` /
 * `fill-opacity`, rendered by both map flavours). Only cells with a non-negative margin are drawn.
 */
object CoverageLayer {
    /** Lower margin bound (dB) of each class; the last class is open ended. */
    val classBoundsDb = floatArrayOf(0f, 3f, 6f, 10f, 15f, 20f, 30f)

    /** Weakest → strongest (plasma-like palette). */
    val classColors = arrayOf("#4b0c6b", "#781c6d", "#a52c60", "#cf4446", "#ed6925", "#fb9b06", "#f7d13d")

    const val FILL_OPACITY = 0.5

    /** Side of a dot relative to its grid cell; the rest stays empty so the base map shows through. */
    private const val DOT_FRACTION = 0.5

    /** Target number of grid cells across the coverage diameter. */
    private const val GRID_CELLS = 180

    fun classOf(marginDb: Float): Int {
        if (marginDb.isNaN() || marginDb < classBoundsDb[0]) return -1
        var c = 0
        for (i in classBoundsDb.indices) if (marginDb >= classBoundsDb[i]) c = i
        return c
    }

    /**
     * Fine grid of tiny semi-transparent dots: only places the signal really reaches get one, so terrain shadows
     * (hills, valleys) stay empty and the base map stays visible between the dots.
     */
    fun render(coverage: CoverageResult, name: String): String {
        val features = ArrayList<String>()
        val radiusM = coverage.ringsM.lastOrNull() ?: 0.0
        if (radiusM > 0.0 && coverage.radials >= 3) {
            val cellM = radiusM * 2.0 / GRID_CELLS
            val dLat = cellM / METERS_PER_DEG_LAT
            val cosLat = cos(coverage.center.lat * PI / 180.0).coerceAtLeast(0.05)
            val dLon = cellM / (METERS_PER_DEG_LAT * cosLat)
            val rows = GRID_CELLS
            val lat0 = coverage.center.lat - dLat * rows / 2.0
            val lon0 = coverage.center.lon - dLon * rows / 2.0
            val insetLat = dLat * (1.0 - DOT_FRACTION) / 2.0
            val insetLon = dLon * (1.0 - DOT_FRACTION) / 2.0
            for (row in 0 until rows) {
                val latS = lat0 + dLat * row
                val latC = latS + dLat / 2.0
                for (col in 0 until rows) {
                    val lonW = lon0 + dLon * col
                    val cls = classOf(coverage.marginAt(latC, lonW + dLon / 2.0) ?: Float.NaN)
                    if (cls < 0) continue
                    features.add(
                        cell(latS + insetLat, latS + dLat - insetLat, lonW + insetLon, lonW + dLon - insetLon, cls),
                    )
                }
            }
        }
        features.add(
            "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":" +
                pos(coverage.center.lat, coverage.center.lon) + "},\"properties\":{\"name\":" + str(name) +
                ",\"marker-color\":\"#1565c0\"}}",
        )
        return "{\"type\":\"FeatureCollection\",\"name\":" + str(name) + ",\"features\":[\n" +
            features.joinToString(",\n") + "\n]}\n"
    }

    private fun cell(south: Double, north: Double, west: Double, east: Double, cls: Int): String {
        val ring = pos(south, west) + "," + pos(south, east) + "," + pos(north, east) + "," + pos(north, west) + "," +
            pos(south, west)
        return "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[" + ring +
            "]]},\"properties\":{\"fill\":\"" + classColors[cls] + "\",\"fill-opacity\":" + FILL_OPACITY.toString() +
            ",\"stroke-width\":0,\"stroke-opacity\":0,\"class\":" + cls + "}}"
    }

    private const val METERS_PER_DEG_LAT = 111_320.0

    private fun pos(lat: Double, lon: Double) = "[" + Num.fmt(lon, 5) + "," + Num.fmt(lat, 5) + "]"

    private fun str(s: String): String {
        val sb = StringBuilder("\"")
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                else -> sb.append(ch)
            }
        }
        return sb.append('"').toString()
    }
}
