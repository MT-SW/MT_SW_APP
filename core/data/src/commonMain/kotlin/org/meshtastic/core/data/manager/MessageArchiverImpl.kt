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
package org.meshtastic.core.data.manager

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import org.meshtastic.core.common.crypto.ArchiveCrypto
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.repository.FolderService
import org.meshtastic.core.repository.MessageArchiveConfig
import org.meshtastic.core.repository.MessageArchiveMonitor
import org.meshtastic.core.repository.MessageArchivePrefs
import org.meshtastic.core.repository.MessageArchiveState
import org.meshtastic.core.repository.MessageArchiver
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.archive_channel_n
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.unknown_username
import kotlin.time.Instant

/**
 * Keeps a copy of the text messages in a folder the user chose: one file per day (or per conversation and day), in the
 * format they picked. Every run rewrites the files of today and yesterday from the message history, so nothing is
 * missed when the app was closed for a while, and a file is written only when its content changed. It does not need
 * the radio: it reads the message history stored on the device.
 */
@Single
class MessageArchiverImpl(
    private val packets: PacketRepository,
    private val nodes: NodeRepository,
    private val folders: FolderService,
    private val prefs: MessageArchivePrefs,
) : MessageArchiver,
    MessageArchiveMonitor {
    private val mutableState = MutableStateFlow(MessageArchiveState())
    override val state: StateFlow<MessageArchiveState> = mutableState.asStateFlow()
    private val mutex = Mutex()

    /** File name → hash of the content last written there, so an unchanged file is not written again. */
    private val written = mutableMapOf<String, Int>()

    override fun start(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                try {
                    val config = prefs.config.value
                    val dueMillis = mutableState.value.lastRunMillis + config.intervalMinutes * MILLIS_PER_MINUTE
                    if (config.enabled && config.folder.isNotBlank() && nowMillis >= dueMillis) archiveNow()
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Logger.e(e) { "Message archive failed" }
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    override suspend fun archiveNow(): Boolean = mutex.withLock {
        val config = prefs.config.value
        if (config.folder.isBlank()) return false
        mutableState.update { it.copy(running = true) }
        var ok = true
        var files = 0
        try {
            for ((plainName, content) in buildFiles(config)) {
                // The hash covers the password too, so a new password rewrites the files.
                val hash = (content + config.password).hashCode()
                val name = if (config.encrypted) "$plainName.$ENCRYPTED_EXTENSION" else plainName
                if (written[name] == hash) continue
                val bytes =
                    if (config.encrypted) ArchiveCrypto.encrypt(config.password, content.encodeToByteArray()) else null
                val done =
                    folders.writeFile(config.folder, name) { sink ->
                        if (bytes != null) sink.write(bytes) else sink.writeUtf8(content)
                    }
                if (done) {
                    written[name] = hash
                    files++
                } else {
                    ok = false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(e) { "Message archive failed" }
            ok = false
        } finally {
            mutableState.value =
                MessageArchiveState(running = false, lastRunMillis = nowMillis, lastFileCount = files, lastFailed = !ok)
        }
        ok
    }

    /** File name → content, for the messages of today and yesterday. */
    private suspend fun buildFiles(config: MessageArchiveConfig): Map<String, String> {
        val zone = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone).date
        val days = setOf(today.toString(), today.minus(1, DateTimeUnit.DAY).toString())
        val unknown = getStringSuspend(Res.string.unknown_username)
        val entries = mutableListOf<Pair<Long, ArchiveEntry>>()
        for (contact in packets.getContacts().first().keys) {
            val title = conversationTitle(contact)
            val messages =
                packets
                    .getMessagesFrom(
                        contact = contact,
                        limit = MESSAGES_PER_CONVERSATION,
                        includeFiltered = false,
                        getNode = { userId -> nodes.getNode(userId ?: "") },
                    )
                    .first()
            for (message in messages) {
                val local = Instant.fromEpochMilliseconds(message.displayTime).toLocalDateTime(zone)
                val date = local.date.toString()
                if (date !in days) continue
                val sender = message.node.effectiveLongName.takeIf { it.isNotBlank() } ?: unknown
                val time = local.time.toString().substringBefore('.')
                entries += message.displayTime to ArchiveEntry(date, time, title, sender, message.text)
            }
        }
        val extension = archiveExtension(config.format)
        return entries
            .sortedBy { it.first }
            .map { it.second }
            .groupBy {
                if (config.perConversation) {
                    "wiadomosci_${it.date}_${safeFileName(it.conversation)}.$extension"
                } else {
                    "wiadomosci_${it.date}.$extension"
                }
            }
            .mapValues { (_, list) -> formatArchive(list, config.format) }
    }

    private suspend fun conversationTitle(contact: String): String {
        val key = ContactKey(contact)
        val address = key.addressString
        return if (address == NodeAddress.ID_BROADCAST) {
            getStringSuspend(Res.string.archive_channel_n, key.channel)
        } else {
            nodes.getNode(address).effectiveLongName.takeIf { it.isNotBlank() } ?: address
        }
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val CHECK_INTERVAL_MS = 60_000L
        const val ENCRYPTED_EXTENSION = "enc"
        const val MESSAGES_PER_CONVERSATION = 500
    }
}
