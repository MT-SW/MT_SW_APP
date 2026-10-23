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
package org.meshtastic.core.repository

import kotlinx.coroutines.flow.StateFlow

/** One small note file of the assistant; [byUser] notes are written by the user and never changed by the assistant. */
data class MemoryNote(val name: String, val text: String, val byUser: Boolean)

/**
 * What the assistant remembers between conversations: a few short note files kept only on this device. The user's own
 * instructions are one of them; the others are written by the assistant itself and can be read, edited and deleted.
 */
interface AssistantMemory {
    val notes: StateFlow<List<MemoryNote>>

    /** Writes (or replaces) a note; an empty text deletes it. */
    fun save(name: String, text: String)

    fun delete(name: String)

    /** Deletes all notes written by the assistant; the user's own instructions stay. */
    fun clearLearned()

    /** The notes as one text for the prompt, the user's instructions first, cut to [maxChars]. */
    fun promptText(maxChars: Int): String

    companion object {
        /** Name of the note with the user's own instructions. */
        const val USER_NOTE = "moje-instrukcje"
    }
}
