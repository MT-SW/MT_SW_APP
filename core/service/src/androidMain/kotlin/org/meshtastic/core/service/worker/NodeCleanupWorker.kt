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
import kotlinx.coroutines.CancellationException
import org.koin.android.annotation.KoinWorker
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.domain.usecase.settings.CleanNodeDatabaseUseCase
import org.meshtastic.core.repository.UiPrefs

@KoinWorker
class NodeCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val cleanNodeDatabaseUseCase: CleanNodeDatabaseUseCase,
    private val uiPrefs: UiPrefs,
) : CoroutineWorker(appContext, workerParams) {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = try {
        val policy = uiPrefs.awaitAutoCleanNodesPolicy()
        if (!policy.enabled) {
            logger.i { "Skipping node cleanup because auto-clean is disabled" }
        } else {
            val nodesToDelete =
                cleanNodeDatabaseUseCase.getNodesToClean(
                    olderThanDays = policy.inactivityDays.toFloat(),
                    onlyUnknownNodes = false,
                    currentTimeSeconds = nowSeconds,
                )
            if (nodesToDelete.isEmpty()) {
                logger.i { "No inactive nodes to clean" }
            } else {
                logger.d { "Cleaning ${nodesToDelete.size} node(s) inactive for over ${policy.inactivityDays} days" }
                cleanNodeDatabaseUseCase.cleanNodes(nodesToDelete.map { it.num })
                logger.i { "Successfully auto-cleaned inactive nodes" }
            }
        }
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e(e) { "Failed to auto-clean node database" }
        Result.failure()
    }

    companion object {
        const val WORK_NAME = "node_cleanup_worker"
    }

    private val logger = Logger.withTag(WORK_NAME)
}