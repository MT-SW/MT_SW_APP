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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.database.entity.MeshLog
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.DeviceType
import org.meshtastic.core.model.InterfaceId
import org.meshtastic.core.model.MeshActivity
import org.meshtastic.core.model.util.effectivePortNum
import org.meshtastic.core.repository.RadioInterfaceService
import org.meshtastic.core.repository.RadioSessionContext
import org.meshtastic.core.repository.RadioSessionLease
import org.meshtastic.core.repository.ReceivedRadioFrame
import org.meshtastic.core.repository.TransportDisconnectReason
import org.meshtastic.proto.FromRadio
import org.meshtastic.proto.ToRadio
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** A packet heard by a background device together with that device's own node number. */
class BackgroundPacket(val myNodeNum: Int, val packet: org.meshtastic.proto.MeshPacket)

/**
 * The quiet link (MT_SW) to one saved radio that is not in use. A transport talks to it as to the radio service of the
 * app, but nothing here reaches the rest of the app: the link only asks the radio for its packets and turns each packet
 * it hears into a log entry for that radio's own database ([logs]). The radio in use is handled by the real service.
 */
@OptIn(ExperimentalUuidApi::class)
class BackgroundRadioLink(
    private val address: String,
    override val serviceScope: CoroutineScope,
    override val supportedDeviceTypes: List<DeviceType>,
    private val onFrame: (ByteArray) -> Unit = {},
) : RadioInterfaceService {
    /** Log entries waiting to be written; a full queue drops the newest ones. */
    val logs = Channel<MeshLog>(LOG_QUEUE)

    /** Packets that need more than a log entry (messages, node names, positions, telemetry), for the processor. */
    val packets = Channel<BackgroundPacket>(PACKET_QUEUE)

    /** Completes when the radio went away (or the link was closed). */
    val ended = CompletableDeferred<Unit>()

    private val lock = SynchronizedObject()
    private var sender: ((ByteArray) -> Boolean)? = null
    private var connected = false
    private var configAsked = false

    /**
     * Given by the owner right after the transport is created. A transport may connect before this is called, so
     * whichever of the two happens last asks the radio for its packets.
     */
    fun attach(send: (ByteArray) -> Boolean) {
        synchronized(lock) { sender = send }
        askForConfig()
    }

    /** Sends [bytes] to the radio through the transport; false when there is no transport yet. */
    fun send(bytes: ByteArray): Boolean = synchronized(lock) { sender }?.invoke(bytes) ?: false

    private fun askForConfig() {
        val send =
            synchronized(lock) {
                if (!connected || configAsked) return
                sender?.also { configAsked = true }
            } ?: return
        // Asking for the configuration is what makes the radio send the packets it hears.
        val request = ToRadio.Builder().also { it.want_config_id = (1..Int.MAX_VALUE).random() }.build()
        send(request.encode())
    }

    private var myNodeNum = 0

    private val state = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    override val connectionState: StateFlow<ConnectionState> = state
    override val currentDeviceAddressFlow: StateFlow<String?> = MutableStateFlow(address)
    override val sessionGeneration: StateFlow<Long> = MutableStateFlow(0L)
    override val mockTransportEnabled: StateFlow<Boolean> = MutableStateFlow(false)
    override val isReplayTransportAvailable: Boolean = false
    override val receivedData: Flow<ReceivedRadioFrame> = emptyFlow()
    override val meshActivity: Flow<MeshActivity> = emptyFlow()
    override val connectionError: Flow<String> = emptyFlow()
    override val activeSession: StateFlow<RadioSessionContext?> = MutableStateFlow(null)

    override fun onConnect() {
        state.value = ConnectionState.Connected
        Logger.i { "Background radio link connected" }
        synchronized(lock) {
            connected = true
            // A new start of the conversation (also after the device was in use): ask for the configuration again.
            configAsked = false
        }
        askForConfig()
    }

    override fun onDisconnect(isPermanent: Boolean, errorMessage: String?, reason: TransportDisconnectReason?) {
        state.value = ConnectionState.Disconnected
        Logger.i { "Background radio link ended (permanent=$isPermanent): ${errorMessage ?: "-"}" }
        ended.complete(Unit)
    }

    override fun handleFromRadio(bytes: ByteArray) {
        // The bridge of this device (if it is on) gets every frame, also while the device is not shown in the app.
        onFrame(bytes)
        val frame = runCatching { FromRadio.ADAPTER.decode(bytes) }.getOrNull() ?: return
        frame.my_info?.let { myNodeNum = it.my_node_num }
        val packet = frame.packet ?: return
        val decoded = packet.decoded
        val log =
            MeshLog(
                uuid = Uuid.random().toString(),
                message_type = "Packet",
                received_date = nowMillis,
                raw_message = packet.toString(),
                fromNum = if (packet.from == myNodeNum) MeshLog.NODE_NUM_LOCAL else packet.from,
                portNum = decoded?.effectivePortNum() ?: 0,
                fromRadio = FromRadio.Builder().also { it.packet = packet }.build(),
            )
        if (!logs.trySend(log).isSuccess) Logger.w { "Background radio link: log queue is full, a packet was dropped" }
        if (decoded != null) packets.trySend(BackgroundPacket(myNodeNum, packet))
    }

    // Everything below belongs to the app's own connection and is not used by a quiet link.
    override fun trySendToRadio(bytes: ByteArray): Boolean = send(bytes)

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

    override fun consumeGattCacheInvalidationRequest(): Boolean = false

    override fun getDeviceAddress(): String? = address

    override fun setDeviceAddress(deviceAddr: String?): Boolean = false

    override fun toInterfaceAddress(interfaceId: InterfaceId, rest: String): String = "${interfaceId.id}$rest"

    private companion object {
        const val LOG_QUEUE = 2_000
        const val PACKET_QUEUE = 500
    }
}
