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
@file:Suppress("ReturnCount", "TooGenericExceptionCaught")

package org.meshtastic.feature.messaging.translation

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.meshtastic.core.repository.AiModelInfo
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.AiRole
import org.meshtastic.core.repository.TranslatorModelManager
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.repository.UiPrefs

/** Message translator on the iPhone: the chosen language model translates; also the model manager of its screen. */
class IosTranslationRouter(
    private val gemma: GemmaTranslator,
    private val store: IosAiModelStore,
    private val uiPrefs: UiPrefs,
) : MessageTranslationService,
    TranslatorModelManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val state: StateFlow<TranslatorModelState> =
        combine(store.states, uiPrefs.translatorModelId) { states, selected ->
            states[activeId(states, selected)] ?: TranslatorModelState.NotDownloaded
        }
            .stateIn(scope, SharingStarted.Eagerly, TranslatorModelState.NotDownloaded)

    override val approximateSizeMb: Int
        get() = activeModel().sizeMb

    override val approximateRamMb: Int
        get() = activeModel().ramMb

    override suspend fun isLanguageAvailable(locale: String): Boolean =
        uiPrefs.translatorEnabled.value && TranslatorLanguages.fromLocale(locale) != null

    override suspend fun translate(text: String, targetLocale: String): TranslationResult {
        if (text.isBlank() || !uiPrefs.translatorEnabled.value) return TranslationResult.Unavailable
        val target = TranslatorLanguages.fromLocale(targetLocale) ?: return TranslationResult.Unavailable
        val source = LanguageGuesser.guess(text) ?: return TranslationResult.Unavailable
        if (source == target.nllb) return TranslationResult.NotRequired
        return translateBetween(text, source, target.nllb)
    }

    override suspend fun translateBetween(text: String, sourceNllb: String?, targetNllb: String): TranslationResult {
        if (text.isBlank() || !uiPrefs.translatorEnabled.value) return TranslationResult.Unavailable
        val source = sourceNllb ?: LanguageGuesser.guess(text) ?: return TranslationResult.Unavailable
        if (source == targetNllb) return TranslationResult.NotRequired
        val model = activeModel()
        if (store.states.value[model.id] != TranslatorModelState.Ready) {
            return TranslationResult.ModelDownloadRequired(listOf(model.id), model.sizeMb)
        }
        return try {
            TranslationResult.Success(gemma.translate(model.id, text, source, targetNllb))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Logger.w(throwable = e) { "Translation with ${model.name} failed: ${e.message}" }
            TranslationResult.Unavailable
        }
    }

    override suspend fun downloadLanguageModels(languageTags: List<String>): DownloadResult {
        download()
        return when (val s = state.value) {
            TranslatorModelState.Ready -> DownloadResult.Success
            is TranslatorModelState.Failed -> DownloadResult.Failed(s.reason)
            else -> DownloadResult.Failed("Download not finished")
        }
    }

    override suspend fun download() = store.download(activeModel().id)

    override fun cancelDownload() = store.cancelDownload(activeModel().id)

    override suspend fun deleteModel() = store.delete(activeModel().id)

    private fun activeModel(): AiModelInfo =
        AiModels.byId(activeId(store.states.value, uiPrefs.translatorModelId.value)) ?: AiModels.G4E2b

    private fun activeId(states: Map<String, TranslatorModelState>, selected: String): String {
        val chosen = AiModels.byId(selected)?.takeIf { it.canDo(AiRole.TRANSLATOR) && it.engineReady }
        if (chosen != null && states[chosen.id] == TranslatorModelState.Ready) return chosen.id
        val other =
            AiModels.all.firstOrNull {
                it.canDo(AiRole.TRANSLATOR) && it.engineReady && states[it.id] == TranslatorModelState.Ready
            }
        return (other ?: chosen ?: AiModels.G4E2b).id
    }
}
