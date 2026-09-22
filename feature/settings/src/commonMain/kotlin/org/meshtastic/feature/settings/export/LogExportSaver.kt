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
package org.meshtastic.feature.settings.export

import androidx.compose.runtime.Composable

/** Plain-text log content to save to a user-chosen file, shared by the Radio and MQTT Sniffer Log screens. */
data class LogExportContent(val content: ByteArray, val fileName: String, val mimeType: String = "text/plain") {
    override fun equals(other: Any?): Boolean = this === other ||
        (
            other is LogExportContent &&
                content.contentEquals(other.content) &&
                fileName == other.fileName &&
                mimeType == other.mimeType
            )

    override fun hashCode(): Int = content.contentHashCode() * 31 * 31 + fileName.hashCode() * 31 + mimeType.hashCode()
}

/**
 * Returns a launcher that saves [LogExportContent] to the platform's file system.
 *
 * On Android this opens a SAF document-picker (ACTION_CREATE_DOCUMENT). On Desktop this writes to a user-chosen file
 * via a file dialog. Mirrors `feature/discovery`'s `ExportSaverLauncher` -- kept as its own small copy here rather than
 * a cross-feature dependency, matching this module's existing pattern of a self-contained expect/actual per feature
 * (see the Desktop image picker).
 */
@Composable expect fun rememberLogExportSaver(): LogExportSaverLauncher

/** Platform-agnostic handle for triggering a file-save from log content. */
fun interface LogExportSaverLauncher {
    fun save(export: LogExportContent)
}
