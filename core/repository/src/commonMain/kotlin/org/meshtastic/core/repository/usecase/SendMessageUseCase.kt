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
package org.meshtastic.core.repository.usecase

import co.touchlab.kermit.Logger
import org.meshtastic.core.common.util.HomoglyphCharacterStringTransformer
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.HomoglyphPrefs
import org.meshtastic.core.repository.MessageQueue
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.PlatformAnalytics
import org.meshtastic.core.repository.RadioController
import kotlin.random.Random

/**
 * Use case for sending a message over the mesh network.
 *
 * This component orchestrates the process of:
 * 1. Resolving the destination and sender information.
 * 2. Handling implicit actions for direct messages (e.g., sharing contacts, favoriting).
 * 3. Applying message transformations (e.g., homoglyph encoding).
 * 4. Persisting the outgoing message in the local history.
 * 5. Enqueuing the message for durable delivery via the platform's message queue.
 *
 * This implementation is platform-agnostic and relies on injected repositories and controllers.
 */
interface SendMessageUseCase {
    suspend operator fun invoke(
        text: String,
        contactKey: String = "0${NodeAddress.ID_BROADCAST}",
        replyId: Int? = null,
    ): Int
}

@Suppress("TooGenericExceptionCaught")
class SendMessageUseCaseImpl(
    private val nodeRepository: NodeRepository,
    private val packetRepository: PacketRepository,
    private val radioController: RadioController,
    private val homoglyphEncodingPrefs: HomoglyphPrefs,
    private val messageQueue: MessageQueue,
    private val analytics: PlatformAnalytics,
) : SendMessageUseCase {

    /**
     * Executes the send message workflow.
     *
     * @param text The plain text message to send.
     * @param contactKey The identifier of the target contact or channel (e.g., "0!ffffffff" for broadcast).
     * @param replyId Optional ID of a message being replied to.
     */
    @Suppress("NestedBlockDepth", "LongMethod", "CyclomaticComplexMethod")
    override suspend operator fun invoke(text: String, contactKey: String, replyId: Int?): Int {
        val parsedKey = ContactKey(contactKey)
        val channel = parsedKey.channelOrNull
        val dest = parsedKey.addressString

        val ourNode = nodeRepository.ourNodeInfo.value
        val fromId = ourNode?.user?.id ?: NodeAddress.ID_LOCAL

        // Auto-share-contact / auto-favorite on DM celowo wyłączone (fork MT-SW)

        // Apply homoglyph encoding
        val finalMessageText =
            if (homoglyphEncodingPrefs.homoglyphEncodingEnabled.value) {
                HomoglyphCharacterStringTransformer.optimizeUtf8StringWithHomoglyphs(text)
            } else {
                text
            }

        val packetId = Random.nextInt(1, Int.MAX_VALUE)

        val packet =
            DataPacket(dest, channel ?: 0, finalMessageText, replyId).apply {
                from = fromId
                id = packetId
                status = MessageStatus.QUEUED
            }

        try {
            // Write to the DB to immediately reflect the queued state on the UI
            val persistedId =
                packetRepository.savePacket(
                    myNodeNum = ourNode?.num ?: 0,
                    contactKey = contactKey,
                    packet = packet,
                    receivedTime = nowMillis,
                )

            // Enqueue for durable transmission via the platform-specific queue
            messageQueue.enqueue(persistedId)
            // Reported here rather than at transmission: the queue worker can run long after the RUM
            // session ended, and re-runs the send on retry.
            analytics.trackAction(
                "message_send",
                mapOf("num_bytes" to finalMessageText.length, "is_reply" to (replyId != null)),
            )
        } catch (ex: Exception) {
            Logger.e(ex) { "Failed to enqueue message packet" }
            throw ex
        }

        return packetId
    }
}
