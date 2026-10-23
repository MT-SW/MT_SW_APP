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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import java.util.Locale

/** Reads with the Android text-to-speech engine; the engine is only created when something is read for the first time. */
private class AndroidSpeaker(private val context: Context) : Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    override fun speak(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val engine = tts
        if (engine == null) {
            pending = clean
            tts =
                TextToSpeech(context) { status ->
                    ready = status == TextToSpeech.SUCCESS
                    if (ready) {
                        runCatching { tts?.language = Locale.getDefault() }
                        pending?.let { say(it) }
                    } else {
                        Logger.w(tag = TAG) { "Text-to-speech could not start (status $status)" }
                    }
                    pending = null
                }
        } else if (ready) {
            say(clean)
        } else {
            pending = clean
        }
    }

    private fun say(text: String) {
        val engine = tts ?: return
        engine.stop()
        // The engine refuses very long texts, so they are read in pieces.
        val limit = (TextToSpeech.getMaxSpeechInputLength() - 100).coerceAtLeast(500)
        var first = true
        text.chunked(limit).forEach { piece ->
            engine.speak(piece, if (first) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "mtsw-$piece".take(32))
            first = false
        }
    }

    override fun stop() {
        pending = null
        runCatching { tts?.stop() }
    }

    fun release() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        ready = false
    }

    private companion object {
        const val TAG = "AndroidSpeaker"
    }
}

@Composable
actual fun rememberSpeaker(): Speaker {
    val context = LocalContext.current.applicationContext
    val speaker = remember { AndroidSpeaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.release() } }
    return speaker
}

/** Records the microphone with [AudioRecord] as 16 kHz mono PCM; the recording is capped at 30 seconds. */
private class AndroidMic(private val context: Context, private val ask: suspend () -> Boolean) {
    private var record: AudioRecord? = null
    private var thread: Thread? = null
    private val buffer = java.io.ByteArrayOutputStream()

    @Volatile private var running = false

    suspend fun permission(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return ask()
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun start() {
        val minimum = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder =
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minimum, RATE),
            )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Microphone is not available" }
        buffer.reset()
        record = recorder
        running = true
        recorder.startRecording()
        thread =
            Thread {
                    val chunk = ByteArray(4096)
                    while (running && buffer.size() < RATE * 2 * 30) {
                        val read = recorder.read(chunk, 0, chunk.size)
                        if (read > 0) synchronized(buffer) { buffer.write(chunk, 0, read) }
                    }
                }
                .apply {
                    isDaemon = true
                    start()
                }
    }

    fun stop(): ByteArray {
        running = false
        runCatching { thread?.join(1000) }
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null
        thread = null
        return synchronized(buffer) { buffer.toByteArray() }
    }

    private companion object {
        const val RATE = 16_000
    }
}

@Composable
actual fun rememberMicRecorder(): MicRecorder {
    val context = LocalContext.current.applicationContext
    val waiting = remember { arrayOfNulls<CompletableDeferred<Boolean>>(1) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            waiting[0]?.complete(granted)
            waiting[0] = null
        }
    val mic =
        remember(launcher) {
            AndroidMic(context) {
                val answer = CompletableDeferred<Boolean>()
                waiting[0] = answer
                launcher.launch(Manifest.permission.RECORD_AUDIO)
                answer.await()
            }
        }
    return remember(mic) { MicRecorder(permission = mic::permission, start = mic::start, stop = mic::stop) }
}
