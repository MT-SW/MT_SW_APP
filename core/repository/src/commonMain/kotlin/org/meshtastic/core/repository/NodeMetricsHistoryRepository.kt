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

import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.model.NodeMetricsHistoryEntry

/**
 * Repository for accumulated node telemetry history, used to render trend charts on the Network Health screen.
 *
 * Unlike the live per-node telemetry fields, this repository never overwrites — every recorded point is appended and
 * retained until pruned by [deleteOlderThan].
 *
 * This interface is shared across platforms via Kotlin Multiplatform (KMP).
 */
interface NodeMetricsHistoryRepository {
    /** Records a new telemetry snapshot for [nodeNum] at [timestamp]. */
    suspend fun insert(entry: NodeMetricsHistoryEntry)

    /** Returns history points for [nodeNum] since [sinceTimestamp], oldest first. */
    fun getHistoryFor(nodeNum: Int, sinceTimestamp: Long): Flow<List<NodeMetricsHistoryEntry>>

    /** Returns history points for every node since [sinceTimestamp], oldest first. */
    fun getAllHistorySince(sinceTimestamp: Long): Flow<List<NodeMetricsHistoryEntry>>

    /** Prunes history points older than [cutoffTimestamp]. */
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}
