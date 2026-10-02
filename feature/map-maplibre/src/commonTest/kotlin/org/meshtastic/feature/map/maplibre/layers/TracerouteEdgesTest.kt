/*
 * Copyright (c) 2026 Meshtastic LLC
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
package org.meshtastic.feature.map.maplibre.layers

import org.maplibre.spatialk.geojson.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TracerouteEdgesTest {
    private fun edge(toLat: Double, toLon: Double, snr: Float? = null) =
        TracerouteEdge(from = Position(longitude = 20.0, latitude = 50.0), to = Position(longitude = toLon, latitude = toLat), snrDb = snr)

    @Test
    fun arrowRotationFollowsTheHop() {
        assertEquals(0.0, edge(50.0, 20.1).arrowRotationDeg, 1e-6)
        assertEquals(-90.0, edge(50.1, 20.0).arrowRotationDeg, 1e-6)
        assertEquals(90.0, edge(49.9, 20.0).arrowRotationDeg, 1e-6)
    }

    @Test
    fun labelNeverRunsUpsideDown() {
        val east = edge(50.0, 20.1)
        assertEquals(0.0, east.labelRotationDeg, 1e-6)
        assertFalse(east.labelFlipped)
        val west = edge(50.0, 19.9)
        assertEquals(0.0, west.labelRotationDeg, 1e-6)
        assertTrue(west.labelFlipped)
    }

    @Test
    fun labelShowsSnrOrQuestionMark() {
        assertEquals("-3.5 dB", edge(50.0, 20.1, -3.5f).label)
        assertEquals("?", edge(50.0, 20.1, null).label)
    }

    @Test
    fun lineStopsShortOfBothNodes() {
        val e = edge(50.0, 21.0)
        assertTrue(e.lineStart.longitude > 20.0)
        assertTrue(e.lineEnd.longitude < 21.0)
        assertTrue(e.lineEnd.longitude > e.lineStart.longitude)
    }

    @Test
    fun midpointIsBetweenTheEnds() {
        val m = edge(50.2, 20.4).midpoint
        assertEquals(50.1, m.latitude, 1e-9)
        assertEquals(20.2, m.longitude, 1e-9)
    }

    @Test
    fun unknownSnrIsNull() {
        assertNull(edge(50.0, 20.1, null).snrDb)
    }
}
