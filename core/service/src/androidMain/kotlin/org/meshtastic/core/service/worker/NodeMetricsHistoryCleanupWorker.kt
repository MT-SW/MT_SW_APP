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
package org.meshtastic.core.service.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import org.koin.android.annotation.KoinWorker
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.repository.NodeMetricsHistoryRepository

/** Prunes old points from [NodeMetricsHistoryRepository] so the Network Health trend charts stay bounded in size. */
@KoinWorker
class NodeMetricsHistoryCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val nodeMetricsHistoryRepository: NodeMetricsHistoryRepository,
) : CoroutineWorker(appContext, workerParams) {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = try {
        val cutoff = nowMillis - RETENTION_DAYS * MILLIS_PER_DAY
        logger.d { "Cleaning node metrics history older than $RETENTION_DAYS days" }
        nodeMetricsHistoryRepository.deleteOlderThan(cutoff)
        logger.i { "Successfully cleaned old NodeMetricsHistory entries" }
        Result.success()
    } catch (e: Exception) {
        logger.e(e) { "Failed to clean NodeMetricsHistory entries" }
        Result.failure()
    }

    companion object {
        const val WORK_NAME = "node_metrics_history_cleanup_worker"
        private const val RETENTION_DAYS = 30L
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    private val logger = Logger.withTag(WORK_NAME)
}
