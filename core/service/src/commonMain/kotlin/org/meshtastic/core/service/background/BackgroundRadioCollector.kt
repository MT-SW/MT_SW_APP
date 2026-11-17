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
package org.meshtastic.core.service.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.isValidDeviceAddress
import org.meshtastic.core.database.DatabaseManager
import org.meshtastic.core.database.entity.MeshLog
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.repository.RadioBackgroundMonitor
import org.meshtastic.core.repository.RadioPrefs
import org.meshtastic.core.repository.RadioSlotsPrefs
import org.meshtastic.core.repository.RadioTransportFactory
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.proto.Heartbeat
import org.meshtastic.proto.ToRadio

/**
 * Background collection (MT_SW). While the mesh service runs and the option is on, every saved radio that is not in
 * use (and not the chosen one) is connected quietly through [BackgroundRadioLink]; the packets it hears are written to
 * the database of that radio, so its history, charts and statistics keep growing like in a separate app. Only the log
 * of packets is kept this way; the full node list of a radio is loaded when it becomes the radio in use. A radio
 * without a database yet (never used) is skipped. A link that drops is tried again after a pause. Errors only go to
 * the log. Switching the option off or choosing another radio closes the links that are no longer wanted.
 */
@Single
class BackgroundRadioCollector(
    private val slotsPrefs: RadioSlotsPrefs,
    private val radioPrefs: RadioPrefs,
    private val uiPrefs: UiPrefs,
    private val transportFactory: RadioTransportFactory,
    private val databaseManager: DatabaseManager,
    private val dispatchers: CoroutineDispatchers,
) : RadioBackgroundMonitor {
    override fun start(scope: CoroutineScope) {
        scope.launch {
            combine(slotsPrefs.slots, slotsPrefs.selectedId, radioPrefs.devAddr, uiPrefs.uiOptions) {
                slots,
                selectedId,
                active,
                options,
                ->
                if (options[UiOptions.RADIO_BACKGROUND] != 1) return@combine emptySet()
                slots
                    .filter { it.id != selectedId && it.address != active }
                    .map { it.address }
                    .filter { isValidDeviceAddress(it) && transportFactory.isAddressValid(it) }
                    .toSet()
            }
                .distinctUntilChanged()
                .collectLatest { wanted ->
                    supervisorScope { wanted.forEach { address -> launch { keepCollecting(address) } } }
                }
        }
    }

    private suspend fun keepCollecting(address: String) {
        while (true) {
            try {
                collectOnce(address)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(e) { "Background collection from a radio failed: ${e.message}" }
            }
            delay(RETRY_MS)
        }
    }

    /** One connection: until the radio goes away or this is cancelled. */
    private suspend fun collectOnce(address: String) {
        val linkScope = CoroutineScope(SupervisorJob() + dispatchers.io)
        val link = BackgroundRadioLink(address, linkScope, transportFactory.supportedDeviceTypes)
        val transport = transportFactory.createTransport(address, link)
        link.send = transport::handleSendToRadio
        try {
            supervisorScope {
                val writer = launch { writeLogs(address, link) }
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
                writer.join()
            }
        } finally {
            withContext(NonCancellable) {
                runCatching { transport.close() }
                linkScope.cancel()
            }
        }
    }

    /** Writes the waiting log entries to the radio's own database in small batches. */
    private suspend fun writeLogs(address: String, link: BackgroundRadioLink) {
        val batch = ArrayList<MeshLog>()
        suspend fun flush() {
            if (batch.isEmpty()) return
            val toWrite = batch.toList()
            batch.clear()
            val written = databaseManager.withDeviceDb(address) { it.meshLogDao().insertIgnore(toWrite) }
            if (written == null) Logger.d { "Background collection: this radio has no database yet, packets dropped" }
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
        const val RETRY_MS = 30_000L
        const val HEARTBEAT_MS = 5 * 60 * 1_000L
        const val BATCH_SIZE = 100
        const val FLUSH_MS = 5_000L
        const val POLL_MS = 250L
    }
}
