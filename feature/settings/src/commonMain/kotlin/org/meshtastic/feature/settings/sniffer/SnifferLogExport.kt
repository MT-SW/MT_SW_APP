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
package org.meshtastic.feature.settings.sniffer

import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.datastore.SnifferLogFormat
import org.meshtastic.feature.settings.debugging.sanitizeForExport
import org.meshtastic.feature.settings.export.LogExportContent

/** Multi-line, redacted representation for export -- reuses the Debug Panel's redaction like the per-type cards do. */
private fun SnifferExportRow.summaryText(): String = sanitizeForExport(
    buildString {
        topic?.let { appendLine(it) }
        val fromLabel = fromShortName?.let { "$fromId ($it)" } ?: fromId
        val toLabel = toShortName?.let { "$toId ($it)" } ?: toId
        appendLine("$fromLabel -> $toLabel" + (channel?.let { " (ch $it)" } ?: ""))
        val portText =
            if (isEncrypted) "encrypted" else portNum?.let { "port $it" } ?: if (isJson) "JSON" else "unknown port"
        appendLine(portText)
        if (gatewayIds.isNotEmpty()) appendLine("via " + gatewayIds.joinToString(", "))
        val signal = listOfNotNull(rssi?.let { "RSSI $it" }, snr?.let { "SNR $it" }).joinToString(" • ")
        if (signal.isNotEmpty()) appendLine(signal)
        append(decodedPayload ?: payloadHex)
    },
)

private fun List<SnifferExportRow>.toTxtBytes(): ByteArray = buildString {
    this@toTxtBytes.forEach { row ->
        appendLine(
            "${DateFormatter.formatDate(row.receivedAtMillis)} " +
                DateFormatter.formatTimeWithSeconds(row.receivedAtMillis),
        )
        appendLine(row.summaryText())
        appendLine()
    }
}
    .encodeToByteArray()

private val SnifferExportJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private fun List<SnifferExportRow>.toJsonBytes(): ByteArray = SnifferExportJson.encodeToString(this).encodeToByteArray()

/** Parses a previously exported JSON log back into rows, or null if [bytes] isn't one this app wrote. */
fun parseSnifferExportJson(bytes: ByteArray): List<SnifferExportRow>? =
    runCatching { SnifferExportJson.decodeFromString<List<SnifferExportRow>>(bytes.decodeToString()) }.getOrNull()

private val CSV_HEADER =
    listOf(
        "source",
        "receivedAtMillis",
        "fromId",
        "fromShortName",
        "toId",
        "toShortName",
        "channel",
        "portNum",
        "isEncrypted",
        "gatewayIds",
        "topic",
        "payload",
    )

private fun csvField(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
    "\"" + value.replace("\"", "\"\"") + "\""
} else {
    value
}

private fun List<SnifferExportRow>.toCsvBytes(): ByteArray = buildString {
    appendLine(CSV_HEADER.joinToString(","))
    this@toCsvBytes.forEach { row ->
        val payload = sanitizeForExport(row.decodedPayload ?: row.payloadHex)
        val fields =
            listOf(
                row.source,
                row.receivedAtMillis.toString(),
                row.fromId,
                row.fromShortName.orEmpty(),
                row.toId,
                row.toShortName.orEmpty(),
                row.channel.orEmpty(),
                row.portNum?.toString().orEmpty(),
                row.isEncrypted.toString(),
                row.gatewayIds.joinToString(";"),
                row.topic.orEmpty(),
                payload,
            )
        appendLine(fields.joinToString(",") { csvField(it) })
    }
}
    .encodeToByteArray()

/**
 * Builds the file to hand to [org.meshtastic.feature.settings.export.rememberLogExportSaver] for [rows] in [format].
 * [rows] should already be in the order the file should read (oldest first, matching the old per-type exporters).
 */
fun buildSnifferExport(rows: List<SnifferExportRow>, format: SnifferLogFormat, sourceLabel: String): LogExportContent {
    val timestamp = Clock.System.now().toEpochMilliseconds()
    return when (format) {
        SnifferLogFormat.TXT ->
            LogExportContent(
                content = rows.toTxtBytes(),
                fileName = "sniffer_log_${sourceLabel}_$timestamp.txt",
                mimeType = "text/plain",
            )

        SnifferLogFormat.JSON ->
            LogExportContent(
                content = rows.toJsonBytes(),
                fileName = "sniffer_log_${sourceLabel}_$timestamp.json",
                mimeType = "application/json",
            )

        SnifferLogFormat.CSV ->
            LogExportContent(
                content = rows.toCsvBytes(),
                fileName = "sniffer_log_${sourceLabel}_$timestamp.csv",
                mimeType = "text/csv",
            )
    }
}
