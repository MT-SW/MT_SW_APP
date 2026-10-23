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

/**
 * Routes recording to a connected headset's microphone. A wired or USB headset is simply preferred; a Bluetooth headset
 * needs its voice link (SCO) switched on for the time of the recording, and switched off again afterwards.
 */
private class HeadsetMic(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    private var routed = false
    private var previousMode = android.media.AudioManager.MODE_NORMAL

    /** The headset microphone, ready to record from, or null when no headset is connected. */
    fun begin(): android.media.AudioDeviceInfo? {
        val inputs = audio.getDevices(android.media.AudioManager.GET_DEVICES_INPUTS)
        inputs.firstOrNull { it.type in WIRED }?.let { return it }
        val bluetooth = inputs.firstOrNull { isBluetooth(it.type) } ?: return null
        previousMode = audio.mode
        audio.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val target = audio.availableCommunicationDevices.firstOrNull { isBluetooth(it.type) }
            routed = target != null && audio.setCommunicationDevice(target)
        } else {
            @Suppress("DEPRECATION")
            audio.startBluetoothSco()
            @Suppress("DEPRECATION")
            audio.isBluetoothScoOn = true
            routed = true
            // The voice link takes a moment to come up.
            val until = android.os.SystemClock.elapsedRealtime() + SCO_WAIT_MS
            @Suppress("DEPRECATION")
            while (!audio.isBluetoothScoOn && android.os.SystemClock.elapsedRealtime() < until) Thread.sleep(POLL_MS)
        }
        if (!routed) {
            audio.mode = previousMode
            return null
        }
        Thread.sleep(SETTLE_MS)
        return bluetooth
    }

    fun end() {
        if (!routed) return
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                audio.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audio.isBluetoothScoOn = false
                @Suppress("DEPRECATION")
                audio.stopBluetoothSco()
            }
            audio.mode = previousMode
        }
        routed = false
    }

    private fun isBluetooth(type: Int) =
        type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET)

    private companion object {
        val WIRED =
            setOf(
                android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET,
                android.media.AudioDeviceInfo.TYPE_USB_HEADSET,
                android.media.AudioDeviceInfo.TYPE_USB_DEVICE,
            )
        const val SCO_WAIT_MS = 2000L
        const val POLL_MS = 50L
        const val SETTLE_MS = 300L
    }
}

/** Records the microphone with [AudioRecord] as 16 kHz mono PCM; the recording is capped at 30 seconds. */
private class AndroidMic(
    private val context: Context,
    private val useHeadset: () -> Boolean,
    private val ask: suspend () -> Boolean,
) {
    private val headset = HeadsetMic(context)
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
        // With the setting on and a headset connected, its microphone is used (a Bluetooth one through the call route).
        val device = if (useHeadset()) runCatching { headset.begin() }.getOrNull() else null
        val bluetooth = device != null && device.type !in setOf(
            android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET,
            android.media.AudioDeviceInfo.TYPE_USB_HEADSET,
            android.media.AudioDeviceInfo.TYPE_USB_DEVICE,
        )
        val recorder =
            AudioRecord(
                if (bluetooth) MediaRecorder.AudioSource.VOICE_COMMUNICATION else MediaRecorder.AudioSource.MIC,
                RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minimum, RATE),
            )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            headset.end()
            error("Microphone is not available")
        }
        device?.let { runCatching { recorder.setPreferredDevice(it) } }
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
        headset.end()
        return synchronized(buffer) { buffer.toByteArray() }
    }

    private companion object {
        const val RATE = 16_000
    }
}

@Composable
actual fun rememberMicRecorder(): MicRecorder {
    val context = LocalContext.current.applicationContext
    val prefs = org.koin.compose.koinInject<org.meshtastic.core.repository.UiPrefs>()
    val waiting = remember { arrayOfNulls<CompletableDeferred<Boolean>>(1) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            waiting[0]?.complete(granted)
            waiting[0] = null
        }
    val mic =
        remember(launcher) {
            AndroidMic(context, useHeadset = { prefs.dictationExternalMic.value }) {
                val answer = CompletableDeferred<Boolean>()
                waiting[0] = answer
                launcher.launch(Manifest.permission.RECORD_AUDIO)
                answer.await()
            }
        }
    return remember(mic) { MicRecorder(permission = mic::permission, start = mic::start, stop = mic::stop) }
}
