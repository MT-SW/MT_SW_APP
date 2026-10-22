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

/**
 * A very small offline language guesser for short chat messages: script first, then a handful of very common words
 * per language. It only has to tell the languages in [TranslatorLanguages] apart well enough to pick the model's source
 * language; when unsure it returns null and the UI asks the user to choose.
 */
object LanguageGuesser {
    private val words: Map<String, Set<String>> =
        mapOf(
            "pol_Latn" to
                setOf(
                    "i", "w", "nie", "na", "to", "się", "że", "jest", "z", "do", "jak", "co", "ale", "czy", "tak", "po",
                    "mam", "jestem", "dzień", "dobry", "cześć", "proszę", "dzięki", "też", "już", "tu", "tam", "ktoś",
                    "mnie", "który", "bardzo", "dla", "oraz", "ja", "my", "wy", "będzie", "żeby",
                ),
            "eng_Latn" to
                setOf(
                    "the", "and", "to", "of", "is", "in", "it", "you", "that", "i", "for", "on", "are", "with", "this",
                    "have", "not", "my", "can", "hear", "me", "hello", "good", "morning", "thanks", "please", "we",
                    "be", "at", "was", "your", "but", "just", "what", "how",
                ),
            "deu_Latn" to
                setOf(
                    "und", "der", "die", "das", "ist", "nicht", "ich", "ein", "eine", "zu", "mit", "auf", "für", "den",
                    "dem", "es", "sie", "wir", "auch", "sind", "habe", "gestern", "guten", "danke", "hallo", "aber",
                    "wie", "von", "dass",
                ),
            "ces_Latn" to
                setOf(
                    "a", "je", "se", "na", "že", "to", "v", "s", "jsem", "jsi", "ale", "ahoj", "nebo", "jak", "ne",
                    "mám", "mě", "jsou", "dobrý", "děkuji", "už", "tady", "kde", "slyšíte", "mám",
                ),
            "slk_Latn" to
                setOf(
                    "a", "je", "sa", "na", "že", "to", "v", "s", "som", "ale", "ahoj", "alebo", "ako", "nie", "mám",
                    "ma", "sú", "dobrý", "ďakujem", "už", "tu", "kde", "počujete",
                ),
            "fra_Latn" to
                setOf(
                    "le", "la", "les", "de", "des", "et", "est", "un", "une", "que", "pas", "pour", "je", "vous",
                    "nous", "avec", "bonjour", "merci", "sur", "dans", "ce", "ça",
                ),
            "spa_Latn" to
                setOf(
                    "el", "la", "los", "las", "de", "y", "es", "un", "una", "que", "no", "por", "para", "con", "hola",
                    "gracias", "pero", "está", "muy", "yo", "se",
                ),
            "ita_Latn" to
                setOf(
                    "il", "lo", "gli", "di", "e", "è", "un", "una", "che", "non", "per", "con", "ciao", "grazie", "ma",
                    "sono", "molto", "io", "si",
                ),
            "nld_Latn" to
                setOf(
                    "de", "het", "een", "en", "is", "niet", "ik", "je", "dat", "van", "op", "te", "met", "voor",
                    "hallo", "dank", "maar", "ook", "zijn",
                ),
            "rus_Cyrl" to
                setOf(
                    "и", "в", "не", "на", "я", "что", "это", "как", "он", "с", "но", "привет", "спасибо", "да", "нет",
                    "есть", "мне", "вы",
                ),
            "ukr_Cyrl" to
                setOf(
                    "і", "в", "не", "на", "я", "що", "це", "як", "він", "з", "але", "привіт", "дякую", "так", "ні",
                    "є", "мені", "ви", "чи",
                ),
        )

    /** Returns the model code of the most likely language, or null when it cannot tell. */
    fun guess(text: String): String? {
        scriptOf(text)?.let { return it }
        val lower = text.lowercase()
        // Characters that only one language in the set uses settle the common Cyrillic and Czech/Slovak cases fast.
        if (lower.any { it in "іїєґ" }) return "ukr_Cyrl"
        if (lower.any { it in "ыэъё" }) return "rus_Cyrl"
        if (lower.any { it in "ěřů" }) return "ces_Latn"
        if (lower.any { it in "ľĺôŕ" }) return "slk_Latn"
        val tokens = lower.split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        var best: String? = null
        var bestScore = 0
        var second = 0
        for ((code, set) in words) {
            var score = tokens.count { it in set }
            if (code == "pol_Latn" && lower.any { it in "ąćęłńóśźż" }) score += 2
            if (code == "deu_Latn" && lower.any { it in "äöüß" }) score += 1
            if (score > bestScore) {
                second = bestScore
                bestScore = score
                best = code
            } else if (score > second) {
                second = score
            }
        }
        return if (bestScore >= 2 && bestScore > second) best else null
    }

    private fun scriptOf(text: String): String? {
        var greek = 0
        var arabic = 0
        var hebrew = 0
        var kana = 0
        var hangul = 0
        var han = 0
        for (c in text) {
            when (c.code) {
                in 0x0370..0x03FF -> greek++
                in 0x0600..0x06FF -> arabic++
                in 0x0590..0x05FF -> hebrew++
                in 0x3040..0x30FF -> kana++
                in 0xAC00..0xD7AF -> hangul++
                in 0x4E00..0x9FFF -> han++
            }
        }
        return when {
            kana > 0 -> "jpn_Jpan"
            hangul > 0 -> "kor_Hang"
            han > 0 -> "zho_Hans"
            arabic > 0 -> "arb_Arab"
            hebrew > 0 -> "heb_Hebr"
            greek > 0 -> "ell_Grek"
            else -> null
        }
    }
}

/** Cuts a chat message into pieces the model handles well: it translates one sentence at a time. */
object SentenceSplitter {
    private const val MAX_PIECE = 300

    /** Lines are kept apart (blank ones dropped); every line becomes a list of sentences. */
    fun splitLines(text: String): List<List<String>> = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.map {
        splitSentences(it)
    }

    fun splitSentences(line: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        for (i in line.indices) {
            val c = line[i]
            cur.append(c)
            val endsSentence = c == '.' || c == '!' || c == '?' || c == '…'
            val nextIsSpace = i + 1 >= line.length || line[i + 1].isWhitespace()
            if (endsSentence && nextIsSpace) {
                out.add(cur.toString().trim())
                cur.clear()
            }
        }
        if (cur.isNotBlank()) out.add(cur.toString().trim())
        return out.flatMap { cutLong(it) }.filter { it.isNotBlank() }
    }

    private fun cutLong(piece: String): List<String> {
        if (piece.length <= MAX_PIECE) return listOf(piece)
        val out = ArrayList<String>()
        var rest = piece
        while (rest.length > MAX_PIECE) {
            var cut = rest.lastIndexOf(' ', MAX_PIECE)
            if (cut <= 0) cut = MAX_PIECE
            out.add(rest.substring(0, cut).trim())
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) out.add(rest)
        return out
    }
}
