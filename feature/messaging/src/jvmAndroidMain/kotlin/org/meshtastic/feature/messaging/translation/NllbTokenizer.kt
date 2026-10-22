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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.text.Normalizer

/**
 * Tokenizer of the NLLB model, read from its `tokenizer.json` (byte-pair encoding over SentencePiece-style pieces, with
 * "▁" marking a word start). Text is normalised with NFKC, which is what the model's own normaliser is based on.
 */
internal class NllbTokenizer
private constructor(
    private val vocab: HashMap<String, Int>,
    private val idToToken: Array<String?>,
    private val ranks: HashMap<String, Int>,
) {
    fun languageId(code: String): Int? = vocab[code]

    /** Builds the encoder input: source language token, the text pieces, end of sentence. */
    fun encode(text: String, sourceLanguageId: Int): LongArray {
        val ids = ArrayList<Int>()
        ids.add(sourceLanguageId)
        val normalized = normalize(text)
        if (normalized.isNotEmpty()) {
            val marked = SPACE_MARK + normalized.replace(' ', SPACE_MARK)
            for (word in splitWords(marked)) {
                for (piece in bpe(word)) {
                    val id = vocab[piece] ?: UNK
                    // Neighbouring unknown pieces are fused into one, as the reference tokenizer does.
                    if (id == UNK && ids.last() == UNK) continue
                    ids.add(id)
                    if (ids.size >= MAX_INPUT_TOKENS - 1) break
                }
                if (ids.size >= MAX_INPUT_TOKENS - 1) break
            }
        }
        ids.add(EOS)
        return LongArray(ids.size) { ids[it].toLong() }
    }

    fun decode(ids: List<Int>): String {
        val sb = StringBuilder()
        for (id in ids) {
            if (id <= UNK || id >= FIRST_LANGUAGE_ID) continue
            sb.append(idToToken.getOrNull(id) ?: continue)
        }
        return sb.toString().replace(SPACE_MARK, ' ').trim()
    }

    private fun normalize(text: String): String {
        val nfkc = Normalizer.normalize(text, Normalizer.Form.NFKC)
        val sb = StringBuilder(nfkc.length)
        var lastSpace = true
        for (c in nfkc) {
            val isSpace = c.isWhitespace() || c.isISOControl()
            if (isSpace) {
                if (!lastSpace) sb.append(' ')
                lastSpace = true
            } else {
                sb.append(c)
                lastSpace = false
            }
        }
        return sb.toString().trim()
    }

    private fun splitWords(marked: String): List<String> {
        val words = ArrayList<String>()
        var start = 0
        for (i in 1 until marked.length) {
            if (marked[i] == SPACE_MARK) {
                words.add(marked.substring(start, i))
                start = i
            }
        }
        words.add(marked.substring(start))
        return words
    }

    private fun bpe(word: String): List<String> {
        val symbols = ArrayList<String>()
        var i = 0
        while (i < word.length) {
            val n = Character.charCount(word.codePointAt(i))
            symbols.add(word.substring(i, i + n))
            i += n
        }
        while (symbols.size > 1) {
            var best = Int.MAX_VALUE
            var bestIndex = -1
            for (j in 0 until symbols.size - 1) {
                val rank = ranks[symbols[j] + " " + symbols[j + 1]]
                if (rank != null && rank < best) {
                    best = rank
                    bestIndex = j
                }
            }
            if (bestIndex < 0) break
            symbols[bestIndex] = symbols[bestIndex] + symbols[bestIndex + 1]
            symbols.removeAt(bestIndex + 1)
        }
        return symbols
    }

    companion object {
        const val SPACE_MARK = '▁'
        const val UNK = 3
        const val EOS = 2
        const val PAD = 1
        const val FIRST_LANGUAGE_ID = 256001
        private const val MAX_INPUT_TOKENS = 256

        fun load(file: File): NllbTokenizer {
            val root = Json.parseToJsonElement(file.readText()).jsonObject
            val model = root["model"]!!.jsonObject
            val vocabJson: JsonObject = model["vocab"]!!.jsonObject
            val vocab = HashMap<String, Int>(vocabJson.size * 2)
            var maxId = 0
            for ((token, idElement) in vocabJson) {
                val id = idElement.jsonPrimitive.int
                vocab[token] = id
                if (id > maxId) maxId = id
            }
            val idToToken = arrayOfNulls<String>(maxId + 1)
            for ((token, id) in vocab) idToToken[id] = token
            val mergesJson: JsonArray = model["merges"]!!.jsonArray
            val ranks = HashMap<String, Int>(mergesJson.size * 2)
            for (index in mergesJson.indices) {
                val element = mergesJson[index]
                val key =
                    if (element is JsonPrimitive) {
                        element.content
                    } else {
                        element.jsonArray.joinToString(" ") { it.jsonPrimitive.content }
                    }
                ranks[key] = index
            }
            return NllbTokenizer(vocab, idToToken, ranks)
        }
    }
}
