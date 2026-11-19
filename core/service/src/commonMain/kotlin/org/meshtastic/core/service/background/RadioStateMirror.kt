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

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.meshtastic.core.repository.HandshakeConstants
import org.meshtastic.proto.Config
import org.meshtastic.proto.FromRadio
import org.meshtastic.proto.ModuleConfig

/**
 * A live copy of what one radio has told the app about itself: its own info, configuration, channels and the list of
 * nodes. Every frame the radio sends goes through it, so it is kept up to date all the time, whichever part of the app
 * listens to the connection.
 *
 * When the app view takes over a connection that is already open (switching between devices), the view asks the radio
 * for its configuration as it always does. The radio is not asked: the question is answered from this copy, with the
 * same frames the radio would send, so the connection just keeps running and nothing is negotiated again.
 */
internal class RadioStateMirror {
    private val lock = SynchronizedObject()

    // Everything but the nodes, in the order it first arrived; a newer frame of the same kind replaces the older one.
    private val setup = LinkedHashMap<String, ByteArray>()
    private val nodes = LinkedHashMap<Int, ByteArray>()
    private var hasMyInfo = false
    private var complete = false

    /** True once a whole configuration and node list were received, so a request can be answered from the copy. */
    val isComplete: Boolean
        get() = synchronized(lock) { complete }

    /** The connection ended: the copy no longer describes anything. */
    fun reset() {
        synchronized(lock) {
            setup.clear()
            nodes.clear()
            hasMyInfo = false
            complete = false
        }
    }

    /** A frame from the radio. */
    @Suppress("CyclomaticComplexMethod")
    fun onFromRadio(payload: ByteArray) {
        val frame = runCatching { FromRadio.ADAPTER.decode(payload) }.getOrNull() ?: return
        val config = frame.config
        val moduleConfig = frame.moduleConfig
        val channel = frame.channel
        val nodeInfo = frame.node_info
        val doneNonce = frame.config_complete_id ?: 0
        synchronized(lock) {
            when {
                // A new handshake of the radio with a client: the copy starts again.
                frame.my_info != null -> {
                    setup.clear()
                    nodes.clear()
                    complete = false
                    hasMyInfo = true
                    setup["my"] = payload
                }

                frame.deviceuiConfig != null -> setup["ui"] = payload

                frame.metadata != null -> setup["meta"] = payload

                frame.region_presets != null -> setup["regions"] = payload

                config != null -> setup["cfg:${variant(Config.ADAPTER.encode(config))}"] = payload

                moduleConfig != null -> setup["mod:${variant(ModuleConfig.ADAPTER.encode(moduleConfig))}"] = payload

                channel != null -> setup["ch:${channel.index}"] = payload

                frame.fileInfo != null -> setup["file:${payload.contentHashCode()}"] = payload

                nodeInfo != null -> nodes[nodeInfo.num] = payload

                // The list of nodes (not the configuration alone) was sent: the copy is whole.
                doneNonce != 0 && doneNonce != HandshakeConstants.CONFIG_NONCE && hasMyInfo -> complete = true
            }
        }
    }

    /**
     * The frames the radio would send for a request with [nonce], ending with the closing frame; null when the copy is
     * not whole yet (the request must then go to the radio).
     */
    fun replay(nonce: Int): List<ByteArray>? = synchronized(lock) {
        if (!complete) return null
        val done = FromRadio.Builder().also { it.config_complete_id = nonce }.build().encode()
        val frames =
            when (nonce) {
                HandshakeConstants.CONFIG_NONCE -> setup.values.toList()
                HandshakeConstants.NODE_INFO_NONCE -> nodes.values.toList()
                else -> setup.values.toList() + nodes.values.toList()
            }
        frames + done
    }

    /** Which of its kinds a configuration message is: the number of the one field that is set (the first byte). */
    private fun variant(encoded: ByteArray): Int = encoded.firstOrNull()?.toInt() ?: 0
}
