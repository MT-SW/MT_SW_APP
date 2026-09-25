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
package org.meshtastic.feature.messaging

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.util.MessageSplitter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.message_split_incomplete
import org.meshtastic.core.resources.message_split_receiving
import org.meshtastic.core.resources.message_split_sending

// A group stops waiting for its missing chunks after this long and just shows what arrived, permanently — mirrors
// how a real SMS app eventually gives up on a concatenated message that never fully landed.
private const val SPLIT_REASSEMBLY_TIMEOUT_MILLIS = 3 * 60 * 1000L

/** Everything the merged bubble needs, for the one chunk in its group that hosts it — see [buildSplitReassembly]. */
internal data class SplitGroupInfo(
    val mergedBody: String,
    val haveCount: Int,
    val total: Int,
    val isComplete: Boolean,
    val timedOut: Boolean,
    val replyId: Int?,
    val originalMessage: Message?,
    val packetId: Int,
    /** Whether this group is a message we're sending (all its chunks originated on this device) vs. receiving. */
    val fromLocal: Boolean,
)

internal data class SplitReassembly(
    val suppressed: Set<Long> = emptySet(),
    val groups: Map<Long, SplitGroupInfo> = emptyMap(),
)

/**
 * Collapses MessageSplitter's tagged chunks back into one logical message, purely as a display-time pass over messages
 * already loaded — no separate table, matching how this codebase already decodes Network Health / relay names live from
 * stored packets rather than persisting derived rows.
 *
 * Chunks sharing a sender and a [MessageSplitter.SplitTag.groupId] become one group. The chunk with the highest index
 * received so far "hosts" the merged bubble (all others in the group are [suppressed] from rendering entirely) — that
 * is always the chronologically newest chunk of the group we have, so the bubble naturally sits at the right point in
 * the timeline and keeps advancing as later chunks arrive.
 */
internal fun buildSplitReassembly(messages: List<Message>): SplitReassembly {
    data class GroupKey(val nodeNum: Int, val fromLocal: Boolean, val groupId: String)

    val chunksByGroup = LinkedHashMap<GroupKey, MutableList<Pair<Message, MessageSplitter.SplitTag>>>()
    for (message in messages) {
        val tag = MessageSplitter.parseSplitTag(message.text) ?: continue
        val key = GroupKey(message.node.num, message.fromLocal, tag.groupId)
        chunksByGroup.getOrPut(key) { mutableListOf() }.add(message to tag)
    }
    if (chunksByGroup.isEmpty()) return SplitReassembly()

    val suppressed = mutableSetOf<Long>()
    val groups = mutableMapOf<Long, SplitGroupInfo>()
    val now = nowMillis

    for (entries in chunksByGroup.values) {
        val byIndex = entries.associateBy({ it.second.index }, { it })
        val total = entries.first().second.total
        val hostEntry = entries.maxWith(compareBy({ it.second.index }, { it.first.displayTime }))
        val mergedBody = (1..total).mapNotNull { byIndex[it]?.second?.body }.joinToString(" ")
        val haveCount = byIndex.size
        val firstReceivedAt = entries.minOf { it.first.displayTime }
        val isComplete = haveCount == total
        // Chunk 1 alone carries the reply reference (see SendMessageUseCaseImpl) — may not have arrived yet.
        val firstChunk = byIndex[1]?.first

        groups[hostEntry.first.uuid] =
            SplitGroupInfo(
                mergedBody = mergedBody,
                haveCount = haveCount,
                total = total,
                isComplete = isComplete,
                timedOut = !isComplete && (now - firstReceivedAt) > SPLIT_REASSEMBLY_TIMEOUT_MILLIS,
                replyId = firstChunk?.replyId,
                originalMessage = firstChunk?.originalMessage,
                packetId = firstChunk?.packetId ?: hostEntry.first.packetId,
                fromLocal = hostEntry.first.fromLocal,
            )
        entries.forEach { (message, _) -> if (message.uuid != hostEntry.first.uuid) suppressed += message.uuid }
    }

    return SplitReassembly(suppressed = suppressed, groups = groups)
}

/**
 * The text to actually show for a merged bubble: the reassembled body, plus a small status line while the group isn't
 * complete yet (still arriving, or — past [SPLIT_REASSEMBLY_TIMEOUT_MILLIS] — given up on for good).
 */
@Composable
internal fun SplitGroupInfo.displayText(): String {
    if (isComplete) return mergedBody
    val statusLine =
        when {
            timedOut -> stringResource(Res.string.message_split_incomplete, total - haveCount, total)
            // Direction matters here: while our own outgoing chunks are still being paced out, this is *our* send
            // still in progress, not something arriving from the other end -- showing "Receiving..." on your own
            // message reads as if the app had the direction backwards.
            fromLocal -> stringResource(Res.string.message_split_sending, haveCount, total)
            else -> stringResource(Res.string.message_split_receiving, haveCount, total)
        }
    return "$mergedBody\n\n$statusLine"
}
