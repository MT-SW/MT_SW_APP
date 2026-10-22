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

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.meshtastic.core.repository.TranslatorModelManager
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.repository.UiPrefs
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.FloatBuffer

/**
 * Offline translator built on the multilingual NLLB-200 (distilled, 600M) model, run with ONNX Runtime on both Android
 * and desktop. One model covers every language pair. The model files (~870 MB) are downloaded after the user switches
 * the translator on, and are loaded into memory only while translating (and freed again after a short idle time).
 */
@Suppress("TooManyFunctions")
class NllbTranslator(private val modelDir: File, private val uiPrefs: UiPrefs) :
    MessageTranslationService,
    TranslatorModelManager {

    private val _state = MutableStateFlow<TranslatorModelState>(initialState())
    override val state: StateFlow<TranslatorModelState> = _state.asStateFlow()
    override val approximateSizeMb: Int = APPROX_SIZE_MB
    override val approximateRamMb: Int = APPROX_RAM_MB

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runMutex = Mutex()
    private var unloadJob: Job? = null

    private var tokenizer: NllbTokenizer? = null
    private var encoder: OrtSession? = null
    private var decoder: OrtSession? = null

    // region MessageTranslationService

    override suspend fun isLanguageAvailable(locale: String): Boolean =
        uiPrefs.translatorEnabled.value && TranslatorLanguages.fromLocale(locale) != null

    override suspend fun translate(text: String, targetLocale: String): TranslationResult {
        if (text.isBlank() || !uiPrefs.translatorEnabled.value) return TranslationResult.Unavailable
        val target = TranslatorLanguages.fromLocale(targetLocale) ?: return TranslationResult.Unavailable
        val source = LanguageGuesser.guess(text) ?: return TranslationResult.Unavailable
        if (source == target.nllb) return TranslationResult.NotRequired
        return translateBetween(text, source, target.nllb)
    }

    override suspend fun translateBetween(text: String, sourceNllb: String?, targetNllb: String): TranslationResult {
        if (text.isBlank() || !uiPrefs.translatorEnabled.value) return TranslationResult.Unavailable
        val source = sourceNllb ?: LanguageGuesser.guess(text) ?: return TranslationResult.Unavailable
        if (source == targetNllb) return TranslationResult.NotRequired
        if (_state.value != TranslatorModelState.Ready) {
            return TranslationResult.ModelDownloadRequired(listOf(MODEL_TAG), APPROX_SIZE_MB)
        }
        return try {
            TranslationResult.Success(run(text, source, targetNllb))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // OutOfMemoryError and UnsatisfiedLinkError are real outcomes on weak devices, not bugs to crash on.
            Logger.w(tag = TAG, throwable = e) { "Translation failed: ${e.message}" }
            unload()
            TranslationResult.Unavailable
        }
    }

    override suspend fun downloadLanguageModels(languageTags: List<String>): DownloadResult {
        download()
        return when (val s = _state.value) {
            TranslatorModelState.Ready -> DownloadResult.Success
            is TranslatorModelState.Failed -> DownloadResult.Failed(s.reason)
            else -> DownloadResult.Failed("Download not finished")
        }
    }

    // endregion

    // region TranslatorModelManager

    override suspend fun download() {
        if (_state.value is TranslatorModelState.Downloading) return
        if (isComplete()) {
            _state.value = TranslatorModelState.Ready
            return
        }
        _state.value = TranslatorModelState.Downloading(0f)
        downloadJob = currentCoroutineContext()[Job]
        try {
            withContext(Dispatchers.IO) { downloadAll() }
            _state.value = TranslatorModelState.Ready
        } catch (e: CancellationException) {
            _state.value = TranslatorModelState.Failed("cancelled")
            throw e
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "Model download failed: ${e.message}" }
            _state.value = TranslatorModelState.Failed(e.message ?: "download failed")
        }
    }

    @Volatile private var downloadJob: Job? = null

    override fun cancelDownload() {
        downloadJob?.cancel()
    }

    override suspend fun deleteModel() {
        unload()
        withContext(Dispatchers.IO) {
            modelDir.listFiles()?.forEach { it.delete() }
        }
        _state.value = TranslatorModelState.NotDownloaded
    }

    // endregion

    // region Download

    private fun initialState(): TranslatorModelState =
        if (isComplete()) TranslatorModelState.Ready else TranslatorModelState.NotDownloaded

    private fun isComplete(): Boolean = FILES.all { f ->
        val file = File(modelDir, f.name)
        file.isFile && file.length() >= f.minBytes
    }

    private suspend fun downloadAll() {
        modelDir.mkdirs()
        val totalWeight = FILES.sumOf { it.weightBytes }
        var doneBefore = 0L
        for (f in FILES) {
            val target = File(modelDir, f.name)
            if (target.isFile && target.length() >= f.minBytes) {
                doneBefore += f.weightBytes
                continue
            }
            downloadOne(f, target) { fileBytes ->
                val fraction = (doneBefore + fileBytes).toFloat() / totalWeight
                _state.value = TranslatorModelState.Downloading(fraction.coerceIn(0f, 0.99f))
            }
            doneBefore += f.weightBytes
        }
    }

    private suspend fun downloadOne(f: ModelFile, target: File, onProgress: (Long) -> Unit) {
        val part = File(modelDir, f.name + ".part")
        var attempts = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val have = if (part.isFile) part.length() else 0L
            val connection = (URL(BASE_URL + f.remotePath).openConnection() as HttpURLConnection)
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                if (have > 0) connection.setRequestProperty("Range", "bytes=$have-")
                val code = connection.responseCode
                val append = code == HttpURLConnection.HTTP_PARTIAL
                if (code != HttpURLConnection.HTTP_OK && !append) {
                    if (code == 416 && part.isFile) {
                        // Requested range is past the end: the part file is already complete.
                        part.renameTo(target)
                        return
                    }
                    error("HTTP $code for ${f.remotePath}")
                }
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
                            onProgress(written)
                        }
                    }
                }
                if (written < f.minBytes) error("Incomplete download of ${f.remotePath}")
                if (target.exists()) target.delete()
                if (!part.renameTo(target)) error("Cannot save ${f.name}")
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

    // endregion

    // region Running the model

    private suspend fun run(text: String, source: String, target: String): String = runMutex.withLock {
        unloadJob?.cancel()
        try {
            withContext(Dispatchers.Default) {
                ensureLoaded()
                val tok = tokenizer!!
                val srcId = tok.languageId(source) ?: error("Unknown language $source")
                val tgtId = tok.languageId(target) ?: error("Unknown language $target")
                val context = currentCoroutineContext()
                SentenceSplitter.splitLines(text)
                    .joinToString("\n") { sentences ->
                        sentences.joinToString(" ") { sentence ->
                            context.ensureActive()
                            translateSentence(tok, sentence, srcId, tgtId)
                        }
                    }
                    .trim()
            }
        } finally {
            scheduleUnload()
        }
    }

    private fun ensureLoaded() {
        if (tokenizer != null && encoder != null && decoder != null) return
        val env = OrtEnvironment.getEnvironment()
        val tok = NllbTokenizer.load(File(modelDir, "tokenizer.json"))
        // Optimisation is switched off: it only trades start-up time for a second copy of the weights in RAM, and
        // some ONNX Runtime builds hang on the optimised decoder graph.
        val options =
            OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT)
                setIntraOpNumThreads(threadCount())
                setInterOpNumThreads(1)
            }
        val enc = env.createSession(File(modelDir, "encoder.onnx").path, options)
        val dec = env.createSession(File(modelDir, "decoder.onnx").path, options)
        tokenizer = tok
        encoder = enc
        decoder = dec
    }

    private fun threadCount(): Int = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, 4)

    private fun scheduleUnload() {
        unloadJob?.cancel()
        unloadJob =
            scope.launch {
                delay(IDLE_UNLOAD_MS)
                runMutex.withLock { unload() }
            }
    }

    private fun unload() {
        runCatching { encoder?.close() }
        runCatching { decoder?.close() }
        encoder = null
        decoder = null
        tokenizer = null
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun translateSentence(tok: NllbTokenizer, sentence: String, srcId: Int, tgtId: Int): String {
        val inputIds = tok.encode(sentence, srcId)
        if (inputIds.size <= 2) return sentence
        val env = OrtEnvironment.getEnvironment()
        val enc = encoder!!
        val dec = decoder!!
        val seqLen = inputIds.size
        val mask = LongArray(seqLen) { 1L }
        val generated = ArrayList<Int>()
        val owned = ArrayList<AutoCloseable>()
        try {
            val idsTensor = OnnxTensor.createTensor(env, arrayOf(inputIds)).also { owned.add(it) }
            val maskTensor = OnnxTensor.createTensor(env, arrayOf(mask)).also { owned.add(it) }
            val encResult =
                enc.run(mapOf("input_ids" to idsTensor, "attention_mask" to maskTensor)).also { owned.add(it) }
            val hidden = encResult.get(0) as OnnxTensor

            val maxNew = minOf(MAX_NEW_TOKENS, seqLen * 2 + 16)
            var current = longArrayOf(DECODER_START.toLong(), tgtId.toLong())
            var firstResult: OrtSession.Result? = null
            var lastResult: OrtSession.Result? = null
            var encoderKv: Map<String, OnnxTensor> = emptyMap()
            try {
                for (step in 0 until maxNew) {
                    val stepOwned = ArrayList<AutoCloseable>()
                    try {
                        val inputs = HashMap<String, OnnxTensor>()
                        inputs["encoder_attention_mask"] = maskTensor
                        inputs["encoder_hidden_states"] = hidden
                        inputs["input_ids"] =
                            OnnxTensor.createTensor(env, arrayOf(current)).also { stepOwned.add(it) }
                        inputs["use_cache_branch"] =
                            OnnxTensor.createTensor(env, booleanArrayOf(step > 0)).also { stepOwned.add(it) }
                        if (step == 0) {
                            // The merged decoder wants past tensors even when it does not use them; tiny dummies do.
                            for (layer in 0 until LAYERS) {
                                for (kind in KV_KINDS) {
                                    inputs["past_key_values.$layer.$kind"] =
                                        OnnxTensor.createTensor(
                                            env,
                                            FloatBuffer.allocate(HEADS * HEAD_DIM),
                                            longArrayOf(1, HEADS.toLong(), 1, HEAD_DIM.toLong()),
                                        )
                                            .also { stepOwned.add(it) }
                                }
                            }
                        } else {
                            val previous = lastResult!!
                            for (entry in previous) {
                                val name = entry.key
                                if (name.startsWith("present.") && name.contains(".decoder.")) {
                                    inputs[name.replaceFirst("present.", "past_key_values.")] =
                                        entry.value as OnnxTensor
                                }
                            }
                            for ((name, tensor) in encoderKv) inputs[name] = tensor
                        }

                        val result = dec.run(inputs)
                        val logits = result.get(0) as OnnxTensor
                        val vocabSize = logits.info.shape[2].toInt()
                        val rows = logits.info.shape[1].toInt()
                        val buffer = logits.floatBuffer
                        val next = argmax(buffer, (rows - 1) * vocabSize, vocabSize)

                        if (step == 0) {
                            val kv = HashMap<String, OnnxTensor>()
                            for (entry in result) {
                                val name = entry.key
                                if (name.startsWith("present.") && name.contains(".encoder.")) {
                                    kv[name.replaceFirst("present.", "past_key_values.")] = entry.value as OnnxTensor
                                }
                            }
                            encoderKv = kv
                            firstResult = result
                        } else if (lastResult !== firstResult) {
                            lastResult?.close()
                        }
                        lastResult = result

                        if (next == NllbTokenizer.EOS) break
                        generated.add(next)
                        if (isRepeating(generated)) {
                            repeat(REPEAT_WINDOW) { generated.removeAt(generated.lastIndex) }
                            break
                        }
                        current = longArrayOf(next.toLong())
                    } finally {
                        stepOwned.forEach { runCatching { it.close() } }
                    }
                }
            } finally {
                if (lastResult !== firstResult) runCatching { lastResult?.close() }
                runCatching { firstResult?.close() }
            }
        } finally {
            owned.forEach { runCatching { it.close() } }
        }
        return tok.decode(generated)
    }

    private fun argmax(buffer: FloatBuffer, offset: Int, size: Int): Int {
        var best = NllbTokenizer.EOS
        var bestValue = Float.NEGATIVE_INFINITY
        for (i in 0 until size) {
            // Never produce padding, unknown or language tokens: only real text pieces and the end marker.
            if (i == 0 || i == NllbTokenizer.PAD || i == NllbTokenizer.UNK || i >= NllbTokenizer.FIRST_LANGUAGE_ID) {
                continue
            }
            val v = buffer.get(offset + i)
            if (v > bestValue) {
                bestValue = v
                best = i
            }
        }
        return best
    }

    private fun isRepeating(tokens: List<Int>): Boolean {
        val n = tokens.size
        if (n < REPEAT_WINDOW * 2) return false
        for (i in 1..REPEAT_WINDOW) {
            if (tokens[n - i] != tokens[n - i - REPEAT_WINDOW]) return false
        }
        return true
    }

    // endregion

    private data class ModelFile(
        val name: String,
        val remotePath: String,
        val minBytes: Long,
        val weightBytes: Long,
    )

    private companion object {
        const val TAG = "NllbTranslator"
        const val MODEL_TAG = "nllb-200-distilled-600M"
        const val BASE_URL = "https://huggingface.co/Xenova/nllb-200-distilled-600M/resolve/main/"
        const val APPROX_SIZE_MB = 870
        const val APPROX_RAM_MB = 2300
        const val LAYERS = 12
        const val HEADS = 16
        const val HEAD_DIM = 64
        const val DECODER_START = 2
        const val MAX_NEW_TOKENS = 200
        const val REPEAT_WINDOW = 4
        const val IDLE_UNLOAD_MS = 90_000L
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 30_000
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_ATTEMPTS = 5
        const val RETRY_DELAY_MS = 3_000L
        val KV_KINDS = listOf("decoder.key", "decoder.value", "encoder.key", "encoder.value")
        val FILES =
            listOf(
                ModelFile("tokenizer.json", "tokenizer.json", 10_000_000L, 17_000_000L),
                ModelFile("encoder.onnx", "onnx/encoder_model_quantized.onnx", 300_000_000L, 420_000_000L),
                ModelFile("decoder.onnx", "onnx/decoder_model_merged_quantized.onnx", 300_000_000L, 475_000_000L),
            )
    }
}
