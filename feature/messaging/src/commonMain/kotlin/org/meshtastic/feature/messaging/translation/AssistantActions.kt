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
 * A button shown under an answer of the assistant. [uri] is a deep link of the app, [highlight] (optional) the title of
 * a setting that blinks when the screen opens. With [auto] the app goes there by itself right after the answer, because
 * the user asked for exactly that ("show node X on the map"); the button stays to come back later. An action only ever
 * opens a place in the app: it never changes a setting and never sends a message.
 */
data class AssistantAction(
    val label: String,
    val uri: String,
    val highlight: String = "",
    val auto: Boolean = false,
    /** The whole caption of the button, already in the language of the answer; empty for older saved answers. */
    val caption: String = "",
)

/**
 * The actions travel inside the text of the answer as short marker lines, so they are saved with the conversation and
 * need no extra storage. [parse] cuts them out for display, [strip] removes them before the text goes back to the model.
 */
object AssistantActions {
    private val marker = Regex("""\n?\[\[akcja\|([^|\]]*)\|([^|\]]*)\|([^|\]]*)(\|auto)?(?:\|t=([^|\]]*))?]]""")

    fun encode(action: AssistantAction): String =
        "\n[[akcja|${clean(action.label)}|${clean(action.uri)}|${clean(action.highlight)}${if (action.auto) "|auto" else ""}" +
            "${if (action.caption.isNotBlank()) "|t=" + clean(action.caption) else ""}]]"

    /** The text without the markers and the actions found in it (no duplicates, in order). */
    fun parse(text: String): Pair<String, List<AssistantAction>> {
        val actions =
            marker
                .findAll(text)
                .map { AssistantAction(it.groupValues[1], it.groupValues[2], it.groupValues[3], it.groupValues[4].isNotEmpty(), it.groupValues[5]) }
                .distinct()
                .toList()
        return strip(text) to actions
    }

    fun strip(text: String): String = webMarker.replace(marker.replace(text, ""), "").trimEnd()

    // The answer used the internet: what was searched and where it came from, shown as a small note under the answer.
    // Older answers: [[internet|searches|sources]]; newer: [[internet|whole note]] already in the answer's language.
    private val webMarker = Regex("""\n?\[\[internet\|([^|\]]*)(?:\|([^|\]]*))?]]""")

    /** The note of an answer that used the internet: the whole [note], or the [queries] and [sources] of older ones. */
    data class WebInfo(val note: String, val queries: String, val sources: String)

    fun encodeWeb(note: String): String = "\n[[internet|${clean(note)}]]"

    fun web(text: String): WebInfo? = webMarker.find(text)?.let {
        if (it.groups[2] == null) WebInfo(it.groupValues[1].trim(), "", "") else WebInfo("", it.groupValues[1].trim(), it.groupValues[2].trim())
    }

    private fun clean(value: String) = value.replace('|', ' ').replace("[[", "(").replace("]]", ")").replace('\n', ' ')
}
