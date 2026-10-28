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

import org.meshtastic.core.repository.AiEngine
import org.meshtastic.core.repository.AiModels

/**
 * The one engine the rest of the app sees: language models (and Gemma models that hear) go to [llama], Whisper speech
 * models to [whisper] (null where the platform has no speech program).
 */
class AiEngineRouter(
    store: AiModelStoreImpl,
    private val llama: LlamaCppEngine,
    private val whisper: WhisperCppEngine?,
) : AiTextEngine {

    init {
        // A model is never deleted while a program still has its file open.
        store.beforeModelDelete = { unloadNow() }
    }

    override val activeBackend: String get() = llama.activeBackend

    override fun contextTokens(modelId: String): Int = llama.contextTokens(modelId)

    override suspend fun chat(
        modelId: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        temperature: Float,
        onPiece: (String) -> Unit,
        jsonSchema: String?,
        thinking: Boolean,
    ): String = llama.chat(modelId, messages, maxTokens, temperature, onPiece, jsonSchema, thinking)

    override suspend fun transcribe(modelId: String, wav: ByteArray): String =
        if (AiModels.byId(modelId)?.engine == AiEngine.WHISPER_CPP) {
            checkNotNull(whisper) { "Speech models are not available on this system" }.transcribe(modelId, wav)
        } else {
            llama.transcribe(modelId, wav)
        }

    override suspend fun unloadNow() {
        llama.unloadNow()
        whisper?.unloadNow()
    }
}
