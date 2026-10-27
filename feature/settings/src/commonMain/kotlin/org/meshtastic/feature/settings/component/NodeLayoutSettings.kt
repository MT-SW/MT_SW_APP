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
package org.meshtastic.feature.settings.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okio.ByteString.Companion.toByteString
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import org.meshtastic.core.model.NodeExtraStats
import org.meshtastic.core.ui.component.LocalNodeExtraConfig
import org.meshtastic.core.ui.component.LocalNodeExtraStats
import org.meshtastic.core.ui.component.LocalNodeExtraTick
import org.meshtastic.core.ui.component.NODE_EXTRA_PAGE_MILLIS
import org.meshtastic.core.ui.component.NodeExtraConfig
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeExtraMode
import org.meshtastic.core.model.NodeListDensity
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.node_layout_channel
import org.meshtastic.core.resources.node_layout_compact
import org.meshtastic.core.resources.node_layout_compact_fields_header
import org.meshtastic.core.resources.node_layout_complete
import org.meshtastic.core.resources.node_layout_complete_description
import org.meshtastic.core.resources.node_layout_device_role
import org.meshtastic.core.resources.node_layout_distance_and_bearing
import org.meshtastic.core.resources.node_layout_hops_away
import org.meshtastic.core.resources.node_layout_last_heard_time
import org.meshtastic.core.resources.node_layout_log_icons
import org.meshtastic.core.resources.node_layout_neighbors
import org.meshtastic.core.resources.node_layout_power
import org.meshtastic.core.resources.node_layout_preview
import org.meshtastic.core.resources.node_layout_relative_last_heard
import org.meshtastic.core.resources.node_layout_section_title
import org.meshtastic.core.resources.node_layout_signal_direct_only
import org.meshtastic.core.ui.component.NodeItem
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.component.nodeExtraLegend
import org.meshtastic.core.ui.component.NodeItemCompact
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.feature.settings.NodeListSettingsState
import org.meshtastic.proto.Config
import org.meshtastic.proto.DeviceMetrics
import org.meshtastic.proto.EnvironmentMetrics
import org.meshtastic.proto.HardwareModel
import org.meshtastic.proto.Position
import org.meshtastic.proto.User

/** Node layout density picker and compact field toggles for the Settings screen. */
@Composable
@Suppress("LongParameterList", "LongMethod")
fun NodeLayoutSettings(
    state: NodeListSettingsState,
    onDensityChange: (NodeListDensity) -> Unit,
    onShowPowerChange: (Boolean) -> Unit,
    onShowLastHeardChange: (Boolean) -> Unit,
    onLastHeardIsRelativeChange: (Boolean) -> Unit,
    onShowLocationChange: (Boolean) -> Unit,
    onShowHopsChange: (Boolean) -> Unit,
    onShowSignalChange: (Boolean) -> Unit,
    onShowChannelChange: (Boolean) -> Unit,
    onShowRoleChange: (Boolean) -> Unit,
    onShowTelemetryChange: (Boolean) -> Unit,
    onExtraModeChange: (NodeExtraMode) -> Unit = {},
    onExtraIconsOnlyChange: (Boolean) -> Unit = {},
    onExtraPacketsChange: (Boolean) -> Unit = {},
    onExtraHardwareChange: (Boolean) -> Unit = {},
    onExtraEtherChange: (Boolean) -> Unit = {},
    onExtraNeighborsChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ExpressiveSection(modifier = modifier, title = stringResource(Res.string.node_layout_section_title)) {
        // Density picker
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            NodeListDensity.entries.forEachIndexed { index, option ->
                val label =
                    when (option) {
                        NodeListDensity.COMPLETE -> stringResource(Res.string.node_layout_complete)
                        NodeListDensity.COMPACT -> stringResource(Res.string.node_layout_compact)
                    }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, NodeListDensity.entries.size),
                    onClick = { onDensityChange(option) },
                    selected = state.density == option,
                    label = { Text(label) },
                )
            }
        }

        // Live preview — positioned above toggles so it doesn't jump with list size changes
        Text(
            text = stringResource(Res.string.node_layout_preview),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
        )

        val previewNode = remember { previewSampleNode() }
        val localNode = remember { previewLocalNode() }

        // The preview shows the extra line too, with sample readings, and pages turn by themselves here as well.
        val previewTick = remember { mutableIntStateOf(0) }
        if (state.extraMode == NodeExtraMode.PAGED) {
            LaunchedEffect(Unit) {
                while (true) {
                    delay(NODE_EXTRA_PAGE_MILLIS)
                    previewTick.intValue++
                }
            }
        }
        val previewStats = remember { mapOf(previewNode.num to previewSampleExtraStats()) }
        CompositionLocalProvider(
            LocalNodeExtraConfig provides NodeExtraConfig(
                state.extraMode,
                state.extraIconsOnly,
                state.extraPackets,
                state.extraHardware,
                state.extraEther,
                state.extraNeighbors,
            ),
            LocalNodeExtraStats provides previewStats,
            LocalNodeExtraTick provides previewTick,
        ) {
        Box(modifier = Modifier.animateContentSize().padding(bottom = 8.dp)) {
            when (state.density) {
                NodeListDensity.COMPLETE ->
                    NodeItem(
                        thisNode = localNode,
                        thatNode = previewNode,
                        distanceUnits = MeasurementSystem.METRIC,
                        tempInFahrenheit = false,
                        connectionState = ConnectionState.Connected,
                        showTelemetry = state.showTelemetry,
                        neighborCount = 4,
                    )

                NodeListDensity.COMPACT ->
                    NodeItemCompact(
                        thisNode = localNode,
                        thatNode = previewNode,
                        distanceUnits = MeasurementSystem.METRIC,
                        showPower = state.showPower,
                        showLastHeard = state.showLastHeard,
                        lastHeardIsRelative = state.lastHeardIsRelative,
                        showLocation = state.showLocation,
                        showHops = state.showHops,
                        showSignal = state.showSignal,
                        showChannel = state.showChannel,
                        showRole = state.showRole,
                        showTelemetry = state.showTelemetry,
                        neighborCount = 4,
                    )
            }
        }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(4.dp))

        // Shared toggle — applies to both layouts
        SwitchPreference(
            title = stringResource(Res.string.node_layout_log_icons),
            checked = state.showTelemetry,
            enabled = true,
            onCheckedChange = onShowTelemetryChange,
        )

        SwitchPreference(
            title = stringResource(Res.string.node_layout_neighbors),
            checked = state.extraNeighbors,
            enabled = true,
            onCheckedChange = onExtraNeighborsChange,
        )

        NodeExtraSettings(
            mode = state.extraMode,
            iconsOnly = state.extraIconsOnly,
            packets = state.extraPackets,
            hardware = state.extraHardware,
            ether = state.extraEther,
            onModeChange = onExtraModeChange,
            onIconsOnlyChange = onExtraIconsOnlyChange,
            onPacketsChange = onExtraPacketsChange,
            onHardwareChange = onExtraHardwareChange,
            onEtherChange = onExtraEtherChange,
        )

        if (state.density == NodeListDensity.COMPLETE) {
            Text(
                text = stringResource(Res.string.node_layout_complete_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            // Compact-specific toggles
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            Text(
                text = stringResource(Res.string.node_layout_compact_fields_header),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                SwitchPreference(
                    title = stringResource(Res.string.node_layout_power),
                    checked = state.showPower,
                    enabled = true,
                    onCheckedChange = onShowPowerChange,
                )
                SwitchPreference(
                    title = stringResource(Res.string.node_layout_last_heard_time),
                    checked = state.showLastHeard,
                    enabled = true,
                    onCheckedChange = onShowLastHeardChange,
                )
                SwitchPreference(
                    title = stringResource(Res.string.node_layout_relative_last_heard),
                    checked = state.lastHeardIsRelative,
                    enabled = state.showLastHeard,
                    onCheckedChange = onLastHeardIsRelativeChange,
                )
                SwitchPreference(
                    title = stringResource(Res.string.node_layout_distance_and_bearing),
                    checked = state.showLocation,
                    enabled = true,
                    onCheckedChange = onShowLocationChange,
                )
                SwitchPreference(
                    title = stringResource(Res.string.node_layout_hops_away),
                    checked = state.showHops,
                    enabled = true,
                    onCheckedChange = onShowHopsChange,
                )
                SwitchPreference(
                    title = stringResource(Res.string.node_layout_signal_direct_only),
                    checked = state.showSignal,
                    enabled = true,
                    onCheckedChange = onShowSignalChange,
                )
                SwitchPreference(
                    title = stringResource(Res.string.node_layout_channel),
                    checked = state.showChannel,
                    enabled = true,
                    onCheckedChange = onShowChannelChange,
                )
                SwitchPreference(
                    title = stringResource(Res.string.node_layout_device_role),
                    checked = state.showRole,
                    enabled = true,
                    onCheckedChange = onShowRoleChange,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Suppress("MagicNumber")
private fun previewSampleExtraStats(): NodeExtraStats = NodeExtraStats(
    packetsTx = 1280,
    packetsRx = 20418,
    packetsRxBad = 311,
    rxDupe = 5302,
    txRelay = 904,
    cpuPercent = 12,
    heapUsedPercent = 59f,
    flashUsedPercent = 63f,
    wifiRssi = -67,
    noiseFloor = -112,
)

@Suppress("MagicNumber")
internal fun previewSampleNode(hopsAway: Int = 1): Node = Node(
    num = 0x1A2B3C4D,
    user =
    User.Builder()
        .also { wb ->
            wb.id = "!1a2b3c4d"
            wb.long_name = "Solar Hilltop"
            wb.short_name = "SoHi"
            wb.hw_model = HardwareModel.TBEAM
            wb.role = Config.DeviceConfig.Role.ROUTER
            wb.public_key = ByteArray(32) { (it * 7).toByte() }.toByteString()
        }
        .build(),
    position =
    Position.Builder()
        .also { wb ->
            wb.latitude_i = 338125110
            wb.longitude_i = -1179189760
            wb.altitude = 138
            wb.sats_in_view = 8
        }
        .build(),
    lastHeard = (nowSeconds - 300).toInt(),
    channel = 1,
    snr = 10.25F,
    rssi = -67,
    deviceMetrics =
    DeviceMetrics.Builder()
        .also { wb ->
            wb.channel_utilization = 3.2F
            wb.air_util_tx = 1.8F
            wb.battery_level = 92
            wb.voltage = 4.1F
            wb.uptime_seconds = 86400
        }
        .build(),
    environmentMetrics =
    EnvironmentMetrics.Builder()
        .also { wb ->
            wb.temperature = 24.5F
            wb.relative_humidity = 45.0F
            wb.barometric_pressure = 1013.25F
            wb.current = -11.5F
        }
        .build(),
    isFavorite = true,
    hopsAway = hopsAway,
)

/** Local device node used as reference point for distance calculation in previews. */
@Suppress("MagicNumber")
internal fun previewLocalNode(): Node = Node(
    num = 0xDEADBEEF.toInt(),
    user =
    User.Builder()
        .also { wb ->
            wb.id = "!deadbeef"
            wb.long_name = "My Radio"
            wb.short_name = "MyRd"
            wb.hw_model = HardwareModel.HELTEC_V3
            wb.role = Config.DeviceConfig.Role.CLIENT
        }
        .build(),
    position =
    Position.Builder()
        .also { wb ->
            wb.latitude_i = 338000000
            wb.longitude_i = -1179000000
            wb.altitude = 50
            wb.sats_in_view = 10
        }
        .build(),
    lastHeard = (nowSeconds - 30).toInt(),
)

/**
 * Extra line of data under each node. The sensors (power, weather) follow the environment metrics switch above; here
 * come the optional statistics (packets, hardware), how the line is shown and "same icons" with a legend.
 */
@Composable
private fun NodeExtraSettings(
    mode: NodeExtraMode,
    iconsOnly: Boolean,
    packets: Boolean,
    hardware: Boolean,
    ether: Boolean,
    onModeChange: (NodeExtraMode) -> Unit,
    onIconsOnlyChange: (Boolean) -> Unit,
    onPacketsChange: (Boolean) -> Unit,
    onHardwareChange: (Boolean) -> Unit,
    onEtherChange: (Boolean) -> Unit,
) {
    var showLegend by remember { mutableStateOf(false) }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    Text(
        text = "Dodatkowe statystyki",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
    Text(
        text = "Dane z ostatniego pakietu węzła. Metryki środowiskowe pokazują napięcie, prąd i pogodę.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    SwitchPreference(
        title = "Pakiety (TX, RX, błędne, duplikaty)",
        checked = packets,
        enabled = true,
        onCheckedChange = onPacketsChange,
    )
    SwitchPreference(
        title = "Eter (zajętość kanału, czas nadawania, szum — w linii ze skokami)",
        checked = ether,
        enabled = true,
        onCheckedChange = onEtherChange,
    )
    SwitchPreference(
        title = "Sprzęt (procesor, pamięć, Wi-Fi)",
        checked = hardware,
        enabled = true,
        onCheckedChange = onHardwareChange,
    )
    Text(
        text = "Sposób pokazywania",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
    val modes = listOf(NodeExtraMode.PAGED to "Strony", NodeExtraMode.ALL to "Wszystko naraz")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        modes.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                onClick = { onModeChange(option) },
                selected = mode == option,
                label = { Text(label) },
            )
        }
    }
    if (mode == NodeExtraMode.PAGED) {
        Text(
            text = "Strony zmieniają się same. Możesz je też przesuwać palcem w lewo i w prawo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            SwitchPreference(
                title = "Same ikony (bez podpisów)",
                checked = iconsOnly,
                enabled = true,
                onCheckedChange = onIconsOnlyChange,
            )
        }
        IconButton(onClick = { showLegend = true }) {
            Icon(imageVector = MeshtasticIcons.Info, contentDescription = "Znaczenie ikon")
        }
    }
    if (showLegend) {
        AlertDialog(
            onDismissRequest = { showLegend = false },
            confirmButton = { TextButton(onClick = { showLegend = false }) { Text("OK") } },
            title = { Text("Znaczenie ikon") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    nodeExtraLegend.forEach { (icon, meaning) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon.invoke(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp).padding(end = 6.dp),
                                )
                            }
                            Text(meaning, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
        )
    }
}
