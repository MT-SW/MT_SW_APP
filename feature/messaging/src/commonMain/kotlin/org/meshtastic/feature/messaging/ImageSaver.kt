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

/** Raw bytes fetched from a chat image's link, plus the server's reported content type -- see [downloadImageBytes]. */
class DownloadedImage(val bytes: ByteArray, val mimeType: String)

/**
 * Re-downloads the full-resolution image at [url] (the same link the chat bubble/full-screen preview loads via Coil) so
 * it can be handed to the platform's own "Save As" flow. Returns null on any network/HTTP failure.
 */
expect suspend fun downloadImageBytes(url: String): DownloadedImage?

/** Platform-agnostic handle for triggering a save-to-file of already-downloaded image bytes. */
fun interface ImageSaverLauncher {
    fun save(bytes: ByteArray, fileName: String, mimeType: String)
}

/**
 * Returns a launcher that saves image bytes to the platform's file system.
 *
 * On Android this opens a SAF document-picker (ACTION_CREATE_DOCUMENT). On Desktop this writes to a user-chosen file
 * via a file dialog. Mirrors `feature/settings`'s `LogExportSaver` -- kept as its own small copy here rather than a
 * cross-feature dependency, matching this module's existing self-contained expect/actual pattern (see
 * [uploadToCatbox]).
 */
@Composable expect fun rememberImageSaver(): ImageSaverLauncher

/** Best-effort file extension for a saved image, from the download's `Content-Type`. Falls back to `jpg`. */
fun String.toImageFileExtension(): String = when {
    contains("png") -> "png"
    contains("gif") -> "gif"
    contains("webp") -> "webp"
    contains("bmp") -> "bmp"
    else -> "jpg"
}
