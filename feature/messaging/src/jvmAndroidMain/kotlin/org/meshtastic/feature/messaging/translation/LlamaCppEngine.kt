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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.repository.AiFileKind
import org.meshtastic.core.repository.AiModels
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * Runs GGUF language models with llama.cpp: the app starts the official `llama-server` program on this computer (only
 * reachable from the computer itself) and talks to it over a local connection. The program decides by itself how many
 * layers of the model go to the graphics card and how many stay with the processor and the main memory, from the
 * memory that is free at that moment. If it does not start on the graphics card, it is started again on the processor
 * (and, on Windows and Linux, with the plain processor version of the program); every step is written to the log.
 * Only one model is in memory at a time and requests run one after another. The model is loaded on first use and freed
 * after a short idle time.
 */
class LlamaCppEngine(
    private val store: AiModelStoreImpl,
    private val installer: ModelServerInstaller,
    private val logDir: File,
) : AiTextEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runMutex = Mutex()
    private var unloadJob: Job? = null
    private var server: ModelServerProcess? = null
    private var loadedId: String? = null
    private var loadedHearing = false
    private val report = StringBuilder()

    @Volatile
    override var activeBackend: String = ""
        private set

    init {
        store.beforeModelDelete = { unloadNow() }
        // A program left running by an app that was killed would keep gigabytes of memory.
        ModelServerProcess.stopLeftovers(File(logDir, PID_FILE))
    }

    override fun contextTokens(modelId: String): Int =
        when (modelId) {
            AiModels.G4_E2B, AiModels.QWEN_2B -> CONTEXT_SMALL
            else -> CONTEXT_LARGE
        }

    override suspend fun chat(
        modelId: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        temperature: Float,
        onPiece: (String) -> Unit,
        jsonSchema: String?,
        thinking: Boolean,
    ): String = runMutex.withLock {
        unloadJob?.cancel()
        try {
            withContext(Dispatchers.IO) {
                val running = ensureLoaded(modelId, hearing = false)
                val body = chatBody(messages, maxTokens, temperature, jsonSchema, thinking)
                complete(running, body, maxTokens, onPiece, modelId)
            }
        } finally {
            scheduleUnload()
        }
    }

    override suspend fun transcribe(modelId: String, wav: ByteArray): String = runMutex.withLock {
        unloadJob?.cancel()
        try {
            withContext(Dispatchers.IO) {
                val running = ensureLoaded(modelId, hearing = true)
                val audio = Base64.getEncoder().encodeToString(wav)
                val body = buildJsonObject {
                    putJsonArray("messages") {
                        add(buildJsonObject {
                            put("role", "system")
                            put("content", TRANSCRIBE_RULE)
                        })
                        add(buildJsonObject {
                            put("role", "user")
                            putJsonArray("content") {
                                add(buildJsonObject {
                                    put("type", "input_audio")
                                    putJsonObject("input_audio") {
                                        put("data", audio)
                                        put("format", "wav")
                                    }
                                })
                                add(buildJsonObject {
                                    put("type", "text")
                                    put("text", TRANSCRIBE_ASK)
                                })
                            }
                        })
                    }
                    put("temperature", 0.0)
                    put("max_tokens", MAX_TRANSCRIPT_TOKENS)
                    put("stream", true)
                    putJsonObject("chat_template_kwargs") { put("enable_thinking", false) }
                }
                complete(running, body, MAX_TRANSCRIPT_TOKENS, {}, modelId).trim().trim('"', '„', '”').trim()
            }
        } finally {
            scheduleUnload()
        }
    }

    override suspend fun unloadNow() {
        runMutex.withLock { unload() }
    }

    private fun chatBody(
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        temperature: Float,
        jsonSchema: String?,
        thinking: Boolean,
    ): JsonObject {
        require(messages.isNotEmpty() && messages.last().first == USER) { "The last message must come from the user" }
        return buildJsonObject {
            putJsonArray("messages") {
                messages.forEach { (role, text) ->
                    add(buildJsonObject {
                        put("role", role)
                        put("content", text)
                    })
                }
            }
            if (maxTokens > 0) put("max_tokens", maxTokens)
            put("temperature", temperature.toDouble())
            put("top_k", TOP_K)
            put("top_p", TOP_P)
            put("stream", true)
            // Quiet thinking is switched on or off per answer; models whose template has no such switch ignore it.
            putJsonObject("chat_template_kwargs") { put("enable_thinking", thinking) }
            if (jsonSchema != null) {
                putJsonObject("response_format") {
                    put("type", "json_schema")
                    putJsonObject("json_schema") {
                        put("name", "reply")
                        put("schema", Json.parseToJsonElement(jsonSchema))
                    }
                }
            }
        }
    }

    /** Sends the request and returns the whole answer; [onPiece] gets the text as it arrives. */
    private suspend fun complete(
        running: ModelServerProcess,
        body: JsonObject,
        maxTokens: Int,
        onPiece: (String) -> Unit,
        modelId: String,
    ): String {
        val started = nowMillis
        val reply = StringBuilder()
        val limit = if (maxTokens > 0) maxTokens * CHARS_PER_TOKEN else Int.MAX_VALUE
        val connection = URL(running.baseUrl + "/v1/chat/completions").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val text = (connection.errorStream ?: connection.inputStream).bufferedReader().readText()
                error("The model program answered HTTP $code: ${text.take(MAX_ERROR)}")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { input ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val line = input.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == DONE) break
                    val piece = runCatching { deltaText(Json.parseToJsonElement(data)) }.getOrNull().orEmpty()
                    if (piece.isEmpty()) continue
                    reply.append(piece)
                    runCatching { onPiece(piece) }
                    // Enough written, or a small model stuck in a loop: stop and let the connection close cancel the rest.
                    if (reply.length >= limit || reply.isLooping()) break
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The answer so far is kept when the connection broke half-way; an empty answer is a real failure.
            if (reply.isEmpty()) {
                Logger.w(tag = TAG, throwable = e) { "Generation failed, the model is reloaded for the next answer: ${e.message}" }
                unload()
                throw e
            }
            Logger.w(tag = TAG, throwable = e) { "Answer ended early: ${e.message}" }
        } finally {
            connection.disconnect()
        }
        val elapsed = maxOf(1L, nowMillis - started)
        Logger.i(tag = TAG) {
            "Answer from $modelId on $activeBackend: ${reply.length} chars in $elapsed ms, about ${reply.length * MS_PER_SECOND / elapsed} chars/s"
        }
        return reply.withoutLoop().trim()
    }

    /** The text of one streamed piece; the model's quiet thoughts (`reasoning_content`) are not shown. */
    private fun deltaText(event: JsonElement): String {
        val choices = event.jsonObject["choices"] as? JsonArray ?: return ""
        val delta = choices.firstOrNull()?.jsonObject?.get("delta") as? JsonObject ?: return ""
        return (delta["content"] as? JsonPrimitive)?.contentOrNull.orEmpty()
    }

    private suspend fun ensureLoaded(modelId: String, hearing: Boolean): ModelServerProcess {
        // A program started with hearing can do everything a plain one can, so it is kept for text too.
        server?.let { if (it.isAlive && loadedId == modelId && (loadedHearing || !hearing)) return it }
        unload()
        val model = AiModels.byId(modelId) ?: error("Unknown model $modelId")
        val file = store.fileOf(modelId) ?: error("Model $modelId is not downloaded")
        val hearingFile = if (hearing) store.fileOf(modelId, AiFileKind.HEARING) ?: error("Model ${model.name} cannot hear") else null
        val window = contextTokens(modelId)
        val gpuProgram = installer.install(installer.llamaAsset())
        val attempts = buildList {
            add(Attempt("graphics card and processor (automatic split)", gpuProgram, emptyList()))
            add(Attempt("processor only", gpuProgram, listOf("-ngl", "0")))
            installer.llamaCpuAsset()?.let { asset -> add(Attempt("processor program", null, listOf("-ngl", "0"), asset)) }
        }
        var failure: Throwable? = null
        for (attempt in attempts) {
            val program = attempt.program ?: attempt.asset?.let { runCatching { installer.install(it) }.getOrNull() } ?: continue
            Logger.i(tag = TAG) { "Starting ${model.name}: ${attempt.name} (context $window tokens, hearing $hearing)" }
            val started = startServer(program, file, hearingFile, window, attempt.extra)
            try {
                started.awaitReady("/health", LOAD_TIMEOUT_MS)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    started.stop()
                    throw e
                }
                failure = e
                Logger.w(tag = TAG, throwable = e) { "${attempt.name} did not start: ${e.message}\n${started.logTail().takeLast(MAX_TAIL)}" }
                started.stop()
                continue
            }
            server = started
            loadedId = modelId
            loadedHearing = hearing
            activeBackend = describeBackend(attempt)
            Logger.i(tag = TAG) { "Model ${model.name} runs: $activeBackend" }
            return started
        }
        throw failure ?: IllegalStateException("The model program could not be started")
    }

    private class Attempt(
        val name: String,
        val program: File?,
        val extra: List<String>,
        val asset: ModelServerInstaller.Asset? = null,
    )

    private fun startServer(program: File, model: File, hearing: File?, window: Int, extra: List<String>): ModelServerProcess {
        val port = ModelServerProcess.freePort()
        val command = buildList {
            addAll(installer.launchPrefix(program))
            addAll(listOf("-m", model.absolutePath))
            if (hearing != null) addAll(listOf("--mmproj", hearing.absolutePath))
            addAll(listOf("--host", "127.0.0.1", "--port", port.toString()))
            addAll(listOf("-c", window.toString()))
            addAll(listOf("-np", "1", "--jinja", "--no-webui", "--cache-ram", CACHE_RAM_MB.toString()))
            addAll(extra)
        }
        synchronized(report) { report.clear() }
        return ModelServerProcess.start(command, program.parentFile, port, File(logDir, "llama-server.log"), File(logDir, PID_FILE)) { line ->
            // Keep only the lines that say where the model went (card, layers), for the status shown to the user.
            if (BACKEND_LINE.containsMatchIn(line)) synchronized(report) { if (report.length < MAX_REPORT) report.appendLine(line.trim()) }
        }
    }

    /** A short sentence for the log and the status line: which chip and how many layers. */
    private fun describeBackend(attempt: Attempt): String {
        val text = synchronized(report) { report.toString() }
        val layers = LAYERS.find(text)?.let { "${it.groupValues[1]} of ${it.groupValues[2]} layers on the graphics card" }
        val device = DEVICE.find(text)?.groupValues?.get(1)?.trim()
        return when {
            attempt.extra.contains("0") -> "CPU"
            layers != null -> "GPU" + (device?.let { " ($it)" } ?: "") + ", $layers"
            device != null -> "GPU ($device)"
            else -> "automatic"
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
        server?.stop()
        server = null
        loadedId = null
        loadedHearing = false
        activeBackend = ""
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

    private companion object {
        const val TAG = "LlamaCppEngine"
        const val PID_FILE = "llama-server.pid"
        const val USER = "user"
        const val DONE = "[DONE]"
        const val TOP_P = 0.95
        const val TOP_K = 40
        const val CHARS_PER_TOKEN = 4
        const val LOOP_TAIL = 40
        const val LOOP_WINDOW = 800
        const val LOOP_REPEATS = 3
        const val MAX_TRANSCRIPT_TOKENS = 600
        const val CONTEXT_SMALL = 8192
        const val CONTEXT_LARGE = 16384
        const val CACHE_RAM_MB = 256
        const val LOAD_TIMEOUT_MS = 10 * 60 * 1000L
        const val IDLE_UNLOAD_MS = 120_000L
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 10 * 60 * 1000
        const val MAX_ERROR = 300
        const val MAX_TAIL = 4000
        const val MAX_REPORT = 4000
        const val MS_PER_SECOND = 1000L
        val BACKEND_LINE = Regex("offloaded|ggml_vulkan|ggml_metal|ggml_cuda|using device|Found \\d+ (Vulkan|CUDA|Metal)", RegexOption.IGNORE_CASE)
        val LAYERS = Regex("offloaded (\\d+)/(\\d+) layers to GPU", RegexOption.IGNORE_CASE)
        val DEVICE = Regex("(?:ggml_vulkan: \\d+ = |using device |Device \\d+: )([^\\n(]+)", RegexOption.IGNORE_CASE)
        const val TRANSCRIBE_RULE =
            "You are a speech-to-text engine. You write down exactly what is said in the recording, in the language " +
                "that is spoken, with normal punctuation. You never answer, translate, explain or add anything."
        const val TRANSCRIBE_ASK = "Write down exactly what is said in this recording. Reply with the text only."
    }
}
