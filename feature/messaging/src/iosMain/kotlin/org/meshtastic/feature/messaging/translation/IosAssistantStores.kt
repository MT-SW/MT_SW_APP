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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.meshtastic.core.repository.AssistantMemory
import org.meshtastic.core.repository.MemoryNote

private val json = Json { ignoreUnknownKeys = true }

/** Assistant conversations kept as one small file each, only in the app's private folder. */
class IosAssistantHistoryStore(baseDir: String) : AssistantHistoryStore {
    private val fs = FileSystem.SYSTEM
    private val dir: Path = (baseDir + "/assistant-chats").toPath().also { fs.createDirectories(it) }

    private fun fileOf(id: String): Path =
        dir / (id.filter { it.isLetterOrDigit() || it == '-' || it == '_' } + ".json")

    override suspend fun list(): List<StoredChat> = fs.list(dir)
        .mapNotNull { p ->
            runCatching { json.decodeFromString<StoredChat>(fs.read(p) { readUtf8() }) }.getOrNull()
        }
        .sortedByDescending { it.updatedAt }

    override suspend fun save(chat: StoredChat) {
        fs.write(fileOf(chat.id)) { writeUtf8(json.encodeToString(StoredChat.serializer(), chat)) }
    }

    override suspend fun delete(id: String) {
        runCatching { fs.delete(fileOf(id), mustExist = false) }
    }
}

/** Assistant notes kept as small text files, only in the app's private folder. */
class IosAssistantMemory(baseDir: String) : AssistantMemory {
    private val fs = FileSystem.SYSTEM
    private val dir: Path = (baseDir + "/assistant-memory").toPath().also { fs.createDirectories(it) }
    private val state = MutableStateFlow(load())
    override val notes: StateFlow<List<MemoryNote>> = state

    private fun safe(name: String) = name.filter { it.isLetterOrDigit() || it == '-' || it == '_' }

    private fun load(): List<MemoryNote> = fs.list(dir)
        .filter { it.name.endsWith(".md") }
        .sortedBy { it.name }
        .mapNotNull { p ->
            val text = runCatching { fs.read(p) { readUtf8() } }.getOrNull()?.trim().orEmpty()
            val name = p.name.removeSuffix(".md")
            if (text.isEmpty()) null else MemoryNote(name, text, name == AssistantMemory.USER_NOTE)
        }

    override fun save(name: String, text: String) {
        val n = safe(name)
        if (n.isEmpty()) return
        if (text.isBlank()) {
            delete(n)
            return
        }
        fs.write(dir / "$n.md") { writeUtf8(text.trim()) }
        val byUser = n == AssistantMemory.USER_NOTE
        state.update { old -> old.filterNot { it.name == n } + MemoryNote(n, text.trim(), byUser) }
    }

    override fun delete(name: String) {
        val n = safe(name)
        runCatching { fs.delete(dir / "$n.md", mustExist = false) }
        state.update { old -> old.filterNot { it.name == n } }
    }

    override fun clearLearned() {
        state.value.filterNot { it.byUser }.forEach { delete(it.name) }
    }

    override fun promptText(maxChars: Int): String {
        val out = StringBuilder()
        for (note in state.value.sortedByDescending { it.byUser }) {
            val line =
                if (note.byUser) {
                    "User's own instructions (always follow them): ${note.text}"
                } else {
                    "${note.name}: ${note.text}"
                }
            if (out.length + line.length + 1 > maxChars) break
            out.append(line).append('\n')
        }
        return out.toString().trim()
    }
}
