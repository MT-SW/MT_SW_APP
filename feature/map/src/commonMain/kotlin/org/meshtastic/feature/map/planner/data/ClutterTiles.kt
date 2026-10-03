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
package org.meshtastic.feature.map.planner.data

import org.meshtastic.feature.map.planner.export.Num
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max

/** A latitude/longitude rectangle (degrees) for which obstacle data is requested in one piece. */
data class ClutterBox(val south: Double, val west: Double, val north: Double, val east: Double) {
    val heightKm: Double get() = (north - south) * KM_PER_DEG_LAT

    val widthKm: Double
        get() = (east - west) * KM_PER_DEG_LON_AT_EQUATOR * max(MIN_COS, cos((north + south) / 2.0 * PI / 180.0))

    val maxSideKm: Double get() = max(heightKm, widthKm)

    /** The four quarters of this box (used to split a box the server found too heavy). */
    fun quarters(): List<ClutterBox> {
        val midLat = (south + north) / 2.0
        val midLon = (west + east) / 2.0
        return listOf(
            ClutterBox(south, west, midLat, midLon),
            ClutterBox(south, midLon, midLat, east),
            ClutterBox(midLat, west, north, midLon),
            ClutterBox(midLat, midLon, north, east),
        )
    }

    /** `south,west,north,east` as Overpass wants it. */
    fun asQueryBox(): String =
        Num.fmt(south, 5) + "," + Num.fmt(west, 5) + "," + Num.fmt(north, 5) + "," + Num.fmt(east, 5)

    companion object {
        const val KM_PER_DEG_LAT = 110.574
        const val KM_PER_DEG_LON_AT_EQUATOR = 111.320
        private const val MIN_COS = 0.05

        /** The square of half-side [radiusKm] around a point (the same maths the planner always used). */
        fun around(lat: Double, lon: Double, radiusKm: Double): ClutterBox {
            val dLat = radiusKm / KM_PER_DEG_LAT
            val dLon = radiusKm / (KM_PER_DEG_LON_AT_EQUATOR * max(MIN_COS, cos(lat * PI / 180.0)))
            return ClutterBox(lat - dLat, lon - dLon, lat + dLat, lon + dLon)
        }
    }
}

object ClutterTiles {
    /** [box] cut into an even grid whose tiles are at most about [maxSideKm] on a side (at least one tile). */
    fun grid(box: ClutterBox, maxSideKm: Double): List<ClutterBox> {
        val rows = ceil(box.heightKm / maxSideKm).toInt().coerceAtLeast(1)
        val cols = ceil(box.widthKm / maxSideKm).toInt().coerceAtLeast(1)
        val dLat = (box.north - box.south) / rows
        val dLon = (box.east - box.west) / cols
        val out = ArrayList<ClutterBox>(rows * cols)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                out.add(
                    ClutterBox(
                        box.south + dLat * r,
                        box.west + dLon * c,
                        box.south + dLat * (r + 1),
                        box.west + dLon * (c + 1),
                    ),
                )
            }
        }
        return out
    }
}
