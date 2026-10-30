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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.ui.assistant.AssistantLinks
import org.meshtastic.core.ui.assistant.SettingHighlight

/** One bubble of the conversation. */
/** [at] is when a question was sent or an answer received (milliseconds, 0 = not known); [tookMs] is how long an answer took. */
data class AssistantLine(val fromUser: Boolean, val text: String, val at: Long = 0L, val tookMs: Long = 0L, val info: String = "")

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
        val command = testCommand(question)
        if (command != null) {
            runSelfTest(command)
            return
        }
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
        pending.value = ""
        val sentAt = nowMillis
        _state.update { st -> st.copy(lines = st.lines.dropLast(2) + AssistantLine(true, question, at = sentAt) + st.lines.last()) }
        val pump =
            viewModelScope.launch {
                while (true) {
                    delay(FLUSH_MS)
                    flushPieces()
                }
            }
        job =
            viewModelScope.launch {
                try {
                    val answer = service.reply(history) { piece -> appendToAnswer(piece) }
                    flushPieces()
                    setAnswer(answer)
                    // The user asked to go somewhere ("show node X on the map"): the app opens it right away.
                    AssistantActions.parse(answer).second.firstOrNull { it.auto }?.let { action ->
                        SettingHighlight.show(action.highlight)
                        AssistantLinks.open(action.uri)
                    }
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
                    pump.cancel()
                    flushPieces()
                    stampAnswer(sentAt)
                    _state.update { it.copy(busy = false) }
                    withContext(NonCancellable) { persist() }
                }
            }
    }

    /** The test command typed in [text] with or without Polish letters (/tłumacz = /tlumacz), or null when it is none. */
    private fun testCommand(text: String): String? {
        val plain = text.trim().lowercase().map { POLISH_TO_PLAIN[it] ?: it }.joinToString("")
        return SELF_TEST_COMMANDS.firstOrNull { it == plain }
    }

    /**
     * Typed "/test": the assistant asks itself all the test questions one after another; each question and answer
     * appears in the chat (and in the terminal and a results file on a computer). Nothing opens by itself meanwhile.
     */
    private fun runSelfTest(command: String) {
        _state.update { it.copy(input = "", busy = true, failed = false) }
        job =
            viewModelScope.launch {
                try {
                    // Every test answer gets its time and the model and route, like an answer in a normal conversation.
                    var mark = nowMillis
                    service.selfTest(command) { q, a ->
                        val now = nowMillis
                        val took = (now - mark).coerceAtLeast(1L)
                        mark = now
                        val info = service.lastInfo
                        _state.update {
                            it.copy(lines = it.lines + AssistantLine(true, q, at = now - took) + AssistantLine(false, a, at = now, tookMs = took, info = info))
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Logger.w(tag = "AssistantViewModel", throwable = e) { "Self test failed: ${e.message}" }
                    _state.update { it.copy(failed = true) }
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
        _state.update { AssistantUiState(lines = chat.lines.map { AssistantLine(it.fromUser, it.text, it.at, it.tookMs, it.info) }) }
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
            history.save(StoredChat(id, title, nowMillis, lines.map { StoredLine(it.fromUser, it.text, it.at, it.tookMs, it.info) }))
        }
        refreshSaved()
    }

    // The model gives the answer word by word; showing every word redraws the chat that often and makes it stutter.
    // The words are collected here and shown together a few times a second instead.
    private val pending = MutableStateFlow("")

    private fun appendToAnswer(piece: String) = pending.update { it + piece }

    private fun flushPieces() {
        val text = pending.getAndUpdate { "" }
        if (text.isEmpty()) return
        _state.update { s ->
            if (s.lines.isEmpty() || s.lines.last().fromUser) return@update s
            s.copy(lines = s.lines.dropLast(1) + s.lines.last().copy(text = s.lines.last().text + text))
        }
    }

    private fun setAnswer(answer: String) = _state.update { s ->
        if (s.lines.isEmpty() || s.lines.last().fromUser || answer.isBlank()) return@update s
        s.copy(lines = s.lines.dropLast(1) + s.lines.last().copy(text = answer))
    }

    /** Notes when the answer arrived and how long it took (also for an answer stopped halfway). */
    private fun stampAnswer(sentAt: Long) = _state.update { s ->
        val last = s.lines.lastOrNull() ?: return@update s
        if (last.fromUser || last.at != 0L || last.text.isBlank()) return@update s
        val now = nowMillis
        s.copy(lines = s.lines.dropLast(1) + last.copy(at = now, tookMs = now - sentAt, info = service.lastInfo))
    }

    private companion object {
        const val TITLE_LENGTH = 48
        const val FLUSH_MS = 120L
        const val SELF_TEST = "/test"

        private val POLISH_TO_PLAIN =
            mapOf('ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n', 'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z')

        /** /test runs everything; the others run one group (tools, free chat, knowledge base, hardware report). */
        val SELF_TEST_COMMANDS = listOf(SELF_TEST, "/szybki", "/narzedzia", "/rozmowa", "/baza", "/tlumacz", "/sprzet")
    }
}
