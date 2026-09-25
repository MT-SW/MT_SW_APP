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
