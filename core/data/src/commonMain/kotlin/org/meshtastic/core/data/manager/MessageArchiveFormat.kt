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
package org.meshtastic.core.data.manager

import org.meshtastic.core.repository.MessageArchiveConfig

/** One message as it is written to the archive; the times are already in the local time zone. */
internal data class ArchiveEntry(
    val date: String,
    val time: String,
    val conversation: String,
    val sender: String,
    val text: String,
)

internal fun archiveExtension(format: Int): String = when (format) {
    MessageArchiveConfig.FORMAT_CSV -> "csv"
    MessageArchiveConfig.FORMAT_MARKDOWN -> "md"
    else -> "txt"
}

/** A name that every file system accepts: letters, digits, dash and underscore, the rest becomes an underscore. */
internal fun safeFileName(text: String): String {
    val cleaned = text.map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }.joinToString("")
    return cleaned.trim('_').take(MAX_NAME_CHARS).ifEmpty { "rozmowa" }
}

private const val MAX_NAME_CHARS = 40

/** The content of one archive file. [entries] must already be in time order. */
internal fun formatArchive(entries: List<ArchiveEntry>, format: Int): String = when (format) {
    MessageArchiveConfig.FORMAT_CSV ->
        (
            listOf("\"data\",\"godzina\",\"rozmowa\",\"nadawca\",\"tekst\"") +
                entries.map { csvRow(it.date, it.time, it.conversation, it.sender, it.text) }
            )
            .joinToString("\n", postfix = "\n")
    MessageArchiveConfig.FORMAT_MARKDOWN ->
        entries.joinToString("\n", postfix = "\n") {
            "- **${it.time}** · ${it.conversation} · **${it.sender}**: ${it.text.oneLine()}"
        }
    else ->
        entries.joinToString("\n", postfix = "\n") {
            "[${it.date} ${it.time}] ${it.conversation} | ${it.sender}: ${it.text.oneLine()}"
        }
}

private fun String.oneLine() = replace("\r\n", " ⏎ ").replace('\n', ' ').replace('\r', ' ')

private fun csvRow(vararg cells: String) = cells.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
