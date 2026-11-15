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

import org.meshtastic.core.model.util.ON_DEMAND_PORT_NUM
import org.meshtastic.proto.OnDemand
import org.meshtastic.proto.OnDemandType
import org.meshtastic.proto.Presence

/** MT_SW: the activity status of a node's user (see the Presence message of the on-demand protocol). */
enum class NodePresenceState {
    /** The node's user is around (a client is attached to the node). */
    ONLINE,

    /** The node's user is not around. */
    OFFLINE,
}

/**
 * The last activity status heard from a node.
 *
 * @param state the status that node reported.
 * @param sinceMillis since when the node has been in [state], as far as it said (the time of the answer minus the
 *   seconds it reported, or just the time it was heard).
 * @param receivedAtMillis when the report arrived.
 */
data class NodePresence(val state: NodePresenceState, val sinceMillis: Long, val receivedAtMillis: Long) {

    /**
     * What to show now, or null for "nothing" (unknown). An ONLINE report holds only while the node is still heard:
     * when neither the report nor the node's last packet is newer than [expiryMillis], the person has most likely
     * left without the OFFLINE report arriving (a dead battery, out of range), so nothing is shown.
     */
    fun effective(nowMillis: Long, lastHeardSeconds: Int, expiryMillis: Long): NodePresenceState? = when (state) {
        NodePresenceState.OFFLINE -> NodePresenceState.OFFLINE
        NodePresenceState.ONLINE -> {
            val lastSign = maxOf(receivedAtMillis, lastHeardSeconds * MILLIS_PER_SECOND)
            if (nowMillis - lastSign <= expiryMillis) NodePresenceState.ONLINE else null
        }
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1000L

        /** Choices of how long an ONLINE report holds without hearing the node, in minutes. */
        val EXPIRY_STEPS_MINUTES = intArrayOf(30, 60, 120, 360, 720)
    }
}

/** Reads the last activity status of every node from the logged on-demand answers (port 354). */
object NodePresenceLogs {
    private const val MILLIS_PER_SECOND = 1000L

    /** How far back the log is read for statuses, in milliseconds. */
    const val WINDOW_MILLIS = 2 * 24 * 60 * 60 * MILLIS_PER_SECOND

    /** The newest status per node. A node whose newest report says "unknown" (sharing off) is left out. */
    fun latestPerNode(logs: List<MeshLog>): Map<Int, NodePresence> {
        val answered = HashSet<Int>()
        val result = HashMap<Int, NodePresence>()
        // Newest first, so the first status of a node is its current one.
        for (log in logs.sortedByDescending { it.received_date }) {
            if (log.fromNum in answered) continue
            val presence = decode(log) ?: continue
            answered += log.fromNum
            val state =
                when (presence.state) {
                    Presence.State.ONLINE -> NodePresenceState.ONLINE
                    Presence.State.OFFLINE -> NodePresenceState.OFFLINE
                    else -> null
                } ?: continue
            val since = log.received_date - (presence.secs_since_change ?: 0) * MILLIS_PER_SECOND
            result[log.fromNum] = NodePresence(state, sinceMillis = since, receivedAtMillis = log.received_date)
        }
        return result
    }

    private fun decode(log: MeshLog): Presence? = log.fromRadio.packet
        ?.decoded
        ?.takeIf { log.portNum == ON_DEMAND_PORT_NUM }
        ?.payload
        ?.let { payload -> runCatching { OnDemand.ADAPTER.decode(payload) }.getOrNull() }
        ?.response
        ?.takeIf { it.response_type == OnDemandType.RESPONSE_PRESENCE }
        ?.presence
}
