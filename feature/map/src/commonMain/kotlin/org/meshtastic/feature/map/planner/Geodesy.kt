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

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** WGS-84 latitude / longitude in degrees. */
data class GeoPoint(val lat: Double, val lon: Double)

/** Spherical geodesy helpers (mean earth radius). */
object Geodesy {
    const val EARTH_RADIUS_M = 6371008.8

    private fun rad(d: Double) = d * PI / 180.0
    private fun deg(r: Double) = r * 180.0 / PI

    /** Haversine great-circle distance in metres. */
    fun distanceM(a: GeoPoint, b: GeoPoint): Double {
        val p1 = rad(a.lat)
        val p2 = rad(b.lat)
        val dp = p2 - p1
        val dl = rad(b.lon - a.lon)
        val h = sin(dp / 2).let { it * it } + cos(p1) * cos(p2) * sin(dl / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))
    }

    /** Initial bearing from [from] to [to], degrees clockwise from true north, 0..360. */
    fun bearingDeg(from: GeoPoint, to: GeoPoint): Double {
        val p1 = rad(from.lat)
        val p2 = rad(to.lat)
        val dl = rad(to.lon - from.lon)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        val b = deg(atan2(y, x))
        return ((b % 360.0) + 360.0) % 360.0
    }

    /** Point reached from [from] travelling [distM] metres on initial bearing [bearingDeg]. */
    fun destination(from: GeoPoint, bearingDeg: Double, distM: Double): GeoPoint {
        val d = distM / EARTH_RADIUS_M
        val br = rad(bearingDeg)
        val p1 = rad(from.lat)
        val l1 = rad(from.lon)
        val sinP2 = sin(p1) * cos(d) + cos(p1) * sin(d) * cos(br)
        val p2 = asin(max(-1.0, min(1.0, sinP2)))
        val l2 = l1 + atan2(sin(br) * sin(d) * cos(p1), cos(d) - sin(p1) * sinP2)
        val lon = ((deg(l2) + 540.0) % 360.0) - 180.0
        return GeoPoint(deg(p2), lon)
    }

    /** Point at [fraction] (0..1) along the great circle from [a] to [b]. */
    fun interpolate(a: GeoPoint, b: GeoPoint, fraction: Double): GeoPoint {
        val delta = distanceM(a, b) / EARTH_RADIUS_M
        if (delta < 1e-12) return a
        val p1 = rad(a.lat)
        val l1 = rad(a.lon)
        val p2 = rad(b.lat)
        val l2 = rad(b.lon)
        val sd = sin(delta)
        val ka = sin((1 - fraction) * delta) / sd
        val kb = sin(fraction * delta) / sd
        val x = ka * cos(p1) * cos(l1) + kb * cos(p2) * cos(l2)
        val y = ka * cos(p1) * sin(l1) + kb * cos(p2) * sin(l2)
        val z = ka * sin(p1) + kb * sin(p2)
        return GeoPoint(deg(atan2(z, sqrt(x * x + y * y))), deg(atan2(y, x)))
    }

    /**
     * Antenna elevation angle (degrees, positive = up) at a station at [fromAltM] pointing to a station at
     * [toAltM] [distM] away, including earth-curvature drop d^2/(2kR) of the far end; terrain is ignored.
     */
    fun elevationAngleDeg(fromAltM: Double, toAltM: Double, distM: Double, k: Double): Double {
        if (distM <= 0.0) return 0.0
        val drop = distM * distM / (2.0 * k * EARTH_RADIUS_M)
        return deg(atan2(toAltM - fromAltM - drop, distM))
    }
}
