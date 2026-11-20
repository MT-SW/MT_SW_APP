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
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.DeviceType
import org.meshtastic.core.model.InterfaceId
import org.meshtastic.core.model.MeshActivity
import org.meshtastic.core.model.TransportStatus
import org.meshtastic.core.repository.RadioInterfaceService
import org.meshtastic.core.repository.RadioSessionContext
import org.meshtastic.core.repository.RadioSessionLease
import org.meshtastic.core.repository.RadioTransport
import org.meshtastic.core.repository.RadioTransportFactory
import org.meshtastic.core.repository.ReceivedRadioFrame
import org.meshtastic.core.repository.TransportDisconnectReason
import org.meshtastic.proto.ToRadio
import kotlin.time.TimeSource

/**
 * The pool of connections (MT_SW): exactly one transport per device address. The app view (the foreground, one device
 * at a time) and the quiet background collector (every saved device) are only two consumers that listen to the same
 * transport. Switching the device in use passes the already open transport from one consumer to the other, so a device
 * is never disconnected and connected again just because the app starts to show it, and two transports to the same
 * device never exist (BLE and TCP devices drop such a second connection).
 *
 * Packets go to the foreground consumer when there is one, otherwise to the background consumer.
 */
@Suppress("TooManyFunctions")
@Single
class RadioTransportPool(
    private val transportFactory: RadioTransportFactory,
    private val dispatchers: CoroutineDispatchers,
) {
    /** How long a just-closed device waits before it is opened again; tests set it to 0 (their clock is virtual). */
    internal var reopenGapMs: Long = REOPEN_GAP_MS

    private val lock = SynchronizedObject()
    private val orphanScope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val entries = mutableMapOf<String, Entry>()
    private val live = MutableStateFlow<Set<String>>(emptySet())

    /**
     * When (monotonic, ms) the transport of an address was closed last; a reopen waits for [REOPEN_GAP_MS] after it.
     */
    private val closedAt = mutableMapOf<String, Long>()

    /** Addresses of devices whose transport is connected right now (shown or in the background). */
    val connectedAddresses: StateFlow<Set<String>> = live

    private inner class Entry(val address: String) {
        val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
        var foreground: RadioInterfaceService? = null
        var background: RadioInterfaceService? = null
        var connected = false
        val mirror = RadioStateMirror()
        lateinit var transport: RadioTransport
        val link = Link(this)
    }

    /** What the transport talks to: it forwards every event to the consumer that is attached at that moment. */
    @Suppress("TooManyFunctions")
    private inner class Link(private val entry: Entry) : RadioInterfaceService {
        private fun target(): RadioInterfaceService? = synchronized(lock) { entry.foreground ?: entry.background }

        override val serviceScope: CoroutineScope
            get() = entry.scope

        override fun onConnect() {
            synchronized(lock) { entry.connected = true }
            publish()
            target()?.onConnect()
        }

        override fun onDisconnect(isPermanent: Boolean, errorMessage: String?, reason: TransportDisconnectReason?) {
            synchronized(lock) { entry.connected = false }
            entry.mirror.reset()
            publish()
            target()?.onDisconnect(isPermanent, errorMessage, reason)
        }

        // MT_SW: the progress of the connection (waiting to retry, pairing needed, ...) goes on to whoever holds the link.
        override fun onStatus(status: TransportStatus) {
            target()?.onStatus(status)
        }

        override fun handleFromRadio(bytes: ByteArray) {
            entry.mirror.onFromRadio(bytes)
            target()?.handleFromRadio(bytes)
        }

        override val supportedDeviceTypes: List<DeviceType> = transportFactory.supportedDeviceTypes
        override val connectionState: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
        override val currentDeviceAddressFlow: StateFlow<String?> = MutableStateFlow(entry.address)
        override val sessionGeneration: StateFlow<Long> = MutableStateFlow(0L)
        override val mockTransportEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override val isReplayTransportAvailable: Boolean = false
        override val receivedData: Flow<ReceivedRadioFrame> = emptyFlow()
        override val meshActivity: Flow<MeshActivity> = emptyFlow()
        override val connectionError: Flow<String> = emptyFlow()

        // MT_SW: nobody follows the status of a quiet link, so it stays empty.
        override val transportStatus: StateFlow<TransportStatus?> = MutableStateFlow(null)
        override val activeSession: StateFlow<RadioSessionContext?> = MutableStateFlow(null)

        override fun trySendToRadio(bytes: ByteArray): Boolean = entry.transport.handleSendToRadio(bytes)

        override fun isSessionActive(session: RadioSessionContext): Boolean = false

        override fun runIfSessionActive(session: RadioSessionContext, block: () -> Unit): Boolean = false

        override suspend fun runWithSessionLease(
            session: RadioSessionContext,
            block: suspend (RadioSessionLease) -> Unit,
        ): Boolean = false

        override fun resetReceivedBuffer() = Unit

        override fun connect() = Unit

        override suspend fun disconnect() = Unit

        override suspend fun restartTransport() = Unit

        override fun requestGattCacheInvalidationOnNextConnect() = Unit

        // Only the device in use can ask for a fresh Bluetooth service table (after a firmware update).
        override fun consumeGattCacheInvalidationRequest(): Boolean =
            synchronized(lock) { entry.foreground }?.consumeGattCacheInvalidationRequest() ?: false

        override fun getDeviceAddress(): String? = entry.address

        override fun setDeviceAddress(deviceAddr: String?): Boolean = false

        override fun toInterfaceAddress(interfaceId: InterfaceId, rest: String): String = "${interfaceId.id}$rest"
    }

    private fun publish() {
        live.value = synchronized(lock) { entries.values.filter { it.connected }.map { it.address }.toSet() }
    }

    /**
     * Gives the radio a moment after its connection was closed before the same device is opened again. A radio serves
     * one client at a time: closing and reopening right away (restart of a broken connection, quick switching back and
     * forth) can leave it still holding the old socket, so it stops answering. Nothing waits when the device was not
     * closed recently or when its transport is still open (it is then reused as it is).
     */
    suspend fun awaitReopenGap(address: String) {
        val wait =
            synchronized(lock) {
                if (entries.containsKey(address)) return
                val closed = closedAt[address] ?: return
                reopenGapMs - (monotonicMillis() - closed)
            }
        if (wait > 0) {
            Logger.i { "Waiting $wait ms before opening the same device again" }
            delay(wait)
        }
    }

    /** Opens the transport of [address] or takes the one that is already open. Never opens a second one. */
    private fun entryFor(address: String): Entry = synchronized(lock) {
        entries[address]
            ?: Entry(address).also { created ->
                // Created under the lock on purpose: two callers must never both open a transport to one device.
                created.transport = transportFactory.createTransport(address, created.link)
                entries[address] = created
            }
    }

    /**
     * The device in use takes the transport of [address]. [consumer] then receives every event. If the transport is
     * already connected, nothing is opened again; the owner calls [PooledRadioTransport.announceIfConnected] once it
     * has stored the returned handle, so that [consumer] hears about the open connection.
     */
    fun attachForeground(address: String, consumer: RadioInterfaceService): PooledRadioTransport {
        val entry = entryFor(address)
        synchronized(lock) { entry.foreground = consumer }
        return PooledRadioTransport(entry.address, consumer)
    }

    /** The background collector listens to [address] while the device is not in use. */
    fun attachBackground(address: String, consumer: RadioInterfaceService): Boolean {
        val entry = entryFor(address)
        val connected =
            synchronized(lock) {
                entry.background = consumer
                entry.connected && entry.foreground == null
            }
        if (connected) consumer.onConnect()
        return true
    }

    /** True when a background consumer waits for [address], so a transport released by the app view is taken over. */
    fun hasBackground(address: String): Boolean = synchronized(lock) { entries[address]?.background != null }

    /**
     * The quiet background collector sends through here. Its request for the configuration is not passed to a radio
     * that is already configured: the radio keeps sending what it hears on its own, and asking again is only a
     * disturbance (a Wi-Fi radio does not answer a second request on the same connection).
     */
    fun sendFromBackground(address: String, bytes: ByteArray): Boolean {
        val entry = synchronized(lock) { entries[address] } ?: return false
        val answeredByTheCopy = entry.mirror.isComplete && wantedConfiguration(bytes) != 0
        return answeredByTheCopy || entry.transport.handleSendToRadio(bytes)
    }

    /** Sends [bytes] through the transport of [address]. */
    fun send(address: String, bytes: ByteArray): Boolean =
        synchronized(lock) { entries[address] }?.transport?.handleSendToRadio(bytes) ?: false

    /** The background collector no longer wants [address]. The transport is closed unless the app view uses it. */
    suspend fun detachBackground(address: String, consumer: RadioInterfaceService) {
        val toClose =
            synchronized(lock) {
                val entry = entries[address] ?: return
                if (entry.background === consumer) entry.background = null
                closeIfUnusedLocked(entry)
            }
        toClose?.let { closeEntry(it) }
    }

    /** A transport handed over with nobody listening is closed after a while, so it never stays open for nothing. */
    private fun scheduleOrphanCheck(entry: Entry) {
        orphanScope.launch {
            delay(ORPHAN_GRACE_MS)
            val toClose =
                synchronized(lock) { if (entries[entry.address] === entry) closeIfUnusedLocked(entry) else null }
            toClose?.let { closeEntry(it) }
        }
    }

    private fun closeIfUnusedLocked(entry: Entry): Entry? {
        if (entry.foreground != null || entry.background != null) return null
        entries.remove(entry.address)
        return entry
    }

    private suspend fun closeEntry(entry: Entry) {
        withContext(NonCancellable) {
            try {
                entry.transport.close()
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(e) { "Closing a transport failed" }
            }
            entry.scope.cancel()
            synchronized(lock) { closedAt[entry.address] = monotonicMillis() }
            publish()
        }
    }

    /** What the app view holds instead of the raw transport. Releasing it hands the transport over when possible. */
    inner class PooledRadioTransport(private val address: String, private val consumer: RadioInterfaceService) :
        RadioTransport {
        private fun current(): Entry? = synchronized(lock) { entries[address] }

        override fun handleSendToRadio(p: ByteArray): Boolean {
            val entry = current() ?: return false
            // A radio that is already open and known is not asked for its configuration again: the request is answered
            // from the live copy, so switching between devices never negotiates with the radio.
            val nonce = wantedConfiguration(p)
            val frames = if (nonce != 0) entry.mirror.replay(nonce) else null
            if (frames != null) {
                entry.scope.launch {
                    // The radio itself answers after a moment; the asker has already noted what it waits for.
                    delay(REPLAY_DELAY_MS)
                    frames.forEach { consumer.handleFromRadio(it) }
                }
            }
            return frames != null || entry.transport.handleSendToRadio(p)
        }

        override fun keepAlive() {
            current()?.transport?.keepAlive()
        }

        /** Tells the consumer that the transport it took over is already connected (no new connection is made). */
        fun announceIfConnected() {
            val connected = synchronized(lock) { entries[address]?.connected == true }
            if (connected) consumer.onConnect()
        }

        /** Closes the transport for good, whoever else listens (restart of a broken connection). */
        override suspend fun close() = release(handOver = false)

        /**
         * Gives the transport up. With [handOver] the background consumer (if any) keeps the open transport; without it
         * the transport is closed and the background consumer is told that the device went away.
         */
        suspend fun release(handOver: Boolean) {
            var background: RadioInterfaceService? = null
            val toClose =
                synchronized(lock) {
                    val entry = entries[address] ?: return
                    if (entry.foreground === consumer) entry.foreground = null
                    if (handOver) {
                        // Kept open even when the background collector does not listen yet (it starts a moment after
                        // a switch); if nobody takes it over in time, it is closed.
                        if (entry.background == null) scheduleOrphanCheck(entry)
                        null
                    } else {
                        background = entry.background
                        entry.background = null
                        entries.remove(address)
                        entry
                    }
                }
            if (toClose == null) {
                // The background consumer takes over; it learns the current state of the already open transport.
                val bg = synchronized(lock) { entries[address]?.takeIf { it.connected }?.background }
                bg?.onConnect()
                return
            }
            closeEntry(toClose)
            background?.onDisconnect(isPermanent = false, errorMessage = null, reason = null)
        }
    }
}

/** The number in a request for the configuration (`want_config_id`), 0 for any other message to the radio. */
private fun wantedConfiguration(bytes: ByteArray): Int =
    runCatching { ToRadio.ADAPTER.decode(bytes).want_config_id }.getOrNull() ?: 0

private const val ORPHAN_GRACE_MS = 60_000L

/** The pause before a request for the configuration is answered from the copy, like a radio's answer takes. */
private const val REPLAY_DELAY_MS = 30L

/** Pause between closing a connection and opening the same device again. */
private const val REOPEN_GAP_MS = 3_000L

private val monotonicStart = TimeSource.Monotonic.markNow()

private fun monotonicMillis(): Long = monotonicStart.elapsedNow().inWholeMilliseconds
