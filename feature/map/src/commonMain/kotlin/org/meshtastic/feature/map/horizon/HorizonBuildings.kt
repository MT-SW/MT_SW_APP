/*
 * Copyright (c) 2025-2026 Meshtastic LLC
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

package org.meshtastic.feature.map.horizon

import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.Geodesy
import org.meshtastic.feature.map.planner.data.ClutterHeights
import org.meshtastic.feature.map.planner.data.ClutterKind
import org.meshtastic.feature.map.planner.data.ClutterMap
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * One building as seen from the station: its outline corner by corner, each with its bearing and the angle of its foot
 * and of its roof above the horizontal, so the panorama can draw the real walls (at an angle to the viewer) and the
 * roof instead of columns. [distanceM] is the distance of the nearest corner, used to draw far ones first.
 */
@Suppress("ArrayInDataClass")
class HorizonBuilding(
    val bearingsDeg: DoubleArray,
    val footDeg: DoubleArray,
    val roofDeg: DoubleArray,
    /** Distance of every corner (m), to tell which walls face the viewer. */
    val cornerM: DoubleArray,
    val distanceM: Double,
)

/** Projects the saved buildings around [centre] into [HorizonBuilding]s (nearest [MAX_BUILDINGS], within [maxM]). */
object HorizonBuildings {
    private const val MAX_BUILDINGS = 2500
    private const val METRES_PER_DEGREE = 111_320.0
    // Closer than this a building would only fill the picture with one huge wall; the own roof and the street are skipped.
    private const val OWN_BUILDING_M = 100.0
    private const val MIN_HEIGHT_M = 2.0

    @Suppress("LoopWithTooManyJumpStatements")
    fun project(
        buildings: ClutterMap,
        centre: GeoPoint,
        observerM: Double,
        maxM: Double,
        k: Double,
        ground: (lat: Double, lon: Double) -> Double,
    ): List<HorizonBuilding> {
        val heights = ClutterHeights()
        val effective = Geodesy.EARTH_RADIUS_M * k
        val metresPerLon = METRES_PER_DEGREE * cos(centre.lat * PI / 180.0)
        val out = ArrayList<HorizonBuilding>()
        for (p in buildings.polygons) {
            if (p.kind != ClutterKind.BUILDING) continue
            val ring = p.outer
            val n = ring.size / 2
            if (n < 3) continue
            val height = p.heightM(heights)
            if (height < MIN_HEIGHT_M) continue
            val bearings = DoubleArray(n)
            val foot = DoubleArray(n)
            val roof = DoubleArray(n)
            val corner = DoubleArray(n)
            var nearest = Double.MAX_VALUE
            val roofZ = ground((p.minLat + p.maxLat) / 2, (p.minLon + p.maxLon) / 2) + height
            for (i in 0 until n) {
                val lat = ring[2 * i]
                val lon = ring[2 * i + 1]
                val dy = (lat - centre.lat) * METRES_PER_DEGREE
                val dx = (lon - centre.lon) * metresPerLon
                val d = sqrt(dx * dx + dy * dy)
                if (d < nearest) nearest = d
                corner[i] = d
                bearings[i] = (atan2(dx, dy) * 180.0 / PI + 360.0) % 360.0
                foot[i] = HorizonPanorama.angleDeg(observerM, ground(lat, lon), d, effective)
                roof[i] = HorizonPanorama.angleDeg(observerM, roofZ, d, effective)
            }
            if (nearest < OWN_BUILDING_M || nearest > maxM) continue
            out += HorizonBuilding(bearings, foot, roof, corner, nearest)
        }
        return out.sortedBy { it.distanceM }.take(MAX_BUILDINGS)
    }
}
