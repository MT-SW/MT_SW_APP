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

import co.touchlab.kermit.Logger
import org.meshtastic.core.common.AppDataRoot
import java.io.File
import javax.swing.JFileChooser

private val folderFile: File
    get() = File(AppDataRoot.dir, "photo-folder.txt")

/** Where pictures go when no folder is chosen (a window was closed without a choice). */
private val fallbackFolder: File
    get() = File(File(System.getProperty("user.home"), "Pictures"), HIKE_ALBUM_NAME)

actual fun currentPhotoFolder(): String? =
    try {
        if (folderFile.exists()) folderFile.readText().trim().ifBlank { null } else null
    } catch (e: Exception) {
        Logger.w { "Album zdjęć: folder nie został odczytany: ${e.message}" }
        null
    }

actual fun changeAppPhotoFolder(): String? {
    val chooser =
        JFileChooser(File(System.getProperty("user.home"), "Pictures").takeIf { it.isDirectory }).apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = HIKE_ALBUM_NAME
        }
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
        Logger.i { "Album zdjęć: wybór folderu anulowany" }
        return null
    }
    val path = chooser.selectedFile.path
    try {
        folderFile.parentFile?.mkdirs()
        folderFile.writeText(path)
        Logger.i { "Album zdjęć: wybrano folder" }
    } catch (e: Exception) {
        Logger.w { "Album zdjęć: folder nie został zapisany: ${e.message}" }
    }
    return path
}

/** The first picture asks for the folder; a closed window means the default one beside the pictures of the user. */
actual fun appPhotoFolder(): String? = currentPhotoFolder() ?: changeAppPhotoFolder() ?: fallbackFolder.path

actual fun saveToHikeAlbum(bytes: ByteArray, fileName: String, folder: String?): Boolean =
    try {
        val dir = File(folder?.takeIf { it.isNotBlank() } ?: fallbackFolder.path)
        dir.mkdirs()
        File(dir, fileName).writeBytes(bytes)
        Logger.i { "Album zdjęć: zapisano ${bytes.size} bajtów w folderze" }
        true
    } catch (e: Exception) {
        Logger.w { "Album zdjęć: zapis w folderze nie udał się: ${e.message}" }
        false
    }

actual fun defaultPhotoFolder(): String? = fallbackFolder.path

actual fun photoFolderUsage(folder: String?): PhotoFolderUsage? =
    try {
        val dir = File(folder?.takeIf { it.isNotBlank() } ?: fallbackFolder.path)
        val used = if (dir.isDirectory) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
        val free = (if (dir.exists()) dir else dir.parentFile ?: dir).usableSpace
        PhotoFolderUsage(usedMb = used / BYTES_IN_MB, freeMb = free / BYTES_IN_MB)
    } catch (e: Exception) {
        Logger.w { "Album zdjęć: zajęte miejsce nie zostało policzone: ${e.message}" }
        null
    }

private const val BYTES_IN_MB = 1024L * 1024L
