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

import androidx.compose.runtime.Composable

/** The name of the separate album, the same as the app (on the computer: the folder) where the pictures taken for hikes are kept. */
const val HIKE_ALBUM_NAME = "MT_SW"

/**
 * MT_SW: keeps a copy of a picture taken for a hike in a separate album of the phone's pictures (on the computer: a
 * folder, [folder] or a default one when null). The copy is the whole picture, and it stays when a hike is deleted in
 * the app. Returns false when it could not be saved (the reason goes to the log).
 */
expect fun saveToHikeAlbum(bytes: ByteArray, fileName: String, folder: String?): Boolean

/** Where the album folder is chosen by hand (the computer); null elsewhere, where the system keeps the album. */
@Composable expect fun rememberAlbumFolderPicker(onPicked: (String) -> Unit): (() -> Unit)?

/** The folder pictures are saved to on the computer when none is chosen; null elsewhere. */
expect fun defaultAlbumFolder(): String?
