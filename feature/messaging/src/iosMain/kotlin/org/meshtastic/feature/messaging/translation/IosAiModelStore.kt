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
@file:Suppress(
    "ktlint:standard:backing-property-naming",
    "BackingPropertyNaming",
    "FunctionSignature",
    "LoopWithTooManyJumpStatements",
    "MagicNumber",
    "ReturnCount",
    "TooGenericExceptionCaught",
)

package org.meshtastic.feature.messaging.translation

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.meshtastic.core.repository.AiFileKind
import org.meshtastic.core.repository.AiModelStore
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.TranslatorModelState

private const val CHUNK_BYTES = 256 * 1024

/** Models of the on-device AI for the iPhone: downloaded to the app's own folder, one folder per model. */
class IosAiModelStore(baseDir: String) : AiModelStore {
    private val fs = FileSystem.SYSTEM
    private val root: Path = (baseDir + "/ai-models").toPath().also { fs.createDirectories(it) }
    private val jobs = HashMap<String, Job>()
    private val _states = MutableStateFlow(initialStates())
    override val states: StateFlow<Map<String, TranslatorModelState>> = _states
    private val loadedBy = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _loaded = MutableStateFlow<Set<String>>(emptySet())

    /** Set by the engine: frees every model it holds in memory. */
    var unloadAll: (suspend () -> Unit)? = null

    /** An engine ([engine] names it) says which model it now holds in memory; null when it holds none. */
    fun reportLoaded(engine: String, modelId: String?) {
        loadedBy.update { if (modelId == null) it - engine else it + (engine to modelId) }
        _loaded.value = loadedBy.value.values.toSet()
    }

    private fun filesOf(id: String) = AiModels.byId(id)?.activeFiles.orEmpty()

    private fun fileOf(id: String, name: String): Path = root / id / name

    private fun isComplete(id: String): Boolean {
        val files = filesOf(id)
        return files.isNotEmpty() && files.all { fs.exists(fileOf(id, it.fileName)) }
    }

    private fun initialStates(): Map<String, TranslatorModelState> = AiModels.all
        .filter { it.files.isNotEmpty() }
        .associate {
            it.id to (if (isComplete(it.id)) TranslatorModelState.Ready else TranslatorModelState.NotDownloaded)
        }

    /** Full path of the main model file when the model is on the device; null otherwise. */
    fun modelPath(id: String): String? {
        if (!isComplete(id)) return null
        val main = filesOf(id).firstOrNull { it.kind == AiFileKind.MODEL } ?: return null
        return fileOf(id, main.fileName).toString()
    }

    /** Full path of the hearing (audio) file of a model that can hear; null when it has none. */
    fun hearingPath(id: String): String? {
        if (!isComplete(id)) return null
        val file = filesOf(id).firstOrNull { it.kind == AiFileKind.HEARING } ?: return null
        return fileOf(id, file.fileName).toString()
    }

    private fun setState(id: String, state: TranslatorModelState) = _states.update { it + (id to state) }

    override suspend fun download(id: String) {
        val files = filesOf(id)
        if (files.isEmpty()) return
        if (isComplete(id)) {
            setState(id, TranslatorModelState.Ready)
            return
        }
        jobs[id] = currentCoroutineContext()[Job]!!
        setState(id, TranslatorModelState.Downloading(0f))
        val client =
            HttpClient(Darwin) {
                install(HttpTimeout) {
                    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                }
            }
        try {
            withContext(Dispatchers.Default) {
                fs.createDirectories(root / id)
                val total = files.sumOf { it.sizeMb.toLong() }.coerceAtLeast(1) * 1024L * 1024L
                var before = 0L
                for (file in files) {
                    val target = fileOf(id, file.fileName)
                    if (fs.exists(target)) {
                        before += file.sizeMb.toLong() * 1024L * 1024L
                        continue
                    }
                    val part = fileOf(id, file.fileName + ".part")
                    client.prepareGet(file.url).execute { response ->
                        val channel = response.bodyAsChannel()
                        val buffer = ByteArray(CHUNK_BYTES)
                        var written = 0L
                        val sink = fs.sink(part).buffer()
                        try {
                            while (true) {
                                currentCoroutineContext()[Job]?.let { if (!it.isActive) throw CancellationException() }
                                val n = channel.readAvailable(buffer, 0, buffer.size)
                                if (n < 0) break
                                if (n == 0) continue
                                sink.write(buffer, 0, n)
                                written += n
                                setState(
                                    id,
                                    TranslatorModelState.Downloading(
                                        ((before + written).toFloat() / total).coerceIn(0f, 0.99f),
                                    ),
                                )
                            }
                        } finally {
                            sink.close()
                        }
                    }
                    fs.atomicMove(part, target)
                    before += fs.metadata(target).size ?: 0L
                }
            }
            setState(id, TranslatorModelState.Ready)
        } catch (e: CancellationException) {
            filesOf(id).forEach { fs.delete(fileOf(id, it.fileName + ".part"), mustExist = false) }
            setState(id, TranslatorModelState.NotDownloaded)
            throw e
        } catch (e: Exception) {
            Logger.e(throwable = e) { "Model download failed: $id" }
            filesOf(id).forEach { fs.delete(fileOf(id, it.fileName + ".part"), mustExist = false) }
            setState(id, TranslatorModelState.Failed(e.message ?: "download failed"))
        } finally {
            jobs.remove(id)
            client.close()
        }
    }

    override fun cancelDownload(id: String) {
        jobs[id]?.cancel()
    }

    override suspend fun delete(id: String) {
        cancelDownload(id)
        if (fs.exists(root / id)) fs.deleteRecursively(root / id)
        setState(id, TranslatorModelState.NotDownloaded)
    }

    override val loadedModels: StateFlow<Set<String>>
        get() = _loaded

    override suspend fun unloadModels() {
        unloadAll?.invoke()
    }
}
