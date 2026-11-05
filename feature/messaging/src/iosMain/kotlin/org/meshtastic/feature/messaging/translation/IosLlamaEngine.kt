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
@file:Suppress("LoopWithTooManyJumpStatements", "MagicNumber", "TooManyFunctions")

package org.meshtastic.feature.messaging.translation

import cnames.structs.llama_context
import cnames.structs.llama_model
import cnames.structs.llama_vocab
import cnames.structs.mtmd_bitmap
import cnames.structs.mtmd_context
import cnames.structs.whisper_context
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.copy
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.plus
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import llama.llama_backend_init
import llama.llama_batch_get_one
import llama.llama_chat_apply_template
import llama.llama_chat_message
import llama.llama_context_default_params
import llama.llama_decode
import llama.llama_free
import llama.llama_get_memory
import llama.llama_init_from_model
import llama.llama_memory_clear
import llama.llama_model_chat_template
import llama.llama_model_default_params
import llama.llama_model_free
import llama.llama_model_get_vocab
import llama.llama_model_load_from_file
import llama.llama_sampler
import llama.llama_sampler_chain_add
import llama.llama_sampler_chain_default_params
import llama.llama_sampler_chain_init
import llama.llama_sampler_free
import llama.llama_sampler_init_dist
import llama.llama_sampler_init_greedy
import llama.llama_sampler_init_temp
import llama.llama_sampler_sample
import llama.llama_token_to_piece
import llama.llama_tokenize
import llama.llama_vocab_is_eog
import llama.mtmd_bitmap_free
import llama.mtmd_bitmap_init_from_audio
import llama.mtmd_context_params_default
import llama.mtmd_default_marker
import llama.mtmd_free
import llama.mtmd_helper_eval_chunks
import llama.mtmd_init_from_file
import llama.mtmd_input_chunks_free
import llama.mtmd_input_chunks_init
import llama.mtmd_input_text
import llama.mtmd_support_audio
import llama.mtmd_tokenize
import llama.whisper_context_default_params
import llama.whisper_free
import llama.whisper_full
import llama.whisper_full_default_params
import llama.whisper_full_get_segment_text
import llama.whisper_full_n_segments
import llama.whisper_init_from_file_with_params
import llama.whisper_sampling_strategy
import org.meshtastic.core.repository.AiEngine
import org.meshtastic.core.repository.AiModels
import platform.Foundation.NSProcessInfo

/** The language of the dictation: an ISO code (for example "pl") or "auto". Set by the dictation service. */
object IosDictationLanguage {
    var hint: String = "auto"
}

private const val CONTEXT_TOKENS = 4096
private const val BATCH_TOKENS = 512
private const val GPU_LAYERS = 99
private const val MAX_PIECE_BYTES = 256
private const val MAX_TRANSCRIPT_TOKENS = 600
private const val TRANSCRIBE_RULE =
    "You are a speech-to-text engine. You write down exactly what is said in the recording, in the language " +
        "that is spoken, with normal punctuation. You never answer, translate, explain or add anything."
private const val TRANSCRIBE_ASK = "Write down exactly what is said in this recording. Reply with the text only."

/** On-device language model for the iPhone: llama.cpp linked into the app (see scripts/build-llama-ios.sh). */
@OptIn(ExperimentalForeignApi::class)
class IosLlamaEngine(
    private val modelPath: (String) -> String?,
    private val hearingPath: (String) -> String? = { null },
) : AiTextEngine {
    private var mtmdContext: CPointer<mtmd_context>? = null
    private var whisperContext: CPointer<whisper_context>? = null
    private var whisperFor: String? = null
    private var mtmdFor: String? = null
    private val lock = Mutex()
    private var loadedId: String? = null
    private var model: CPointer<llama_model>? = null
    private var ctx: CPointer<llama_context>? = null
    private var backend = ""

    override val activeBackend: String
        get() = backend

    override fun contextTokens(modelId: String): Int = CONTEXT_TOKENS

    private fun isSimulator() = NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null

    private fun releaseWhisper() {
        whisperContext?.let { whisper_free(it) }
        whisperContext = null
        whisperFor = null
    }

    private fun release() {
        releaseWhisper()
        mtmdContext?.let { mtmd_free(it) }
        mtmdContext = null
        mtmdFor = null
        ctx?.let { llama_free(it) }
        model?.let { llama_model_free(it) }
        ctx = null
        model = null
        loadedId = null
        backend = ""
    }

    private fun ensureLoaded(modelId: String) {
        if (loadedId == modelId && ctx != null) return
        release()
        val path = modelPath(modelId) ?: error("Model $modelId is not downloaded")
        llama_backend_init()
        val gpu = if (isSimulator()) 0 else GPU_LAYERS
        val loaded =
            llama_model_load_from_file(path, llama_model_default_params().copy { n_gpu_layers = gpu })
                ?: error("Could not load the model file")
        val context =
            llama_init_from_model(
                loaded,
                llama_context_default_params().copy {
                    n_ctx = CONTEXT_TOKENS.convert()
                    n_batch = BATCH_TOKENS.convert()
                },
            )
        if (context == null) {
            llama_model_free(loaded)
            error("Could not create the model context")
        }
        model = loaded
        ctx = context
        loadedId = modelId
        backend = if (gpu > 0) "GPU (Metal)" else "CPU"
    }

    private fun prompt(m: CPointer<llama_model>, messages: List<Pair<String, String>>): String = memScoped {
        val template = llama_model_chat_template(m, null)
        if (template == null) {
            return@memScoped messages.joinToString("\n") { "${it.first}: ${it.second}" } + "\nassistant:"
        }
        val chat = allocArray<llama_chat_message>(messages.size)
        messages.forEachIndexed { i, (role, text) ->
            chat[i].role = role.cstr.ptr
            chat[i].content = text.cstr.ptr
        }
        var capacity = messages.sumOf { it.second.length + it.first.length } * 2 + 1024
        var buffer = allocArray<ByteVar>(capacity)
        var n = llama_chat_apply_template(template, chat, messages.size.convert(), true, buffer, capacity)
        if (n > capacity) {
            capacity = n
            buffer = allocArray(capacity)
            n = llama_chat_apply_template(template, chat, messages.size.convert(), true, buffer, capacity)
        }
        if (n < 0) error("The chat template of this model is not supported")
        buffer.readBytes(n).decodeToString()
    }

    private suspend fun generate(
        c: CPointer<llama_context>,
        vocab: CPointer<llama_vocab>?,
        sampler: CPointer<llama_sampler>,
        startPosition: Int,
        limit: Int,
        onPiece: (String) -> Unit,
    ): String = memScoped {
        val one = alloc<IntVar>()
        val piece = allocArray<ByteVar>(MAX_PIECE_BYTES)
        val out = ArrayList<Byte>()
        var emitted = 0
        var produced = 0
        var position = startPosition
        while (produced < limit && position < CONTEXT_TOKENS - 1) {
            currentCoroutineContext().ensureActive()
            val token = llama_sampler_sample(sampler, c, -1)
            if (llama_vocab_is_eog(vocab, token)) break
            val len = llama_token_to_piece(vocab, token, piece, MAX_PIECE_BYTES, 0, true)
            for (i in 0 until len) out.add(piece[i])
            val decoded = out.toByteArray().decodeToString()
            if (!decoded.endsWith('\uFFFD') && decoded.length > emitted) {
                onPiece(decoded.substring(emitted))
                emitted = decoded.length
            }
            one.value = token
            if (llama_decode(c, llama_batch_get_one(one.ptr, 1)) != 0) break
            produced++
            position++
        }
        out.toByteArray().decodeToString()
    }

    private fun newSampler(temperature: Float): CPointer<llama_sampler> {
        val sampler = llama_sampler_chain_init(llama_sampler_chain_default_params())!!
        if (temperature > 0f) {
            llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature))
            llama_sampler_chain_add(sampler, llama_sampler_init_dist(0xFFFFFFFFu))
        } else {
            llama_sampler_chain_add(sampler, llama_sampler_init_greedy())
        }
        return sampler
    }

    /** The chat template already writes the start-of-text mark itself; adding it a second time worsens the answers. */
    private fun needsStartMark(text: String) = !(text.startsWith("<bos>") || text.startsWith("<|begin_of_text|>"))

    override suspend fun chat(
        modelId: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        temperature: Float,
        onPiece: (String) -> Unit,
        jsonSchema: String?,
        thinking: Boolean,
    ): String = lock.withLock {
        withContext(Dispatchers.Default) {
            ensureLoaded(modelId)
            val m = model!!
            val c = ctx!!
            val vocab = llama_model_get_vocab(m)
            val text = prompt(m, messages)
            val limit = if (maxTokens > 0) maxTokens else CONTEXT_TOKENS
            llama_memory_clear(llama_get_memory(c), true)
            val sampler = newSampler(temperature)
            try {
                memScoped {
                    val bytes = text.encodeToByteArray()
                    var cap = bytes.size + 16
                    var tokens = allocArray<IntVar>(cap)
                    var count = llama_tokenize(vocab, text, bytes.size, tokens, cap, needsStartMark(text), true)
                    if (count < 0) {
                        cap = -count
                        tokens = allocArray(cap)
                        count = llama_tokenize(vocab, text, bytes.size, tokens, cap, needsStartMark(text), true)
                    }
                    if (count <= 0 || count >= CONTEXT_TOKENS - 8) error("The question is too long for this model")
                    // The question goes in pieces that fit one batch.
                    var done = 0
                    while (done < count) {
                        currentCoroutineContext().ensureActive()
                        val part = minOf(BATCH_TOKENS, count - done)
                        if (llama_decode(c, llama_batch_get_one(tokens + done, part)) != 0) {
                            error("The model failed to read the question")
                        }
                        done += part
                    }
                    generate(c, vocab, sampler, count, limit, onPiece)
                }
            } finally {
                llama_sampler_free(sampler)
            }
        }
    }

    /** Dictation with a Whisper model (whisper.cpp, in the same framework as llama.cpp). */
    private fun transcribeWithWhisper(modelId: String, wav: ByteArray): String {
        val pcm = wavSamples(wav)
        val samples =
            FloatArray(pcm.size / 2) { i ->
                ((pcm[2 * i].toInt() and 0xFF) or (pcm[2 * i + 1].toInt() shl 8)).toShort() / 32768f
            }
        if (samples.isEmpty()) return ""
        if (whisperFor != modelId) {
            releaseWhisper()
            val path = modelPath(modelId) ?: error("Model $modelId is not downloaded")
            val params = whisper_context_default_params().copy { use_gpu = !isSimulator() }
            whisperContext =
                whisper_init_from_file_with_params(path, params) ?: error("Could not load the speech model")
            whisperFor = modelId
        }
        val w = whisperContext!!
        return memScoped {
            val language = IosDictationLanguage.hint
            val languageCode = language.cstr.ptr
            val params =
                whisper_full_default_params(whisper_sampling_strategy.WHISPER_SAMPLING_GREEDY).copy {
                    n_threads = 4
                    translate = false
                    no_context = true
                    print_special = false
                    print_progress = false
                    print_realtime = false
                    print_timestamps = false
                    this.language = languageCode
                    detect_language = language == "auto"
                }
            val result = samples.usePinned { whisper_full(w, params, it.addressOf(0), samples.size) }
            if (result != 0) error("The speech model failed")
            buildString {
                for (i in 0 until whisper_full_n_segments(w)) {
                    append(whisper_full_get_segment_text(w, i)?.toKString().orEmpty())
                }
            }
                .trim()
        }
    }

    override suspend fun transcribe(modelId: String, wav: ByteArray): String = lock.withLock {
        withContext(Dispatchers.Default) {
            if (AiModels.byId(modelId)?.engine == AiEngine.WHISPER_CPP) {
                return@withContext transcribeWithWhisper(modelId, wav)
            }
            ensureLoaded(modelId)
            val m = model!!
            val c = ctx!!
            val vocab = llama_model_get_vocab(m)
            val hearing = hearingPath(modelId) ?: error("This model has no hearing file")
            val mtmd =
                mtmdContext?.takeIf { mtmdFor == modelId }
                    ?: run {
                        mtmdContext?.let { mtmd_free(it) }
                        val params =
                            mtmd_context_params_default().copy {
                                use_gpu = !isSimulator()
                                n_threads = 4
                            }
                        (mtmd_init_from_file(hearing, m, params) ?: error("Could not load the hearing file")).also {
                            mtmdContext = it
                            mtmdFor = modelId
                        }
                    }
            if (!mtmd_support_audio(mtmd)) error("This model cannot hear")
            val pcm = wavSamples(wav)
            val samples =
                FloatArray(pcm.size / 2) { i ->
                    ((pcm[2 * i].toInt() and 0xFF) or (pcm[2 * i + 1].toInt() shl 8)).toShort() / 32768f
                }
            if (samples.isEmpty()) return@withContext ""
            val marker = mtmd_default_marker()!!.toKString()
            val text = prompt(m, listOf("system" to TRANSCRIBE_RULE, "user" to "$marker\n$TRANSCRIBE_ASK"))
            llama_memory_clear(llama_get_memory(c), true)
            val sampler = newSampler(0f)
            val chunks = mtmd_input_chunks_init()
            var bitmap: CPointer<mtmd_bitmap>? = null
            try {
                memScoped {
                    val bitmapPointer = samples.usePinned {
                        mtmd_bitmap_init_from_audio(samples.size.convert(), it.addressOf(0))
                    }
                    bitmap = bitmapPointer
                    val input = alloc<mtmd_input_text>()
                    input.text = text.cstr.ptr
                    input.text_len = text.encodeToByteArray().size.convert()
                    input.add_special = needsStartMark(text)
                    input.parse_special = true
                    val bitmaps = allocArray<CPointerVar<mtmd_bitmap>>(1)
                    bitmaps[0] = bitmapPointer
                    if (mtmd_tokenize(mtmd, chunks, input.ptr, bitmaps, 1.convert()) != 0) {
                        error("Could not prepare the recording")
                    }
                    val newPast = alloc<IntVar>()
                    if (mtmd_helper_eval_chunks(mtmd, c, chunks, 0, 0, BATCH_TOKENS, true, newPast.ptr) != 0) {
                        error("The model failed to listen to the recording")
                    }
                    generate(c, vocab, sampler, newPast.value, MAX_TRANSCRIPT_TOKENS, {})
                        .trim()
                        .trim('"', '„', '”')
                        .trim()
                }
            } finally {
                bitmap?.let { mtmd_bitmap_free(it) }
                mtmd_input_chunks_free(chunks)
                llama_sampler_free(sampler)
            }
        }
    }

    /** The raw samples of a WAV recording (the "data" block). */
    private fun wavSamples(wav: ByteArray): ByteArray {
        var pos = 12
        while (pos + 8 <= wav.size) {
            val id = CharArray(4) { (wav[pos + it].toInt() and 0xFF).toChar() }.concatToString()
            var size = 0
            for (i in 3 downTo 0) size = (size shl 8) or (wav[pos + 4 + i].toInt() and 0xFF)
            if (id == "data") {
                val end = if (size <= 0 || pos + 8 + size > wav.size) wav.size else pos + 8 + size
                return wav.copyOfRange(pos + 8, end)
            }
            pos += 8 + size + (size and 1)
        }
        return ByteArray(0)
    }

    override suspend fun unloadNow() {
        lock.withLock { release() }
    }
}
