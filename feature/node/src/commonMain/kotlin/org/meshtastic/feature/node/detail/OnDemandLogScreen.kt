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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.formatByteSize
import org.meshtastic.core.common.util.formatString
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.formatDuration
import org.meshtastic.core.resources.on_demand_air_activity
import org.meshtastic.core.resources.on_demand_air_activity_desc
import org.meshtastic.core.resources.on_demand_air_util_tx
import org.meshtastic.core.resources.on_demand_ask
import org.meshtastic.core.resources.on_demand_battery_level
import org.meshtastic.core.resources.on_demand_blocked_hoplimit
import org.meshtastic.core.resources.on_demand_channel_utilization
import org.meshtastic.core.resources.on_demand_cpu_usage
import org.meshtastic.core.resources.on_demand_exchange_log
import org.meshtastic.core.resources.on_demand_exchange_log_desc
import org.meshtastic.core.resources.on_demand_export
import org.meshtastic.core.resources.on_demand_export_csv
import org.meshtastic.core.resources.on_demand_export_html
import org.meshtastic.core.resources.on_demand_export_markdown
import org.meshtastic.core.resources.on_demand_export_pdf
import org.meshtastic.core.resources.on_demand_export_text
import org.meshtastic.core.resources.on_demand_firmware_version
import org.meshtastic.core.resources.on_demand_flash
import org.meshtastic.core.resources.on_demand_flood_counter
import org.meshtastic.core.resources.on_demand_fw_plus_version
import org.meshtastic.core.resources.on_demand_fw_plus_version_desc
import org.meshtastic.core.resources.on_demand_heap_free
import org.meshtastic.core.resources.on_demand_heap_total
import org.meshtastic.core.resources.on_demand_hero_hint
import org.meshtastic.core.resources.on_demand_hero_progress
import org.meshtastic.core.resources.on_demand_hops
import org.meshtastic.core.resources.on_demand_log_title
import org.meshtastic.core.resources.on_demand_nexthop_counter
import org.meshtastic.core.resources.on_demand_no_answer
import org.meshtastic.core.resources.on_demand_no_errors
import org.meshtastic.core.resources.on_demand_node_stats
import org.meshtastic.core.resources.on_demand_node_stats_desc
import org.meshtastic.core.resources.on_demand_nodes_online
import org.meshtastic.core.resources.on_demand_nodes_online_desc
import org.meshtastic.core.resources.on_demand_packets_rx
import org.meshtastic.core.resources.on_demand_packets_rx_bad
import org.meshtastic.core.resources.on_demand_packets_tx
import org.meshtastic.core.resources.on_demand_ping
import org.meshtastic.core.resources.on_demand_ping_ack
import org.meshtastic.core.resources.on_demand_ping_desc
import org.meshtastic.core.resources.on_demand_port_counters
import org.meshtastic.core.resources.on_demand_port_counters_desc
import org.meshtastic.core.resources.on_demand_psram
import org.meshtastic.core.resources.on_demand_reboots
import org.meshtastic.core.resources.on_demand_received_at
import org.meshtastic.core.resources.on_demand_round_trip
import org.meshtastic.core.resources.on_demand_routing_errors
import org.meshtastic.core.resources.on_demand_routing_errors_desc
import org.meshtastic.core.resources.on_demand_rssi
import org.meshtastic.core.resources.on_demand_rx_avg_time
import org.meshtastic.core.resources.on_demand_rx_avg_time_desc
import org.meshtastic.core.resources.on_demand_rx_packet_history
import org.meshtastic.core.resources.on_demand_rx_packet_history_desc
import org.meshtastic.core.resources.on_demand_share
import org.meshtastic.core.resources.on_demand_snr
import org.meshtastic.core.resources.on_demand_stat_nodes_online
import org.meshtastic.core.resources.on_demand_stat_nodes_total
import org.meshtastic.core.resources.on_demand_uptime
import org.meshtastic.core.resources.port_desc_admin
import org.meshtastic.core.resources.port_desc_alert
import org.meshtastic.core.resources.port_desc_detection_sensor
import org.meshtastic.core.resources.port_desc_map_report
import org.meshtastic.core.resources.port_desc_mesh_beacon
import org.meshtastic.core.resources.port_desc_neighborinfo
import org.meshtastic.core.resources.port_desc_node_status
import org.meshtastic.core.resources.port_desc_nodeinfo
import org.meshtastic.core.resources.port_desc_on_demand
import org.meshtastic.core.resources.port_desc_paxcounter
import org.meshtastic.core.resources.port_desc_position
import org.meshtastic.core.resources.port_desc_range_test
import org.meshtastic.core.resources.port_desc_remote_hardware
import org.meshtastic.core.resources.port_desc_routing
import org.meshtastic.core.resources.port_desc_serial
import org.meshtastic.core.resources.port_desc_simulator
import org.meshtastic.core.resources.port_desc_store_forward
import org.meshtastic.core.resources.port_desc_store_forward_plus
import org.meshtastic.core.resources.port_desc_telemetry
import org.meshtastic.core.resources.port_desc_text_message
import org.meshtastic.core.resources.port_desc_text_message_compressed
import org.meshtastic.core.resources.port_desc_traceroute
import org.meshtastic.core.resources.port_desc_unknown
import org.meshtastic.core.resources.port_desc_waypoint
import org.meshtastic.core.resources.port_desc_zps
import org.meshtastic.core.resources.port_name_admin
import org.meshtastic.core.resources.port_name_alert
import org.meshtastic.core.resources.port_name_detection_sensor
import org.meshtastic.core.resources.port_name_map_report
import org.meshtastic.core.resources.port_name_mesh_beacon
import org.meshtastic.core.resources.port_name_neighborinfo
import org.meshtastic.core.resources.port_name_node_status
import org.meshtastic.core.resources.port_name_nodeinfo
import org.meshtastic.core.resources.port_name_on_demand
import org.meshtastic.core.resources.port_name_paxcounter
import org.meshtastic.core.resources.port_name_position
import org.meshtastic.core.resources.port_name_range_test
import org.meshtastic.core.resources.port_name_remote_hardware
import org.meshtastic.core.resources.port_name_routing
import org.meshtastic.core.resources.port_name_serial
import org.meshtastic.core.resources.port_name_simulator
import org.meshtastic.core.resources.port_name_store_forward
import org.meshtastic.core.resources.port_name_store_forward_plus
import org.meshtastic.core.resources.port_name_telemetry
import org.meshtastic.core.resources.port_name_text_message
import org.meshtastic.core.resources.port_name_text_message_compressed
import org.meshtastic.core.resources.port_name_traceroute
import org.meshtastic.core.resources.port_name_unknown
import org.meshtastic.core.resources.port_name_waypoint
import org.meshtastic.core.resources.port_name_zps
import org.meshtastic.core.resources.wifi_drop_reason_label
import org.meshtastic.core.resources.wifi_drops_label
import org.meshtastic.core.resources.wifi_rssi_signal
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.AirUtilization
import org.meshtastic.core.ui.icon.ChannelUtilization
import org.meshtastic.core.ui.icon.Chart
import org.meshtastic.core.ui.icon.CheckCircle
import org.meshtastic.core.ui.icon.ChevronRight
import org.meshtastic.core.ui.icon.DataArray
import org.meshtastic.core.ui.icon.Download
import org.meshtastic.core.ui.icon.ErrorOutline
import org.meshtastic.core.ui.icon.History
import org.meshtastic.core.ui.icon.HopCount
import org.meshtastic.core.ui.icon.Memory
import org.meshtastic.core.ui.icon.MeshHub
import org.meshtastic.core.ui.icon.MeshRadio
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.NearMe
import org.meshtastic.core.ui.icon.Nodes
import org.meshtastic.core.ui.icon.Reconnecting
import org.meshtastic.core.ui.icon.Rssi
import org.meshtastic.core.ui.icon.Send
import org.meshtastic.core.ui.icon.Share
import org.meshtastic.core.ui.icon.Snr
import org.meshtastic.core.ui.icon.Voltage
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline
import org.meshtastic.core.ui.util.rememberFileExporter
import org.meshtastic.core.ui.util.rememberShareText
import org.meshtastic.feature.node.health.BarList
import org.meshtastic.feature.node.health.ColumnChart
import org.meshtastic.feature.node.health.DistributionBar
import org.meshtastic.feature.node.health.DonutChart
import org.meshtastic.feature.node.health.GaugeRow
import org.meshtastic.feature.node.health.HealthDot
import org.meshtastic.feature.node.health.HealthLevel
import org.meshtastic.feature.node.health.ReportBar
import org.meshtastic.feature.node.health.StatTile
import org.meshtastic.feature.node.health.color
import org.meshtastic.feature.node.health.etherLevel
import org.meshtastic.feature.node.health.generatedStamp
import org.meshtastic.feature.node.health.snrLevel
import org.meshtastic.feature.node.health.toCsv
import org.meshtastic.feature.node.health.toHtml
import org.meshtastic.feature.node.health.toMarkdown
import org.meshtastic.feature.node.health.toPdf
import org.meshtastic.feature.node.health.toPlainText
import org.meshtastic.proto.NodeStats
import org.meshtastic.proto.OnDemandType
import org.meshtastic.proto.Ping
import org.meshtastic.proto.PortNum

/** OnDemand diagnostics screen: an overview of what it can do, then one card per query with its own request button. */
@Composable
fun OnDemandLogScreen(viewModel: OnDemandLogViewModel, onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gold = MaterialTheme.colorScheme.StatusOnline
    val red = HealthLevel.MEDIUM.color()
    val purple = HealthLevel.BAD.color()
    val report = rememberOnDemandReport(uiState)
    val hasAnswers = report.sections.isNotEmpty()
    val share = rememberShareText()
    var menuOpen by remember { mutableStateOf(false) }
    val exportPdf = rememberFileExporter(content = { report.copy(generated = generatedStamp()).toPdf() })
    val exportHtml =
        rememberFileExporter(content = { report.copy(generated = generatedStamp()).toHtml().encodeToByteArray() })
    val exportMarkdown =
        rememberFileExporter(content = { report.copy(generated = generatedStamp()).toMarkdown().encodeToByteArray() })
    val exportText =
        rememberFileExporter(content = { report.copy(generated = generatedStamp()).toPlainText().encodeToByteArray() })
    val exportCsv =
        rememberFileExporter(content = { report.copy(generated = generatedStamp()).toCsv().encodeToByteArray() })
    val iconShare = MeshtasticIcons.Share
    val iconDownload = MeshtasticIcons.Download

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.on_demand_log_title),
                subtitle = null,
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                actions = {
                    IconButton(
                        enabled = hasAnswers,
                        onClick = {
                            val stamped = report.copy(generated = generatedStamp())
                            share(stamped.toPlainText(), stamped.title)
                        },
                    ) {
                        Icon(imageVector = iconShare, contentDescription = stringResource(Res.string.on_demand_share))
                    }
                    IconButton(enabled = hasAnswers, onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = iconDownload,
                            contentDescription = stringResource(Res.string.on_demand_export),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.on_demand_export_pdf)) },
                            onClick = {
                                menuOpen = false
                                exportPdf("diagnostyka_na_zadanie.pdf", MIME_PDF)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.on_demand_export_html)) },
                            onClick = {
                                menuOpen = false
                                exportHtml("diagnostyka_na_zadanie.html", MIME_HTML)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.on_demand_export_markdown)) },
                            onClick = {
                                menuOpen = false
                                exportMarkdown("diagnostyka_na_zadanie.md", MIME_MARKDOWN)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.on_demand_export_text)) },
                            onClick = {
                                menuOpen = false
                                exportText("diagnostyka_na_zadanie.txt", MIME_TEXT)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.on_demand_export_csv)) },
                            onClick = {
                                menuOpen = false
                                exportCsv("diagnostyka_na_zadanie.csv", MIME_CSV)
                            },
                        )
                    }
                },
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
            PaddingValues(
                top = paddingValues.calculateTopPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "hero") { OnDemandHero(uiState.receivedAt.keys) }

            item(key = "node-stats") {
                QueryCard(
                    title = stringResource(Res.string.on_demand_node_stats),
                    description = stringResource(Res.string.on_demand_node_stats_desc),
                    icon = MeshtasticIcons.Memory,
                    accent = gold,
                    onRequest = viewModel::requestNodeStats,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_NODE_STATS],
                ) {
                    uiState.nodeStats?.let { NodeStatsVisual(it) }
                }
            }

            item(key = "ping") { PingCard(uiState, viewModel::requestPing, viewModel::requestPingAck) }

            item(key = "nodes-online") {
                QueryCard(
                    title = stringResource(Res.string.on_demand_nodes_online),
                    description = stringResource(Res.string.on_demand_nodes_online_desc),
                    icon = MeshtasticIcons.Nodes,
                    accent = gold,
                    onRequest = viewModel::requestNodesOnline,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_NODES_ONLINE],
                ) {
                    if (uiState.nodesOnline.isNotEmpty()) NodesOnlineVisual(uiState.nodesOnline)
                }
            }

            item(key = "routing-errors") {
                QueryCard(
                    title = stringResource(Res.string.on_demand_routing_errors),
                    description = stringResource(Res.string.on_demand_routing_errors_desc),
                    icon = MeshtasticIcons.ErrorOutline,
                    accent = purple,
                    onRequest = viewModel::requestRoutingErrors,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_ROUTING_ERRORS],
                ) {
                    if (OnDemandType.RESPONSE_ROUTING_ERRORS in uiState.receivedAt) {
                        val errors = uiState.routingErrors.filter { it.counter > 0 }
                        if (errors.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(MeshtasticIcons.CheckCircle, contentDescription = null, tint = gold)
                                Text(stringResource(Res.string.on_demand_no_errors))
                            }
                        } else {
                            BarList(
                                bars =
                                errors.sortedByDescending { it.counter }.map {
                                    ReportBar("#${it.num}", it.counter.toFloat(), "${it.counter}")
                                },
                                color = purple,
                            )
                        }
                    }
                }
            }

            item(key = "port-counters") {
                QueryCard(
                    title = stringResource(Res.string.on_demand_port_counters),
                    description = stringResource(Res.string.on_demand_port_counters_desc),
                    icon = MeshtasticIcons.DataArray,
                    accent = gold,
                    onRequest = viewModel::requestPortCounterHistory,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_PORT_COUNTER_HISTORY],
                ) {
                    val bars =
                        uiState.portCounterHistory
                            .sortedByDescending { it.count }
                            .take(PORT_BARS)
                            .map { entry ->
                                val info = portInfo(entry.port)
                                ReportBar("${info.label} (${entry.port})", entry.count.toFloat(), "${entry.count}")
                            }
                    if (bars.isNotEmpty()) BarList(bars = bars, color = gold)
                }
            }

            item(key = "air-activity") {
                QueryCard(
                    title = stringResource(Res.string.on_demand_air_activity),
                    description = stringResource(Res.string.on_demand_air_activity_desc),
                    icon = MeshtasticIcons.AirUtilization,
                    accent = red,
                    onRequest = viewModel::requestAirActivityHistory,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_AIR_ACTIVITY_HISTORY],
                ) {
                    if (uiState.airActivityHistory.isNotEmpty()) AirActivityChart(uiState.airActivityHistory)
                }
            }

            item(key = "exchange-log") {
                QueryCard(
                    title = stringResource(Res.string.on_demand_exchange_log),
                    description = stringResource(Res.string.on_demand_exchange_log_desc),
                    icon = MeshtasticIcons.MeshHub,
                    accent = gold,
                    onRequest = viewModel::requestExchangeLog,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_PACKET_EXCHANGE_HISTORY],
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.exchangeLog.forEach { entry ->
                            val from = NodeAddress.numToDefaultId(entry.from_node)
                            val to = NodeAddress.numToDefaultId(entry.to_node)
                            val info = portInfo(entry.port_num)
                            ExchangeRow(from = from, to = to, port = "${info.label} (${entry.port_num})")
                        }
                    }
                }
            }

            item(key = "rx-avg-time") {
                QueryCard(
                    title = stringResource(Res.string.on_demand_rx_avg_time),
                    description = stringResource(Res.string.on_demand_rx_avg_time_desc),
                    icon = MeshtasticIcons.History,
                    accent = red,
                    onRequest = viewModel::requestRxAvgTimeHistory,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_RX_AVG_TIME],
                ) {
                    WindowColumns(values = uiState.rxAvgTimeHistory, color = red)
                }
            }

            item(key = "rx-packet-history") {
                QueryCard(
                    title = stringResource(Res.string.on_demand_rx_packet_history),
                    description = stringResource(Res.string.on_demand_rx_packet_history_desc),
                    icon = MeshtasticIcons.Chart,
                    accent = gold,
                    onRequest = viewModel::requestRxPacketHistory,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_PACKET_RX_HISTORY],
                ) {
                    WindowColumns(values = uiState.rxPacketHistory, color = gold)
                }
            }

            item(key = "fw-plus-version") {
                QueryCard(
                    title = stringResource(Res.string.on_demand_fw_plus_version),
                    description = stringResource(Res.string.on_demand_fw_plus_version_desc),
                    icon = MeshtasticIcons.MeshRadio,
                    accent = purple,
                    onRequest = viewModel::requestFwPlusVersion,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_FW_PLUS_VERSION],
                ) {
                    uiState.fwPlusVersion?.let { version ->
                        Text(
                            text = "v${version.version_number}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = purple,
                        )
                    }
                }
            }
        }
    }
}

private const val MIME_PDF = "application/pdf"
private const val MIME_HTML = "text/html"
private const val MIME_MARKDOWN = "text/markdown"
private const val MIME_TEXT = "text/plain"
private const val MIME_CSV = "text/csv"
private const val PORT_BARS = 8
private const val TINT_ALPHA = 0.14f
private const val WINDOW_MINUTES = 10
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600

/** What the screen can do, each capability marked once it has been answered. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnDemandHero(answered: Set<OnDemandType>) {
    val gold = MaterialTheme.colorScheme.StatusOnline
    val capabilities =
        listOf(
            Triple(Res.string.on_demand_node_stats, MeshtasticIcons.Memory, OnDemandType.RESPONSE_NODE_STATS),
            Triple(Res.string.on_demand_ping, MeshtasticIcons.NearMe, OnDemandType.RESPONSE_PING),
            Triple(Res.string.on_demand_ping_ack, MeshtasticIcons.NearMe, OnDemandType.RESPONSE_PING_ACK),
            Triple(Res.string.on_demand_nodes_online, MeshtasticIcons.Nodes, OnDemandType.RESPONSE_NODES_ONLINE),
            Triple(
                Res.string.on_demand_routing_errors,
                MeshtasticIcons.ErrorOutline,
                OnDemandType.RESPONSE_ROUTING_ERRORS,
            ),
            Triple(
                Res.string.on_demand_port_counters,
                MeshtasticIcons.DataArray,
                OnDemandType.RESPONSE_PORT_COUNTER_HISTORY,
            ),
            Triple(
                Res.string.on_demand_air_activity,
                MeshtasticIcons.AirUtilization,
                OnDemandType.RESPONSE_AIR_ACTIVITY_HISTORY,
            ),
            Triple(
                Res.string.on_demand_exchange_log,
                MeshtasticIcons.MeshHub,
                OnDemandType.RESPONSE_PACKET_EXCHANGE_HISTORY,
            ),
            Triple(Res.string.on_demand_rx_avg_time, MeshtasticIcons.History, OnDemandType.RESPONSE_RX_AVG_TIME),
            Triple(
                Res.string.on_demand_rx_packet_history,
                MeshtasticIcons.Chart,
                OnDemandType.RESPONSE_PACKET_RX_HISTORY,
            ),
            Triple(
                Res.string.on_demand_fw_plus_version,
                MeshtasticIcons.MeshRadio,
                OnDemandType.RESPONSE_FW_PLUS_VERSION,
            ),
        )
    val done = capabilities.count { it.third in answered }
    val brush =
        Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), MaterialTheme.colorScheme.surfaceContainer),
        )
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(brush).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DonutChart(
                    segments =
                    listOf(
                        gold to done,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) to (capabilities.size - done),
                    ),
                    centerText = "$done/${capabilities.size}",
                    sizeDp = HERO_DONUT_DP,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.on_demand_log_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.on_demand_hero_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(Res.string.on_demand_hero_progress, done, capabilities.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = gold,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                capabilities.forEach { (titleRes, icon, type) ->
                    val isDone = type in answered
                    val tint = if (isDone) gold else MaterialTheme.colorScheme.onSurfaceVariant
                    Row(
                        modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(tint.copy(alpha = TINT_ALPHA))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(titleRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isDone) MaterialTheme.colorScheme.onSurface else tint,
                        )
                    }
                }
            }
        }
    }
}

private const val HERO_DONUT_DP = 96

/** A card for one query: coloured icon, title, what it does, a "Zapytaj" button, the time of the last answer. */
@Composable
private fun QueryCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    onRequest: () -> Unit,
    receivedAtMillis: Long?,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(icon = icon, accent = accent)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onRequest) {
                    Icon(MeshtasticIcons.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.on_demand_ask), modifier = Modifier.padding(start = 8.dp))
                }
                AnswerStamp(receivedAtMillis)
            }
            content()
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = TINT_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
    }
}

/** A dot and "Odebrano: ..." once an answer exists, or "Brak odpowiedzi" before that. */
@Composable
private fun AnswerStamp(receivedAtMillis: Long?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HealthDot(
            color =
            if (receivedAtMillis != null) {
                MaterialTheme.colorScheme.StatusOnline
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
        )
        Text(
            text =
            receivedAtMillis?.let { stringResource(Res.string.on_demand_received_at, DateFormatter.formatDateTime(it)) }
                ?: stringResource(Res.string.on_demand_no_answer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val USAGE_GOOD = 70f
private const val USAGE_MEDIUM = 85f
private const val USAGE_BAD = 95f
private const val PERCENT = 100f

private fun batteryLevelOf(percent: Int): HealthLevel = when {
    percent >= 50 -> HealthLevel.GOOD
    percent >= 20 -> HealthLevel.MEDIUM
    percent >= 5 -> HealthLevel.BAD
    else -> HealthLevel.CRITICAL
}

/** How full something is (memory, flash, CPU): the fuller, the worse. */
private fun usageLevelOf(percent: Float): HealthLevel = when {
    percent < USAGE_GOOD -> HealthLevel.GOOD
    percent < USAGE_MEDIUM -> HealthLevel.MEDIUM
    percent < USAGE_BAD -> HealthLevel.BAD
    else -> HealthLevel.CRITICAL
}

/** The node's own statistics as tiles, gauges and a packets donut instead of a plain list. */
@Composable
private fun NodeStatsVisual(stats: NodeStats) {
    val gold = MaterialTheme.colorScheme.StatusOnline
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            stats.battery_level?.let { level ->
                StatTile(
                    label = stringResource(Res.string.on_demand_battery_level),
                    value = "$level%",
                    icon = MeshtasticIcons.Voltage,
                    color = batteryLevelOf(level).color(),
                    modifier = Modifier.weight(1f),
                )
            }
            stats.uptime_seconds?.let { seconds ->
                StatTile(
                    label = stringResource(Res.string.on_demand_uptime),
                    value = formatDuration(seconds.toLong()),
                    icon = MeshtasticIcons.History,
                    color = gold,
                    modifier = Modifier.weight(1f),
                )
            }
            stats.reboots?.let { reboots ->
                StatTile(
                    label = stringResource(Res.string.on_demand_reboots),
                    value = "$reboots",
                    icon = MeshtasticIcons.Reconnecting,
                    color = gold,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        stats.channel_utilization?.let {
            GaugeRow(
                label = stringResource(Res.string.on_demand_channel_utilization),
                percentText = "${formatString("%.2f", it)}%",
                fraction = it / PERCENT,
                color = etherLevel(it).color(),
            )
        }
        stats.air_util_tx?.let {
            GaugeRow(
                label = stringResource(Res.string.on_demand_air_util_tx),
                percentText = "${formatString("%.2f", it)}%",
                fraction = it / PERCENT,
                color = etherLevel(it).color(),
            )
        }
        stats.cpu_usage_percent?.let {
            GaugeRow(
                label = stringResource(Res.string.on_demand_cpu_usage),
                percentText = "$it%",
                fraction = it / PERCENT,
                color = usageLevelOf(it.toFloat()).color(),
            )
        }
        UsageGauge(
            label = stringResource(Res.string.on_demand_heap_free),
            free = stats.memory_free_cheap,
            total = stats.memory_total,
        )
        val flashUsed = stats.flash_used_bytes
        val flashTotal = stats.flash_total_bytes
        if (flashUsed != null && flashTotal != null && flashTotal > 0) {
            UsageGauge(
                label = stringResource(Res.string.on_demand_flash),
                free = flashTotal - flashUsed,
                total = flashTotal,
            )
        }
        UsageGauge(
            label = stringResource(Res.string.on_demand_psram),
            free = stats.memory_psram_free,
            total = stats.memory_psram_total,
        )

        val rx = stats.num_packets_rx
        if (rx != null) {
            val bad = stats.num_packets_rx_bad ?: 0
            val dupes = stats.num_rx_dupe ?: 0
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DonutChart(
                    segments =
                    listOf(
                        gold to (rx - bad - dupes).coerceAtLeast(0),
                        HealthLevel.MEDIUM.color() to dupes,
                        HealthLevel.BAD.color() to bad,
                    ),
                    centerText = "$rx",
                    sizeDp = PACKETS_DONUT_DP,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(Res.string.on_demand_packets_rx), fontWeight = FontWeight.Bold)
                    LegendLine(gold, "${(rx - bad - dupes).coerceAtLeast(0)}")
                    LegendLine(HealthLevel.MEDIUM.color(), "$dupes dup.")
                    LegendLine(HealthLevel.BAD.color(), "$bad ${stringResource(Res.string.on_demand_packets_rx_bad)}")
                    stats.num_packets_tx?.let {
                        Text(
                            text = "${stringResource(Res.string.on_demand_packets_tx)}: $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // The small counters that do not need a picture.
        val extra =
            nodeStatsRows(stats).filter {
                it.first in
                    setOf(
                        Res.string.on_demand_stat_nodes_online,
                        Res.string.on_demand_stat_nodes_total,
                        Res.string.on_demand_flood_counter,
                        Res.string.on_demand_nexthop_counter,
                        Res.string.on_demand_blocked_hoplimit,
                        Res.string.wifi_rssi_signal,
                        Res.string.wifi_drops_label,
                        Res.string.wifi_drop_reason_label,
                        Res.string.on_demand_firmware_version,
                    )
            }
        StatRows(extra)
    }
}

private const val PACKETS_DONUT_DP = 96

@Composable
private fun LegendLine(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HealthDot(color = color)
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

/** A "how full" gauge from free and total bytes; nothing when either is unknown. */
@Composable
private fun UsageGauge(label: String, free: Int?, total: Int?) {
    if (free == null || total == null || total <= 0) return
    val usedPercent = (total - free).coerceAtLeast(0) * PERCENT / total
    GaugeRow(
        label = label,
        percentText = "${formatByteSize((total - free).toLong().coerceAtLeast(0))} / ${formatByteSize(total.toLong())}",
        fraction = usedPercent / PERCENT,
        color = usageLevelOf(usedPercent).color(),
    )
}

/** Ping and acknowledged ping side by side; the result shows signal strength coloured like the rest of the app. */
@Composable
private fun PingCard(uiState: OnDemandLogUiState, onPing: () -> Unit, onPingAck: () -> Unit) {
    val gold = MaterialTheme.colorScheme.StatusOnline
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(icon = MeshtasticIcons.NearMe, accent = gold)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(Res.string.on_demand_ping),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.on_demand_ping_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PingColumn(
                    label = stringResource(Res.string.on_demand_ping),
                    onClick = onPing,
                    ping = uiState.pingResult,
                    roundTripMs = uiState.pingRoundTripMs,
                    receivedAt = uiState.receivedAt[OnDemandType.RESPONSE_PING],
                    modifier = Modifier.weight(1f),
                )
                PingColumn(
                    label = stringResource(Res.string.on_demand_ping_ack),
                    onClick = onPingAck,
                    ping = uiState.pingAckResult,
                    roundTripMs = uiState.pingAckRoundTripMs,
                    receivedAt = uiState.receivedAt[OnDemandType.RESPONSE_PING_ACK],
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PingColumn(
    label: String,
    onClick: () -> Unit,
    ping: Ping?,
    roundTripMs: Long?,
    receivedAt: Long?,
    modifier: Modifier = Modifier,
) {
    val gold = MaterialTheme.colorScheme.StatusOnline
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(MeshtasticIcons.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
        }
        if (ping != null) {
            roundTripMs?.let {
                StatTile(
                    label = stringResource(Res.string.on_demand_round_trip),
                    value = "$it ms",
                    icon = MeshtasticIcons.History,
                    color = gold,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ping.snr?.let {
                StatTile(
                    label = stringResource(Res.string.on_demand_snr),
                    value = "$it",
                    icon = MeshtasticIcons.Snr,
                    color = snrLevel(it).color(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ping.rx_rssi?.let {
                StatTile(
                    label = stringResource(Res.string.on_demand_rssi),
                    value = "$it",
                    icon = MeshtasticIcons.Rssi,
                    color = gold,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (ping.snr == null && ping.rx_rssi == null) {
                ping.hop_count?.let {
                    StatTile(
                        label = stringResource(Res.string.on_demand_hops),
                        value = "$it",
                        icon = MeshtasticIcons.HopCount,
                        color = gold,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        AnswerStamp(receivedAt)
    }
}

/** Nodes seen by the asked node: a coloured badge per node (by SNR), plus the overall spread of signal quality. */
@Composable
private fun NodesOnlineVisual(entries: List<org.meshtastic.proto.NodeEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DistributionBar(
            counts =
            listOf(
                entries.count { snrLevel(it.snr) == HealthLevel.GOOD },
                entries.count { snrLevel(it.snr) == HealthLevel.MEDIUM },
                entries.count { snrLevel(it.snr) == HealthLevel.BAD },
                entries.count { snrLevel(it.snr) == HealthLevel.CRITICAL },
            ),
        )
        entries.forEach { entry ->
            val level = snrLevel(entry.snr)
            val tint = level.color()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = TINT_ALPHA))
                        .border(2.dp, tint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = entry.short_name.ifBlank { "?" }.take(NAME_BADGE_CHARS),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.long_name.ifBlank { entry.short_name },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            MeshtasticIcons.HopCount,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${entry.hops} • ${ageText(entry.last_heard)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = "SNR ${entry.snr}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                )
            }
        }
    }
}

private const val NAME_BADGE_CHARS = 4

private fun ageText(seconds: Int): String = when {
    seconds < SECONDS_PER_MINUTE -> "$seconds s"
    seconds < SECONDS_PER_HOUR -> "${seconds / SECONDS_PER_MINUTE} min"
    else -> "${seconds / SECONDS_PER_HOUR} h"
}

/** One sender → receiver pair, with the kind of packet shown as a small tag. */
@Composable
private fun ExchangeRow(from: String, to: String, port: String) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(from, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Icon(
                MeshtasticIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.StatusOnline,
                modifier = Modifier.size(18.dp),
            )
            Text(to, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Text(port, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Ten-minute windows from oldest to newest as columns; only the ends are labelled. */
@Composable
private fun WindowColumns(values: List<Int>, color: Color) {
    if (values.isEmpty()) return
    val labels =
        values.indices.map { i ->
            when (i) {
                0 -> "-${(values.size - 1) * WINDOW_MINUTES} min"
                values.lastIndex -> "teraz"
                else -> ""
            }
        }
    ColumnChart(values = values, labels = labels, color = color)
}

/** Time on air per window, stacked: sending (gold), receiving (red) and bad receptions (purple). */
@Composable
private fun AirActivityChart(entries: List<org.meshtastic.proto.AirActivityEntry>) {
    val gold = MaterialTheme.colorScheme.StatusOnline
    val red = HealthLevel.MEDIUM.color()
    val purple = HealthLevel.BAD.color()
    val maxTotal = entries.maxOf { it.tx_time + it.rx_time + it.rxBad_time }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(AIR_CHART_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            entries.forEach { e ->
                val total = e.tx_time + e.rx_time + e.rxBad_time
                Column(
                    modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(total / maxTotal.toFloat())
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)),
                ) {
                    listOf(e.rxBad_time to purple, e.rx_time to red, e.tx_time to gold).forEach { (part, tint) ->
                        if (part > 0) Box(modifier = Modifier.weight(part.toFloat()).fillMaxWidth().background(tint))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendLine(gold, "TX")
            LegendLine(red, "RX")
            LegendLine(purple, "RX!")
        }
    }
}

private const val AIR_CHART_DP = 110

@Composable
private fun StatRows(rows: List<Pair<StringResource, String>>) {
    rows.forEachIndexed { index, (labelRes, value) ->
        ListItem(
            text = stringResource(labelRes),
            leadingIcon = iconForLabel(labelRes),
            supportingText = value,
            trailingIcon = null,
        )
        if (index != rows.lastIndex) HorizontalDivider()
    }
}

/** Maps each stat row's label to a matching icon. Falls back to [MeshtasticIcons.Memory] for anything unmapped. */
@Composable
private fun iconForLabel(label: StringResource): ImageVector = when (label) {
    Res.string.on_demand_battery_level -> MeshtasticIcons.Voltage

    Res.string.on_demand_uptime -> MeshtasticIcons.History

    Res.string.on_demand_channel_utilization -> MeshtasticIcons.ChannelUtilization

    Res.string.on_demand_air_util_tx -> MeshtasticIcons.AirUtilization

    Res.string.on_demand_packets_tx,
    Res.string.on_demand_packets_rx,
    Res.string.on_demand_packets_rx_bad,
    -> MeshtasticIcons.DataArray

    Res.string.on_demand_stat_nodes_online,
    Res.string.on_demand_stat_nodes_total,
    -> MeshtasticIcons.Nodes

    Res.string.on_demand_reboots -> MeshtasticIcons.Reconnecting

    Res.string.on_demand_flood_counter,
    Res.string.on_demand_nexthop_counter,
    -> MeshtasticIcons.Chart

    Res.string.on_demand_blocked_hoplimit -> MeshtasticIcons.HopCount

    Res.string.on_demand_firmware_version -> MeshtasticIcons.MeshRadio

    Res.string.on_demand_rssi,
    Res.string.wifi_rssi_signal,
    -> MeshtasticIcons.Rssi

    Res.string.on_demand_snr -> MeshtasticIcons.Snr

    Res.string.on_demand_hops -> MeshtasticIcons.HopCount

    Res.string.on_demand_round_trip -> MeshtasticIcons.History

    else -> MeshtasticIcons.Memory
}

/** Localized name + resource IDs for a known portnum. */
private data class PortInfo(val labelRes: StringResource, val descRes: StringResource)

/** Localized name + description already resolved to display strings. */
internal data class ResolvedPortInfo(val label: String, val description: String)

private val KNOWN_PORTS =
    mapOf(
        1 to PortInfo(Res.string.port_name_text_message, Res.string.port_desc_text_message),
        2 to PortInfo(Res.string.port_name_remote_hardware, Res.string.port_desc_remote_hardware),
        3 to PortInfo(Res.string.port_name_position, Res.string.port_desc_position),
        4 to PortInfo(Res.string.port_name_nodeinfo, Res.string.port_desc_nodeinfo),
        5 to PortInfo(Res.string.port_name_routing, Res.string.port_desc_routing),
        6 to PortInfo(Res.string.port_name_admin, Res.string.port_desc_admin),
        7 to PortInfo(Res.string.port_name_text_message_compressed, Res.string.port_desc_text_message_compressed),
        8 to PortInfo(Res.string.port_name_waypoint, Res.string.port_desc_waypoint),
        10 to PortInfo(Res.string.port_name_detection_sensor, Res.string.port_desc_detection_sensor),
        11 to PortInfo(Res.string.port_name_alert, Res.string.port_desc_alert),
        34 to PortInfo(Res.string.port_name_paxcounter, Res.string.port_desc_paxcounter),
        35 to PortInfo(Res.string.port_name_store_forward_plus, Res.string.port_desc_store_forward_plus),
        36 to PortInfo(Res.string.port_name_node_status, Res.string.port_desc_node_status),
        37 to PortInfo(Res.string.port_name_mesh_beacon, Res.string.port_desc_mesh_beacon),
        64 to PortInfo(Res.string.port_name_serial, Res.string.port_desc_serial),
        65 to PortInfo(Res.string.port_name_store_forward, Res.string.port_desc_store_forward),
        66 to PortInfo(Res.string.port_name_range_test, Res.string.port_desc_range_test),
        67 to PortInfo(Res.string.port_name_telemetry, Res.string.port_desc_telemetry),
        68 to PortInfo(Res.string.port_name_zps, Res.string.port_desc_zps),
        69 to PortInfo(Res.string.port_name_simulator, Res.string.port_desc_simulator),
        70 to PortInfo(Res.string.port_name_traceroute, Res.string.port_desc_traceroute),
        71 to PortInfo(Res.string.port_name_neighborinfo, Res.string.port_desc_neighborinfo),
        73 to PortInfo(Res.string.port_name_map_report, Res.string.port_desc_map_report),
        354 to PortInfo(Res.string.port_name_on_demand, Res.string.port_desc_on_demand),
    )

@Composable
internal fun portInfo(port: Int): ResolvedPortInfo {
    val known = KNOWN_PORTS[port]
    if (known != null) return ResolvedPortInfo(stringResource(known.labelRes), stringResource(known.descRes))
    val fallbackLabel =
        PortNum.fromValue(port)?.name?.removeSuffix("_APP")?.split("_")?.joinToString(" ") {
            it.lowercase().replaceFirstChar(Char::titlecase)
        } ?: stringResource(Res.string.port_name_unknown)
    return ResolvedPortInfo(fallbackLabel, stringResource(Res.string.port_desc_unknown))
}

@Composable
internal fun nodeStatsRows(stats: NodeStats): List<Pair<StringResource, String>> = buildList {
    stats.battery_level?.let { add(Res.string.on_demand_battery_level to "$it%") }
    stats.uptime_seconds?.let { add(Res.string.on_demand_uptime to formatDuration(it.toLong())) }
    stats.channel_utilization?.let { add(Res.string.on_demand_channel_utilization to "${formatString("%.2f", it)}%") }
    stats.air_util_tx?.let { add(Res.string.on_demand_air_util_tx to "${formatString("%.2f", it)}%") }
    stats.num_packets_tx?.let { add(Res.string.on_demand_packets_tx to "$it") }
    stats.num_packets_rx?.let { add(Res.string.on_demand_packets_rx to "$it") }
    stats.num_packets_rx_bad?.let { add(Res.string.on_demand_packets_rx_bad to "$it") }
    stats.num_online_nodes?.let { add(Res.string.on_demand_stat_nodes_online to "$it") }
    stats.num_total_nodes?.let { add(Res.string.on_demand_stat_nodes_total to "$it") }
    stats.reboots?.let { add(Res.string.on_demand_reboots to "$it") }
    stats.memory_free_cheap?.let { add(Res.string.on_demand_heap_free to formatByteSize(it.toLong())) }
    stats.memory_total?.let { add(Res.string.on_demand_heap_total to formatByteSize(it.toLong())) }
    stats.cpu_usage_percent?.let { add(Res.string.on_demand_cpu_usage to "$it%") }
    if (stats.flash_used_bytes != null || stats.flash_total_bytes != null) {
        val used = stats.flash_used_bytes?.let { formatByteSize(it.toLong()) } ?: "?"
        val total = stats.flash_total_bytes?.let { formatByteSize(it.toLong()) } ?: "?"
        add(Res.string.on_demand_flash to "$used / $total")
    }
    if (stats.memory_psram_free != null || stats.memory_psram_total != null) {
        val free = stats.memory_psram_free?.let { formatByteSize(it.toLong()) } ?: "?"
        val total = stats.memory_psram_total?.let { formatByteSize(it.toLong()) } ?: "?"
        add(Res.string.on_demand_psram to "$free / $total")
    }
    stats.flood_counter?.let { add(Res.string.on_demand_flood_counter to "$it") }
    stats.nexthop_counter?.let { add(Res.string.on_demand_nexthop_counter to "$it") }
    stats.blocked_by_hoplimit?.let { add(Res.string.on_demand_blocked_hoplimit to "$it") }
    stats.wifi_rssi?.let { add(Res.string.wifi_rssi_signal to MetricFormatter.rssi(it)) }
    stats.wifi_drops?.let { add(Res.string.wifi_drops_label to "$it") }
    // Only the ESP-IDF code here; the signal quality screen spells out what it means.
    stats.wifi_drop_reason?.takeIf { it != 0 }?.let { add(Res.string.wifi_drop_reason_label to "$it") }
    stats.firmware_version?.let { add(Res.string.on_demand_firmware_version to it) }
}
