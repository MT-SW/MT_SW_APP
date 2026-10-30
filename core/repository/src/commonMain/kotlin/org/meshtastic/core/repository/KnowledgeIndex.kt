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

import kotlin.math.ln

/**
 * A searchable collection of markdown texts (the user guide, the change list): they are cut into short fragments, and
 * for each question the fragments that share the most (and the rarest) words with it are returned. Word endings are
 * ignored by comparing only the first letters of a word, which is enough for Polish and English. Nothing leaves the
 * device.
 */
class KnowledgeIndex(documents: List<Document>) {

    /**
     * One text to index: [title] of the page, its [markdown], and [news] for the change list (only searched for news).
     * [base] marks a page of the technical knowledge base (antennas, flashing...): it is found only by [lookup] with
     * `includeBase`, so the manual answers never quote it.
     */
    class Document(val title: String, val markdown: String, val news: Boolean = false, val base: Boolean = false)

    private class Chunk(val title: String, val text: String, val stems: Set<String>, val news: Boolean, val base: Boolean, val keywords: String = "")

    private val chunks: List<Chunk> = documents.flatMap { split(it.title, it.markdown, it.news, it.base) }

    val isEmpty: Boolean
        get() = chunks.isEmpty()

    /** The fragments that best match [question], as one text of at most [maxChars] characters (empty if none fit). */
    fun lookup(question: String, maxChars: Int, includeBase: Boolean = false, baseOnly: Boolean = false): String {
        val terms = stems(question)
        if (terms.isEmpty() || maxChars < MIN_ROOM || chunks.isEmpty()) return ""
        val lower = question.lowercase()
        val wantsNews = NEWS_WORDS.any { lower.contains(it) }
        val pool = chunks.filter { (wantsNews || !it.news) && (includeBase || !it.base) && (!baseOnly || it.base) }
        val total = pool.size.toDouble()
        val frequency = HashMap<String, Int>()
        for (chunk in pool) for (term in terms) if (term in chunk.stems) frequency[term] = (frequency[term] ?: 0) + 1
        val ranked =
            pool
                .map { chunk ->
                    var score = 0.0
                    for (term in terms) {
                        if (term in chunk.stems) score += ln(1.0 + total / (1 + (frequency[term] ?: 0)))
                    }
                    val titleStems = stems(chunk.title)
                    val inTitle = terms.count { it in titleStems }
                    score += inTitle * TITLE_BONUS
                    // A title that holds every word of the question fits even when the word is in every page ("What is Meshtastic?").
                    if (inTitle == terms.size) score = maxOf(score, MIN_SCORE)
                    chunk to score
                }
                .filter { it.second >= MIN_SCORE }
                .sortedByDescending { it.second }
        val out = StringBuilder()
        for ((chunk, _) in ranked) {
            // The keywords of an entry of the base are given with it (the quoting counts them as part of the title).
            val block = "[${chunk.title}]\n${chunk.text}" + if (chunk.base && chunk.keywords.isNotBlank()) "\nSłowa: ${chunk.keywords}" else ""
            if (out.length + block.length + 2 > maxChars) {
                if (out.isEmpty()) out.append(block.take(maxChars))
                break
            }
            if (out.isNotEmpty()) out.append("\n\n")
            out.append(block)
        }
        return out.toString()
    }

    /** The newest entries of the change list (its first part with a list), or empty. */
    fun latestNews(maxChars: Int): String {
        val first = chunks.firstOrNull { it.news && it.text.startsWith("•") } ?: return ""
        return "[${first.title}]\n${first.text}".take(maxChars)
    }

    /** Cuts a markdown text into fragments at headings and, when a part is long, at blank lines. */
    private fun split(pageTitle: String, markdown: String, news: Boolean, base: Boolean): List<Chunk> {
        val result = ArrayList<Chunk>()
        var heading = pageTitle
        val body = StringBuilder()
        // Words from a "Słowa:" / "Keywords:" line of an entry: searched, never shown (synonyms, English and Polish names).
        val hidden = StringBuilder()
        // An entry of the knowledge base stays whole (its keywords and source line with it).
        val limit = if (base) BASE_CHUNK else MAX_CHUNK
        fun flush() {
            val text = body.toString().trim()
            val extra = hidden.toString()
            body.clear()
            hidden.clear()
            if (text.length < MIN_CHUNK) return
            var rest = text
            while (rest.isNotEmpty()) {
                var cut = rest.length
                if (rest.length > limit) {
                    // At a blank line, else at a line end, so a fragment never stops in the middle of a word.
                    cut =
                        rest.lastIndexOf("\n\n", limit).takeIf { it > MIN_CHUNK }
                            ?: rest.lastIndexOf('\n', limit).takeIf { it > MIN_CHUNK }
                            ?: rest.lastIndexOf(". ", limit).takeIf { it > MIN_CHUNK }?.plus(1)
                            ?: limit
                }
                val piece = rest.substring(0, cut).trim()
                if (piece.isNotEmpty()) {
                    val title = if (heading == pageTitle) pageTitle else "$pageTitle: $heading"
                    result += Chunk(title, piece, stems("$heading $piece $extra"), news, base, extra.trim())
                }
                rest = rest.substring(cut).trim()
            }
        }
        var inComment = false
        for (rawLine in markdown.lines()) {
            val line = rawLine.trimEnd()
            // A multi-line <!-- comment --> is for the editors of the file, not for the answers.
            if (inComment) {
                if (line.contains("-->")) inComment = false
                continue
            }
            if (line.trimStart().startsWith("<!--") && !line.contains("-->")) {
                inComment = true
                continue
            }
            if (line.startsWith("#")) {
                flush()
                heading = line.trimStart('#').trim().ifEmpty { pageTitle }
                continue
            }
            if (line.startsWith("![") || line.startsWith("---") || line.startsWith("<")) continue
            val keywords = KEYWORDS.find(line)
            if (keywords != null) {
                hidden.append(' ').append(keywords.groupValues[1])
                continue
            }
            body.append(plain(line)).append('\n')
        }
        flush()
        return result
    }

    private fun plain(line: String): String =
        line.replace(LINK, "$1").replace("**", "").replace("`", "").replace(BULLET, "• ")

    private fun stems(text: String): Set<String> =
        text
            .lowercase()
            .replace("wi-fi", "wifi")
            .replace('ą', 'a').replace('ć', 'c').replace('ę', 'e').replace('ł', 'l').replace('ń', 'n')
            .replace('ó', 'o').replace('ś', 's').replace('ź', 'z').replace('ż', 'z')
            .split(SPLIT)
            .filter { it.length >= MIN_WORD && it !in STOP_WORDS }
            .map { if (it.length > STEM_LENGTH) it.take(STEM_LENGTH) else it }
            .toSet()

    private companion object {
        const val MIN_ROOM = 200
        const val MIN_CHUNK = 40
        const val MAX_CHUNK = 900
        const val BASE_CHUNK = 1700
        const val MIN_WORD = 3
        const val STEM_LENGTH = 5
        const val MIN_SCORE = 1.5
        const val TITLE_BONUS = 1.0
        val SPLIT = Regex("[^\\p{L}\\p{N}]+")
        val LINK = Regex("\\[([^\\]]*)]\\([^)]*\\)")
        val KEYWORDS = Regex("^(?:Słowa|Keywords):\\s*(.*)$", RegexOption.IGNORE_CASE)
        val BULLET = Regex("^\\s*[-*]\\s+")
        val NEWS_WORDS = listOf("nowo", "zmian", "aktualiz", "wersj", "changelog", "what's new", "new in", "changed", "release")
        val STOP_WORDS =
            setOf(
                "jak", "jakie", "jaki", "czy", "gdzie", "mam", "mogę", "moge", "jest", "się", "sie", "jestem", "oraz",
                "dla", "nie", "tak", "ale", "lub", "albo", "który", "która", "które", "tym", "tego", "tej", "ten",
                "the", "and", "how", "what", "where", "when", "can", "does", "for", "with", "this", "that", "are",
                "you", "your", "from", "have", "has", "was", "will", "not",
            )
    }
}
