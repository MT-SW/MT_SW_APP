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

    private fun pick(states: Map<String, TranslatorModelState>, id: String): String? {
        val model = AiModels.byId(id) ?: return null
        return id.takeIf { model.canDo(AiRole.DICTATION) && model.engineReady && states[id] == TranslatorModelState.Ready }
    }

    override val available: StateFlow<Boolean> = flow()

    // One model serves both: what is dictated for the chat and what is dictated for the translator.
    override val translatorAvailable: StateFlow<Boolean> = flow()

    private fun flow(): StateFlow<Boolean> =
        combine(store.states, uiPrefs.dictationModelId) { states, id -> pick(states, id) != null }
            .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun transcribe(wav: ByteArray, translator: Boolean): String {
        val id = checkNotNull(pick(store.states.value, uiPrefs.dictationModelId.value)) { "Dictation is not available" }
        return engine.transcribe(id, wav)
    }
}
