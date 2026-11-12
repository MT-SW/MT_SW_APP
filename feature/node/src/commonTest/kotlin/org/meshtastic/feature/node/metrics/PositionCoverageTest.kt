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
package org.meshtastic.feature.node.metrics

import org.meshtastic.core.ui.util.SNR_UNKNOWN_RGB
import org.meshtastic.core.ui.util.snrRgb
import org.meshtastic.proto.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PositionCoverageTest {

    private fun position(latitude: Int, longitude: Int, time: Int) = Position.Builder()
        .also { wb ->
            wb.latitude_i = latitude
            wb.longitude_i = longitude
            wb.time = time
        }
        .build()

    private val track =
        listOf(
            position(latitude = 525_200_000, longitude = 134_050_000, time = 100),
            position(latitude = 525_300_000, longitude = 134_150_000, time = 200),
            position(latitude = 525_400_000, longitude = 134_250_000, time = 300),
        )

    @Test
    fun `snr colour clamps at both ends and is grey without a measurement`() {
        assertEquals(0xD32F2F, snrRgb(-40f))
        assertEquals(0x2E7D32, snrRgb(20f))
        assertEquals(SNR_UNKNOWN_RGB, snrRgb(null))
        assertEquals(SNR_UNKNOWN_RGB, snrRgb(Float.NaN))
    }

    @Test
    fun `snr colour blends between steps`() {
        // Halfway between -6 (0xFBC02D) and 0 (0x9CCC65): every channel sits between its two ends.
        val rgb = snrRgb(-3f)
        val red = (rgb shr 16) and 0xFF
        assertTrue(red in 0x9C..0xFB)
    }

    @Test
    fun `kml colour is blue green red with an opaque alpha`() {
        assertEquals("ff2f32d3", kmlColor(0xD32F2F))
    }

    @Test
    fun `kml has one leg per pair of fixes and one dot per fix`() {
        val kml = buildCoverageKml(track, mapOf(100 to -10f, 200 to -4f, 300 to 3f), "Node & co")
        assertEquals(2, Regex("<LineString>").findAll(kml).count())
        assertEquals(3, Regex("<Point>").findAll(kml).count())
        assertTrue(kml.contains("Node &amp; co"))
    }

    @Test
    fun `kml writes coordinates with a dot and in longitude latitude order`() {
        val kml = buildCoverageKml(track, emptyMap(), "n")
        assertTrue(kml.contains("13.4050000,52.5200000,0"))
        // No measurement anywhere: everything is the grey style.
        assertTrue(kml.contains("snr_none"))
    }

    @Test
    fun `kml dots carry their snr`() {
        val kml = buildCoverageKml(track, mapOf(200 to -4.5f), "n")
        assertTrue(kml.contains("<name>-4.5 dB</name>"))
        assertTrue(kml.contains("<name>-</name>"))
    }

    @Test
    fun `track length sums the legs and ignores order`() {
        val forward = trackLengthMeters(track)
        assertTrue(forward > 0.0)
        assertEquals(forward, trackLengthMeters(track.reversed()))
        assertEquals(0.0, trackLengthMeters(track.take(1)))
    }
}
