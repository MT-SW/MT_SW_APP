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

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.repository.AssistantMemory
import org.meshtastic.core.repository.MemoryNote

/** The notes of the assistant as small text files in one folder; nothing leaves the device. */
class FileAssistantMemory(private val dir: File) : AssistantMemory {

    private val state = MutableStateFlow(load())
    override val notes: StateFlow<List<MemoryNote>> = state

    @Synchronized
    override fun save(name: String, text: String) {
        val clean = safeName(name)
        if (clean.isEmpty()) return
        val body = text.trim().take(if (clean == AssistantMemory.USER_NOTE) USER_MAX else LEARNED_MAX)
        val file = File(dir, "$clean.md")
        if (body.isEmpty()) {
            file.delete()
        } else {
            val isNew = !file.exists()
            if (isNew && clean != AssistantMemory.USER_NOTE && state.value.count { !it.byUser } >= MAX_LEARNED) return
            dir.mkdirs()
            file.writeText(body)
        }
        state.value = load()
    }

    @Synchronized
    override fun delete(name: String) {
        File(dir, "${safeName(name)}.md").delete()
        state.value = load()
    }

    @Synchronized
    override fun clearLearned() {
        state.value.filter { !it.byUser }.forEach { File(dir, "${it.name}.md").delete() }
        state.value = load()
    }

    override fun promptText(maxChars: Int): String {
        val list = state.value.sortedByDescending { it.byUser }
        val out = StringBuilder()
        for (note in list) {
            val line = if (note.byUser) "User's own instructions (always follow them): ${note.text}" else "${note.name}: ${note.text}"
            if (out.length + line.length + 1 > maxChars) break
            out.append(line).append('\n')
        }
        return out.toString().trim()
    }

    private fun load(): List<MemoryNote> =
        dir.listFiles { f -> f.extension == "md" }
            ?.sortedBy { it.name }
            ?.mapNotNull { f ->
                val text = runCatching { f.readText() }.getOrNull()?.trim().orEmpty()
                if (text.isEmpty()) null else MemoryNote(f.nameWithoutExtension, text, f.nameWithoutExtension == AssistantMemory.USER_NOTE)
            }
            .orEmpty()

    companion object {
        const val USER_MAX = 1500
        const val LEARNED_MAX = 700
        const val MAX_LEARNED = 8

        fun safeName(name: String): String =
            name.trim().lowercase().map { if (it.isLetterOrDigit()) it else '-' }.joinToString("").trim('-').take(40)
    }
}
