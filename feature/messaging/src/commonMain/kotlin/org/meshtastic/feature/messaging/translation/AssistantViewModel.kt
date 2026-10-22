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
import org.meshtastic.core.repository.UiPrefs

/** One bubble of the conversation. */
data class AssistantLine(val fromUser: Boolean, val text: String)

data class AssistantUiState(
    val lines: List<AssistantLine> = emptyList(),
    val input: String = "",
    val busy: Boolean = false,
    val failed: Boolean = false,
)

@KoinViewModel
class AssistantViewModel(private val service: AssistantService, uiPrefs: UiPrefs) : ViewModel() {

    val enabled: StateFlow<Boolean> = uiPrefs.assistantEnabled
    val modelReady: StateFlow<Boolean> = service.ready

    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun setInput(value: String) = _state.update { it.copy(input = value) }

    fun send() {
        val current = _state.value
        val question = current.input.trim()
        if (question.isEmpty() || current.busy) return
        val history = current.lines.map { AssistantTurn(it.fromUser, it.text) } + AssistantTurn(true, question)
        _state.update {
            it.copy(
                lines = it.lines + AssistantLine(true, question) + AssistantLine(false, ""),
                input = "",
                busy = true,
                failed = false,
            )
        }
        job =
            viewModelScope.launch {
                try {
                    val answer = service.reply(history) { piece -> appendToAnswer(piece) }
                    setAnswer(answer)
                } catch (e: CancellationException) {
                    // Stopped by the user: keep what was written so far.
                    throw e
                } catch (e: Throwable) {
                    Logger.w(tag = "AssistantViewModel", throwable = e) { "Assistant failed: ${e.message}" }
                    _state.update { it.copy(failed = true) }
                } finally {
                    _state.update { it.copy(busy = false) }
                }
            }
    }

    fun stop() {
        job?.cancel()
    }

    fun clear() {
        job?.cancel()
        _state.update { AssistantUiState() }
    }

    private fun appendToAnswer(piece: String) = _state.update { s ->
        if (s.lines.isEmpty() || s.lines.last().fromUser) return@update s
        s.copy(lines = s.lines.dropLast(1) + AssistantLine(false, s.lines.last().text + piece))
    }

    private fun setAnswer(answer: String) = _state.update { s ->
        if (s.lines.isEmpty() || s.lines.last().fromUser || answer.isBlank()) return@update s
        s.copy(lines = s.lines.dropLast(1) + AssistantLine(false, answer))
    }
}
