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
package org.meshtastic.core.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuietHoursTest {
    @Test
    fun `a span inside one day includes its start and excludes its end`() {
        val hours = QuietHours(startHour = 9, endHour = 17)
        assertTrue(hours.contains(9))
        assertTrue(hours.contains(16))
        assertFalse(hours.contains(17))
        assertFalse(hours.contains(3))
    }

    @Test
    fun `a span across midnight covers both sides of it`() {
        val hours = QuietHours(startHour = 22, endHour = 7)
        assertTrue(hours.contains(23))
        assertTrue(hours.contains(0))
        assertTrue(hours.contains(6))
        assertFalse(hours.contains(7))
        assertFalse(hours.contains(12))
        assertTrue(hours.contains(22))
    }

    @Test
    fun `equal hours mean the whole day`() {
        assertTrue(QuietHours(5, 5).contains(0))
        assertTrue(QuietHours(5, 5).contains(23))
    }
}
