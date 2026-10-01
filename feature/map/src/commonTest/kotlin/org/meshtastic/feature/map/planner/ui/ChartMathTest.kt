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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartMathTest {
    @Test
    fun niceStep_oneTwoFive() {
        assertEquals(1.0, ChartMath.niceStep(1.0))
        assertEquals(2.0, ChartMath.niceStep(2.2))
        assertEquals(5.0, ChartMath.niceStep(4.0))
        assertEquals(10.0, ChartMath.niceStep(8.0))
        assertEquals(0.5, ChartMath.niceStep(0.4), 1.0e-12)
    }

    @Test
    fun niceTicks_insideRange() {
        val ticks = ChartMath.niceTicks(3.0, 97.0, 5)
        assertTrue(ticks.isNotEmpty())
        assertTrue(ticks.all { it >= 3.0 - 1.0e-9 && it <= 97.0 + 1.0e-9 })
        assertEquals(20.0, ticks[1] - ticks[0], 1.0e-9)
    }

    @Test
    fun niceTicks_degenerateRange() {
        assertEquals(listOf(5.0), ChartMath.niceTicks(5.0, 5.0, 4))
    }

    @Test
    fun decimals() {
        assertEquals(0, ChartMath.decimalsFor(5.0))
        assertEquals(1, ChartMath.decimalsFor(0.5))
        assertEquals(2, ChartMath.decimalsFor(0.05))
    }

    @Test
    fun nearestIndex() {
        val d = doubleArrayOf(0.0, 100.0, 200.0, 300.0)
        assertEquals(0, ChartMath.nearestIndex(d, -50.0))
        assertEquals(2, ChartMath.nearestIndex(d, 190.0))
        assertEquals(3, ChartMath.nearestIndex(d, 1000.0))
        assertEquals(0, ChartMath.nearestIndex(DoubleArray(0), 5.0))
    }
}
