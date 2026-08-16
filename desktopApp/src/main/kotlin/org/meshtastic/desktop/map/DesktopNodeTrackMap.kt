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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.feature.map.node.NodeMapViewModel
import org.meshtastic.proto.Position

private const val TRACK_LINE_WIDTH_DP = 3
private const val POSITION_DOT_SIZE_DP = 10
private const val SELECTED_POSITION_DOT_SIZE_DP = 16
private val TRACK_LINE_COLOR = Color(0xFF2196F3)
private val POSITION_DOT_COLOR = Color(0xFF2196F3)
private val SELECTED_POSITION_DOT_COLOR = Color(0xFFFF5722)

/**
 * Pure position-track map renderer — no ViewModel of its own, takes [positions] directly. Mirrors the fdroid/google
 * flavors' `NodeTrackOsmMap`: draws a polyline connecting consecutive positions plus a dot marker per position,
 * highlighting [selectedPositionTime] if given and reporting taps via [onPositionSelected]. Auto-fits the camera to
 * the track's full bounding box the first time non-empty [positions] arrive for a nonzero viewport size, same pattern
 * as [DesktopMapViewProvider]'s node auto-framing.
 */
@Composable
fun DesktopTrackMap(
    positions: List<Position>,
    modifier: Modifier = Modifier,
    selectedPositionTime: Int? = null,
    onPositionSelected: ((Int) -> Unit)? = null,
) {
    val viewport = rememberMapViewportState()
    val hasCentered = remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OsmCanvasMap(viewport = viewport, modifier = Modifier.fillMaxSize()) { toScreenOffset, viewportSize ->
            LaunchedEffect(positions, viewportSize) {
                if (!hasCentered.value && positions.isNotEmpty() && viewportSize.x > 0f && viewportSize.y > 0f) {
                    val lats = positions.map { (it.latitude_i ?: 0) * 1e-7 }
                    val lons = positions.map { (it.longitude_i ?: 0) * 1e-7 }
                    viewport.fitToBounds(lons.min(), lats.min(), lons.max(), lats.max(), viewportSize)
                    hasCentered.value = true
                }
            }

            if (positions.size >= 2) {
                val screenPoints =
                    positions.map { pos ->
                        toScreenOffset((pos.longitude_i ?: 0) * 1e-7, (pos.latitude_i ?: 0) * 1e-7)
                    }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0 until screenPoints.size - 1) {
                        drawLine(
                            color = TRACK_LINE_COLOR,
                            start = screenPoints[i],
                            end = screenPoints[i + 1],
                            strokeWidth = TRACK_LINE_WIDTH_DP.dp.toPx(),
                        )
                    }
                }
            }

            positions.forEach { pos ->
                val screenOffset = toScreenOffset((pos.longitude_i ?: 0) * 1e-7, (pos.latitude_i ?: 0) * 1e-7)
                val isSelected = selectedPositionTime != null && pos.time == selectedPositionTime
                val dotSize = if (isSelected) SELECTED_POSITION_DOT_SIZE_DP else POSITION_DOT_SIZE_DP
                val dotColor = if (isSelected) SELECTED_POSITION_DOT_COLOR else POSITION_DOT_COLOR
                Box(
                    modifier =
                        Modifier
                            .offset {
                                IntOffset(
                                    screenOffset.x.toInt() - dotSize / 2,
                                    screenOffset.y.toInt() - dotSize / 2,
                                )
                            }
                            .size(dotSize.dp)
                            .background(dotColor, CircleShape)
                            .let { m -> if (onPositionSelected != null) m.clickable { onPositionSelected(pos.time) } else m },
                )
            }
        }
    }
}

/**
 * Desktop implementation of [org.meshtastic.core.ui.util.LocalNodeTrackMapProvider]'s function type — the embedded
 * track map shown above the chart in the Position Log screen. Resolves [destNum] via [NodeMapViewModel.setDestNum],
 * mirroring the fdroid/google flavors' `NodeTrackMap` wrapper.
 */
@Composable
fun DesktopNodeTrackMap(
    destNum: Int,
    positions: List<Position>,
    modifier: Modifier = Modifier,
    selectedPositionTime: Int? = null,
    onPositionSelected: ((Int) -> Unit)? = null,
) {
    val vm: NodeMapViewModel = koinViewModel()
    LaunchedEffect(destNum) { vm.setDestNum(destNum) }
    // Rounded to match the app's card styling (e.g. PositionSection's inline map Surface) — BaseMetricChart's
    // chartPart slot itself has no rounded container, so without this the tiles show sharp square corners.
    Surface(shape = MaterialTheme.shapes.large, modifier = modifier) {
        DesktopTrackMap(
            positions = positions,
            modifier = Modifier.fillMaxSize(),
            selectedPositionTime = selectedPositionTime,
            onPositionSelected = onPositionSelected,
        )
    }
}