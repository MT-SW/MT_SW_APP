/*
 * Copyright (c) 2025-2026 Meshtastic LLC
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
@file:Suppress(
    "LongMethod",
    "MagicNumber",
    "MutableStateAutoboxing",
    "ParameterNaming",
    "ReturnCount",
    "TooManyFunctions",
)

package org.meshtastic.feature.map.horizon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.latitude
import org.meshtastic.core.resources.longitude
import org.meshtastic.core.resources.mesh_hud_cardinal_east
import org.meshtastic.core.resources.mesh_hud_cardinal_north
import org.meshtastic.core.resources.mesh_hud_cardinal_south
import org.meshtastic.core.resources.mesh_hud_cardinal_west
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.resources.retry
import org.meshtastic.core.resources.terrain_horizon_antenna
import org.meshtastic.core.resources.terrain_horizon_antenna_auto
import org.meshtastic.core.resources.terrain_horizon_apply
import org.meshtastic.core.resources.terrain_horizon_camera
import org.meshtastic.core.resources.terrain_horizon_compass
import org.meshtastic.core.resources.terrain_horizon_favorites
import org.meshtastic.core.resources.terrain_horizon_fov
import org.meshtastic.core.resources.terrain_horizon_from
import org.meshtastic.core.resources.terrain_horizon_hint
import org.meshtastic.core.resources.terrain_horizon_in_sight
import org.meshtastic.core.resources.terrain_horizon_info_body
import org.meshtastic.core.resources.terrain_horizon_info_title
import org.meshtastic.core.resources.terrain_horizon_labels
import org.meshtastic.core.resources.terrain_horizon_loading
import org.meshtastic.core.resources.terrain_horizon_move
import org.meshtastic.core.resources.terrain_horizon_move_back
import org.meshtastic.core.resources.terrain_horizon_move_hint
import org.meshtastic.core.resources.terrain_horizon_network
import org.meshtastic.core.resources.terrain_horizon_no_position
import org.meshtastic.core.resources.terrain_horizon_node_range
import org.meshtastic.core.resources.terrain_horizon_none
import org.meshtastic.core.resources.terrain_horizon_observer_my_node
import org.meshtastic.core.resources.terrain_horizon_observer_node
import org.meshtastic.core.resources.terrain_horizon_observer_phone
import org.meshtastic.core.resources.terrain_horizon_observer_point
import org.meshtastic.core.resources.terrain_horizon_observer_title
import org.meshtastic.core.resources.terrain_horizon_offline
import org.meshtastic.core.resources.terrain_horizon_offline_hint
import org.meshtastic.core.resources.terrain_horizon_only_visible
import org.meshtastic.core.resources.terrain_horizon_options
import org.meshtastic.core.resources.terrain_horizon_precision
import org.meshtastic.core.resources.terrain_horizon_range
import org.meshtastic.core.resources.terrain_horizon_refraction
import org.meshtastic.core.resources.terrain_horizon_save
import org.meshtastic.core.resources.terrain_horizon_save_clear
import org.meshtastic.core.resources.terrain_horizon_save_failed
import org.meshtastic.core.resources.terrain_horizon_save_range
import org.meshtastic.core.resources.terrain_horizon_saved
import org.meshtastic.core.resources.terrain_horizon_saving
import org.meshtastic.core.resources.terrain_horizon_target_antenna
import org.meshtastic.core.resources.terrain_horizon_title
import org.meshtastic.core.ui.icon.ArrowBack
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PhotoCamera
import org.meshtastic.core.ui.icon.Settings
import org.meshtastic.core.ui.util.HorizonMapNode
import org.meshtastic.core.ui.util.LocalHorizonPickMapProvider
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val Visible = Color(0xFF66BB6A)
private val Hidden = Color(0xFFEF5350)
private val BandShares = listOf(0.5f, 0.38f, 0.26f, 0.15f)
private val BandEdges = listOf(0.0, 0.2, 0.45, 0.75, 1.0001)
private val FIELDS_OF_VIEW = listOf(60, 120, 240, 360)

private const val DEFAULT_FIELD_OF_VIEW = 120
private const val FULL_TURN = 360f
private const val MIN_ANGLE_FLOOR = -15.0
private const val MAX_ANGLE_CEIL = 30.0
private const val TAP_RADIUS_PX = 48f
private const val COLUMN_STEP_PX = 3
private const val MAX_LABELS = 40
private const val LIST_HEIGHT_DP = 220

/**
 * The terrain panorama: the skyline all round with the mesh's nodes placed against it, green where a node stands above
 * the ridge in front of it and red where a ridge hides it. It is the first view of the mesh view; the camera is the
 * second ([onOpenCamera]). It is looked from the connected radio (with its height) unless another place is picked.
 * [focusNodeNum] limits it to one node.
 */
@Composable
fun TerrainHorizonScreen(
    focusNodeNum: Int?,
    onNavigateUp: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenNode: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TerrainHorizonViewModel = koinViewModel(),
) {
    LaunchedEffect(focusNodeNum) { viewModel.open(focusNodeNum) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var fieldOfView by rememberSaveable { mutableStateOf(DEFAULT_FIELD_OF_VIEW) }
    var onlyVisible by rememberSaveable { mutableStateOf(false) }
    var names by rememberSaveable { mutableStateOf(true) }
    var moreOptions by rememberSaveable { mutableStateOf(false) }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var compass by rememberSaveable { mutableStateOf(true) }
    var moving by rememberSaveable { mutableStateOf(false) }
    val heading by (if (compass) viewModel.heading else emptyFlow()).collectAsStateWithLifecycle(initialValue = null)
    if (picking) {
        ObserverDialog(
            nodes = state.nodeChoices,
            onPick = {
                picking = false
                viewModel.setObserver(it)
            },
            onDismiss = { picking = false },
        )
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(Res.string.terrain_horizon_info_title)) },
            text = {
                Text(
                    stringResource(Res.string.terrain_horizon_info_body),
                    fontSize = 13.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text(stringResource(Res.string.close)) } },
        )
    }
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    MeshtasticIcons.ArrowBack,
                    stringResource(Res.string.navigate_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                stringResource(Res.string.terrain_horizon_title),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showInfo = true }) {
                Icon(
                    MeshtasticIcons.Info,
                    stringResource(Res.string.terrain_horizon_info_title),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(onClick = { moreOptions = !moreOptions }) {
                Icon(
                    MeshtasticIcons.Settings,
                    stringResource(Res.string.terrain_horizon_options),
                    tint =
                    if (moreOptions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(onClick = onOpenCamera) {
                Icon(
                    MeshtasticIcons.PhotoCamera,
                    stringResource(Res.string.terrain_horizon_camera),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = { picking = true },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                label = { Text(observerLabel(state)) },
                modifier = Modifier.weight(1f, fill = false),
            )
            BrandChip(
                selected = compass,
                onClick = { compass = !compass },
                label = { Text(stringResource(Res.string.terrain_horizon_compass)) },
            )
            BrandChip(
                selected = moving,
                onClick = { moving = !moving },
                label = { Text(stringResource(Res.string.terrain_horizon_move)) },
            )
        }
        ChipRow(stringResource(Res.string.terrain_horizon_range)) {
            HORIZON_RADII_KM.forEach { km ->
                BrandChip(
                    selected = state.radiusKm == km,
                    onClick = { viewModel.setRadius(km) },
                    label = { Text("$km km") },
                )
            }
        }
        ChipRow(stringResource(Res.string.terrain_horizon_node_range)) {
            HORIZON_NODE_RANGES_KM.forEach { km ->
                BrandChip(
                    selected = state.nodeRangeKm == km,
                    onClick = { viewModel.setNodeRange(km) },
                    label = { Text("$km km") },
                )
            }
        }
        ChipRow(stringResource(Res.string.terrain_horizon_antenna)) {
            state.autoAntennaM?.let { auto ->
                BrandChip(
                    selected = state.antennaAuto,
                    onClick = viewModel::setAntennaAuto,
                    label = { Text(stringResource(Res.string.terrain_horizon_antenna_auto) + " ($auto m)") },
                )
            }
            HORIZON_ANTENNAS_M.forEach { m ->
                BrandChip(
                    selected = !(state.antennaAuto && state.autoAntennaM != null) && state.antennaM == m,
                    onClick = { viewModel.setAntenna(m) },
                    label = { Text("$m m") },
                )
            }
        }
        if (moreOptions) {
            ChipRow(stringResource(Res.string.terrain_horizon_target_antenna)) {
                HORIZON_ANTENNAS_M.forEach { m ->
                    BrandChip(
                        selected = state.targetAntennaM == m,
                        onClick = { viewModel.setTargetAntenna(m) },
                        label = { Text("$m m") },
                    )
                }
            }
            ChipRow(stringResource(Res.string.terrain_horizon_refraction)) {
                HORIZON_REFRACTIONS.forEach { k ->
                    BrandChip(
                        selected = state.refraction == k,
                        onClick = { viewModel.setRefraction(k) },
                        label = { Text(if (k == HorizonPanorama.DEFAULT_K) "4/3" else "1") },
                    )
                }
            }
            ChipRow(stringResource(Res.string.terrain_horizon_fov)) {
                FIELDS_OF_VIEW.forEach { deg ->
                    BrandChip(selected = fieldOfView == deg, onClick = { fieldOfView = deg }, label = { Text("$deg°") })
                }
            }
            OfflineTerrain(
                state = state,
                onRadius = viewModel::setSaveRadius,
                onSave = viewModel::saveTerrain,
                onClear = viewModel::clearSavedTerrain,
            )
            ChipRow(stringResource(Res.string.terrain_horizon_only_visible)) {
                Switch(checked = onlyVisible, onCheckedChange = { onlyVisible = it })
            }
            ChipRow(stringResource(Res.string.terrain_horizon_favorites)) {
                Switch(checked = state.onlyFavorites, onCheckedChange = viewModel::setOnlyFavorites)
            }
            ChipRow(stringResource(Res.string.terrain_horizon_precision)) {
                Switch(checked = state.precisionCircles, onCheckedChange = viewModel::setPrecisionCircles)
            }
            ChipRow(stringResource(Res.string.terrain_horizon_labels)) {
                Switch(checked = names, onCheckedChange = { names = it })
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val profile = state.profile
            when {
                state.loading ->
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            stringResource(Res.string.terrain_horizon_loading),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                state.failure != null ->
                    Column(
                        Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(
                                if (state.failure == TerrainHorizonFailure.NO_POSITION) {
                                    Res.string.terrain_horizon_no_position
                                } else {
                                    Res.string.terrain_horizon_network
                                },
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Button(onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) {
                            Text(stringResource(Res.string.retry))
                        }
                    }

                profile != null ->
                    Panorama(
                        profile = profile,
                        targets = if (onlyVisible) state.targets.filter { it.visible } else state.targets,
                        focusNodeNum = state.focusNodeNum,
                        fieldOfView = fieldOfView.toFloat(),
                        names = names,
                        markers = state.nodeChoices.associate { it.num to (Color(it.color) to Color(it.textColor)) },
                        precisions =
                        if (state.precisionCircles) {
                            state.nodeChoices.mapNotNull { c -> c.precisionM?.let { c.num to it } }.toMap()
                        } else {
                            emptyMap()
                        },
                        compass = compass,
                        heading = heading,
                        onManualTurn = { compass = false },
                        onOpenNode = onOpenNode,
                    )
            }
        }
        if (moving) {
            MoveMap(
                state = state,
                onMoved = { lat, lon -> viewModel.setObserver(HorizonObserver.Point(lat, lon)) },
                onReset = { viewModel.setObserver(HorizonObserver.MyNode) },
            )
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (!state.loading && state.failure == null) {
                Text(
                    if (state.targets.isEmpty()) {
                        stringResource(Res.string.terrain_horizon_none)
                    } else {
                        stringResource(
                            Res.string.terrain_horizon_in_sight,
                            state.targets.count { it.visible },
                            state.targets.size,
                        )
                    },
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                stringResource(Res.string.terrain_horizon_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun observerLabel(state: TerrainHorizonState): String {
    val kind =
        when (state.observer) {
            HorizonObserver.MyNode -> stringResource(Res.string.terrain_horizon_observer_my_node)

            HorizonObserver.Phone -> stringResource(Res.string.terrain_horizon_observer_phone)

            is HorizonObserver.OtherNode ->
                state.observerName.ifBlank { stringResource(Res.string.terrain_horizon_observer_node) }

            is HorizonObserver.Point -> stringResource(Res.string.terrain_horizon_observer_point)
        }
    val name =
        if (state.observer == HorizonObserver.MyNode && state.observerName.isNotBlank()) {
            " (${state.observerName})"
        } else {
            ""
        }
    return stringResource(Res.string.terrain_horizon_from) + ": " + kind + name + " · " + state.usedAntennaM + " m"
}

@Composable
private fun ChipRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
        content()
    }
}

/** The place to look from: the radio, the phone, another node from a list, or a point typed in. */
@Composable
private fun ObserverDialog(nodes: List<HorizonNodeChoice>, onPick: (HorizonObserver) -> Unit, onDismiss: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    var lat by rememberSaveable { mutableStateOf("") }
    var lon by rememberSaveable { mutableStateOf("") }
    val point =
        lat.replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it in -90.0..90.0 }
            ?.let { la ->
                lon.replace(',', '.')
                    .toDoubleOrNull()
                    ?.takeIf { it in -180.0..180.0 }
                    ?.let { lo -> HorizonObserver.Point(la, lo) }
            }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.terrain_horizon_observer_title)) },
        text = {
            Column {
                @Composable
                fun Choice(text: String, onClick: () -> Unit) {
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = onClick),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = false, onClick = onClick)
                        Text(text)
                    }
                }
                when (step) {
                    1 ->
                        LazyColumn(Modifier.heightIn(max = LIST_HEIGHT_DP.dp)) {
                            items(nodes, key = { it.num }) { node ->
                                Choice(node.name) { onPick(HorizonObserver.OtherNode(node.num)) }
                            }
                        }

                    2 -> {
                        OutlinedTextField(
                            value = lat,
                            onValueChange = { lat = it },
                            label = { Text(stringResource(Res.string.latitude)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        OutlinedTextField(
                            value = lon,
                            onValueChange = { lon = it },
                            label = { Text(stringResource(Res.string.longitude)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    else -> {
                        Choice(stringResource(Res.string.terrain_horizon_observer_my_node)) {
                            onPick(HorizonObserver.MyNode)
                        }
                        Choice(stringResource(Res.string.terrain_horizon_observer_phone)) {
                            onPick(HorizonObserver.Phone)
                        }
                        Choice(stringResource(Res.string.terrain_horizon_observer_node)) { step = 1 }
                        Choice(stringResource(Res.string.terrain_horizon_observer_point)) { step = 2 }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 2) {
                TextButton(onClick = { point?.let(onPick) }, enabled = point != null) {
                    Text(stringResource(Res.string.terrain_horizon_apply))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

@Composable
private fun Panorama(
    profile: HorizonProfile,
    targets: List<HorizonTarget>,
    focusNodeNum: Int?,
    fieldOfView: Float,
    names: Boolean,
    markers: Map<Int, Pair<Color, Color>>,
    precisions: Map<Int, Double>,
    compass: Boolean,
    heading: Float?,
    onManualTurn: () -> Unit,
    onOpenNode: (Int) -> Unit,
) {
    var centre by rememberSaveable { mutableFloatStateOf(0f) }
    // Opened for one node: the view starts turned to it.
    LaunchedEffect(focusNodeNum, targets) {
        if (focusNodeNum != null) {
            targets.firstOrNull { it.nodeNum == focusNodeNum }?.let { centre = it.bearingDeg.toFloat() }
        }
    }
    // The phone turning turns the view; a finger on the picture takes the turning over until the compass is switched on
    // again.
    LaunchedEffect(compass, heading) { if (compass && heading != null) centre = heading }
    val range = remember(profile, targets) { angleRange(profile, targets) }
    val sky = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val bands = remember(sky, primary) { BandShares.map { lerp(sky, primary, it) } }
    val measurer = rememberTextMeasurer()
    val cardinals =
        listOf(
            stringResource(Res.string.mesh_hud_cardinal_north),
            stringResource(Res.string.mesh_hud_cardinal_east),
            stringResource(Res.string.mesh_hud_cardinal_south),
            stringResource(Res.string.mesh_hud_cardinal_west),
        )
    val currentCentre by rememberUpdatedState(centre)
    val currentTargets by rememberUpdatedState(targets)
    val currentFov by rememberUpdatedState(fieldOfView)
    Canvas(
        modifier =
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dx ->
                    onManualTurn()
                    val perDeg = size.width / currentFov
                    centre = ((currentCentre - dx / perDeg) % FULL_TURN + FULL_TURN) % FULL_TURN
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val perDeg = size.width / currentFov
                    currentTargets
                        .map {
                            it to
                                Offset(
                                    xOf(it.bearingDeg, currentCentre, perDeg, size.width.toFloat()),
                                    yOf(it.angleDeg, range, size.height.toFloat()),
                                )
                        }
                        .minByOrNull { (_, at) -> (at - tap).getDistance() }
                        ?.takeIf { (_, at) -> (at - tap).getDistance() <= TAP_RADIUS_PX }
                        ?.let { (target, _) -> onOpenNode(target.nodeNum) }
                }
            },
    ) {
        val perDeg = size.width / fieldOfView
        drawBands(profile, bands, centre, perDeg, range)
        drawGuides(measurer, centre, perDeg, fieldOfView, range, cardinals)
        drawTargets(measurer, targets, markers, precisions, centre, perDeg, range, focusNodeNum, names)
    }
}

/**
 * The vertical range of the picture in degrees above the horizontal: the whole skyline and every node, never less than
 * a few degrees.
 */
private fun angleRange(profile: HorizonProfile, targets: List<HorizonTarget>): ClosedFloatingPointRange<Double> {
    val skyline = profile.points.map { it.angleDeg }
    val low = max(MIN_ANGLE_FLOOR, min(-3.0, (skyline.minOrNull() ?: 0.0) - 1.0))
    var high = max(4.0, (skyline.maxOrNull() ?: 0.0) + 2.0)
    targets.forEach { high = max(high, it.angleDeg + 1.0) }
    return low..min(MAX_ANGLE_CEIL, high)
}

private fun yOf(angleDeg: Double, range: ClosedFloatingPointRange<Double>, height: Float): Float {
    val share = ((angleDeg - range.start) / (range.endInclusive - range.start)).coerceIn(0.0, 1.0)
    return (height * (1.0 - share)).toFloat()
}

/** Where a bearing falls across the view; a bearing outside the field of view lands outside the width. */
private fun xOf(bearingDeg: Double, centre: Float, perDeg: Float, width: Float): Float {
    var delta = ((bearingDeg - centre) % FULL_TURN + FULL_TURN) % FULL_TURN
    if (delta > FULL_TURN / 2) delta -= FULL_TURN
    return width / 2 + delta.toFloat() * perDeg
}

/** The ridges, far ones light and near ones dark, each drawn over the one behind it. */
private fun DrawScope.drawBands(
    profile: HorizonProfile,
    bands: List<Color>,
    centre: Float,
    perDeg: Float,
    range: ClosedFloatingPointRange<Double>,
) {
    val w = size.width
    val h = size.height
    for (band in bands.indices) {
        val from = BandEdges[bands.lastIndex - band] * profile.radiusM
        val to = BandEdges[bands.lastIndex - band + 1] * profile.radiusM
        val path = Path()
        path.moveTo(0f, h)
        var x = 0f
        while (x <= w + COLUMN_STEP_PX) {
            val bearing = centre + (x - w / 2) / perDeg
            val angle =
                profile
                    .at(bearing.toDouble())
                    .crests
                    .filter { it.distanceM >= from && it.distanceM < to }
                    .maxOfOrNull { it.angleDeg }
            path.lineTo(x, if (angle == null) h else yOf(angle, range, h))
            x += COLUMN_STEP_PX
        }
        path.lineTo(w + COLUMN_STEP_PX, h)
        path.close()
        drawPath(path, bands[bands.lastIndex - band])
    }
}

/** The horizontal and the bearings across the top: the compass letters and a mark every 15°. */
private fun DrawScope.drawGuides(
    measurer: TextMeasurer,
    centre: Float,
    perDeg: Float,
    fieldOfView: Float,
    range: ClosedFloatingPointRange<Double>,
    cardinals: List<String>,
) {
    val w = size.width
    val zero = yOf(0.0, range, size.height)
    drawLine(Color(0x88FFFFFF), Offset(0f, zero), Offset(w, zero), strokeWidth = 1f)
    val first = ((centre - fieldOfView / 2) / 15f).toInt() - 1
    val last = ((centre + fieldOfView / 2) / 15f).toInt() + 1
    for (step in first..last) {
        val bearing = ((step * 15f) % FULL_TURN + FULL_TURN) % FULL_TURN
        val x = xOf(bearing.toDouble(), centre, perDeg, w)
        if (x < 0f || x > w) continue
        val label =
            if (bearing % 90f == 0f) {
                cardinals[(bearing / 90f).toInt() % 4]
            } else if (bearing % 45f == 0f) {
                "${bearing.roundToInt()}°"
            } else {
                ""
            }
        drawLine(Color(0x44FFFFFF), Offset(x, 0f), Offset(x, if (label.isEmpty()) 10f else 18f), strokeWidth = 1f)
        if (label.isNotEmpty()) {
            val layout = measurer.measure(label, TextStyle(color = Color.White, fontSize = 12.sp))
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, 20f))
        }
    }
}

private fun DrawScope.drawTargets(
    measurer: TextMeasurer,
    targets: List<HorizonTarget>,
    markers: Map<Int, Pair<Color, Color>>,
    precisions: Map<Int, Double>,
    centre: Float,
    perDeg: Float,
    range: ClosedFloatingPointRange<Double>,
    focusNodeNum: Int?,
    names: Boolean,
) {
    val w = size.width
    val labelled = targets.sortedBy { it.distanceM }.take(MAX_LABELS).toSet()
    // The hidden ones first, so a node in sight is never covered by one that is not.
    for (target in targets.sortedBy { it.visible }) {
        val x = xOf(target.bearingDeg, centre, perDeg, w)
        if (x < -20f || x > w + 20f) continue
        val y = yOf(target.angleDeg, range, size.height)
        val focus = target.nodeNum == focusNodeNum
        val (background, text) = markers[target.nodeNum] ?: (Color.DarkGray to Color.White)
        val ring = if (target.visible) Visible else Hidden
        // The doubt about where the node stands, as the angle its radius subtends from here, across and up.
        precisions[target.nodeNum]?.let { r ->
            val half = kotlin.math.atan(r / target.distanceM) * 180.0 / kotlin.math.PI
            val rx = (half * perDeg).toFloat()
            val ry = abs(yOf(target.angleDeg + half, range, size.height) - y)
            if (rx >= PRECISION_MIN_PX) {
                val topLeft = Offset(x - rx, y - ry)
                val oval = androidx.compose.ui.geometry.Size(2 * rx, 2 * ry)
                drawOval(background.copy(alpha = PRECISION_FILL_ALPHA), topLeft, oval)
                drawOval(background.copy(alpha = PRECISION_LINE_ALPHA), topLeft, oval, style = Stroke(1.5f))
            }
        }
        drawNodeMarker(
            measurer,
            if (names && (target in labelled || focus)) target.name else null,
            Offset(x, y),
            background,
            text,
            ring,
            focus,
        )
    }
}

private const val METRES_PER_DEGREE = 111_320.0
private const val MAP_HEIGHT_DP = 220
private const val NODE_DOT_PX = 5f

/**
 * A map from above, centred on the place the panorama is looked from, with the nodes on it. Dragging it carries the
 * place to look from to any point; letting go draws the panorama from there.
 */
@Composable
private fun MoveMap(state: TerrainHorizonState, onMoved: (Double, Double) -> Unit, onReset: () -> Unit) {
    val lat0 = state.observerLat ?: return
    val lon0 = state.observerLon ?: return
    val pickMap = LocalHorizonPickMapProvider.current
    if (pickMap != null) {
        // The same map as the main one, with the nodes drawn as there; a cross in the middle marks the place.
        val mapNodes =
            remember(state.nodeChoices, state.precisionCircles, state.onlyFavorites) {
                state.nodeChoices
                    .filter { c -> !state.onlyFavorites || c.favorite }
                    .map {
                        HorizonMapNode(
                            it.lat,
                            it.lon,
                            it.shortName,
                            it.color,
                            it.textColor,
                            it.precisionM.takeIf { _ -> state.precisionCircles },
                        )
                    }
            }
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(Res.string.terrain_horizon_move_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onReset) { Text(stringResource(Res.string.terrain_horizon_move_back)) }
            }
            pickMap(
                lat0,
                lon0,
                state.radiusKm,
                mapNodes,
                onMoved,
                Modifier.fillMaxWidth().height(PICK_MAP_HEIGHT_DP.dp),
            )
        }
        return
    }
    val cosLat = kotlin.math.cos(lat0 * kotlin.math.PI / HALF_TURN_DEG).coerceAtLeast(MIN_COS_LAT)
    var dx by remember(lat0, lon0) { mutableFloatStateOf(0f) }
    var dy by remember(lat0, lon0) { mutableFloatStateOf(0f) }
    val measurer = rememberTextMeasurer()
    val accent = MaterialTheme.colorScheme.primary
    val halfSpanM = state.radiusKm * MAP_METRES_PER_KM
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(Res.string.terrain_horizon_move_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onReset) { Text(stringResource(Res.string.terrain_horizon_move_back)) }
        }
        Canvas(
            modifier =
            Modifier.fillMaxWidth()
                .heightIn(max = MAP_HEIGHT_DP.dp)
                .height(MAP_HEIGHT_DP.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .pointerInput(lat0, lon0, state.radiusKm) {
                    val metresPerPx = halfSpanM / (minOf(size.width, size.height) / 2f)
                    detectDragGestures(
                        onDragEnd = {
                            // The map follows the finger, so the place to look from goes the other way.
                            val eastM = -dx * metresPerPx
                            val northM = dy * metresPerPx
                            if (dx != 0f || dy != 0f) {
                                onMoved(
                                    (lat0 + northM / METRES_PER_DEGREE).coerceIn(-MAX_LAT, MAX_LAT),
                                    lon0 + eastM / (METRES_PER_DEGREE * cosLat),
                                )
                            }
                        },
                    ) { change, drag ->
                        change.consume()
                        dx += drag.x
                        dy += drag.y
                    }
                },
        ) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val radiusPx = minOf(size.width, size.height) / 2f
            val metresPerPx = halfSpanM / radiusPx
            drawCircle(Color(0x33FFFFFF), radius = radiusPx, center = centre, style = Stroke(1f))
            drawCircle(Color(0x22FFFFFF), radius = radiusPx / 2f, center = centre, style = Stroke(1f))
            val shift = Offset(dx, dy)
            state.nodeChoices.forEach { node ->
                val east = (node.lon - lon0) * METRES_PER_DEGREE * cosLat
                val north = (node.lat - lat0) * METRES_PER_DEGREE
                val at = centre + shift + Offset((east / metresPerPx).toFloat(), (-north / metresPerPx).toFloat())
                if (at.x in 0f..size.width && at.y in 0f..size.height) {
                    drawNodeMarker(
                        measurer,
                        node.shortName.ifBlank { null },
                        at,
                        Color(node.color),
                        Color(node.textColor),
                        Color.White,
                        false,
                    )
                }
            }
            // Where the panorama is drawn from now (hollow) and where the finger is taking it (the cross).
            drawCircle(Color.White, radius = NODE_DOT_PX * 1.6f, center = centre + shift, style = Stroke(2f))
            drawLine(accent, centre - Offset(12f, 0f), centre + Offset(12f, 0f), strokeWidth = 3f)
            drawLine(accent, centre - Offset(0f, 12f), centre + Offset(0f, 12f), strokeWidth = 3f)
        }
    }
}

private const val PICK_MAP_HEIGHT_DP = 340
private const val HALF_TURN_DEG = 180.0
private const val MIN_COS_LAT = 0.01
private const val MAX_LAT = 89.9

/** A choice chip in the app's own colours: the chosen one has the yellow outline and a faint yellow fill. */
@Composable
private fun BrandChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        colors =
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = primary.copy(alpha = SELECTED_FILL),
            selectedLabelColor = primary,
        ),
        border =
        BorderStroke(
            if (selected) SELECTED_BORDER_DP.dp else 1.dp,
            if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

private const val SELECTED_FILL = 0.18f
private const val SELECTED_BORDER_DP = 2

/**
 * A node exactly as the main map draws it: its dot (the node's colour, white ring) on the node's point, and above it,
 * after a small gap, the node-coloured chip with the short name and a little tail pointing down (a plain dot when
 * [label] is null). The chip carries a thin [ring] border, which the panorama uses to tell what stands above the ridge.
 */
private fun DrawScope.drawNodeMarker(
    measurer: TextMeasurer,
    label: String?,
    at: Offset,
    background: Color,
    text: Color,
    ring: Color,
    focus: Boolean,
) {
    val dot = CHIP_DOT_DP.dp.toPx()
    drawCircle(background, radius = dot, center = at)
    // A dot alone (no name shown) takes the ring colour, so what stands above the ridge can still be told.
    drawCircle(
        if (label == null) ring else Color.White,
        radius = dot,
        center = at,
        style = Stroke(CHIP_DOT_RING_DP.dp.toPx()),
    )
    if (label == null) return
    val layout = measurer.measure(label, TextStyle(color = text, fontSize = 11.sp))
    val width = max(CHIP_MIN_WIDTH_DP.dp.toPx(), layout.size.width + 2 * CHIP_PAD_DP.dp.toPx())
    val bodyH = CHIP_HEIGHT_DP.dp.toPx()
    val tailH = CHIP_TAIL_HEIGHT_DP.dp.toPx()
    val tailHalf = CHIP_TAIL_WIDTH_DP.dp.toPx() / 2f
    val bodyBottom = at.y - CHIP_GAP_DP.dp.toPx() - tailH
    val top = Offset(at.x - width / 2f, max(0f, bodyBottom - bodyH))
    val corner = CornerRadius(CHIP_CORNER_DP.dp.toPx())
    drawRoundRect(background, top, Size(width, bodyH), corner)
    val border = (if (focus) CHIP_FOCUS_RING_DP else CHIP_RING_DP).dp.toPx()
    drawRoundRect(
        ring,
        Offset(top.x + border / 2f, top.y + border / 2f),
        Size(width - border, bodyH - border),
        corner,
        style = Stroke(border),
    )
    val tail =
        Path().apply {
            moveTo(at.x - tailHalf, top.y + bodyH)
            lineTo(at.x + tailHalf, top.y + bodyH)
            lineTo(at.x, top.y + bodyH + tailH)
            close()
        }
    drawPath(tail, background)
    drawPath(
        tail,
        background,
        style = Stroke(CHIP_TAIL_ROUND_DP.dp.toPx() * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawText(
        layout,
        topLeft = Offset(top.x + (width - layout.size.width) / 2f, top.y + (bodyH - layout.size.height) / 2f),
    )
}

// The main map's chip, in dp: Material's small corner, a body 20 tall and at least 44 wide, a 6 x 8 tail, 8 of gap and
// a
// dot of 4 with a 2 ring.
private const val CHIP_DOT_DP = 4
private const val CHIP_DOT_RING_DP = 2
private const val CHIP_MIN_WIDTH_DP = 44
private const val CHIP_PAD_DP = 6
private const val CHIP_HEIGHT_DP = 20
private const val CHIP_CORNER_DP = 6
private const val CHIP_TAIL_HEIGHT_DP = 6
private const val CHIP_TAIL_WIDTH_DP = 8
private const val CHIP_GAP_DP = 8
private const val CHIP_TAIL_ROUND_DP = 2
private const val CHIP_RING_DP = 1.5f
private const val PRECISION_MIN_PX = 3f
private const val PRECISION_FILL_ALPHA = 0.18f
private const val PRECISION_LINE_ALPHA = 0.7f
private const val CHIP_FOCUS_RING_DP = 3f
private const val MAP_METRES_PER_KM = 1_000.0

/** Saving the terrain on the device, so the planner and the panorama work there without the internet. */
@Composable
private fun OfflineTerrain(
    state: TerrainHorizonState,
    onRadius: (Int) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            stringResource(Res.string.terrain_horizon_offline),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
        )
        Text(
            stringResource(Res.string.terrain_horizon_offline_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        ChipRow(stringResource(Res.string.terrain_horizon_save_range)) {
            HORIZON_RADII_KM.forEach { km ->
                BrandChip(selected = state.saveRadiusKm == km, onClick = { onRadius(km) }, label = { Text("$km km") })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onSave, enabled = !state.saving && state.observerLat != null) {
                Text(stringResource(Res.string.terrain_horizon_save, state.saveRadiusKm))
            }
            if (state.savedBytes > 0L && !state.saving) {
                TextButton(onClick = onClear) { Text(stringResource(Res.string.terrain_horizon_save_clear)) }
            }
        }
        when {
            state.saving ->
                Text(
                    stringResource(Res.string.terrain_horizon_saving, state.saveDone, state.saveTotal),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                )

            state.saveOk == false ->
                Text(
                    stringResource(Res.string.terrain_horizon_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                )

            state.savedBytes > 0L ->
                Text(
                    stringResource(Res.string.terrain_horizon_saved, megabytes(state.savedBytes)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
        }
    }
}

private const val BYTES_PER_MB = 1_000_000L
private const val TENTHS = 10L

private fun megabytes(bytes: Long): String {
    val tenths = bytes * TENTHS / BYTES_PER_MB
    return "${tenths / TENTHS}.${tenths % TENTHS}"
}
