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
import kotlinx.coroutines.flow.map
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

    override val available: StateFlow<Boolean> =
        store.states.map { pick(it) != null }.stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun transcribe(wav: ByteArray): String {
        val id = pick(store.states.value) ?: error("No model that can hear is downloaded")
        return engine.transcribe(id, wav)
    }

    /** The model chosen for the assistant (or translator) if it can hear, otherwise any downloaded one that can. */
    private fun pick(states: Map<String, TranslatorModelState>): String? {
        fun ok(id: String) = id in HEARING && states[id] == TranslatorModelState.Ready
        return listOf(uiPrefs.assistantModelId.value, uiPrefs.translatorModelId.value).firstOrNull { ok(it) }
            ?: HEARING.firstOrNull { ok(it) }
    }

    private companion object {
        val HEARING = listOf(AiModels.GEMMA_E2B_ID, AiModels.GEMMA_E4B_ID)
    }
}
