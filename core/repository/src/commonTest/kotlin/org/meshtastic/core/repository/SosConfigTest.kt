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

class SosConfigTest {
    @Test
    fun encodedConfigReadsBackTheSame() {
        val config =
            SosConfig(
                channels = listOf(1, 2),
                nodes = listOf(1234, -5),
                message = "Pomocy | tu jestem",
                includePosition = false,
                repeatMinutes = 5,
                repeatCount = 10,
                stopWhenDelivered = false,
                holdSeconds = 3,
            )
        assertEquals(config, SosConfig.decode(config.encode()))
    }

    @Test
    fun unreadableTextGivesTheDefaults() {
        assertEquals(SosConfig(), SosConfig.decode(null))
        assertEquals(SosConfig(), SosConfig.decode(""))
        assertEquals(SosConfig(), SosConfig.decode("a|b|c"))
        assertEquals(SosConfig(), SosConfig.decode("x|1|0|3|1|2|tekst"))
        assertEquals(SosConfig(), SosConfig.decode("c0|a|0|3|1|2||tekst"))
    }

    @Test
    fun onlyTheMarkedMessageIsAnSos() {
        val sent = "\uD83D\uDD14 " + SosMarker.TAG + "Pomocy"
        assertTrue(SosMarker.isSos(sent))
        assertEquals("\uD83D\uDD14 Pomocy", SosMarker.clean(sent))
        assertFalse(SosMarker.isSos("SOS pomocy"))
        assertFalse(SosMarker.isSos("\uD83D\uDD14 SOS"))
        assertFalse(SosMarker.isSos(null))
        // The mark far down in a long text (pasted, not sent by the app) does not count.
        assertFalse(SosMarker.isSos("x".repeat(40) + SosMarker.TAG))
    }

    @Test
    fun theBarButtonNeedsAtLeastTwoSeconds() {
        assertEquals(2, SosConfig(holdSeconds = 0).barHoldSeconds)
        assertEquals(2, SosConfig(holdSeconds = 1).barHoldSeconds)
        assertEquals(3, SosConfig(holdSeconds = 3).barHoldSeconds)
        assertEquals(5, SosConfig(holdSeconds = 5).barHoldSeconds)
    }

    @Test
    fun newLinesInTheMessageBecomeSpaces() {
        assertEquals("a b", SosConfig.decode(SosConfig(message = "a\nb").encode()).message)
    }

    @Test
    fun valuesAreKeptInRange() {
        val decoded = SosConfig.decode("99|1|-4|0|1|99|x")
        assertEquals(listOf(7), decoded.channels)
        assertEquals(0, decoded.repeatMinutes)
        assertEquals(1, decoded.repeatCount)
        assertEquals(10, decoded.holdSeconds)
    }

    @Test
    fun theOlderSingleChannelFormStillReads() {
        val decoded = SosConfig.decode("2|0|5|10|0|3|Pomocy")
        assertEquals(listOf(2), decoded.channels)
        assertEquals(emptyList(), decoded.nodes)
        assertEquals("Pomocy", decoded.message)
    }

    @Test
    fun withNothingChosenTheMainChannelStays() {
        assertEquals(listOf(0), SosConfig.decode("c|1|0|3|1|2||x").channels)
        assertEquals(emptyList(), SosConfig.decode("c|1|0|3|1|2|42|x").channels)
        assertEquals(listOf(42), SosConfig.decode("c|1|0|3|1|2|42|x").nodes)
    }
}
