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
package org.meshtastic.core.data.manager

/**
 * Remembers, for each possible last byte of a node number, when a packet last reached this radio through a node with
 * that byte (the packet's `relay_node` field carries only that one byte). The node silence alert uses it as an optional
 * extra sign of life: a node that only passes other nodes' packets on is still alive.
 *
 * Kept in memory only; after a restart the alert falls back to the time the node was last heard itself.
 */
object RelayActivityTracker {
    private const val SUFFIXES = 256
    private const val SUFFIX_MASK = 0xFF
    private val lastRelayed = IntArray(SUFFIXES)

    /** Notes that a packet was just relayed by the node whose number ends with [relayNode], at [atSeconds]. */
    fun record(relayNode: Int, atSeconds: Int) {
        lastRelayed[relayNode and SUFFIX_MASK] = atSeconds
    }

    /** The time (seconds) a packet last came through a node whose number ends like [nodeNum], or 0 if never. */
    fun lastRelayedSeconds(nodeNum: Int): Int = lastRelayed[nodeNum and SUFFIX_MASK]
}
