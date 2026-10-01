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
 * Map-layer GeoJSON of a coverage prediction as a continuous raster: a fine grid of small, adjacent, semi-transparent
 * squares coloured with a smooth purple → orange → yellow scale (simplestyle `fill` / `fill-opacity`, rendered by both
 * map flavours). Only cells with a non-negative margin are drawn, so terrain shadows stay empty and the base map shows
 * through. Neighbouring cells of the same colour are merged into one polygon to keep the layer light.
 */
object CoverageLayer {
    /** Margin (dB) at which the scale reaches its strongest colour. */
    const val MAX_DB = 30f

    /** Number of colour steps between 0 dB and [MAX_DB]. */
    const val CLASS_COUNT = 24

    const val FILL_OPACITY = 0.62

    /** Target number of grid cells across the coverage diameter. */
    private const val GRID_CELLS = 280

    // Weakest → strongest, picked to match the familiar MeshMap look.
    private val stopPos = doubleArrayOf(0.0, 0.3, 0.55, 0.78, 1.0)
    private val stopRgb = arrayOf(
        intArrayOf(0x7a, 0x3c, 0xb5),
        intArrayOf(0xb6, 0x4f, 0xa8),
        intArrayOf(0xee, 0x7f, 0x5c),
        intArrayOf(0xfb, 0xa7, 0x3a),
        intArrayOf(0xe9, 0xf0, 0x3b),
    )

    fun classOf(marginDb: Float): Int {
        if (marginDb.isNaN() || marginDb < 0f) return -1
        val t = (marginDb / MAX_DB).coerceIn(0f, 1f)
        return (t * (CLASS_COUNT - 1) + 0.5f).toInt().coerceIn(0, CLASS_COUNT - 1)
    }

    /** `#rrggbb` of colour step [cls]. */
    fun colorOf(cls: Int): String {
        val t = cls.coerceIn(0, CLASS_COUNT - 1).toDouble() / (CLASS_COUNT - 1)
        var i = 0
        while (i < stopPos.size - 2 && t > stopPos[i + 1]) i++
        val f = ((t - stopPos[i]) / (stopPos[i + 1] - stopPos[i])).coerceIn(0.0, 1.0)
        val sb = StringBuilder("#")
        for (c in 0..2) {
            val v = (stopRgb[i][c] + (stopRgb[i + 1][c] - stopRgb[i][c]) * f + 0.5).toInt().coerceIn(0, 255)
            sb.append(HEX[v shr 4]).append(HEX[v and 15])
        }
        return sb.toString()
    }

    private const val HEX = "0123456789abcdef"

    fun render(coverage: CoverageResult, name: String, opacity: Double = FILL_OPACITY): String {
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
            for (row in 0 until rows) {
                val latS = lat0 + dLat * row
                val latC = latS + dLat / 2.0
                var col = 0
                while (col < rows) {
                    val cls = classOf(coverage.marginAt(latC, lon0 + dLon * (col + 0.5)) ?: Float.NaN)
                    if (cls < 0) {
                        col++
                        continue
                    }
                    var end = col
                    while (end + 1 < rows &&
                        classOf(coverage.marginAt(latC, lon0 + dLon * (end + 1.5)) ?: Float.NaN) == cls
                    ) {
                        end++
                    }
                    features.add(cell(latS, latS + dLat, lon0 + dLon * col, lon0 + dLon * (end + 1), cls, opacity))
                    col = end + 1
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

    private fun cell(south: Double, north: Double, west: Double, east: Double, cls: Int, opacity: Double): String {
        val ring = pos(south, west) + "," + pos(south, east) + "," + pos(north, east) + "," + pos(north, west) + "," +
            pos(south, west)
        return "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[" + ring +
            "]]},\"properties\":{\"fill\":\"" + colorOf(cls) + "\",\"fill-opacity\":" + opacity.toString() +
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
