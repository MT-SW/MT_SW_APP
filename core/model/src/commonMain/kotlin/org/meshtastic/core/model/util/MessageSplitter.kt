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
package org.meshtastic.core.model.util

import kotlin.random.Random

/**
 * Splits a text message that is too long for a single mesh packet into several packets that fit, and reassembles them
 * back into text on the receiving side (see [parseSplitTag]).
 *
 * MT-SW fork feature: the stock TEXT_MESSAGE_APP payload has no framing for "this is part 2 of 4", so each chunk
 * carries a small visible tag of its own as the first thing in its text, e.g. `[k9 2/4] `. That tag is plain text — any
 * Meshtastic client (this fork or stock) shows it, it just looks like an odd prefix to a client that doesn't know to
 * strip and reassemble it.
 *
 * The tag has three parts:
 * - a random 2-character group id, so two long messages sent back-to-back (or delivered out of order over the mesh) can
 *   never be cross-stitched together;
 * - the chunk's own position and the total chunk count, so a receiver always knows exactly how many parts to expect and
 *   which ones (if any) are still missing — a boundary-only marker (just "this is a cut point") can't tell a
 *   still-arriving message from one that lost a chunk for good.
 */
object MessageSplitter {

    /** Characters a group id is drawn from — plain lowercase alphanumerics, unambiguous in a chat bubble. */
    private const val ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    private const val ID_LENGTH = 2

    /** Bytes reserved for the tag itself, worst case (`"[xx 99/99] "`) — see [buildTag]. */
    private const val TAG_RESERVE_BYTES = 11

    /** Never shrink a chunk's usable body below this, even if [perPacketBudget] is set unreasonably small. */
    private const val MIN_BODY_BUDGET_BYTES = 20

    /** A tagged group is always 2 or more chunks — a single chunk never gets a tag at all. */
    private const val MIN_SPLIT_TOTAL = 2

    // TAG_REGEX capture group indices (1-based, per MatchResult.groupValues) — named so parseSplitTag reads as
    // "which field", not "magic index".
    private const val TAG_GROUP_ID_INDEX = 1
    private const val TAG_CHUNK_INDEX_INDEX = 2
    private const val TAG_TOTAL_INDEX = 3
    private const val TAG_BODY_INDEX = 4

    private val TAG_REGEX = Regex("""^\[([a-z0-9]{2}) (\d{1,2})/(\d{1,2})] ([\s\S]*)$""")

    /** One received (or still-arriving) chunk of a split message, decoded from its tag. */
    data class SplitTag(val groupId: String, val index: Int, val total: Int, val body: String)

    /**
     * Splits [text] into one or more packet-ready strings.
     *
     * If [text] already fits within [perPacketBudget] UTF-8 bytes, returns it unchanged as a single-element list — no
     * tag is added, so ordinary short messages are completely unaffected.
     *
     * Otherwise, splits on whitespace so a chunk never ends mid-word, and prefixes every chunk (including the first)
     * with its tag. The one exception is a single "word" that alone is longer than one chunk can hold (e.g. a long URL
     * with no spaces) — that gets hard-split as a last resort, since there is no word boundary to use.
     */
    fun splitForMesh(text: String, perPacketBudget: Int = 200): List<String> {
        if (text.encodeToByteArray().size <= perPacketBudget) return listOf(text)

        val bodyBudget = (perPacketBudget - TAG_RESERVE_BYTES).coerceAtLeast(MIN_BODY_BUDGET_BYTES)
        val bodies = splitIntoBodies(text, bodyBudget)
        val groupId = randomGroupId()
        val total = bodies.size
        return bodies.mapIndexed { i, body -> buildTag(groupId, i + 1, total) + body }
    }

    /** Parses a single chunk's text; null if it carries no split tag (an ordinary, unsplit message). */
    fun parseSplitTag(text: String): SplitTag? {
        val match = TAG_REGEX.matchEntire(text) ?: return null
        val index = match.groupValues[TAG_CHUNK_INDEX_INDEX].toIntOrNull()
        val total = match.groupValues[TAG_TOTAL_INDEX].toIntOrNull()
        return if (index != null && total != null && total >= MIN_SPLIT_TOTAL && index in 1..total) {
            SplitTag(
                groupId = match.groupValues[TAG_GROUP_ID_INDEX],
                index = index,
                total = total,
                body = match.groupValues[TAG_BODY_INDEX],
            )
        } else {
            null
        }
    }

    private fun buildTag(groupId: String, index: Int, total: Int): String = "[$groupId $index/$total] "

    private fun randomGroupId(): String = buildString {
        repeat(ID_LENGTH) { append(ID_ALPHABET[Random.nextInt(ID_ALPHABET.length)]) }
    }

    private fun splitIntoBodies(text: String, bodyBudget: Int): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val bodies = mutableListOf<String>()
        var current = StringBuilder()
        var currentBytes = 0

        fun flush() {
            if (current.isNotEmpty()) {
                bodies += current.toString()
                current = StringBuilder()
                currentBytes = 0
            }
        }

        for (word in words) {
            val wordBytes = word.encodeToByteArray().size
            if (wordBytes > bodyBudget) {
                // No space to break on inside this single word — hard-split it by bytes as a last resort.
                flush()
                var rest = word
                while (rest.encodeToByteArray().size > bodyBudget) {
                    val piece = takeMaxUtf8Bytes(rest, bodyBudget)
                    bodies += piece
                    rest = rest.substring(piece.length)
                }
                current = StringBuilder(rest)
                currentBytes = rest.encodeToByteArray().size
                continue
            }
            val withSpace = if (current.isEmpty()) wordBytes else currentBytes + 1 + wordBytes
            if (withSpace > bodyBudget) {
                flush()
                current.append(word)
                currentBytes = wordBytes
            } else {
                if (current.isNotEmpty()) {
                    current.append(' ')
                    currentBytes += 1
                }
                current.append(word)
                currentBytes += wordBytes
            }
        }
        flush()
        return bodies
    }

    /** The longest prefix of [s] (whole characters only — never splits a surrogate pair) within [maxBytes]. */
    private fun takeMaxUtf8Bytes(s: String, maxBytes: Int): String {
        var end = 0
        var bytes = 0
        while (end < s.length) {
            val isSurrogatePair = s[end].isHighSurrogate() && end + 1 < s.length && s[end + 1].isLowSurrogate()
            val charLen = if (isSurrogatePair) 2 else 1
            val pieceBytes = s.substring(end, end + charLen).encodeToByteArray().size
            if (bytes + pieceBytes > maxBytes) break
            bytes += pieceBytes
            end += charLen
        }
        // Never return an empty string (would loop forever in the caller) — take at least one char.
        return if (end == 0 && s.isNotEmpty()) s.substring(0, 1) else s.substring(0, end)
    }
}
