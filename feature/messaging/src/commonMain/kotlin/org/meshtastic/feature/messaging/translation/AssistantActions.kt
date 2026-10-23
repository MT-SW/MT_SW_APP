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
 * A button shown under an answer of the assistant. It never does anything by itself: the user taps it. [uri] is a deep
 * link of the app, [highlight] (optional) the title of a setting that blinks when the screen opens.
 */
data class AssistantAction(val label: String, val uri: String, val highlight: String = "")

/**
 * The actions travel inside the text of the answer as short marker lines, so they are saved with the conversation and
 * need no extra storage. [parse] cuts them out for display, [strip] removes them before the text goes back to the model.
 */
object AssistantActions {
    private val marker = Regex("""\n?\[\[akcja\|([^|\]]*)\|([^|\]]*)\|([^|\]]*)]]""")

    fun encode(action: AssistantAction): String =
        "\n[[akcja|${clean(action.label)}|${clean(action.uri)}|${clean(action.highlight)}]]"

    /** The text without the markers and the actions found in it (no duplicates, in order). */
    fun parse(text: String): Pair<String, List<AssistantAction>> {
        val actions =
            marker
                .findAll(text)
                .map { AssistantAction(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }
                .distinct()
                .toList()
        return strip(text) to actions
    }

    fun strip(text: String): String = marker.replace(text, "").trimEnd()

    private fun clean(value: String) = value.replace('|', ' ').replace("[[", "(").replace("]]", ")").replace('\n', ' ')
}
