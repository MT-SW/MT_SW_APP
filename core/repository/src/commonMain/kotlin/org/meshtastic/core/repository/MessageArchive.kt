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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import okio.BufferedSink

/**
 * The settings of the automatic message archive. [folder] is the address of the chosen folder as the platform gave it
 * (empty = none chosen). [intervalMinutes] is how often the archive is brought up to date. [format] is one of
 * [FORMAT_TEXT], [FORMAT_CSV], [FORMAT_MARKDOWN]. With [perConversation] every conversation gets its own file for a day,
 * otherwise there is one file for all of them.
 */
data class MessageArchiveConfig(
    val enabled: Boolean = false,
    val folder: String = "",
    val intervalMinutes: Int = DEFAULT_INTERVAL_MINUTES,
    val format: Int = FORMAT_TEXT,
    val perConversation: Boolean = false,
) {
    /** Written as `enabled|intervalMinutes|format|perConversation|folder`. */
    fun encode(): String = listOf(
        if (enabled) 1 else 0,
        intervalMinutes,
        format,
        if (perConversation) 1 else 0,
    )
        .joinToString("|") + "|" + folder

    companion object {
        const val FORMAT_TEXT = 0
        const val FORMAT_CSV = 1
        const val FORMAT_MARKDOWN = 2
        const val DEFAULT_INTERVAL_MINUTES = 15
        private const val FIELDS = 5

        /** Reads what [encode] wrote; anything unreadable gives the defaults. */
        fun decode(text: String?): MessageArchiveConfig {
            val parts = text?.split('|', limit = FIELDS) ?: return MessageArchiveConfig()
            if (parts.size < FIELDS) return MessageArchiveConfig()
            val numbers = parts.take(FIELDS - 1).map { it.toIntOrNull() ?: return MessageArchiveConfig() }
            return MessageArchiveConfig(
                enabled = numbers[0] != 0,
                intervalMinutes = numbers[1].coerceAtLeast(1),
                format = numbers[2].coerceIn(FORMAT_TEXT, FORMAT_MARKDOWN),
                perConversation = numbers[3] != 0,
                folder = parts[FIELDS - 1],
            )
        }
    }
}

/** Where the archive settings are kept. */
interface MessageArchivePrefs {
    val config: StateFlow<MessageArchiveConfig>

    fun setConfig(config: MessageArchiveConfig)
}

/** When the archive was last brought up to date, how many files that wrote, and whether it went wrong. */
data class MessageArchiveState(
    val running: Boolean = false,
    val lastRunMillis: Long = 0L,
    val lastFileCount: Int = 0,
    val lastFailed: Boolean = false,
)

/** Writes the messages of today and yesterday into the chosen folder. */
interface MessageArchiver {
    val state: StateFlow<MessageArchiveState>

    /** Brings the archive up to date now; true when every file was written. */
    suspend fun archiveNow(): Boolean
}

/** Starts the periodic archiving when the mesh service starts. */
fun interface MessageArchiveMonitor {
    fun start(scope: CoroutineScope)
}

/** Writes files into a folder the user chose (Android: a document tree, desktop: a directory, iPhone: a bookmark). */
interface FolderService {
    /** Writes the file [name] in [folder] (created when missing, otherwise replaced). True when it worked. */
    suspend fun writeFile(folder: String, name: String, block: suspend (BufferedSink) -> Unit): Boolean
}
