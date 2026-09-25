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
@file:OptIn(ExperimentalMaterial3Api::class)

package org.meshtastic.feature.settings.sniffer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.datastore.SnifferBufferOverflowPolicy
import org.meshtastic.core.datastore.SnifferLogFormat
import org.meshtastic.core.datastore.SnifferSource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.mqtt_sniffer_log_empty
import org.meshtastic.core.resources.save_log_to_file
import org.meshtastic.core.resources.sniffer_auto_scroll
import org.meshtastic.core.resources.sniffer_category_admin
import org.meshtastic.core.resources.sniffer_category_alert
import org.meshtastic.core.resources.sniffer_category_encrypted
import org.meshtastic.core.resources.sniffer_category_neighborinfo
import org.meshtastic.core.resources.sniffer_category_nodeinfo
import org.meshtastic.core.resources.sniffer_category_paxcounter
import org.meshtastic.core.resources.sniffer_category_position
import org.meshtastic.core.resources.sniffer_category_routing
import org.meshtastic.core.resources.sniffer_category_storeforward
import org.meshtastic.core.resources.sniffer_category_telemetry
import org.meshtastic.core.resources.sniffer_category_text_message
import org.meshtastic.core.resources.sniffer_category_traceroute
import org.meshtastic.core.resources.sniffer_category_unknown
import org.meshtastic.core.resources.sniffer_category_waypoint
import org.meshtastic.core.resources.sniffer_chip_hops
import org.meshtastic.core.resources.sniffer_chip_received
import org.meshtastic.core.resources.sniffer_chip_source_count
import org.meshtastic.core.resources.sniffer_clear_log
import org.meshtastic.core.resources.sniffer_content_title
import org.meshtastic.core.resources.sniffer_decrypt_payloads_summary
import org.meshtastic.core.resources.sniffer_decrypt_payloads_title
import org.meshtastic.core.resources.sniffer_export_format_csv
import org.meshtastic.core.resources.sniffer_export_format_json
import org.meshtastic.core.resources.sniffer_export_format_title
import org.meshtastic.core.resources.sniffer_export_format_txt
import org.meshtastic.core.resources.sniffer_gear_menu
import org.meshtastic.core.resources.sniffer_group_by_gateway_summary
import org.meshtastic.core.resources.sniffer_group_by_gateway_title
import org.meshtastic.core.resources.sniffer_load_log_summary
import org.meshtastic.core.resources.sniffer_load_log_title
import org.meshtastic.core.resources.sniffer_log_empty
import org.meshtastic.core.resources.sniffer_mqtt_section_title
import org.meshtastic.core.resources.sniffer_not_supported_summary
import org.meshtastic.core.resources.sniffer_packet_metadata_channel
import org.meshtastic.core.resources.sniffer_packet_metadata_id
import org.meshtastic.core.resources.sniffer_packet_metadata_size
import org.meshtastic.core.resources.sniffer_packet_metadata_title
import org.meshtastic.core.resources.sniffer_panel_off_summary
import org.meshtastic.core.resources.sniffer_radio_section_title
import org.meshtastic.core.resources.sniffer_receipt_direct
import org.meshtastic.core.resources.sniffer_receipts_title
import org.meshtastic.core.resources.sniffer_source_mqtt_chip
import org.meshtastic.core.resources.sniffer_source_mqtt_unavailable_summary
import org.meshtastic.core.resources.sniffer_source_off
import org.meshtastic.core.resources.sniffer_source_radio_chip
import org.meshtastic.core.resources.sniffer_source_title
import org.meshtastic.core.resources.sniffer_summary_neighbor_count
import org.meshtastic.core.resources.sniffer_summary_nodeinfo_unknown
import org.meshtastic.core.resources.sniffer_summary_position_unknown
import org.meshtastic.core.resources.sniffer_summary_telemetry_unknown
import org.meshtastic.core.ui.component.BasicListItem
import org.meshtastic.core.ui.component.CopyIconButton
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.FolderOpen
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Save
import org.meshtastic.core.ui.icon.Settings
import org.meshtastic.feature.settings.export.LogExportSaverLauncher
import org.meshtastic.feature.settings.sniffer.mqtt.GroupedMqttSniffedPacket
import org.meshtastic.feature.settings.sniffer.mqtt.MqttPacketReceipt
import org.meshtastic.feature.settings.util.PacketSummary
import org.meshtastic.proto.PortNum

/** The three top-bar actions for [SnifferSettingsScreen] -- save, open the gear menu, and clear. */
@Composable
internal fun SnifferTopBarActions(
    display: SnifferDisplayState,
    exportFormat: SnifferLogFormat,
    exportSaver: LogExportSaverLauncher,
    onOpenGearSheet: () -> Unit,
    onClear: () -> Unit,
) {
    Row {
        IconButton(
            enabled = display.itemCount > 0,
            onClick = {
                val rows =
                    if (display.showingMqtt) {
                        display.groupedMqttPackets.asReversed().map {
                            it.packet.toExportRow(gatewayIds = it.gatewayIds)
                        }
                    } else {
                        display.displayedRadioPackets.asReversed().map { it.toExportRow() }
                    }
                val sourceLabel = if (display.showingMqtt) "mqtt" else "radio"
                exportSaver.save(buildSnifferExport(rows, exportFormat, sourceLabel))
            },
        ) {
            Icon(imageVector = MeshtasticIcons.Save, contentDescription = stringResource(Res.string.save_log_to_file))
        }
        IconButton(onClick = onOpenGearSheet) {
            Icon(
                imageVector = MeshtasticIcons.Settings,
                contentDescription = stringResource(Res.string.sniffer_gear_menu),
            )
        }
        IconButton(enabled = display.itemCount > 0, onClick = onClear) {
            Icon(
                imageVector = MeshtasticIcons.Delete,
                contentDescription = stringResource(Res.string.sniffer_clear_log),
            )
        }
    }
}

/** The empty-state message for [SnifferSettingsScreen] -- what to say depends on why there's nothing to show. */
@Composable
internal fun snifferEmptyStateText(loadedLog: LoadedSnifferLog?, activeSource: SnifferSource): String = when {
    loadedLog != null -> stringResource(Res.string.sniffer_log_empty)
    activeSource == SnifferSource.RADIO -> stringResource(Res.string.sniffer_log_empty)
    activeSource == SnifferSource.MQTT -> stringResource(Res.string.mqtt_sniffer_log_empty)
    else -> stringResource(Res.string.sniffer_panel_off_summary)
}

@Composable
internal fun SnifferGearSheet(
    onDismissRequest: () -> Unit,
    activeSource: SnifferSource,
    onSelectSource: (SnifferSource) -> Unit,
    radioSelectable: Boolean,
    radioLoading: Boolean,
    mqttConfigured: Boolean,
    groupByGateway: Boolean,
    onGroupByGatewayChange: (Boolean) -> Unit,
    autoScroll: Boolean,
    onAutoScrollChange: (Boolean) -> Unit,
    decryptPayloads: Boolean,
    onDecryptPayloadsChange: (Boolean) -> Unit,
    exportFormat: SnifferLogFormat,
    onExportFormatChange: (SnifferLogFormat) -> Unit,
    bufferOverflowPolicy: SnifferBufferOverflowPolicy,
    onBufferOverflowPolicyChange: (SnifferBufferOverflowPolicy) -> Unit,
    onLoadLogClick: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            SnifferSourceSelector(
                activeSource = activeSource,
                onSelectSource = onSelectSource,
                radioSelectable = radioSelectable,
                radioLoading = radioLoading,
                mqttConfigured = mqttConfigured,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SnifferPreferencesSection(
                groupByGateway = groupByGateway,
                onGroupByGatewayChange = onGroupByGatewayChange,
                autoScroll = autoScroll,
                onAutoScrollChange = onAutoScrollChange,
                decryptPayloads = decryptPayloads,
                onDecryptPayloadsChange = onDecryptPayloadsChange,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SnifferExportFormatSelector(exportFormat = exportFormat, onExportFormatChange = onExportFormatChange)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SnifferBufferOverflowSelector(
                bufferOverflowPolicy = bufferOverflowPolicy,
                onBufferOverflowPolicyChange = onBufferOverflowPolicyChange,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            BasicListItem(
                text = stringResource(Res.string.sniffer_load_log_title),
                supportingText = stringResource(Res.string.sniffer_load_log_summary),
                leadingIcon = MeshtasticIcons.FolderOpen,
                onClick = onLoadLogClick,
            )
        }
    }
}

@Composable
private fun SnifferSourceSelector(
    activeSource: SnifferSource,
    onSelectSource: (SnifferSource) -> Unit,
    radioSelectable: Boolean,
    radioLoading: Boolean,
    mqttConfigured: Boolean,
) {
    Text(
        text = stringResource(Res.string.sniffer_source_title),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
    BasicListItem(
        text = stringResource(Res.string.sniffer_source_off),
        onClick = { onSelectSource(SnifferSource.OFF) },
        trailingContent = { RadioButton(selected = activeSource == SnifferSource.OFF, onClick = null) },
    )
    BasicListItem(
        text = stringResource(Res.string.sniffer_radio_section_title),
        enabled = radioSelectable,
        supportingText =
        if (!radioLoading && !radioSelectable) {
            stringResource(Res.string.sniffer_not_supported_summary)
        } else {
            null
        },
        onClick = { onSelectSource(SnifferSource.RADIO) },
        trailingContent = {
            if (radioLoading) {
                CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                RadioButton(selected = activeSource == SnifferSource.RADIO, onClick = null, enabled = radioSelectable)
            }
        },
    )
    BasicListItem(
        text = stringResource(Res.string.sniffer_mqtt_section_title),
        enabled = mqttConfigured,
        supportingText =
        if (!mqttConfigured) stringResource(Res.string.sniffer_source_mqtt_unavailable_summary) else null,
        onClick = { onSelectSource(SnifferSource.MQTT) },
        trailingContent = {
            RadioButton(selected = activeSource == SnifferSource.MQTT, onClick = null, enabled = mqttConfigured)
        },
    )
}

@Composable
private fun SnifferPreferencesSection(
    groupByGateway: Boolean,
    onGroupByGatewayChange: (Boolean) -> Unit,
    autoScroll: Boolean,
    onAutoScrollChange: (Boolean) -> Unit,
    decryptPayloads: Boolean,
    onDecryptPayloadsChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        title = stringResource(Res.string.sniffer_group_by_gateway_title),
        summary = stringResource(Res.string.sniffer_group_by_gateway_summary),
        checked = groupByGateway,
        enabled = true,
        onCheckedChange = onGroupByGatewayChange,
    )
    SwitchPreference(
        title = stringResource(Res.string.sniffer_auto_scroll),
        checked = autoScroll,
        enabled = true,
        onCheckedChange = onAutoScrollChange,
    )
    SwitchPreference(
        title = stringResource(Res.string.sniffer_decrypt_payloads_title),
        summary = stringResource(Res.string.sniffer_decrypt_payloads_summary),
        checked = decryptPayloads,
        enabled = true,
        onCheckedChange = onDecryptPayloadsChange,
    )
}

@Composable
private fun SnifferExportFormatSelector(
    exportFormat: SnifferLogFormat,
    onExportFormatChange: (SnifferLogFormat) -> Unit,
) {
    Column {
        Text(
            text = stringResource(Res.string.sniffer_export_format_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            val formats = SnifferLogFormat.entries
            formats.forEachIndexed { index, format ->
                val label =
                    when (format) {
                        SnifferLogFormat.TXT -> stringResource(Res.string.sniffer_export_format_txt)
                        SnifferLogFormat.JSON -> stringResource(Res.string.sniffer_export_format_json)
                        SnifferLogFormat.CSV -> stringResource(Res.string.sniffer_export_format_csv)
                    }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, formats.size),
                    onClick = { onExportFormatChange(format) },
                    selected = exportFormat == format,
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun SnifferBufferOverflowSelector(
    bufferOverflowPolicy: SnifferBufferOverflowPolicy,
    onBufferOverflowPolicyChange: (SnifferBufferOverflowPolicy) -> Unit,
) {
    Column {
        Text(
            text = stringResource(Res.string.sniffer_buffer_overflow_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
        )
        Text(
            text = stringResource(Res.string.sniffer_buffer_overflow_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            val policies = SnifferBufferOverflowPolicy.entries
            policies.forEachIndexed { index, policy ->
                val label =
                    when (policy) {
                        SnifferBufferOverflowPolicy.STOP -> stringResource(Res.string.sniffer_buffer_overflow_stop)

                        SnifferBufferOverflowPolicy.OVERWRITE ->
                            stringResource(Res.string.sniffer_buffer_overflow_overwrite)
                    }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, policies.size),
                    onClick = { onBufferOverflowPolicyChange(policy) },
                    selected = bufferOverflowPolicy == policy,
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun packetCategoryColor(portNum: Int?, isEncrypted: Boolean): Color = when {
    isEncrypted -> Color(0xFF9E9E9E)
    portNum == PortNum.POSITION_APP.value -> Color(0xFF4CAF50)
    portNum == PortNum.NODEINFO_APP.value -> Color(0xFF9C27B0)
    portNum == PortNum.NEIGHBORINFO_APP.value -> Color(0xFF00BCD4)
    portNum == PortNum.TELEMETRY_APP.value -> Color(0xFFFF9800)
    portNum == PortNum.TEXT_MESSAGE_APP.value -> Color(0xFF2196F3)
    portNum == PortNum.ALERT_APP.value -> Color(0xFFF44336)
    portNum == PortNum.TRACEROUTE_APP.value -> Color(0xFFFFC107)
    portNum == PortNum.ROUTING_APP.value -> Color(0xFF607D8B)
    portNum == PortNum.ADMIN_APP.value -> Color(0xFFFF5722)
    portNum == PortNum.WAYPOINT_APP.value -> Color(0xFF8BC34A)
    portNum == PortNum.PAXCOUNTER_APP.value -> Color(0xFF795548)
    portNum == PortNum.STORE_FORWARD_APP.value -> Color(0xFF3F51B5)
    portNum == PortNum.STORE_FORWARD_PLUSPLUS_APP.value -> Color(0xFF3F51B5)
    else -> Color(0xFF757575)
}

@Composable
private fun packetCategoryLabel(portNum: Int?, isEncrypted: Boolean): String = when {
    isEncrypted -> stringResource(Res.string.sniffer_category_encrypted)
    portNum == PortNum.POSITION_APP.value -> stringResource(Res.string.sniffer_category_position)
    portNum == PortNum.NODEINFO_APP.value -> stringResource(Res.string.sniffer_category_nodeinfo)
    portNum == PortNum.NEIGHBORINFO_APP.value -> stringResource(Res.string.sniffer_category_neighborinfo)
    portNum == PortNum.TELEMETRY_APP.value -> stringResource(Res.string.sniffer_category_telemetry)
    portNum == PortNum.TEXT_MESSAGE_APP.value -> stringResource(Res.string.sniffer_category_text_message)
    portNum == PortNum.ALERT_APP.value -> stringResource(Res.string.sniffer_category_alert)
    portNum == PortNum.TRACEROUTE_APP.value -> stringResource(Res.string.sniffer_category_traceroute)
    portNum == PortNum.ROUTING_APP.value -> stringResource(Res.string.sniffer_category_routing)
    portNum == PortNum.ADMIN_APP.value -> stringResource(Res.string.sniffer_category_admin)
    portNum == PortNum.WAYPOINT_APP.value -> stringResource(Res.string.sniffer_category_waypoint)
    portNum == PortNum.PAXCOUNTER_APP.value -> stringResource(Res.string.sniffer_category_paxcounter)
    portNum == PortNum.STORE_FORWARD_APP.value -> stringResource(Res.string.sniffer_category_storeforward)
    portNum == PortNum.STORE_FORWARD_PLUSPLUS_APP.value -> stringResource(Res.string.sniffer_category_storeforward)
    else -> stringResource(Res.string.sniffer_category_unknown)
}

/** Localizes a [PacketSummary] into the card's always-visible content line -- null when there is nothing to show. */
@Composable
private fun PacketSummary.render(): String? = when (this) {
    is PacketSummary.Text -> text

    is PacketSummary.PositionSummary ->
        if (latitude != null && longitude != null) {
            val coords =
                "${NumberFormatter.format(latitude, POSITION_DECIMAL_PLACES)}, " +
                    NumberFormatter.format(longitude, POSITION_DECIMAL_PLACES)
            altitudeMeters?.let { "$coords ($it m)" } ?: coords
        } else {
            stringResource(Res.string.sniffer_summary_position_unknown)
        }

    is PacketSummary.NodeInfoSummary ->
        listOfNotNull(longName, shortName?.let { "($it)" })
            .joinToString(" ")
            .ifBlank { stringResource(Res.string.sniffer_summary_nodeinfo_unknown) }

    is PacketSummary.TelemetrySummary -> {
        val parts =
            buildList {
                temperatureCelsius?.let { add(MetricFormatter.temperature(it, isFahrenheit = false)) }
                humidityPercent?.let { add(MetricFormatter.humidity(it)) }
                pressureHpa?.let { add(MetricFormatter.pressure(it)) }
                voltage?.let { add(MetricFormatter.voltage(it)) }
                batteryPercent?.let { add(MetricFormatter.percent(it)) }
            }
        parts.joinToString(" • ").ifBlank { stringResource(Res.string.sniffer_summary_telemetry_unknown) }
    }

    is PacketSummary.NeighborCount -> stringResource(Res.string.sniffer_summary_neighbor_count, count)
}

private const val POSITION_DECIMAL_PLACES = 5

/** Timestamp for the first (oldest) receipt, absolute; every later one as a delta from it -- "+32ms" / "+2.4s". */
private fun formatReceiptTime(receivedAtMillis: Long, firstReceivedAtMillis: Long): String {
    if (receivedAtMillis <= firstReceivedAtMillis) {
        return "${DateFormatter.formatDate(receivedAtMillis)} ${DateFormatter.formatTimeWithSeconds(receivedAtMillis)}"
    }
    val deltaMs = receivedAtMillis - firstReceivedAtMillis
    return if (deltaMs < MILLIS_PER_SECOND) {
        "+${deltaMs}ms"
    } else {
        "+${NumberFormatter.format(deltaMs / MILLIS_PER_SECOND.toDouble(), 1)}s"
    }
}

private const val MILLIS_PER_SECOND = 1000L

/** One row of the expandable "receipts" list -- who this copy came through, its signal, and when. */
@Composable
private fun ReceiptRow(label: String, snr: Float?, rssi: Int?, timeLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            text = listOfNotNull(MetricFormatter.snr(snr), MetricFormatter.rssi(rssi)).joinToString(" • "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Shared visual shell for both card types: a colored left strip keyed to the packet's category, dark card body. */
@Composable
private fun PacketCardShell(
    portNum: Int?,
    isEncrypted: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier =
                Modifier.width(4.dp).fillMaxHeight().background(packetCategoryColor(portNum, isEncrypted)),
            )
            Column(
                modifier = Modifier.weight(1f).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

@Composable
internal fun GroupedRadioPacketCard(
    grouped: GroupedSniffedPacket,
    decryptPayloads: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val packet = grouped.packet
    PacketCardShell(portNum = packet.portNum, isEncrypted = packet.isEncrypted, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = packetCategoryLabel(packet.portNum, packet.isEncrypted),
                style = MaterialTheme.typography.labelLarge,
                color = packetCategoryColor(packet.portNum, packet.isEncrypted),
            )
            Text(
                text =
                "${DateFormatter.formatDate(packet.receivedAtMillis)} " +
                    DateFormatter.formatTimeWithSeconds(packet.receivedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val fromLabel = packet.fromShortName?.let { "${packet.fromId} ($it)" } ?: packet.fromId
        val toLabel = packet.toShortName?.let { "${packet.toId} ($it)" } ?: packet.toId
        Text(text = "$fromLabel → $toLabel", style = MaterialTheme.typography.titleSmall)
        val radioSummaryText = packet.summary?.render()
        if (radioSummaryText != null) {
            Text(text = radioSummaryText, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(
                onClick = onClick,
                label = {
                    val label = stringResource(Res.string.sniffer_source_radio_chip)
                    Text(
                        if (grouped.relayIds.size > 1) {
                            stringResource(Res.string.sniffer_chip_source_count, label, grouped.relayIds.size)
                        } else {
                            label
                        },
                    )
                },
            )
            AssistChip(
                onClick = onClick,
                label = {
                    Text(
                        stringResource(
                            Res.string.sniffer_chip_hops,
                            packet.hopStart - packet.hopLimit,
                            packet.hopStart,
                        ),
                    )
                },
            )
            AssistChip(
                onClick = onClick,
                label = {
                    val signal = listOfNotNull(MetricFormatter.snr(packet.snr), MetricFormatter.rssi(packet.rssi))
                    Text(signal.joinToString(" • "))
                },
            )
            if (grouped.receipts.size > 1) {
                AssistChip(
                    onClick = onClick,
                    colors =
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    label = { Text(stringResource(Res.string.sniffer_chip_received, grouped.receipts.size)) },
                )
            }
        }
        if (isExpanded) {
            if (grouped.receipts.size > 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = stringResource(Res.string.sniffer_receipts_title, grouped.receipts.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val firstAt = grouped.receipts.minOf { it.receivedAtMillis }
                grouped.receipts.forEach { receipt ->
                    ReceiptRow(
                        label = receipt.relayId ?: stringResource(Res.string.sniffer_receipt_direct),
                        snr = receipt.snr,
                        rssi = receipt.rssi,
                        timeLabel = formatReceiptTime(receipt.receivedAtMillis, firstAt),
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(Res.string.sniffer_content_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val payload = if (decryptPayloads) packet.decodedPayload ?: packet.payloadHex else packet.payloadHex
            Text(text = payload, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(Res.string.sniffer_packet_metadata_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.sniffer_packet_metadata_channel, packet.channel),
                style = MaterialTheme.typography.bodySmall,
            )
            packet.packetId?.let {
                Text(
                    text = stringResource(Res.string.sniffer_packet_metadata_id, it.toString()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            CopyIconButton(valueToCopy = packet.copyText, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
internal fun GroupedMqttPacketCard(
    grouped: GroupedMqttSniffedPacket,
    decryptPayloads: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val packet = grouped.packet
    PacketCardShell(portNum = packet.portNum, isEncrypted = packet.isEncrypted, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text =
                if (packet.isJson) {
                    packet.topic
                } else {
                    packetCategoryLabel(packet.portNum, packet.isEncrypted)
                },
                style = MaterialTheme.typography.labelLarge,
                color = packetCategoryColor(packet.portNum, packet.isEncrypted),
            )
            Text(
                text =
                "${DateFormatter.formatDate(packet.receivedAtMillis)} " +
                    DateFormatter.formatTimeWithSeconds(packet.receivedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!packet.isJson && packet.fromId.isNotEmpty()) {
            val fromLabel = packet.fromShortName?.let { "${packet.fromId} ($it)" } ?: packet.fromId
            val toLabel = packet.toShortName?.let { "${packet.toId} ($it)" } ?: packet.toId
            Text(
                text = "$fromLabel → $toLabel" + (packet.channelId?.let { " • $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val mqttSummaryText = packet.summary?.render()
        when {
            mqttSummaryText != null -> Text(text = mqttSummaryText, style = MaterialTheme.typography.bodyMedium)
            packet.isJson ->
                Text(text = packet.decodedPayload ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(
                onClick = onClick,
                label = {
                    val label = stringResource(Res.string.sniffer_source_mqtt_chip)
                    Text(
                        if (grouped.gatewayIds.size > 1) {
                            stringResource(Res.string.sniffer_chip_source_count, label, grouped.gatewayIds.size)
                        } else {
                            label
                        },
                    )
                },
            )
            if (packet.snr != null || packet.rssi != null) {
                AssistChip(
                    onClick = onClick,
                    label = {
                        val signal = listOfNotNull(MetricFormatter.snr(packet.snr), MetricFormatter.rssi(packet.rssi))
                        Text(signal.joinToString(" • "))
                    },
                )
            }
            if (grouped.receipts.size > 1) {
                AssistChip(
                    onClick = onClick,
                    colors =
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    label = { Text(stringResource(Res.string.sniffer_chip_received, grouped.receipts.size)) },
                )
            }
        }
        if (isExpanded) {
            if (grouped.receipts.size > 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = stringResource(Res.string.sniffer_receipts_title, grouped.receipts.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val firstAt = grouped.receipts.minOf { it.receivedAtMillis }
                grouped.receipts.forEach { receipt ->
                    ReceiptRow(
                        label = receipt.gatewayId ?: stringResource(Res.string.sniffer_receipt_direct),
                        snr = receipt.snr,
                        rssi = receipt.rssi,
                        timeLabel = formatReceiptTime(receipt.receivedAtMillis, firstAt),
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(Res.string.sniffer_content_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val payload = if (decryptPayloads) packet.decodedPayload ?: packet.payloadHex else packet.payloadHex
            Text(text = payload, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
            CopyIconButton(valueToCopy = packet.copyText, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
