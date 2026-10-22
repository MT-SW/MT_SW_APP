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

import java.util.Locale

/** Translates with a general Gemma model: a plain instruction, no special translation model needed. */
class GemmaTranslator(private val engine: LiteRtEngine) {

    suspend fun translate(modelId: String, text: String, sourceNllb: String, targetNllb: String): String {
        val source = englishName(sourceNllb)
        val target = englishName(targetNllb)
        val from = if (source != null) "$source text" else "text"
        val into = target ?: targetNllb
        val instruction =
            "Translate the following $from into $into. " +
                "Keep names of people, places, mesh nodes, numbers, URLs and technical terms exactly as they are; " +
                "do not translate proper names. " +
                "Output only the translation, without quotes, notes or explanations."
        val reply =
            engine.chat(
                modelId = modelId,
                messages = listOf("user" to "$instruction\n\n$text"),
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
