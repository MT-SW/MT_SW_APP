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
@file:Suppress("TooGenericExceptionCaught", "TooManyFunctions")
@file:OptIn(ExperimentalForeignApi::class)

package org.meshtastic.core.takserver

import co.touchlab.kermit.Logger
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Network.nw_connection_cancel
import platform.Network.nw_connection_receive
import platform.Network.nw_connection_send
import platform.Network.nw_connection_set_queue
import platform.Network.nw_connection_set_state_changed_handler
import platform.Network.nw_connection_start
import platform.Network.nw_connection_state_cancelled
import platform.Network.nw_connection_state_failed
import platform.Network.nw_connection_state_ready
import platform.Network.nw_connection_t
import platform.Network.nw_content_context_create
import platform.darwin.dispatch_data_apply
import platform.darwin.dispatch_data_create
import platform.darwin.dispatch_data_t
import platform.darwin.dispatch_queue_t

/**
 * One connected TAK client (ATAK / iTAK on this phone) on the iOS listener. Same behaviour as the Android and computer
 * `TAKClientConnection`: the TAK protocol handshake (`t-x-takp-v` / `-q` / `-r`), `<event>` framing, ping replies and
 * a keepalive. Everything runs on the server's serial [queue], so no locking is needed.
 */
internal class IosTakConnection(
    private val connection: nw_connection_t,
    val clientInfo: TAKClientInfo,
    private val queue: dispatch_queue_t,
    private val onEvent: (TAKConnectionEvent) -> Unit,
) {
    private var info = clientInfo
    private val frames = CoTXmlFrameBuffer()
    private val context = nw_content_context_create("tak")
    private var closed = false

    /** True once the TLS handshake (and the client certificate check) has passed. */
    var isReady = false
        private set

    fun start() {
        nw_connection_set_queue(connection, queue)
        nw_connection_set_state_changed_handler(connection) { state, _ ->
            when (state) {
                nw_connection_state_ready ->
                    if (!isReady && !closed) {
                        isReady = true
                        onEvent(TAKConnectionEvent.Connected(info))
                        sendProtocolSupport()
                        receive()
                    }

                nw_connection_state_failed,
                nw_connection_state_cancelled,
                -> finish(TAKConnectionEvent.Disconnected)

                else -> Unit
            }
        }
        nw_connection_start(connection)
    }

    fun close() = finish(TAKConnectionEvent.Disconnected)

    fun send(cotMessage: CoTMessage) = sendXml(cotMessage.toXml())

    fun sendXml(xml: String) {
        if (closed) return
        val bytes = xml.encodeToByteArray()
        if (bytes.isEmpty()) return
        val data =
            bytes.usePinned { dispatch_data_create(it.addressOf(0), bytes.size.convert(), queue, null) } ?: return
        nw_connection_send(connection, data, context, true) { error ->
            if (error != null) finish(TAKConnectionEvent.Error(IllegalStateException("TAK send failed")))
        }
    }

    fun sendKeepalive() {
        val now = Clock.System.now()
        sendXml(eventXml("takPong", "t-x-d-d", now, now + TAK_KEEPALIVE_INTERVAL_MS.milliseconds, ""))
    }

    private fun receive() {
        if (closed) return
        nw_connection_receive(connection, 1.convert(), RECEIVE_MAX.convert()) { content, _, isComplete, error ->
            if (content != null) handleData(readBytes(content))
            when {
                error != null -> finish(TAKConnectionEvent.Error(IllegalStateException("TAK receive failed")))
                isComplete -> finish(TAKConnectionEvent.Disconnected)
                else -> receive()
            }
        }
    }

    private fun readBytes(data: dispatch_data_t): ByteArray {
        val chunks = ArrayList<ByteArray>()
        dispatch_data_apply(data) { _, _, buffer, size ->
            if (buffer != null && size > 0u) chunks += buffer.reinterpret<kotlinx.cinterop.ByteVar>().readBytes(size.toInt())
            true
        }
        return Der.concat(*chunks.toTypedArray())
    }

    private fun handleData(data: ByteArray) {
        frames.append(data).forEach { handleMessage(it) }
    }

    private fun handleMessage(xml: String) {
        if (xml.contains("t-x-c-t") || xml.contains("uid=\"ping\"")) {
            val now = Clock.System.now()
            sendXml(eventXml("takPong", "t-x-c-t-r", now, now + TAK_KEEPALIVE_INTERVAL_MS.milliseconds, ""))
            return
        }
        Logger.d { "RAW CoT IN (TAK ${info.id}): $xml" }
        CoTXmlParser(xml)
            .parse()
            .onSuccess { cot ->
                if (cot.type.startsWith("t-x-takp")) {
                    if (cot.type == "t-x-takp-q") sendProtocolResponse()
                    return@onSuccess
                }
                cot.contact?.let { contact ->
                    val updated =
                        info.copy(callsign = info.callsign ?: contact.callsign, uid = info.uid ?: cot.uid)
                    if (updated != info) {
                        info = updated
                        onEvent(TAKConnectionEvent.ClientInfoUpdated(updated))
                    }
                }
                onEvent(TAKConnectionEvent.Message(cot, info))
            }
            .onFailure { Logger.w(it) { "Failed to parse CoT XML from TAK client ${info.id}" } }
    }

    private fun sendProtocolSupport() {
        val now = Clock.System.now()
        val detail = "<TakControl><TakProtocolSupport version=\"0\"/></TakControl>"
        sendXml(eventXml(serverUid(), "t-x-takp-v", now, now + TAK_KEEPALIVE_INTERVAL_MS.milliseconds, detail))
    }

    private fun sendProtocolResponse() {
        val now = Clock.System.now()
        val detail = "<TakControl><TakResponse status=\"true\"/></TakControl>"
        sendXml(eventXml(serverUid(), "t-x-takp-r", now, now + TAK_KEEPALIVE_INTERVAL_MS.milliseconds, detail))
    }

    private fun serverUid() = "Meshtastic-TAK-Server-${Random.nextInt().toString(TAK_HEX_RADIX)}"

    private fun eventXml(uid: String, type: String, now: Instant, stale: Instant, detail: String): String {
        val detailContent = if (detail.isBlank()) "<detail/>" else "<detail>$detail</detail>"
        val point = "<point lat=\"0\" lon=\"0\" hae=\"0\" ce=\"$TAK_UNKNOWN_POINT_VALUE\" le=\"$TAK_UNKNOWN_POINT_VALUE\"/>"
        return "<event version=\"2.0\" uid=\"$uid\" type=\"$type\" time=\"$now\" start=\"$now\" stale=\"$stale\" how=\"m-g\">" +
            point +
            detailContent +
            "</event>"
    }

    private fun finish(event: TAKConnectionEvent) {
        if (closed) return
        closed = true
        frames.clear()
        onEvent(event)
        nw_connection_cancel(connection)
    }

    private companion object {
        const val RECEIVE_MAX = 65_536
    }
}
