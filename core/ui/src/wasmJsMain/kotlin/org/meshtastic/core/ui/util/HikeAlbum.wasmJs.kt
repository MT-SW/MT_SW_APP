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
package org.meshtastic.core.ui.util

actual fun saveToHikeAlbum(bytes: ByteArray, fileName: String, folder: String?): Boolean = false

actual fun appPhotoFolder(): String? = null

actual fun currentPhotoFolder(): String? = null

actual fun changeAppPhotoFolder(): String? = null

actual fun defaultPhotoFolder(): String? = null

actual fun photoFolderUsage(folder: String?): PhotoFolderUsage? = null
