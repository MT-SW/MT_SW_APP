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
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
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
import llama.llama_context
import llama.llama_context_default_params
import llama.llama_decode
import llama.llama_free
import llama.llama_get_memory
import llama.llama_init_from_model
import llama.llama_memory_clear
import llama.llama_model
import llama.llama_model_chat_template
import llama.llama_model_default_params
import llama.llama_model_free
import llama.llama_model_get_vocab
import llama.llama_model_load_from_file
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
import platform.Foundation.NSProcessInfo

private const val CONTEXT_TOKENS = 4096
private const val BATCH_TOKENS = 512
private const val GPU_LAYERS = 99
private const val MAX_PIECE_BYTES = 256

/** On-device language model for the iPhone: llama.cpp linked into the app (see scripts/build-llama-ios.sh). */
@OptIn(ExperimentalForeignApi::class)
class IosLlamaEngine(private val modelPath: (String) -> String?) : AiTextEngine {
    private val lock = Mutex()
    private var loadedId: String? = null
    private var model: CPointer<llama_model>? = null
    private var ctx: CPointer<llama_context>? = null
    private var backend = ""

    override val activeBackend: String get() = backend

    override fun contextTokens(modelId: String): Int = CONTEXT_TOKENS

    private fun isSimulator() = NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null

    private fun release() {
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
        val loaded = llama_model_load_from_file(path, llama_model_default_params().copy { n_gpu_layers = gpu })
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
            val sampler = llama_sampler_chain_init(llama_sampler_chain_default_params())!!
            if (temperature > 0f) {
                llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature))
                llama_sampler_chain_add(sampler, llama_sampler_init_dist(0xFFFFFFFFu))
            } else {
                llama_sampler_chain_add(sampler, llama_sampler_init_greedy())
            }
            try {
                memScoped {
                    val bytes = text.encodeToByteArray()
                    var cap = bytes.size + 16
                    var tokens = allocArray<IntVar>(cap)
                    var count = llama_tokenize(vocab, text, bytes.size, tokens, cap, true, true)
                    if (count < 0) {
                        cap = -count
                        tokens = allocArray(cap)
                        count = llama_tokenize(vocab, text, bytes.size, tokens, cap, true, true)
                    }
                    if (count <= 0 || count >= CONTEXT_TOKENS - 8) error("The question is too long for this model")
                    // The question goes in pieces that fit one batch.
                    var done = 0
                    while (done < count) {
                        currentCoroutineContext().ensureActive()
                        val part = minOf(BATCH_TOKENS, count - done)
                        if (llama_decode(c, llama_batch_get_one(tokens + done, part)) != 0) error("The model failed to read the question")
                        done += part
                    }
                    val one = alloc<IntVar>()
                    val piece = allocArray<ByteVar>(MAX_PIECE_BYTES)
                    val out = ArrayList<Byte>()
                    var emitted = 0
                    var produced = 0
                    var position = count
                    while (produced < limit && position < CONTEXT_TOKENS - 1) {
                        currentCoroutineContext().ensureActive()
                        val token = llama_sampler_sample(sampler, c, -1)
                        if (llama_vocab_is_eog(vocab, token)) break
                        val len = llama_token_to_piece(vocab, token, piece, MAX_PIECE_BYTES, 0, true)
                        for (i in 0 until len) out.add(piece[i])
                        val decoded = out.toByteArray().decodeToString()
                        if (!decoded.endsWith('�') && decoded.length > emitted) {
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
            } finally {
                llama_sampler_free(sampler)
            }
        }
    }

    override suspend fun transcribe(modelId: String, wav: ByteArray): String =
        error("Speech recognition is not available on the iPhone yet")

    override suspend fun unloadNow() {
        lock.withLock { release() }
    }
}
