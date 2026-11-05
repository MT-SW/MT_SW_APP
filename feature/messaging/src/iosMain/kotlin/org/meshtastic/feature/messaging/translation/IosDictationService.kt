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
import org.meshtastic.core.repository.AiEngine
import org.meshtastic.core.repository.AiModelInfo
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.AiRole
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.repository.UiPrefs
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

/** Dictation on the iPhone with the hearing of a downloaded Gemma model; no speech service of the system is used. */
class IosDictationService(
    private val engine: AiTextEngine,
    private val store: IosAiModelStore,
    private val uiPrefs: UiPrefs,
) : DictationService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun usable(model: AiModelInfo, states: Map<String, TranslatorModelState>) = model.canDo(AiRole.DICTATION) &&
        (model.hearing || model.engine == AiEngine.WHISPER_CPP) &&
        model.engineReady &&
        states[model.id] == TranslatorModelState.Ready

    private fun pick(states: Map<String, TranslatorModelState>, id: String): String? {
        AiModels.byId(id)
            ?.takeIf { usable(it, states) }
            ?.let {
                return it.id
            }
        return AiModels.all.firstOrNull { usable(it, states) }?.id
    }

    private fun flow(): StateFlow<Boolean> =
        combine(store.states, uiPrefs.dictationModelId, uiPrefs.dictationEnabled) { states, id, on ->
            on && pick(states, id) != null
        }
            .stateIn(scope, SharingStarted.Eagerly, false)

    override val available: StateFlow<Boolean> = flow()

    override val translatorAvailable: StateFlow<Boolean> = flow()

    override suspend fun transcribe(wav: ByteArray, translator: Boolean): String {
        check(uiPrefs.dictationEnabled.value) { "Dictation is switched off" }
        val id = checkNotNull(pick(store.states.value, uiPrefs.dictationModelId.value)) { "Dictation is not available" }
        // The dictation for the chat is in the language of the app; the translator's can be in any language.
        IosDictationLanguage.hint = if (translator) "auto" else appLanguage()
        return engine.transcribe(id, wav)
    }

    private fun appLanguage(): String {
        val code = (NSLocale.preferredLanguages.firstOrNull() as? String)?.take(2)?.lowercase()
        return code?.takeIf { it in SUPPORTED } ?: "auto"
    }

    private companion object {
        val SUPPORTED =
            setOf(
                "pl",
                "en",
                "de",
                "fr",
                "es",
                "it",
                "cs",
                "sk",
                "uk",
                "ru",
                "nl",
                "pt",
                "sv",
                "no",
                "da",
                "fi",
                "hu",
                "ro",
                "bg",
                "hr",
                "sl",
                "lt",
                "lv",
                "et",
                "tr",
            )
    }
}
