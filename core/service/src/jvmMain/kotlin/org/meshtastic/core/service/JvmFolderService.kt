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
package org.meshtastic.core.service

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.buffer
import okio.sink
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.FolderService
import java.io.File
import java.net.URI

/** The chosen folder is a directory (a `file:` address or a plain path); the file is written inside it. */
@Single
class JvmFolderService(private val dispatchers: CoroutineDispatchers) : FolderService {
    override suspend fun writeFile(folder: String, name: String, block: suspend (BufferedSink) -> Unit): Boolean =
        withContext(dispatchers.io) {
            try {
                val directory = if (folder.startsWith("file:")) File(URI(folder)) else File(folder)
                directory.mkdirs()
                File(directory, name).sink().buffer().use { sink -> block(sink) }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Failed to write $name to the folder" }
                false
            }
        }
}
