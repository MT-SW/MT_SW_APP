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

/**
 * The name of the separate album, the same as the app (on the computer: the folder) where the pictures taken for hikes
 * are kept.
 */
const val HIKE_ALBUM_NAME = "MT_SW"

/**
 * MT_SW: keeps a copy of a picture taken for a hike in a separate album of the phone's pictures (on the computer: a
 * folder, [folder] or a default one when null). The copy is the whole picture, and it stays when a hike is deleted in
 * the app. Returns false when it could not be saved (the reason goes to the log).
 */
expect fun saveToHikeAlbum(bytes: ByteArray, fileName: String, folder: String?): Boolean

/**
 * The folder pictures taken in the app go to on the computer. It is chosen at the first picture (a window asks) and can
 * be changed in the settings; null elsewhere, where the system keeps the album and creates it by itself.
 */
expect fun appPhotoFolder(): String?

/** The folder chosen so far, without asking; null when none is chosen yet or where the system keeps the album. */
expect fun currentPhotoFolder(): String?

/** Opens the folder choice (the computer) and remembers the answer; null when cancelled or not offered. */
expect fun changeAppPhotoFolder(): String?

/** Saves a picture taken with the camera in the app to the app's album (on the computer: the chosen folder). */
fun saveAppPhoto(bytes: ByteArray): Boolean =
    saveToHikeAlbum(bytes, "mtsw_${org.meshtastic.core.common.util.nowMillis}.jpg", appPhotoFolder())

/** How much room the pictures take in the folder and how much free space the disk has, both in megabytes. */
data class PhotoFolderUsage(val usedMb: Long, val freeMb: Long)

/** The folder pictures go to when none is chosen yet (the computer); null elsewhere. */
expect fun defaultPhotoFolder(): String?

/** The room taken by the pictures in [folder] and the free space of its disk; null where the system keeps the album. */
expect fun photoFolderUsage(folder: String?): PhotoFolderUsage?
