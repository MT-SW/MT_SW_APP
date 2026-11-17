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

import org.meshtastic.core.repository.RadioBridgeConfig
import org.meshtastic.proto.FromRadio
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.ToRadio

/** A frame (without the stream header) for one client of the bridge. */
class BridgeOut(val clientId: Int, val payload: ByteArray)

/** What to do after a client sent a frame: answers for clients, a frame for the radio, whether to close the client. */
class BridgeClientResult(
    val replies: List<BridgeOut> = emptyList(),
    val toRadio: ByteArray? = null,
    val close: Boolean = false,
)

/**
 * The rules of the bridge, without any socket (so they can be tested): what a client gets from the radio and what it
 * may send to it. The app already holds the one connection to the radio, so a client never talks to the radio's
 * configuration stream: it is given the copy the app received itself.
 *
 * - **Configuration.** Every frame from the start of a handshake (the radio's `my_info`) up to its `config_complete_id`
 *   is kept. A client that asks for the configuration (`want_config_id`) gets the copy, closed with its own number; the
 *   request is never passed to the radio, which would disturb the app's own session.
 * - **Live data.** Packets heard by the radio go to every client. Extras meant for the app (file transfer, MQTT proxy)
 *   do not.
 * - **Remote administration.** An answer to a packet a client sent (its `request_id`) goes back to that client only. An
 *   administration packet that is not an answer to a client is never shown to clients.
 * - **Restrictions** of [config] (all off by default) limit what clients may send.
 */
class BridgeRouter(var config: RadioBridgeConfig = RadioBridgeConfig()) {
    private val configFrames = ArrayList<ByteArray>()
    private var configComplete = false

    // Packet id -> the client that sent it; the oldest are forgotten first.
    private val owners = LinkedHashMap<Int, Int>()

    // Clients that asked for the configuration before it was complete: client -> the number to close it with.
    private val waiting = LinkedHashMap<Int, Int>()

    /** Whether the radio's configuration is complete, so a client can be given it. */
    val hasConfiguration: Boolean
        get() = configComplete

    /** A frame from the radio. [clients] are the connected clients. */
    fun onFromRadio(payload: ByteArray, clients: Set<Int>): List<BridgeOut> {
        val frame = runCatching { FromRadio.ADAPTER.decode(payload) }.getOrNull() ?: return emptyList()
        if (frame.my_info != null) {
            // A new handshake of the app with the radio: the copy starts again.
            configFrames.clear()
            configComplete = false
        }
        if (!configComplete) {
            if (frame.config_complete_id != 0) {
                configComplete = true
                return waiting.keys.toList().filter { it in clients }.flatMap { replay(it, waiting.getValue(it)) }
                    .also { waiting.clear() }
            }
            if (configFrames.size < MAX_CONFIG_FRAMES) configFrames.add(payload)
            return emptyList()
        }
        if (frame.xmodemPacket != null || frame.mqttClientProxyMessage != null) return emptyList()
        val data = frame.packet?.decoded
        val requestId = data?.request_id ?: 0
        val owner = if (requestId != 0) owners[requestId] else null
        return when {
            owner != null -> if (owner in clients) listOf(BridgeOut(owner, payload)) else emptyList()
            data?.portnum == PortNum.ADMIN_APP -> emptyList()
            else -> clients.map { BridgeOut(it, payload) }
        }
    }

    /** A frame from the client [clientId]. */
    fun onClientFrame(clientId: Int, payload: ByteArray): BridgeClientResult {
        val frame = runCatching { ToRadio.ADAPTER.decode(payload) }.getOrNull() ?: return BridgeClientResult()
        if (frame.disconnect) return BridgeClientResult(close = true)
        if (frame.want_config_id != 0) {
            return if (configComplete) {
                BridgeClientResult(replies = replay(clientId, frame.want_config_id))
            } else {
                waiting[clientId] = frame.want_config_id
                BridgeClientResult()
            }
        }
        val packet = frame.packet ?: return BridgeClientResult()
        val portnum = packet.decoded?.portnum
        val blocked =
            config.readOnly ||
                (config.blockAdmin && portnum == PortNum.ADMIN_APP) ||
                (config.blockMessages && portnum == PortNum.TEXT_MESSAGE_APP)
        if (blocked) return BridgeClientResult()
        if (packet.id != 0) {
            owners[packet.id] = clientId
            if (owners.size > MAX_OWNERS) owners.remove(owners.keys.first())
        }
        return BridgeClientResult(toRadio = payload)
    }

    /** The client [clientId] is gone: nothing more is kept for it. */
    fun onClientGone(clientId: Int) {
        waiting.remove(clientId)
        owners.entries.removeAll { it.value == clientId }
    }

    private fun replay(clientId: Int, nonce: Int): List<BridgeOut> {
        val done = FromRadio.Builder().also { it.config_complete_id = nonce }.build().encode()
        return configFrames.map { BridgeOut(clientId, it) } + BridgeOut(clientId, done)
    }

    private companion object {
        const val MAX_CONFIG_FRAMES = 5_000
        const val MAX_OWNERS = 512
    }
}
