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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.meshtastic.core.common.util.nowMillis
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * The language dictation is expected in. With a fixed language Whisper skips the pass that first guesses the language
 * (a whole extra run of the model), which makes short dictations clearly faster. "auto" lets Whisper guess.
 */
object DictationLanguage {
    @Volatile var hint: String = "auto"

    private val SUPPORTED =
        setOf("pl", "en", "de", "fr", "es", "it", "cs", "sk", "uk", "ru", "nl", "pt", "sv", "no", "da", "fi", "hu", "ro", "bg", "hr", "sl", "lt", "lv", "et", "tr")

    /** The language of the app's interface when Whisper knows it, otherwise "auto". */
    fun ofApp(): String = java.util.Locale.getDefault().language.lowercase().takeIf { it in SUPPORTED } ?: "auto"
}

/**
 * Dictation with Whisper models through the official `whisper-server` program of whisper.cpp (started on this computer,
 * reachable only from the computer itself). The recording is sent to it as a WAV file and the text comes back. Like the
 * language model, the speech model is loaded on first use (or as soon as it is chosen) and stays in memory while it is
 * used, until another model is chosen or the memory is freed.
 */
class WhisperCppEngine(
    private val store: AiModelStoreImpl,
    private val installer: ModelServerInstaller,
    private val logDir: File,
) {
    private val mutex = Mutex()
    private var server: ModelServerProcess? = null
    private var loadedId: String? = null

    init {
        ModelServerProcess.stopLeftovers(File(logDir, PID_FILE))
    }

    suspend fun transcribe(modelId: String, wav: ByteArray): String = mutex.withLock {
        run {
            withContext(Dispatchers.IO) {
                val audioMs = (wav.size - WAV_HEADER_BYTES).coerceAtLeast(0) * MS_PER_SECOND / BYTES_PER_SECOND
                var result = ""
                // A chip that writes nonsense is switched off and the next way (in the end the processor) does the work.
                repeat(MAX_SWITCHES + 1) {
                    val running = ensureLoaded(modelId)
                    val started = nowMillis
                    val text = send(running, wav)
                    val took = nowMillis - started
                    Logger.i(tag = TAG) { "Dictation with $modelId: ${text.length} chars from ${audioMs} ms of speech in $took ms (language ${DictationLanguage.hint})" }
                    result = text
                    val chipMark = activeChipMark
                    if (chipMark == null || !looksLikeNonsense(text, audioMs)) return@withContext text
                    Logger.w(tag = TAG) { "$backend wrote nonsense (${text.length} chars from ${audioMs} ms): ${text.take(80)} - switching it off" }
                    installer.markBroken(chipMark.first, modelId + chipMark.second)
                    unload()
                }
                result
            }
        }
    }

    /** More letters than anyone speaks, or the same few words again and again: the chip counted wrongly. */
    private fun looksLikeNonsense(text: String, audioMs: Long): Boolean {
        if (text.length > MIN_NONSENSE_CHARS + audioMs * MAX_CHARS_PER_SECOND / MS_PER_SECOND) return true
        val words = text.lowercase().split(' ').filter { it.isNotBlank() }
        return words.size >= MIN_WORDS_FOR_REPEAT_CHECK && words.toSet().size * REPEAT_RATIO < words.size
    }

    suspend fun unloadNow() {
        mutex.withLock { unload() }
    }

    /** Loads [modelId] now (it was just chosen), so the first dictation does not wait; failures are only logged. */
    suspend fun preload(modelId: String) {
        if (store.fileOf(modelId) == null) return
        mutex.withLock {
            try {
                withContext(Dispatchers.IO) { ensureLoaded(modelId) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(tag = TAG, throwable = e) { "Loading $modelId in advance failed: ${e.message}" }
            }
        }
    }

    /** One way to start the speech program: [chip] ones (NPU, graphics card) count only when they really work. */
    private class Attempt(
        val name: String,
        val asset: ModelServerInstaller.Asset,
        val chip: Boolean = false,
        val dsp: Boolean = false,
        val extra: List<String> = emptyList(),
        val env: Map<String, String> = emptyMap(),
        /** Added to the model id for the mark that remembers a failure of this way (own mark = other ways stay open). */
        val markSuffix: String = "",
    )

    /** Which way the speech program runs now (for the log and the hardware report). */
    @Volatile var backend: String = ""
        private set

    private val chipLines = StringBuilder()

    /** The program and mark suffix of the chip that runs now (null for the processor): needed to switch a chip off. */
    @Volatile private var activeChipMark: Pair<ModelServerInstaller.Asset, String>? = null

    private suspend fun ensureLoaded(modelId: String): ModelServerProcess {
        server?.let { if (it.isAlive && loadedId == modelId) return it }
        unload()
        val model = store.fileOf(modelId) ?: error("Model $modelId is not downloaded")
        val cpuAssets = installer.whisperProcessorAssets().ifEmpty { error("No speech program exists for this system") }
        // A phone first tries its NPU (Hexagon) and graphics card, then the processor. A chip that does not start, does
        // not take part or fails a test run is skipped for some days for this model.
        val attempts = buildList {
            for (asset in installer.whisperChipAssets()) {
                val kind = asset.folder.substringAfterLast('-')
                if (!installer.android) {
                    if (installer.isBroken(asset, modelId)) continue
                    add(Attempt("graphics card ($kind)", asset, chip = true))
                } else if (kind == "snapdragon") {
                    if (installer.isBroken(asset, modelId)) {
                        Logger.i(tag = TAG) { "Skipping ${asset.folder} for $modelId: it failed here a short time ago" }
                        continue
                    }
                    add(Attempt("NPU (Hexagon)", asset, chip = true, dsp = true, extra = listOf("-fa"), env = mapOf("GGML_HEXAGON_DEVICES" to "1")))
                } else {
                    // Flash attention crashed the OpenCL program and failed in the Adreno Vulkan driver (log 2026-10-30).
                    if (installer.isBroken(asset, modelId)) {
                        Logger.i(tag = TAG) { "Skipping ${asset.folder} for $modelId: it failed here a short time ago" }
                    } else {
                        add(Attempt("graphics card ($kind)", asset, chip = true, extra = listOf("-nfa")))
                    }
                    // Vulkan drivers of some phones (Adreno) fail to build the fast shaders: the same card once more with
                    // the newer driver features switched off (own mark), as the language model program does.
                    if (kind == "vulkan" && !installer.isBroken(asset, modelId + SAFE_SUFFIX)) {
                        add(
                            Attempt(
                                "graphics card (vulkan, safe mode)", asset, chip = true, extra = listOf("-nfa"), markSuffix = SAFE_SUFFIX,
                                env = mapOf(
                                    "GGML_VK_DISABLE_F16" to "1", "GGML_VK_DISABLE_COOPMAT" to "1",
                                    "GGML_VK_DISABLE_INTEGER_DOT_PRODUCT" to "1", "GGML_VK_DISABLE_FUSION" to "1",
                                ),
                            ),
                        )
                    }
                }
            }
            // The Mac program is the Metal build: without the graphics card it is the processor build.
            for (cpu in cpuAssets) {
                val name = if (cpu.folder.endsWith("-i8mm")) "processor (i8mm)" else "processor"
                add(Attempt(name, cpu, extra = if (installer.mac) listOf("-ng") else emptyList()))
            }
        }
        val threads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, MAX_THREADS)
        var failure: Throwable? = null
        for (attempt in attempts) {
            val program =
                if (attempt.chip) installer.installGpu(attempt.asset) else installer.install(attempt.asset)
            if (program == null) {
                Logger.w(tag = TAG) { "${attempt.name}: the program could not be downloaded (${installer.lastInstallError})" }
                continue
            }
            val port = ModelServerProcess.freePort()
            val command =
                installer.launchPrefix(program) +
                    listOf("-m", model.absolutePath, "--host", "127.0.0.1", "--port", port.toString(), "-l", "auto", "-t", threads.toString(), "-nt") +
                    attempt.extra
            val libDirs = buildList {
                File(program.parentFile.parentFile, "lib").takeIf { it.isDirectory }?.let { add(it.absolutePath) }
                if (attempt.chip) addAll(VENDOR_LIB_DIRS.filter { File(it).isDirectory })
            }
            val env = buildMap<String, String> {
                putAll(attempt.env)
                if (attempt.dsp) {
                    // The NPU programs (libggml-htp-v*.so) are in the "lib" folder of the package, next to "bin".
                    val skeletons = File(program.parentFile.parentFile, "lib").absolutePath
                    put("ADSP_LIBRARY_PATH", (listOf(skeletons, program.parentFile.absolutePath) + DSP_DIRS).joinToString(";"))
                }
            }
            Logger.i(tag = TAG) { "Starting the speech program for $modelId: ${attempt.name}" }
            synchronized(chipLines) { chipLines.clear() }
            val started =
                ModelServerProcess.start(command, program.parentFile, port, File(logDir, "whisper-server.log"), File(logDir, PID_FILE), env, libDirs) { line ->
                    if (CHIP_LINE.containsMatchIn(line)) synchronized(chipLines) { if (chipLines.length < MAX_TAIL) chipLines.appendLine(line.trim()) }
                }
            try {
                started.awaitReady("/", LOAD_TIMEOUT_MS, anyAnswer = true)
                if (attempt.chip) {
                    // The program's log lines reach us a little after the program answers: wait for the chip's own lines.
                    var seen = synchronized(chipLines) { chipLines.toString() }
                    var waited = 0
                    while (!CHIP_USED.containsMatchIn(seen) && waited < CHIP_LOG_WAIT_MS) {
                        Thread.sleep(CHIP_LOG_STEP_MS)
                        waited += CHIP_LOG_STEP_MS.toInt()
                        seen = synchronized(chipLines) { chipLines.toString() }
                    }
                    check(CHIP_USED.containsMatchIn(seen)) { "the chip took no part: ${seen.takeLast(300)}" }
                    // A software "graphics card" (llvmpipe) is slower than the processor build.
                    check(!seen.contains("llvmpipe", ignoreCase = true)) { "only a software graphics card was found" }
                    // A chip can load the model and still fail or write nonsense: one second of silence must go through,
                    // give (almost) no text, and the second run must be faster than the processor (about 2 seconds).
                    withContext(Dispatchers.IO) {
                        val first = send(started, silence())
                        check(first.length <= MAX_SILENCE_CHARS) { "it wrote ${first.length} characters for silence: ${first.take(60)}" }
                        val t0 = nowMillis
                        send(started, silence())
                        val took = nowMillis - t0
                        check(took <= MAX_SILENCE_MS) { "it is slower than the processor (${took} ms for a test run)" }
                    }
                }
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    started.stop()
                    throw e
                }
                Logger.w(tag = TAG, throwable = e) { "${attempt.name} did not work: ${e.message}\n${started.logTail().takeLast(MAX_TAIL)}" }
                started.stop()
                if (attempt.chip) installer.markBroken(attempt.asset, modelId + attempt.markSuffix)
                failure = e
                continue
            }
            server = started
            loadedId = modelId
            backend = attempt.name
            activeChipMark = if (attempt.chip) attempt.asset to attempt.markSuffix else null
            Logger.i(tag = TAG) { "Speech program runs on: ${attempt.name}" }
            store.reportLoaded(TAG, modelId)
            return started
        }
        throw failure ?: IllegalStateException("The speech program could not be started")
    }

    /** One second of silence as a WAV file (16 kHz, mono, 16 bit): a test run for a chip. */
    private fun silence(): ByteArray {
        val data = BYTES_PER_SECOND.toInt()
        val header = ByteBuffer.allocate(WAV_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray()).put("fmt ".toByteArray())
        header.putInt(16).putShort(1).putShort(1).putInt(16_000).putInt(32_000).putShort(2).putShort(16)
        header.put("data".toByteArray()).putInt(data)
        return header.array() + ByteArray(data)
    }

    private fun send(running: ModelServerProcess, wav: ByteArray): String {
        val boundary = "----mtsw" + UUID.randomUUID().toString().replace("-", "")
        val connection = URL(running.baseUrl + "/inference").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.outputStream.use { out ->
                fun field(name: String, value: String) {
                    out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray())
                }
                out.write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\nContent-Type: audio/wav\r\n\r\n".toByteArray())
                out.write(wav)
                out.write("\r\n".toByteArray())
                field("temperature", "0.0")
                // No repeated attempts at higher temperatures and no beam search: the fastest decoding that stays accurate.
                field("temperature_inc", "0.0")
                field("beam_size", "1")
                field("best_of", "1")
                field("response_format", "json")
                field("language", DictationLanguage.hint)
                out.write("--$boundary--\r\n".toByteArray())
            }
            val code = connection.responseCode
            val body = (if (code == HttpURLConnection.HTTP_OK) connection.inputStream else connection.errorStream ?: connection.inputStream)
                .bufferedReader(Charsets.UTF_8).readText()
            if (code != HttpURLConnection.HTTP_OK) error("The speech program answered HTTP $code: ${body.take(MAX_ERROR)}")
            val json = Json.parseToJsonElement(body).jsonObject
            json["error"]?.let { error("The speech program reported: ${it.jsonPrimitive.contentOrNull}") }
            return clean(json["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    /** Whisper marks silence and noise in brackets, for example [BLANK_AUDIO] or (music); those are not words. */
    private fun clean(text: String): String =
        text.replace(Regex("\\[[^\\]]*\\]|\\([^)]*\\)"), " ").replace(Regex("\\s+"), " ").trim()


    private fun unload() {
        server?.stop()
        server = null
        loadedId = null
        activeChipMark = null
        store.reportLoaded(TAG, null)
    }

    private companion object {
        const val TAG = "WhisperCppEngine"
        const val PID_FILE = "whisper-server.pid"
        const val SAFE_SUFFIX = "-safe"
        const val MAX_THREADS = 8
        const val MAX_SWITCHES = 3
        const val CHIP_LOG_WAIT_MS = 3_000
        const val CHIP_LOG_STEP_MS = 100L
        const val MIN_NONSENSE_CHARS = 60
        const val MAX_CHARS_PER_SECOND = 30
        const val MIN_WORDS_FOR_REPEAT_CHECK = 12
        const val REPEAT_RATIO = 4
        const val MAX_SILENCE_CHARS = 30
        const val MAX_SILENCE_MS = 4_500L
        const val WAV_HEADER_BYTES = 44
        val VENDOR_LIB_DIRS = listOf("/vendor/lib64", "/system/vendor/lib64", "/odm/lib64")
        val DSP_DIRS = listOf("/vendor/dsp/cdsp", "/vendor/lib/rfsa/adsp", "/vendor/dsp", "/system/lib/rfsa/adsp", "/dsp")
        val CHIP_LINE = Regex("backend|HTP\\d|Hexagon|OpenCL|Vulkan|Metal|Adreno|device", RegexOption.IGNORE_CASE)
        val CHIP_USED = Regex("using (HTP|Hexagon|OpenCL|Vulkan|GPU)|HTP\\d|ggml_opencl|ggml_vulkan|ggml_metal|using Metal|using MTL\\d|MTL\\d|ggml-hex|Adreno|Found \\d+ Vulkan", RegexOption.IGNORE_CASE)
        const val BYTES_PER_SECOND = 32_000L
        const val MS_PER_SECOND = 1_000L
        const val LOAD_TIMEOUT_MS = 120_000L
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 5 * 60 * 1000
        const val MAX_ERROR = 300
        const val MAX_TAIL = 3000
    }
}
