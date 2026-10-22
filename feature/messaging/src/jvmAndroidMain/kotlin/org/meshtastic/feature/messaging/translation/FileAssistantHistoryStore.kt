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
package org.meshtastic.feature.messaging.translation

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** Stores each assistant conversation as one small JSON file in [dir]. */
class FileAssistantHistoryStore(private val dir: File) : AssistantHistoryStore {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun list(): List<StoredChat> = withContext(Dispatchers.IO) {
        dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { f ->
                runCatching { json.decodeFromString<StoredChat>(f.readText()) }
                    .onFailure { Logger.w(tag = TAG, throwable = it) { "Cannot read ${f.name}" } }
                    .getOrNull()
            }
            ?.sortedByDescending { it.updatedAt }
            .orEmpty()
    }

    override suspend fun save(chat: StoredChat) {
        withContext(Dispatchers.IO) {
            dir.mkdirs()
            val target = fileOf(chat.id)
            val temp = File(dir, chat.id + ".tmp")
            temp.writeText(json.encodeToString(StoredChat.serializer(), chat))
            if (target.exists()) target.delete()
            temp.renameTo(target)
        }
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) { fileOf(id).delete() }
    }

    private fun fileOf(id: String) = File(dir, id.filter { it.isLetterOrDigit() } + ".json")

    private companion object {
        const val TAG = "AssistantHistory"
    }
}
