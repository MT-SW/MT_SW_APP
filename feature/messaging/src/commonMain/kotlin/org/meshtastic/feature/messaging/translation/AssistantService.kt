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
     * The dictation part of the compatibility test: makes sure the smallest dictation model is on the device (it is
     * downloaded when there is none), runs it once and describes how it went. [onStep] gets a short note for each step.
     * Empty where the platform has no dictation.
     */
    suspend fun dictationCheck(onStep: (String) -> Unit): String = ""

    /**
     * The dictation model the compatibility test would use and whether it is on the device already; null where the
     * platform has no dictation. The test asks for consent to download and try it before it starts.
     */
    val dictationOffer: DictationOffer?
        get() = null

    /** Forgets the remembered failed model starts, so a compatibility test shows what the device really can do. */
    suspend fun clearFailedAttempts() {}

    /** Releases the models from memory, so the next answer starts one again with the current settings. */
    suspend fun unloadModels() {}

    /** Where the model runs now (for example "NPU (...)", "GPU (...)" or "CPU"); empty where not known. */
    val backendNow: String
        get() = ""

    /** The short question the compatibility test asks (in the language of the app). */
    val compatQuestion: String
        get() = "Co to jest zasięg LoRa?"

    /**
     * What the last answer was made with, "model|route", saved with the answer for diagnosis. Empty where not known.
     */
    val lastInfo: String
        get() = ""

    /**
     * Asks all test questions one after another and reports each one to [onResult] (question, answer); also prints them
     * and writes a results file where the platform can. Typed in the chat as /test (everything) or a shorter command
     * (/narzedzia, /rozmowa, /baza, /sprzet); [command] says which. Does nothing where not supported.
     */
    suspend fun selfTest(command: String, onResult: (question: String, answer: String) -> Unit) {}
}

/** The dictation model of the compatibility test: its [name], its size in MB and whether it is on the device. */
data class DictationOffer(val name: String, val sizeMb: Int, val onDevice: Boolean)
