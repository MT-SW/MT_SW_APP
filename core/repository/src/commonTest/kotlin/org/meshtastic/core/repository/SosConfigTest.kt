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

class SosConfigTest {
    @Test
    fun encodedConfigReadsBackTheSame() {
        val config =
            SosConfig(
                channel = 2,
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
    }

    @Test
    fun newLinesInTheMessageBecomeSpaces() {
        assertEquals("a b", SosConfig.decode(SosConfig(message = "a\nb").encode()).message)
    }

    @Test
    fun valuesAreKeptInRange() {
        val decoded = SosConfig.decode("99|1|-4|0|1|99|x")
        assertEquals(7, decoded.channel)
        assertEquals(0, decoded.repeatMinutes)
        assertEquals(1, decoded.repeatCount)
        assertEquals(10, decoded.holdSeconds)
    }
}
