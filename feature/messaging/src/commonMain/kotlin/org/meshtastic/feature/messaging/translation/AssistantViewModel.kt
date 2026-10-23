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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.nowMillis
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
class AssistantViewModel(
    private val service: AssistantService,
    private val history: AssistantHistoryStore,
    uiPrefs: UiPrefs,
) : ViewModel() {

    val enabled: StateFlow<Boolean> = uiPrefs.assistantEnabled
    val modelReady: StateFlow<Boolean> = service.ready

    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    private val _saved = MutableStateFlow<List<StoredChat>>(emptyList())

    /** Saved conversations, newest first. */
    val saved: StateFlow<List<StoredChat>> = _saved.asStateFlow()

    private var job: Job? = null
    private var chatId: String? = null

    init {
        refreshSaved()
    }

    fun refreshSaved() {
        viewModelScope.launch { _saved.value = runCatching { history.list() }.getOrDefault(emptyList()) }
    }

    fun setInput(value: String) = _state.update { it.copy(input = value) }

    fun send() {
        val current = _state.value
        val question = current.input.trim()
        if (question.isEmpty() || current.busy) return
        val history =
            current.lines.map { AssistantTurn(it.fromUser, AssistantActions.strip(it.text)) } +
                AssistantTurn(true, question)
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
                    _state.update { st ->
                        val last = st.lines.lastOrNull()
                        if (last != null && !last.fromUser && last.text.isBlank() && st.lines.size >= 2) {
                            // Nothing came back: take the question back so it can be sent again.
                            st.copy(
                                lines = st.lines.dropLast(2),
                                input = if (st.input.isBlank()) question else st.input,
                                failed = true,
                            )
                        } else {
                            st.copy(failed = true)
                        }
                    }
                } finally {
                    _state.update { it.copy(busy = false) }
                    withContext(NonCancellable) { persist() }
                }
            }
    }

    /** Asks the same question again: [index] is a question or the answer under it; the answer is replaced. */
    fun retry(index: Int) {
        val st = _state.value
        if (st.busy) return
        val questionIndex = if (st.lines.getOrNull(index)?.fromUser == true) index else index - 1
        val question = st.lines.getOrNull(questionIndex)?.takeIf { it.fromUser } ?: return
        _state.update { it.copy(lines = it.lines.take(questionIndex), input = question.text, failed = false) }
        send()
    }

    fun stop() {
        job?.cancel()
    }

    /** Starts a fresh conversation; the previous one stays in the saved list. */
    fun startNew() {
        job?.cancel()
        chatId = null
        _state.update { AssistantUiState() }
    }

    fun open(chat: StoredChat) {
        job?.cancel()
        chatId = chat.id
        _state.update { AssistantUiState(lines = chat.lines.map { AssistantLine(it.fromUser, it.text) }) }
    }

    fun deleteSaved(id: String) {
        if (id == chatId) startNew()
        viewModelScope.launch {
            runCatching { history.delete(id) }
            refreshSaved()
        }
    }

    private suspend fun persist() {
        val lines = _state.value.lines.filter { it.text.isNotBlank() }
        if (lines.none { it.fromUser }) return
        val id = chatId ?: nowMillis.toString().also { chatId = it }
        val title = lines.first { it.fromUser }.text.lines().first().take(TITLE_LENGTH)
        runCatching {
            history.save(StoredChat(id, title, nowMillis, lines.map { StoredLine(it.fromUser, it.text) }))
        }
        refreshSaved()
    }

    private fun appendToAnswer(piece: String) = _state.update { s ->
        if (s.lines.isEmpty() || s.lines.last().fromUser) return@update s
        s.copy(lines = s.lines.dropLast(1) + AssistantLine(false, s.lines.last().text + piece))
    }

    private fun setAnswer(answer: String) = _state.update { s ->
        if (s.lines.isEmpty() || s.lines.last().fromUser || answer.isBlank()) return@update s
        s.copy(lines = s.lines.dropLast(1) + AssistantLine(false, answer))
    }

    private companion object {
        const val TITLE_LENGTH = 48
    }
}
