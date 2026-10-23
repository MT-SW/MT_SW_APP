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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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

@Composable
actual fun rememberDictation(onText: (String) -> Unit): Dictation {
    val current = rememberUpdatedState(onText)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                if (!text.isNullOrBlank()) current.value(text)
            }
        }
    return remember(launcher) {
        Dictation(available = true) {
            val intent =
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            try {
                launcher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                Logger.w(tag = "Dictation") { "No speech recognizer is installed" }
            }
        }
    }
}
