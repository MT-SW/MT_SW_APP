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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger

@Composable
actual fun rememberMultiImagePicker(onPicked: (List<PickedImage>) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val current = rememberUpdatedState(onPicked)
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(MAX_PICKED_IMAGES),
        ) { uris: List<Uri> ->
            if (uris.isEmpty()) {
                Logger.i { "Galeria: nic nie wybrano" }
                return@rememberLauncherForActivityResult
            }
            val images = uris.mapNotNull { uri ->
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    bytes?.let { PickedImage(it, context.contentResolver.getType(uri) ?: "image/jpeg") }
                } catch (e: Exception) {
                    Logger.w { "Galeria: nie udało się odczytać zdjęcia: ${e.message}" }
                    null
                }
            }
            Logger.i { "Galeria: wybrano ${uris.size}, odczytano ${images.size}" }
            current.value(images)
        }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
