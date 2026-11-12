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
package org.meshtastic.feature.map.layers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.meshtastic.core.common.util.ioDispatcher
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.ui.util.rememberOpenFileLauncher
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

/**
 * Opens the system file picker (Files app) and hands the chosen map file back as a [PickedMapFile].
 *
 * The picker copies the file into the app's own folder, so it can be read without extra permission. Like on Android,
 * the extension is validated after selection, not in the picker.
 */
@Composable
actual fun rememberMapLayerPicker(onPick: (PickedMapFile) -> Unit): MapLayerPickerLauncher {
    val scope = rememberCoroutineScope()
    val openFile =
        rememberOpenFileLauncher { uri ->
            val url = uri?.toString()?.let { NSURL.URLWithString(it) } ?: return@rememberOpenFileLauncher
            val name = url.lastPathComponent?.takeIf { it.isNotBlank() } ?: "layer_$nowMillis"
            scope.launch {
                onPick(
                    PickedMapFile(
                        displayName = name,
                        extensionOrMime = name.substringAfterLast('.', "").ifBlank { null },
                        read = { withContext(ioDispatcher) { url.readBytesOrNull() } },
                    ),
                )
            }
        }
    return MapLayerPickerLauncher { openFile("*/*") }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSURL.readBytesOrNull(): ByteArray? {
    val data = NSData.dataWithContentsOfURL(this)
    if (data == null) {
        Logger.withTag("MapLayer").e { "Failed to read the picked map layer" }
        return null
    }
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    }
    return bytes
}
