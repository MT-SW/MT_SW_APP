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
package org.meshtastic.feature.messaging.translation

import kotlin.test.Test
import kotlin.test.assertEquals

class ThinkTagFilterTest {
    @Test
    fun emptyThinkingBlockIsRemoved() {
        assertEquals("Odpowiedź", ThinkTagFilter.strip("<think>\n\n</think>\n\nOdpowiedź"))
    }

    @Test
    fun textWithoutTagsStaysAsItIs() {
        assertEquals("a < b i c > d", ThinkTagFilter.strip("a < b i c > d"))
    }

    @Test
    fun unfinishedThinkingIsDropped() {
        assertEquals("Wstęp", ThinkTagFilter.strip("Wstęp <think>myślę dalej"))
    }

    @Test
    fun streamingPassesOnlyTheVisiblePart() {
        val seen = StringBuilder()
        val filter = ThinkTagFilter { seen.append(it) }
        listOf("<th", "ink>", "\n\n</th", "ink>\n\nZasi", "ęg ", "LoRa").forEach { filter.feed(it) }
        assertEquals("Zasięg LoRa", seen.toString())
        assertEquals("Zasięg LoRa", filter.result())
    }

    @Test
    fun aLessThanSignIsNotHeldBackForever() {
        val seen = StringBuilder()
        val filter = ThinkTagFilter { seen.append(it) }
        listOf("1 ", "<", " 2").forEach { filter.feed(it) }
        assertEquals("1 < 2", seen.toString())
    }
}
