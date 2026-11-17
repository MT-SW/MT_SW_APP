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
package org.meshtastic.core.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import okio.ByteString

/**
 * The TCP bridge (MT_SW) of one radio: a local server that other programs can connect to as if they connected to the
 * radio itself. [enabled] is off by default. The restrictions are optional and all off by default.
 */
data class RadioBridgeConfig(
    val enabled: Boolean = false,
    /** The port; 0 means the default of the radio (see [defaultPort]). */
    val port: Int = 0,
    /** Off: only this device can connect (loopback). On: other devices of the local network can connect too. */
    val lanAccess: Boolean = false,
    /** On: clients can only read; nothing they send reaches the radio. */
    val readOnly: Boolean = false,
    /** On: remote administration (the admin port) sent by clients is not passed on. */
    val blockAdmin: Boolean = false,
    /** On: text messages sent by clients are not passed on. */
    val blockMessages: Boolean = false,
    /** The most clients connected at the same time. */
    val maxClients: Int = DEFAULT_MAX_CLIENTS,
) {
    /** The port to listen on for the saved radio [slotId]: the chosen one, or the default of that radio. */
    fun portFor(slotId: Int): Int = if (port in MIN_PORT..MAX_PORT) port else defaultPort(slotId)

    companion object {
        const val MIN_PORT = 1024
        const val MAX_PORT = 65535
        const val DEFAULT_MAX_CLIENTS = 4
        val MAX_CLIENTS_CHOICES = listOf(1, 2, 4, 8)

        /** 4404 for radio 1, 4405 for radio 2, 4406 for radio 3 (4403 is the port of the radio itself). */
        fun defaultPort(slotId: Int): Int = 4403 + slotId.coerceAtLeast(1)
    }
}

/** The bridge settings of every saved radio, kept per radio id (see [RadioSlotsPrefs]). */
interface RadioBridgePrefs {
    val configs: StateFlow<Map<Int, RadioBridgeConfig>>

    fun setConfig(slotId: Int, config: RadioBridgeConfig)
}

/**
 * Runs the bridge of the selected radio while the mesh service runs, and gets every frame the radio sends (after the
 * session check) so it can hand it on to the connected clients.
 */
interface RadioBridgeMonitor {
    fun start(scope: CoroutineScope)

    /** One frame (a FromRadio message) the radio sent to the app. */
    fun onFromRadio(payload: ByteString)
}
