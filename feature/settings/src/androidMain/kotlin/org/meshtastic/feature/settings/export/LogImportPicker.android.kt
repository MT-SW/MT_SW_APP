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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberLogImportPicker(onResult: (LogImportContent) -> Unit): LogImportPickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            scope.launch {
                val imported =
                    withContext(Dispatchers.IO) {
                        @Suppress("TooGenericExceptionCaught")
                        try {
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            bytes?.let { LogImportContent(content = it, fileName = uri.displayName(context)) }
                        } catch (e: Exception) {
                            Logger.e(throwable = e) { "Failed to read log import file" }
                            null
                        }
                    }
                imported?.let(onResult)
            }
        }

    return LogImportPickerLauncher {
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
        launcher.launch(intent)
    }
}

/**
 * Resolve a display file name for [this] content URI. Untrusted providers can throw or return a null display name, so
 * guard both and fall back to the URI's last path segment -- mirrors `Uri.getFileName` in `feature/map`'s layer picker.
 */
@Suppress("NestedBlockDepth")
private fun Uri.displayName(context: Context): String {
    var name = lastPathSegment ?: "sniffer_log_import"
    if (scheme == "content") {
        try {
            context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) cursor.getString(index)?.let { name = it }
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(throwable = e) { "Failed to resolve display name for content URI; using fallback" }
        }
    }
    return name
}
