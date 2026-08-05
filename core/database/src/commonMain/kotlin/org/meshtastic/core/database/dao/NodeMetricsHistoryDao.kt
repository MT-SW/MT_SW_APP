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
package org.meshtastic.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.database.entity.NodeMetricsHistoryEntity

@Dao
interface NodeMetricsHistoryDao {

    @Insert suspend fun insert(entry: NodeMetricsHistoryEntity)

    /** Returns history for [num] within [sinceTimestamp], oldest first — ready to feed straight into a chart. */
    @Query(
        """
        SELECT * FROM node_metrics_history
        WHERE num = :num AND timestamp >= :sinceTimestamp
        ORDER BY timestamp ASC
        """,
    )
    fun getHistoryFor(num: Int, sinceTimestamp: Long): Flow<List<NodeMetricsHistoryEntity>>

    @Query("DELETE FROM node_metrics_history WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    /** Returns all history points across all nodes since [sinceTimestamp], oldest first — grouped by caller. */
    @Query("SELECT * FROM node_metrics_history WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getAllHistorySince(sinceTimestamp: Long): Flow<List<NodeMetricsHistoryEntity>>
}
