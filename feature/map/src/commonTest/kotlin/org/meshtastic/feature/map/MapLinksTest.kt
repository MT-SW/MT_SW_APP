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
package org.meshtastic.feature.map

import org.meshtastic.core.model.Node
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Which radio links the map draws, and which node pairs are left out. */
class MapLinksTest {

    private val now = 1_800_000_000L

    private fun nodes(vararg nodes: Node) = nodes.associateBy { it.num }

    @Test
    fun nodeHeardDirectlyAndRecentlyGetsALinkToTheConnectedRadio() {
        val direct = Node(num = 2, snr = 6.5f, lastHeard = (now - 60).toInt(), hopsAway = 0)

        val links =
            buildMapLinks(
                neighborLogs = emptyList(),
                tracerouteLogs = emptyList(),
                nodes = nodes(Node(num = 1), direct),
                myNodeNum = 1,
                nowMillis = now * 1000,
                nowSeconds = now,
            )

        assertEquals(listOf(MapLink(1, 2, MapLinkKind.DIRECT, 6.5f)), links)
    }

    @Test
    fun relayedStaleOrUnmeasuredNodesGetNoDirectLink() {
        val relayed = Node(num = 2, snr = 6f, lastHeard = (now - 60).toInt(), hopsAway = 2)
        val stale = Node(num = 3, snr = 6f, lastHeard = (now - 10 * 3600).toInt(), hopsAway = 0)
        val unmeasured = Node(num = 4, lastHeard = (now - 60).toInt(), hopsAway = 0)

        val links =
            buildMapLinks(
                neighborLogs = emptyList(),
                tracerouteLogs = emptyList(),
                nodes = nodes(Node(num = 1), relayed, stale, unmeasured),
                myNodeNum = 1,
                nowMillis = now * 1000,
                nowSeconds = now,
            )

        assertTrue(links.isEmpty(), links.toString())
    }

    @Test
    fun withoutAConnectedRadioNothingIsDrawnFromDirectReceptions() {
        val direct = Node(num = 2, snr = 6f, lastHeard = (now - 60).toInt(), hopsAway = 0)

        val links =
            buildMapLinks(
                neighborLogs = emptyList(),
                tracerouteLogs = emptyList(),
                nodes = nodes(direct),
                myNodeNum = null,
                nowMillis = now * 1000,
                nowSeconds = now,
            )

        assertTrue(links.isEmpty(), links.toString())
    }
}
