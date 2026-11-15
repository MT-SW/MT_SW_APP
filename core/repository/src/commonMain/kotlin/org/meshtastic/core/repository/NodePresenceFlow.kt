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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodePresence
import org.meshtastic.core.model.NodePresenceState
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.model.NodePresenceLogs
import org.meshtastic.core.model.util.ON_DEMAND_PORT_NUM

private const val PRESENCE_DEBOUNCE_MS = 3_000L

/**
 * MT_SW: the last activity status of every node that shares one, read from the logged on-demand answers (port 354).
 * Every received packet touches the log, so it waits for a quiet moment instead of reading it again each time.
 */
@OptIn(FlowPreview::class)
fun MeshLogRepository.nodePresenceFlow(): Flow<Map<Int, NodePresence>> =
    getLogsByPortNumSince(ON_DEMAND_PORT_NUM, nowMillis - NodePresenceLogs.WINDOW_MILLIS)
        .debounce(PRESENCE_DEBOUNCE_MS)
        .map { NodePresenceLogs.latestPerNode(it) }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * MT_SW: the numbers of the nodes that report someone is at them now, for the map filter "Active only". The same rule as
 * the node list: an "active" report holds for the time chosen in the settings after the last sign of life.
 */
fun activeNodeNumsFlow(
    meshLogRepository: MeshLogRepository,
    nodes: Flow<Map<Int, Node>>,
    uiOptions: Flow<Map<String, Int>>,
): Flow<Set<Int>> = combine(meshLogRepository.nodePresenceFlow(), nodes, uiOptions) { presence, byNum, options ->
    val steps = NodePresence.EXPIRY_STEPS_MINUTES
    val expiryMillis = steps[UiOptions(options)[UiOptions.PRESENCE_EXPIRY].coerceIn(0, steps.lastIndex)] * MILLIS_PER_MINUTE
    val now = nowMillis
    presence
        .filter { (num, status) ->
            status.effective(now, byNum[num]?.lastHeard ?: 0, expiryMillis) == NodePresenceState.ONLINE
        }
        .keys
}
