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
    fun arrowPointsTheWayTheHopGoes() {
        assertEquals("↑", edge(50.1, 20.0).arrow)
        assertEquals("→", edge(50.0, 20.1).arrow)
        assertEquals("↓", edge(49.9, 20.0).arrow)
        assertEquals("←", edge(50.0, 19.9).arrow)
    }

    @Test
    fun labelShowsSnrOrQuestionMark() {
        assertEquals("→ -3.5 dB", edge(50.0, 20.1, -3.5f).label)
        assertEquals("→ ?", edge(50.0, 20.1, null).label)
    }

    @Test
    fun eastWestHopsAreSeparatedVertically() {
        assertTrue(edge(50.0, 20.1).isMostlyEastWest)
        assertFalse(edge(50.1, 20.0).isMostlyEastWest)
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
