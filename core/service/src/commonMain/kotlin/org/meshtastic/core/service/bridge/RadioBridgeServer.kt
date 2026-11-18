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
package org.meshtastic.core.service.bridge

import co.touchlab.kermit.Logger
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import okio.ByteString
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.network.transport.StreamFrameCodec
import org.meshtastic.core.repository.RadioBridgeConfig
import org.meshtastic.core.repository.RadioBridgeMonitor
import org.meshtastic.core.repository.RadioBridgePrefs
import org.meshtastic.core.repository.RadioInterfaceService
import org.meshtastic.core.repository.RadioSlot
import org.meshtastic.core.repository.RadioSlotsPrefs
import org.meshtastic.core.service.background.RadioTransportPool
import org.meshtastic.proto.ToRadio
import kotlin.concurrent.Volatile

/**
 * The TCP bridges (MT_SW), written for this app: for every saved device that has its bridge switched on, a server on
 * the computer or phone that speaks the same stream protocol as the device's own TCP port, so other programs can
 * connect to that device through the app while the app keeps its single connection. Several devices can be shared at
 * the same time, each on its own port. What clients may see and send is decided by one [BridgeRouter] per device. The
 * frames of the device shown in the app come from the app's own session ([onFromRadio]); the frames of a device kept
 * in the background come from its quiet link ([onDeviceFrame]). Errors only go to the log. A server moves when its
 * port, the local-network access or its device changes, stops when its bridge is switched off, and takes the
 * restrictions and the number of clients live, without dropping anyone.
 *
 * The routers and the client lists are shared by the receive loops and the sockets, so every access goes through
 * [lock] (never held while suspending).
 */
@Single
class RadioBridgeServer(
    private val radioInterfaceService: RadioInterfaceService,
    private val pool: RadioTransportPool,
    private val slotsPrefs: RadioSlotsPrefs,
    private val bridgePrefs: RadioBridgePrefs,
    private val dispatchers: CoroutineDispatchers,
) : RadioBridgeMonitor {
    private val lock = SynchronizedObject()
    private val instances = LinkedHashMap<Int, Instance>()
    private var nextClientId = 1

    /** Where a server listens; a change of this moves the server, a change of anything else does not. */
    private data class Listen(val port: Int, val lanAccess: Boolean)

    /** The bridge of one saved device. */
    private inner class Instance(val slot: RadioSlot, var listen: Listen) {
        val router = BridgeRouter()
        val clients = LinkedHashMap<Int, Channel<ByteArray>>()

        @Volatile var serving = false

        // True from the moment the device was asked for its configuration again (to get one to hand out) until the
        // configuration is there.
        @Volatile var handshakeAsked = false
        val handshakeRequests = Channel<Unit>(Channel.CONFLATED)
        var job: Job? = null
    }

    override fun start(scope: CoroutineScope) {
        scope.launch {
            combine(slotsPrefs.slots, bridgePrefs.configs) { slots, all ->
                slots.associateWith { slot -> all[slot.id] ?: RadioBridgeConfig() }
            }
                .collect { desired -> reconcile(scope, desired) }
        }
    }

    /** Starts, moves or stops the servers so that they match the settings of the saved devices. */
    private fun reconcile(scope: CoroutineScope, desired: Map<RadioSlot, RadioBridgeConfig>) {
        val wanted = desired.filter { it.value.enabled }.mapKeys { it.key.id }
        val toStop = ArrayList<Instance>()
        synchronized(lock) {
            for ((id, instance) in instances.toList()) {
                val entry = wanted[id]
                val slot = desired.keys.firstOrNull { it.id == id }
                if (entry == null || slot == null || slot.address != instance.slot.address) {
                    instances.remove(id)
                    toStop.add(instance)
                }
            }
            // Restrictions and the number of clients apply at once.
            for ((id, instance) in instances) wanted[id]?.let { instance.router.config = it }
        }
        toStop.forEach { it.job?.cancel() }
        for ((slot, config) in desired.filter { it.value.enabled }) {
            val listen = Listen(config.portFor(slot.id), config.lanAccess)
            val running = synchronized(lock) { instances[slot.id] }
            if (running != null && running.listen == listen) continue
            // The old server of this device must be gone (its port free) before the new one binds.
            val previous = running?.job ?: toStop.firstOrNull { it.slot.id == slot.id }?.job
            running?.job?.cancel()
            val instance = Instance(slot, listen).also { it.router.config = config }
            synchronized(lock) { instances[slot.id] = instance }
            instance.job =
                scope.launch {
                    try {
                        previous?.join()
                        serve(instance)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        Logger.w(e) { "Radio bridge stopped: ${e.message}" }
                    }
                }
        }
    }

    override fun onFromRadio(payload: ByteString) {
        val address = radioInterfaceService.getDeviceAddress() ?: return
        onDeviceFrame(address, payload)
    }

    override fun onDeviceFrame(address: String, payload: ByteString) {
        val instance = synchronized(lock) { instances.values.firstOrNull { it.slot.address == address } } ?: return
        if (!instance.serving) return
        val bytes = payload.toByteArray()
        val outs =
            synchronized(lock) {
                val result = instance.router.onFromRadio(bytes, instance.clients.keys.toSet())
                if (instance.router.hasConfiguration) instance.handshakeAsked = false
                result
            }
        for (out in outs) send(instance, out)
    }

    private fun send(instance: Instance, out: BridgeOut) {
        val channel = synchronized(lock) { instance.clients[out.clientId] } ?: return
        // A client that cannot keep up is closed instead of being sent a stream with holes in it.
        if (!channel.trySend(frame(out.payload)).isSuccess) {
            Logger.w { "Radio bridge: client ${out.clientId} is too slow, closing it" }
            channel.close()
        }
    }

    /** Sends a frame of a client to the device of [instance], through the app's session or the quiet link. */
    private fun toDevice(instance: Instance, bytes: ByteArray) {
        if (instance.slot.address == radioInterfaceService.getDeviceAddress()) {
            radioInterfaceService.sendToRadio(bytes)
        } else if (!pool.send(instance.slot.address, bytes)) {
            Logger.d { "Radio bridge: the device is not connected, a frame of a client was not sent" }
        }
    }

    /** Makes the device send its configuration once more, so that one passes by and is kept for the clients. */
    private suspend fun askConfiguration(instance: Instance) {
        if (instance.slot.address == radioInterfaceService.getDeviceAddress()) {
            // The device shown in the app: connecting again makes the app itself receive the configuration.
            radioInterfaceService.restartTransport()
        } else {
            val request = ToRadio.Builder().also { it.want_config_id = (1..Int.MAX_VALUE).random() }.build()
            pool.send(instance.slot.address, request.encode())
        }
    }

    private suspend fun serve(instance: Instance) = coroutineScope {
        val listen = instance.listen
        val selector = SelectorManager(dispatchers.io)
        val host = if (listen.lanAccess) "0.0.0.0" else "127.0.0.1"
        val server = aSocket(selector).tcp().bind(InetSocketAddress(host, listen.port))
        val scopeText = if (listen.lanAccess) "local network" else "this device only"
        val label = instance.slot.name?.takeIf { it.isNotBlank() } ?: "device ${instance.slot.id}"
        Logger.i { "Radio bridge ($label) listening on port ${listen.port} ($scopeText)" }
        synchronized(lock) { instance.router.reset() }
        instance.handshakeAsked = false
        instance.serving = true
        launch {
            while (true) {
                instance.handshakeRequests.receive()
                if (instance.handshakeAsked) continue
                instance.handshakeAsked = true
                Logger.i { "Radio bridge ($label): asking the device for its configuration for clients" }
                askConfiguration(instance)
            }
        }
        try {
            while (true) {
                val socket = server.accept()
                val admitted =
                    synchronized(lock) { instance.clients.size < instance.router.config.maxClients.coerceAtLeast(1) }
                if (!admitted) {
                    Logger.i { "Radio bridge ($label): too many clients, refusing one" }
                    socket.close()
                    continue
                }
                launch { handleClient(instance, socket, label) }
            }
        } finally {
            instance.serving = false
            server.close()
            selector.close()
            synchronized(lock) {
                instance.clients.values.forEach { it.close() }
                instance.clients.clear()
            }
        }
    }

    private suspend fun handleClient(instance: Instance, socket: Socket, label: String) = coroutineScope {
        val outbox = Channel<ByteArray>(OUTBOX_CAPACITY)
        val id =
            synchronized(lock) {
                val newId = nextClientId++
                instance.clients[newId] = outbox
                newId
            }
        Logger.i { "Radio bridge ($label): client $id connected" }
        val output = socket.openWriteChannel(autoFlush = true)
        val input = socket.openReadChannel()
        val writer = launch {
            try {
                for (bytes in outbox) output.writeFully(bytes)
                // The outbox was closed (the client said goodbye, or was too slow): end the connection.
                runCatching { socket.close() }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                if (e is CancellationException) throw e
                Logger.d { "Radio bridge ($label): client $id write ended: ${e.message}" }
            }
        }
        val codec =
            StreamFrameCodec(
                onPacketReceived = { payload -> onClientFrame(instance, id, payload, outbox) },
                logTag = "Bridge",
            )
        try {
            val buffer = ByteArray(READ_BUFFER)
            while (true) {
                val n = input.readAvailable(buffer)
                if (n < 0) break
                for (i in 0 until n) codec.processInputByte(buffer[i])
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            if (e is CancellationException) throw e
            Logger.d { "Radio bridge ($label): client $id read ended: ${e.message}" }
        } finally {
            synchronized(lock) {
                instance.clients.remove(id)
                instance.router.onClientGone(id)
            }
            outbox.close()
            writer.cancel()
            runCatching { socket.close() }
            Logger.i { "Radio bridge ($label): client $id gone" }
        }
    }

    private fun onClientFrame(instance: Instance, id: Int, payload: ByteArray, outbox: Channel<ByteArray>) {
        val (result, needsHandshake) =
            synchronized(lock) {
                instance.router.onClientFrame(id, payload) to instance.router.waitingWithoutHandshake
            }
        for (reply in result.replies) send(instance, reply)
        result.toRadio?.let { toDevice(instance, it) }
        if (result.close) outbox.close()
        if (needsHandshake) instance.handshakeRequests.trySend(Unit)
    }

    private fun frame(payload: ByteArray): ByteArray {
        val out = ByteArray(StreamFrameCodec.HEADER_SIZE + payload.size)
        out[0] = StreamFrameCodec.START1
        out[1] = StreamFrameCodec.START2
        out[2] = (payload.size shr 8).toByte()
        out[3] = payload.size.toByte()
        payload.copyInto(out, StreamFrameCodec.HEADER_SIZE)
        return out
    }

    private companion object {
        const val OUTBOX_CAPACITY = 256
        const val READ_BUFFER = 1024
    }
}
