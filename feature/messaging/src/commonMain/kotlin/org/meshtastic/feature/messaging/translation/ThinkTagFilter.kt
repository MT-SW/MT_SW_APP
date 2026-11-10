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

private const val OPEN_TAG = "<think>"
private const val CLOSE_TAG = "</think>"

/**
 * Keeps the model's hidden "thinking" out of the answer. Some models write `<think>...</think>` in front of the reply
 * (empty when quiet thinking is on); the chat must show only what comes after it. Fed piece by piece, it passes on only
 * the visible part, and holds back a piece that could still turn into an opening tag.
 */
class ThinkTagFilter(private val emit: (String) -> Unit) {
    private val raw = StringBuilder()
    private var emitted = 0

    /** Takes the next piece of the model's output and passes on whatever is already certain to be visible. */
    fun feed(piece: String) {
        raw.append(piece)
        val visible = visible(raw.toString(), final = false)
        if (visible.length > emitted) {
            emit(visible.substring(emitted))
            emitted = visible.length
        }
    }

    /** The whole visible answer, once the model is done. */
    fun result(): String = visible(raw.toString(), final = true).trim()

    companion object {
        /** [text] without its thinking blocks (an unfinished block is dropped to its end). */
        fun strip(text: String): String = visible(text, final = true).trim()

        private fun visible(text: String, final: Boolean): String {
            val out = StringBuilder()
            var i = 0
            while (i < text.length) {
                val open = text.indexOf(OPEN_TAG, i)
                if (open < 0) {
                    var end = text.length
                    if (!final) end -= partialOpenTagLength(text.substring(i))
                    out.append(text, i, end)
                    i = text.length
                } else {
                    out.append(text, i, open)
                    val close = text.indexOf(CLOSE_TAG, open + OPEN_TAG.length)
                    i = if (close < 0) text.length else close + CLOSE_TAG.length
                }
            }
            return out.toString().trimStart()
        }

        /** How many characters at the end of [tail] could still become the start of an opening tag. */
        private fun partialOpenTagLength(tail: String): Int {
            val max = minOf(tail.length, OPEN_TAG.length - 1)
            for (n in max downTo 1) if (OPEN_TAG.startsWith(tail.takeLast(n))) return n
            return 0
        }
    }
}
