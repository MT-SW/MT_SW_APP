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
@file:Suppress("TooGenericExceptionCaught")

package org.meshtastic.core.ui.util

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import co.touchlab.kermit.Logger
import org.meshtastic.core.common.ContextServices

actual fun saveToHikeAlbum(bytes: ByteArray, fileName: String, folder: String?): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Logger.w { "Album wędrówek: ten system nie pozwala zapisać do albumu bez dodatkowej zgody" }
        return false
    }
    return try {
        val resolver = ContextServices.app.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$HIKE_ALBUM_NAME")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            Logger.w { "Album wędrówek: system nie utworzył pliku" }
            false
        } else {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            Logger.i { "Album wędrówek: zapisano ${bytes.size} bajtów w albumie" }
            true
        }
    } catch (e: Exception) {
        Logger.w { "Album wędrówek: zapis nie udał się: ${e.message}" }
        false
    }
}

actual fun appPhotoFolder(): String? = null

actual fun currentPhotoFolder(): String? = null

actual fun changeAppPhotoFolder(): String? = null

actual fun defaultPhotoFolder(): String? = null

actual fun photoFolderUsage(folder: String?): PhotoFolderUsage? = null
