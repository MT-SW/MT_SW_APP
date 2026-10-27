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
package org.meshtastic.feature.docs.data

import kotlinx.coroutines.CancellationException
import meshtasticandroid.feature.docs.generated.resources.Res

/** The three tiers of the knowledge base; the folder of a file decides the tier. */
enum class KbTier {
    /** Closest to our network: Meshtastic, the radio, antennas, settings (the files directly in `base/`). */
    MAIN,

    /** A bit further: IT, electronics, Wi-Fi, amateur radio, other networks (`base/pokrewne/`). */
    RELATED,

    /** Everything else in life (`base/offtop/`). */
    OFFTOP,
}

/** One entry of the knowledge base: a question, its text, the hidden search words and the source. */
data class KbEntry(
    val id: Int,
    val tier: KbTier,
    val topic: String,
    val question: String,
    val body: String,
    val source: String,
    /** Lower-case text without Polish diacritics that the search looks through (question, text, hidden words, topic). */
    val haystack: String,
    /** The same for the question alone, so that matches in the question rank first. */
    val questionKey: String,
)

/** Reads the knowledge base files (the same ones the assistant searches) into [KbEntry] items. */
object KnowledgeBaseParser {
    private val ENTRY_START = Regex("(?m)^### ")

    fun tierOf(file: String): KbTier =
        when {
            file.startsWith("offtop/") -> KbTier.OFFTOP
            file.startsWith("pokrewne/") -> KbTier.RELATED
            else -> KbTier.MAIN
        }

    /** Parses one file ("# Title" then "### question" entries). [firstId] numbers the entries. */
    fun parse(file: String, text: String, firstId: Int): List<KbEntry> {
        val tier = tierOf(file)
        val topic = text.lineSequence().firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim() ?: file
        var id = firstId
        return text.split(ENTRY_START).drop(1).mapNotNull { part ->
            val lines = part.trim().lines()
            val question = lines.firstOrNull()?.trim().orEmpty()
            if (question.isEmpty()) return@mapNotNull null
            val body = StringBuilder()
            var keywords = ""
            var source = ""
            for (line in lines.drop(1)) {
                when {
                    line.startsWith("Słowa:") -> keywords = line.removePrefix("Słowa:").trim()
                    line.startsWith("Źródło:") -> source = line.removePrefix("Źródło:").trim()
                    else -> body.append(line).append('\n')
                }
            }
            val text = body.toString().trim()
            KbEntry(
                id = id++,
                tier = tier,
                topic = topic,
                question = question,
                body = text,
                source = source,
                haystack = searchKey("$question $text $keywords $topic"),
                questionKey = searchKey(question),
            )
        }
    }

    /** Lower case and without Polish diacritics, so that "zmija" finds "żmija". */
    fun searchKey(text: String): String =
        buildString(text.length) {
            for (c in text.lowercase()) {
                append(
                    when (c) {
                        'ą' -> 'a'
                        'ć' -> 'c'
                        'ę' -> 'e'
                        'ł' -> 'l'
                        'ń' -> 'n'
                        'ó' -> 'o'
                        'ś' -> 's'
                        'ź', 'ż' -> 'z'
                        else -> c
                    },
                )
            }
        }

    /** Entries matching every word of [query] (all of them when it is blank), best matches in the question first. */
    fun filter(entries: List<KbEntry>, query: String, tier: KbTier?): List<KbEntry> {
        val words = searchKey(query).split(' ', ',', '.', '?').filter { it.isNotBlank() }
        val inTier = if (tier == null) entries else entries.filter { it.tier == tier }
        if (words.isEmpty()) return inTier
        return inTier
            .filter { e -> words.all { e.haystack.contains(it) } }
            .sortedByDescending { e -> words.count { e.questionKey.contains(it) } }
    }
}

/** Loads the packaged knowledge base: `files/knowledge/base/index.txt` lists its files (compose resources cannot list a folder). */
object KnowledgeBaseLoader {
    private const val BASE_PATH = "files/knowledge/base"

    suspend fun load(): List<KbEntry> {
        val listing = readText("$BASE_PATH/index.txt").lines().map { it.trim() }.filter { it.endsWith(".md") }
        val all = ArrayList<KbEntry>()
        for (file in listing) {
            val text = readText("$BASE_PATH/$file")
            if (text.isBlank()) continue
            all += KnowledgeBaseParser.parse(file, text, all.size)
        }
        return all
    }

    private suspend fun readText(path: String): String =
        try {
            Res.readBytes(path).decodeToString()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ""
        }
}
