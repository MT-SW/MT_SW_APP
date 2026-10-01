/*
 * Copyright (c) 2025 MT_SW contributors
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
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_antenna_height
import org.meshtastic.core.resources.planner_antenna_height_title
import org.meshtastic.core.resources.planner_clear_point
import org.meshtastic.core.resources.planner_close
import org.meshtastic.core.resources.planner_coordinates_hint
import org.meshtastic.core.resources.planner_coordinates_title
import org.meshtastic.core.resources.planner_error_latitude
import org.meshtastic.core.resources.planner_error_longitude
import org.meshtastic.core.resources.planner_ground_alt
import org.meshtastic.core.resources.planner_ground_alt_auto
import org.meshtastic.core.resources.planner_ground_alt_from_model
import org.meshtastic.core.resources.planner_ground_alt_manual
import org.meshtastic.core.resources.planner_ground_alt_no_point
import org.meshtastic.core.resources.planner_ground_alt_title
import org.meshtastic.core.resources.planner_ground_alt_waiting
import org.meshtastic.core.resources.planner_latitude
import org.meshtastic.core.resources.planner_longitude
import org.meshtastic.core.resources.planner_node_hops
import org.meshtastic.core.resources.planner_node_none
import org.meshtastic.core.resources.planner_node_ours
import org.meshtastic.core.resources.planner_node_picker_title
import org.meshtastic.core.resources.planner_node_rssi
import org.meshtastic.core.resources.planner_node_search
import org.meshtastic.core.resources.planner_node_snr
import org.meshtastic.core.resources.planner_point_name
import org.meshtastic.core.resources.planner_section_point
import org.meshtastic.core.resources.planner_source_label
import org.meshtastic.core.resources.planner_source_manual
import org.meshtastic.core.resources.planner_source_map
import org.meshtastic.core.resources.planner_source_map_center
import org.meshtastic.core.resources.planner_source_node
import org.meshtastic.core.resources.planner_source_none
import org.meshtastic.core.resources.planner_source_pick_node
import org.meshtastic.core.resources.planner_source_station
import org.meshtastic.core.resources.planner_source_station_used
import org.meshtastic.core.resources.planner_source_tap_map
import org.meshtastic.core.resources.planner_unit_m
import org.meshtastic.core.resources.planner_unit_m_asl
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.Map
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.MyLocation
import org.meshtastic.core.ui.icon.Nodes
import org.meshtastic.core.ui.icon.PinDrop
import org.meshtastic.core.ui.icon.Search
import org.meshtastic.feature.map.planner.state.PlannerEnd
import org.meshtastic.feature.map.planner.state.PlannerNodeOption
import org.meshtastic.feature.map.planner.state.PlannerPointSource
import org.meshtastic.feature.map.planner.state.PlannerSide
import org.meshtastic.feature.map.planner.state.PlannerUiState
import org.meshtastic.feature.map.planner.state.PlannerViewModel

private const val COORD_DECIMALS = 6
private const val MIN_GROUND_M = -500.0
private const val MAX_GROUND_M = 9000.0
private const val MAX_HEIGHT_M = 2000.0
private val NODE_LIST_MAX_HEIGHT = 360.dp

/** Point of the selected side: source, coordinates, ground altitude and antenna height. */
@Composable
internal fun PointSection(
    state: PlannerUiState,
    nodes: List<PlannerNodeOption>,
    vm: PlannerViewModel,
    bridge: PlannerMapBridge,
) {
    val side = state.selectedSide
    val end = state.end(side)
    var showPicker by remember { mutableStateOf(false) }
    PlannerCard(
        title = stringResource(Res.string.planner_section_point, sideName(side)),
        info = PlannerInfoTopic.POINTS,
    ) {
        key(side) {
            PlannerTextField(
                label = stringResource(Res.string.planner_point_name),
                value = end.name,
                onValue = { vm.setName(side, it) },
            )
            PointSourceLine(end = end, onClear = { vm.clearPoint(side) })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceChip(MeshtasticIcons.Nodes, stringResource(Res.string.planner_source_pick_node)) { showPicker = true }
                SourceChip(MeshtasticIcons.PinDrop, stringResource(Res.string.planner_source_tap_map)) {
                    bridge.pickingSide = side
                }
                SourceChip(MeshtasticIcons.MyLocation, stringResource(Res.string.planner_source_station)) {
                    vm.useStation(side)
                }
                SourceChip(MeshtasticIcons.Map, stringResource(Res.string.planner_source_map_center)) {
                    bridge.mapCenter()?.let { (lat, lon) -> vm.setPointFromMap(side, lat, lon) }
                }
            }
            CoordinateFields(side = side, end = end, vm = vm)
            state.hintsBySide[side].orEmpty().forEach { hint ->
                PlannerNotice(text = hintText(side, hint), isError = false)
            }
            GroundAltitudeFields(side = side, end = end, vm = vm)
            PlannerSubheading(
                text = stringResource(Res.string.planner_antenna_height_title),
                info = PlannerInfoTopic.ANTENNA_HEIGHT,
            )
            PlannerNumberField(
                label = stringResource(Res.string.planner_antenna_height),
                value = end.antennaHeightM,
                onValue = { vm.setAntennaHeight(side, it) },
                decimals = 1,
                suffix = stringResource(Res.string.planner_unit_m),
                min = 0.0,
                max = MAX_HEIGHT_M,
            )
        }
    }
    if (showPicker) {
        NodePickerDialog(
            nodes = nodes,
            selectedNum = (end.pointSource as? PlannerPointSource.Node)?.num,
            onPick = { num ->
                vm.setPointFromNode(side, num)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun SourceChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
        label = { Text(label) },
    )
}

@Composable
private fun PointSourceLine(end: PlannerEnd, onClear: () -> Unit) {
    val sourceText =
        stringResource(
            when (end.pointSource) {
                PlannerPointSource.None -> Res.string.planner_source_none
                is PlannerPointSource.Node -> Res.string.planner_source_node
                PlannerPointSource.Map -> Res.string.planner_source_map
                PlannerPointSource.Manual -> Res.string.planner_source_manual
                PlannerPointSource.Station -> Res.string.planner_source_station_used
            },
        )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.planner_source_label, sourceText),
                style = MaterialTheme.typography.bodyMedium,
            )
            val lat = end.lat
            val lon = end.lon
            if (lat != null && lon != null) {
                Text(
                    text = PlannerInput.formatCoordinates(lat, lon),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (end.isComplete) {
            IconButton(onClick = onClear) {
                Icon(MeshtasticIcons.Close, contentDescription = stringResource(Res.string.planner_clear_point))
            }
        }
    }
}

@Composable
private fun CoordinateFields(side: PlannerSide, end: PlannerEnd, vm: PlannerViewModel) {
    var latText by remember { mutableStateOf(end.lat?.let { PlannerInput.formatField(it, COORD_DECIMALS) } ?: "") }
    var lonText by remember { mutableStateOf(end.lon?.let { PlannerInput.formatField(it, COORD_DECIMALS) } ?: "") }
    LaunchedEffect(end.lat, end.lon) {
        val lat = end.lat
        val lon = end.lon
        if (lat == null || lon == null) {
            latText = ""
            lonText = ""
        } else {
            val pLat = PlannerInput.parseCoordinate(latText, true)
            val pLon = PlannerInput.parseCoordinate(lonText, false)
            if (pLat == null || !PlannerInput.sameValue(pLat, lat)) latText = PlannerInput.formatField(lat, COORD_DECIMALS)
            if (pLon == null || !PlannerInput.sameValue(pLon, lon)) lonText = PlannerInput.formatField(lon, COORD_DECIMALS)
        }
    }

    fun commit(latInput: String, lonInput: String) {
        val la = PlannerInput.parseCoordinate(latInput, true)
        val lo = PlannerInput.parseCoordinate(lonInput, false)
        if (la != null && lo != null) vm.setPointManual(side, la, lo)
    }

    val latInvalid = latText.isNotBlank() && PlannerInput.parseCoordinate(latText, true) == null
    val lonInvalid = lonText.isNotBlank() && PlannerInput.parseCoordinate(lonText, false) == null
    val lonError = if (lonInvalid) stringResource(Res.string.planner_error_longitude) else null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlannerSubheading(text = stringResource(Res.string.planner_coordinates_title))
        OutlinedTextField(
            value = latText,
            onValueChange = { t ->
                val pair = if (t.contains(';') || t.trim().contains(' ')) PlannerInput.parseLatLonPair(t) else null
                if (pair != null) {
                    latText = PlannerInput.formatField(pair.first, COORD_DECIMALS)
                    lonText = PlannerInput.formatField(pair.second, COORD_DECIMALS)
                    vm.setPointManual(side, pair.first, pair.second)
                } else {
                    latText = t
                    commit(t, lonText)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.planner_latitude)) },
            singleLine = true,
            isError = latInvalid,
            supportingText = {
                Text(
                    if (latInvalid) {
                        stringResource(Res.string.planner_error_latitude)
                    } else {
                        stringResource(Res.string.planner_coordinates_hint)
                    },
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        OutlinedTextField(
            value = lonText,
            onValueChange = { t ->
                lonText = t
                commit(latText, t)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.planner_longitude)) },
            singleLine = true,
            isError = lonInvalid,
            supportingText = lonError?.let { e -> { Text(e) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
    }
}

@Composable
private fun GroundAltitudeFields(side: PlannerSide, end: PlannerEnd, vm: PlannerViewModel) {
    PlannerSubheading(text = stringResource(Res.string.planner_ground_alt_title), info = PlannerInfoTopic.GROUND_ALT)
    val supporting =
        when {
            !end.isComplete -> stringResource(Res.string.planner_ground_alt_no_point)
            end.groundAltManual -> stringResource(Res.string.planner_ground_alt_manual)
            end.groundAltM != null -> stringResource(Res.string.planner_ground_alt_from_model)
            else -> stringResource(Res.string.planner_ground_alt_waiting)
        }
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PlannerNumberField(
            label = stringResource(Res.string.planner_ground_alt),
            value = end.groundAltM,
            onValue = { vm.setGroundAlt(side, it) },
            onBlank = { vm.setGroundAlt(side, null) },
            modifier = Modifier.weight(1f),
            decimals = 1,
            suffix = stringResource(Res.string.planner_unit_m_asl),
            min = MIN_GROUND_M,
            max = MAX_GROUND_M,
            supporting = supporting,
            enabled = end.isComplete,
        )
        FilterChip(
            selected = !end.groundAltManual,
            onClick = { vm.setGroundAlt(side, null) },
            enabled = end.isComplete,
            label = { Text(stringResource(Res.string.planner_ground_alt_auto)) },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun NodePickerDialog(
    nodes: List<PlannerNodeOption>,
    selectedNum: Int?,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(nodes, query) {
            val q = query.trim()
            nodes
                .filter { q.isEmpty() || it.displayName.contains(q, ignoreCase = true) || it.shortName.contains(q, ignoreCase = true) }
                .sortedWith(
                    compareByDescending<PlannerNodeOption> { it.isOurs }
                        .thenBy { it.hopsAway }
                        .thenBy { it.displayName.lowercase() },
                )
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.planner_node_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.planner_node_search)) },
                    leadingIcon = { Icon(MeshtasticIcons.Search, contentDescription = null) },
                )
                if (filtered.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.planner_node_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = NODE_LIST_MAX_HEIGHT)) {
                        items(filtered, key = { it.num }) { node ->
                            NodeRow(node = node, selected = node.num == selectedNum, onClick = { onPick(node.num) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.planner_close)) } },
    )
}

@Composable
private fun NodeRow(node: PlannerNodeOption, selected: Boolean, onClick: () -> Unit) {
    val ours = stringResource(Res.string.planner_node_ours)
    val hops = stringResource(Res.string.planner_node_hops, node.hopsAway.toString())
    val snr = node.snrDb?.let { stringResource(Res.string.planner_node_snr, fmt(it.toDouble(), 1)) }
    val rssi = node.rssiDbm?.let { stringResource(Res.string.planner_node_rssi, it.toString()) }
    val detail = listOfNotNull(hops, snr, rssi).joinToString(" · ")
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = node.displayName + if (node.isOurs) " ($ours)" else "",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else null,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
