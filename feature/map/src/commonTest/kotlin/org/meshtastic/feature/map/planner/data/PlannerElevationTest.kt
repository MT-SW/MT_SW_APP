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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.terrain.ElevationTile
import org.meshtastic.feature.map.terrain.MapterhornEndpoints
import org.meshtastic.feature.map.terrain.TerrainTileFetcher
import org.meshtastic.feature.map.terrain.TileIndex
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sinh
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeFetcher(var fail: Boolean = false, var absent: Boolean = false) : TerrainTileFetcher {
    var calls = 0

    override fun fetchTile(zoom: Int, x: Int, y: Int): ByteArray? {
        calls++
        if (fail) throw Exception("offline")
        return if (absent) null else ByteArray(1)
    }

    override fun close() {}
}

class PlannerElevationTest {
    /** 2x2 tile with a west-east ramp: left column 100 m, right column 200 m. */
    private fun ramp() = ElevationTile(2, 2, floatArrayOf(100f, 200f, 100f, 200f))

    private fun elevation(fetcher: FakeFetcher, tile: ElevationTile? = ramp()) = PlannerElevation(
        fetcherFactory = { fetcher },
        tileDecoder = { tile ?: throw IllegalStateException("bad") },
        ioContext = Dispatchers.Unconfined,
    )

    @Test
    fun altitudeIsBilinearAndCached() = runTest {
        val f = FakeFetcher()
        val e = elevation(f)
        val a = e.altitudeAt(50.0, 20.0)
        val b = e.altitudeAt(50.0, 20.0)
        assertEquals(a, b)
        assertEquals(1, f.calls)
        assertTrue(a != null && a in 100.0..200.0, "a=$a")
    }

    @Test
    fun missingTileIsNullAltitudeAndZeroInProfile() = runTest {
        val e = elevation(FakeFetcher(absent = true))
        assertNull(e.altitudeAt(50.0, 20.0))
        val p = e.profile(GeoPoint(50.0, 20.0), GeoPoint(50.0, 20.1))
        assertTrue(p.groundM.all { it == 0.0 })
    }

    @Test
    fun networkFailureIsTyped() = runTest {
        val e = elevation(FakeFetcher(fail = true))
        val ex = assertFailsWith<PlannerElevationException> { e.altitudeAt(50.0, 20.0) }
        assertEquals(PlannerElevationFailure.NETWORK, ex.failure)
    }

    @Test
    fun decodeFailureIsTyped() = runTest {
        val e = elevation(FakeFetcher(), tile = null)
        val ex = assertFailsWith<PlannerElevationException> { e.profile(GeoPoint(50.0, 20.0), GeoPoint(50.0, 20.05)) }
        assertEquals(PlannerElevationFailure.DECODE, ex.failure)
    }

    @Test
    fun profileAndAreaSamplerUseTiles() = runTest {
        val e = elevation(FakeFetcher())
        val p = e.profile(GeoPoint(50.0, 20.0), GeoPoint(50.0, 20.05))
        assertTrue(p.intervals >= 2)
        assertTrue(p.groundM.all { it in 100.0..200.0 })
        val sampler = e.prepareArea(GeoPoint(50.0, 20.0), 3.0)
        assertTrue(sampler(50.0, 20.0) in 100.0..200.0)
    }

    @Test
    fun bilinearInterpolatesInsideTile() {
        val tile = ramp()
        val tiles = mapOf<TileIndex, ElevationTile?>(TileIndex(12, 2230, 1380) to tile)
        // find the lon/lat in the middle of that tile at zoom 12
        val z = MapterhornEndpoints.GLOBAL_MAX_ZOOM
        val n = (1 shl z).toDouble()
        val lon = (2230 + 0.5) / n * 360.0 - 180.0
        val lat = atan(sinh(PI * (1.0 - 2.0 * (1380 + 0.5) / n))) * 180.0 / PI
        assertEquals(150.0, PlannerElevation.sampleBilinear(z, lat, lon, tiles), 1e-6)
        assertEquals(0.0, PlannerElevation.sampleBilinear(z, lat + 5.0, lon, tiles))
    }
}
