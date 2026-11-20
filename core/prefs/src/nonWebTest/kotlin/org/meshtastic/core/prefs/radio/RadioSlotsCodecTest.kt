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
package org.meshtastic.core.prefs.radio

import org.meshtastic.core.repository.RadioSlot
import kotlin.test.Test
import kotlin.test.assertEquals

class RadioSlotsCodecTest {
    @Test
    fun `slots survive a round trip`() {
        val slots = listOf(RadioSlot(1, "x12:34", "Radio A"), RadioSlot(3, "t192.168.0.5", null))

        assertEquals(slots, RadioSlotsCodec.decode(RadioSlotsCodec.encode(slots)))
    }

    @Test
    fun `damaged text reads as no radios`() {
        assertEquals(emptyList(), RadioSlotsCodec.decode("{not json"))
        assertEquals(emptyList(), RadioSlotsCodec.decode(null))
    }

    @Test
    fun `ids outside the range and duplicates are dropped`() {
        val text = """[{"id":0,"address":"a"},{"id":4,"address":"b"},{"id":2,"address":"c"},{"id":2,"address":"d"}]"""

        assertEquals(listOf(RadioSlot(2, "c", null)), RadioSlotsCodec.decode(text))
    }

    @Test
    fun `the single address saved before becomes radio 1`() {
        assertEquals(
            listOf(RadioSlot(1, "x12:34", "Stare")),
            RadioSlotsCodec.effective(emptyList(), "x12:34", "Stare"),
        )
        assertEquals(emptyList(), RadioSlotsCodec.effective(emptyList(), "n", null))
        assertEquals(emptyList(), RadioSlotsCodec.effective(emptyList(), null, null))
    }

    @Test
    fun `saved radios win over the old single address`() {
        val saved = listOf(RadioSlot(2, "t1.2.3.4", null))

        assertEquals(saved, RadioSlotsCodec.effective(saved, "x12:34", "Stare"))
    }
}
