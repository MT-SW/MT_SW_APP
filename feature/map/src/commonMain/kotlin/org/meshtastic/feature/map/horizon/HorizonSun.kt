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

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Where the sun stands: [azimuthDeg] clockwise from north, [elevationDeg] above the horizon (negative at night). */
data class SunPosition(val azimuthDeg: Double, val elevationDeg: Double)

/** The position of the sun for a place and a moment (the usual low-precision almanac formulas, about a degree). */
object HorizonSun {
    private fun rad(d: Double) = d * PI / 180.0

    private fun deg(r: Double) = r * 180.0 / PI

    fun at(latDeg: Double, lonDeg: Double, epochSeconds: Long): SunPosition {
        val n = epochSeconds / 86400.0 + 2440587.5 - 2451545.0
        val meanLon = (280.46 + 0.9856474 * n) % 360.0
        val anomaly = rad((357.528 + 0.9856003 * n) % 360.0)
        val eclipticLon = rad(meanLon + 1.915 * sin(anomaly) + 0.020 * sin(2 * anomaly))
        val obliquity = rad(23.439 - 0.0000004 * n)
        val rightAscension = atan2(cos(obliquity) * sin(eclipticLon), cos(eclipticLon))
        val declination = asin(sin(obliquity) * sin(eclipticLon))
        val siderealDeg = (280.46061837 + 360.98564736629 * n + lonDeg) % 360.0
        val hourAngle = rad(siderealDeg) - rightAscension
        val lat = rad(latDeg)
        val elevation = asin(sin(lat) * sin(declination) + cos(lat) * cos(declination) * cos(hourAngle))
        val azimuth =
            atan2(
                -sin(hourAngle) * cos(declination),
                sin(declination) * cos(lat) - cos(declination) * sin(lat) * cos(hourAngle),
            )
        return SunPosition((deg(azimuth) + 360.0) % 360.0, deg(elevation))
    }
}
