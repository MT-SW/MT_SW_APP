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
package org.meshtastic.feature.node.metrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.model.hasFix
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.clear
import org.meshtastic.core.resources.clear_position_track_message
import org.meshtastic.core.resources.clear_position_track_title
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.export_gpx
import org.meshtastic.core.resources.mesh_hud_open
import org.meshtastic.core.resources.mt_sw_other_compare_body
import org.meshtastic.core.resources.mt_sw_other_coverage_body
import org.meshtastic.core.resources.position_log
import org.meshtastic.core.resources.track_compare
import org.meshtastic.core.resources.track_compare_empty
import org.meshtastic.core.resources.track_compare_off
import org.meshtastic.core.resources.track_compare_summary
import org.meshtastic.core.resources.track_compare_title
import org.meshtastic.core.resources.track_export_kml
import org.meshtastic.core.resources.track_info_title
import org.meshtastic.core.resources.track_legend_compare
import org.meshtastic.core.resources.track_legend_gpx
import org.meshtastic.core.resources.track_legend_hide
import org.meshtastic.core.resources.track_legend_hud
import org.meshtastic.core.resources.track_legend_kml
import org.meshtastic.core.resources.track_legend_show
import org.meshtastic.core.resources.track_legend_snr
import org.meshtastic.core.resources.track_legend_title
import org.meshtastic.core.resources.track_snr_colors
import org.meshtastic.core.ui.component.InfoButton
import org.meshtastic.core.ui.component.MeshtasticResourceDialog
import org.meshtastic.core.ui.icon.CenterFocusStrong
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.FileDownload
import org.meshtastic.core.ui.icon.Layers
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Refresh
import org.meshtastic.core.ui.icon.Route
import org.meshtastic.core.ui.icon.Snr
import org.meshtastic.core.ui.util.LocalNodeTrackMapProvider
import org.meshtastic.core.ui.util.LocalTrackOverlay
import org.meshtastic.core.ui.util.TrackCompare
import org.meshtastic.core.ui.util.TrackOverlay
import org.meshtastic.core.ui.util.rememberSaveFileLauncher
import org.meshtastic.proto.Position

@Composable
fun PositionLogScreen(
    viewModel: MetricsViewModel,
    onNavigateUp: () -> Unit,
    /** Null hides the mesh view button, for platforms with no camera. */
    onOpenMeshHud: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val positions = state.positionLogs

    val exportPositionLauncher = rememberSaveFileLauncher { uri -> viewModel.savePositionCSV(uri, positions) }
    val nodeName = state.node?.effectiveLongName.orEmpty()
    val gpxTrackName = gpxTrackName(state.node)
    val exportGpxLauncher = rememberSaveFileLauncher { uri -> viewModel.savePositionGpx(uri, positions, gpxTrackName) }
    val hasTrackPoint = remember(positions) { positions.any { it.hasFix() } }

    // Coverage view: the track coloured by the SNR it was heard with, saved as KML, and a second track beside it.
    val snrByTime = state.positionSnr
    var colorBySnr by rememberSaveable { mutableStateOf(false) }
    val exportKmlLauncher =
        rememberSaveFileLauncher { uri -> viewModel.savePositionKml(uri, positions, snrByTime, gpxTrackName) }
    val compare by viewModel.trackCompare.collectAsStateWithLifecycle()
    val compareCandidates by viewModel.compareCandidates.collectAsStateWithLifecycle()
    var showCompareDialog by rememberSaveable { mutableStateOf(false) }

    val trackMap = LocalNodeTrackMapProvider.current
    val destNum = state.node?.num ?: 0

    CompositionLocalProvider(
        LocalTrackOverlay provides
            TrackOverlay(colorBySnr = colorBySnr && snrByTime.isNotEmpty(), snrByTime = snrByTime, compare = compare),
    ) {
        BaseMetricScreen(
        isLoading = isLoading,
        onNavigateUp = onNavigateUp,
        telemetryType = null,
        titleRes = Res.string.position_log,
        nodeName = nodeName,
        data = positions,
        timeProvider = { it.time.toDouble() },
        onExportCsv = { exportPositionLauncher("position.csv", "text/csv") },
        extraActions = {
            if (hasTrackPoint && onOpenMeshHud != null) {
                IconButton(onClick = onOpenMeshHud) {
                    Icon(
                        imageVector = MeshtasticIcons.CenterFocusStrong,
                        contentDescription = stringResource(Res.string.mesh_hud_open),
                    )
                }
            }
            if (hasTrackPoint) {
                InfoButton(
                    Res.string.track_info_title,
                    Res.string.mt_sw_other_coverage_body,
                    Res.string.mt_sw_other_compare_body,
                )
            }
            if (snrByTime.isNotEmpty()) {
                IconButton(onClick = { colorBySnr = !colorBySnr }) {
                    Icon(
                        imageVector = MeshtasticIcons.Snr,
                        contentDescription = stringResource(Res.string.track_snr_colors),
                        tint = if (colorBySnr) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    )
                }
                IconButton(onClick = { exportKmlLauncher("coverage.kml", "application/vnd.google-earth.kml+xml") }) {
                    Icon(
                        imageVector = MeshtasticIcons.Layers,
                        contentDescription = stringResource(Res.string.track_export_kml),
                    )
                }
            }
            if (hasTrackPoint) {
                IconButton(onClick = { showCompareDialog = true }) {
                    Icon(
                        imageVector = MeshtasticIcons.Route,
                        contentDescription = stringResource(Res.string.track_compare),
                        tint = if (compare != null) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    )
                }
            }
            if (hasTrackPoint) {
                IconButton(onClick = { exportGpxLauncher("track.gpx", "application/gpx+xml") }) {
                    Icon(
                        imageVector = MeshtasticIcons.FileDownload,
                        contentDescription = stringResource(Res.string.export_gpx),
                    )
                }
            }
            if (positions.isNotEmpty()) {
                ClearPositionTrackButton(onConfirm = { viewModel.clearPosition() })
            }
            if (!state.isLocal) {
                IconButton(onClick = { viewModel.requestPosition() }) {
                    Icon(imageVector = MeshtasticIcons.Refresh, contentDescription = null)
                }
            }
        },
        chartPart = { modifier, selectedX, _, onPointSelected ->
            val selectedTime = selectedX?.toInt()
            // Positional: trackMap is a function type, so it takes no named arguments. Collapsed credit, this being a
            // strip inside a screen whose own map is a tab away.
            trackMap(destNum, positions, modifier, selectedTime, { time -> onPointSelected(time.toDouble()) }, false)
        },
        listPart = { modifier, selectedX, lazyListState, onCardClick ->
            LazyColumn(modifier = modifier.fillMaxSize(), state = lazyListState) {
                if (hasTrackPoint) {
                    item(key = "icon_legend", contentType = "icon_legend") {
                        TrackIconLegend(
                            showHud = onOpenMeshHud != null,
                            showSnr = snrByTime.isNotEmpty(),
                        )
                    }
                }
                itemsIndexed(
                    positions,
                    key = { index, position -> "${position.time}_$index" },
                    contentType = { _, _ -> "position_log" },
                ) { _, position ->
                    PositionCard(
                        position = position,
                        displayUnits = state.displayUnits,
                        isSelected = position.time.toDouble() == selectedX,
                        onClick = { onCardClick(position.time.toDouble()) },
                    )
                }
            }
        },
        )
    }

    if (showCompareDialog) {
        TrackCompareDialog(
            candidates = compareCandidates,
            ownName = nodeName,
            ownPositions = positions,
            compare = compare,
            onSelect = {
                viewModel.setCompareNode(it)
                showCompareDialog = false
            },
            onDismiss = { showCompareDialog = false },
        )
    }
}

/**
 * A plain description of the icons above the track: what each one does and what it is for. The icons only have a
 * contentDescription, which nobody sees, so this card says it in words; it can be folded away.
 */
@Composable
private fun TrackIconLegend(showHud: Boolean, showSnr: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.track_legend_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        stringResource(if (expanded) Res.string.track_legend_hide else Res.string.track_legend_show),
                    )
                }
            }
            if (expanded) {
                if (showHud) LegendRow(MeshtasticIcons.CenterFocusStrong, Res.string.track_legend_hud)
                if (showSnr) {
                    LegendRow(MeshtasticIcons.Snr, Res.string.track_legend_snr)
                    LegendRow(MeshtasticIcons.Layers, Res.string.track_legend_kml)
                }
                LegendRow(MeshtasticIcons.Route, Res.string.track_legend_compare)
                LegendRow(MeshtasticIcons.FileDownload, Res.string.track_legend_gpx)
            }
        }
    }
}

@Composable
private fun LegendRow(icon: ImageVector, text: StringResource) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = stringResource(text), style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Picks the node whose track is drawn beside this one. While a comparison is on it also gives the two tracks' length
 * and number of fixes, so the routes can be told apart by more than colour.
 */
@Composable
private fun TrackCompareDialog(
    candidates: List<Node>,
    ownName: String,
    ownPositions: List<Position>,
    compare: TrackCompare?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.track_compare_title)) },
        text = {
            Column {
                if (compare != null) {
                    Text(trackSummary(ownName, ownPositions))
                    Text(trackSummary(compare.label, compare.positions))
                }
                if (candidates.isEmpty()) {
                    Text(stringResource(Res.string.track_compare_empty))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = COMPARE_LIST_MAX_HEIGHT.dp)) {
                        items(candidates, key = { it.num }) { node ->
                            ListItem(
                                headlineContent = { Text(node.effectiveLongName) },
                                modifier = Modifier.clickable { onSelect(node.num) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } },
        dismissButton =
        if (compare != null) {
            { TextButton(onClick = { onSelect(null) }) { Text(stringResource(Res.string.track_compare_off)) } }
        } else {
            null
        },
    )
}

@Composable
private fun trackSummary(name: String, positions: List<Position>): String {
    val km = NumberFormatter.format(trackLengthMeters(positions) / METERS_PER_KM, 2)
    return stringResource(Res.string.track_compare_summary, name, positions.count { it.hasFix() }, "$km km")
}

private const val COMPARE_LIST_MAX_HEIGHT = 320
private const val METERS_PER_KM = 1000.0

/** Trash-can action that asks for confirmation before invoking [onConfirm] to delete the position track. */
@Composable
private fun ClearPositionTrackButton(onConfirm: () -> Unit) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { showDialog = true }) {
        Icon(imageVector = MeshtasticIcons.Delete, contentDescription = stringResource(Res.string.clear))
    }
    if (showDialog) {
        MeshtasticResourceDialog(
            titleRes = Res.string.clear_position_track_title,
            messageRes = Res.string.clear_position_track_message,
            confirmTextRes = Res.string.delete,
            onConfirm = {
                showDialog = false
                onConfirm()
            },
            onDismiss = { showDialog = false },
        )
    }
}
