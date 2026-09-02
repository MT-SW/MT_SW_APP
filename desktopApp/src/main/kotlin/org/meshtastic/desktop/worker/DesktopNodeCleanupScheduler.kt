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
package org.meshtastic.desktop.worker

import co.touchlab.kermit.Logger
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.domain.usecase.settings.CleanNodeDatabaseUseCase
import org.meshtastic.core.repository.UiPrefs
import kotlin.time.Duration.Companion.days

/**
 * Desktop equivalent of the Android `NodeCleanupWorker`. There is no WorkManager on the JVM, so the caller (see
 * `NodeCleanupLifecycle` in `Main.kt`) drives this with a simple periodic loop for the lifetime of the app process.
 * The last successful run is tracked in [UiPrefs.autoCleanNodesLastRunMillis] so runs stay spaced roughly
 * `AutoCleanNodesPolicy.checkIntervalDays` apart across app restarts.
 */
class DesktopNodeCleanupScheduler(
    private val uiPrefs: UiPrefs,
    private val cleanNodeDatabaseUseCase: CleanNodeDatabaseUseCase,
) {
    /** Runs the auto-clean check if it's due; a no-op otherwise. Safe to call as often as you like. */
    suspend fun runIfDue() {
        val policy = uiPrefs.awaitAutoCleanNodesPolicy()
        if (!policy.enabled) return

        val dueAtMillis = uiPrefs.autoCleanNodesLastRunMillis.value + policy.checkIntervalDays.days.inWholeMilliseconds
        val nowMillis = System.currentTimeMillis()
        if (nowMillis < dueAtMillis) return

        val nodesToDelete =
            cleanNodeDatabaseUseCase.getNodesToClean(
                olderThanDays = policy.inactivityDays.toFloat(),
                onlyUnknownNodes = false,
                currentTimeSeconds = nowSeconds,
            )
        if (nodesToDelete.isNotEmpty()) {
            logger.i { "Cleaning ${nodesToDelete.size} node(s) inactive for over ${policy.inactivityDays} days" }
            cleanNodeDatabaseUseCase.cleanNodes(nodesToDelete.map { it.num })
        }
        uiPrefs.setAutoCleanNodesLastRunMillis(nowMillis)
    }

    private val logger = Logger.withTag("NodeCleanupScheduler")
}