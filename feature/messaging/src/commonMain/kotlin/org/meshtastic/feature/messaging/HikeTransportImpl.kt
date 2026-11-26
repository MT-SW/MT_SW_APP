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

package org.meshtastic.feature.messaging

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.koin.core.annotation.Single
import org.meshtastic.core.ui.util.HikeTransport

/** MT_SW: the hosts of the chat pictures, used for hikes: each is tried in turn (see [ChatImageUploader]). */
@Single(binds = [HikeTransport::class])
class HikeTransportImpl : HikeTransport {
    override suspend fun upload(
        bytes: ByteArray,
        mimeType: String,
        fileName: String,
        onProgress: (Float) -> Unit,
    ): String {
        var failure: Exception? = null
        for ((i, host) in ImageHost.entries.withIndex()) {
            try {
                val url = uploadToCatbox(bytes, mimeType, fileName, host, onProgress)
                Logger.i { "Wędrówki: plik wysłany na ${host.label}, ${bytes.size} bajtów" }
                return url
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
                Logger.w { "Wędrówki: serwer ${host.label} odmówił: ${e.message}" }
                if (i < ImageHost.entries.lastIndex) delay(RETRY_DELAY_MS)
            }
        }
        throw failure ?: error("Upload failed")
    }

    override suspend fun download(url: String, maxBytes: Long): ByteArray = downloadBytes(url, maxBytes)

    private companion object {
        const val RETRY_DELAY_MS = 1_500L
    }
}

/** Downloads [url] (at most [maxBytes]); throws on failure or when the file is bigger. */
expect suspend fun downloadBytes(url: String, maxBytes: Long): ByteArray
