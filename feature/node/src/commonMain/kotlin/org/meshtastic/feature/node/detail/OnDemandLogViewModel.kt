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
package org.meshtastic.feature.node.detail

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.model.MeshLog
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeManager
import org.meshtastic.core.ui.viewmodel.safeLaunch
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.AirActivityEntry
import org.meshtastic.proto.ExchangeEntry
import org.meshtastic.proto.FwPlusVersion
import org.meshtastic.proto.NodeEntry
import org.meshtastic.proto.NodeStats
import org.meshtastic.proto.OnDemand
import org.meshtastic.proto.OnDemandResponse
import org.meshtastic.proto.OnDemandType
import org.meshtastic.proto.Ping
import org.meshtastic.proto.PortCounterEntry
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.RoutingErrorEntry

/** UI state for the OnDemand diagnostics screen — one field per query type. */
data class OnDemandLogUiState(
    val nodeStats: NodeStats? = null,
    val pingResult: Ping? = null,
    val pingRoundTripMs: Long? = null,
    val pingAckResult: Ping? = null,
    val pingAckRoundTripMs: Long? = null,
    val nodesOnline: List<NodeEntry> = emptyList(),
    val routingErrors: List<RoutingErrorEntry> = emptyList(),
    val portCounterHistory: List<PortCounterEntry> = emptyList(),
    val airActivityHistory: List<AirActivityEntry> = emptyList(),
    val exchangeLog: List<ExchangeEntry> = emptyList(),
    val rxAvgTimeHistory: List<Int> = emptyList(),
    val rxPacketHistory: List<Int> = emptyList(),
    val fwPlusVersion: FwPlusVersion? = null,
    val receivedAt: Map<OnDemandType, Long> = emptyMap(),
)

/**
 * ViewModel for the OnDemand diagnostics screen. Reads every response type live from [MeshLog] on port
 * [PortNum.ON_DEMAND_APP] — nothing new is persisted, matching the app's Network Health/Traceroute precedent.
 *
 * Nodes Online arrives split across several packets ([OnDemand.packet_index]/[OnDemand.packet_total]);
 * [decodeNodesOnline] merges the newest copy received for each index. Best-effort — not yet verified against real
 * multi-segment traffic.
 *
 * Ping/Ping-Ack round-trip time is measured client-side: [requestPing]/[requestPingAck] stamp the tap time, and
 * [updateRoundTrip] attributes the next newer matching response's [MeshLog.received_date] to it once. Not
 * request-id correlated (OnDemand responses don't carry one) — acceptable for a manually-tapped diagnostics screen
 * with one request in flight at a time.
 */
@KoinViewModel
class OnDemandLogViewModel(
    private val destNum: Int,
    private val nodeRequestActions: NodeRequestActions,
    private val meshLogRepository: MeshLogRepository,
    private val nodeManager: NodeManager,
) : ViewModel() {

    private val longName: String
        get() = nodeManager.nodeDBbyNodeNum[destNum]?.user?.long_name.orEmpty()

    @Volatile private var pingSentAtMillis: Long? = null
    @Volatile private var pingAckSentAtMillis: Long? = null
    @Volatile private var lastPingRoundTripMs: Long? = null
    @Volatile private var lastPingAckRoundTripMs: Long? = null
    @Volatile private var lastAttributedPingReceivedDate: Long = 0L
    @Volatile private var lastAttributedPingAckReceivedDate: Long = 0L

    val uiState: StateFlow<OnDemandLogUiState> =
        meshLogRepository
            .getLogsFrom(destNum, PortNum.ON_DEMAND_APP.value)
            .map(::decodeState)
            .stateInWhileSubscribed(initialValue = OnDemandLogUiState())

    private fun decodeState(logs: List<MeshLog>): OnDemandLogUiState {
        val decoded =
            logs.mapNotNull { log ->
                log.fromRadio
                    ?.packet
                    ?.decoded
                    ?.payload
                    ?.let { payload -> runCatching { OnDemand.ADAPTER.decode(payload) }.getOrNull() }
                    ?.let { it to log.received_date }
            }

        fun <T> latest(vararg types: OnDemandType, extract: (OnDemandResponse) -> T?): T? =
            decoded
                .filter { (od, _) -> od.response?.response_type in types }
                .maxByOrNull { it.second }
                ?.first
                ?.response
                ?.let(extract)

        return OnDemandLogUiState(
            nodeStats = latest(OnDemandType.RESPONSE_NODE_STATS) { it.node_stats },
            pingResult = latest(OnDemandType.RESPONSE_PING) { it.ping },
            pingRoundTripMs = updateRoundTrip(
                decoded = decoded,
                type = OnDemandType.RESPONSE_PING,
                sentAt = pingSentAtMillis,
                clearSentAt = { pingSentAtMillis = null },
                lastAttributed = lastAttributedPingReceivedDate,
                setLastAttributed = { lastAttributedPingReceivedDate = it },
                lastResult = lastPingRoundTripMs,
                setLastResult = { lastPingRoundTripMs = it },
            ),
            pingAckResult = latest(OnDemandType.RESPONSE_PING_ACK) { it.ping },
            pingAckRoundTripMs = updateRoundTrip(
                decoded = decoded,
                type = OnDemandType.RESPONSE_PING_ACK,
                sentAt = pingAckSentAtMillis,
                clearSentAt = { pingAckSentAtMillis = null },
                lastAttributed = lastAttributedPingAckReceivedDate,
                setLastAttributed = { lastAttributedPingAckReceivedDate = it },
                lastResult = lastPingAckRoundTripMs,
                setLastResult = { lastPingAckRoundTripMs = it },
            ),
            nodesOnline = decodeNodesOnline(decoded),
            routingErrors =
                latest(OnDemandType.RESPONSE_ROUTING_ERRORS) { it.routing_errors }?.routing_errors.orEmpty(),
            portCounterHistory =
                latest(OnDemandType.RESPONSE_PORT_COUNTER_HISTORY) { it.port_counter_history }
                    ?.port_counter_history
                    .orEmpty(),
            airActivityHistory =
                latest(OnDemandType.RESPONSE_AIR_ACTIVITY_HISTORY) { it.air_activity_history }
                    ?.air_activity_history
                    .orEmpty(),
            exchangeLog =
                latest(OnDemandType.RESPONSE_PACKET_EXCHANGE_HISTORY) { it.exchange_packet_log }
                    ?.exchange_list
                    .orEmpty(),
            rxAvgTimeHistory =
                latest(OnDemandType.RESPONSE_RX_AVG_TIME) { it.rx_avg_time_history }?.rx_avg_history.orEmpty(),
            rxPacketHistory =
                latest(OnDemandType.RESPONSE_PACKET_RX_HISTORY) { it.rx_packet_history }
                    ?.rx_packet_history
                    .orEmpty(),
            fwPlusVersion = latest(OnDemandType.RESPONSE_FW_PLUS_VERSION) { it.fw_plus_version },
            receivedAt = decoded
                .mapNotNull { (od, ts) -> od.response?.response_type?.let { it to ts } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, timestamps) -> timestamps.max() },
        )
    }

    /**
     * Attributes the most recent [type] response's timestamp to the pending request's send time, once, then
     * remembers that result until the next request overwrites it.
     */
    private fun updateRoundTrip(
        decoded: List<Pair<OnDemand, Long>>,
        type: OnDemandType,
        sentAt: Long?,
        clearSentAt: () -> Unit,
        lastAttributed: Long,
        setLastAttributed: (Long) -> Unit,
        lastResult: Long?,
        setLastResult: (Long?) -> Unit,
    ): Long? {
        val newest = decoded.filter { (od, _) -> od.response?.response_type == type }.maxByOrNull { it.second }
        if (newest != null && newest.second > lastAttributed) {
            setLastAttributed(newest.second)
            if (sentAt != null) {
                setLastResult((newest.second - sentAt).coerceAtLeast(0))
                clearSentAt()
            }
        }
        return lastResult
    }

    /**
     * Merges Nodes Online segments: keeps the most recently received copy of each [OnDemand.packet_index], then
     * concatenates 1..packet_total in order. Best-effort — not yet verified against real multi-segment traffic.
     */
    private fun decodeNodesOnline(decoded: List<Pair<OnDemand, Long>>): List<NodeEntry> {
        val segments =
            decoded.mapNotNull { (od, ts) ->
                val response = od.response?.takeIf { it.response_type == OnDemandType.RESPONSE_NODES_ONLINE }
                val nodeList = response?.node_list ?: return@mapNotNull null
                val index = od.packet_index ?: 1
                val total = od.packet_total ?: 1
                Triple(index, total, nodeList.node_list) to ts
            }
        if (segments.isEmpty()) return emptyList()
        val latestPerIndex = segments.groupBy { it.first.first }.mapValues { (_, v) -> v.maxByOrNull { it.second }!! }
        val total = latestPerIndex.values.maxOf { it.first.second }
        return (1..total).flatMap { index -> latestPerIndex[index]?.first?.third.orEmpty() }
    }

    fun requestNodeStats() = requestOnDemand(OnDemandType.REQUEST_NODE_STATS)

    fun requestPing() {
        pingSentAtMillis = System.currentTimeMillis()
        requestOnDemand(OnDemandType.REQUEST_PING)
    }

    fun requestPingAck() {
        pingAckSentAtMillis = System.currentTimeMillis()
        requestOnDemand(OnDemandType.REQUEST_PING_ACK)
    }

    fun requestNodesOnline() = requestOnDemand(OnDemandType.REQUEST_NODES_ONLINE)

    fun requestRoutingErrors() = requestOnDemand(OnDemandType.REQUEST_ROUTING_ERRORS)

    fun requestPortCounterHistory() = requestOnDemand(OnDemandType.REQUEST_PORT_COUNTER_HISTORY)

    fun requestAirActivityHistory() = requestOnDemand(OnDemandType.REQUEST_AIR_ACTIVITY_HISTORY)

    fun requestExchangeLog() = requestOnDemand(OnDemandType.REQUEST_PACKET_EXCHANGE_HISTORY)

    fun requestRxAvgTimeHistory() = requestOnDemand(OnDemandType.REQUEST_RX_AVG_TIME)

    fun requestRxPacketHistory() = requestOnDemand(OnDemandType.REQUEST_PACKET_RX_HISTORY)

    fun requestFwPlusVersion() = requestOnDemand(OnDemandType.REQUEST_FW_PLUS_VERSION)

    private fun requestOnDemand(type: OnDemandType) =
        safeLaunch(tag = "requestOnDemand-${type.name}") { nodeRequestActions.requestOnDemand(destNum, longName, type) }
}