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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.currentLocaleCode
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.repository.TranslatorModelManager
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.repository.UiPrefs

/** What the translator screen shows. A null [sourceNllb] means "detect the language". */
data class TranslatorUiState(
    val input: String = "",
    val sourceNllb: String? = null,
    val targetNllb: String = "pol_Latn",
    val output: String = "",
    val busy: Boolean = false,
    val error: TranslatorError? = null,
)

enum class TranslatorError {
    /** Could not tell the language of the text; the user should choose it. */
    UnknownLanguage,

    /** The text is already in the chosen language. */
    SameLanguage,

    /** The translator failed (for example the device ran out of memory). */
    Failed,

    /** The model has not been downloaded. */
    ModelMissing,
}

@KoinViewModel
class TranslatorViewModel(
    private val service: MessageTranslationService,
    private val modelManager: TranslatorModelManager,
    uiPrefs: UiPrefs,
) : ViewModel() {

    val enabled: StateFlow<Boolean> = uiPrefs.translatorEnabled
    val modelState: StateFlow<TranslatorModelState> = modelManager.state

    private val _state =
        MutableStateFlow(
            TranslatorUiState(targetNllb = TranslatorLanguages.fromLocale(currentLocaleCode())?.nllb ?: "pol_Latn"),
        )
    val state: StateFlow<TranslatorUiState> = _state.asStateFlow()

    fun setInput(value: String) = _state.update { it.copy(input = value, error = null) }

    fun setSource(nllb: String?) = _state.update { it.copy(sourceNllb = nllb, error = null) }

    fun setTarget(nllb: String) = _state.update { it.copy(targetNllb = nllb, error = null) }

    fun swapLanguages() = _state.update {
        val newTarget = it.sourceNllb ?: LanguageGuesser.guess(it.input) ?: return@update it
        it.copy(sourceNllb = it.targetNllb, targetNllb = newTarget, input = it.output.ifBlank { it.input }, output = "")
    }

    fun translate() {
        val current = _state.value
        if (current.input.isBlank() || current.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, output = "") }
            val started = nowMillis
            val result = service.translateBetween(current.input, current.sourceNllb, current.targetNllb)
            Logger.i(tag = "TranslatorViewModel") { "Translation finished in ${nowMillis - started} ms: ${result::class.simpleName}" }
            _state.update {
                when (result) {
                    is TranslationResult.Success -> it.copy(busy = false, output = result.translatedText)
                    is TranslationResult.NotRequired -> it.copy(busy = false, error = TranslatorError.SameLanguage)
                    is TranslationResult.ModelDownloadRequired ->
                        it.copy(busy = false, error = TranslatorError.ModelMissing)
                    is TranslationResult.Unavailable ->
                        it.copy(
                            busy = false,
                            error =
                            if (current.sourceNllb == null && LanguageGuesser.guess(current.input) == null) {
                                TranslatorError.UnknownLanguage
                            } else {
                                TranslatorError.Failed
                            },
                        )
                }
            }
        }
    }

    /** Warn when a translation will not fit in one radio packet (about 200 characters). */
    fun isTooLongForOnePacket(text: String): Boolean = text.length > ONE_PACKET_CHARS

    companion object {
        const val ONE_PACKET_CHARS = 200
    }
}
