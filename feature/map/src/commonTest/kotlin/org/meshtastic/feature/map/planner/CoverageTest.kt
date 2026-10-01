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
 *
 */
package org.meshtastic.feature.map.planner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoverageTest {
    private val c = GeoPoint(50.8661, 20.6286)

    private fun inp(tx: Double = 20.0, hill: Boolean = false) = CoverageInput(
        center = c, groundAltM = 250.0, antennaHeightM = 15.0, txPowerDbm = tx, antennaGainDbi = 3.0,
        feederLossDb = 1.0, frequencyMHz = 868.0, bandwidthKhz = 125.0, spreadingFactor = 9,
        kFactor = 4.0 / 3.0, surfaceRefractivity = 301.0, maxRangeKm = 40.0, radials = 24, stepM = 200.0,
    )

    private val flat: (Double, Double) -> Double = { _, _ -> 250.0 }

    @Test
    fun flatTerrain() {
        val r = Coverage.compute(inp(), flat)
        assertEquals(24, r.radials)
        assertEquals(40, r.ringsM.size)
        assertEquals(40000.0, r.ringsM.last(), 1e-6)
        assertTrue(r.marginDb[0][0] > 20f, "near=${r.marginDb[0][0]}")
        assertTrue(r.marginDb[0][0] > r.marginDb[0][39])
        // flat terrain is isotropic
        assertEquals(r.maxReachM[0], r.maxReachM[12], 1000.0)
        val m = r.marginAt(c.lat, c.lon)
        assertNotNull(m)
        assertNull(r.marginAt(c.lat + 2.0, c.lon))
    }

    @Test
    fun deterministicAndPowerScaling() {
        val a = Coverage.compute(inp(20.0), flat)
        val b = Coverage.compute(inp(20.0), flat)
        for (i in a.marginDb.indices) assertTrue(a.marginDb[i].contentEquals(b.marginDb[i]))
        val low = Coverage.compute(inp(0.0), flat)
        assertTrue(low.maxReachM[0] < a.maxReachM[0], "low=${low.maxReachM[0]} hi=${a.maxReachM[0]}")
        // 20 dB less power -> margin exactly 20 dB lower
        assertEquals(20.0f, a.marginDb[3][5] - low.marginDb[3][5], 0.01f)
    }

    @Test
    fun hillShadowAndLookup() {
        val hillPoint = Geodesy.destination(c, 90.0, 10000.0)
        val terrain: (Double, Double) -> Double = { lat, lon ->
            val d = Geodesy.distanceM(GeoPoint(lat, lon), hillPoint)
            250.0 + 400.0 * kotlin.math.exp(-(d / 2500.0) * (d / 2500.0))
        }
        val flatR = Coverage.compute(inp(), flat)
        val r = Coverage.compute(inp(), terrain)
        assertTrue(r.maxReachM[6] < flatR.maxReachM[6], "east=${r.maxReachM[6]} flat=${flatR.maxReachM[6]}")
        val p = Geodesy.destination(c, 90.0, 20000.0)
        val shadow = r.marginAt(p.lat, p.lon)
        val open = flatR.marginAt(p.lat, p.lon)
        assertTrue(shadow != null && open != null && shadow < open - 10f, "shadow=$shadow open=$open")
        var calls = 0f
        Coverage.compute(inp().copy(radials = 4, rangeSteps = 5), flat) { calls = it }
        assertEquals(1f, calls, 1e-6f)
    }
}
