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
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private var loadedWithAudio = false
    /** When (and why) a chip last failed to start: it is skipped only for a short time, and every skip is logged. */
    private val failedBackends = mutableMapOf<String, Pair<Long, String>>()

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
     * [jsonSchema] forces the answer into that JSON shape: the library lets the model write nothing else, so the result
     * can always be read by code. [thinking] lets the model think quietly before it answers (the thoughts are not
     * shown); it makes answers better but slower.
     */
    suspend fun chat(
        modelId: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        temperature: Float,
        onPiece: (String) -> Unit = {},
        tools: List<ToolSet> = emptyList(),
        jsonSchema: String? = null,
        thinking: Boolean = false,
    ): String = runMutex.withLock {
        unloadJob?.cancel()
        try {
            withContext(Dispatchers.Default) {
                generate(modelId, messages, maxTokens, temperature, onPiece, tools, jsonSchema, thinking)
            }
        } finally {
            scheduleUnload()
        }
    }

    /**
     * Turns a recording ([wav]: 16 kHz, mono, 16 bit, at most about 30 seconds) into text with the model's own hearing,
     * fully on the device. Only the Gemma models of the E2B and E4B size can hear. The first call after the model was
     * loaded without audio reloads it with the audio part switched on.
     */
    suspend fun transcribe(modelId: String, wav: ByteArray): String = runMutex.withLock {
        unloadJob?.cancel()
        try {
            withContext(Dispatchers.Default) {
                val loaded = ensureLoaded(modelId, audio = true)
                val config =
                    ConversationConfig(
                        systemInstruction = Contents.of(TRANSCRIBE_RULE),
                        samplerConfig = SamplerConfig(topK = TOP_K, topP = TOP_P, temperature = 0.0),
                    )
                val text = StringBuilder()
                loaded.createConversation(config).use { conversation ->
                    conversation.stream(
                        send = { callback ->
                            conversation.sendMessageAsync(Contents.of(Content.AudioBytes(wav), Content.Text(TRANSCRIBE_ASK)), callback)
                        },
                        wantMore = { text.length < MAX_TRANSCRIPT_CHARS && !text.isLooping() },
                        onPiece = { piece -> text.append(piece) },
                    )
                }
                text.withoutLoop().trim().trim('"', '\u201E', '\u201D').trim()
            }
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
        jsonSchema: String?,
        thinking: Boolean,
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
                enableResponseFormat = jsonSchema != null,
            )
        val format = jsonSchema?.let { ResponseFormat.json(it) }
        val think = if (thinking) ThinkingConfig(enableThinking = true, thinkingTokenBudget = THINKING_BUDGET) else null

        val reply = StringBuilder()
        val restorer = SymbolRestorer(symbols)
        val thoughts = ThoughtFilter()
        val limit = if (maxTokens > 0) maxTokens * CHARS_PER_TOKEN else Int.MAX_VALUE
        try {
            loaded.createConversation(config).use { conversation ->
                conversation.stream(
                    send = { callback ->
                        conversation.sendMessageAsync(
                            dialogue.last().second,
                            callback,
                            thinkingConfig = think,
                            responseFormat = format,
                        )
                    },
                    wantMore = { reply.length < limit && !reply.isLooping() },
                    onPiece = { piece ->
                        if (piece.isNotEmpty()) {
                            reply.append(piece)
                            thoughts.push(restorer.push(piece)).takeIf { it.isNotEmpty() }?.let { runCatching { onPiece(it) } }
                        }
                    },
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // The model's window is full: keep what was written instead of failing the whole answer.
            if (reply.isEmpty()) {
                // A failure of the native part can leave the model's memory locked, and every next answer would fail
                // too ("tensor buffer is already locked"): the model is freed and loaded fresh for the next try.
                Logger.w(tag = TAG) { "Generation failed, the model is reloaded for the next answer: ${e.message?.lineSequence()?.firstOrNull()}" }
                unload()
                throw e
            }
            Logger.w(tag = TAG, throwable = e) { "Answer ended early: ${e.message}" }
        }
        thoughts.push(restorer.finish()).plus(thoughts.finish()).takeIf { it.isNotEmpty() }?.let { runCatching { onPiece(it) } }
        Logger.i(tag = TAG) {
            "Answer from $modelId on $activeBackend: loaded=${!wasLoaded} in ${loadedAt - started} ms, generated ${reply.length} chars " +
                "in ${nowMillis - loadedAt} ms, about ${reply.length * MS_PER_SECOND / maxOf(1L, nowMillis - loadedAt)} chars/s " +
                "(thinking=$thinking, json=${jsonSchema != null})"
        }
        val finalText = reply.withoutLoop()
        return ThoughtFilter.strip(restorer.restoreAll(finalText)).trim()
    }

    /**
     * Streams a reply and returns only when the native side has really finished. Stopping early (length limit, a loop,
     * the user cancelling) asks the library to cancel and WAITS for its end before the conversation may be closed:
     * closing it while it still decodes makes the native library write to freed memory and kills the whole app.
     */
    private suspend fun Conversation.stream(
        send: (MessageCallback) -> Unit,
        wantMore: () -> Boolean,
        onPiece: (String) -> Unit,
    ) {
        val finished = CompletableDeferred<Throwable?>()
        val stopping = AtomicBoolean(false)
        val callback =
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    if (stopping.get()) return
                    if (!wantMore()) {
                        stopping.set(true)
                        return
                    }
                    runCatching { onPiece(message.toString()) }
                }

                override fun onDone() {
                    finished.complete(null)
                }

                override fun onError(throwable: Throwable) {
                    finished.complete(throwable)
                }
            }
        send(callback)
        suspend fun waitForEnd() {
            while (!finished.isCompleted) {
                if (stopping.get()) runCatching { cancelProcess() }
                withTimeoutOrNull(CANCEL_POLL_MS) { finished.await() }
            }
        }
        try {
            waitForEnd()
        } catch (e: kotlinx.coroutines.CancellationException) {
            stopping.set(true)
            withContext(NonCancellable) { waitForEnd() }
            throw e
        }
        // After our own cancel the library may report the cancel as an error: that is not a failure.
        finished.await()?.let { if (!stopping.get()) throw it }
    }

    private fun ensureLoaded(modelId: String, audio: Boolean = false): Engine {
        // An engine started with hearing can do everything a plain one can, so it is kept for text too.
        engine?.let { if (loadedId == modelId && (loadedWithAudio || !audio)) return it }
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
        var failure: Throwable? = null
        for ((name, backend) in attempts) {
            // A chip that failed a moment ago is not tried again at once (every try costs seconds), but the skip is
            // always written to the log with the old reason, so a quiet fall-back to the processor cannot happen.
            val failedBefore = failedBackends[name]
            if (failedBefore != null && name != "CPU") {
                val ago = nowMillis - failedBefore.first
                if (ago < RETRY_AFTER_MS) {
                    Logger.w(tag = TAG) { "Backend $name skipped for $modelId: it failed ${ago / MS_PER_SECOND} s ago (${failedBefore.second})" }
                    continue
                }
            }
            Logger.i(tag = TAG) { "Trying backend $name for $modelId (context $window tokens, hearing $audio)" }
            try {
                created =
                    if (name == "GPU") startOnGpu(file, backend, cache, window, audio)
                    else start(file, backend(), cache, window, audio)
                activeBackend = name
                Logger.i(tag = TAG) { "Model $modelId runs on $name (context $window tokens)" }
                break
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                failure = e
                failedBackends[name] = nowMillis to (e.message?.lineSequence()?.firstOrNull() ?: e::class.simpleName.orEmpty())
                Logger.w(tag = TAG, throwable = e) {
                    "Backend $name not usable for $modelId: ${e.message}" + (e.cause?.let { " (cause: ${it.message})" } ?: "")
                }
                // The library's own message names no reason, so the log carries what is known about this computer.
                if (name == "GPU") Logger.w(tag = TAG) { gpuFailureReport(file, cache, window, audio, e) }
            }
        }
        val ready = created ?: throw (failure ?: IllegalStateException("No backend could start"))
        engine = ready
        loadedId = modelId
        loadedWithAudio = audio
        return ready
    }

    /** What the log gets when the graphics chip could not be used: the system, the model, the cache and the graphics cards. */
    private fun gpuFailureReport(file: File, cache: File, window: Int, audio: Boolean, error: Throwable): String =
        buildString {
            fun prop(key: String) = System.getProperty(key).orEmpty()
            val runtime = Runtime.getRuntime()
            val megabyte = 1024L * 1024L
            appendLine("GPU could not be used; system report:")
            appendLine("  System: ${prop("os.name")} ${prop("os.version")} ${prop("os.arch")}")
            appendLine("  Java: ${prop("java.version")} (${prop("java.vendor")}), ${prop("java.vm.name")}")
            appendLine(
                "  Processors: ${runtime.availableProcessors()}, memory limit ${runtime.maxMemory() / megabyte} MB, " +
                    "free ${runtime.freeMemory() / megabyte} MB",
            )
            appendLine("  Model: ${file.name}, ${file.length() / megabyte} MB, context $window tokens, hearing $audio")
            val cached = runCatching { cache.walkTopDown().filter { it.isFile }.toList() }.getOrDefault(emptyList())
            appendLine("  Cache folder: ${cached.size} files, ${cached.sumOf { it.length() } / megabyte} MB")
            var cause: Throwable? = error
            var depth = 0
            while (cause != null && depth < MAX_CAUSE_DEPTH) {
                appendLine("  Error ${depth + 1}: ${cause::class.simpleName}: ${cause.message?.lineSequence()?.firstOrNull().orEmpty()}")
                cause = cause.cause
                depth++
            }
            val cards = graphicsCards()
            appendLine("  Graphics: " + (cards ?: "not available"))
        }

    /** The graphics cards the computer reports (desktop only), or null; the question to the system gives up after a few seconds. */
    private fun graphicsCards(): String? {
        if (System.getProperty("java.vendor").orEmpty().contains("Android")) return null
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val command =
            when {
                os.contains("win") ->
                    listOf(
                        "powershell",
                        "-NoProfile",
                        "-Command",
                        // Windows keeps the card's memory in a 32-bit field that stops at 4 GB, so the real size is read from the registry.
                        "\$reg = Get-ItemProperty 'HKLM:\\SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}\\0*' -ErrorAction SilentlyContinue; " +
                            "Get-CimInstance Win32_VideoController | ForEach-Object { \$n = \$_.Name; \$m = [math]::Round(\$_.AdapterRAM / 1MB); " +
                            "\$r = \$reg | Where-Object { \$_.DriverDesc -eq \$n } | Select-Object -First 1; " +
                            "if (\$r -and \$r.'HardwareInformation.qwMemorySize') { \$m = [math]::Round([uint64]\$r.'HardwareInformation.qwMemorySize' / 1MB) }; " +
                            "\$n + ' | driver ' + \$_.DriverVersion + ' | ' + \$m + ' MB' }",
                    )
                os.contains("mac") -> listOf("system_profiler", "SPDisplaysDataType")
                else -> listOf("sh", "-c", "lspci 2>/dev/null | grep -iE 'vga|3d|display'")
            }
        return runCatching {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            if (!process.waitFor(GRAPHICS_QUERY_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return@runCatching null
            }
            process.inputStream.bufferedReader().readText().lines().map { it.trim() }.filter { it.isNotEmpty() }
                .take(MAX_GRAPHICS_LINES).joinToString("; ")
        }.getOrNull()?.ifBlank { null }
    }

    /** How many tokens (question, data and answer together) the model keeps in its window; bigger models get more. */
    fun contextTokens(modelId: String): Int =
        when (modelId) {
            AiModels.GEMMA_E2B_ID -> CONTEXT_SMALL
            else -> CONTEXT_LARGE
        }

    /**
     * The graphics chip gets more than one chance, because the same model starts on it in Google's own command-line
     * tool: first as it is, then after the cache folder is cleared (an old cache can be the reason), then with a
     * smaller context window. The library's own message names no reason, so each step is written to the log.
     */
    private fun startOnGpu(file: File, backend: () -> Backend, cache: File, window: Int, audio: Boolean): Engine {
        try {
            return start(file, backend(), cache, window, audio)
        } catch (e: Throwable) {
            Logger.w(tag = TAG) { "GPU did not start; clearing the cache folder and trying once more" }
            cache.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
        }
        try {
            return start(file, backend(), cache, window, audio)
        } catch (e: Throwable) {
            if (window <= GPU_SMALL_WINDOW) throw e
            Logger.w(tag = TAG) { "GPU did not start again; trying a context of $GPU_SMALL_WINDOW tokens" }
        }
        try {
            return start(file, backend(), cache, GPU_SMALL_WINDOW, audio)
        } catch (e: Throwable) {
            Logger.w(tag = TAG) { "GPU did not start with a small context either; trying a fresh cache folder" }
        }
        // Last chance: a new, empty cache folder (a damaged or locked one can make the graphics chip refuse to start).
        val fresh = File(cache.parentFile, "cache-" + nowMillis).apply { mkdirs() }
        try {
            return start(file, backend(), fresh, GPU_SMALL_WINDOW, audio)
        } catch (e: Throwable) {
            runCatching { fresh.deleteRecursively() }
            throw e
        }
    }

    private fun start(file: File, backend: Backend, cache: File, window: Int, audio: Boolean): Engine {
        val runtime = Runtime.getRuntime()
        val megabyte = 1024L * 1024L
        val began = nowMillis
        val heapBefore = (runtime.totalMemory() - runtime.freeMemory()) / megabyte
        Logger.i(tag = TAG) {
            "Starting engine: backend ${backend::class.simpleName}, context $window, hearing $audio, model ${file.name} " +
                "(${file.length() / megabyte} MB), heap used $heapBefore of ${runtime.maxMemory() / megabyte} MB"
        }
        val created =
            Engine(
                EngineConfig(
                    modelPath = file.absolutePath,
                    backend = backend,
                    audioBackend = if (audio) Backend.CPU() else null,
                    maxNumTokens = window,
                    cacheDir = cache.absolutePath,
                ),
            )
        try {
            created.initialize()
        } catch (e: Throwable) {
            runCatching { created.close() }
            Logger.w(tag = TAG) { "Engine did not start after ${nowMillis - began} ms: ${e.message?.lineSequence()?.firstOrNull()}" }
            throw e
        }
        Logger.i(tag = TAG) {
            "Engine ready in ${nowMillis - began} ms, heap used ${(runtime.totalMemory() - runtime.freeMemory()) / megabyte} MB"
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

    /**
     * Hides the model's quiet thinking if it ever comes inside the answer text instead of in its own channel: a block
     * between the thinking markers is dropped, and a marker split between two pieces is held back.
     */
    private class ThoughtFilter {
        private val pending = StringBuilder()
        private var inside = false

        fun push(piece: String): String {
            pending.append(piece)
            val out = StringBuilder()
            while (true) {
                if (inside) {
                    val end = pending.indexOf(END)
                    if (end < 0) {
                        val keep = minOf(pending.length, END.length - 1)
                        pending.delete(0, pending.length - keep)
                        return out.toString()
                    }
                    pending.delete(0, end + END.length)
                    inside = false
                    continue
                }
                val start = pending.indexOf(START)
                if (start >= 0) {
                    out.append(pending.substring(0, start))
                    pending.delete(0, start + START.length)
                    inside = true
                    continue
                }
                var hold = 0
                for (k in START.length - 1 downTo 1) {
                    if (pending.length >= k && pending.endsWith(START.substring(0, k))) {
                        hold = k
                        break
                    }
                }
                out.append(pending.substring(0, pending.length - hold))
                pending.delete(0, pending.length - hold)
                return out.toString()
            }
        }

        fun finish(): String {
            val rest = if (inside) "" else pending.toString()
            pending.clear()
            inside = false
            return rest
        }

        companion object {
            const val START = "<|channel>"
            const val END = "<channel|>"
            private val BLOCK = Regex("(?s)<\\|channel>.*?(<channel\\|>|$)")

            fun strip(text: String): String = text.replace(BLOCK, "")
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
        loadedWithAudio = false
        activeBackend = ""
    }

    private companion object {
        const val TAG = "LiteRtEngine"
        const val MAX_CAUSE_DEPTH = 5
        const val GRAPHICS_QUERY_SECONDS = 5L
        const val MAX_GRAPHICS_LINES = 12
        const val SYSTEM = "system"
        const val USER = "user"
        const val ASSISTANT = "assistant"
        const val TOP_P = 0.95
        const val TOP_K = 40
        const val CHARS_PER_TOKEN = 4
        const val LOOP_TAIL = 40
        const val LOOP_WINDOW = 800
        const val LOOP_REPEATS = 3
        const val MAX_TRANSCRIPT_CHARS = 2000
        const val TRANSCRIBE_RULE =
            "You are a speech-to-text engine. You write down exactly what is said in the recording, in the language " +
                "that is spoken, with normal punctuation. You never answer, translate, explain or add anything."
        const val TRANSCRIBE_ASK = "Write down exactly what is said in this recording. Reply with the text only."
        const val CANCEL_POLL_MS = 200L
        const val IDLE_UNLOAD_MS = 120_000L
        const val CONTEXT_SMALL = 8192
        const val CONTEXT_LARGE = 16384

        /** At most this many tokens of quiet thinking before an answer; the same on a phone and a computer. */
        const val THINKING_BUDGET = 1536

        /** The context a second GPU attempt uses when the full window did not start. */
        const val GPU_SMALL_WINDOW = 4096
        const val MS_PER_SECOND = 1000L

        /** A chip that failed is tried again after this long (and always after the app restarts). */
        const val RETRY_AFTER_MS = 2 * 60 * 1000L
    }
}
