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
package org.meshtastic.feature.settings.console

/** What a command printed; [clear] empties the screen first. */
data class ConsoleResult(val lines: List<String> = emptyList(), val clear: Boolean = false)

/**
 * One command of the console. [name], [summary] and [usage] are texts for the person (in the language of the app);
 * [complete] gives the choices for the argument after [args] (the words typed so far), [run] does the work.
 */
class ConsoleCommand(
    val name: String,
    val summary: String,
    val usage: String,
    val complete: (args: List<String>) -> List<String> = { emptyList() },
    val run: suspend (args: List<String>) -> ConsoleResult,
)

/** Splitting a typed line into words, and comparing words the way a person types them (no accents, any case). */
object ConsoleText {
    /** Words are split on spaces; text in double quotes is one word. */
    fun split(line: String): List<String> {
        val words = ArrayList<String>()
        val current = StringBuilder()
        var quoted = false
        var started = false
        for (ch in line) {
            when {
                ch == '"' -> {
                    quoted = !quoted
                    started = true
                }

                ch.isWhitespace() && !quoted -> {
                    if (started) words.add(current.toString())
                    current.clear()
                    started = false
                }

                else -> {
                    current.append(ch)
                    started = true
                }
            }
        }
        if (started) words.add(current.toString())
        return words
    }

    /** Lower case without Polish accents, so "połącz" can be typed as "polacz". */
    fun fold(text: String): String = buildString {
        for (ch in text.lowercase()) append(ACCENTS[ch] ?: ch)
    }

    private val ACCENTS =
        mapOf(
            'ą' to 'a',
            'ć' to 'c',
            'ę' to 'e',
            'ł' to 'l',
            'ń' to 'n',
            'ó' to 'o',
            'ś' to 's',
            'ź' to 'z',
            'ż' to 'z',
        )
}

/**
 * The one list of console commands: running a typed line, the help and the choices offered while typing all come from
 * here, so a command is written once. [unknown] is the text for a word that is no command.
 */
class ConsoleRegistry(val commands: List<ConsoleCommand>, private val unknown: suspend (String) -> String) {
    fun find(word: String): ConsoleCommand? {
        val key = ConsoleText.fold(word)
        return commands.firstOrNull { ConsoleText.fold(it.name) == key }
    }

    @Suppress("ReturnCount")
    suspend fun execute(line: String): ConsoleResult {
        val words = ConsoleText.split(line)
        if (words.isEmpty()) return ConsoleResult()
        val command = find(words.first()) ?: return ConsoleResult(listOf(unknown(words.first())))
        return command.run(words.drop(1))
    }

    /** Whole lines that the typed [input] can be completed to (the first word, or the next argument). */
    @Suppress("ReturnCount")
    fun suggestions(input: String): List<String> {
        val words = ConsoleText.split(input)
        val endsWithSpace = input.isNotEmpty() && input.last().isWhitespace()
        if (words.isEmpty()) return commands.map { it.name }
        if (words.size == 1 && !endsWithSpace) {
            val key = ConsoleText.fold(words.first())
            return commands.map { it.name }.filter { completes(it, key) }
        }
        val command = find(words.first()) ?: return emptyList()
        val typed = if (endsWithSpace) words.drop(1) else words.drop(1).dropLast(1)
        val partial = if (endsWithSpace) "" else words.last()
        val head = (listOf(words.first()) + typed).joinToString(" ")
        val key = ConsoleText.fold(partial)
        return command.complete(typed).filter { completes(it, key) }.map { "$head $it" }
    }
}

/** Whether [choice] goes on from the folded start [key] (and is not the same word already). */
private fun completes(choice: String, key: String): Boolean {
    val folded = ConsoleText.fold(choice)
    return folded.startsWith(key) && folded != key
}

/** The lines typed before, to go back and forth through with up and down. */
class ConsoleHistory(private val max: Int = MAX_ENTRIES) {
    private val entries = ArrayList<String>()
    private var position = 0

    fun add(line: String) {
        if (line.isBlank()) return
        if (entries.lastOrNull() != line) entries.add(line)
        while (entries.size > max) entries.removeAt(0)
        position = entries.size
    }

    /** The line before the one shown now, or null when there is none. */
    fun previous(): String? {
        if (entries.isEmpty() || position <= 0) return null
        position--
        return entries[position]
    }

    /** The line after the one shown now; an empty text after the newest one, null when already there. */
    fun next(): String? {
        if (position >= entries.size) return null
        position++
        return if (position == entries.size) "" else entries[position]
    }

    private companion object {
        const val MAX_ENTRIES = 100
    }
}
