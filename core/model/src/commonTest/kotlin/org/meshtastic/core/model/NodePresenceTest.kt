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
package org.meshtastic.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NodePresenceTest {
    private val minute = 60_000L
    private val expiry = 120 * minute

    @Test
    fun `offline report is always shown`() {
        val presence = NodePresence(NodePresenceState.OFFLINE, sinceMillis = 0, receivedAtMillis = 0)
        assertEquals(NodePresenceState.OFFLINE, presence.effective(1_000 * minute, 0, expiry))
    }

    @Test
    fun `fresh online report is shown`() {
        val presence = NodePresence(NodePresenceState.ONLINE, sinceMillis = 0, receivedAtMillis = 100 * minute)
        assertEquals(NodePresenceState.ONLINE, presence.effective(150 * minute, 0, expiry))
    }

    @Test
    fun `stale online report is hidden`() {
        val presence = NodePresence(NodePresenceState.ONLINE, sinceMillis = 0, receivedAtMillis = 0)
        assertNull(presence.effective(500 * minute, 0, expiry))
    }

    @Test
    fun `online report stays while the node is still heard`() {
        val presence = NodePresence(NodePresenceState.ONLINE, sinceMillis = 0, receivedAtMillis = 0)
        val lastHeardSeconds = (480 * minute / 1000).toInt()
        assertEquals(NodePresenceState.ONLINE, presence.effective(500 * minute, lastHeardSeconds, expiry))
    }
}
