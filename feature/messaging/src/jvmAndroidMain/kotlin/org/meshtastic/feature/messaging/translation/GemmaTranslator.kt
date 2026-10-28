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

import org.meshtastic.core.repository.AiModels
import java.util.Locale

/**
 * Translates with a language model: a plain instruction for the general models, the model's own prompt for
 * TranslateGemma (which was trained for exactly one wording).
 */
class GemmaTranslator(private val engine: AiTextEngine) {

    suspend fun translate(modelId: String, text: String, sourceNllb: String, targetNllb: String): String {
        val source = englishName(sourceNllb)
        val target = englishName(targetNllb)
        val from = if (source != null) "$source text" else "text"
        val into = target ?: targetNllb
        val prompt =
            if (modelId == AiModels.TG_4B || modelId == AiModels.TG_12B) {
                val sourceCode = TranslatorLanguages.fromNllb(sourceNllb)?.iso ?: "und"
                val targetCode = TranslatorLanguages.fromNllb(targetNllb)?.iso ?: targetNllb
                val src = source ?: "source language"
                val tgt = target ?: targetNllb
                "You are a professional $src ($sourceCode) to $tgt ($targetCode) translator. Your goal is to accurately " +
                    "convey the meaning and nuances of the original $src text while adhering to $tgt grammar, vocabulary, " +
                    "and cultural sensitivities.\nProduce only the $tgt translation, without any additional explanations " +
                    "or commentary. Please translate the following $src text into $tgt:\n\n\n$text"
            } else {
                "Translate the following $from into $into. " +
                    "Keep names of people, places, mesh nodes, numbers, URLs and technical terms exactly as they are; " +
                    "do not translate proper names. " +
                    "Output only the translation, without quotes, notes or explanations.\n\n$text"
            }
        val reply =
            engine.chat(
                modelId = modelId,
                messages = listOf("user" to prompt),
                maxTokens = (text.length / 2 + EXTRA_TOKENS).coerceIn(MIN_TOKENS, MAX_TOKENS),
                temperature = 0f,
            )
        return reply.ifBlank { text }
    }

    private fun englishName(nllb: String): String? {
        val iso = TranslatorLanguages.fromNllb(nllb)?.iso ?: return null
        return Locale.forLanguageTag(iso).getDisplayLanguage(Locale.ENGLISH).takeIf { it.isNotBlank() }
    }

    private companion object {
        const val EXTRA_TOKENS = 96
        const val MIN_TOKENS = 96
        const val MAX_TOKENS = 1024
    }
}
