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

/** One line of the conversation sent to the assistant. */
data class AssistantTurn(val fromUser: Boolean, val text: String)

/** The offline assistant: answers questions with the language model chosen in the settings. */
interface AssistantService {
    /** True when a model that can act as the assistant is on the device. */
    val ready: StateFlow<Boolean>

    /**
     * Answers the last user turn in [history]. [onPiece] receives the text as it is produced. Throws on failure;
     * cancelling the coroutine stops the answer.
     */
    suspend fun reply(history: List<AssistantTurn>, onPiece: (String) -> Unit): String

    /**
     * Asks all test questions one after another and reports each one to [onResult] (question, answer); also prints them
     * and writes a results file where the platform can. Typed in the chat as /test (everything) or a shorter command (/narzedzia, /rozmowa, /baza, /sprzet); [command] says which. Does nothing where not supported.
     */
    suspend fun selfTest(command: String, onResult: (question: String, answer: String) -> Unit) {}
}
