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
import com.llamatik.core.platform.GenStream
import com.llamatik.core.platform.LlamaBridge
import kotlinx.coroutines.CancellationException
import org.meshtastic.core.common.util.nowMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs GGUF language models (Gemma) with llama.cpp through the Llamatik library, on Android and on desktop. Only one
 * model is in memory at a time and requests run one after another, so translation and the assistant can safely share
 * it. The model is loaded on first use and freed again after a short idle time.
 */
class LlamaEngine(private val store: AiModelStoreImpl) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runMutex = Mutex()
    private var unloadJob: Job? = null
    private var loadedId: String? = null

    init {
        store.beforeGgufDelete = { unloadNow() }
    }

    /**
     * Sends [messages] (role to text pairs, roles `system`, `user`, `assistant`) to model [modelId] and returns the
     * reply. [onPiece] receives the text as it is produced. Cancelling the coroutine stops the generation.
     */
    suspend fun chat(
        modelId: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        temperature: Float,
        onPiece: (String) -> Unit = {},
    ): String = runMutex.withLock {
        unloadJob?.cancel()
        try {
            withContext(Dispatchers.Default) { generate(modelId, messages, maxTokens, temperature, onPiece) }
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
    ): String = coroutineScope {
        val started = nowMillis
        val wasLoaded = loadedId == modelId
        ensureLoaded(modelId, maxTokens, temperature)
        val loadedAt = nowMillis
        val prompt = LlamaBridge.applyChatTemplate(messages, true) ?: fallbackPrompt(messages)
        val reply = StringBuilder()
        var failure: String? = null
        val finished = AtomicBoolean(false)
        // A blocking native call cannot be interrupted by the coroutine, so a watcher asks the library to stop.
        val watcher =
            launch {
                try {
                    awaitCancellation()
                } finally {
                    if (!finished.get()) runCatching { LlamaBridge.nativeCancelGenerate() }
                }
            }
        try {
            LlamaBridge.generateStream(
                prompt,
                object : GenStream {
                    override fun onDelta(text: String) {
                        if (text.isEmpty()) return
                        reply.append(text)
                        runCatching { onPiece(text) }
                    }

                    override fun onComplete() = Unit

                    override fun onError(message: String) {
                        failure = message
                    }
                },
            )
        } finally {
            finished.set(true)
            watcher.cancel()
        }
        Logger.i(tag = TAG) {
            "Answer from $modelId: loaded=${!wasLoaded} in ${loadedAt - started} ms, generated ${reply.length} chars " +
                "in ${nowMillis - loadedAt} ms"
        }
        failure?.let { error(it) }
        reply.toString().replace(END_OF_TURN, "").trim()
    }

    private fun ensureLoaded(modelId: String, maxTokens: Int, temperature: Float) {
        if (loadedId != modelId) {
            unload()
        }
        // Settings are given before loading so the context size takes effect.
        LlamaBridge.updateGenerateParams(
            temperature = temperature,
            maxTokens = maxTokens,
            topP = TOP_P,
            topK = TOP_K,
            repeatPenalty = REPEAT_PENALTY,
            contextLength = CONTEXT_LENGTH,
            numThreads = threadCount(),
            useMmap = true,
            flashAttention = false,
            batchSize = BATCH_SIZE,
            gpuLayers = gpuLayers(),
        )
        if (loadedId == modelId) return
        val file = store.fileOf(modelId) ?: error("Model $modelId is not downloaded")
        if (!LlamaBridge.initGenerateModel(LlamaBridge.getModelPath(file.absolutePath))) {
            error("Cannot load model $modelId")
        }
        loadedId = modelId
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
        if (loadedId == null) return
        runCatching { LlamaBridge.shutdown() }.onFailure { Logger.w(tag = TAG, throwable = it) { "Unload failed" } }
        loadedId = null
    }

    /** On a Mac the graphics chip does the heavy work (much faster); elsewhere the processor does it. */
    private fun gpuLayers(): Int =
        if (System.getProperty("os.name").orEmpty().contains("Mac", ignoreCase = true)) ALL_LAYERS else 0

    private fun threadCount(): Int = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, MAX_THREADS)

    /** Used only when the model file carries no chat template. */
    private fun fallbackPrompt(messages: List<Pair<String, String>>): String = buildString {
        val system = messages.filter { it.first == "system" }.joinToString("\n") { it.second }
        var first = true
        for ((role, content) in messages) {
            if (role == "system") continue
            append("<start_of_turn>").append(if (role == "assistant") "model" else "user").append('\n')
            if (first && role == "user" && system.isNotBlank()) append(system).append("\n\n")
            first = false
            append(content).append("<end_of_turn>\n")
        }
        append("<start_of_turn>model\n")
    }

    private companion object {
        const val TAG = "LlamaEngine"
        const val END_OF_TURN = "<end_of_turn>"
        const val CONTEXT_LENGTH = 4096
        const val BATCH_SIZE = 512
        const val TOP_P = 0.95f
        const val TOP_K = 40
        const val REPEAT_PENALTY = 1.0f
        const val MAX_THREADS = 6
        const val ALL_LAYERS = 99
        const val IDLE_UNLOAD_MS = 120_000L
    }
}
