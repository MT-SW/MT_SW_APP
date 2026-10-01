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
package org.meshtastic.feature.map.planner.ui

import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlannerInputTest {

    private fun near(expected: Double, actual: Double?, eps: Double = 1.0e-9) {
        assertNotNull(actual)
        assertTrue((expected - actual).absoluteValue <= eps, "expected $expected but was $actual")
    }

    @Test
    fun parseNumber_acceptsDotAndComma() {
        near(12.5, PlannerInput.parseNumber("12.5"))
        near(12.5, PlannerInput.parseNumber("12,5"))
        near(-3.0, PlannerInput.parseNumber(" -3 "))
        near(1234.5, PlannerInput.parseNumber("1.234,5"))
        near(1234.5, PlannerInput.parseNumber("1,234.5"))
        near(5.0, PlannerInput.parseNumber("5."))
    }

    @Test
    fun parseNumber_rejectsGarbage() {
        assertNull(PlannerInput.parseNumber(""))
        assertNull(PlannerInput.parseNumber("-"))
        assertNull(PlannerInput.parseNumber("abc"))
        assertNull(PlannerInput.parseNumber("NaN"))
        assertNull(PlannerInput.parseNumber("Infinity"))
        assertNull(PlannerInput.parseNumber("1.2.3"))
    }

    @Test
    fun parseCoordinate_plainAndHemispheres() {
        near(52.25, PlannerInput.parseCoordinate("52,25", true))
        near(-52.25, PlannerInput.parseCoordinate("-52.25", true))
        near(52.25, PlannerInput.parseCoordinate("52.25N", true))
        near(-52.25, PlannerInput.parseCoordinate("S 52.25", true))
        near(19.5, PlannerInput.parseCoordinate("19.5 e", false))
        near(-19.5, PlannerInput.parseCoordinate("W19,5", false))
    }

    @Test
    fun parseCoordinate_rejectsWrongAxisAndRange() {
        assertNull(PlannerInput.parseCoordinate("52.25E", true))
        assertNull(PlannerInput.parseCoordinate("19.5N", false))
        assertNull(PlannerInput.parseCoordinate("91", true))
        assertNull(PlannerInput.parseCoordinate("181", false))
        assertNull(PlannerInput.parseCoordinate("-5N", true))
        assertNull(PlannerInput.parseCoordinate("", true))
    }

    @Test
    fun parseCoordinate_degreesMinutesSeconds() {
        near(52.5, PlannerInput.parseCoordinate("52°30'0\"N", true))
        near(-19.25, PlannerInput.parseCoordinate("19°15'W", false))
        near(50.8666666667, PlannerInput.parseCoordinate("50°52'", true), 1.0e-6)
        assertNull(PlannerInput.parseCoordinate("50°75'", true))
    }

    @Test
    fun parseLatLonPair_variants() {
        val a = PlannerInput.parseLatLonPair("50.87, 20.63")
        assertNotNull(a)
        near(50.87, a.first)
        near(20.63, a.second)
        val b = PlannerInput.parseLatLonPair("50,87 20,63")
        assertNotNull(b)
        near(50.87, b.first)
        near(20.63, b.second)
        val c = PlannerInput.parseLatLonPair("50.87;20.63")
        assertNotNull(c)
        near(20.63, c.second)
        val d = PlannerInput.parseLatLonPair("50.87,20.63")
        assertNotNull(d)
        near(50.87, d.first)
        val e = PlannerInput.parseLatLonPair("50.87N 20.63E")
        assertNotNull(e)
        near(20.63, e.second)
        assertNull(PlannerInput.parseLatLonPair("50.87"))
        assertNull(PlannerInput.parseLatLonPair("a b"))
    }

    @Test
    fun parseLatLonPair_swapsWhenFirstCannotBeLatitude() {
        val p = PlannerInput.parseLatLonPair("120.5, 19.2")
        assertNotNull(p)
        near(19.2, p.first)
        near(120.5, p.second)
    }

    @Test
    fun formatting() {
        assertEquals("52.12346, 19.00000", PlannerInput.formatCoordinates(52.123456, 19.0))
        assertEquals("-1.5", PlannerInput.formatField(-1.5, 2))
        assertEquals("20", PlannerInput.formatField(20.0, 2))
        assertEquals("0.1", PlannerInput.formatWatts(0.1))
        assertEquals("1.58", PlannerInput.formatWatts(1.5849))
    }

    @Test
    fun sameValue_tolerance() {
        assertTrue(PlannerInput.sameValue(0.1, 0.10000000000000002))
        assertTrue(PlannerInput.sameValue(1000.0, 1000.0001))
        assertFalse(PlannerInput.sameValue(1.0, 1.01))
    }
}
