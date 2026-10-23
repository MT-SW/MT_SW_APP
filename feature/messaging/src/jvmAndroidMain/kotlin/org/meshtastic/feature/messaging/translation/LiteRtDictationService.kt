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
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.repository.UiPrefs

/** Dictation through [LiteRtEngine]; the same on every platform, no speech service of the system is needed. */
class LiteRtDictationService(
    private val engine: LiteRtEngine,
    private val store: AiModelStoreImpl,
    private val uiPrefs: UiPrefs,
) : DictationService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Chats and the assistant use the model chosen for the assistant (when it is on); the translator its own. */
    private fun pick(states: Map<String, TranslatorModelState>, translator: Boolean): String? {
        val id = if (translator) uiPrefs.translatorModelId.value else uiPrefs.assistantModelId.value
        val on = if (translator) uiPrefs.translatorEnabled.value else uiPrefs.assistantEnabled.value
        return id.takeIf { on && ready(states, it) }
    }

    override val available: StateFlow<Boolean> = flowFor(translator = false)

    override val translatorAvailable: StateFlow<Boolean> = flowFor(translator = true)

    private fun flowFor(translator: Boolean): StateFlow<Boolean> =
        combine(
            store.states,
            uiPrefs.assistantEnabled,
            uiPrefs.assistantModelId,
            uiPrefs.translatorEnabled,
            uiPrefs.translatorModelId,
        ) { states, _, _, _, _ ->
            pick(states, translator) != null
        }
            .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun transcribe(wav: ByteArray, translator: Boolean): String {
        val id = checkNotNull(pick(store.states.value, translator)) { "Dictation is not available" }
        return engine.transcribe(id, wav)
    }

    private fun ready(states: Map<String, TranslatorModelState>, id: String) =
        id in HEARING && states[id] == TranslatorModelState.Ready

    private companion object {
        val HEARING = listOf(AiModels.GEMMA_E2B_ID, AiModels.GEMMA_E4B_ID)
    }
}
