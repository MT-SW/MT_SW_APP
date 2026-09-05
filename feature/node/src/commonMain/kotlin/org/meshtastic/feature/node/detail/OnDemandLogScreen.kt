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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import org.meshtastic.core.common.util.DateFormatter
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.meshtastic.core.common.util.formatString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.util.formatUptime
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.on_demand_air_activity
import org.meshtastic.core.resources.on_demand_air_activity_desc
import org.meshtastic.core.resources.on_demand_air_util_tx
import org.meshtastic.core.resources.on_demand_battery_level
import org.meshtastic.core.resources.on_demand_blocked_hoplimit
import org.meshtastic.core.resources.on_demand_channel_utilization
import org.meshtastic.core.resources.on_demand_cpu_usage
import org.meshtastic.core.resources.on_demand_exchange_log
import org.meshtastic.core.resources.on_demand_exchange_log_desc
import org.meshtastic.core.resources.on_demand_firmware_version
import org.meshtastic.core.resources.on_demand_flash
import org.meshtastic.core.resources.on_demand_flood_counter
import org.meshtastic.core.resources.on_demand_fw_plus_version
import org.meshtastic.core.resources.on_demand_hops
import org.meshtastic.core.resources.on_demand_round_trip
import org.meshtastic.core.resources.on_demand_fw_plus_version_desc
import org.meshtastic.core.resources.on_demand_heap_free
import org.meshtastic.core.resources.on_demand_heap_total
import org.meshtastic.core.resources.on_demand_log_title
import org.meshtastic.core.resources.on_demand_nexthop_counter
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
import org.meshtastic.core.resources.on_demand_rssi
import org.meshtastic.core.resources.on_demand_routing_errors
import org.meshtastic.core.resources.on_demand_routing_errors_desc
import org.meshtastic.core.resources.on_demand_rx_avg_time
import org.meshtastic.core.resources.on_demand_rx_avg_time_desc
import org.meshtastic.core.resources.on_demand_rx_packet_history
import org.meshtastic.core.resources.on_demand_rx_packet_history_desc
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
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.AirUtilization
import org.meshtastic.core.ui.icon.Chart
import org.meshtastic.core.ui.icon.ChannelUtilization
import org.meshtastic.core.ui.icon.DataArray
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
import org.meshtastic.core.ui.icon.Snr
import org.meshtastic.core.ui.icon.Voltage
import org.meshtastic.feature.node.metrics.formatBytes
import org.meshtastic.proto.NodeStats
import org.meshtastic.proto.OnDemandType
import org.meshtastic.proto.Ping
import org.meshtastic.proto.PortNum

/** OnDemand diagnostics screen: one card per query type, each with its own request button and decoded result rows. */
@Composable
fun OnDemandLogScreen(viewModel: OnDemandLogViewModel, onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.on_demand_log_title),
                subtitle = null,
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                actions = {},
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
            item(key = "node-stats") {
                OnDemandQueryCard(
                    title = stringResource(Res.string.on_demand_node_stats),
                    description = stringResource(Res.string.on_demand_node_stats_desc),
                    icon = MeshtasticIcons.Memory,
                    onRequest = viewModel::requestNodeStats,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_NODE_STATS],
                ) {
                    uiState.nodeStats?.let { StatRows(nodeStatsRows(it)) }
                }
            }

            item(key = "ping") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardHeader(title = stringResource(Res.string.on_demand_ping), icon = MeshtasticIcons.NearMe)
                        Text(
                            text = stringResource(Res.string.on_demand_ping_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = viewModel::requestPing, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(Res.string.on_demand_ping))
                                }
                                uiState.pingResult?.let { StatRows(pingRows(it, uiState.pingRoundTripMs)) }
                                uiState.receivedAt[OnDemandType.RESPONSE_PING]?.let { ReceivedAtLabel(it) }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = viewModel::requestPingAck, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(Res.string.on_demand_ping_ack))
                                }
                                uiState.pingAckResult?.let { StatRows(pingRows(it, uiState.pingAckRoundTripMs)) }
                                uiState.receivedAt[OnDemandType.RESPONSE_PING_ACK]?.let { ReceivedAtLabel(it) }
                            }
                        }
                    }
                }
            }

            item(key = "nodes-online") {
                OnDemandQueryCard(
                    title = stringResource(Res.string.on_demand_nodes_online),
                    description = stringResource(Res.string.on_demand_nodes_online_desc),
                    icon = MeshtasticIcons.Nodes,
                    onRequest = viewModel::requestNodesOnline,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_NODES_ONLINE],
                ) {
                    uiState.nodesOnline.forEach { entry ->
                        ListItem(
                            text = "${entry.long_name} (${entry.short_name})",
                            leadingIcon = MeshtasticIcons.Nodes,
                            supportingText = "${entry.last_heard}s • ${entry.hops} hop • SNR ${entry.snr}",
                            trailingIcon = null,
                        )
                    }
                }
            }

            item(key = "routing-errors") {
                OnDemandQueryCard(
                    title = stringResource(Res.string.on_demand_routing_errors),
                    description = stringResource(Res.string.on_demand_routing_errors_desc),
                    icon = MeshtasticIcons.ErrorOutline,
                    onRequest = viewModel::requestRoutingErrors,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_ROUTING_ERRORS],
                ) {
                    uiState.routingErrors.filter { it.counter > 0 }.forEach { entry ->
                        ListItem(
                            text = "#${entry.num}",
                            leadingIcon = MeshtasticIcons.ErrorOutline,
                            supportingText = "${entry.counter}",
                            trailingIcon = null,
                        )
                    }
                }
            }

            item(key = "port-counters") {
                OnDemandQueryCard(
                    title = stringResource(Res.string.on_demand_port_counters),
                    description = stringResource(Res.string.on_demand_port_counters_desc),
                    icon = MeshtasticIcons.DataArray,
                    onRequest = viewModel::requestPortCounterHistory,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_PORT_COUNTER_HISTORY],
                ) {
                    uiState.portCounterHistory.forEach { entry ->
                        val info = portInfo(entry.port)
                        ListItem(
                            text = "${info.label} (${entry.port})",
                            leadingIcon = MeshtasticIcons.DataArray,
                            supportingText = "${info.description} • ${entry.count}",
                            trailingIcon = null,
                        )
                    }
                }
            }

            item(key = "air-activity") {
                OnDemandQueryCard(
                    title = stringResource(Res.string.on_demand_air_activity),
                    description = stringResource(Res.string.on_demand_air_activity_desc),
                    icon = MeshtasticIcons.AirUtilization,
                    onRequest = viewModel::requestAirActivityHistory,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_AIR_ACTIVITY_HISTORY],
                ) {
                    uiState.airActivityHistory.forEachIndexed { i, entry ->
                        ListItem(
                            text = "#$i",
                            leadingIcon = MeshtasticIcons.AirUtilization,
                            supportingText = "TX ${entry.tx_time}ms • RX ${entry.rx_time}ms • RX! ${entry.rxBad_time}ms",
                            trailingIcon = null,
                        )
                    }
                }
            }

            item(key = "exchange-log") {
                OnDemandQueryCard(
                    title = stringResource(Res.string.on_demand_exchange_log),
                    description = stringResource(Res.string.on_demand_exchange_log_desc),
                    icon = MeshtasticIcons.MeshHub,
                    onRequest = viewModel::requestExchangeLog,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_PACKET_EXCHANGE_HISTORY],
                ) {
                    uiState.exchangeLog.forEach { entry ->
                        val from = NodeAddress.numToDefaultId(entry.from_node)
                        val to = NodeAddress.numToDefaultId(entry.to_node)
                        val info = portInfo(entry.port_num)
                        ListItem(
                            text = "$from → $to",
                            leadingIcon = MeshtasticIcons.MeshHub,
                            supportingText = "${info.label} (${entry.port_num}) — ${info.description}",
                            trailingIcon = null,
                        )
                    }
                }
            }

            item(key = "rx-avg-time") {
                OnDemandQueryCard(
                    title = stringResource(Res.string.on_demand_rx_avg_time),
                    description = stringResource(Res.string.on_demand_rx_avg_time_desc),
                    icon = MeshtasticIcons.History,
                    onRequest = viewModel::requestRxAvgTimeHistory,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_RX_AVG_TIME],
                ) {
                    if (uiState.rxAvgTimeHistory.isNotEmpty()) {
                        Text(
                            uiState.rxAvgTimeHistory.joinToString(", "),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            item(key = "rx-packet-history") {
                OnDemandQueryCard(
                    title = stringResource(Res.string.on_demand_rx_packet_history),
                    description = stringResource(Res.string.on_demand_rx_packet_history_desc),
                    icon = MeshtasticIcons.Chart,
                    onRequest = viewModel::requestRxPacketHistory,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_PACKET_RX_HISTORY],
                ) {
                    if (uiState.rxPacketHistory.isNotEmpty()) {
                        Text(
                            uiState.rxPacketHistory.joinToString(", "),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            item(key = "fw-plus-version") {
                OnDemandQueryCard(
                    title = stringResource(Res.string.on_demand_fw_plus_version),
                    description = stringResource(Res.string.on_demand_fw_plus_version_desc),
                    icon = MeshtasticIcons.MeshRadio,
                    onRequest = viewModel::requestFwPlusVersion,
                    receivedAtMillis = uiState.receivedAt[OnDemandType.RESPONSE_FW_PLUS_VERSION],
                ) {
                    uiState.fwPlusVersion?.let { version ->
                        ListItem(
                            text = stringResource(Res.string.on_demand_fw_plus_version),
                            leadingIcon = MeshtasticIcons.MeshRadio,
                            supportingText = "v${version.version_number}",
                            trailingIcon = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(imageVector = icon, contentDescription = null)
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun OnDemandQueryCard(
    title: String,
    description: String,
    icon: ImageVector,
    onRequest: () -> Unit,
    receivedAtMillis: Long? = null,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CardHeader(title = title, icon = icon)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRequest) { Text(title) }
                receivedAtMillis?.let { ReceivedAtLabel(it) }
            }
            content()
        }
    }
}

@Composable
private fun ReceivedAtLabel(receivedAtMillis: Long) {
    Text(
        text = stringResource(Res.string.on_demand_received_at, DateFormatter.formatDateTime(receivedAtMillis)),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

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
    Res.string.on_demand_rssi -> MeshtasticIcons.Rssi
    Res.string.on_demand_snr -> MeshtasticIcons.Snr
    Res.string.on_demand_hops -> MeshtasticIcons.HopCount
    Res.string.on_demand_round_trip -> MeshtasticIcons.History
    else -> MeshtasticIcons.Memory
}

/** Localized name + resource IDs for a known portnum. */
private data class PortInfo(val labelRes: StringResource, val descRes: StringResource)

/** Localized name + description already resolved to display strings. */
private data class ResolvedPortInfo(val label: String, val description: String)

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
private fun portInfo(port: Int): ResolvedPortInfo {
    val known = KNOWN_PORTS[port]
    if (known != null) return ResolvedPortInfo(stringResource(known.labelRes), stringResource(known.descRes))
    val fallbackLabel =
        PortNum.fromValue(port)
            ?.name
            ?.removeSuffix("_APP")
            ?.split("_")
            ?.joinToString(" ") { it.lowercase().replaceFirstChar(Char::titlecase) }
            ?: stringResource(Res.string.port_name_unknown)
    return ResolvedPortInfo(fallbackLabel, stringResource(Res.string.port_desc_unknown))
}

private fun nodeStatsRows(stats: NodeStats): List<Pair<StringResource, String>> = buildList {
    stats.battery_level?.let { add(Res.string.on_demand_battery_level to "$it%") }
    stats.uptime_seconds?.let { add(Res.string.on_demand_uptime to formatUptime(it)) }
    stats.channel_utilization?.let { add(Res.string.on_demand_channel_utilization to "${formatString("%.2f", it)}%") }
    stats.air_util_tx?.let { add(Res.string.on_demand_air_util_tx to "${formatString("%.2f", it)}%") }
    stats.num_packets_tx?.let { add(Res.string.on_demand_packets_tx to "$it") }
    stats.num_packets_rx?.let { add(Res.string.on_demand_packets_rx to "$it") }
    stats.num_packets_rx_bad?.let { add(Res.string.on_demand_packets_rx_bad to "$it") }
    stats.num_online_nodes?.let { add(Res.string.on_demand_stat_nodes_online to "$it") }
    stats.num_total_nodes?.let { add(Res.string.on_demand_stat_nodes_total to "$it") }
    stats.reboots?.let { add(Res.string.on_demand_reboots to "$it") }
    stats.memory_free_cheap?.let { add(Res.string.on_demand_heap_free to formatBytes(it.toLong())) }
    stats.memory_total?.let { add(Res.string.on_demand_heap_total to formatBytes(it.toLong())) }
    stats.cpu_usage_percent?.let { add(Res.string.on_demand_cpu_usage to "$it%") }
    if (stats.flash_used_bytes != null || stats.flash_total_bytes != null) {
        val used = stats.flash_used_bytes?.let { formatBytes(it.toLong()) } ?: "?"
        val total = stats.flash_total_bytes?.let { formatBytes(it.toLong()) } ?: "?"
        add(Res.string.on_demand_flash to "$used / $total")
    }
    if (stats.memory_psram_free != null || stats.memory_psram_total != null) {
        val free = stats.memory_psram_free?.let { formatBytes(it.toLong()) } ?: "?"
        val total = stats.memory_psram_total?.let { formatBytes(it.toLong()) } ?: "?"
        add(Res.string.on_demand_psram to "$free / $total")
    }
    stats.flood_counter?.let { add(Res.string.on_demand_flood_counter to "$it") }
    stats.nexthop_counter?.let { add(Res.string.on_demand_nexthop_counter to "$it") }
    stats.blocked_by_hoplimit?.let { add(Res.string.on_demand_blocked_hoplimit to "$it") }
    stats.firmware_version?.let { add(Res.string.on_demand_firmware_version to it) }
}

private fun pingRows(ping: Ping, roundTripMs: Long?): List<Pair<StringResource, String>> = buildList {
    roundTripMs?.let { add(Res.string.on_demand_round_trip to "${it}ms") }
    if (ping.rx_rssi != null || ping.snr != null) {
        ping.rx_rssi?.let { add(Res.string.on_demand_rssi to "$it") }
        ping.snr?.let { add(Res.string.on_demand_snr to "$it") }
    } else {
        add(Res.string.on_demand_hops to "${ping.hops}")
    }
}