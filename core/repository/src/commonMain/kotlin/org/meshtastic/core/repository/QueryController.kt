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

import org.meshtastic.core.model.Position
import org.meshtastic.proto.LocalStatsBroadcastConfig
import org.meshtastic.proto.OnDemandType
import org.meshtastic.proto.PresenceConfig

/**
 * Mesh query operations — position, traceroute, telemetry, user info, and metadata.
 *
 * These are "pull" operations that request data from remote nodes. When the SDK is adopted, implementations delegate to
 * `RadioClient.telemetry` and `RadioClient.routing` sub-APIs.
 *
 * @see RadioController which extends this interface for backward compatibility
 */
interface QueryController {

    /** Requests device metadata from a remote node. A [requestId] of 0 lets the sender assign one. */
    suspend fun refreshMetadata(destNum: Int, requestId: Int = 0)

    /** Requests the current GPS position from a remote node. A [requestId] of 0 lets the sender assign one. */
    suspend fun requestPosition(destNum: Int, currentPosition: Position, requestId: Int = 0)

    /** Requests detailed user info from a remote node. A [requestId] of 0 lets the sender assign one. */
    suspend fun requestUserInfo(destNum: Int, requestId: Int = 0)

    /** Sends an OnDemand diagnostics query (e.g. node stats) to a remote node. */
    suspend fun requestOnDemand(destNum: Int, requestType: OnDemandType)

    /** Sets both local stats broadcast options (basic and extended, with intervals) on [destNum] at once. */
    suspend fun setLocalStatsBroadcastConfig(destNum: Int, config: LocalStatsBroadcastConfig)

    /** Sets the activity status (presence) switch on [destNum]. */
    suspend fun setPresenceConfig(destNum: Int, config: PresenceConfig)

    /** Initiates a traceroute request to a remote node. */
    suspend fun requestTraceroute(requestId: Int, destNum: Int)

    /** Requests telemetry data from a remote node. */
    suspend fun requestTelemetry(requestId: Int, destNum: Int, typeValue: Int)

    /** Requests neighbor information (detected nodes) from a remote node. */
    suspend fun requestNeighborInfo(requestId: Int, destNum: Int)

    /**
     * Sets the GPIO pins selected in [gpioMask] on [destNum] to the corresponding bits in [gpioValue], via the Remote
     * Hardware module. See https://meshtastic.org/docs/configuration/module/remote-hardware/.
     *
     * @return true when the target confirmed delivery, false on NAK, timeout or local rejection.
     */
    suspend fun writeGpio(destNum: Int, gpioMask: Long, gpioValue: Long): Boolean

    /**
     * Reads the GPIO pins selected in [gpioMask] on [destNum] via the Remote Hardware module.
     *
     * @return the reported gpio_value bitmask, or `null` on timeout/no response.
     */
    suspend fun readGpio(destNum: Int, gpioMask: Long): Long?

    /** Asks [destNum] over the remote admin for the pins its Remote Hardware module may control; `null` on no answer. */
    suspend fun requestGpioPins(destNum: Int): List<org.meshtastic.proto.RemoteHardwarePin>?

    /** Whether [destNum] is known to run MT_SW_FIRMWARE v3 or newer. */
    fun gpioAdminSupportedFlow(destNum: Int): kotlinx.coroutines.flow.Flow<Boolean>

    /** Finds out the firmware version of [destNum] when it is not known yet (at most once a week). */
    suspend fun checkGpioVersion(destNum: Int)
}
