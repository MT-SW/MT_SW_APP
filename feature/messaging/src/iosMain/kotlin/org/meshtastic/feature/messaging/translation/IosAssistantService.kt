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
import org.meshtastic.core.repository.AssistantKnowledge
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.repository.UiPrefs
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

private const val KNOWLEDGE_CHARS = 3500
private const val HISTORY_TURNS = 6
private const val ANSWER_TOKENS = 700

/** The offline assistant on the iPhone: answers with the chosen model, using the app's own knowledge as the source. */
class IosAssistantService(
    private val engine: AiTextEngine,
    private val store: IosAiModelStore,
    private val uiPrefs: UiPrefs,
    private val knowledge: AssistantKnowledge,
) : AssistantService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun pick(states: Map<String, TranslatorModelState>, chosen: String): String? {
        fun usable(id: String) = AiModels.byId(id)?.let { it.canDo(AiRole.ASSISTANT) && it.engineReady } == true &&
            states[id] == TranslatorModelState.Ready
        if (usable(chosen)) return chosen
        return AiModels.all.firstOrNull { usable(it.id) }?.id
    }

    override val ready: StateFlow<Boolean> =
        combine(store.states, uiPrefs.assistantModelId) { states, id -> pick(states, id) != null }
            .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun reply(history: List<AssistantTurn>, onPiece: (String) -> Unit): String {
        val model = checkNotNull(pick(store.states.value, uiPrefs.assistantModelId.value)) { "No assistant model" }
        val polish = (NSLocale.preferredLanguages.firstOrNull() as? String)?.startsWith("pl") != false
        val language = if (polish) "pl" else "en"
        val question = history.last { it.fromUser }.text
        val facts = runCatching { knowledge.lookup(question, KNOWLEDGE_CHARS, language) }.getOrDefault("")
        val system =
            if (polish) {
                "Jesteś asystentem aplikacji MT_SW (sieć LoRa mesh). Odpowiadaj po polsku, prosto i krótko. " +
                    "Korzystaj z wiedzy poniżej; jeśli jej brakuje, powiedz, że nie wiesz.\n\n$facts"
            } else {
                "You are the assistant of the MT_SW app (LoRa mesh network). Answer simply and briefly. " +
                    "Use the knowledge below; if it is missing, say you do not know.\n\n$facts"
            }
        val messages = buildList {
            add("system" to system)
            history.dropLast(1).takeLast(HISTORY_TURNS).forEach {
                add((if (it.fromUser) "user" else "assistant") to it.text)
            }
            add("user" to question)
        }
        return engine.chat(model, messages, ANSWER_TOKENS, 0.3f, onPiece).trim()
    }
}
