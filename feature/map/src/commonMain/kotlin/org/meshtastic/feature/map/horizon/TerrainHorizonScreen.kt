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
package org.meshtastic.feature.map.horizon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
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
import org.meshtastic.core.resources.terrain_horizon_fov
import org.meshtastic.core.resources.terrain_horizon_from
import org.meshtastic.core.resources.terrain_horizon_hint
import org.meshtastic.core.resources.terrain_horizon_in_sight
import org.meshtastic.core.resources.terrain_horizon_labels
import org.meshtastic.core.resources.terrain_horizon_loading
import org.meshtastic.core.resources.terrain_horizon_network
import org.meshtastic.core.resources.terrain_horizon_no_position
import org.meshtastic.core.resources.terrain_horizon_none
import org.meshtastic.core.resources.terrain_horizon_observer_my_node
import org.meshtastic.core.resources.terrain_horizon_observer_node
import org.meshtastic.core.resources.terrain_horizon_observer_phone
import org.meshtastic.core.resources.terrain_horizon_observer_point
import org.meshtastic.core.resources.terrain_horizon_observer_title
import org.meshtastic.core.resources.terrain_horizon_only_visible
import org.meshtastic.core.resources.terrain_horizon_options
import org.meshtastic.core.resources.terrain_horizon_range
import org.meshtastic.core.resources.terrain_horizon_refraction
import org.meshtastic.core.resources.terrain_horizon_target_antenna
import org.meshtastic.core.resources.terrain_horizon_title
import org.meshtastic.core.ui.icon.ArrowBack
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PhotoCamera
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val Sky = Color(0xFF0E1620)
private val Visible = Color(0xFF66BB6A)
private val Hidden = Color(0xFFEF5350)
private val Bands = listOf(Color(0xFF9DB0C2), Color(0xFF73889C), Color(0xFF506477), Color(0xFF334453))
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
    var picking by rememberSaveable { mutableStateOf(false) }
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
    Column(modifier = modifier.fillMaxSize().background(Sky).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateUp) {
                Icon(MeshtasticIcons.ArrowBack, stringResource(Res.string.navigate_back), tint = Color.White)
            }
            Text(stringResource(Res.string.terrain_horizon_title), color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenCamera) {
                Icon(MeshtasticIcons.PhotoCamera, stringResource(Res.string.terrain_horizon_camera), tint = Color.White)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = { picking = true },
                label = { Text(observerLabel(state)) },
                modifier = Modifier.weight(1f, fill = false),
            )
            TextButton(onClick = { moreOptions = !moreOptions }) { Text(stringResource(Res.string.terrain_horizon_options)) }
        }
        ChipRow(stringResource(Res.string.terrain_horizon_range)) {
            HORIZON_RADII_KM.forEach { km ->
                FilterChip(selected = state.radiusKm == km, onClick = { viewModel.setRadius(km) }, label = { Text("$km km") })
            }
        }
        ChipRow(stringResource(Res.string.terrain_horizon_antenna)) {
            state.autoAntennaM?.let { auto ->
                FilterChip(
                    selected = state.antennaAuto,
                    onClick = viewModel::setAntennaAuto,
                    label = { Text(stringResource(Res.string.terrain_horizon_antenna_auto) + " ($auto m)") },
                )
            }
            HORIZON_ANTENNAS_M.forEach { m ->
                FilterChip(
                    selected = !(state.antennaAuto && state.autoAntennaM != null) && state.antennaM == m,
                    onClick = { viewModel.setAntenna(m) },
                    label = { Text("$m m") },
                )
            }
        }
        if (moreOptions) {
            ChipRow(stringResource(Res.string.terrain_horizon_target_antenna)) {
                HORIZON_ANTENNAS_M.forEach { m ->
                    FilterChip(selected = state.targetAntennaM == m, onClick = { viewModel.setTargetAntenna(m) }, label = { Text("$m m") })
                }
            }
            ChipRow(stringResource(Res.string.terrain_horizon_refraction)) {
                HORIZON_REFRACTIONS.forEach { k ->
                    FilterChip(
                        selected = state.refraction == k,
                        onClick = { viewModel.setRefraction(k) },
                        label = { Text(if (k == HorizonPanorama.DEFAULT_K) "4/3" else "1") },
                    )
                }
            }
            ChipRow(stringResource(Res.string.terrain_horizon_fov)) {
                FIELDS_OF_VIEW.forEach { deg ->
                    FilterChip(selected = fieldOfView == deg, onClick = { fieldOfView = deg }, label = { Text("$deg°") })
                }
            }
            ChipRow(stringResource(Res.string.terrain_horizon_only_visible)) { Switch(checked = onlyVisible, onCheckedChange = { onlyVisible = it }) }
            ChipRow(stringResource(Res.string.terrain_horizon_labels)) { Switch(checked = names, onCheckedChange = { names = it }) }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val profile = state.profile
            when {
                state.loading ->
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.terrain_horizon_loading), color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    }
                state.failure != null ->
                    Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(
                                if (state.failure == TerrainHorizonFailure.NO_POSITION) {
                                    Res.string.terrain_horizon_no_position
                                } else {
                                    Res.string.terrain_horizon_network
                                },
                            ),
                            color = Color.White,
                        )
                        Button(onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(Res.string.retry)) }
                    }
                profile != null ->
                    Panorama(
                        profile = profile,
                        targets = if (onlyVisible) state.targets.filter { it.visible } else state.targets,
                        focusNodeNum = state.focusNodeNum,
                        fieldOfView = fieldOfView.toFloat(),
                        names = names,
                        onOpenNode = onOpenNode,
                    )
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (!state.loading && state.failure == null) {
                Text(
                    if (state.targets.isEmpty()) {
                        stringResource(Res.string.terrain_horizon_none)
                    } else {
                        stringResource(Res.string.terrain_horizon_in_sight, state.targets.count { it.visible }, state.targets.size)
                    },
                    color = Color.White,
                )
            }
            Text(stringResource(Res.string.terrain_horizon_hint), color = Color(0xFFB0BEC5), fontSize = 11.sp)
        }
    }
}

@Composable
private fun observerLabel(state: TerrainHorizonState): String {
    val kind =
        when (state.observer) {
            HorizonObserver.MyNode -> stringResource(Res.string.terrain_horizon_observer_my_node)
            HorizonObserver.Phone -> stringResource(Res.string.terrain_horizon_observer_phone)
            is HorizonObserver.OtherNode -> state.observerName.ifBlank { stringResource(Res.string.terrain_horizon_observer_node) }
            is HorizonObserver.Point -> stringResource(Res.string.terrain_horizon_observer_point)
        }
    val name = if (state.observer == HorizonObserver.MyNode && state.observerName.isNotBlank()) " (${state.observerName})" else ""
    return stringResource(Res.string.terrain_horizon_from) + ": " + kind + name + " · " + state.usedAntennaM + " m"
}

@Composable
private fun ChipRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
        content()
    }
}

/** The place to look from: the radio, the phone, another node from a list, or a point typed in. */
@Composable
private fun ObserverDialog(nodes: List<HorizonNodeChoice>, onPick: (HorizonObserver) -> Unit, onDismiss: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    var lat by rememberSaveable { mutableStateOf("") }
    var lon by rememberSaveable { mutableStateOf("") }
    val point = lat.replace(',', '.').toDoubleOrNull()?.takeIf { it in -90.0..90.0 }?.let { la ->
        lon.replace(',', '.').toDoubleOrNull()?.takeIf { it in -180.0..180.0 }?.let { lo -> HorizonObserver.Point(la, lo) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.terrain_horizon_observer_title)) },
        text = {
            Column {
                @Composable
                fun Choice(text: String, onClick: () -> Unit) {
                    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = false, onClick = onClick)
                        Text(text)
                    }
                }
                when (step) {
                    1 ->
                        LazyColumn(Modifier.heightIn(max = LIST_HEIGHT_DP.dp)) {
                            items(nodes, key = { it.num }) { node -> Choice(node.name) { onPick(HorizonObserver.OtherNode(node.num)) } }
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
                        Choice(stringResource(Res.string.terrain_horizon_observer_my_node)) { onPick(HorizonObserver.MyNode) }
                        Choice(stringResource(Res.string.terrain_horizon_observer_phone)) { onPick(HorizonObserver.Phone) }
                        Choice(stringResource(Res.string.terrain_horizon_observer_node)) { step = 1 }
                        Choice(stringResource(Res.string.terrain_horizon_observer_point)) { step = 2 }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 2) {
                TextButton(onClick = { point?.let(onPick) }, enabled = point != null) { Text(stringResource(Res.string.terrain_horizon_apply)) }
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
    onOpenNode: (Int) -> Unit,
) {
    var centre by rememberSaveable { mutableFloatStateOf(0f) }
    // Opened for one node: the view starts turned to it.
    LaunchedEffect(focusNodeNum, targets) {
        if (focusNodeNum != null) targets.firstOrNull { it.nodeNum == focusNodeNum }?.let { centre = it.bearingDeg.toFloat() }
    }
    val range = remember(profile, targets) { angleRange(profile, targets) }
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
                    val perDeg = size.width / currentFov
                    centre = ((currentCentre - dx / perDeg) % FULL_TURN + FULL_TURN) % FULL_TURN
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val perDeg = size.width / currentFov
                    currentTargets
                        .map { it to Offset(xOf(it.bearingDeg, currentCentre, perDeg, size.width.toFloat()), yOf(it.angleDeg, range, size.height.toFloat())) }
                        .minByOrNull { (_, at) -> (at - tap).getDistance() }
                        ?.takeIf { (_, at) -> (at - tap).getDistance() <= TAP_RADIUS_PX }
                        ?.let { (target, _) -> onOpenNode(target.nodeNum) }
                }
            },
    ) {
        val perDeg = size.width / fieldOfView
        drawBands(profile, centre, perDeg, range)
        drawGuides(measurer, centre, perDeg, fieldOfView, range, cardinals)
        drawTargets(measurer, targets, centre, perDeg, range, focusNodeNum, names)
    }
}

/** The vertical range of the picture in degrees above the horizontal: the whole skyline and every node, never less than a few degrees. */
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
private fun DrawScope.drawBands(profile: HorizonProfile, centre: Float, perDeg: Float, range: ClosedFloatingPointRange<Double>) {
    val w = size.width
    val h = size.height
    for (band in Bands.indices) {
        val from = BandEdges[Bands.lastIndex - band] * profile.radiusM
        val to = BandEdges[Bands.lastIndex - band + 1] * profile.radiusM
        val path = Path()
        path.moveTo(0f, h)
        var x = 0f
        while (x <= w + COLUMN_STEP_PX) {
            val bearing = centre + (x - w / 2) / perDeg
            val angle = profile.at(bearing.toDouble()).crests.filter { it.distanceM >= from && it.distanceM < to }.maxOfOrNull { it.angleDeg }
            path.lineTo(x, if (angle == null) h else yOf(angle, range, h))
            x += COLUMN_STEP_PX
        }
        path.lineTo(w + COLUMN_STEP_PX, h)
        path.close()
        drawPath(path, Bands[Bands.lastIndex - band])
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
        val label = if (bearing % 90f == 0f) cardinals[(bearing / 90f).toInt() % 4] else if (bearing % 45f == 0f) "${bearing.roundToInt()}°" else ""
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
        val radius = if (focus) 11f else 7f
        drawCircle(if (target.visible) Visible else Hidden, radius = radius, center = Offset(x, y))
        drawCircle(Color.White, radius = radius, center = Offset(x, y), style = Stroke(width = 1.5f))
        if (names && (target in labelled || focus)) {
            val layout = measurer.measure(target.name, TextStyle(color = Color.White, fontSize = 11.sp))
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, max(0f, y - 16f - layout.size.height)))
        }
    }
}
