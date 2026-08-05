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
package org.meshtastic.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.meshtastic.core.database.DatabaseProvider
import org.meshtastic.core.database.entity.asEntity
import org.meshtastic.core.database.entity.asExternalModel
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.NodeMetricsHistoryEntry
import org.meshtastic.core.repository.NodeMetricsHistoryRepository

/** Repository implementation for accumulated node telemetry history, backed by the local database. */
@Single
class NodeMetricsHistoryRepositoryImpl(
    private val dbManager: DatabaseProvider,
    private val dispatchers: CoroutineDispatchers,
) : NodeMetricsHistoryRepository {

    override suspend fun insert(entry: NodeMetricsHistoryEntry) = withContext(dispatchers.io) {
        dbManager.withDb { it.nodeMetricsHistoryDao().insert(entry.asEntity()) }
        Unit
    }

    override fun getHistoryFor(nodeNum: Int, sinceTimestamp: Long): Flow<List<NodeMetricsHistoryEntry>> =
        dbManager.currentDb
            .flatMapLatest { it.nodeMetricsHistoryDao().getHistoryFor(nodeNum, sinceTimestamp) }
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(dispatchers.io)

    override fun getAllHistorySince(sinceTimestamp: Long): Flow<List<NodeMetricsHistoryEntry>> = dbManager.currentDb
        .flatMapLatest { it.nodeMetricsHistoryDao().getAllHistorySince(sinceTimestamp) }
        .map { list -> list.map { it.asExternalModel() } }
        .flowOn(dispatchers.io)

    override suspend fun deleteOlderThan(cutoffTimestamp: Long) = withContext(dispatchers.io) {
        dbManager.withDb { it.nodeMetricsHistoryDao().deleteOlderThan(cutoffTimestamp) }
        Unit
    }
}
