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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    /** Results of the translation test (the /tłumacz command), one line per sentence; empty when no test was run. */
    val testLines: List<TranslatorTestLine> = emptyList(),
    val testing: Boolean = false,
    /** Translations made on this screen since it was opened (or cleared), oldest first; they can be saved as a file. */
    val history: List<TranslatorHistoryEntry> = emptyList(),
)

/** One finished translation: the text as typed, its translation and when it was made. */
data class TranslatorHistoryEntry(val source: String, val result: String, val at: Long)

/** One sentence of the translation test: what was translated, the result with the check, and how long it took. */
data class TranslatorTestLine(val source: String, val result: String, val tookMs: Long)

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
    private val assistant: AssistantService,
    uiPrefs: UiPrefs,
) : ViewModel() {

    private var testJob: Job? = null

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
        // The same command as in the assistant: "/tłumacz" (also without Polish letters) runs the translation test.
        if (isTestCommand(current.input)) {
            runTest()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, output = "") }
            val started = nowMillis
            val result = service.translateBetween(current.input, current.sourceNllb, current.targetNllb)
            Logger.i(tag = "TranslatorViewModel") { "Translation finished in ${nowMillis - started} ms: ${result::class.simpleName}" }
            _state.update {
                when (result) {
                    is TranslationResult.Success ->
                        it.copy(
                            busy = false,
                            output = result.translatedText,
                            history = it.history + TranslatorHistoryEntry(current.input.trim(), result.translatedText, nowMillis),
                        )
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

    private fun isTestCommand(text: String): Boolean =
        text.trim().lowercase().map { POLISH_TO_PLAIN[it] ?: it }.joinToString("") == TEST_COMMAND

    /**
     * The translation test of the assistant (the same sentences for every model, so results can be compared): each
     * sentence appears here with its translation, a check that names, numbers and links stayed as they are, and its time.
     */
    private fun runTest() {
        _state.update { it.copy(busy = true, testing = true, testLines = emptyList(), input = "", output = "", error = null) }
        testJob =
            viewModelScope.launch {
                var mark = nowMillis
                try {
                    assistant.selfTest(TEST_COMMAND) { source, result ->
                        val now = nowMillis
                        val took = (now - mark).coerceAtLeast(1L)
                        mark = now
                        _state.update { it.copy(testLines = it.testLines + TranslatorTestLine(source, result, took)) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.w(tag = "TranslatorViewModel", throwable = e) { "Translation test failed: ${e.message}" }
                    _state.update { it.copy(error = TranslatorError.Failed) }
                } finally {
                    _state.update { it.copy(busy = false, testing = false) }
                }
            }
    }

    /** Forgets the translations kept for saving. */
    fun clearHistory() = _state.update { it.copy(history = emptyList()) }

    /** Stops a running translation test. */
    fun cancelTest() {
        testJob?.cancel()
    }

    /** Warn when a translation will not fit in one radio packet (about 200 characters). */
    fun isTooLongForOnePacket(text: String): Boolean = text.length > ONE_PACKET_CHARS

    companion object {
        const val ONE_PACKET_CHARS = 200
        private const val TEST_COMMAND = "/tlumacz"
        private val POLISH_TO_PLAIN =
            mapOf('ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n', 'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z')
    }
}
