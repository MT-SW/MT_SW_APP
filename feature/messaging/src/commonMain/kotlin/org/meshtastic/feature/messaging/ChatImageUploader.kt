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

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the sending of a chat image stands, for the bar above the message field. */
sealed interface ImageUploadState {
    data object Idle : ImageUploadState

    /** [percent] of the picture is sent; [attempt] counts from 1 (it is tried again after a failure). */
    data class Uploading(val percent: Int, val attempt: Int) : ImageUploadState

    data class Failed(val reason: String) : ImageUploadState
}

/**
 * Sends a chat picture to the image host and then the link as a message. It lives apart from the screen: a screen that
 * is left (another conversation, a turned phone) used to cancel the sending halfway. A failure is tried again on the
 * next image host (catbox.moe, then litterbox.catbox.moe, then uguu.se) before it is shown, and the progress is a flow
 * the message field reads.
 */
object ChatImageUploader {
    private const val RETRY_DELAY_MS = 2_000L
    private const val PERCENT = 100

    private val hosts = ImageHost.entries
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
    val state: StateFlow<ImageUploadState> = _state.asStateFlow()

    private var job: Job? = null
    private var last: Pending? = null

    private class Pending(val bytes: ByteArray, val mimeType: String, val onUploaded: (String) -> Unit)

    /**
     * Starts sending; [onUploaded] gets the link on the main thread when it is done (the caller sends it as a message).
     */
    fun start(bytes: ByteArray, mimeType: String, onUploaded: (String) -> Unit) {
        run(Pending(bytes, mimeType, onUploaded))
    }

    /** Tries the last picture again after a failure. */
    fun retry() {
        last?.let(::run)
    }

    /** Stops the sending and clears the bar. */
    fun cancel() {
        job?.cancel()
        last = null
        _state.value = ImageUploadState.Idle
    }

    /** Clears a failure from the bar. */
    fun dismiss() {
        if (_state.value is ImageUploadState.Failed) {
            last = null
            _state.value = ImageUploadState.Idle
        }
    }

    private fun run(pending: Pending) {
        job?.cancel()
        last = pending
        job = scope.launch {
            try {
                val url = uploadWithRetries(pending.bytes, pending.mimeType)
                _state.value = ImageUploadState.Idle
                last = null
                withContext(Dispatchers.Main) { pending.onUploaded(url) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(throwable = e) { "Image upload failed: ${e.message}" }
                _state.value = ImageUploadState.Failed(e.message ?: e::class.simpleName.orEmpty())
            }
        }
    }

    private suspend fun uploadWithRetries(bytes: ByteArray, mime: String): String {
        var failure: Exception? = null
        for (attempt in 1..hosts.size) {
            _state.value = ImageUploadState.Uploading(0, attempt)
            try {
                return uploadToCatbox(bytes, mime, host = hosts[attempt - 1]) { share ->
                    _state.value = ImageUploadState.Uploading((share * PERCENT).toInt().coerceIn(0, PERCENT), attempt)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
                Logger.w(throwable = e) { "Image upload attempt $attempt of ${hosts.size} failed: ${e.message}" }
                if (attempt < hosts.size) delay(RETRY_DELAY_MS * attempt)
            }
        }
        throw failure ?: IllegalStateException("Upload failed")
    }
}
