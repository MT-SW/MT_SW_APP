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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.meshtastic.core.database.DatabaseProvider
import org.meshtastic.core.database.entity.QuickChatAction
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.QuickChatActionRepository
import org.meshtastic.core.repository.UiPrefs

@Single
class QuickChatActionRepositoryImpl(
    private val dbManager: DatabaseProvider,
    private val dispatchers: CoroutineDispatchers,
    private val uiPrefs: UiPrefs,
) : QuickChatActionRepository {

    private val seedMutex = Mutex()

    override fun getAllActions(): Flow<List<QuickChatAction>> =
        dbManager.observeCurrentDb { it.quickChatActionDao().getAll() }
            .onStart { seedDefaultsIfNeeded() }
            .flowOn(dispatchers.io)

    /**
     * Populates the built-in default Quick Chat templates the first time this fork is used, so a fresh install
     * doesn't start with an empty list. Gated by [UiPrefs.quickChatDefaultsSeeded] so it only ever runs once —
     * if the user later deletes some or all of them, they stay deleted.
     */
    private suspend fun seedDefaultsIfNeeded() {
        if (uiPrefs.quickChatDefaultsSeeded.value) return
        seedMutex.withLock {
            if (uiPrefs.quickChatDefaultsSeeded.value) return@withLock
            val seeded =
                withContext(dispatchers.io) {
                    dbManager.withDb { db -> DEFAULT_QUICK_CHAT_ACTIONS.forEach { db.quickChatActionDao().upsert(it) } }
                }
            if (seeded != null) uiPrefs.setQuickChatDefaultsSeeded(true)
        }
    }

    // Writes go through withDb so they register with the cross-transport merge drain barrier (see DatabaseProvider).
    override suspend fun upsert(action: QuickChatAction) {
        withContext(dispatchers.io) { dbManager.withDb { it.quickChatActionDao().upsert(action) } }
    }

    override suspend fun deleteAll() {
        withContext(dispatchers.io) { dbManager.withDb { it.quickChatActionDao().deleteAll() } }
    }

    override suspend fun delete(action: QuickChatAction) {
        withContext(dispatchers.io) { dbManager.withDb { it.quickChatActionDao().delete(action) } }
    }

    override suspend fun setItemPosition(uuid: Long, newPos: Int) {
        withContext(dispatchers.io) { dbManager.withDb { it.quickChatActionDao().updateActionPosition(uuid, newPos) } }
    }

    private companion object {
        val DEFAULT_QUICK_CHAT_ACTIONS =
            listOf(
                QuickChatAction(
                    name = "S_P",
                    message = "scyzoryk pomoc",
                    mode = QuickChatAction.Mode.Instant,
                    position = 0,
                ),
                QuickChatAction(
                    name = "S_T",
                    message = "scyzoryk test",
                    mode = QuickChatAction.Mode.Instant,
                    position = 1,
                ),
                QuickChatAction(
                    name = "S_R",
                    message = "scyzoryk range",
                    mode = QuickChatAction.Mode.Instant,
                    position = 2,
                ),
                QuickChatAction(
                    name = "S_W",
                    message = "scyzoryk pogoda",
                    mode = QuickChatAction.Mode.Instant,
                    position = 3,
                ),
                QuickChatAction(
                    name = "S_I",
                    message = "scyzoryk info",
                    mode = QuickChatAction.Mode.Instant,
                    position = 4,
                ),
                QuickChatAction(
                    name = "S_A",
                    message = "scyzoryk aktualnosci",
                    mode = QuickChatAction.Mode.Instant,
                    position = 5,
                ),
            )
    }
}