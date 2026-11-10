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

package org.meshtastic.core.database

import co.touchlab.kermit.Logger
import org.meshtastic.core.database.entity.MeshLog
import org.meshtastic.core.database.entity.NodeEntity

/**
 * Keeps the statistics of the nodes that two device databases both know equal. Each radio has its own database, so the
 * same node (heard by two radios) has its own telemetry history in each. Only nodes present in BOTH databases are
 * touched: a node known to one database only is never copied to the other.
 *
 * For such a node the newer node row (by last heard) refreshes the older one's telemetry, position and signal, while
 * what the user wrote (notes, name, favourite, ignore, mute) stays as it was in each database. The history of
 * telemetry, position and similar packets of the node is completed in both directions, without duplicates (a packet
 * heard by both radios counts once).
 */
object DatabaseStatsSync {
    // Position, paxcounter, telemetry, neighbour info (PortNum values).
    private val PORTS = listOf(3, 34, 67, 71)
    private const val CHUNK = 400

    /** Synchronizes [a] and [b] both ways; returns how many rows were written (nodes + history entries). */
    suspend fun sync(a: MeshtasticDatabase, b: MeshtasticDatabase): Int {
        val nodesA = a.nodeInfoDao().getAllNodesSnapshot().associateBy { it.num }
        val nodesB = b.nodeInfoDao().getAllNodesSnapshot().associateBy { it.num }
        val common = nodesA.keys.intersect(nodesB.keys).toList()
        if (common.isEmpty()) return 0
        var written = 0
        val toA = ArrayList<NodeEntity>()
        val toB = ArrayList<NodeEntity>()
        for (num in common) {
            val na = nodesA.getValue(num)
            val nb = nodesB.getValue(num)
            when {
                na.lastHeard > nb.lastHeard -> toB.add(refreshed(older = nb, newer = na))
                nb.lastHeard > na.lastHeard -> toA.add(refreshed(older = na, newer = nb))
            }
        }
        if (toA.isNotEmpty()) a.nodeInfoDao().putAll(toA)
        if (toB.isNotEmpty()) b.nodeInfoDao().putAll(toB)
        written += toA.size + toB.size
        for (part in common.chunked(CHUNK)) {
            val logsA = a.meshLogDao().getLogsFromNodesOnPorts(part, PORTS)
            val logsB = b.meshLogDao().getLogsFromNodesOnPorts(part, PORTS)
            val keysA = logsA.mapTo(HashSet()) { key(it) }
            val keysB = logsB.mapTo(HashSet()) { key(it) }
            val missingInA = logsB.filter { key(it) !in keysA }
            val missingInB = logsA.filter { key(it) !in keysB }
            if (missingInA.isNotEmpty()) a.meshLogDao().insertIgnore(missingInA)
            if (missingInB.isNotEmpty()) b.meshLogDao().insertIgnore(missingInB)
            written += missingInA.size + missingInB.size
        }
        if (written > 0) Logger.i { "Statistics sync between two device databases: $written entries" }
        return written
    }

    /** One packet is the same packet in both databases when sender, port and packet id agree. */
    private fun key(log: MeshLog): String {
        val id = log.meshPacket?.id ?: 0
        return if (id != 0) "${log.fromNum}|${log.portNum}|$id" else log.uuid
    }

    private fun refreshed(older: NodeEntity, newer: NodeEntity): NodeEntity = newer.copy(
        notes = older.notes,
        displayName = older.displayName,
        isFavorite = older.isFavorite,
        isIgnored = older.isIgnored,
        isMuted = older.isMuted,
        manuallyVerified = older.manuallyVerified,
        powerChannelLabels = older.powerChannelLabels,
    )
}
