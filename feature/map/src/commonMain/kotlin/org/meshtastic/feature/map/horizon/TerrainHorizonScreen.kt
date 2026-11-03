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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.mesh_hud_cardinal_east
import org.meshtastic.core.resources.mesh_hud_cardinal_north
import org.meshtastic.core.resources.mesh_hud_cardinal_south
import org.meshtastic.core.resources.mesh_hud_cardinal_west
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.resources.retry
import org.meshtastic.core.resources.terrain_horizon_antenna
import org.meshtastic.core.resources.terrain_horizon_camera
import org.meshtastic.core.resources.terrain_horizon_hint
import org.meshtastic.core.resources.terrain_horizon_in_sight
import org.meshtastic.core.resources.terrain_horizon_loading
import org.meshtastic.core.resources.terrain_horizon_network
import org.meshtastic.core.resources.terrain_horizon_no_position
import org.meshtastic.core.resources.terrain_horizon_none
import org.meshtastic.core.resources.terrain_horizon_range
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

private const val FIELD_OF_VIEW_DEG = 120f
private const val FULL_TURN = 360f
private const val MIN_ANGLE_FLOOR = -15.0
private const val MAX_ANGLE_CEIL = 30.0
private const val TAP_RADIUS_PX = 48f
private const val COLUMN_STEP_PX = 3
private const val MAX_LABELS = 40

/**
 * The terrain panorama: the skyline all round with the mesh's nodes placed against it, green where a node stands above
 * the ridge in front of it and red where a ridge hides it. It is the first view of the mesh view; the camera is the
 * second ([onOpenCamera]). [focusNodeNum] limits it to one node.
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
    Column(modifier = modifier.fillMaxSize().background(Sky).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateUp) {
                Icon(MeshtasticIcons.ArrowBack, stringResource(Res.string.navigate_back), tint = Color.White)
            }
            Text(
                stringResource(Res.string.terrain_horizon_title),
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenCamera) {
                Icon(MeshtasticIcons.PhotoCamera, stringResource(Res.string.terrain_horizon_camera), tint = Color.White)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.terrain_horizon_range), color = Color.White)
            HORIZON_RADII_KM.forEach { km ->
                FilterChip(
                    selected = state.radiusKm == km,
                    onClick = { viewModel.setRadius(km) },
                    label = { Text("$km km") },
                )
            }
            Text(stringResource(Res.string.terrain_horizon_antenna), color = Color.White)
            HORIZON_ANTENNAS_M.forEach { m ->
                FilterChip(
                    selected = state.antennaM == m,
                    onClick = { viewModel.setAntenna(m) },
                    label = { Text("$m m") },
                )
            }
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
                        Button(onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) {
                            Text(stringResource(Res.string.retry))
                        }
                    }
                profile != null -> Panorama(profile, state.targets, state.focusNodeNum, onOpenNode)
            }
        }
        val seen = state.targets.count { it.visible }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (!state.loading && state.failure == null) {
                Text(
                    if (state.targets.isEmpty()) {
                        stringResource(Res.string.terrain_horizon_none)
                    } else {
                        stringResource(Res.string.terrain_horizon_in_sight, seen, state.targets.size)
                    },
                    color = Color.White,
                )
            }
            Text(stringResource(Res.string.terrain_horizon_hint), color = Color(0xFFB0BEC5), fontSize = 11.sp)
        }
    }
}

@Composable
private fun Panorama(
    profile: HorizonProfile,
    targets: List<HorizonTarget>,
    focusNodeNum: Int?,
    onOpenNode: (Int) -> Unit,
) {
    var centre by rememberSaveable { mutableFloatStateOf(0f) }
    // Opened for one node: the view starts turned to it.
    LaunchedEffect(focusNodeNum, targets) {
        if (focusNodeNum != null) targets.firstOrNull { it.nodeNum == focusNodeNum }?.let { centre = it.bearingDeg.toFloat() }
    }
    val range = remember(profile, targets) { angleRange(profile, targets) }
    val measurer = rememberTextMeasurer()
    val north = stringResource(Res.string.mesh_hud_cardinal_north)
    val east = stringResource(Res.string.mesh_hud_cardinal_east)
    val south = stringResource(Res.string.mesh_hud_cardinal_south)
    val west = stringResource(Res.string.mesh_hud_cardinal_west)
    val currentCentre by rememberUpdatedState(centre)
    val currentTargets by rememberUpdatedState(targets)
    Canvas(
        modifier =
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dx ->
                    val perDeg = size.width / FIELD_OF_VIEW_DEG
                    centre = ((currentCentre - dx / perDeg) % FULL_TURN + FULL_TURN) % FULL_TURN
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val perDeg = size.width / FIELD_OF_VIEW_DEG
                    currentTargets
                        .map { it to Offset(xOf(it.bearingDeg, currentCentre, perDeg, size.width.toFloat()), yOf(it.angleDeg, range, size.height.toFloat())) }
                        .minByOrNull { (_, at) -> (at - tap).getDistance() }
                        ?.takeIf { (_, at) -> (at - tap).getDistance() <= TAP_RADIUS_PX }
                        ?.let { (target, _) -> onOpenNode(target.nodeNum) }
                }
            },
    ) {
        val perDeg = size.width / FIELD_OF_VIEW_DEG
        drawBands(profile, centre, perDeg, range)
        drawGuides(measurer, centre, perDeg, range, listOf(north, east, south, west))
        drawTargets(measurer, targets, centre, perDeg, range, focusNodeNum)
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
private fun DrawScope.drawGuides(measurer: TextMeasurer, centre: Float, perDeg: Float, range: ClosedFloatingPointRange<Double>, cardinals: List<String>) {
    val w = size.width
    val zero = yOf(0.0, range, size.height)
    drawLine(Color(0x88FFFFFF), Offset(0f, zero), Offset(w, zero), strokeWidth = 1f)
    val first = ((centre - FIELD_OF_VIEW_DEG / 2) / 15f).toInt() - 1
    val last = ((centre + FIELD_OF_VIEW_DEG / 2) / 15f).toInt() + 1
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
) {
    val w = size.width
    val labelled = targets.sortedBy { it.distanceM }.take(MAX_LABELS).toSet()
    // The hidden ones first, so a node in sight is never covered by one that is not.
    for (target in targets.sortedBy { it.visible }) {
        val x = xOf(target.bearingDeg, centre, perDeg, w)
        if (x < -20f || x > w + 20f) continue
        val y = yOf(target.angleDeg, range, size.height)
        val color = if (target.visible) Visible else Hidden
        val focus = target.nodeNum == focusNodeNum
        drawCircle(color, radius = if (focus) 11f else 7f, center = Offset(x, y))
        drawCircle(Color.White, radius = if (focus) 11f else 7f, center = Offset(x, y), style = Stroke(width = 1.5f))
        if (target in labelled || focus) {
            val layout = measurer.measure(target.name, TextStyle(color = Color.White, fontSize = 11.sp))
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, max(0f, y - 16f - layout.size.height)))
        }
    }
}
