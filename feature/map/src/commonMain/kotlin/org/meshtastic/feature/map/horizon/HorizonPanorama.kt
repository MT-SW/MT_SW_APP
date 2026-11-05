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
@file:Suppress("MagicNumber", "MaxLineLength")

package org.meshtastic.feature.map.horizon

import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.Geodesy
import kotlin.math.abs
import kotlin.math.atan2

/*
 * The 360° skyline from one station, with nodes placed against it. The method follows the Horizon view of MeshRF
 * (https://github.com/Ixitxachitl/MeshRF, GPL-3.0-or-later, by Ixitxachitl): for every bearing the ground is sampled
 * outwards, the elevation angle of each sample (with the earth falling away) is computed, and the running maximum is the
 * skyline. Geometry only: a node above the skyline can still be out of range, and one just below it can be heard by
 * diffraction; the link profile of the planner is the authority on either.
 */

/** A ridge that stood as the skyline for a while and was then overtopped by something further out. */
data class HorizonCrest(val angleDeg: Double, val distanceM: Double)

/**
 * The skyline in one direction: [angleDeg] above the horizontal at [distanceM], with every [crests] seen on the way.
 */
data class HorizonPoint(
    val bearingDeg: Double,
    val angleDeg: Double,
    val distanceM: Double,
    val crests: List<HorizonCrest>,
)

/** The skyline all round: [points] in order of bearing, one every `360 / points.size` degrees. */
class HorizonProfile(
    val centre: GeoPoint,
    val observerM: Double,
    val radiusM: Double,
    val stepM: Double,
    val points: List<HorizonPoint>,
) {
    /** The point whose bearing is nearest [bearingDeg] (any number of degrees, it wraps). */
    fun at(bearingDeg: Double): HorizonPoint {
        val wrapped = ((bearingDeg % FULL) + FULL) % FULL
        return points[(wrapped / FULL * points.size).toInt().coerceIn(0, points.size - 1)]
    }

    private companion object {
        const val FULL = 360.0
    }
}

/** A node placed against the skyline: [visible] when it stands above everything between it and the station. */
data class HorizonTarget(
    val nodeNum: Int,
    val name: String,
    val bearingDeg: Double,
    val distanceM: Double,
    val angleDeg: Double,
    val occludingDeg: Double,
) {
    val visible: Boolean
        get() = angleDeg > occludingDeg

    /** How far above (positive) or below (negative) the ridge in front of it the node stands, in degrees. */
    val clearanceDeg: Double
        get() = angleDeg - occludingDeg
}

object HorizonPanorama {
    const val DEFAULT_BEARINGS = 720
    const val DEFAULT_SAMPLES = 400
    const val DEFAULT_K = 4.0 / 3.0
    private const val FULL = 360.0

    /**
     * The skyline from [centre] with the antenna [antennaM] above the ground, as far as [radiusM]. [ground] gives the
     * terrain height in metres (sea and missing data as 0), see `PlannerElevationSource.prepareArea`.
     */
    fun build(
        ground: (lat: Double, lon: Double) -> Double,
        centre: GeoPoint,
        antennaM: Double,
        radiusM: Double,
        bearings: Int = DEFAULT_BEARINGS,
        samples: Int = DEFAULT_SAMPLES,
        k: Double = DEFAULT_K,
    ): HorizonProfile {
        require(bearings >= 3) { "a panorama needs at least three bearings" }
        require(radiusM > 0) { "the radius has to be positive" }
        val centreGround = ground(centre.lat, centre.lon)
        val observer = centreGround + antennaM
        val effectiveRadius = Geodesy.EARTH_RADIUS_M * k
        val spacing = radiusM / samples
        val points =
            List(bearings) { index ->
                val bearing = FULL * index / bearings
                var bestAngle = Double.NEGATIVE_INFINITY
                var bestDistance = radiusM
                val crests = ArrayList<HorizonCrest>()
                var standing = false
                for (step in 1..samples) {
                    val distance = step * spacing
                    val at = Geodesy.destination(centre, bearing, distance)
                    val angle = angleDeg(observer, ground(at.lat, at.lon), distance, effectiveRadius)
                    if (angle <= bestAngle) {
                        // Lower ground after a rise: whatever raised it last is an edge seen against the country
                        // beyond.
                        if (standing) {
                            crests += HorizonCrest(bestAngle, bestDistance)
                            standing = false
                        }
                        continue
                    }
                    bestAngle = angle
                    bestDistance = distance
                    standing = true
                }
                // A direction with no ground standing in it reports the geometric horizon of a featureless earth.
                if (bestAngle == Double.NEGATIVE_INFINITY) {
                    bestAngle = angleDeg(observer, centreGround, radiusM, effectiveRadius)
                }
                // The sweep ended on rising ground: the skyline itself is not in the list yet.
                if (standing || crests.isEmpty()) crests += HorizonCrest(bestAngle, bestDistance)
                HorizonPoint(bearing, bestAngle, bestDistance, crests)
            }
        return HorizonProfile(centre, observer, radiusM, spacing, points)
    }

    /** The angle above the horizontal at which ground of height [targetM] at [distanceM] is seen from [observerM]. */
    fun angleDeg(observerM: Double, targetM: Double, distanceM: Double, effectiveRadiusM: Double): Double {
        val drop = distanceM * distanceM / (2 * effectiveRadiusM)
        return atan2(targetM - drop - observerM, distanceM) * 180.0 / kotlin.math.PI
    }

    /**
     * Places [nodes] (number, name, position) against [profile]; [targetAntennaM] is the height of their antennas above
     * the ground. Nodes at the station itself or beyond [maxDistanceM] are left out; the terrain hiding a node is taken
     * only from inside the profile radius.
     */
    fun place(
        profile: HorizonProfile,
        ground: (lat: Double, lon: Double) -> Double,
        nodes: List<Triple<Int, String, GeoPoint>>,
        targetAntennaM: Double,
        k: Double = DEFAULT_K,
        maxDistanceM: Double = profile.radiusM,
    ): List<HorizonTarget> {
        val effectiveRadius = Geodesy.EARTH_RADIUS_M * k
        return nodes.mapNotNull { (num, name, at) ->
            val distance = Geodesy.distanceM(profile.centre, at)
            if (distance <= 0 || distance > maxDistanceM) return@mapNotNull null
            val bearing = Geodesy.bearingDeg(profile.centre, at)
            val angle = angleDeg(profile.observerM, ground(at.lat, at.lon) + targetAntennaM, distance, effectiveRadius)
            var highest = Double.NEGATIVE_INFINITY
            var d = profile.stepM
            // Terrain is known only as far as the profile reaches; farther nodes are hidden only by what lies inside
            // it.
            val known = minOf(distance, profile.radiusM)
            while (d < known) {
                val p = Geodesy.destination(profile.centre, bearing, d)
                val a = angleDeg(profile.observerM, ground(p.lat, p.lon), d, effectiveRadius)
                if (a > highest) highest = a
                d += profile.stepM
            }
            HorizonTarget(num, name, bearing, distance, angle, highest)
        }
    }

    /** The smallest difference between two bearings, 0..180. */
    fun bearingGap(a: Double, b: Double): Double {
        val d = abs(((a - b) % FULL + FULL) % FULL)
        return if (d > FULL / 2) FULL - d else d
    }
}
