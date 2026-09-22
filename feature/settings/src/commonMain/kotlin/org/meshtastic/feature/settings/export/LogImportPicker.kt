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

/** A file picked to load back into a Sniffer Log panel -- see [rememberLogImportPicker]. */
data class LogImportContent(val content: ByteArray, val fileName: String) {
    override fun equals(other: Any?): Boolean = this === other ||
        (other is LogImportContent && content.contentEquals(other.content) && fileName == other.fileName)

    override fun hashCode(): Int = content.contentHashCode() * 31 + fileName.hashCode()
}

/**
 * Returns a launcher that opens the platform's file picker and reports the chosen file's bytes and name via [onResult].
 * The counterpart to [rememberLogExportSaver] -- same per-platform SAF/`JFileChooser`/stub split, mirrored here as its
 * own small expect/actual rather than a cross-feature dependency, matching this module's existing pattern.
 *
 * [onResult] is invoked on the main dispatcher once a file is chosen; nothing is called if the user cancels the picker.
 */
@Composable expect fun rememberLogImportPicker(onResult: (LogImportContent) -> Unit): LogImportPickerLauncher

/** Platform-agnostic handle for triggering a file-open picker. */
fun interface LogImportPickerLauncher {
    fun pick()
}
