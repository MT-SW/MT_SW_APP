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
package org.meshtastic.feature.messaging

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit): () -> Unit = {
    val chooser =
        JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Obrazy", "jpg", "jpeg", "png", "gif", "webp", "bmp")
            isAcceptAllFileFilterUsed = false
        }
    val result = chooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        val file: File = chooser.selectedFile
        try {
            val bytes = file.readBytes()
            val mimeType =
                when (file.extension.lowercase()) {
                    "jpg",
                    "jpeg",
                    -> "image/jpeg"

                    "png" -> "image/png"

                    "gif" -> "image/gif"

                    "webp" -> "image/webp"

                    "bmp" -> "image/bmp"

                    else -> "application/octet-stream"
                }
            onImagePicked(bytes, mimeType)
        } catch (ex: Exception) {
            Logger.e(ex) { "ImagePicker: failed to read selected file" }
        }
    } else {
        Logger.i { "ImagePicker: selection cancelled" }
    }
}
