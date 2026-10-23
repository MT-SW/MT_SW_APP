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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.meshtastic.feature.messaging.translation.DictationService

/** What the dictation is doing right now. */
enum class DictationPhase {
    IDLE,
    LISTENING,
    WORKING,
}

/**
 * Dictation of a text: the microphone records, then the downloaded Gemma model turns the speech into text, all on the
 * device and the same on every platform. [available] is false until such a model is downloaded; then the button for it
 * is not shown. [toggle] starts the recording, and a second press ends it and starts the conversion into text.
 */
class Dictation(val available: Boolean, val phase: DictationPhase, val toggle: () -> Unit)

/** The microphone of the platform: PCM 16 bit, 16 kHz, mono. [permission] asks the user when needed. */
class MicRecorder(val permission: suspend () -> Boolean, val start: () -> Unit, val stop: () -> ByteArray)

/** The recorder of this platform, released with the screen. */
@Composable expect fun rememberMicRecorder(): MicRecorder

/** [onText] receives the recognized text once the user has finished speaking. */
@Composable
fun rememberDictation(translator: Boolean = false, onText: (String) -> Unit): Dictation {
    val service = koinInject<DictationService>()
    val available by (if (translator) service.translatorAvailable else service.available).collectAsState()
    val mic = rememberMicRecorder()
    val scope = rememberCoroutineScope()
    val current = rememberUpdatedState(onText)
    var phase by remember { mutableStateOf(DictationPhase.IDLE) }
    var limit by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(mic) { onDispose { if (phase == DictationPhase.LISTENING) runCatching { mic.stop() } } }

    fun finish() {
        limit?.cancel()
        scope.launch {
            val pcm = withContext(Dispatchers.Default) { runCatching { mic.stop() }.getOrDefault(ByteArray(0)) }
            if (pcm.size < MIN_BYTES) {
                phase = DictationPhase.IDLE
                return@launch
            }
            phase = DictationPhase.WORKING
            val text =
                try {
                    service.transcribe(wavOf(pcm), translator)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.w(tag = "Dictation", throwable = e) { "Dictation failed: ${e.message}" }
                    ""
                }
            phase = DictationPhase.IDLE
            if (text.isNotBlank()) current.value(text)
        }
    }

    return Dictation(available = available, phase = phase) {
        when (phase) {
            DictationPhase.IDLE ->
                scope.launch {
                    if (!mic.permission()) return@launch
                    try {
                        mic.start()
                    } catch (e: Exception) {
                        Logger.w(tag = "Dictation", throwable = e) { "Microphone could not start: ${e.message}" }
                        return@launch
                    }
                    phase = DictationPhase.LISTENING
                    // The model hears about 30 seconds at a time, so the recording ends by itself then.
                    limit = launch {
                        delay(MAX_MS)
                        if (phase == DictationPhase.LISTENING) finish()
                    }
                }
            DictationPhase.LISTENING -> finish()
            DictationPhase.WORKING -> Unit
        }
    }
}

private const val MAX_MS = 29_000L
private const val MIN_BYTES = 16_000 // half a second of 16 kHz, 16 bit audio

/** Wraps raw 16 kHz, mono, 16 bit samples in a WAV header. */
internal fun wavOf(pcm: ByteArray, sampleRate: Int = 16_000): ByteArray {
    val out = ByteArray(44 + pcm.size)
    fun int(at: Int, value: Int) {
        for (i in 0 until 4) out[at + i] = (value shr (8 * i)).toByte()
    }
    fun short(at: Int, value: Int) {
        out[at] = value.toByte()
        out[at + 1] = (value shr 8).toByte()
    }
    "RIFF".forEachIndexed { i, c -> out[i] = c.code.toByte() }
    int(4, 36 + pcm.size)
    "WAVEfmt ".forEachIndexed { i, c -> out[8 + i] = c.code.toByte() }
    int(16, 16)
    short(20, 1) // PCM
    short(22, 1) // mono
    int(24, sampleRate)
    int(28, sampleRate * 2)
    short(32, 2)
    short(34, 16)
    "data".forEachIndexed { i, c -> out[36 + i] = c.code.toByte() }
    int(40, pcm.size)
    pcm.copyInto(out, 44)
    return out
}
