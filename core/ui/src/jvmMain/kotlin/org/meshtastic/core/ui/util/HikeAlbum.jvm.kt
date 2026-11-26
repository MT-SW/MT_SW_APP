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
@file:Suppress("TooGenericExceptionCaught")

package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import org.meshtastic.core.common.AppDataRoot
import java.io.File
import javax.swing.JFileChooser

/** By default beside the logs and the other data of the app; the person can choose any other folder. */
actual fun defaultAlbumFolder(): String? = File(AppDataRoot.dir.path, "Zdjęcia").path

actual fun saveToHikeAlbum(bytes: ByteArray, fileName: String, folder: String?): Boolean =
    try {
        val dir = File(folder?.takeIf { it.isNotBlank() } ?: defaultAlbumFolder().orEmpty())
        dir.mkdirs()
        File(dir, fileName).writeBytes(bytes)
        Logger.i { "Album wędrówek: zapisano ${bytes.size} bajtów w folderze" }
        true
    } catch (e: Exception) {
        Logger.w { "Album wędrówek: zapis w folderze nie udał się: ${e.message}" }
        false
    }

@Composable
actual fun rememberAlbumFolderPicker(onPicked: (String) -> Unit): (() -> Unit)? {
    val current = rememberUpdatedState(onPicked)
    return {
        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            Logger.i { "Album wędrówek: wybrano folder" }
            current.value(chooser.selectedFile.path)
        }
    }
}
