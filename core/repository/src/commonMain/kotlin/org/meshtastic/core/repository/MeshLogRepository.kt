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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.meshtastic.core.model.MeshLog
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.MyNodeInfo
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.Telemetry

/**
 * Repository interface for managing and retrieving logs from the database.
 *
 * This component provides access to the application's message log, telemetry history, and debug records. It supports
 * reactive queries for packets, telemetry data, and node-specific logs.
 *
 * This interface is shared across platforms via Kotlin Multiplatform (KMP).
 */
@Suppress("TooManyFunctions")
interface MeshLogRepository {
    /** Retrieves all [MeshLog]s in the database, up to [maxItem]. */
    fun getAllLogs(maxItem: Int = DEFAULT_MAX_LOGS): Flow<List<MeshLog>>

    /**
     * Emits every [MeshLog] once, oldest first, then completes. The logs are read a page at a time rather than held in
     * memory together.
     */
    fun readAllLogsInReceiveOrder(): Flow<MeshLog>

    /** Retrieves all [MeshLog]s in the database without any limit. */
    fun getAllLogsUnbounded(): Flow<List<MeshLog>>

    /** Retrieves all [MeshLog]s for a given [portNum], across every sender — used for cross-node summaries. */
    fun getLogsByPortNum(portNum: Int, maxItem: Int = DEFAULT_MAX_LOGS): Flow<List<MeshLog>>

    /** Retrieves all [MeshLog]s for a given [portNum] since [sinceTimestamp], across every sender, uncapped. */
    fun getLogsByPortNumSince(portNum: Int, sinceTimestamp: Long): Flow<List<MeshLog>>

    /** Retrieves all [MeshLog]s from a single [nodeNum] since [sinceTimestamp], across every port. */
    fun getLogsFromNodeSince(nodeNum: Int, sinceTimestamp: Long): Flow<List<MeshLog>>

    /**
     * The newest [MeshLog] of [portNum] from every sender (nothing is cut off: older reports of the same sender are
     * simply not needed by callers that only look at each node's latest report).
     */
    fun getLatestLogsByPortNumPerSender(portNum: Int): Flow<List<MeshLog>> = getLogsByPortNumSince(portNum, 0L)

    /** Like [getAllLogsSince], but entries carry no text copy of the packet (`raw_message` is empty). */
    fun getAllLogsSinceSlim(sinceTimestamp: Long): Flow<List<MeshLog>> = getAllLogsSince(sinceTimestamp)

    /** Like [getLogsByPortNumSince], but entries carry no text copy of the packet (`raw_message` is empty). */
    fun getLogsByPortNumSinceSlim(portNum: Int, sinceTimestamp: Long): Flow<List<MeshLog>> =
        getLogsByPortNumSince(portNum, sinceTimestamp)

    /** Like [getLogsFromNodeSince], but entries carry no text copy of the packet (`raw_message` is empty). */
    fun getLogsFromNodeSinceSlim(nodeNum: Int, sinceTimestamp: Long): Flow<List<MeshLog>> =
        getLogsFromNodeSince(nodeNum, sinceTimestamp)

    /** Retrieves all [MeshLog]s across every sender and port since [sinceTimestamp]. */
    fun getAllLogsSince(sinceTimestamp: Long): Flow<List<MeshLog>>

    /** Retrieves all [MeshLog]s associated with a specific [nodeNum] and [portNum]. */
    fun getLogsFrom(nodeNum: Int, portNum: Int): Flow<List<MeshLog>>

    /** Like [getLogsFrom], but reads only the newest [maxItem] entries -- a quick first look at a large history. */
    fun getLogsFromLimited(nodeNum: Int, portNum: Int, maxItem: Int): Flow<List<MeshLog>> =
        getLogsFrom(nodeNum, portNum)

    /**
     * The single most recent [MeshLog] per distinct sender, across every port — used for live per-node reads (e.g.
     * relay) without a persisted column.
     */
    fun getLatestLogPerNode(): Flow<List<MeshLog>>

    /** Retrieves all [MeshLog]s containing [MeshPacket]s for a specific [nodeNum]. */
    fun getMeshPacketsFrom(nodeNum: Int, portNum: Int = -1): Flow<List<MeshPacket>>

    /** Retrieves telemetry history for a specific node, automatically handling local node redirection. */
    fun getTelemetryFrom(nodeNum: Int): Flow<List<Telemetry>>

    /** Like [getTelemetryFrom], but reads only the newest [maxItem] entries. */
    fun getTelemetryFromLimited(nodeNum: Int, maxItem: Int): Flow<List<Telemetry>> = getTelemetryFrom(nodeNum)

    /** Like [getMeshPacketsFrom], but reads only the newest [maxItem] entries. */
    fun getMeshPacketsFromLimited(nodeNum: Int, portNum: Int = -1, maxItem: Int): Flow<List<MeshPacket>> =
        getMeshPacketsFrom(nodeNum, portNum)

    /**
     * Retrieves all outgoing request logs for a specific [targetNodeNum] and [portNum].
     *
     * A request log is defined as an outgoing packet where `want_response` is true.
     */
    fun getRequestLogs(targetNodeNum: Int, portNum: PortNum): Flow<List<MeshLog>>

    /** Like [getRequestLogs], but looks only through the newest [maxItem] outgoing entries of that port. */
    fun getRequestLogsLimited(targetNodeNum: Int, portNum: PortNum, maxItem: Int): Flow<List<MeshLog>> =
        getRequestLogs(targetNodeNum, portNum)

    /** Returns the cached [MyNodeInfo] from the system logs. */
    fun getMyNodeInfo(): Flow<MyNodeInfo?>

    /** Persists a new log entry to the database. */
    suspend fun insert(log: MeshLog)

    /** Clears all logs from the database. */
    suspend fun deleteAll()

    /** Deletes a specific log entry by its [uuid]. */
    suspend fun deleteLog(uuid: String)

    /** Deletes all logs associated with a specific [nodeNum] and [portNum]. */
    suspend fun deleteLogs(nodeNum: Int, portNum: Int)

    /** Deletes only local stats telemetry logs for [nodeNum], preserving other telemetry logs. */
    suspend fun deleteLocalStatsLogs(nodeNum: Int)

    /**
     * Prunes the log database based on the configured [retentionDays], which carries the [MeshLogRetention] sentinels:
     * [MeshLogRetention.KEEP_FOREVER] deletes nothing and [MeshLogRetention.ONE_HOUR] keeps only the last hour.
     */
    suspend fun deleteLogsOlderThan(retentionDays: Int)

    companion object {
        const val DEFAULT_MAX_LOGS = 5000
    }
}
