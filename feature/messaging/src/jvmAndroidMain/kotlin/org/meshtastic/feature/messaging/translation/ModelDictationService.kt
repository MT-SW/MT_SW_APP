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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.AiRole
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.repository.UiPrefs

/**
 * Dictation with the model the user chose for it (a Whisper model or a Gemma model that can hear); the same on every
 * platform, no speech service of the system is needed. Without a chosen and downloaded model, dictation is off.
 */
class ModelDictationService(
    private val engine: AiTextEngine,
    private val store: AiModelStoreImpl,
    private val uiPrefs: UiPrefs,
) : DictationService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The chosen model when it is installed; otherwise the first installed dictation model (the choice is not rewritten). */
    private fun pick(states: Map<String, TranslatorModelState>, id: String): String? {
        fun usable(model: org.meshtastic.core.repository.AiModelInfo) =
            model.canDo(AiRole.DICTATION) && model.engineReady && states[model.id] == TranslatorModelState.Ready
        AiModels.byId(id)?.takeIf { usable(it) }?.let { return it.id }
        return AiModels.all.firstOrNull { usable(it) }?.id
    }

    override val available: StateFlow<Boolean> = flow()

    // One model serves both: what is dictated for the chat and what is dictated for the translator.
    override val translatorAvailable: StateFlow<Boolean> = flow()

    private fun flow(): StateFlow<Boolean> =
        combine(store.states, uiPrefs.dictationModelId, uiPrefs.dictationEnabled) { states, id, on -> on && pick(states, id) != null }
            .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun transcribe(wav: ByteArray, translator: Boolean): String {
        check(uiPrefs.dictationEnabled.value) { "Dictation is switched off" }
        val id = checkNotNull(pick(store.states.value, uiPrefs.dictationModelId.value)) { "Dictation is not available" }
        // The dictation for the chat is in the language of the app; the translator's can be in any language.
        DictationLanguage.hint = if (translator) "auto" else DictationLanguage.ofApp()
        return engine.transcribe(id, wav)
    }
}
