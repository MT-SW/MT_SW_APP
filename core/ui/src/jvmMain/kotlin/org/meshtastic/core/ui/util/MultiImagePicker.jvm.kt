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
@file:Suppress("ParameterNaming", "TooGenericExceptionCaught")

package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberMultiImagePicker(onPicked: (List<PickedImage>) -> Unit): (() -> Unit)? {
    val current = rememberUpdatedState(onPicked)
    return {
        val chooser =
            JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("Obrazy", "jpg", "jpeg", "png", "webp")
                isAcceptAllFileFilterUsed = false
                isMultiSelectionEnabled = true
            }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val images =
                chooser.selectedFiles.take(MAX_PICKED_IMAGES).mapNotNull { file ->
                    try {
                        val mime = if (file.extension.lowercase() == "png") "image/png" else "image/jpeg"
                        PickedImage(file.readBytes(), mime)
                    } catch (e: Exception) {
                        Logger.w { "Galeria: nie udało się odczytać pliku: ${e.message}" }
                        null
                    }
                }
            Logger.i { "Galeria: wybrano ${chooser.selectedFiles.size}, odczytano ${images.size}" }
            current.value(images)
        }
    }
}
