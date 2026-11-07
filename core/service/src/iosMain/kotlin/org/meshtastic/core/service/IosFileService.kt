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
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.FileService
import platform.Foundation.NSURL

/** Reads and writes the files the iPhone pickers hand back (`file:` addresses) or plain paths. */
@Single
class IosFileService(private val dispatchers: CoroutineDispatchers) : FileService {
    override suspend fun write(uri: CommonUri, block: suspend (BufferedSink) -> Unit): Boolean =
        withContext(dispatchers.io) {
            val scope = uri.openSecurityScope()
            try {
                val path = uri.toFilePath().toPath()
                path.parent?.takeIf { !FileSystem.SYSTEM.exists(it) }?.let { FileSystem.SYSTEM.createDirectories(it) }
                val sink = FileSystem.SYSTEM.sink(path).buffer()
                try {
                    block(sink)
                } finally {
                    sink.close()
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(e) { "Failed to write to URI: $uri" }
                false
            } finally {
                scope()
            }
        }

    override suspend fun read(uri: CommonUri, block: suspend (BufferedSource) -> Unit): Boolean =
        withContext(dispatchers.io) {
            val scope = uri.openSecurityScope()
            try {
                val source = FileSystem.SYSTEM.source(uri.toFilePath().toPath()).buffer()
                try {
                    block(source)
                } finally {
                    source.close()
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(e) { "Failed to read from URI: $uri" }
                false
            } finally {
                scope()
            }
        }

    /**
     * A place chosen in the Files picker lies outside the app's own folder, and iOS lets the app touch it only between
     * these two calls. Returns the call that ends the access.
     */
    private fun CommonUri.openSecurityScope(): () -> Unit {
        val url = toString().takeIf { it.startsWith("file:") }?.let { NSURL.URLWithString(it) } ?: return {}
        val opened = url.startAccessingSecurityScopedResource()
        return { if (opened) url.stopAccessingSecurityScopedResource() }
    }

    private fun CommonUri.toFilePath(): String {
        val text = toString()
        return if (text.startsWith("file:")) NSURL.URLWithString(text)?.path ?: text.removePrefix("file://") else text
    }
}
