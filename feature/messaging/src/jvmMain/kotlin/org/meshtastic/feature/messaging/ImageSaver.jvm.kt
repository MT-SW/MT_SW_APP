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
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser

@Suppress("TooGenericExceptionCaught")
actual suspend fun downloadImageBytes(url: String): DownloadedImage? {
    val client = HttpClient(OkHttp)
    return try {
        val response: HttpResponse = client.get(url)
        DownloadedImage(bytes = response.bodyAsBytes(), mimeType = response.contentType()?.toString() ?: "image/jpeg")
    } catch (e: Exception) {
        Logger.e(throwable = e) { "Failed to download image for save: ${e.message}" }
        null
    } finally {
        client.close()
    }
}

@Composable
actual fun rememberImageSaver(): ImageSaverLauncher {
    val scope = rememberCoroutineScope()
    return ImageSaverLauncher { bytes, fileName, _ ->
        scope.launch {
            withContext(Dispatchers.IO) {
                @Suppress("TooGenericExceptionCaught")
                try {
                    val chooser =
                        JFileChooser().apply {
                            dialogTitle = "Zapisz zdjęcie"
                            selectedFile = File(fileName)
                        }
                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        chooser.selectedFile.writeBytes(bytes)
                    }
                } catch (e: Exception) {
                    Logger.e(throwable = e) { "Failed to save image on desktop" }
                }
            }
        }
    }
}
