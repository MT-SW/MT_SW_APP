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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SosAlarmPolicyTest {
    private val night = SosQuietHours(enabled = true, fromMinutes = 22 * 60, toMinutes = 7 * 60)

    private fun rings(
        sender: Int? = 1,
        receive: Boolean = true,
        onlyChosen: Boolean = false,
        chosen: Set<Int> = emptySet(),
        quiet: SosQuietHours = SosQuietHours(),
        minute: Int = 12 * 60,
        test: Boolean = false,
    ) = SosAlarmPolicy.rings(test, receive, sender, onlyChosen, chosen, quiet, minute)

    @Test
    fun ordinaryAlarmRings() = assertTrue(rings())

    @Test
    fun switchedOffNeverRingsExceptATest() {
        assertFalse(rings(receive = false))
        assertTrue(rings(receive = false, test = true))
    }

    @Test
    fun onlyChosenKeepsOthersQuiet() {
        assertFalse(rings(onlyChosen = true, chosen = setOf(2)))
        assertFalse(rings(sender = null, onlyChosen = true, chosen = setOf(2)))
        assertTrue(rings(sender = 2, onlyChosen = true, chosen = setOf(2)))
    }

    @Test
    fun quietHoursLetOnlyChosenThrough() {
        assertFalse(rings(quiet = night, minute = 23 * 60))
        assertFalse(rings(quiet = night, minute = 3 * 60, chosen = setOf(2)))
        assertTrue(rings(sender = 2, quiet = night, minute = 3 * 60, chosen = setOf(2)))
        assertTrue(rings(quiet = night, minute = 12 * 60))
    }

    @Test
    fun quietHoursSpanMidnightAndSameDay() {
        assertTrue(night.contains(22 * 60))
        assertTrue(night.contains(0))
        assertTrue(night.contains(6 * 60 + 59))
        assertFalse(night.contains(7 * 60))
        assertFalse(night.contains(21 * 60 + 59))
        val day = SosQuietHours(enabled = true, fromMinutes = 9 * 60, toMinutes = 17 * 60)
        assertTrue(day.contains(9 * 60))
        assertFalse(day.contains(17 * 60))
        assertFalse(SosQuietHours(enabled = false).contains(23 * 60))
        assertFalse(SosQuietHours(enabled = true, fromMinutes = 60, toMinutes = 60).contains(60))
    }

    @Test
    fun quietHoursEncodeAndDecode() {
        assertEquals(night, SosQuietHours.decode(night.encode()))
        assertEquals(SosQuietHours(), SosQuietHours.decode(null))
        assertEquals(SosQuietHours(), SosQuietHours.decode("a|b"))
        assertEquals(1439, SosQuietHours.decode("1|5000|0").fromMinutes)
    }

    @Test
    fun historyEncodesAndDecodes() {
        val entries =
            listOf(
                SosHistoryEntry(2L, 5, "Ala", "0!abc", "SOS | pomocy\nTu", 50.5, 20.25, alarmed = false),
                SosHistoryEntry(1L, null, "Bez pozycji", "k", "tekst"),
            )
        assertEquals(entries, SosHistoryCodec.decode(SosHistoryCodec.encode(entries)))
        assertEquals(emptyList(), SosHistoryCodec.decode(null))
        assertEquals(emptyList(), SosHistoryCodec.decode(""))
        assertEquals(emptyList(), SosHistoryCodec.decode("zepsute"))
    }

    @Test
    fun historyKeepsOnlyTheNewestEntries() {
        val many = (1..SosHistoryCodec.MAX_ENTRIES + 10).map { SosHistoryEntry(it.toLong(), 1, "n", "k", "t") }
        assertEquals(SosHistoryCodec.MAX_ENTRIES, SosHistoryCodec.decode(SosHistoryCodec.encode(many)).size)
    }
}
