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

/**
 * Watches the nodes that have a "node silence alert" (set in the node details) and raises a notification when the radio
 * has not heard such a node for the chosen time.
 */
interface NodeSilenceMonitor {
    /** Starts watching in [scope]; the watching ends when the scope is cancelled. */
    fun start(scope: CoroutineScope)

    /**
     * Raises a test notification, built and shown the same way as the real one, as if [nodeNum] had just gone silent.
     * Quiet hours and the connection to the radio do not matter for it. Does nothing when the node is not known.
     */
    suspend fun sendTest(nodeNum: Int)
}
