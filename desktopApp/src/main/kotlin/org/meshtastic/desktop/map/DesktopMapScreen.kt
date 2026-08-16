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
package org.meshtastic.desktop.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.last_heard_filter_label
import org.meshtastic.core.resources.manage_map_layers
import org.meshtastic.core.resources.map
import org.meshtastic.core.resources.only_favorites
import org.meshtastic.core.resources.show_precision_circle
import org.meshtastic.core.resources.show_waypoints
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.Add
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.Favorite
import org.meshtastic.core.ui.icon.Layers
import org.meshtastic.core.ui.icon.Lens
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PinDrop
import org.meshtastic.feature.map.BaseMapViewModel.MapFilterState
import org.meshtastic.feature.map.LastHeardFilter
import java.awt.FileDialog
import java.io.File
import kotlin.math.roundToInt

private val LAYER_FILE_EXTENSIONS = setOf("geojson", "json", "kml")

/**
 * Desktop's map main screen — mirrors the common Android `MapScreen.kt` (top bar + node chip) but calls
 * [DesktopMapViewProvider] directly instead of going through `LocalMapViewProvider`, since desktop has only one map
 * implementation (no google/fdroid flavor split to route between).
 *
 * NOTE (recovery script): "No layers imported yet." / "Add Layer" / "Close" in MapLayersDialog are hardcoded English
 * (not yet wired to Res.string.* resources) — same follow-up pattern noted for the waypoint dialog. The filter button
 * still reuses MeshtasticIcons.Favorite as its icon (MeshtasticIcons.FilterAlt in Actions.kt would fit better but was
 * never swapped in — cosmetic, optional).
 */
@Composable
fun DesktopMapScreen(
    onClickNodeChip: (Int) -> Unit,
    navigateToNodeDetails: (Int) -> Unit,
    waypointId: Int?,
    sitePlannerNodeNum: Int? = null,
) {
    val viewModel: DesktopMapViewModel = koinViewModel()
    val layerManager = koinInject<DesktopMapLayerManager>()
    val ourNodeInfo by viewModel.ourNodeInfo.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val mapFilterState by viewModel.mapFilterStateFlow.collectAsState()
    val mapLayers by layerManager.layers.collectAsState()
    val mapViewProvider = remember { DesktopMapViewProvider() }
    var filterExpanded by remember { mutableStateOf(false) }
    var showLayersDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.map),
                ourNode = ourNodeInfo,
                showNodeChip = ourNodeInfo != null && isConnected,
                canNavigateUp = false,
                onNavigateUp = {},
                actions = {
                    IconButton(onClick = { showLayersDialog = true }) {
                        Icon(
                            imageVector = MeshtasticIcons.Layers,
                            contentDescription = stringResource(Res.string.manage_map_layers),
                        )
                    }
                    IconButton(onClick = { filterExpanded = true }) {
                        Icon(
                            imageVector = MeshtasticIcons.Favorite,
                            contentDescription = stringResource(Res.string.map),
                        )
                    }
                    DesktopMapFilterDropdown(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false },
                        mapFilterState = mapFilterState,
                        onToggleOnlyFavorites = viewModel::toggleOnlyFavorites,
                        onToggleShowWaypoints = viewModel::toggleShowWaypointsOnMap,
                        onToggleShowPrecisionCircle = viewModel::toggleShowPrecisionCircleOnMap,
                        onSetLastHeardFilter = viewModel::setLastHeardFilter,
                    )
                },
                onClickChip = { onClickNodeChip(it.num) },
            )
        },
    ) { paddingValues ->
        mapViewProvider.MapView(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            navigateToNodeDetails = navigateToNodeDetails,
            waypointId = waypointId,
            sitePlannerNodeNum = sitePlannerNodeNum,
        )
    }

    if (showLayersDialog) {
        MapLayersDialog(
            layers = mapLayers,
            onAddLayer = { pickMapLayerFile()?.let { layerManager.addLayer(it) } },
            onToggleVisibility = layerManager::toggleVisibility,
            onRemoveLayer = layerManager::removeLayer,
            onDismiss = { showLayersDialog = false },
        )
    }
}

/**
 * Opens a native AWT file picker restricted to GeoJSON/KML files. Blocks the calling thread until the user closes the
 * dialog — the standard, accepted approach for a modal file picker in a Compose Desktop app (AWT `FileDialog` is
 * inherently modal; Compose Desktop's UI thread is the AWT event thread, same as a plain Swing app).
 */
private fun pickMapLayerFile(): File? {
    val dialog = FileDialog(null as java.awt.Frame?, "Import Map Layer", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in LAYER_FILE_EXTENSIONS }
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(dir, file)
}

/** Layer list dialog: visibility checkbox + delete per layer, plus an "Add Layer" file-picker button. */
@Composable
private fun MapLayersDialog(
    layers: List<DesktopMapLayer>,
    onAddLayer: () -> Unit,
    onToggleVisibility: (String) -> Unit,
    onRemoveLayer: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.manage_map_layers)) },
        text = {
            Column {
                if (layers.isEmpty()) {
                    Text("No layers imported yet.")
                } else {
                    layers.forEach { layer ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = layer.isVisible, onCheckedChange = { onToggleVisibility(layer.id) })
                            Text(layer.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemoveLayer(layer.id) }) {
                                Icon(imageVector = MeshtasticIcons.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAddLayer) {
                Icon(
                    imageVector = MeshtasticIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text("Add Layer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/**
 * Desktop map filter dropdown — trimmed port of the Android fork's `FdroidMainMapFilterDropdown`: favorites-only,
 * show-waypoints, show-precision-circle toggles, plus the last-heard filter slider. Skips the Android version's
 * `ExperimentalMaterial3ExpressiveApi` grouped-shapes styling in favor of a plain `DropdownMenu`.
 */
@Composable
private fun DesktopMapFilterDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    mapFilterState: MapFilterState,
    onToggleOnlyFavorites: () -> Unit,
    onToggleShowWaypoints: () -> Unit,
    onToggleShowPrecisionCircle: () -> Unit,
    onSetLastHeardFilter: (LastHeardFilter) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            text = {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MeshtasticIcons.Favorite,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(text = stringResource(Res.string.only_favorites), modifier = Modifier.weight(1f))
                    Checkbox(checked = mapFilterState.onlyFavorites, onCheckedChange = { onToggleOnlyFavorites() })
                }
            },
            onClick = onToggleOnlyFavorites,
        )
        DropdownMenuItem(
            text = {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MeshtasticIcons.PinDrop,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(text = stringResource(Res.string.show_waypoints), modifier = Modifier.weight(1f))
                    Checkbox(checked = mapFilterState.showWaypoints, onCheckedChange = { onToggleShowWaypoints() })
                }
            },
            onClick = onToggleShowWaypoints,
        )
        DropdownMenuItem(
            text = {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MeshtasticIcons.Lens,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(text = stringResource(Res.string.show_precision_circle), modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = mapFilterState.showPrecisionCircle,
                        onCheckedChange = { onToggleShowPrecisionCircle() },
                    )
                }
            },
            onClick = onToggleShowPrecisionCircle,
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            val filterOptions = LastHeardFilter.entries
            val selectedIndex = filterOptions.indexOf(mapFilterState.lastHeardFilter)
            var sliderPosition by remember(selectedIndex) { mutableFloatStateOf(selectedIndex.toFloat()) }
            Text(
                text =
                stringResource(
                    Res.string.last_heard_filter_label,
                    stringResource(mapFilterState.lastHeardFilter.label),
                ),
                style = MaterialTheme.typography.labelLarge,
            )
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = {
                    val newIndex = sliderPosition.roundToInt().coerceIn(0, filterOptions.size - 1)
                    onSetLastHeardFilter(filterOptions[newIndex])
                },
                valueRange = 0f..(filterOptions.size - 1).toFloat(),
                steps = filterOptions.size - 2,
            )
        }
    }
}
