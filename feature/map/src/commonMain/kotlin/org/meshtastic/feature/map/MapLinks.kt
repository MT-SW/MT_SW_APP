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
package org.meshtastic.feature.map

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.MeshLog
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.fullRouteDiscovery
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.ui.viewmodel.settleBursts
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.NeighborInfo
import org.meshtastic.proto.PortNum

/** How a link between two nodes is known, from the most to the least certain. */
enum class MapLinkKind {
    /** One of the two nodes reported the other as a direct neighbour (NeighborInfo). */
    NEIGHBOR,

    /** The two were consecutive hops of a traceroute. */
    TRACEROUTE,

    /** The connected radio hears the node directly, with no relay in between. */
    DIRECT,
}

/** A radio link between two nodes. [snrDb] is the signal-to-noise ratio of the link when it is known. */
data class MapLink(val nodeA: Int, val nodeB: Int, val kind: MapLinkKind, val snrDb: Float?)

/**
 * The links the map draws between nodes, gathered from what the app already stores: neighbour reports, traceroute
 * results and the connected radio's own direct receptions. Read off the main thread, and re-read only after a burst of
 * new packets has died down.
 */
@KoinViewModel
class MapLinksViewModel(nodeRepository: NodeRepository, meshLogRepository: MeshLogRepository) : ViewModel() {

    val links: StateFlow<List<MapLink>> =
        combine(
            // Evaluated on every (re)subscription, so the window never stays stuck at the moment the app started.
            flow { emitAll(meshLogRepository.getLatestLogsByPortNumPerSender(PortNum.NEIGHBORINFO_APP.value)) }
                .settleBursts(LINKS_QUIET_MILLIS),
            flow {
                emitAll(
                    meshLogRepository.getLogsByPortNumSinceSlim(
                        PortNum.TRACEROUTE_APP.value,
                        nowMillis - LINKS_WINDOW_MILLIS,
                    ),
                )
            }
                .settleBursts(LINKS_QUIET_MILLIS),
            nodeRepository.nodeDBbyNum.settleBursts(LINKS_QUIET_MILLIS),
            nodeRepository.myNodeInfo,
        ) { neighborLogs, tracerouteLogs, nodes, myNodeInfo ->
            buildMapLinks(
                neighborLogs = neighborLogs,
                tracerouteLogs = tracerouteLogs,
                nodes = nodes,
                myNodeNum = myNodeInfo?.myNodeNum,
                nowMillis = nowMillis,
                nowSeconds = nowSeconds,
            )
        }
            .flowOn(Dispatchers.Default)
            .catch { Logger.e(it) { "Map links failed" } }
            .stateInWhileSubscribed(initialValue = emptyList())
}

/**
 * Combines every source into one link per pair of nodes. When the same pair is known several ways the most certain one
 * wins ([MapLinkKind.NEIGHBOR] over [MapLinkKind.TRACEROUTE] over [MapLinkKind.DIRECT]). Pairs where either node is
 * unknown to the app are dropped; whether a node has a position is checked when the line is drawn.
 */
@Suppress("LongParameterList", "CyclomaticComplexMethod")
internal fun buildMapLinks(
    neighborLogs: List<MeshLog>,
    tracerouteLogs: List<MeshLog>,
    nodes: Map<Int, Node>,
    myNodeNum: Int?,
    nowMillis: Long,
    nowSeconds: Long,
): List<MapLink> {
    val byPair = LinkedHashMap<Long, MapLink>()

    fun add(a: Int, b: Int, kind: MapLinkKind, snrDb: Float?) {
        if (a == b || a !in nodes || b !in nodes) return
        val key = (minOf(a, b).toLong() shl INT_BITS) or (maxOf(a, b).toLong() and INT_MASK)
        val existing = byPair[key]
        if (existing == null || kind.ordinal <= existing.kind.ordinal) byPair[key] = MapLink(a, b, kind, snrDb)
    }

    // The connected radio hears these directly: a node with no hops in between and a signal reading.
    if (myNodeNum != null) {
        for (node in nodes.values) {
            if (node.num != myNodeNum && node.hopsAway == 0 && node.snr != Float.MAX_VALUE) {
                if (node.lastHeard.toLong() > nowSeconds - DIRECT_WINDOW_SECONDS) {
                    add(myNodeNum, node.num, MapLinkKind.DIRECT, node.snr)
                }
            }
        }
    }

    val cutoff = nowMillis - LINKS_WINDOW_MILLIS
    for (log in tracerouteLogs) {
        val route = log.fromRadio.packet?.fullRouteDiscovery ?: continue
        addHops(route.route, route.snr_towards) { a, b, snr -> add(a, b, MapLinkKind.TRACEROUTE, snr) }
        addHops(route.route_back, route.snr_back) { a, b, snr -> add(a, b, MapLinkKind.TRACEROUTE, snr) }
    }

    for (log in neighborLogs) {
        if (log.received_date < cutoff) continue
        // The connected radio's own reports are stored under node 0.
        val sender = if (log.fromNum == 0 && myNodeNum != null) myNodeNum else log.fromNum
        val payload = log.fromRadio.packet?.decoded?.payload ?: continue
        val info = runCatching { NeighborInfo.ADAPTER.decode(payload) }.getOrNull() ?: continue
        for (neighbor in info.neighbors) add(sender, neighbor.node_id, MapLinkKind.NEIGHBOR, neighbor.snr)
    }
    return byPair.values.toList()
}

/** Consecutive pairs of [route], each with its SNR (quarter dB; -128 or a list of the wrong length means unknown). */
private fun addHops(route: List<Int>, snr: List<Int>, add: (Int, Int, Float?) -> Unit) {
    val snrUsable = snr.size == route.size - 1
    for (i in 0 until route.size - 1) {
        val raw = if (snrUsable) snr[i] else UNKNOWN_SNR
        add(route[i], route[i + 1], if (raw == UNKNOWN_SNR) null else raw / SNR_UNITS_PER_DB)
    }
}

private const val LINKS_QUIET_MILLIS = 2_000L
private const val LINKS_WINDOW_MILLIS = 24 * 60 * 60 * 1000L
private const val DIRECT_WINDOW_SECONDS = 2 * 60 * 60L
private const val INT_BITS = 32
private const val INT_MASK = 0xFFFFFFFFL
private const val UNKNOWN_SNR = -128
private const val SNR_UNITS_PER_DB = 4f
