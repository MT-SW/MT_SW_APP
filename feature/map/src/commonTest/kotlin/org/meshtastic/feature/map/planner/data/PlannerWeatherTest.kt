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

import org.meshtastic.feature.map.planner.Atmosphere
import org.meshtastic.feature.map.planner.AtmosphereSample
import org.meshtastic.feature.map.planner.DuctingLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlannerWeatherTest {
    private fun sample(elevation: Double = 260.0, current: String = CURRENT, hourly: String = HOURLY) =
        """{"latitude":50.87,"longitude":20.63,"generationtime_ms":0.2,"utc_offset_seconds":0,"timezone":"GMT",
        "timezone_abbreviation":"GMT","elevation":$elevation,
        "current_units":{"time":"iso8601","interval":"seconds","temperature_2m":"°C"},
        "current":$current,
        "hourly_units":{"time":"iso8601","precipitation":"mm"},
        "hourly":$hourly}"""

    @Test
    fun parsesRealisticAnswer() {
        val c = parseOpenMeteo(sample())
        assertNotNull(c)
        assertEquals("2026-10-01 12:15", c.fetchedAtIso)
        assertEquals("Open-Meteo", c.source)
        assertEquals(14.3, c.tempC)
        assertEquals(983.4, c.pressureHpa)
        assertEquals(62.0, c.rhPct)
        // hourly precipitation of the 12:00 hour wins over current.precipitation
        assertEquals(0.2, c.rainMmH)
        val expected = Atmosphere.analyzeProfile(
            listOf(
                AtmosphereSample(260.0, 14.3, 983.4, 62.0),
                AtmosphereSample(760.0, 12.1, 925.0, 55.0),
                AtmosphereSample(1480.0, 8.0, 850.0, 48.0),
                AtmosphereSample(3090.0, -2.5, 700.0, 40.0),
            ),
        )
        assertEquals(expected, c.analysis)
        assertEquals(expected.level, c.level)
        assertTrue(c.analysis.kFactor in 1.0..2.5, "k=${c.analysis.kFactor}")
    }

    @Test
    fun levelsBelowGroundAreIgnored() {
        val c = parseOpenMeteo(sample(elevation = 1500.0))
        assertNotNull(c)
        val expected = Atmosphere.analyzeProfile(
            listOf(AtmosphereSample(1500.0, 14.3, 983.4, 62.0), AtmosphereSample(3090.0, -2.5, 700.0, 40.0)),
        )
        assertEquals(expected, c.analysis)
    }

    @Test
    fun missingHourlyFallsBackToStandardAtmosphere() {
        val json = """{"elevation":100.0,"current":$CURRENT}"""
        val c = parseOpenMeteo(json)
        assertNotNull(c)
        assertEquals(Atmosphere.analyzeProfile(listOf(AtmosphereSample(100.0, 14.3, 983.4, 62.0))), c.analysis)
        // no hourly data: current.precipitation is used
        assertEquals(0.0, c.rainMmH)
        assertEquals(DuctingLevel.NORMAL, c.level)
    }

    @Test
    fun invalidInputGivesNull() {
        assertNull(parseOpenMeteo("not json"))
        assertNull(parseOpenMeteo("""{"hourly":{}}"""))
        assertNull(parseOpenMeteo("""{"current":{"time":"2026-10-01T12:15","temperature_2m":null}}"""))
    }

    @Test
    fun urlContainsPressureLevels() {
        val url = PlannerWeather.buildUrl(PlannerWeather.BASE_URL, 50.8661, 20.6286)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?latitude=50.8661&longitude=20.6286"))
        assertTrue(url.contains("geopotential_height_850hPa"))
        assertTrue(url.contains("timezone=GMT"))
    }

    private companion object {
        const val CURRENT = """{"time":"2026-10-01T12:15","interval":900,"temperature_2m":14.3,
            "relative_humidity_2m":62,"surface_pressure":983.4,"precipitation":0.0}"""
        const val HOURLY = """{"time":["2026-10-01T11:00","2026-10-01T12:00","2026-10-01T13:00"],
            "precipitation":[0.0,0.2,0.0],
            "temperature_925hPa":[12.0,12.1,12.3],"relative_humidity_925hPa":[54,55,56],
            "geopotential_height_925hPa":[755.0,760.0,762.0],
            "temperature_850hPa":[7.9,8.0,8.1],"relative_humidity_850hPa":[47,48,49],
            "geopotential_height_850hPa":[1478.0,1480.0,1483.0],
            "temperature_700hPa":[-2.4,-2.5,-2.6],"relative_humidity_700hPa":[41,40,39],
            "geopotential_height_700hPa":[3088.0,3090.0,3093.0]}"""
    }
}
