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
import okio.ByteString.Companion.toByteString
import org.koin.core.annotation.Single
import org.meshtastic.core.common.crypto.ArchiveCrypto
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.model.util.getChannel
import org.meshtastic.core.repository.FolderService
import org.meshtastic.core.repository.MessageArchiveConfig
import org.meshtastic.core.repository.MessageArchiveMonitor
import org.meshtastic.core.repository.MessageArchivePrefs
import org.meshtastic.core.repository.MessageArchiveRestoreResult
import org.meshtastic.core.repository.MessageArchiveState
import org.meshtastic.core.repository.MessageArchiver
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.archive_channel_n
import org.meshtastic.core.resources.archive_channel_named
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.unknown_username
import org.meshtastic.proto.PortNum
import kotlin.time.Instant
import kotlin.time.TimeSource

/**
 * Keeps a copy of the text messages in a folder the user chose: one file per day (or per conversation and day), in the
 * format they picked. Every run rewrites the files of today and yesterday from the message history, so nothing is
 * missed when the app was closed for a while, and a file is written only when its content changed. It does not need the
 * radio: it reads the message history stored on the device.
 */
@Single
@Suppress("TooManyFunctions")
class MessageArchiverImpl(
    private val packets: PacketRepository,
    private val nodes: NodeRepository,
    private val folders: FolderService,
    private val prefs: MessageArchivePrefs,
    private val radioConfig: RadioConfigRepository,
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
        val start = TimeSource.Monotonic.markNow()
        try {
            val built = buildFiles(config)
            Logger.i { "Message archive: ${built.size} files built in ${start.elapsedNow().inWholeMilliseconds} ms" }
            for ((plainName, content) in built) {
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
            Logger.i {
                "Message archive: done in ${start.elapsedNow().inWholeMilliseconds} ms, $files files written, ok=$ok"
            }
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
        val channelNames = channelNamesBySlot()
        val entries = mutableListOf<Pair<Long, ArchiveEntry>>()
        for (contact in packets.getContacts().first().keys) {
            val key = ContactKey(contact)
            val channelName = channelNames[key.channel].orEmpty()
            val fileTitle = conversationTitle(contact)
            // In a channel the title also has the channel name, which a restore from a text file goes by.
            val title =
                if (key.addressString == NodeAddress.ID_BROADCAST && channelName.isNotEmpty()) {
                    getStringSuspend(Res.string.archive_channel_named, key.channel, channelName)
                } else {
                    fileTitle
                }
            val messages =
                packets
                    .getMessagesFrom(
                        contact = contact,
                        limit = MESSAGES_PER_CONVERSATION,
                        includeFiltered = false,
                        getNode = { userId -> nodes.getNode(userId.orEmpty()) },
                    )
                    .first()
            for (message in messages) {
                val local = Instant.fromEpochMilliseconds(message.displayTime).toLocalDateTime(zone)
                val date = local.date.toString()
                if (date !in days) continue
                val sender = message.node.effectiveLongName.takeIf { it.isNotBlank() } ?: unknown
                val time = local.time.toString().substringBefore('.')
                entries +=
                    message.displayTime to
                    ArchiveEntry(
                        date = date,
                        time = time,
                        conversation = title,
                        sender = sender,
                        text = message.text,
                        millis = message.displayTime,
                        channelName = channelName,
                        channelIndex = key.channel,
                        address = key.addressString,
                        senderId = message.node.user.id,
                        fromLocal = message.fromLocal,
                        packetId = message.packetId,
                        fileTitle = fileTitle,
                    )
            }
        }
        val extension = archiveExtension(config.format)
        return entries
            .sortedBy { it.first }
            .map { it.second }
            .groupBy {
                if (config.perConversation) {
                    "wiadomosci_${it.date}_${safeFileName(it.fileTitle)}.$extension"
                } else {
                    "wiadomosci_${it.date}.$extension"
                }
            }
            .mapValues { (_, list) -> formatArchive(list, config.format) }
    }

    /** Slot number → the name of the channel in it as it is now (a blank name is the one of the modem preset). */
    private suspend fun channelNamesBySlot(): Map<Int, String> {
        val set = radioConfig.channelSetFlow.first()
        return set.settings.indices.mapNotNull { slot -> set.getChannel(slot)?.name?.let { slot to it } }.toMap()
    }

    override suspend fun restore(preview: Boolean): MessageArchiveRestoreResult = mutex.withLock {
        val config = prefs.config.value
        if (config.folder.isBlank()) return@withLock MessageArchiveRestoreResult()
        val myNodeNum = nodes.myNodeInfo.value?.myNodeNum ?: return@withLock MessageArchiveRestoreResult()
        val channels = channelNamesBySlot()
        val slotByName = channels.entries.groupBy({ it.value }, { it.key }).mapValues { it.value.min() }
        val suffixes = RESTORE_EXTENSIONS.flatMap { listOf(".$it", ".$it.$ENCRYPTED_EXTENSION") }
        // The tables first: they hold the exact times and numbers, so what the text files add is compared with them.
        val files = folders.readFiles(config.folder, suffixes).sortedBy { (name, _) -> if (isCsvFile(name)) 0 else 1 }
        val counts = RestoreCounts(preview)
        val textRestore = TextRestore(myNodeNum, channels, slotByName)
        for ((name, bytes) in files) {
            val text = readableText(name, bytes, config.password)
            if (text == null) {
                counts.unreadable++
                continue
            }
            val plain = name.removeSuffix(".$ENCRYPTED_EXTENSION")
            when {
                isCsvFile(name) -> restoreCsv(parseArchiveCsv(text), myNodeNum, slotByName, counts)
                plain.endsWith(".md") -> restoreLines(parseArchiveMarkdown(text, name), textRestore, counts)
                else -> restoreLines(parseArchiveText(text), textRestore, counts)
            }.also { found ->
                // A file that is not one of ours, or the wrong password: nothing in it looks like messages.
                if (!found && name.endsWith(".$ENCRYPTED_EXTENSION")) counts.unreadable++
                if (found) counts.files++
            }
        }
        MessageArchiveRestoreResult(
            files = counts.files,
            restored = counts.restored,
            alreadyThere = counts.already,
            noChannel = counts.noChannel,
            unreadable = counts.unreadable,
            preview = preview,
            missingChannels = counts.missing.take(MAX_MISSING_NAMES),
        )
    }

    private fun isCsvFile(name: String) = name.removeSuffix(".$ENCRYPTED_EXTENSION").endsWith(".csv")

    /**
     * The counters of one restore. In a [preview] nothing is written, so what the run would have written is remembered
     * here ([wouldHave]: message numbers, [wouldHaveLines]: chat and second and text) to count a message that is in two
     * files only once, just like a real restore does.
     */
    private class RestoreCounts(val preview: Boolean) {
        val wouldHave = mutableSetOf<Int>()
        val wouldHaveLines = mutableSetOf<String>()
        val missing = linkedSetOf<String>()
        var files = 0
        var restored = 0
        var already = 0
        var noChannel = 0
        var unreadable = 0
    }

    /** What the text and Markdown files need: who is who now, and the messages that are already in each chat. */
    private inner class TextRestore(
        val myNodeNum: Int,
        val channels: Map<Int, String>,
        val slotByName: Map<String, Int>,
    ) {
        val zone: TimeZone = TimeZone.currentSystemDefault()
        val knownNodes by lazy { nodes.nodeDBbyNum.value.values.toList() }
        val myNode by lazy { nodes.nodeDBbyNum.value[myNodeNum] }
        val seen = mutableMapOf<String, MutableSet<String>>()
    }

    /** Puts the messages of one CSV file back; false when the file holds none. */
    private suspend fun restoreCsv(
        records: List<ArchiveRecord>,
        myNodeNum: Int,
        slotByName: Map<String, Int>,
        counts: RestoreCounts,
    ): Boolean {
        for (record in records) {
            val slot = slotFor(record, slotByName)
            when {
                slot == null -> {
                    counts.noChannel++
                    if (record.channelName.isNotBlank()) counts.missing += record.channelName
                }

                record.packetId != 0 && packets.findPacketsWithId(record.packetId).isNotEmpty() -> counts.already++

                counts.preview && record.packetId != 0 && !counts.wouldHave.add(record.packetId) -> counts.already++

                else -> {
                    if (counts.preview) {
                        val key = lineKeyOf(record.millis / MILLIS_PER_SECOND, record.text)
                        counts.wouldHaveLines += "$slot${record.address}|$key"
                    } else {
                        saveRecord(myNodeNum, record, slot)
                    }
                    counts.restored++
                }
            }
        }
        return records.isNotEmpty()
    }

    /**
     * Puts the messages of one text or Markdown file back. These files have no message numbers, so a message counts as
     * already there when the chat has one with the same text in the same second. The chat is found from the title: a
     * channel by its name (or its number, when the file does not have the name), a private chat by the name of the
     * node.
     */
    @Suppress("LoopWithTooManyJumpStatements")
    private suspend fun restoreLines(lines: List<ArchiveLine>, context: TextRestore, counts: RestoreCounts): Boolean {
        for (line in lines) {
            val millis = line.millis(context.zone)
            val target = textTarget(line.conversation, context)
            if (millis == null || target == null) {
                counts.noChannel++
                if (target == null && line.conversation.isNotBlank()) counts.missing += line.conversation
                continue
            }
            val (slot, address) = target
            val contact = "$slot$address"
            val seen =
                context.seen.getOrPut(contact) {
                    packets
                        .getMessagesFrom(contact, null, true) { userId -> nodes.getNode(userId.orEmpty()) }
                        .first()
                        .mapTo(mutableSetOf()) { lineKeyOf(it.displayTime / MILLIS_PER_SECOND, it.text) }
                }
            val key = lineKeyOf(millis / MILLIS_PER_SECOND, line.text)
            if (!seen.add(key) || (counts.preview && "$contact|$key" in counts.wouldHaveLines)) {
                counts.already++
                continue
            }
            val sender = context.knownNodes.singleOrNull { it.effectiveLongName == line.sender }
            val fromLocal = line.sender == context.myNode?.effectiveLongName
            val record =
                ArchiveRecord(
                    millis = millis,
                    text = line.text,
                    channelName = "",
                    channelIndex = slot,
                    address = address,
                    senderId = sender?.user?.id.orEmpty(),
                    fromLocal = fromLocal,
                    packetId = key.hashCode().takeIf { it != 0 } ?: 1,
                )
            if (!counts.preview) saveRecord(context.myNodeNum, record, slot)
            counts.restored++
        }
        return lines.isNotEmpty()
    }

    /** The slot and the address of the chat a text line belongs to now, or null when there is no such chat. */
    @Suppress("ReturnCount")
    private fun textTarget(title: String, context: TextRestore): Pair<Int, String>? {
        parseChannelTitle(title)?.let { channel ->
            val slot =
                if (channel.name != null) {
                    context.slotByName[channel.name]
                } else {
                    channel.index.takeIf { it in context.channels }
                }
            return slot?.let { it to NodeAddress.ID_BROADCAST }
        }
        val node = context.knownNodes.singleOrNull { it.effectiveLongName == title } ?: return null
        val slot = if (context.myNode?.hasPKC == true && node.hasPKC) NodeAddress.PKC_CHANNEL_INDEX else node.channel
        return slot to node.user.id
    }

    /** The text of an archive file, decrypted when it is a `.enc` file; null when it cannot be read. */
    @Suppress("ReturnCount")
    private fun readableText(name: String, bytes: ByteArray, password: String): String? {
        if (!name.endsWith(".$ENCRYPTED_EXTENSION")) return bytes.decodeToString()
        if (password.isEmpty()) return null
        return ArchiveCrypto.decrypt(password, bytes)?.decodeToString()
    }

    /**
     * The slot the channel of [record] has now, found by its name (a blank name is a name too: the channel of the modem
     * preset); the private-message slot is kept as it was.
     */
    private fun slotFor(record: ArchiveRecord, slotByName: Map<String, Int>): Int? = when (record.channelIndex) {
        NodeAddress.PKC_CHANNEL_INDEX -> NodeAddress.PKC_CHANNEL_INDEX
        else -> slotByName[record.channelName]
    }

    private suspend fun saveRecord(myNodeNum: Int, record: ArchiveRecord, slot: Int) {
        val broadcast = record.address == NodeAddress.ID_BROADCAST
        val to =
            when {
                broadcast -> NodeAddress.ID_BROADCAST
                record.fromLocal -> record.address
                else -> NodeAddress.ID_LOCAL
            }
        val packet =
            DataPacket(
                to = to,
                bytes = record.text.encodeToByteArray().toByteString(),
                dataType = PortNum.TEXT_MESSAGE_APP.value,
                from = if (record.fromLocal) NodeAddress.ID_LOCAL else record.senderId.ifEmpty { record.address },
                time = record.millis,
                id = record.packetId,
                status = if (record.fromLocal) MessageStatus.DELIVERED else MessageStatus.UNKNOWN,
                channel = slot,
                wantAck = false,
            )
        packets.savePacket(
            myNodeNum = myNodeNum,
            contactKey = "$slot${record.address}",
            packet = packet,
            receivedTime = record.millis,
            read = true,
        )
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
        const val MILLIS_PER_SECOND = 1_000L
        const val MAX_MISSING_NAMES = 10
        val RESTORE_EXTENSIONS = listOf("csv", "txt", "md")
    }
}
