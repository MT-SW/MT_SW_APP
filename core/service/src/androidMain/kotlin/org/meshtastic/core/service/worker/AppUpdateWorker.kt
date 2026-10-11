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
import org.koin.android.annotation.KoinWorker
import org.meshtastic.core.network.update.AppUpdateService

/** Background check for a newer MT_SW_APP release, scheduled twice a day (see `AndroidMeshWorkerManager`). */
@KoinWorker
class AppUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val appUpdateService: AppUpdateService,
) : CoroutineWorker(appContext, workerParams) {

    // The service swallows network errors itself, so a failed check just waits for the next scheduled run.
    override suspend fun doWork(): Result {
        appUpdateService.checkAndNotify()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "app_update_worker"
    }
}
