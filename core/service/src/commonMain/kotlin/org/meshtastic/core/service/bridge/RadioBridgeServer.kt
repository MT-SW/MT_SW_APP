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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import okio.ByteString
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.network.transport.StreamFrameCodec
import org.meshtastic.core.repository.RadioBridgeConfig
import org.meshtastic.core.repository.RadioBridgeMonitor
import org.meshtastic.core.repository.RadioBridgePrefs
import org.meshtastic.core.repository.RadioInterfaceService
import org.meshtastic.core.repository.RadioSlotsPrefs
import kotlin.concurrent.Volatile

/**
 * The TCP bridge (MT_SW) of the selected radio, written for this app: a server on the device that speaks the same
 * stream protocol as the radio's own TCP port, so other programs can connect to the radio through the app while the
 * app keeps its single connection. What clients may see and send is decided by [BridgeRouter]. Errors only go to the
 * log. The server runs while the mesh service runs and follows the settings: it stops when the bridge is switched off,
 * moves when the port, the local-network access or the radio changes, and takes the restrictions and the number of
 * clients live, without dropping anyone.
 *
 * The router and the client list are shared by the radio receive loop and the sockets, so every access goes through
 * [lock] (never held while suspending).
 */
@Single
class RadioBridgeServer(
    private val radioInterfaceService: RadioInterfaceService,
    private val slotsPrefs: RadioSlotsPrefs,
    private val bridgePrefs: RadioBridgePrefs,
    private val dispatchers: CoroutineDispatchers,
) : RadioBridgeMonitor {
    private val lock = SynchronizedObject()
    private val router = BridgeRouter()
    private val clients = LinkedHashMap<Int, Channel<ByteArray>>()
    private var nextClientId = 1

    @Volatile private var serving = false

    // True from the moment the app was asked to connect to the radio again (to get a configuration to hand out) until
    // the configuration is there.
    @Volatile private var handshakeAsked = false
    private val handshakeRequests = Channel<Unit>(Channel.CONFLATED)

    /** Where the server listens; a change of this moves the server, a change of anything else does not. */
    private data class Listen(val port: Int, val lanAccess: Boolean)

    override fun start(scope: CoroutineScope) {
        // Restrictions and the number of clients apply at once.
        scope.launch {
            combine(slotsPrefs.selectedId, bridgePrefs.configs) { id, all ->
                id?.let { all[it] } ?: RadioBridgeConfig()
            }
                .collect { config -> synchronized(lock) { router.config = config } }
        }
        scope.launch {
            combine(slotsPrefs.selectedId, bridgePrefs.configs) { id, all ->
                val config = id?.let { all[it] } ?: RadioBridgeConfig()
                if (id == null || !config.enabled) null else Listen(config.portFor(id), config.lanAccess)
            }
                .distinctUntilChanged()
                .collectLatest { listen ->
                    if (listen == null) return@collectLatest
                    try {
                        serve(listen)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        Logger.w(e) { "Radio bridge stopped: ${e.message}" }
                    }
                }
        }
    }

    override fun onFromRadio(payload: ByteString) {
        if (!serving) return
        val bytes = payload.toByteArray()
        val outs =
            synchronized(lock) {
                val result = router.onFromRadio(bytes, clients.keys.toSet())
                if (router.hasConfiguration) handshakeAsked = false
                result
            }
        for (out in outs) send(out)
    }

    private fun send(out: BridgeOut) {
        val channel = synchronized(lock) { clients[out.clientId] } ?: return
        // A client that cannot keep up is closed instead of being sent a stream with holes in it.
        if (!channel.trySend(frame(out.payload)).isSuccess) {
            Logger.w { "Radio bridge: client ${out.clientId} is too slow, closing it" }
            channel.close()
        }
    }

    private suspend fun serve(listen: Listen) = coroutineScope {
        val selector = SelectorManager(dispatchers.io)
        val host = if (listen.lanAccess) "0.0.0.0" else "127.0.0.1"
        val server = aSocket(selector).tcp().bind(InetSocketAddress(host, listen.port))
        val scopeText = if (listen.lanAccess) "local network" else "this device only"
        Logger.i { "Radio bridge listening on port ${listen.port} ($scopeText)" }
        synchronized(lock) { router.reset() }
        handshakeAsked = false
        serving = true
        // A client that asks for the configuration before the app has one (the bridge was turned on while connected)
        // makes the app connect to the radio again once, so that the configuration passes by and is kept.
        launch {
            while (true) {
                handshakeRequests.receive()
                if (handshakeAsked) continue
                handshakeAsked = true
                Logger.i { "Radio bridge: reconnecting the radio once to get its configuration for clients" }
                radioInterfaceService.restartTransport()
            }
        }
        try {
            while (true) {
                val socket = server.accept()
                val admitted = synchronized(lock) { clients.size < router.config.maxClients.coerceAtLeast(1) }
                if (!admitted) {
                    Logger.i { "Radio bridge: too many clients, refusing one" }
                    socket.close()
                    continue
                }
                launch { handleClient(socket) }
            }
        } finally {
            serving = false
            server.close()
            selector.close()
            synchronized(lock) {
                clients.values.forEach { it.close() }
                clients.clear()
            }
        }
    }

    private suspend fun handleClient(socket: Socket) = coroutineScope {
        val outbox = Channel<ByteArray>(OUTBOX_CAPACITY)
        val id =
            synchronized(lock) {
                val newId = nextClientId++
                clients[newId] = outbox
                newId
            }
        Logger.i { "Radio bridge: client $id connected" }
        val output = socket.openWriteChannel(autoFlush = true)
        val input = socket.openReadChannel()
        val writer = launch {
            try {
                for (bytes in outbox) output.writeFully(bytes)
                // The outbox was closed (the client said goodbye, or was too slow): end the connection.
                runCatching { socket.close() }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                if (e is CancellationException) throw e
                Logger.d { "Radio bridge: client $id write ended: ${e.message}" }
            }
        }
        val codec =
            StreamFrameCodec(onPacketReceived = { payload -> onClientFrame(id, payload, outbox) }, logTag = "Bridge")
        try {
            val buffer = ByteArray(READ_BUFFER)
            while (true) {
                val n = input.readAvailable(buffer)
                if (n < 0) break
                for (i in 0 until n) codec.processInputByte(buffer[i])
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            if (e is CancellationException) throw e
            Logger.d { "Radio bridge: client $id read ended: ${e.message}" }
        } finally {
            synchronized(lock) {
                clients.remove(id)
                router.onClientGone(id)
            }
            outbox.close()
            writer.cancel()
            runCatching { socket.close() }
            Logger.i { "Radio bridge: client $id gone" }
        }
    }

    private fun onClientFrame(id: Int, payload: ByteArray, outbox: Channel<ByteArray>) {
        val (result, needsHandshake) =
            synchronized(lock) { router.onClientFrame(id, payload) to router.waitingWithoutHandshake }
        for (reply in result.replies) send(reply)
        result.toRadio?.let { radioInterfaceService.sendToRadio(it) }
        if (result.close) outbox.close()
        if (needsHandshake) handshakeRequests.trySend(Unit)
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
