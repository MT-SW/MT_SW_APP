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
package org.meshtastic.core.repository

/**
 * What the offline assistant knows about the app itself: the user guide and the list of changes that ship inside the
 * app (so it works without internet). The assistant asks for the fragments that fit one question.
 */
interface AssistantKnowledge {
    /**
     * Returns the fragments of the guide (and, for questions about news, the change list) that best match [question],
     * as one plain text of at most [maxChars] characters, or an empty text when nothing fits.
     */
    suspend fun lookup(question: String, maxChars: Int): String

    /** The newest entries of the change list, as plain text of at most [maxChars] characters, or empty. */
    suspend fun latestNews(maxChars: Int): String = ""

    /** Like [lookup], but in the manual of [language] ("pl", "en"); null picks the language from the question. */
    suspend fun lookup(question: String, maxChars: Int, language: String?): String = lookup(question, maxChars)

    /**
     * Like [lookup], but also searches the technical knowledge base (antennas, filters, flashing, building, settings);
     * every entry of it ends with a "Źródło:/Source:" line. Empty when nothing fits.
     */
    suspend fun lookupBase(question: String, maxChars: Int, language: String?): String = ""

    /** The short core facts about MT_SW (network settings, parts of the project) in [language], given with every answer. */
    suspend fun coreFacts(language: String): String = ""

    /** The assistant's own texts in [language] ("pl", "en") by resource key, or empty when not bundled. */
    suspend fun answerStrings(language: String): Map<String, String> = emptyMap()
}

/** The "## PL" or "## EN" part of a two-language knowledge file (the whole text when it has no such parts). */
fun knowledgeSection(text: String, language: String): String {
    val marker = "## " + (if (language == "pl") "PL" else "EN")
    val start = text.indexOf(marker)
    if (start < 0) return text.trim()
    val rest = text.substring(start + marker.length)
    val end = rest.indexOf("\n## ")
    return (if (end >= 0) rest.substring(0, end) else rest).trim()
}
