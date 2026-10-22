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

/** The assistant on top of [LiteRtEngine]; it shares the loaded model with the translator. */
class LlamaAssistantService(
    private val engine: LiteRtEngine,
    private val store: AiModelStoreImpl,
    private val uiPrefs: UiPrefs,
    private val data: AssistantDataProvider,
    tools: AssistantTools = AssistantTools(data),
) : AssistantService {

    private val toolSets = listOf(tools)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val ready: StateFlow<Boolean> =
        combine(store.states, uiPrefs.assistantModelId) { states, selected -> activeId(states, selected) != null }
            .stateIn(scope, SharingStarted.Eagerly, false)

    // Rolling memory: older turns are folded into a short summary so a conversation can go on without end while the
    // prompt stays inside the model's window.
    private var memoryKey = ""
    private var memory = ""
    private var memorized = 0

    override suspend fun reply(history: List<AssistantTurn>, onPiece: (String) -> Unit): String {
        val modelId = activeId(store.states.value, uiPrefs.assistantModelId.value) ?: error("No assistant model")
        val question = history.lastOrNull { it.fromUser }?.text.orEmpty()
        val recent = fit(modelId, history)
        return try {
            ask(modelId, recent, memory, data.contextFor(question, AUTO_CONTEXT_CHARS), onPiece)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // The model only takes about 4000 tokens at once: try again with a much shorter prompt.
            if (e.message?.contains("too long", ignoreCase = true) != true) throw e
            ask(modelId, recent.takeLast(1), "", data.contextFor(question, SHORT_CONTEXT_CHARS), onPiece)
        }
    }

    /** Returns the newest turns that fit and, when older ones were dropped, updates [memory] with their summary. */
    private suspend fun fit(modelId: String, history: List<AssistantTurn>): List<AssistantTurn> {
        val turns = history.filterNot { it.text.isBlank() && !it.fromUser }.map { it.copy(text = it.text.take(MAX_TURN_CHARS)) }
        val key = turns.firstOrNull()?.text.orEmpty()
        if (key != memoryKey) {
            memoryKey = key
            memory = ""
            memorized = 0
        }
        var budget = RECENT_CHARS
        var start = turns.size
        while (start > 0) {
            val len = turns[start - 1].text.length
            if (start < turns.size && budget - len < 0) break
            budget -= len
            start--
        }
        // A conversation window starts with a question, never with an answer.
        if (start in 1 until turns.size - 1 && !turns[start].fromUser) start++
        if (memorized > start) {
            memory = ""
            memorized = 0
        }
        if (start > memorized) {
            val added = turns.subList(memorized, start)
            val summary = summarize(modelId, memory, added)
            if (summary != null) {
                memory = summary
                memorized = start
            }
        }
        return turns.subList(start, turns.size)
    }

    private suspend fun summarize(modelId: String, previous: String, added: List<AssistantTurn>): String? = try {
        val text =
            added.joinToString("\n") { (if (it.fromUser) "User: " else "Assistant: ") + it.text.take(SUMMARY_TURN_CHARS) }
                .takeLast(SUMMARY_INPUT_CHARS)
        val prompt =
            "Below are the summary of an earlier conversation and new messages. Write one updated summary of the " +
                "whole conversation in at most 6 short sentences, in the language of the conversation. Keep names, " +
                "numbers, node names and what the user wants. Output only the summary.\n\n" +
                "Earlier summary: ${previous.ifBlank { "(none)" }}\n\nNew messages:\n$text"
        engine.chat(modelId, listOf("user" to prompt), SUMMARY_TOKENS, SUMMARY_TEMPERATURE).trim().take(MAX_SUMMARY_CHARS)
            .takeIf { it.isNotBlank() }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun ask(
        modelId: String,
        turns: List<AssistantTurn>,
        summary: String,
        context: String,
        onPiece: (String) -> Unit,
    ): String {
        val system = buildString {
            append(SYSTEM_PROMPT).append("\n\n").append(TOOLS_RULE)
            if (summary.isNotBlank()) append("\n\nSummary of the earlier part of this conversation: ").append(summary)
            if (context.isNotBlank()) append("\n\n").append(DATA_INTRO).append('\n').append(context)
        }
        val messages = buildList {
            add("system" to system)
            turns.forEach { add((if (it.fromUser) "user" else "assistant") to it.text) }
        }
        return engine.chat(
            modelId = modelId,
            messages = messages,
            maxTokens = MAX_ANSWER_TOKENS,
            temperature = TEMPERATURE,
            onPiece = onPiece,
            tools = toolSets,
        )
    }

    private fun activeId(states: Map<String, TranslatorModelState>, selected: String): String? {
        val chosen = AiModels.byId(selected)?.takeIf { it.canDo(AiRole.ASSISTANT) && it.engineReady }
        if (chosen != null && states[chosen.id] == TranslatorModelState.Ready) return chosen.id
        return AiModels.all
            .firstOrNull { it.canDo(AiRole.ASSISTANT) && it.engineReady && states[it.id] == TranslatorModelState.Ready }
            ?.id
    }

    private companion object {
        const val MAX_ANSWER_TOKENS = 0 // no limit: the model finishes by itself
        const val TEMPERATURE = 0.5f
        const val RECENT_CHARS = 1500
        const val MAX_TURN_CHARS = 1500
        const val SUMMARY_TURN_CHARS = 500
        const val SUMMARY_INPUT_CHARS = 2500
        const val MAX_SUMMARY_CHARS = 700
        const val SUMMARY_TOKENS = 250
        const val SUMMARY_TEMPERATURE = 0.2f
        const val SHORT_CONTEXT_CHARS = 800
        const val AUTO_CONTEXT_CHARS = 1200
        const val DATA_INTRO =
            "Below is live read-only data from the user's app and radio. Use it to answer questions about their " +
                "own network and device. heap is the free internal RAM of the radio node (not the phone). " +
                "Copy every number and unit exactly as it appears in the data (keep the decimal point and the minus " +
                "sign, never combine or change digits); keep the units that stand next to the values (for example °C, %, hPa, V) " +
                "and never add more decimals than the data has; if a value is marked IMPLAUSIBLE say that the sensor is probably faulty and do not use the value; if a value is unknown say so. Answer briefly, list each device only once and never repeat yourself. Write plain text only: no " +
                "markdown, no asterisks, no backticks."
        const val TOOLS_RULE =
            "You have read-only tools that look up the app's data: network overview, node names, details and " +
                "history of any node, ranked node tables (packets, memory, battery, signal, distance), WEATHER " +
                "and sensor data (temperature, humidity, pressure, ...), channel messages for summaries (never ask the " +
                "user for keywords when asked to summarise a channel: read the latest messages and summarise them), radio settings and messages. For ANY question about the user's network, nodes, " +
                "statistics, history, settings, sensors or messages, call the matching tool first and answer from " +
                "its result; never say you have no data before you have tried the tools. You cannot change " +
                "settings or send messages; if the user wants that, explain what to do or draft the text for them."
        const val SYSTEM_PROMPT =
            "You are the offline assistant built into MT_SW, an Android and desktop app for Meshtastic, " +
                "an open-source LoRa mesh radio network that works without internet. " +
                "Answer in the language the user writes in (usually Polish), briefly and in simple words. " +
                "Help with Meshtastic, radio settings, antennas, range, nodes, channels and the app. " +
                "If you are not sure of something, say so instead of guessing. Never invent settings or menu names."
    }
}
