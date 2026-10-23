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

import co.touchlab.kermit.Logger
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.repository.AiModels
import java.io.File

/**
 * Runs the Gemma 4 language models (`.litertlm` files) with Google's LiteRT-LM, on Android and on desktop. The neural
 * chip is tried first (when the platform provides it), then the graphics chip (Metal on a Mac, OpenCL/Vulkan
 * elsewhere), and the processor last. Only one model is in memory at a time and requests run one after another, so translation and the assistant
 * can safely share it. The model is loaded on first use and freed again after a short idle time.
 */
class LiteRtEngine(private val store: AiModelStoreImpl) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runMutex = Mutex()
    private var unloadJob: Job? = null
    private var engine: Engine? = null
    private var loadedId: String? = null

    /**
     * Folder with the neural chip libraries, set by the platform when it has them (Android passes its native library
     * folder). Without it the neural chip is skipped.
     */
    @Volatile var npuLibraryDir: String? = null

    /** Name of the backend of the loaded model (NPU, GPU or CPU); empty while nothing is loaded. */
    @Volatile
    var activeBackend: String = ""
        private set

    init {
        store.beforeModelDelete = { unloadNow() }
    }

    /**
     * Sends [messages] (role to text pairs, roles `system`, `user`, `assistant`; the last one must be a `user`
     * message) to model [modelId] and returns the reply. [onPiece] receives the text as it is produced. Cancelling the
     * coroutine stops the generation. [maxTokens] is a rough limit on the length of the answer, 0 means no limit (the model stops by itself).
     */
    suspend fun chat(
        modelId: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        temperature: Float,
        onPiece: (String) -> Unit = {},
        tools: List<ToolSet> = emptyList(),
    ): String = runMutex.withLock {
        unloadJob?.cancel()
        try {
            withContext(Dispatchers.Default) { generate(modelId, messages, maxTokens, temperature, onPiece, tools) }
        } finally {
            scheduleUnload()
        }
    }

    /** Frees the model from memory right away (for example when it is deleted). */
    suspend fun unloadNow() {
        runMutex.withLock { unload() }
    }

    private suspend fun generate(
        modelId: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        temperature: Float,
        onPiece: (String) -> Unit,
        tools: List<ToolSet>,
    ): String {
        val started = nowMillis
        val wasLoaded = loadedId == modelId
        val loaded = ensureLoaded(modelId)
        val loadedAt = nowMillis

        // The native library turns the texts into JSON and aborts the whole app on characters outside the basic plane
        // (emoji are passed to it in a form it rejects) or on broken ones, so those are swapped for short markers
        // like <E1> and put back in the answer.
        val symbols = LinkedHashMap<String, String>()
        val clean = messages.map { (role, text) -> role to text.cleanForModel(symbols) }
        val system = clean.filter { it.first == SYSTEM }.joinToString("\n") { it.second }
        val dialogue = clean.filter { it.first != SYSTEM }
        require(dialogue.isNotEmpty() && dialogue.last().first == USER) { "The last message must come from the user" }
        val history =
            dialogue.dropLast(1).map { (role, text) -> if (role == ASSISTANT) Message.model(text) else Message.user(text) }
        val config =
            ConversationConfig(
                systemInstruction = if (system.isBlank()) null else Contents.of(system),
                initialMessages = history,
                tools = tools.map { tool(it) },
                samplerConfig = SamplerConfig(topK = TOP_K, topP = TOP_P, temperature = temperature.toDouble()),
            )

        val reply = StringBuilder()
        val restorer = SymbolRestorer(symbols)
        val limit = if (maxTokens > 0) maxTokens * CHARS_PER_TOKEN else Int.MAX_VALUE
        try {
            loaded.createConversation(config).use { conversation ->
                conversation
                    .sendMessageAsync(dialogue.last().second)
                    .takeWhile { reply.length < limit && !reply.isLooping() }
                    .collect { message ->
                        val piece = message.toString()
                        if (piece.isNotEmpty()) {
                            reply.append(piece)
                            restorer.push(piece).takeIf { it.isNotEmpty() }?.let { runCatching { onPiece(it) } }
                        }
                    }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // The model's window is full: keep what was written instead of failing the whole answer.
            if (reply.isEmpty()) throw e
            Logger.w(tag = TAG, throwable = e) { "Answer ended early: ${e.message}" }
        }
        restorer.finish().takeIf { it.isNotEmpty() }?.let { runCatching { onPiece(it) } }
        Logger.i(tag = TAG) {
            "Answer from $modelId on $activeBackend: loaded=${!wasLoaded} in ${loadedAt - started} ms, generated ${reply.length} chars " +
                "in ${nowMillis - loadedAt} ms"
        }
        val finalText = reply.withoutLoop()
        return restorer.restoreAll(finalText).trim()
    }

    private fun ensureLoaded(modelId: String): Engine {
        engine?.let { if (loadedId == modelId) return it }
        unload()
        val file = store.fileOf(modelId) ?: error("Model $modelId is not downloaded")
        val cache = File(store.ggufDir, "cache").apply { mkdirs() }
        val window = contextTokens(modelId)
        // Order of preference: neural chip (only when the platform gave us its library folder), graphics chip,
        // processor. The first one that starts is used, and the choice is written to the log.
        val attempts = buildList<Pair<String, () -> Backend>> {
            npuLibraryDir?.let { dir -> add("NPU" to { Backend.NPU(dir) }) }
            add("GPU" to { Backend.GPU() })
            add("CPU" to { Backend.CPU() })
        }
        var created: Engine? = null
        var failure: Exception? = null
        for ((name, backend) in attempts) {
            try {
                created = start(file, backend(), cache, window)
                activeBackend = name
                Logger.i(tag = TAG) { "Model $modelId runs on $name (context $window tokens)" }
                break
            } catch (e: Exception) {
                failure = e
                Logger.w(tag = TAG, throwable = e) { "Backend $name not usable for $modelId: ${e.message}" }
            }
        }
        val ready = created ?: throw (failure ?: IllegalStateException("No backend could start"))
        engine = ready
        loadedId = modelId
        return ready
    }

    /** How many tokens (question, data and answer together) the model keeps in its window; bigger models get more. */
    fun contextTokens(modelId: String): Int =
        when (modelId) {
            AiModels.GEMMA_E2B_ID -> CONTEXT_SMALL
            else -> CONTEXT_LARGE
        }

    private fun start(file: File, backend: Backend, cache: File, window: Int): Engine {
        val created =
            Engine(
                EngineConfig(
                    modelPath = file.absolutePath,
                    backend = backend,
                    maxNumTokens = window,
                    cacheDir = cache.absolutePath,
                ),
            )
        try {
            created.initialize()
        } catch (e: Exception) {
            runCatching { created.close() }
            throw e
        }
        return created
    }

    /**
     * Makes the text safe for the native library: characters outside the basic plane (emoji) become markers recorded
     * in [symbols], unpaired surrogates, control characters and non-characters are dropped.
     */
    private fun String.cleanForModel(symbols: MutableMap<String, String>): String {
        val out = StringBuilder(length)
        var i = 0
        while (i < length) {
            val c = this[i]
            when {
                c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate() -> {
                    val symbol = substring(i, i + 2)
                    val marker = symbols.entries.firstOrNull { it.value == symbol }?.key
                        ?: "<E${symbols.size + 1}>".also { symbols[it] = symbol }
                    out.append(marker)
                    i++
                }
                c.isSurrogate() -> Unit
                c.code < 0x20 && c != '\n' && c != '\t' -> Unit
                c.code == 0xFFFE || c.code == 0xFFFF -> Unit
                else -> out.append(c)
            }
            i++
        }
        return out.toString()
    }

    /** True when the end of the text repeats itself over and over (a small model stuck in a loop). */
    private fun CharSequence.isLooping(): Boolean {
        if (length < LOOP_TAIL * 3) return false
        val text = toString()
        val tail = text.takeLast(LOOP_TAIL)
        val window = text.takeLast(LOOP_WINDOW)
        var count = 0
        var from = 0
        while (true) {
            val at = window.indexOf(tail, from)
            if (at < 0) break
            count++
            from = at + 1
        }
        return count >= LOOP_REPEATS
    }

    /** Cuts a looping ending after its first occurrence. */
    private fun CharSequence.withoutLoop(): String {
        val text = toString()
        if (!isLooping()) return text
        val tail = text.takeLast(LOOP_TAIL)
        val first = text.indexOf(tail)
        return if (first >= 0) text.substring(0, first + LOOP_TAIL).trimEnd() else text
    }

    /** Puts the original symbols back into the streamed answer; a marker split between two pieces is held back. */
    private class SymbolRestorer(private val symbols: Map<String, String>) {
        private val pending = StringBuilder()

        fun restoreAll(text: String): String = symbols.entries.fold(text) { acc, (marker, symbol) -> acc.replace(marker, symbol) }

        fun push(piece: String): String {
            pending.append(piece)
            val open = pending.lastIndexOf("<")
            val hold = if (open >= 0 && pending.indexOf(">", open) < 0 && pending.length - open < MAX_MARKER) open else pending.length
            val ready = pending.substring(0, hold)
            pending.delete(0, hold)
            return restoreAll(ready)
        }

        fun finish(): String = restoreAll(pending.toString()).also { pending.clear() }

        private companion object {
            const val MAX_MARKER = 8
        }
    }

    private fun scheduleUnload() {
        unloadJob?.cancel()
        unloadJob =
            scope.launch {
                delay(IDLE_UNLOAD_MS)
                runMutex.withLock { unload() }
            }
    }

    private fun unload() {
        val current = engine ?: return
        runCatching { current.close() }.onFailure { Logger.w(tag = TAG, throwable = it) { "Unload failed" } }
        engine = null
        loadedId = null
        activeBackend = ""
    }

    private companion object {
        const val TAG = "LiteRtEngine"
        const val SYSTEM = "system"
        const val USER = "user"
        const val ASSISTANT = "assistant"
        const val TOP_P = 0.95
        const val TOP_K = 40
        const val CHARS_PER_TOKEN = 4
        const val LOOP_TAIL = 40
        const val LOOP_WINDOW = 800
        const val LOOP_REPEATS = 3
        const val IDLE_UNLOAD_MS = 120_000L
        const val CONTEXT_SMALL = 8192
        const val CONTEXT_LARGE = 16384
    }
}
