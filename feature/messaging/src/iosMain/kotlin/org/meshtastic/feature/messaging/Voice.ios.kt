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
import androidx.compose.runtime.remember
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

@Composable
actual fun rememberSpeaker(): Speaker {
    val speaker = remember { IosSpeaker() }
    DisposableEffect(speaker) { onDispose { speaker.stop() } }
    return speaker
}

/** Reads texts aloud with the voice of the iPhone (AVSpeechSynthesizer), in the language of the system. */
private class IosSpeaker : Speaker {
    private val synthesizer = AVSpeechSynthesizer()

    override fun speak(text: String) {
        stop()
        synthesizer.speakUtterance(AVSpeechUtterance.speechUtteranceWithString(text))
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}
