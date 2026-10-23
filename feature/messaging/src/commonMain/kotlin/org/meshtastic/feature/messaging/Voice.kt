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

/** Reads texts aloud with the voice of the system. Nothing is started until [speak] is called. */
interface Speaker {
    /** Reads [text] now; whatever was being read stops first. */
    fun speak(text: String)

    /** Stops reading. */
    fun stop()
}

/** A speaker tied to the screen: it stops and is released when the screen goes away. */
@Composable expect fun rememberSpeaker(): Speaker

/**
 * Dictation of a text with the speech recognizer of the system. [available] is false where the platform has none; then
 * the button for it is not shown.
 */
class Dictation(val available: Boolean, val start: () -> Unit)

/** [onText] receives the recognized text once the user has finished speaking. */
@Composable expect fun rememberDictation(onText: (String) -> Unit): Dictation
