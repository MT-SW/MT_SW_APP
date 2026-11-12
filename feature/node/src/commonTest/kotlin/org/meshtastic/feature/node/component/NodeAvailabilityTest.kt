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
package org.meshtastic.feature.node.component

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class NodeAvailabilityTest {
    private val range = AvailabilityRange(rangeSeconds = 4 * 3_600L, bucketSeconds = 3_600L)
    private val now = 10_000_000_000L

    @Test
    fun countsPacketsPerBar() {
        val start = now - 4 * 3_600_000L
        val times = listOf(start - 1, start, start + 3_599_999, start + 3_600_000, now - 1, now + 1)
        // Before the window and after "now" are ignored; the last bar ends at now.
        assertContentEquals(intArrayOf(2, 1, 0, 1), availabilityCounts(times, now, range))
    }

    @Test
    fun noPacketsGiveEmptyBarsAndNoSilence() {
        assertContentEquals(intArrayOf(0, 0, 0, 0), availabilityCounts(emptyList(), now, range))
        assertEquals(0L, longestSilenceSeconds(emptyList(), now))
    }

    @Test
    fun longestSilenceCountsGapsAndTheTimeUpToNow() {
        val times = listOf(now - 9_000_000L, now - 8_000_000L, now - 2_000_000L)
        // gaps: 1000 s, 6000 s, and 2000 s from the last packet to now
        assertEquals(6_000L, longestSilenceSeconds(times, now))
        assertEquals(2_000L, longestSilenceSeconds(listOf(now - 2_000_000L), now))
    }
}
