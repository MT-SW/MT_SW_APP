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
@file:Suppress("SwallowedException")

package org.meshtastic.core.service.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.isValidDeviceAddress
import org.meshtastic.core.common.util.normalizeAddress
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.database.DatabaseManager
import org.meshtastic.core.database.entity.MeshLog
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.TransportStatus
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.repository.DeviceBadge
import org.meshtastic.core.repository.DeviceHistoryRecorder
import org.meshtastic.core.repository.DeviceSnapshot
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.MessageFilter
import org.meshtastic.core.repository.RadioBackgroundMonitor
import org.meshtastic.core.repository.RadioBridgeMonitor
import org.meshtastic.core.repository.RadioSlotsPrefs
import org.meshtastic.core.repository.RadioTransportFactory
import org.meshtastic.core.repository.SenderMutePrefs
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.repository.badgeFor
import org.meshtastic.proto.Heartbeat
import org.meshtastic.proto.ToRadio

/**
 * Background collection (MT_SW). While the mesh service runs and the option is on, every saved device that is not
 * switched off is kept connected through the [RadioTransportPool]; there is exactly one connection per device. While a
 * device is not the one shown in the app, the packets it hears are written through [BackgroundRadioLink] to the
 * database of that device, so its history, charts and statistics keep growing. When the app shows a device, the pool
 * hands the already open connection over, and back again afterwards, without any disconnect. A link that drops is tried
 * again after a pause. Errors only go to the log.
 */
@Suppress("LongParameterList")
@Single
class BackgroundRadioCollector(
    private val slotsPrefs: RadioSlotsPrefs,
    private val uiPrefs: UiPrefs,
    private val transportFactory: RadioTransportFactory,
    private val pool: RadioTransportPool,
    private val databaseManager: DatabaseManager,
    private val notifier: MeshNotificationManager,
    private val bridge: RadioBridgeMonitor,
    private val senderMutePrefs: SenderMutePrefs,
    private val messageFilter: MessageFilter,
    private val dispatchers: CoroutineDispatchers,
    private val historyRecorder: DeviceHistoryRecorder,
) : RadioBackgroundMonitor {
    override val connectedAddresses: StateFlow<Set<String>> = pool.connectedAddresses

    private val badgeMap = MutableStateFlow<Map<String, DeviceBadge>>(emptyMap())

    override val badges: StateFlow<Map<String, DeviceBadge>> = badgeMap

    private val unread = MutableStateFlow<Map<String, Int>>(emptyMap())

    override val unreadByAddress: StateFlow<Map<String, Int>> = unread

    private val progress = MutableStateFlow<Map<String, String>>(emptyMap())

    override val progressByAddress: StateFlow<Map<String, String>> = progress

    private val statuses = MutableStateFlow<Map<String, org.meshtastic.core.model.TransportStatus>>(emptyMap())

    override val statusByAddress: StateFlow<Map<String, org.meshtastic.core.model.TransportStatus>> = statuses

    override fun clearUnread(address: String) {
        unread.update { it - address }
    }

    override fun start(scope: CoroutineScope) {
        // The plates are read from the database of each saved device; the device in use is left as it was (the app
        // writes to that database itself and draws its own plate).
        scope.launch {
            // The databases of the saved devices are never removed by the cache limit.
            slotsPrefs.slots.collect { slots ->
                databaseManager.setProtectedAddresses(slots.map { it.address }.filter(::isValidDeviceAddress).toSet())
            }
        }
        scope.launch {
            slotsPrefs.slots.collectLatest { slots ->
                while (true) {
                    slots.forEach { slot -> readBadge(slot.address) }
                    delay(BADGE_MS)
                }
            }
        }
        // The history of the device comparison grows by itself, whether or not its window is ever opened.
        scope.launch {
            slotsPrefs.slots.collectLatest { slots ->
                delay(HISTORY_FIRST_MS)
                while (true) {
                    slots.forEach { slot ->
                        try {
                            readSnapshot(slot.address)?.let { historyRecorder.store(slot.address, it, nowMillis) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                            Logger.w { "Historia urządzeń: odczyt w tle nie udał się: \${e.message}" }
                        }
                    }
                    delay(HISTORY_EVERY_MS)
                }
            }
        }
        scope.launch {
            combine(slotsPrefs.slots, slotsPrefs.offIds, uiPrefs.uiOptions) { slots, off, options ->
                if (UiOptions(options)[UiOptions.RADIO_BACKGROUND] != 1) {
                    return@combine emptySet<String>()
                }
                slots
                    .filter { it.id !in off }
                    .map { it.address }
                    .filter { isValidDeviceAddress(it) && transportFactory.isAddressValid(it) }
                    .toSet()
            }
                .distinctUntilChanged()
                .collect { wanted ->
                    // Only the devices that were added or removed are touched; the others keep their connection.
                    (workers.keys - wanted).forEach { workers.remove(it)?.cancel() }
                    (wanted - workers.keys).forEach { address ->
                        workers[address] = scope.launch { keepCollecting(address) }
                    }
                }
        }
    }

    private val workers = mutableMapOf<String, Job>()

    override suspend fun readSnapshot(address: String): DeviceSnapshot? {
        val key = normalizeAddress(address)
        return try {
            databaseManager
                .withDeviceDb(address, includeActive = true) { db ->
                    val dao = db.nodeInfoDao()
                    val mine = dao.getMyNodeEntity() ?: return@withDeviceDb null
                    val nodes = dao.getAllNodesSnapshot()
                    val own = nodes.firstOrNull { it.num == mine.myNodeNum }
                    val metrics = own?.deviceMetrics
                    DeviceSnapshot(
                        nodeNum = mine.myNodeNum,
                        shortName = own?.user?.short_name.orEmpty(),
                        batteryLevel = metrics?.battery_level,
                        voltage = metrics?.voltage,
                        channelUtilization = metrics?.channel_utilization,
                        airUtilTx = metrics?.air_util_tx,
                        uptimeSeconds = metrics?.uptime_seconds,
                        nodeCount = nodes.size,
                        onlineCount = nodes.count { it.isOnline },
                    )
                }
                .also { Logger.d { "Porównanie urządzeń: ${anonymize(key)} odczytane = ${it != null}" } }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.d { "Porównanie urządzeń: ${anonymize(key)} nie dało się odczytać: ${e.message}" }
            null
        }
    }

    override suspend fun readBadge(address: String) {
        val key = normalizeAddress(address)
        val badge =
            try {
                databaseManager.withDeviceDb(address, includeActive = true) { db ->
                    val mine = db.nodeInfoDao().getMyNodeEntity()
                    if (mine == null) {
                        Logger.d { "Plate ${anonymize(key)}: the database has no own node yet" }
                        return@withDeviceDb null
                    }
                    val node = db.nodeInfoDao().getNodeEntitiesByNums(listOf(mine.myNodeNum)).firstOrNull()
                    val name = node?.user?.short_name.orEmpty()
                    if (name.isBlank()) {
                        Logger.d { "Plate ${anonymize(key)}: the own node is there but has no short name yet" }
                    }
                    DeviceBadge(mine.myNodeNum, name)
                }
                    ?: run {
                        Logger.d { "Plate ${anonymize(key)}: no database found for this device" }
                        null
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.d { "Plate ${anonymize(key)}: could not be read: ${e.message}" }
                null
            }
        if (badge != null) publishBadge(key, badge)
    }

    /**
     * Keeps [badges] up to date. A plate with a name is never replaced by one without a name (the own node is stored
     * before its short name arrives); the same device with a new node number gets the new plate.
     */
    private fun publishBadge(key: String, badge: DeviceBadge) {
        badgeMap.update { current ->
            val old = current[key]
            when {
                badge.shortName.isNotBlank() -> current + (key to badge)
                old != null && old.shortName.isNotBlank() -> current
                else -> current + (key to badge)
            }
        }
    }

    /** The plate of a device is also known the moment its start handshake ends (not only on the slow round). */
    internal fun badgeOf(address: String): DeviceBadge? = badgeMap.value.badgeFor(address)

    private fun anonymize(key: String): String = key.takeLast(ADDRESS_TAIL).let { "…$it" }

    private suspend fun keepCollecting(address: String) {
        // A short pause first: a radio that has just been in use may still be closing its connection.
        delay(START_DELAY_MS)
        while (true) {
            try {
                collectOnce(address)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(e) { "Background collection from a device failed: ${e.message}" }
            }
            delay(RETRY_MS)
        }
    }

    /** One connection: until the device goes away or this is cancelled. */
    private suspend fun collectOnce(address: String) {
        val linkScope = CoroutineScope(SupervisorJob() + dispatchers.io)
        val link =
            BackgroundRadioLink(address, linkScope, transportFactory.supportedDeviceTypes) { bytes ->
                bridge.onDeviceFrame(address, bytes.toByteString())
            }
        link.attach { bytes -> pool.sendFromBackground(address, bytes) }
        pool.awaitReopenGap(address)
        pool.attachBackground(address, link)
        try {
            supervisorScope {
                val writer = launch { writeLogs(address, link) }
                launch {
                    link.lastStatus.collect { status ->
                        statuses.update { if (status == null) it - address else it + (address to status) }
                    }
                }
                val processor = launch { processPackets(address, link) }
                val store = launch {
                    BackgroundDeviceStore(
                        address,
                        databaseManager,
                        onProgress = { text ->
                            progress.update { if (text == null) it - address else it + (address to text) }
                            if (text == null) statuses.update { it - address }
                        },
                        onPhase = { phase -> link.lastStatus.value = TransportStatus(phase) },
                    )
                        .run(link.setup)
                    readBadge(address)
                }
                val beat = launch {
                    val heartbeat = ToRadio.Builder().also { it.heartbeat = Heartbeat.Builder().build() }.build()
                    while (true) {
                        delay(HEARTBEAT_MS)
                        link.send(heartbeat.encode())
                    }
                }
                link.ended.await()
                beat.cancel()
                link.logs.close()
                link.packets.close()
                link.setup.close()
                writer.join()
                processor.join()
                store.join()
            }
        } finally {
            withContext(NonCancellable) {
                progress.update { it - address }
                statuses.update { it - address }
                pool.detachBackground(address, link)
                linkScope.cancel()
            }
        }
    }

    /** Hands the packets of a background device to the processor: messages, notifications, node data. */
    private suspend fun processPackets(address: String, link: BackgroundRadioLink) {
        val processor =
            BackgroundPacketProcessor(
                address = address,
                databaseManager = databaseManager,
                notifier = notifier,
                // Only the short name of the device's own node, nothing else.
                deviceName = { badgeMap.value.badgeFor(address)?.shortName.orEmpty() },
                onMessage = { unread.update { it + (address to ((it[address] ?: 0) + 1)) } },
                senderMutes = { senderMutePrefs.mutes.value },
                isFiltered = messageFilter::shouldFilter,
            )
        for (item in link.packets) processor.process(item.myNodeNum, item.packet)
    }

    /** Writes the waiting log entries to the radio's own database in small batches. */
    private suspend fun writeLogs(address: String, link: BackgroundRadioLink) {
        val batch = ArrayList<MeshLog>()
        suspend fun flush() {
            if (batch.isEmpty()) return
            val toWrite = batch.toList()
            batch.clear()
            val written =
                databaseManager.withDeviceDb(address, includeActive = true, createIfMissing = true) {
                    it.meshLogDao().insertIgnore(toWrite)
                }
            if (written == null) Logger.d { "Background collection: this device has no database yet, packets dropped" }
        }
        while (true) {
            val first = link.logs.receiveCatching().getOrNull() ?: break
            batch.add(first)
            // Gather what else is waiting, but write at the latest after a short pause or a full batch.
            var waited = 0L
            while (batch.size < BATCH_SIZE && waited < FLUSH_MS) {
                val next = link.logs.tryReceive().getOrNull()
                if (next != null) {
                    batch.add(next)
                } else {
                    delay(POLL_MS)
                    waited += POLL_MS
                }
            }
            flush()
        }
        flush()
    }

    private companion object {
        const val BADGE_MS = 60_000L
        const val HISTORY_FIRST_MS = 60_000L
        const val HISTORY_EVERY_MS = 10 * 60_000L
        const val ADDRESS_TAIL = 4
        const val START_DELAY_MS = 2_000L
        const val RETRY_MS = 10_000L
        const val HEARTBEAT_MS = 30 * 1_000L
        const val BATCH_SIZE = 100
        const val FLUSH_MS = 5_000L
        const val POLL_MS = 250L
    }
}
