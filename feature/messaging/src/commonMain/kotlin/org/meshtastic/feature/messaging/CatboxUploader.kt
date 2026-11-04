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
 * A public image host that takes anonymous uploads. They are tried one after another (see [ChatImageUploader]), so a
 * host that pauses uploads (catbox.moe answers "412 uploads paused" when its storage is full) does not stop the sending.
 */
enum class ImageHost(val label: String, val url: String, val fileField: String, val extraFields: Map<String, String>) {
    /** The main host: files stay for good. */
    CATBOX("catbox.moe", "https://catbox.moe/user/api.php", "fileToUpload", mapOf("reqtype" to "fileupload")),

    /** The same family, but its own storage; files stay for 72 hours. */
    LITTERBOX(
        "litterbox.catbox.moe",
        "https://litterbox.catbox.moe/resources/internals/api.php",
        "fileToUpload",
        mapOf("reqtype" to "fileupload", "time" to "72h"),
    ),

    /** A separate service; files stay only for a few hours. */
    UGUU("uguu.se", "https://uguu.se/upload?output=text", "files[]", emptyMap()),
}

/** Whether [url] points to one of the image hosts, so a picture that does not load can be tried again. */
fun isOwnImageHost(url: String): Boolean =
    url.contains("catbox.moe", ignoreCase = true) || url.contains("uguu.se", ignoreCase = true)

/**
 * Uploads [bytes] to the image [host] and returns the link. [onProgress] gets the share sent so far (0..1). Throws when the
 * server cannot be reached, is too slow, or does not answer with a link (its error text is never returned as a link).
 */
expect suspend fun uploadToCatbox(
    bytes: ByteArray,
    mimeType: String,
    fileName: String = "image.jpg",
    host: ImageHost = ImageHost.CATBOX,
    onProgress: (Float) -> Unit = {},
): String
