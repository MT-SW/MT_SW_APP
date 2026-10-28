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
import java.util.UUID

/**
 * Dictation with Whisper models through the official `whisper-server` program of whisper.cpp (started on this computer,
 * reachable only from the computer itself). The recording is sent to it as a WAV file and the text comes back. Like the
 * language model, the speech model is loaded on first use and freed after a short idle time.
 */
class WhisperCppEngine(
    private val store: AiModelStoreImpl,
    private val installer: ModelServerInstaller,
    private val logDir: File,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var unloadJob: Job? = null
    private var server: ModelServerProcess? = null
    private var loadedId: String? = null

    init {
        ModelServerProcess.stopLeftovers(File(logDir, PID_FILE))
    }

    suspend fun transcribe(modelId: String, wav: ByteArray): String = mutex.withLock {
        unloadJob?.cancel()
        try {
            withContext(Dispatchers.IO) {
                val running = ensureLoaded(modelId)
                val started = nowMillis
                val text = send(running, wav)
                Logger.i(tag = TAG) { "Dictation with $modelId: ${text.length} chars in ${nowMillis - started} ms" }
                text
            }
        } finally {
            scheduleUnload()
        }
    }

    suspend fun unloadNow() {
        mutex.withLock { unload() }
    }

    private suspend fun ensureLoaded(modelId: String): ModelServerProcess {
        server?.let { if (it.isAlive && loadedId == modelId) return it }
        unload()
        val model = store.fileOf(modelId) ?: error("Model $modelId is not downloaded")
        val asset = installer.whisperAsset() ?: error("No speech program exists for this system")
        val program = installer.install(asset)
        val port = ModelServerProcess.freePort()
        val threads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, MAX_THREADS)
        val command =
            installer.launchPrefix(program) +
                listOf("-m", model.absolutePath, "--host", "127.0.0.1", "--port", port.toString(), "-l", "auto", "-t", threads.toString(), "-nt")
        Logger.i(tag = TAG) { "Starting the speech program for $modelId" }
        val started = ModelServerProcess.start(command, program.parentFile, port, File(logDir, "whisper-server.log"), File(logDir, PID_FILE)) {}
        try {
            started.awaitReady("/", LOAD_TIMEOUT_MS, anyAnswer = true)
        } catch (e: Throwable) {
            Logger.w(tag = TAG, throwable = e) { "The speech program did not start: ${e.message}\n${started.logTail().takeLast(MAX_TAIL)}" }
            started.stop()
            throw e
        }
        server = started
        loadedId = modelId
        store.reportLoaded(TAG, modelId)
        return started
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
                field("response_format", "json")
                field("language", "auto")
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

    private fun scheduleUnload() {
        unloadJob?.cancel()
        unloadJob =
            scope.launch {
                delay(IDLE_UNLOAD_MS)
                mutex.withLock { unload() }
            }
    }

    private fun unload() {
        server?.stop()
        server = null
        loadedId = null
        store.reportLoaded(TAG, null)
    }

    private companion object {
        const val TAG = "WhisperCppEngine"
        const val PID_FILE = "whisper-server.pid"
        const val MAX_THREADS = 8
        const val LOAD_TIMEOUT_MS = 120_000L
        const val IDLE_UNLOAD_MS = 120_000L
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 5 * 60 * 1000
        const val MAX_ERROR = 300
        const val MAX_TAIL = 3000
    }
}
