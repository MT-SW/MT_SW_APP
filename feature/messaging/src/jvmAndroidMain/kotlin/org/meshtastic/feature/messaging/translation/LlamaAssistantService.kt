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

/** The assistant on top of [LlamaEngine]; it shares the loaded model with the translator. */
class LlamaAssistantService(
    private val engine: LlamaEngine,
    private val store: AiModelStoreImpl,
    private val uiPrefs: UiPrefs,
) : AssistantService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val ready: StateFlow<Boolean> =
        combine(store.states, uiPrefs.assistantModelId) { states, selected -> activeId(states, selected) != null }
            .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun reply(history: List<AssistantTurn>, onPiece: (String) -> Unit): String {
        val modelId = activeId(store.states.value, uiPrefs.assistantModelId.value) ?: error("No assistant model")
        val messages = buildList {
            add("system" to SYSTEM_PROMPT)
            trimmed(history).forEach { add((if (it.fromUser) "user" else "assistant") to it.text) }
        }
        return engine.chat(
            modelId = modelId,
            messages = messages,
            maxTokens = MAX_ANSWER_TOKENS,
            temperature = TEMPERATURE,
            onPiece = onPiece,
        )
    }

    /** Keeps the newest turns that fit the model's memory of the conversation (the oldest are dropped). */
    private fun trimmed(history: List<AssistantTurn>): List<AssistantTurn> {
        var budget = MAX_HISTORY_CHARS
        val kept = ArrayList<AssistantTurn>()
        for (turn in history.asReversed()) {
            if (turn.text.isBlank() && !turn.fromUser) continue
            budget -= turn.text.length
            if (budget < 0 && kept.isNotEmpty()) break
            kept.add(turn)
        }
        return kept.asReversed()
    }

    private fun activeId(states: Map<String, TranslatorModelState>, selected: String): String? {
        val chosen = AiModels.byId(selected)?.takeIf { it.canDo(AiRole.ASSISTANT) && it.engineReady }
        if (chosen != null && states[chosen.id] == TranslatorModelState.Ready) return chosen.id
        return AiModels.all
            .firstOrNull { it.canDo(AiRole.ASSISTANT) && it.engineReady && states[it.id] == TranslatorModelState.Ready }
            ?.id
    }

    private companion object {
        const val MAX_ANSWER_TOKENS = 700
        const val TEMPERATURE = 0.6f
        const val MAX_HISTORY_CHARS = 6000
        const val SYSTEM_PROMPT =
            "You are the offline assistant built into MT_SW, an Android and desktop app for Meshtastic, " +
                "an open-source LoRa mesh radio network that works without internet. " +
                "Answer in the language the user writes in (usually Polish), briefly and in simple words. " +
                "Help with Meshtastic, radio settings, antennas, range, nodes, channels and the app. " +
                "If you are not sure of something, say so instead of guessing. Never invent settings or menu names."
    }
}
