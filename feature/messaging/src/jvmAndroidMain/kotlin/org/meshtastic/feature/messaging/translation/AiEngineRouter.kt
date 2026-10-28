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

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.meshtastic.core.repository.AiEngine
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.UiPrefs

/**
 * The one engine the rest of the app sees: language models (and Gemma models that hear) go to [llama], Whisper speech
 * models to [whisper] (null where the platform has no speech program).
 */
class AiEngineRouter(
    store: AiModelStoreImpl,
    private val llama: LlamaCppEngine,
    private val whisper: WhisperCppEngine?,
    uiPrefs: UiPrefs? = null,
) : AiTextEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // A model is never deleted while a program still has its file open.
        store.beforeModelDelete = { unloadNow() }
        store.unloadAll = { unloadNow() }
        // A model stays in memory while it is used. When another model is chosen, the old one is stopped at once and the
        // new one is loaded straight away (so the first question does not wait); a function switched off frees its
        // model. A newer change cancels a loading that is still going on.
        if (uiPrefs != null) {
            scope.launch {
                var previous: Choice? = null
                combine(
                    uiPrefs.assistantEnabled,
                    uiPrefs.translatorEnabled,
                    uiPrefs.assistantModelId,
                    uiPrefs.translatorModelId,
                    uiPrefs.dictationModelId,
                ) { a, t, am, tm, dm -> Choice(a, t, am, tm, dm) }
                    .distinctUntilChanged()
                    .collectLatest { now ->
                        val before = previous
                        previous = now
                        if (before == null) return@collectLatest
                        Logger.i(tag = TAG) { "AI settings changed, switching models" }
                        unloadNow()
                        val load =
                            when {
                                now.assistantOn && (now.assistant != before.assistant || !before.assistantOn) -> now.assistant to false
                                now.translatorOn && (now.translator != before.translator || !before.translatorOn) -> now.translator to false
                                now.dictation != before.dictation -> now.dictation to true
                                else -> null
                            }
                        load?.let { (id, hearing) -> preload(id, hearing) }
                    }
            }
        }
    }

    override val activeBackend: String get() = llama.activeBackend

    override val startReport: String get() = llama.startReport

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

    /** Loads the model just chosen for a function; models of other engines (the NLLB translator) load on first use. */
    private suspend fun preload(modelId: String, hearing: Boolean) {
        when (AiModels.byId(modelId)?.engine) {
            AiEngine.LLAMA_CPP -> llama.preload(modelId, hearing = hearing && AiModels.byId(modelId)?.hearing == true)
            AiEngine.WHISPER_CPP -> whisper?.preload(modelId)
            else -> Unit
        }
    }

    private data class Choice(
        val assistantOn: Boolean,
        val translatorOn: Boolean,
        val assistant: String,
        val translator: String,
        val dictation: String,
    )

    private companion object {
        const val TAG = "AiEngineRouter"
    }
}
