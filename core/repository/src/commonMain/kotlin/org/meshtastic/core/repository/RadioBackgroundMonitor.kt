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

/**
 * What a device is called on its plate (MT_SW): the node number of the device (it gives the colour) and its short name.
 */
data class DeviceBadge(val nodeNum: Int, val shortName: String)

/**
 * Starts the background collection (MT_SW) from the saved radios that are not in use: while the mesh service runs, each
 * of them is connected quietly and the packets it hears are kept in its own database.
 */
interface RadioBackgroundMonitor {
    /** The plate of each saved device by address, read from the database of that device. */
    val badges: StateFlow<Map<String, DeviceBadge>>

    /** The addresses of the radios that are connected quietly right now (the radio in use is not among them). */
    val connectedAddresses: StateFlow<Set<String>>

    /** What each background device is doing while it connects (config, channels, node list), by address; empty when done. */
    val progressByAddress: StateFlow<Map<String, String>>

    /** How many messages reached each background device (by address) since the device was last shown; empty at 0. */
    val unreadByAddress: StateFlow<Map<String, Int>>

    /** The device is shown now: its count of new messages starts again. */
    fun clearUnread(address: String)

    fun start(scope: CoroutineScope)
}
