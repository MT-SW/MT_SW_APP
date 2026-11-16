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
package org.meshtastic.feature.widget

import kotlin.test.Test
import kotlin.test.assertEquals

class SosTapCountTest {

    @Test
    fun `first tap starts a run`() {
        assertEquals(1, nextSosTapCount(previous = 0, lastTap = 0L, now = 10_000L))
    }

    @Test
    fun `quick taps add up`() {
        assertEquals(2, nextSosTapCount(previous = 1, lastTap = 10_000L, now = 10_500L))
        assertEquals(3, nextSosTapCount(previous = 2, lastTap = 10_500L, now = 12_900L))
    }

    @Test
    fun `a slow tap starts a new run`() {
        assertEquals(1, nextSosTapCount(previous = 2, lastTap = 10_000L, now = 10_000L + SOS_TAP_WINDOW_MS + 1))
    }

    @Test
    fun `a clock that went back starts a new run`() {
        assertEquals(1, nextSosTapCount(previous = 2, lastTap = 10_000L, now = 9_000L))
    }
}
