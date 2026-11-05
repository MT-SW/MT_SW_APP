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
package org.meshtastic.feature.map.horizon

import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.Geodesy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HorizonPanoramaTest {
    private val centre = GeoPoint(50.0, 20.0)

    /** A ridge 100 m higher than the plain between 4 and 5 km east of the centre. */
    private val ridgeEast: (Double, Double) -> Double = { lat, lon ->
        val d = Geodesy.distanceM(centre, GeoPoint(lat, lon))
        val east = Geodesy.bearingDeg(centre, GeoPoint(lat, lon)) in 60.0..120.0
        if (east && d in 4_000.0..5_000.0) 300.0 else 200.0
    }

    @Test
    fun flatGroundHasNoSkylineAboveTheHorizontal() {
        val profile =
            HorizonPanorama.build({ _, _ -> 200.0 }, centre, antennaM = 2.0, radiusM = 10_000.0, bearings = 36)
        assertTrue(profile.points.all { it.angleDeg < 0.0 })
    }

    @Test
    fun aRidgeRaisesTheSkylineOnlyWhereItStands() {
        val profile = HorizonPanorama.build(ridgeEast, centre, antennaM = 2.0, radiusM = 10_000.0, bearings = 72)
        assertTrue(profile.at(90.0).angleDeg > 1.0)
        assertTrue(profile.at(270.0).angleDeg < 0.0)
    }

    @Test
    fun aNodeBehindTheRidgeIsHiddenAndOneAboveItIsSeen() {
        val profile = HorizonPanorama.build(ridgeEast, centre, antennaM = 2.0, radiusM = 10_000.0, bearings = 72)
        val behind = Geodesy.destination(centre, 90.0, 8_000.0)
        val beside = Geodesy.destination(centre, 270.0, 8_000.0)
        val placed =
            HorizonPanorama.place(
                profile,
                ridgeEast,
                listOf(Triple(1, "behind", behind), Triple(2, "beside", beside)),
                targetAntennaM = 2.0,
            )
        assertFalse(placed.first { it.nodeNum == 1 }.visible)
        assertTrue(placed.first { it.nodeNum == 2 }.visible)
    }

    @Test
    fun nodesBeyondTheRadiusAreLeftOut() {
        val profile = HorizonPanorama.build({ _, _ -> 0.0 }, centre, antennaM = 2.0, radiusM = 5_000.0, bearings = 12)
        val far = Geodesy.destination(centre, 0.0, 9_000.0)
        assertTrue(HorizonPanorama.place(profile, { _, _ -> 0.0 }, listOf(Triple(1, "far", far)), 2.0).isEmpty())
    }

    @Test
    fun bearingGapWrapsAround() {
        assertEquals(20.0, HorizonPanorama.bearingGap(350.0, 10.0), 1e-9)
    }
}
