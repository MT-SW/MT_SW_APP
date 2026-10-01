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

class PlannerClockTest {
    @Test
    fun epochStart() {
        assertEquals("1970-01-01 00:00", PlannerClock.formatUtcMinute(0L))
    }

    @Test
    fun knownInstants() {
        assertEquals("2026-10-01 12:34", PlannerClock.formatUtcMinute(1_790_858_040_000L))
        assertEquals("2000-02-29 23:59", PlannerClock.formatUtcMinute(951_868_740_000L))
    }

    @Test
    fun fileStampHasNoSeparators() {
        assertEquals("20261001_1234", PlannerClock.fileStamp(1_790_858_040_000L))
    }

    @Test
    fun beforeEpoch() {
        assertEquals("1969-12-31 23:59", PlannerClock.formatUtcMinute(-60_000L))
    }
}
