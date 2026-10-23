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
package org.meshtastic.feature.messaging.translation

import kotlinx.coroutines.flow.StateFlow

/** Turns a recording into text, fully on the device, with the hearing of the downloaded Gemma model. */
interface DictationService {
    /** True while a model that can hear (Gemma E2B or E4B) is downloaded; the dictation button is shown only then. */
    val available: StateFlow<Boolean>

    /** [wav] is a 16 kHz, mono, 16 bit recording of at most about 30 seconds. Returns the spoken text. */
    suspend fun transcribe(wav: ByteArray): String
}
