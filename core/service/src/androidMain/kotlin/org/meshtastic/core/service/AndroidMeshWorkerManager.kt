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
package org.meshtastic.core.service

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.koin.core.annotation.Single
import org.meshtastic.core.repository.MeshWorkerManager
import org.meshtastic.core.repository.PersistedPacketId
import org.meshtastic.core.service.worker.NodeCleanupWorker
import org.meshtastic.core.service.worker.SendMessageWorker
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

@Single
class AndroidMeshWorkerManager(private val workManager: WorkManager) : MeshWorkerManager {
    override fun enqueueSendMessage(persistedId: PersistedPacketId) {
        val workRequest =
            OneTimeWorkRequestBuilder<SendMessageWorker>()
                .setInputData(
                    workDataOf(
                        SendMessageWorker.KEY_PACKET_UUID to persistedId.uuid,
                        SendMessageWorker.KEY_MY_NODE_NUM to persistedId.myNodeNum,
                    ),
                )
                .build()

        // This UUID name does not replace a persisted legacy `send_message_<packetId>` job. Both can briefly coexist
        // after upgrade, but their transactional row claim allows only one send owner. KEEP also prevents repeated
        // scheduling of this exact row from cancelling an active worker after it has handed the packet to the radio.
        workManager.enqueueUniqueWork(
            "${SendMessageWorker.WORK_NAME_PREFIX}${persistedId.myNodeNum}_${persistedId.uuid}",
            ExistingWorkPolicy.KEEP,
            workRequest,
        )
    }

    override fun scheduleNodeCleanup(intervalDays: Int) {
        val cleanupRequest =
            PeriodicWorkRequestBuilder<NodeCleanupWorker>(repeatInterval = intervalDays.days.toJavaDuration()).build()

        workManager.enqueueUniquePeriodicWork(
            NodeCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupRequest,
        )
    }
}
