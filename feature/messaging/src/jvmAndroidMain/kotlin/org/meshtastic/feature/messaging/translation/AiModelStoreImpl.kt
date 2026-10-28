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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.meshtastic.core.repository.AiEngine
import org.meshtastic.core.repository.AiFileKind
import org.meshtastic.core.repository.AiModelFile
import org.meshtastic.core.repository.AiModelInfo
import org.meshtastic.core.repository.AiModelStore
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.TranslatorModelState
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * A program a model needs besides its own files (the llama.cpp or whisper.cpp program on a computer). It is fetched
 * together with the first model that needs it, and only when the user starts that download.
 */
interface AiEngineInstaller {
    /** True when this installer provides the program for [engine]. */
    fun handles(engine: AiEngine): Boolean

    /** Size of the download in megabytes while [engine] is not installed yet, 0 when it is ready. */
    fun missingMb(engine: AiEngine): Int

    /** Downloads and unpacks the program for [engine]; [onProgress] gets 0..1. Throws when it cannot. */
    suspend fun ensure(engine: AiEngine, onProgress: (Float) -> Unit)
}

/**
 * Keeps the downloaded state of every model in [AiModels.all]. The NLLB model is handled by [NllbTranslator] itself;
 * the other models are files downloaded here into one folder per model under [ggufDir]. A model's engine program (if it
 * needs one) comes from [installer]. Several models can be on the device at once and each is downloaded, cancelled and
 * deleted on its own.
 */
class AiModelStoreImpl(
    private val nllb: NllbTranslator,
    val ggufDir: File,
    private val installer: AiEngineInstaller? = null,
) : AiModelStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gguf = MutableStateFlow(initialGgufStates())
    private val jobs = HashMap<String, Job>()

    private val legacyFreed = MutableStateFlow(0)

    /**
     * Megabytes freed when the files of the old engine (Google LiteRT-LM) were removed at this start of the app; 0 when
     * nothing was removed. The settings show a short notice for it.
     */
    override val legacyFreedMb: StateFlow<Int> = legacyFreed

    init {
        scope.launch(Dispatchers.IO) { cleanLegacyOnce() }
    }

    /**
     * Removes what the old engine left (its model files and its cache) once, the first time the app starts with the new
     * engine. Only files the app itself downloaded into its own model folder are touched, and never again afterwards,
     * so a model the user downloads later is never removed by this.
     */
    private fun cleanLegacyOnce() {
        val marker = File(ggufDir.parentFile ?: return, LEGACY_MARKER)
        if (marker.exists()) return
        var bytes = 0L
        fun remove(file: File) {
            if (!file.exists()) return
            bytes += file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            file.deleteRecursively()
        }
        runCatching {
            AiModels.legacyModelIds.forEach { remove(File(ggufDir, it)) }
            ggufDir.listFiles()?.filter { it.name == "cache" || it.name.startsWith("cache-") }?.forEach { remove(it) }
            ggufDir.walkTopDown().filter { it.isFile && it.name.endsWith(".litertlm") }.forEach { remove(it) }
            marker.writeText("old engine files removed")
        }.onFailure { Logger.w(tag = TAG, throwable = it) { "Removing the old engine files failed: ${it.message}" } }
        val mb = (bytes / MB).toInt()
        if (mb > 0) Logger.i(tag = TAG) { "Removed the files of the old engine: $mb MB" }
        legacyFreed.value = mb
    }

    /** Called before a model is deleted so the engine can release its file first. */
    @Volatile var beforeModelDelete: (suspend () -> Unit)? = null

    override val states: StateFlow<Map<String, TranslatorModelState>> =
        combine(nllb.state, gguf) { n, g -> g + (AiModels.NLLB_600M_ID to n) }
            .stateIn(scope, SharingStarted.Eagerly, gguf.value + (AiModels.NLLB_600M_ID to nllb.state.value))

    override suspend fun download(id: String) {
        val model = AiModels.byId(id) ?: return
        if (model.engine == AiEngine.NLLB_ONNX) {
            nllb.download()
            return
        }
        if (!model.engineReady) return
        if (gguf.value[id] is TranslatorModelState.Downloading) return
        if (isComplete(model)) {
            setGguf(id, TranslatorModelState.Ready)
            return
        }
        val dir = modelDir(model)
        dir.mkdirs()
        val engineMb = installer?.takeIf { it.handles(model.engine) }?.missingMb(model.engine) ?: 0
        // Leave a little room beyond the model size so a nearly full disk fails early instead of half-way.
        if (dir.usableSpace < (model.sizeMb + engineMb).toLong() * MB * SPACE_MARGIN) {
            setGguf(id, TranslatorModelState.Failed("not enough free space"))
            return
        }
        setGguf(id, TranslatorModelState.Downloading(0f))
        val job = currentCoroutineContext()[Job]
        if (job != null) synchronized(jobs) { jobs[id] = job }
        try {
            withContext(Dispatchers.IO) {
                val total = (model.sizeMb + engineMb).toFloat().coerceAtLeast(1f)
                var done = 0f
                if (engineMb > 0) {
                    installer?.ensure(model.engine) { fraction ->
                        setGguf(id, TranslatorModelState.Downloading((fraction * engineMb / total).coerceIn(0f, 0.99f)))
                    }
                    done += engineMb
                }
                for (file in model.files) {
                    val size = fileSizeMb(model, file).toFloat()
                    val before = done
                    downloadFile(file, File(dir, file.fileName)) { fraction ->
                        setGguf(id, TranslatorModelState.Downloading(((before + fraction * size) / total).coerceIn(0f, 0.99f)))
                    }
                    done += size
                }
            }
            setGguf(id, TranslatorModelState.Ready)
        } catch (e: CancellationException) {
            setGguf(id, TranslatorModelState.Failed("cancelled"))
            throw e
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "Download of ${model.name} failed: ${e.message}" }
            setGguf(id, TranslatorModelState.Failed(e.message ?: "download failed"))
        } finally {
            synchronized(jobs) { jobs.remove(id) }
        }
    }

    override fun cancelDownload(id: String) {
        if (AiModels.byId(id)?.engine == AiEngine.NLLB_ONNX) {
            nllb.cancelDownload()
        } else {
            synchronized(jobs) { jobs[id] }?.cancel()
        }
    }

    override suspend fun delete(id: String) {
        val model = AiModels.byId(id) ?: return
        if (model.engine == AiEngine.NLLB_ONNX) {
            nllb.deleteModel()
            return
        }
        cancelDownload(id)
        beforeModelDelete?.invoke()
        withContext(Dispatchers.IO) { modelDir(model).deleteRecursively() }
        setGguf(id, TranslatorModelState.NotDownloaded)
    }

    /** Absolute path of a downloaded model file (the model itself), or null while the model is not complete. */
    fun fileOf(id: String): File? = fileOf(id, AiFileKind.MODEL)

    /** Absolute path of the file of the given [kind] (for example the hearing part), or null when there is none or it is not complete. */
    fun fileOf(id: String, kind: AiFileKind): File? {
        val model = AiModels.byId(id) ?: return null
        val file = model.files.firstOrNull { it.kind == kind } ?: return null
        return File(modelDir(model), file.fileName).takeIf { isComplete(model) }
    }

    private fun modelDir(model: AiModelInfo) = File(ggufDir, model.id)

    /** The real size of one file; a model that does not list it falls back to the whole model size. */
    private fun fileSizeMb(model: AiModelInfo, file: AiModelFile): Int = file.sizeMb.takeIf { it > 0 } ?: model.sizeMb

    private fun expectedBytes(model: AiModelInfo, file: AiModelFile): Long =
        fileSizeMb(model, file).toLong() * MB * MIN_COMPLETE_FRACTION / 100

    private fun isComplete(model: AiModelInfo): Boolean = model.files.isNotEmpty() &&
        model.files.all { f ->
            val file = File(modelDir(model), f.fileName)
            file.isFile && file.length() >= expectedBytes(model, f)
        }

    private fun initialGgufStates(): Map<String, TranslatorModelState> = AiModels.all
        .filter { it.engine != AiEngine.NLLB_ONNX && it.engineReady }
        .associate { it.id to (if (isComplete(it)) TranslatorModelState.Ready else TranslatorModelState.NotDownloaded) }

    private fun setGguf(id: String, state: TranslatorModelState) {
        gguf.update { it + (id to state) }
    }

    private suspend fun downloadFile(spec: AiModelFile, target: File, onProgress: (Float) -> Unit) {
        val url = spec.url
        val part = File(target.parentFile, target.name + ".part")
        var attempts = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val have = if (part.isFile) part.length() else 0L
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                if (have > 0) connection.setRequestProperty("Range", "bytes=$have-")
                val code = connection.responseCode
                val append = code == HttpURLConnection.HTTP_PARTIAL
                if (code != HttpURLConnection.HTTP_OK && !append) {
                    if (code == HTTP_RANGE_NOT_SATISFIABLE && part.isFile) {
                        part.renameTo(target)
                        return
                    }
                    error("HTTP $code")
                }
                val total = (if (append) have else 0L) + connection.contentLengthLong.coerceAtLeast(0L)
                var written = if (append) have else 0L
                FileOutputStream(part, append).use { out ->
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            out.write(buffer, 0, n)
                            written += n
                            if (total > 0) onProgress(written.toFloat() / total)
                        }
                    }
                }
                if (written < spec.sizeMb.toLong() * MB * MIN_COMPLETE_FRACTION / 100) error("Incomplete download")
                if (target.exists()) target.delete()
                if (!part.renameTo(target)) error("Cannot save ${target.name}")
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempts++
                if (attempts >= MAX_ATTEMPTS) throw e
                delay(RETRY_DELAY_MS)
            } finally {
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val TAG = "AiModelStore"
        const val LEGACY_MARKER = ".ai-engine-v2"
        const val MB = 1_000_000L
        const val MIN_COMPLETE_FRACTION = 90L
        const val SPACE_MARGIN = 1.1
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 30_000
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_ATTEMPTS = 5
        const val RETRY_DELAY_MS = 3_000L
    }
}
