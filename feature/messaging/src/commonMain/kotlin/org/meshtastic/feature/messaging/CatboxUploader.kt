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

/**
 * Uploads [bytes] to catbox.moe and returns the link. [onProgress] gets the share sent so far (0..1). Throws when the
 * server cannot be reached, is too slow, or does not answer with a link (its error text is never returned as a link).
 */
expect suspend fun uploadToCatbox(
    bytes: ByteArray,
    mimeType: String,
    fileName: String = "image.jpg",
    onProgress: (Float) -> Unit = {},
): String

/**
 * Makes a big photo smaller before it is uploaded (a phone photo is several MB, which on a mobile connection is slow and
 * often timed out): the long side is brought to 2048 px and the picture is stored again, with the camera's rotation
 * applied. Small pictures, animated GIFs and anything that cannot be decoded are returned as they are, and so is the
 * original when the new one would not be smaller. Returns the bytes and their MIME type.
 */
expect fun prepareImageForUpload(bytes: ByteArray, mimeType: String): Pair<ByteArray, String>
