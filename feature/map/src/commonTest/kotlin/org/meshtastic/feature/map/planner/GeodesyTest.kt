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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeodesyTest {
    private val kielce = GeoPoint(50.8661, 20.6286)
    private val warsaw = GeoPoint(52.2297, 21.0122)

    @Test
    fun distanceAndBearing() {
        val d = Geodesy.distanceM(kielce, warsaw)
        assertTrue(d in 153000.0..155000.0, "d=$d")
        val b = Geodesy.bearingDeg(kielce, warsaw)
        assertTrue(b in 9.0..11.0, "b=$b")
        val back = Geodesy.bearingDeg(warsaw, kielce)
        assertTrue(back in 189.0..191.0, "back=$back")
        assertEquals(0.0, Geodesy.distanceM(kielce, kielce), 1e-9)
    }

    @Test
    fun destinationRoundTrip() {
        val p = Geodesy.destination(kielce, 45.0, 25000.0)
        assertEquals(25000.0, Geodesy.distanceM(kielce, p), 1.0)
        assertEquals(45.0, Geodesy.bearingDeg(kielce, p), 0.05)
        val w = Geodesy.destination(kielce, 270.0, 1000.0)
        assertTrue(w.lon < kielce.lon)
    }

    @Test
    fun interpolateMidpoint() {
        val m = Geodesy.interpolate(kielce, warsaw, 0.5)
        val d = Geodesy.distanceM(kielce, warsaw)
        assertEquals(d / 2, Geodesy.distanceM(kielce, m), 1.0)
        assertEquals(d / 2, Geodesy.distanceM(m, warsaw), 1.0)
        val s = Geodesy.interpolate(kielce, warsaw, 0.0)
        assertEquals(kielce.lat, s.lat, 1e-9)
    }

    @Test
    fun elevationAngle() {
        // equal heights, 20 km, k=4/3: drop = 20000^2/(2*1.3333*6371008.8) = 23.5 m
        val e = Geodesy.elevationAngleDeg(100.0, 100.0, 20000.0, 4.0 / 3.0)
        assertEquals(-0.0673, e, 0.002)
        assertTrue(Geodesy.elevationAngleDeg(0.0, 500.0, 1000.0, 1.3) > 25.0)
        assertEquals(0.0, Geodesy.elevationAngleDeg(0.0, 100.0, 0.0, 1.3), 1e-12)
    }
}
