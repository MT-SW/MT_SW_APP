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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.NodePresence
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
