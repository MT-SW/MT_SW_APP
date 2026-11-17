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
package org.meshtastic.core.prefs.bridge

import org.meshtastic.core.repository.RadioBridgeConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RadioBridgePrefsImplTest {
    @Test
    fun `settings survive a round trip`() {
        val all =
            mapOf(
                1 to RadioBridgeConfig(enabled = true, port = 5000, lanAccess = true, readOnly = true, maxClients = 8),
                3 to RadioBridgeConfig(blockAdmin = true, blockMessages = true),
            )

        assertEquals(all, RadioBridgePrefsImpl.decode(RadioBridgePrefsImpl.encode(all)))
    }

    @Test
    fun `damaged text reads as no settings, so the bridge stays off`() {
        assertEquals(emptyMap(), RadioBridgePrefsImpl.decode("{not json"))
        assertEquals(emptyMap(), RadioBridgePrefsImpl.decode(null))
    }

    @Test
    fun `radios outside the range are dropped and a missing field is its default`() {
        val decoded = RadioBridgePrefsImpl.decode("""{"0":{"enabled":true},"2":{"enabled":true},"9":{}}""")

        assertEquals(setOf(2), decoded.keys)
        assertTrue(decoded.getValue(2).enabled)
        assertEquals(RadioBridgeConfig().port, decoded.getValue(2).port)
    }

    @Test
    fun `the port is the chosen one or the default of the radio`() {
        assertEquals(4404, RadioBridgeConfig().portFor(1))
        assertEquals(4406, RadioBridgeConfig().portFor(3))
        assertEquals(5000, RadioBridgeConfig(port = 5000).portFor(1))
        assertEquals(4405, RadioBridgeConfig(port = 80).portFor(2))
    }
}
