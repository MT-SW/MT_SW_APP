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
 * Only one model is in memory at a time and requests run one after another. The model is loaded on first use (or as soon
 * as it is chosen) and stays in memory while it is used; it is freed when another model is chosen, its function is
 * switched off, the user frees the memory in the settings, or the app ends.
 */
class LlamaCppEngine(
    private val store: AiModelStoreImpl,
    private val installer: ModelServerInstaller,
    private val logDir: File,
) : AiTextEngine {

    private val runMutex = Mutex()
    private var server: ModelServerProcess? = null
    private var loadedId: String? = null
    private var loadedHearing = false

    /** The window of text of the program that runs now (smaller than normal on the Intel NPU); 0 = none. */
    @Volatile private var loadedWindow = 0
    private val report = StringBuilder()

    @Volatile
    override var activeBackend: String = ""
        private set

    @Volatile
    override var startReport: String = ""
        private set

    override val deviceSummary: String get() = runCatching { installer.deviceSummary() }.getOrDefault("")

    override val memoryUsedMb: Int? get() = server?.takeIf { it.isAlive }?.memoryMb()

    /** The last successful start, kept on disk so `/sprzet` can still tell where the model ran after it was released or the app restarted. */
    override fun lastStart(modelId: String): LastStart? = runCatching {
        val text = File(logDir, LAST_START_FILE).takeIf { it.isFile }?.readText() ?: return null
        val head = text.split("\n", limit = 3)
        if (head.size < 3 || head[0] != modelId) return null
        val parts = head[2].split(LAST_START_SEPARATOR, limit = 2)
        LastStart(head[1], parts[0].trim(), parts.getOrElse(1) { "" }.trim())
    }.getOrNull()

    @Volatile
    override var activeResources: String = ""
        private set

    /** What the model that just started takes on each chip, read from the program's own log lines. */
    private fun resourceSummary(): String {
        val text = synchronized(report) { report.toString() }
        fun chip(name: String): String =
            when {
                name.startsWith("HTP", ignoreCase = true) -> note("NPU Hexagon", "NPU Hexagon") + " ($name)"
                name.contains("CPU", ignoreCase = true) -> note("processor (RAM)", "procesor (pamięć RAM)")
                name.startsWith("Vulkan", ignoreCase = true) -> note("graphics card", "karta graficzna") + " ($name)"
                name.contains("OpenCL", ignoreCase = true) -> note("graphics card (OpenCL)", "karta graficzna (OpenCL)")
                name.contains("OpenVINO", ignoreCase = true) || name.startsWith("NPU", ignoreCase = true) -> note("NPU (Intel)", "NPU (Intel)")
                name.startsWith("CUDA", ignoreCase = true) -> note("graphics card (NVIDIA)", "karta graficzna (NVIDIA)")
                name.startsWith("Metal", ignoreCase = true) || name.startsWith("MTL", ignoreCase = true) -> note("graphics chip of the Mac", "układ graficzny Maca")
                else -> name
            }
        fun sums(regex: Regex): Map<String, Double> =
            regex.findAll(text).groupBy({ chip(it.groupValues[1]) }, { it.groupValues[2].toDoubleOrNull() ?: 0.0 }).mapValues { it.value.sum() }
        fun list(map: Map<String, Double>) = map.entries.joinToString("; ") { "${it.key}: ${it.value.toInt()} MB" }
        val model = sums(MODEL_BUFFER)
        val kv = sums(KV_BUFFER)
        val compute = sums(COMPUTE_BUFFER)
        val layers = LAYERS.find(text)?.let { "${it.groupValues[1]} " + note("of", "z") + " ${it.groupValues[2]}" }
        val devices = USING_DEVICE.findAll(text).joinToString("; ") { "${it.groupValues[1]} (${it.groupValues[2].trim()})" }
        return buildString {
            if (model.isNotEmpty()) appendLine(note("Model sits in", "Model leży w") + ": " + list(model))
            if (layers != null) appendLine(note("Layers on the graphics chip or NPU", "Warstwy na karcie graficznej lub NPU") + ": $layers")
            if (kv.isNotEmpty()) appendLine(note("Chat memory (KV)", "Pamięć rozmowy (KV)") + ": " + list(kv))
            if (compute.isNotEmpty()) appendLine(note("Work buffers", "Bufory obliczeń") + ": " + list(compute))
            if (devices.isNotEmpty()) appendLine(note("Chips the engine uses", "Układy użyte przez silnik") + ": $devices")
            append(note("Processor cores of the device", "Rdzenie procesora urządzenia") + ": ${Runtime.getRuntime().availableProcessors()}")
        }
    }

    /** Set from a hidden setting: skip every NPU and go straight to the graphics card (to test that path). */
    @Volatile
    var skipNpu: Boolean = false

    /**
     * The model chosen for the assistant (set by the router). The Intel NPU needs a small window of text (about 1000
     * tokens), which is enough for translating a message but too little for the assistant, so that model skips it.
     */
    @Volatile
    var assistantModelId: String = ""

    /** The notes about the last start are written in Polish on a Polish system, otherwise in English. */
    private fun note(en: String, pl: String): String = if (java.util.Locale.getDefault().language == "pl") pl else en

    /** The name of a way to start the model, in Polish on a Polish system (it is shown in the `/sprzet` report). */
    private fun nameFor(name: String): String =
        if (java.util.Locale.getDefault().language != "pl") name
        else name.replace("processor only", "tylko procesor").replace("processor program", "program na procesor")
            .replace("graphics card full and processor", "karta graficzna pełna i procesor").replace("(automatic split)", "(automatyczny podział)")
            .replace(", and processor", " i procesor").replace(" and processor", " i procesor").replace("graphics card", "karta graficzna").replace("window ", "okno ")

    /** A short reason taken from the program's log, for the report (the whole log stays in the log file). */
    private fun reasonFrom(log: String): String {
        val text = log.takeLast(REPORT_TAIL * 4)
        return when {
            NPU_UNSUPPORTED.containsMatchIn(text) ->
                note(
                    "the Intel NPU driver does not support a setting that this engine needs; update the driver of Intel(R) AI Boost",
                    "sterownik NPU Intela nie obsługuje ustawienia, którego potrzebuje silnik; zaktualizuj sterownik Intel(R) AI Boost",
                )
            text.contains("attention_mask", ignoreCase = true) || text.contains("Attention mask", ignoreCase = true) ->
                note("the chip cannot handle the attention window of this model", "układ nie radzi sobie z oknem uwagi tego modelu")
            Regex("out of memory|failed to allocate|cannot allocate|OOM", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                note("not enough memory on the chip", "za mało pamięci w układzie")
            text.contains("device lost", ignoreCase = true) || text.contains("VK_ERROR", ignoreCase = true) ->
                note("the graphics driver reported an error", "sterownik karty graficznej zgłosił błąd")
            else -> note("the chip did not work with this model", "układ nie zadziałał z tym modelem")
        }
    }

    /** Forgets the remembered failed attempts (NPU, graphics card), so the next start tries them again. */
    fun clearFailedAttempts() {
        installer.clearBrokenMarks()
        startReport = ""
    }

    init {
        store.beforeModelDelete = { unloadNow() }
        store.unloadAll = { unloadNow() }
        // A program left running by an app that was killed would keep gigabytes of memory.
        ModelServerProcess.stopLeftovers(File(logDir, PID_FILE))
        // The NPU driver's version is read once in the background (it takes a moment), not while a screen waits for it.
        if (installer.intelNpu) Thread({ installer.intelNpuDriver }, "intel-npu-driver").apply { isDaemon = true }.start()
    }

    override fun contextTokens(modelId: String): Int {
        val normal =
            when (modelId) {
                AiModels.G4_E2B, AiModels.QWEN_2B, AiModels.QWEN_08B, AiModels.BIELIK_15B -> CONTEXT_SMALL
                else -> CONTEXT_LARGE
            }
        // What runs now keeps its own window; before the start, the Intel NPU (tried first) allows a smaller one.
        if (loadedId == modelId && loadedWindow > 0) return loadedWindow
        return intelNpuWindow(modelId)?.let { minOf(normal, it) } ?: normal
    }

    /** Would a plain start (no listening) of [modelId] try the Intel NPU first? */
    fun npuFirst(modelId: String): Boolean = intelNpuTriedFirst(modelId)

    /** True when a start of [modelId] would try the Intel NPU with a big window first. */
    private fun intelNpuTriedFirst(modelId: String): Boolean = intelNpuWindow(modelId) != null

    /** The biggest window of text the Intel NPU is still tried with for [modelId]; null = the NPU is not tried first. */
    private fun intelNpuWindow(modelId: String): Int? {
        val model = AiModels.byId(modelId) ?: return null
        if (installer.android || installer.mac || skipNpu || !installer.intelNpu || !model.intelNpu) return null
        val asset = installer.intelNpuAsset() ?: return null
        return when {
            !installer.isBroken(asset, modelId + BIG_NPU_SUFFIX) -> INTEL_NPU_BIG_WINDOW
            !installer.isBroken(asset, modelId + MID_NPU_SUFFIX) -> INTEL_NPU_MID_WINDOW
            else -> null
        }
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
        run {
            withContext(Dispatchers.IO) {
                val running = ensureLoaded(modelId, hearing = false)
                val body = chatBody(messages, maxTokens, temperature, jsonSchema, thinking)
                complete(running, body, maxTokens, onPiece, modelId)
            }
        }
    }

    override suspend fun transcribe(modelId: String, wav: ByteArray): String = runMutex.withLock {
        run {
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
        }
    }

    override suspend fun unloadNow() {
        runMutex.withLock { unload() }
    }

    /** Loads [modelId] now (a model was just chosen), so the first question does not wait; failures are only logged. */
    suspend fun preload(modelId: String, hearing: Boolean) {
        if (store.fileOf(modelId) == null) return
        runMutex.withLock {
            try {
                withContext(Dispatchers.IO) { ensureLoaded(modelId, hearing) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(tag = TAG, throwable = e) { "Loading $modelId in advance failed: ${e.message}" }
            }
        }
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
            if (jsonSchema == null) put("repeat_penalty", REPEAT_PENALTY)
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
        var thought = 0
        // True when the answer stopped for length (or in a loop), not because the model finished.
        var cutShort = false
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
                    val event = runCatching { Json.parseToJsonElement(data) }.getOrNull() ?: continue
                    // Quiet thinking that goes on and on (small models) stops here; the assistant then asks once more
                    // without thinking instead of waiting minutes for an answer.
                    thought += thinkingLength(event)
                    if (reply.isEmpty() && thought > MAX_THINKING_CHARS) throw ThinkingTooLongException()
                    if (finishedForLength(event)) cutShort = true
                    val piece = runCatching { deltaText(event) }.getOrNull().orEmpty()
                    if (piece.isEmpty()) continue
                    reply.append(piece)
                    runCatching { onPiece(piece) }
                    // Enough written, or a small model stuck in a loop: stop and let the connection close cancel the rest.
                    if (reply.length >= limit || reply.isLooping()) {
                        cutShort = true
                        break
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ThinkingTooLongException) {
            Logger.w(tag = TAG) { "Quiet thinking of $modelId went over $MAX_THINKING_CHARS characters, stopped" }
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
        val whole = reply.withoutLoop().trim()
        // An answer that ran out of length ends at its last whole sentence instead of in the middle of a word.
        return if (cutShort) whole.toLastSentence() else whole
    }

    /** True when this streamed piece says the model stopped because it reached the length limit. */
    private fun finishedForLength(event: JsonElement): Boolean {
        val choices = (event as? JsonObject)?.get("choices") as? JsonArray ?: return false
        val reason = ((choices.firstOrNull() as? JsonObject)?.get("finish_reason") as? JsonPrimitive)?.contentOrNull
        return reason == "length"
    }

    /**
     * Cuts a text that stopped half-way back to its last whole sentence or line, when that keeps most of it; a text
     * with no sentence end in its second half is kept as it is, with an ellipsis.
     */
    private fun String.toLastSentence(): String {
        if (isEmpty() || last() in ".!?)…") return this
        val end = indexOfLast { it in ".!?\n" }
        return if (end >= length * KEEP_SHARE) substring(0, end + 1).trimEnd() else "$this…"
    }
    /** The length of the quiet thoughts (`reasoning_content`) in one streamed piece. */
    private fun thinkingLength(event: JsonElement): Int {
        val choices = (event as? JsonObject)?.get("choices") as? JsonArray ?: return 0
        val delta = (choices.firstOrNull() as? JsonObject)?.get("delta") as? JsonObject ?: return 0
        return (delta["reasoning_content"] as? JsonPrimitive)?.contentOrNull?.length ?: 0
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
        val mainProgram = installer.installLlama()
        val attempts = buildList {
            // A phone with a Snapdragon: the NPU (Hexagon) first. "-ngl 99" is required: without it the NPU reports no
            // memory to the automatic split and the whole model stays on the processor. Its memory is split into sessions of about 2 GB, so a
            // bigger model gets more sessions (layers spread over them).
            installer.androidNpuAsset()?.takeIf { !skipNpu && !installer.isBroken(it, modelId) }?.let { asset ->
                val sessions = ((model.sizeMb + NPU_SESSION_MB - 1) / NPU_SESSION_MB).coerceIn(1, MAX_NPU_SESSIONS)
                val devices = (0 until sessions).joinToString(",") { "HTP$it" }
                add(
                    Attempt(
                        "NPU (Hexagon, $sessions × 2 GB) and processor", null, listOf("--device", devices, "-ngl", "99"), asset,
                        requireGpu = true, npu = "Hexagon", perModel = true, vendorLibs = true,
                        env = mapOf("GGML_HEXAGON_DEVICES" to sessions.toString()), dspLibs = true,
                    ),
                )
            }
            // A phone: our graphics card builds (OpenCL on a Snapdragon, then Vulkan), each only counts when the log
            // shows that the card really took part of the model.
            for (asset in installer.androidGpuAssetsAll()) {
                val kind = asset.folder.substringAfterLast('-')
                if (!installer.isBroken(asset, modelId)) add(Attempt("graphics card ($kind) and processor", null, emptyList(), asset, requireGpu = true, perModel = true, vendorLibs = true))
                // Vulkan drivers of some phones (Adreno) fail to build the fast shaders: the same card once more with the
                // newer driver features switched off (own mark, so a failure here does not hide the normal way).
                if (kind == "vulkan" && !installer.isBroken(asset, modelId + SAFE_SUFFIX)) {
                    add(
                        Attempt(
                            "graphics card (vulkan, safe mode) and processor", null, emptyList(), asset, requireGpu = true, perModel = true,
                            vendorLibs = true, markSuffix = SAFE_SUFFIX,
                            env = mapOf(
                                "GGML_VK_DISABLE_F16" to "1", "GGML_VK_DISABLE_COOPMAT" to "1",
                                "GGML_VK_DISABLE_INTEGER_DOT_PRODUCT" to "1", "GGML_VK_DISABLE_FUSION" to "1",
                            ),
                        ),
                    )
                }
            }
            if (installer.android) {
                add(Attempt("processor only", mainProgram, listOf("-ngl", "0")))
            } else {
                // A computer with an Intel Core Ultra: its NPU through OpenVINO. Models that the NPU cannot run (Qwen 3.5,
                // the biggest ones, listening) skip it; the first compilation is kept on disk for the next starts.
                // The bigger window first (the assistant can use it); the sliding-window layers keep a full-size cache so
                // that the attention mask fits. A failure has its own mark, so the small window is still tried.
                installer.intelNpuAsset()?.takeIf { !skipNpu && model.intelNpu && !hearing && !installer.isBroken(it, modelId + BIG_NPU_SUFFIX) }?.let { asset ->
                    add(
                        Attempt(
                            "NPU (Intel), window $INTEL_NPU_BIG_WINDOW, and processor", null, listOf("--swa-full"), asset, requireGpu = true, npu = "Intel",
                            perModel = true, markSuffix = BIG_NPU_SUFFIX, window = INTEL_NPU_BIG_WINDOW,
                            env = mapOf("GGML_OPENVINO_DEVICE" to "NPU"),
                        ),
                    )
                }
                // The middle window: the assistant's instructions are about 2200 tokens, so 4096 is the smallest that suits it.
                installer.intelNpuAsset()?.takeIf { !skipNpu && model.intelNpu && !hearing && !installer.isBroken(it, modelId + MID_NPU_SUFFIX) }?.let { asset ->
                    add(
                        Attempt(
                            "NPU (Intel), window $INTEL_NPU_MID_WINDOW, and processor", null, listOf("--swa-full"), asset, requireGpu = true, npu = "Intel",
                            perModel = true, markSuffix = MID_NPU_SUFFIX, window = INTEL_NPU_MID_WINDOW,
                            env = mapOf("GGML_OPENVINO_DEVICE" to "NPU"),
                        ),
                    )
                }
                // The small window (512 to 1024 tokens is what the OpenVINO notes advise for the NPU): enough for
                // translating a message, too little for the assistant, so the assistant's model skips this one.
                installer.intelNpuAsset()?.takeIf { !skipNpu && model.intelNpu && !hearing && modelId != assistantModelId && !installer.isBroken(it, modelId) }?.let { asset ->
                    add(
                        Attempt(
                            "NPU (Intel), window $INTEL_NPU_WINDOW, and processor", null, emptyList(), asset, requireGpu = true, npu = "Intel", perModel = true,
                            window = INTEL_NPU_WINDOW,
                            env = mapOf("GGML_OPENVINO_DEVICE" to "NPU"),
                        ),
                    )
                }
                // A Windows computer with a Snapdragon X: the Adreno graphics card through OpenCL.
                installer.windowsArmGpuAsset()?.takeIf { !installer.isBroken(it) }?.let { asset ->
                    add(Attempt("graphics card (OpenCL Adreno) and processor", null, emptyList(), asset, requireGpu = true))
                }
                // The graphics card filled as much as possible (only a small safety margin left free), the rest of the
                // model in the main memory. A Mac shares its memory with the system, so it keeps the normal margin.
                if (!installer.mac) {
                    add(Attempt("graphics card full and processor", mainProgram, listOf("--fit-target", TIGHT_GPU_MARGIN_MB.toString())))
                }
                add(Attempt("graphics card and processor (automatic split)", mainProgram, emptyList()))
                add(Attempt("processor only", mainProgram, listOf("-ngl", "0")))
                installer.llamaCpuAsset()?.let { asset -> add(Attempt("processor program", null, listOf("-ngl", "0"), asset)) }
            }
        }
        var failure: Throwable? = null
        val tried = StringBuilder()
        if (skipNpu) tried.append("- " + note("NPU: skipped on request, the graphics card is tried first", "NPU: pominięte na prośbę, najpierw karta graficzna") + "\n")
        if (installer.android) {
            if (installer.qualcomm) tried.append("- " + note("Snapdragon phone detected", "wykryto telefon ze Snapdragonem") + "\n") else tried.append("- " + note("not a Snapdragon phone: no NPU program", "to nie Snapdragon: brak programu na NPU") + "\n")
            installer.androidNpuAsset()?.takeIf { !skipNpu && installer.isBroken(it, modelId) }
                ?.let { tried.append("- " + note("NPU (Hexagon): skipped, failed recently for this model", "NPU (Hexagon): pominięte, niedawno nie zadziałało z tym modelem") + "\n") }
            if (installer.androidGpuAssets(modelId).isEmpty()) tried.append("- " + note("graphics card programs: skipped, failed recently", "programy na kartę graficzną: pominięte, niedawno nie zadziałały") + "\n")
        } else if (!installer.mac) {
            tried.append(
                when {
                    skipNpu -> ""
                    !installer.intelNpu -> "- " + note("NPU (Intel): not used, no Intel Core Ultra processor found on this computer", "NPU (Intel): nieużywane, na tym komputerze nie ma procesora Intel Core Ultra") + "\n"
                    hearing -> "- " + note("NPU (Intel): skipped, this model was started with listening (dictation on the same model), and the NPU does not listen. Choose Whisper for dictation to let the assistant use the NPU", "NPU (Intel): pominięte, ten model uruchomiono ze słuchaniem (dyktowanie na tym samym modelu), a NPU nie słucha. Wybierz do dyktowania Whisper, a asystent będzie mógł używać NPU") + "\n"
                    !model.intelNpu -> "- " + note("NPU (Intel): skipped, this model does not run on the Intel NPU", "NPU (Intel): pominięte, ten model nie działa na NPU Intela") + "\n"
                    modelId == assistantModelId && installer.intelNpuAsset()?.let { installer.isBroken(it, modelId + BIG_NPU_SUFFIX) && installer.isBroken(it, modelId + MID_NPU_SUFFIX) } == true -> "- " + note("NPU (Intel): skipped, this model is the assistant and needs a bigger window of text than the NPU allows", "NPU (Intel): pominięte, ten model jest asystentem i potrzebuje większego okna tekstu, niż pozwala NPU") + "\n"
                    installer.intelNpuAsset()?.let { installer.isBroken(it, modelId) } == true -> "- " + note("NPU (Intel): skipped, failed recently for this model", "NPU (Intel): pominięte, niedawno nie zadziałało z tym modelem") + "\n"
                    else -> ""
                },
            )
        }
        startReport = ""
        // Set when the driver of the Intel NPU said it cannot run this engine's settings: the other NPU windows would fail the same way.
        var intelNpuRejected = false
        for (attempt in attempts) {
            if (intelNpuRejected && attempt.npu == "Intel") continue
            // A way that an earlier attempt of this same start already showed cannot work (e.g. the safe Vulkan mode on a phone without Vulkan 1.2).
            if (attempt.requireGpu && attempt.asset?.let { installer.isBroken(it, if (attempt.perModel) modelId + attempt.markSuffix else null) } == true) continue
            val program =
                attempt.program
                    ?: attempt.asset?.let { if (attempt.requireGpu) installer.installGpu(it) else runCatching { installer.install(it) }.getOrNull() }
            if (program == null) {
                tried.append("- ${nameFor(attempt.name)}: " + note("the program could not be downloaded", "nie udało się pobrać programu") + " (${installer.lastInstallError.take(REPORT_TAIL)})\n")
                startReport = tried.toString()
                continue
            }
            val attemptWindow = attempt.window ?: window
            Logger.i(tag = TAG) { "Starting ${model.name}: ${attempt.name} (context $attemptWindow tokens, hearing $hearing)" }
            val noThinking = if (model.quietThinking) emptyList() else listOf("--reasoning-budget", "0")
            val started = startServer(program, file, hearingFile, attemptWindow, attempt.extra + noThinking, attempt)
            // A chip that fails is not tried again for a while (an NPU only for this model: another may fit).
            fun giveUp() {
                if (attempt.requireGpu) attempt.asset?.let { installer.markBroken(it, if (attempt.perModel) modelId + attempt.markSuffix else null) }
            }
            try {
                started.awaitReady(
                    "/health", if (attempt.requireGpu) CHIP_LOAD_TIMEOUT_MS else LOAD_TIMEOUT_MS,
                    abortIf =
                    if (attempt.requireGpu) {
                        {
                            val log = started.logTail()
                            when {
                                log.contains(NO_VULKAN) -> NO_VULKAN_MESSAGE
                                // The OpenVINO program prints its error at once; waiting for the load timeout would only lose minutes.
                                attempt.npu == "Intel" -> NPU_ERROR.find(log)?.value?.trim()?.take(NPU_ERROR_SHOWN)
                                else -> null
                            }
                        }
                    } else {
                        null
                    },
                )
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    started.stop()
                    throw e
                }
                failure = e
                val failLog = started.logTail() + " " + e.message.orEmpty()
                if (attempt.npu == "Intel" && NPU_UNSUPPORTED.containsMatchIn(failLog)) {
                    intelNpuRejected = true
                    attempt.asset?.let { asset -> listOf("", BIG_NPU_SUFFIX, MID_NPU_SUFFIX).forEach { installer.markBroken(asset, modelId + it) } }
                }
                tried.append("- ${nameFor(attempt.name)}: " + note("failed", "nie uruchomiło się") + ": ${reasonFrom(failLog)}" + (if (attempt.npu == "Intel") " [${e.message.orEmpty().take(NPU_ERROR_SHOWN)}]" else "") + "\n")
                startReport = tried.toString()
                Logger.w(tag = TAG, throwable = e) { "${attempt.name} did not start: ${e.message}\n${started.logTail().takeLast(MAX_TAIL)}" }
                started.stop()
                giveUp()
                // No Vulkan 1.2 on this phone: the safe Vulkan mode would fail the same way, so it is skipped too.
                if (e.message == NO_VULKAN_MESSAGE) attempt.asset?.let { installer.markBroken(it, modelId + SAFE_SUFFIX) }
                continue
            }
            if (attempt.requireGpu && !gpuTookPart()) {
                // The program started, but the chip did nothing (no driver, no support): not worth keeping here.
                tried.append("- ${nameFor(attempt.name)}: " + note("started, but the chip was not used", "uruchomiło się, ale układ nie został użyty") + ": ${reasonFrom(started.logTail())}\n")
                startReport = tried.toString()
                Logger.w(tag = TAG) { "${attempt.name}: the chip was not used\n${started.logTail().takeLast(MAX_TAIL)}" }
                giveUp()
                started.stop()
                continue
            }
            if (attempt.requireGpu && !answersSanely(started, modelId)) {
                // A chip can load the model and still crash while answering or write nonsense ("##", seen with OpenCL on
                // an Adreno): this chip is not used for this model.
                tried.append("- ${nameFor(attempt.name)}: " + note("started, but the test question was not answered properly, so this chip is not used for this model", "uruchomiło się, ale pytanie testowe nie dostało sensownej odpowiedzi, więc ten układ nie jest używany z tym modelem") + "\n")
                startReport = tried.toString()
                Logger.w(tag = TAG) { "${attempt.name}: a test question was answered with nonsense" }
                giveUp()
                started.stop()
                continue
            }
            server = started
            loadedId = modelId
            loadedHearing = hearing
            loadedWindow = attemptWindow
            activeBackend = describeBackend(attempt)
            tried.append("- ${nameFor(attempt.name)}: " + note("works", "działa") + "\n")
            startReport = tried.toString()
            store.reportLoaded(TAG, modelId)
            Logger.i(tag = TAG) { "Model ${model.name} runs: $activeBackend" }
            activeResources = resourceSummary()
            runCatching { File(logDir, LAST_START_FILE).writeText("$modelId\n$activeBackend\n$activeResources\n$LAST_START_SEPARATOR\n$startReport") }
            return started
        }
        throw failure ?: IllegalStateException("The model program could not be started")
    }

    /**
     * One way to start the model. [requireGpu]: kept only when the log shows that the chip ([npu] names an NPU) really
     * took part. [perModel]: a failure skips this way only for this model. [vendorLibs]: the phone's own chip drivers
     * are needed. [dspLibs]: the NPU programs are found next to the engine. [env]: settings of the chip's driver.
     */
    private class Attempt(
        val name: String,
        val program: File?,
        val extra: List<String>,
        val asset: ModelServerInstaller.Asset? = null,
        val requireGpu: Boolean = false,
        val npu: String? = null,
        val perModel: Boolean = false,
        val vendorLibs: Boolean = false,
        val dspLibs: Boolean = false,
        val env: Map<String, String> = emptyMap(),
        val markSuffix: String = "",
        /** A smaller window of text for this way (the Intel NPU works with fixed sizes). */
        val window: Int? = null,
    )

    /** Asks one tiny question and checks that the answer makes sense (an NPU can run a model and still write garbage). */
    private suspend fun answersSanely(running: ModelServerProcess, modelId: String): Boolean {
        val body = chatBody(listOf(USER to "What is 2 + 2? Answer with one word."), 24, 0f, null, false)
        val text = runCatching { complete(running, body, 24, {}, modelId) }.getOrDefault("").lowercase()
        Logger.i(tag = TAG) { "Test question on the chip: \"${text.take(60)}\"" }
        return text.contains("4") || text.contains("four") || text.contains("cztery")
    }

    /** True when the log of the program that just started says that layers of the model went to the graphics card. */
    private fun gpuTookPart(): Boolean {
        val text = synchronized(report) { report.toString() }
        val layers = LAYERS.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        // An NPU may hold the model without "offloaded ... layers" in the log: its own model buffer counts too.
        // The engine does not always print "offloaded ... layers" (newer builds keep it quiet), so a layer placed on a
        // chip, or a chip's own lines in the log (NPU sessions found), count as well.
        return layers > 0 || NPU_BUFFER.containsMatchIn(text) || CHIP_LAYER.containsMatchIn(text) || NPU_DEVICE.containsMatchIn(text)
    }

    private fun startServer(program: File, model: File, hearing: File?, window: Int, extra: List<String>, attempt: Attempt): ModelServerProcess {
        val port = ModelServerProcess.freePort()
        val command = buildList {
            addAll(installer.launchPrefix(program))
            addAll(listOf("-m", model.absolutePath))
            if (hearing != null) addAll(listOf("--mmproj", hearing.absolutePath))
            addAll(listOf("--host", "127.0.0.1", "--port", port.toString()))
            addAll(listOf("-c", window.toString()))
            // Each question asks the model twice (what is asked, then the answer) with two different long instructions; a
            // big enough prompt cache keeps both read, so neither is read again from the start for every question.
            val cacheMb = if (installer.android) CACHE_RAM_PHONE_MB else CACHE_RAM_COMPUTER_MB
            addAll(listOf("-np", "1", "--jinja", "--no-webui", "--cache-ram", cacheMb.toString()))
            addAll(extra)
        }
        synchronized(report) { report.clear() }
        // The engine's libraries lie next to the program or in a "lib" folder beside it; the phone's chip drivers are in
        // its vendor folders (searched last, so they never replace the engine's own files).
        val programDir = program.absoluteFile.parentFile ?: program
        val libDirs = buildList {
            File(programDir.parentFile, "lib").takeIf { it.isDirectory }?.let { add(it.absolutePath) }
            if (attempt.vendorLibs) addAll(VENDOR_LIB_DIRS.filter { File(it).isDirectory })
        }
        val env = buildMap<String, String> {
            putAll(attempt.env)
            // The NPU loads its own small programs from these folders (the engine's first, then the phone's).
            if (attempt.dspLibs) put("ADSP_LIBRARY_PATH", (libDirs.take(1) + DSP_DIRS).joinToString(";"))
        }
        return ModelServerProcess.start(command, programDir, port, File(logDir, "llama-server.log"), File(logDir, PID_FILE), env, libDirs) { line ->
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
            attempt.npu != null -> "NPU (${attempt.npu})" + (layers?.let { ", " + it.replace("graphics card", "NPU") } ?: "")
            layers != null -> "GPU" + (device?.let { " ($it)" } ?: "") + ", $layers"
            device != null -> "GPU ($device)"
            else -> "automatic"
        }
    }


    private fun unload() {
        server?.stop()
        server = null
        loadedId = null
        loadedHearing = false
        loadedWindow = 0
        activeBackend = ""
        activeResources = ""
        store.reportLoaded(TAG, null)
    }

    /** True when the end of the text repeats itself over and over (a small model stuck in a loop). */
    private fun CharSequence.isLooping(): Boolean {
        // The same word over and over ("w zasięgu zasięgu zasięgu zasięgu").
        if (length > REPEAT_WORD_MIN && SAME_WORD_REPEATED.containsMatchIn(takeLast(REPEAT_WORD_TAIL))) return true
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
        val text = toString().replace(SAME_WORD_REPEATED, "$1")
        if (!isLooping()) return text
        val tail = text.takeLast(LOOP_TAIL)
        val first = text.indexOf(tail)
        return if (first >= 0) text.substring(0, first + LOOP_TAIL).trimEnd() else text
    }

    private companion object {
        const val TAG = "LlamaCppEngine"
        const val PID_FILE = "llama-server.pid"
        const val LAST_START_FILE = "last-start.txt"
        const val LAST_START_SEPARATOR = "\n=====\n"
        const val USER = "user"
        const val DONE = "[DONE]"
        const val TOP_P = 0.95
        const val TOP_K = 40
        const val CHARS_PER_TOKEN = 4
        const val KEEP_SHARE = 0.5
        const val REPEAT_WORD_MIN = 30
        const val REPEAT_WORD_TAIL = 80
        const val REPEAT_PENALTY = 1.08
        val SAME_WORD_REPEATED = Regex("(?<![\\p{L}])(\\p{L}{3,})(\\s+\\1(?![\\p{L}])){3,}\\s*$")
        const val LOOP_TAIL = 40
        const val LOOP_WINDOW = 800
        const val LOOP_REPEATS = 3
        const val MAX_TRANSCRIPT_TOKENS = 600
        const val CONTEXT_SMALL = 8192
        const val CONTEXT_LARGE = 16384
        const val CACHE_RAM_PHONE_MB = 256
        const val CACHE_RAM_COMPUTER_MB = 1536
        const val LOAD_TIMEOUT_MS = 10 * 60 * 1000L
        // A chip (NPU, graphics card) that has not answered in this time is dropped, so a hanging one cannot block the start.
        /** Window of text on the Intel NPU: the notes of the OpenVINO engine say 512 to 1024; it also fits the model's sliding window. */
        const val INTEL_NPU_WINDOW = 1024

        /** The bigger window tried first on the Intel NPU, and the mark that remembers a failure of that try. */
        const val INTEL_NPU_BIG_WINDOW = 8192
        const val BIG_NPU_SUFFIX = "-big"

        /** The middle window tried next on the Intel NPU (the assistant's instructions are about 2200 tokens), and the mark of its failure. */
        const val INTEL_NPU_MID_WINDOW = 4096
        const val MID_NPU_SUFFIX = "-mid"
        const val NPU_ERROR_SHOWN = 200

        /** Lines with which the OpenVINO program reports an error that stops the start. */
        val NPU_ERROR = Regex("^.*(Exception from src|is not supported for current configuration|ZE_RESULT_ERROR|OpenVINO exception|terminate called after).*$", RegexOption.MULTILINE)

        /** The driver of the NPU does not know a setting the engine sets (seen on Meteor Lake). */
        val NPU_UNSUPPORTED = Regex("NPU_COMPILER_DYNAMIC_QUANTIZATION|is not supported for current configuration", RegexOption.IGNORE_CASE)
        const val CHIP_LOAD_TIMEOUT_MS = 4 * 60 * 1000L
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 10 * 60 * 1000
        const val MAX_ERROR = 300
        const val MAX_TAIL = 4000
        const val REPORT_TAIL = 500
        const val MAX_REPORT = 4000
        /** About 600 tokens of quiet thinking: past that the model is stopped and asked again without thinking. */
        const val MAX_THINKING_CHARS = 2400
        const val MS_PER_SECOND = 1000L
        val BACKEND_LINE = Regex("offloaded|assigned to device|HTP\\d|ggml_opencl|ggml_vulkan|ggml_metal|ggml_cuda|ggml-hex|openvino|model buffer size|KV buffer size|compute buffer size|using device|Found \\d+ (Vulkan|CUDA|Metal)", RegexOption.IGNORE_CASE)
        val MODEL_BUFFER = Regex("(\\S+) model buffer size\\s*=\\s*([0-9.]+) MiB", RegexOption.IGNORE_CASE)
        val KV_BUFFER = Regex("(\\S+) KV buffer size\\s*=\\s*([0-9.]+) MiB", RegexOption.IGNORE_CASE)
        val COMPUTE_BUFFER = Regex("(\\S+) compute buffer size\\s*=\\s*([0-9.]+) MiB", RegexOption.IGNORE_CASE)
        val USING_DEVICE = Regex("using device (\\S+) \\(([^)]*)\\)", RegexOption.IGNORE_CASE)
        val CHIP_LAYER = Regex("assigned to device (GPU|HTP|Vulkan|OpenCL|OpenVINO|CUDA|Metal)", RegexOption.IGNORE_CASE)
        val NPU_DEVICE = Regex("device HTP\\d", RegexOption.IGNORE_CASE)
        val NPU_BUFFER = Regex("(HTP\\d|OPENVINO|OpenVINO)\\S*\\s+model buffer size", RegexOption.IGNORE_CASE)
        /** The phone's own driver folders (OpenCL, Vulkan and the NPU's connection library live there). */
        val VENDOR_LIB_DIRS = listOf("/vendor/lib64", "/system/vendor/lib64", "/odm/lib64")
        /** Where the phone keeps the NPU's own programs. */
        val DSP_DIRS = listOf("/vendor/dsp/cdsp", "/vendor/lib/rfsa/adsp", "/vendor/dsp", "/system/lib/rfsa/adsp", "/dsp")
        /** The NPU of a Snapdragon gives each session about 2 GB. */
        const val SAFE_SUFFIX = "-safe"

        /** What the Vulkan engine prints at start on a phone whose driver is too old; the model is then not worth loading on it. */
        private const val NO_VULKAN = "Vulkan 1.2 required"
        private const val NO_VULKAN_MESSAGE = "The graphics driver of this phone is too old (Vulkan 1.2 is required)"
        const val NPU_SESSION_MB = 1800
        const val MAX_NPU_SESSIONS = 4
        /** Memory left free on a computer's graphics card when it is filled with the model (the engine's own default is 1024). */
        const val TIGHT_GPU_MARGIN_MB = 256
        val LAYERS = Regex("offloaded (\\d+)/(\\d+) layers to GPU", RegexOption.IGNORE_CASE)
        val DEVICE = Regex("(?:ggml_vulkan: \\d+ = |using device |Device \\d+: |ggml_opencl: device: \'?)([^\\n(]+)", RegexOption.IGNORE_CASE)
        const val TRANSCRIBE_RULE =
            "You are a speech-to-text engine. You write down exactly what is said in the recording, in the language " +
                "that is spoken, with normal punctuation. You never answer, translate, explain or add anything."
        const val TRANSCRIBE_ASK = "Write down exactly what is said in this recording. Reply with the text only."
    }
}

/** The model thought quietly for too long without starting its answer. */
class ThinkingTooLongException : IllegalStateException("The model kept reasoning without answering")
