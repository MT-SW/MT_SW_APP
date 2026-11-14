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
        quietExceptions: Set<Int> = emptySet(),
        minute: Int = 12 * 60,
        test: Boolean = false,
    ) = SosAlarmPolicy.rings(test, receive, sender, onlyChosen, chosen, quiet, quietExceptions, minute)

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
    fun quietHoursLetOnlyTheirExceptionsThrough() {
        assertFalse(rings(quiet = night, minute = 23 * 60))
        assertFalse(rings(sender = 2, quiet = night, minute = 3 * 60, chosen = setOf(2)))
        assertFalse(rings(sender = null, quiet = night, minute = 3 * 60, quietExceptions = setOf(2)))
        assertTrue(rings(sender = 2, quiet = night, minute = 3 * 60, quietExceptions = setOf(2)))
        assertTrue(rings(quiet = night, minute = 12 * 60))
    }

    @Test
    fun theTwoListsWorkTogether() {
        // Only chosen is on and the sender is in neither list for the quiet hours: quiet wins.
        assertFalse(rings(sender = 2, onlyChosen = true, chosen = setOf(2), quiet = night, minute = 3 * 60))
        assertTrue(
            rings(
                sender = 2,
                onlyChosen = true,
                chosen = setOf(2),
                quiet = night,
                quietExceptions = setOf(2),
                minute = 3 * 60,
            ),
        )
        // In the quiet exceptions but not in the chosen list while only chosen is on: that rule still holds.
        assertFalse(rings(sender = 3, onlyChosen = true, chosen = setOf(2), quietExceptions = setOf(3)))
    }

    @Test
    fun styleIsTakenFromTheListOnly() {
        val styles = mapOf(2 to SosAlarmStyle.BELL, 3 to SosAlarmStyle.SILENT)
        assertEquals(SosAlarmStyle.BELL, SosAlarmPolicy.styleFor(2, setOf(2, 3), styles))
        assertEquals(SosAlarmStyle.SILENT, SosAlarmPolicy.styleFor(3, setOf(2, 3), styles))
        assertEquals(SosAlarmStyle.SIREN, SosAlarmPolicy.styleFor(4, setOf(2, 3), styles))
        assertEquals(SosAlarmStyle.SIREN, SosAlarmPolicy.styleFor(3, setOf(2), styles))
        assertEquals(SosAlarmStyle.SIREN, SosAlarmPolicy.styleFor(null, setOf(2), styles))
    }

    @Test
    fun stylesEncodeAndDecode() {
        val styles = mapOf(2 to SosAlarmStyle.BELL, 3 to SosAlarmStyle.SILENT, 4 to SosAlarmStyle.SIREN)
        assertEquals(mapOf(2 to SosAlarmStyle.BELL, 3 to SosAlarmStyle.SILENT),
            SosAlarmStyle.decodeMap(SosAlarmStyle.encodeMap(styles)))
        assertEquals(emptyMap(), SosAlarmStyle.decodeMap(null))
        assertEquals(emptyMap(), SosAlarmStyle.decodeMap("x,1:2:3"))
        assertEquals(mapOf(7 to SosAlarmStyle.SIREN), SosAlarmStyle.decodeMap("7:NIEZNANY"))
    }

    @Test
    fun unlimitedRepeatsSurviveEncoding() {
        val config = SosConfig(repeatMinutes = 5, repeatCount = SosConfig.UNLIMITED_REPEATS)
        assertEquals(SosConfig.UNLIMITED_REPEATS, SosConfig.decode(config.encode()).repeatCount)
    }

    @Test
    fun historyExportIsAQuotedCsv() {
        val csv =
            SosHistoryExport.csv(
                listOf(
                    SosHistoryEntry(0L, 255, "Ala, \"Ola\"", "k", "SOS\npomocy", 50.5, 20.25, alarmed = false),
                    SosHistoryEntry(1000L, null, "Bez", "k", "t"),
                ),
            )
        val lines = csv.removePrefix("\uFEFF").split('\n')
        assertEquals("time_utc,sender,sender_node,text,latitude,longitude,alarm", lines[0])
        assertTrue(lines[1].startsWith("\"1970-01-01T00:00:00Z\",\"Ala, \"\"Ola\"\"\",\"!000000ff\","))
        assertTrue(csv.contains("\"no\""))
        assertTrue(csv.contains("\"1970-01-01T00:00:01Z\",\"Bez\",\"\",\"t\",\"\",\"\",\"yes\""))
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

    @Test
    fun secondSosCountsOnlyInsideTheWindow() {
        val minute = 60_000L
        val seen = mapOf(7 to 100 * minute)
        assertFalse(SosAlarmPolicy.isSecond(7, emptyMap(), 100 * minute, 10))
        assertTrue(SosAlarmPolicy.isSecond(7, seen, 105 * minute, 10))
        assertTrue(SosAlarmPolicy.isSecond(7, seen, 110 * minute, 10))
        assertFalse(SosAlarmPolicy.isSecond(7, seen, 111 * minute, 10))
        assertFalse(SosAlarmPolicy.isSecond(8, seen, 101 * minute, 10))
        assertTrue(SosAlarmPolicy.isSecond(null, seen, 101 * minute, 10))
    }

    @Test
    fun historyPruneRemovesOldEntries() {
        val day = 86_400_000L
        val now = 100 * day
        val entries =
            listOf(
                SosHistoryEntry(now - 2 * day, 1, "a", "k", "t"),
                SosHistoryEntry(now - 40 * day, 1, "b", "k", "t"),
            )
        assertEquals(entries, SosHistoryCodec.prune(entries, 0, now))
        assertEquals(entries.take(1), SosHistoryCodec.prune(entries, 30, now))
        assertEquals(entries, SosHistoryCodec.prune(entries, 90, now))
    }

    @Test
    fun ackMarkerIsFoundAndRemoved() {
        for (ack in SosAck.entries) {
            val text = SosAckMarker.tag(ack) + "SOS odebrany"
            assertEquals(ack, SosAckMarker.parse(text))
            assertEquals("SOS odebrany", SosAckMarker.clean(text))
        }
        assertEquals(null, SosAckMarker.parse("zwykły tekst"))
        assertEquals(null, SosAckMarker.parse(null))
        assertFalse(SosMarker.isSos(SosAckMarker.tag(SosAck.SEEN) + "tekst"))
    }

    @Test
    fun acksEncodeAndDecode() {
        val entries =
            listOf(
                SosAckEntry(2L, 5, "Ala | ma", SosAck.COMING),
                SosAckEntry(1L, 6, "Olek", SosAck.SEEN),
            )
        assertEquals(entries, SosAckCodec.decode(SosAckCodec.encode(entries)))
        assertEquals(emptyList(), SosAckCodec.decode(""))
        assertEquals(emptyList(), SosAckCodec.decode("zepsute"))
    }

    @Test
    fun targetMessagesEncodeAndDecode() {
        val messages = mapOf(SosTargets.channelKey(1) to "Dla rodziny\nPomocy", SosTargets.nodeKey(42) to "Do Oleka")
        assertEquals(messages, SosTargets.decode(SosTargets.encode(messages)))
        assertEquals(emptyMap(), SosTargets.decode(SosTargets.encode(mapOf("c0" to "  "))))
        assertEquals(emptyMap(), SosTargets.decode(null))
    }

    @Test
    fun acksExportAsCsv() {
        val csv = SosAckExport.csv(listOf(SosAckEntry(0L, 5, "Ala \"A\"", SosAck.COMING)))
        val lines = csv.removePrefix("\uFEFF").lines()
        assertEquals("time_utc,name,node,answer", lines[0])
        assertTrue(lines[1].contains("\"Ala \"\"A\"\"\""))
        assertTrue(lines[1].endsWith("\"coming\""))
    }

    @Test
    fun latestAnswerOfEachNodeWins() {
        val entries =
            listOf(
                SosAckEntry(3L, 5, "Ala", SosAck.CALLING_HELP),
                SosAckEntry(2L, 6, "Olek", SosAck.SEEN),
                SosAckEntry(1L, 5, "Ala", SosAck.SEEN),
            )
        assertEquals(mapOf(5 to SosAck.CALLING_HELP, 6 to SosAck.SEEN), entries.latestByNode())
    }
}
