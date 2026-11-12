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
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.FolderService
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create

/**
 * The chosen folder is kept as a bookmark (`bookmark:` and its bytes as hex), because iOS lets an app reach a folder the
 * user picked only through it. The bookmark is opened for the time of the write.
 */
@Single
class IosFolderService(private val dispatchers: CoroutineDispatchers) : FolderService {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun writeFile(folder: String, name: String, block: suspend (BufferedSink) -> Unit): Boolean =
        withContext(dispatchers.io) {
            var folderUrl: NSURL? = null
            var opened = false
            try {
                val bytes = folder.removePrefix(BOOKMARK_PREFIX).decodeHex().toByteArray()
                val data = bytes.toNSData()
                folderUrl =
                    memScoped {
                        val stale = alloc<BooleanVar>()
                        NSURL.URLByResolvingBookmarkData(data, 0uL, null, stale.ptr, null)
                    }
                val url = folderUrl ?: return@withContext false
                opened = url.startAccessingSecurityScopedResource()
                val directory = (url.path ?: return@withContext false).toPath()
                val sink = FileSystem.SYSTEM.sink(directory / name).buffer()
                try {
                    block(sink)
                } finally {
                    sink.close()
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(e) { "Failed to write $name to the chosen folder" }
                false
            } finally {
                if (opened) folderUrl?.stopAccessingSecurityScopedResource()
            }
        }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData =
        if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

    private companion object {
        const val BOOKMARK_PREFIX = "bookmark:"
    }
}
