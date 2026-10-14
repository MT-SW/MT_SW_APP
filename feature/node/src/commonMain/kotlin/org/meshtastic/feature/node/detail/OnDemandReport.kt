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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.on_demand_air_activity
import org.meshtastic.core.resources.on_demand_exchange_log
import org.meshtastic.core.resources.on_demand_fw_plus_version
import org.meshtastic.core.resources.on_demand_hops
import org.meshtastic.core.resources.on_demand_log_title
import org.meshtastic.core.resources.on_demand_no_errors
import org.meshtastic.core.resources.on_demand_node_stats
import org.meshtastic.core.resources.on_demand_nodes_online
import org.meshtastic.core.resources.on_demand_ping
import org.meshtastic.core.resources.on_demand_ping_ack
import org.meshtastic.core.resources.on_demand_port_counters
import org.meshtastic.core.resources.on_demand_received_label
import org.meshtastic.core.resources.on_demand_round_trip
import org.meshtastic.core.resources.on_demand_routing_errors
import org.meshtastic.core.resources.on_demand_rssi
import org.meshtastic.core.resources.on_demand_rx_avg_time
import org.meshtastic.core.resources.on_demand_rx_packet_history
import org.meshtastic.core.resources.on_demand_snr
import org.meshtastic.feature.node.health.NetworkReport
import org.meshtastic.feature.node.health.ReportBar
import org.meshtastic.feature.node.health.ReportLine
import org.meshtastic.feature.node.health.ReportSection
import org.meshtastic.proto.OnDemandType
import org.meshtastic.proto.Ping

private const val WINDOW_STEP_MINUTES = 10

/**
 * Everything the on-demand screen has received so far, as the same plain report the network summary uses, so it can be
 * saved as PDF, HTML, Markdown, text or CSV. Only queries that have an answer get a section; the report is empty
 * until at least one answer arrived. The "generated" stamp is filled in when the file is written.
 */
@Composable
internal fun rememberOnDemandReport(uiState: OnDemandLogUiState): NetworkReport {
    val title = stringResource(Res.string.on_demand_log_title)
    val statsTitle = stringResource(Res.string.on_demand_node_stats)
    val pingTitle = stringResource(Res.string.on_demand_ping)
    val pingAckTitle = stringResource(Res.string.on_demand_ping_ack)
    val onlineTitle = stringResource(Res.string.on_demand_nodes_online)
    val errorsTitle = stringResource(Res.string.on_demand_routing_errors)
    val noErrors = stringResource(Res.string.on_demand_no_errors)
    val portsTitle = stringResource(Res.string.on_demand_port_counters)
    val airTitle = stringResource(Res.string.on_demand_air_activity)
    val exchangeTitle = stringResource(Res.string.on_demand_exchange_log)
    val avgTitle = stringResource(Res.string.on_demand_rx_avg_time)
    val historyTitle = stringResource(Res.string.on_demand_rx_packet_history)
    val fwTitle = stringResource(Res.string.on_demand_fw_plus_version)
    val roundTrip = stringResource(Res.string.on_demand_round_trip)
    val snr = stringResource(Res.string.on_demand_snr)
    val rssi = stringResource(Res.string.on_demand_rssi)
    val hops = stringResource(Res.string.on_demand_hops)

    val statsLines =
        uiState.nodeStats
            ?.let { stats -> nodeStatsRows(stats).map { ReportLine(stringResource(it.first), it.second) } }
            .orEmpty()
    val ports =
        uiState.portCounterHistory.sortedByDescending { it.count }.map { entry ->
            ReportBar("${portInfo(entry.port).label} (${entry.port})", entry.count.toFloat(), "${entry.count}")
        }
    val exchanges =
        uiState.exchangeLog.map { entry ->
            val from = NodeAddress.numToDefaultId(entry.from_node)
            val to = NodeAddress.numToDefaultId(entry.to_node)
            ReportLine("$from -> $to", "${portInfo(entry.port_num).label} (${entry.port_num})")
        }
    val receivedLabel = stringResource(Res.string.on_demand_received_label)
    val received = uiState.receivedAt.mapValues { DateFormatter.formatDateTime(it.value) }

    return remember(uiState, title, statsLines, ports, exchanges) {
        fun stamp(type: OnDemandType): List<ReportLine> =
            received[type]?.let { listOf(ReportLine(receivedLabel, it)) }.orEmpty()

        fun pingLines(ping: Ping?, roundTripMs: Long?): List<ReportLine> = buildList {
            if (ping == null) return@buildList
            roundTripMs?.let { add(ReportLine(roundTrip, "$it ms")) }
            ping.snr?.let { add(ReportLine(snr, "$it")) }
            ping.rx_rssi?.let { add(ReportLine(rssi, "$it")) }
            if (ping.snr == null && ping.rx_rssi == null) ping.hop_count?.let { add(ReportLine(hops, "$it")) }
        }

        val sections = buildList {
            if (statsLines.isNotEmpty()) {
                add(ReportSection(statsTitle, lines = stamp(OnDemandType.RESPONSE_NODE_STATS) + statsLines))
            }
            pingLines(uiState.pingResult, uiState.pingRoundTripMs).takeIf { it.isNotEmpty() }?.let {
                add(ReportSection(pingTitle, lines = stamp(OnDemandType.RESPONSE_PING) + it))
            }
            pingLines(uiState.pingAckResult, uiState.pingAckRoundTripMs).takeIf { it.isNotEmpty() }?.let {
                add(ReportSection(pingAckTitle, lines = stamp(OnDemandType.RESPONSE_PING_ACK) + it))
            }
            if (uiState.nodesOnline.isNotEmpty()) {
                add(
                    ReportSection(
                        onlineTitle,
                        lines =
                        stamp(OnDemandType.RESPONSE_NODES_ONLINE) +
                            uiState.nodesOnline.map {
                                ReportLine(
                                    it.long_name.ifBlank { it.short_name },
                                    "SNR ${it.snr}, $hops ${it.hops}, ${it.last_heard} s",
                                )
                            },
                    ),
                )
            }
            if (OnDemandType.RESPONSE_ROUTING_ERRORS in uiState.receivedAt) {
                val errors = uiState.routingErrors.filter { it.counter > 0 }
                add(
                    ReportSection(
                        errorsTitle,
                        lines =
                        stamp(OnDemandType.RESPONSE_ROUTING_ERRORS) +
                            if (errors.isEmpty()) listOf(ReportLine(noErrors, "0")) else emptyList(),
                        bars =
                        errors.sortedByDescending { it.counter }.map {
                            ReportBar("#${it.num}", it.counter.toFloat(), "${it.counter}")
                        },
                    ),
                )
            }
            if (ports.isNotEmpty()) {
                add(ReportSection(portsTitle, lines = stamp(OnDemandType.RESPONSE_PORT_COUNTER_HISTORY), bars = ports))
            }
            if (uiState.airActivityHistory.isNotEmpty()) {
                add(
                    ReportSection(
                        airTitle,
                        lines =
                        stamp(OnDemandType.RESPONSE_AIR_ACTIVITY_HISTORY) +
                            uiState.airActivityHistory.mapIndexed { i, e ->
                                ReportLine("#$i", "TX ${e.tx_time} ms, RX ${e.rx_time} ms, RX! ${e.rxBad_time} ms")
                            },
                    ),
                )
            }
            if (exchanges.isNotEmpty()) {
                add(
                    ReportSection(
                        exchangeTitle,
                        lines = stamp(OnDemandType.RESPONSE_PACKET_EXCHANGE_HISTORY) + exchanges,
                    ),
                )
            }
            windowBars(uiState.rxAvgTimeHistory).takeIf { it.isNotEmpty() }?.let {
                add(ReportSection(avgTitle, lines = stamp(OnDemandType.RESPONSE_RX_AVG_TIME), bars = it))
            }
            windowBars(uiState.rxPacketHistory).takeIf { it.isNotEmpty() }?.let {
                add(ReportSection(historyTitle, lines = stamp(OnDemandType.RESPONSE_PACKET_RX_HISTORY), bars = it))
            }
            uiState.fwPlusVersion?.let {
                add(
                    ReportSection(
                        fwTitle,
                        lines =
                        stamp(OnDemandType.RESPONSE_FW_PLUS_VERSION) + ReportLine(fwTitle, "v${it.version_number}"),
                    ),
                )
            }
        }
        NetworkReport(title = title, generated = "", sections = sections)
    }
}

/** Ten-minute windows, oldest first; the newest is "teraz" (now). */
private fun windowBars(values: List<Int>): List<ReportBar> = values.mapIndexed { i, value ->
    val label = if (i == values.lastIndex) "teraz" else "-${(values.size - 1 - i) * WINDOW_STEP_MINUTES} min"
    ReportBar(label, value.toFloat(), "$value")
}
