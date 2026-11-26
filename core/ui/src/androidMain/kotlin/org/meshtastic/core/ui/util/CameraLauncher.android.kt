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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import co.touchlab.kermit.Logger
import java.io.File

private const val PHOTO_DIR = "reports"

@Composable
actual fun rememberCameraLauncher(onPhotoTaken: (bytes: ByteArray, mimeType: String) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val current = rememberUpdatedState(onPhotoTaken)
    var target by remember { mutableStateOf<File?>(null) }
    val take =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val file = target
            target = null
            if (file == null) return@rememberLauncherForActivityResult
            try {
                if (saved && file.length() > 0) {
                    Logger.i { "Aparat: zdjęcie zrobione, ${file.length()} bajtów" }
                    current.value(file.readBytes(), "image/jpeg")
                } else {
                    Logger.i { "Aparat: zdjęcie nie zostało zrobione" }
                }
            } catch (e: Exception) {
                Logger.w { "Aparat: nie udało się odczytać zdjęcia: ${e.message}" }
            } finally {
                file.delete()
            }
        }
    val start: () -> Unit = {
        try {
            val dir = File(context.cacheDir, PHOTO_DIR).apply { mkdirs() }
            val file = File.createTempFile("photo-", ".jpg", dir)
            val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".reportshare", file)
            target = file
            take.launch(uri)
        } catch (e: Exception) {
            Logger.w { "Aparat: nie udało się otworzyć aparatu: ${e.message}" }
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Logger.i { "Aparat: zgoda na aparat ${if (granted) "udzielona" else "odmówiona"}" }
            if (granted) start()
        }
    return {
        val granted =
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        Logger.i { "Aparat: przycisk dotknięty, zgoda ${if (granted) "jest" else "brak, pytam"}" }
        if (granted) start() else permission.launch(android.Manifest.permission.CAMERA)
    }
}
