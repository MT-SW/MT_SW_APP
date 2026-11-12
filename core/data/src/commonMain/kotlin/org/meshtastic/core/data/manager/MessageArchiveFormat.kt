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

/**
 * One message as it is written to the archive; the times are already in the local time zone. The last fields are for
 * restoring: [millis] is the exact time, [channelName] and [channelIndex] tell which channel it was on (restoring goes
 * by the name, because the channels may be in other slots by then), [address] is the conversation (`^all` for a
 * channel, otherwise the node id), [senderId] the node that sent it, [fromLocal] whether it was sent from this radio
 * and [packetId] the number of the message, which keeps a restore from adding it twice.
 */
internal data class ArchiveEntry(
    val date: String,
    val time: String,
    val conversation: String,
    val sender: String,
    val text: String,
    val millis: Long = 0L,
    val channelName: String = "",
    val channelIndex: Int = 0,
    val address: String = "",
    val senderId: String = "",
    val fromLocal: Boolean = false,
    val packetId: Int = 0,
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
            listOf(csvRow(*CSV_HEADER)) +
                entries.map {
                    csvRow(
                        it.date,
                        it.time,
                        it.conversation,
                        it.sender,
                        it.text,
                        it.millis.toString(),
                        it.channelName,
                        it.channelIndex.toString(),
                        it.address,
                        it.senderId,
                        if (it.fromLocal) "1" else "0",
                        it.packetId.toString(),
                    )
                }
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

private val CSV_HEADER =
    arrayOf(
        "data",
        "godzina",
        "rozmowa",
        "nadawca",
        "tekst",
        "czas_ms",
        "kanal",
        "indeks_kanalu",
        "adres",
        "id_nadawcy",
        "moja",
        "id_wiadomosci",
    )

private const val CSV_RESTORE_COLUMNS = 12

/** A message read back from a CSV archive file, with what is needed to put it into the right conversation. */
internal data class ArchiveRecord(
    val millis: Long,
    val text: String,
    val channelName: String,
    val channelIndex: Int,
    val address: String,
    val senderId: String,
    val fromLocal: Boolean,
    val packetId: Int,
)

/**
 * The messages of a CSV archive file. Rows without the restore columns (files from before they existed) and the header
 * are skipped; cells are in quotes, a quote inside a cell is doubled and a cell may span lines.
 */
internal fun parseArchiveCsv(csv: String): List<ArchiveRecord> = csvRows(csv).mapNotNull { row ->
    if (row.size < CSV_RESTORE_COLUMNS) return@mapNotNull null
    val millis = row[5].toLongOrNull() ?: return@mapNotNull null
    val address = row[8]
    if (millis <= 0L || address.isEmpty()) return@mapNotNull null
    ArchiveRecord(
        millis = millis,
        text = row[4],
        channelName = row[6],
        channelIndex = row[7].toIntOrNull() ?: 0,
        address = address,
        senderId = row[9],
        fromLocal = row[10] == "1",
        packetId = row[11].toIntOrNull() ?: 0,
    )
}

private fun csvRows(csv: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var quoted = false
    var i = 0
    while (i < csv.length) {
        val c = csv[i]
        when {
            quoted && c == '"' && i + 1 < csv.length && csv[i + 1] == '"' -> {
                cell.append('"')
                i++
            }
            c == '"' -> quoted = !quoted
            !quoted && c == ',' -> {
                row.add(cell.toString())
                cell.clear()
            }
            !quoted && (c == '\n' || c == '\r') -> {
                if (c == '\r' && i + 1 < csv.length && csv[i + 1] == '\n') i++
                if (cell.isNotEmpty() || row.isNotEmpty()) {
                    row.add(cell.toString())
                    rows.add(row)
                }
                row = mutableListOf()
                cell.clear()
            }
            else -> cell.append(c)
        }
        i++
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) {
        row.add(cell.toString())
        rows.add(row)
    }
    return rows
}

private fun csvRow(vararg cells: String) = cells.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
