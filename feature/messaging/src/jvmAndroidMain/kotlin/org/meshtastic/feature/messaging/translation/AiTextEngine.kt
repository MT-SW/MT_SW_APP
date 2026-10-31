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

/**
 * What the assistant, the translator and dictation need from the engine that runs the language model. Android and
 * desktop each bring their own engine behind this one door, so the rest of the app never depends on a specific one.
 */
interface AiTextEngine {
    /** Name of the chip the loaded model runs on (for example GPU or CPU); empty while nothing is loaded. */
    val activeBackend: String

    /** What happened at the last start of the model, attempt by attempt (empty before the first start); for `/sprzet`. */
    val startReport: String get() = ""

    /** Memory (MB) the model program takes right now; null while nothing is loaded or when it cannot be read. */
    val memoryUsedMb: Int? get() = null

    /** Where the model [modelId] ran at its last start, and that start's report, even after it was released from memory; null = unknown. */
    fun lastStart(modelId: String): Pair<String, String>? = null

    /** Chip, graphics driver and memory of this device, for `/sprzet`; empty when unknown. */
    val deviceSummary: String get() = ""

    /** How many tokens (question, data and answer together) the model keeps in its window. */
    fun contextTokens(modelId: String): Int

    /**
     * Sends [messages] (role to text pairs, roles `system`, `user`, `assistant`; the last one must be a `user`
     * message) to model [modelId] and returns the reply. [onPiece] receives the text as it is produced. Cancelling the
     * coroutine stops the generation. [maxTokens] is a rough limit on the length of the answer, 0 means no limit.
     * [jsonSchema] forces the answer into that JSON shape. [thinking] lets the model think quietly before it answers.
     */
    suspend fun chat(
        modelId: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        temperature: Float,
        onPiece: (String) -> Unit = {},
        jsonSchema: String? = null,
        thinking: Boolean = false,
    ): String

    /** Turns a recording (16 kHz, mono, 16 bit WAV, about 30 seconds at most) into text, fully on the device. */
    suspend fun transcribe(modelId: String, wav: ByteArray): String

    /** Frees the model from memory right away (for example when it is deleted). */
    suspend fun unloadNow()
}
