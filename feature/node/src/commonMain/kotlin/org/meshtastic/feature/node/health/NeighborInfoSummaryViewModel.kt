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
package org.meshtastic.feature.node.health

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.NeighborInfo
import org.meshtastic.proto.PortNum

/** One node's direct-neighbor count, from the most recent NeighborInfo packet it sent. */
data class NeighborSummaryRow(val num: Int, val name: String, val neighborCount: Int)

data class NeighborInfoSummaryUiState(val rows: List<NeighborSummaryRow> = emptyList(), val reportingNodeCount: Int = 0)

@KoinViewModel
class NeighborInfoSummaryViewModel(
    private val meshLogRepository: MeshLogRepository,
    private val nodeRepository: NodeRepository,
) : ViewModel() {

    val uiState: StateFlow<NeighborInfoSummaryUiState> =
        combine(
            meshLogRepository.getLogsByPortNum(PortNum.NEIGHBORINFO_APP.value, MAX_LOGS),
            nodeRepository.getNodes(),
        ) { logs, nodes ->
            logs to nodes
        }
            .map { (logs, nodes) ->
                val nameByNum = nodes.associate { it.num to it.user.long_name.ifBlank { it.user.short_name } }
                val latestPerNode = logs.distinctBy { it.fromNum }
                val rows =
                    latestPerNode
                        .mapNotNull { log ->
                            val payload = log.fromRadio.packet?.decoded?.payload ?: return@mapNotNull null
                            val info =
                                runCatching { NeighborInfo.ADAPTER.decode(payload) }.getOrNull()
                                    ?: return@mapNotNull null
                            NeighborSummaryRow(
                                num = log.fromNum,
                                name = nameByNum[log.fromNum] ?: "!${log.fromNum.toString(16)}",
                                neighborCount = info.neighbors.size,
                            )
                        }
                        .sortedByDescending { it.neighborCount }
                NeighborInfoSummaryUiState(rows = rows, reportingNodeCount = rows.size)
            }
            .stateInWhileSubscribed(initialValue = NeighborInfoSummaryUiState())

    companion object {
        private const val MAX_LOGS = 2000
    }
}
