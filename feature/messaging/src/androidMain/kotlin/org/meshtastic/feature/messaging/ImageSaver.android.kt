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

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
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

private class PendingImageSave(val bytes: ByteArray, val fileName: String, val mimeType: String)

@Composable
actual fun rememberImageSaver(): ImageSaverLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingSave = remember { mutableStateOf<PendingImageSave?>(null) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            val save = pendingSave.value ?: return@rememberLauncherForActivityResult
            pendingSave.value = null
            scope.launch {
                withContext(Dispatchers.IO) {
                    @Suppress("TooGenericExceptionCaught")
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(save.bytes) }
                    } catch (e: Exception) {
                        Logger.e(throwable = e) { "Failed to write saved image" }
                    }
                }
            }
        }

    return ImageSaverLauncher { bytes, fileName, mimeType ->
        pendingSave.value = PendingImageSave(bytes, fileName, mimeType)
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
        launcher.launch(intent)
    }
}
