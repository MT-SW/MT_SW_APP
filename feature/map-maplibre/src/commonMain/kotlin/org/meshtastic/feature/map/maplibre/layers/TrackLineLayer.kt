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
package org.meshtastic.feature.map.maplibre.layers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.json.JsonObject
import org.koin.compose.koinInject
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.LineLayer
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.feature.map.track.TrackRecorder

/** MT_SW: the route being recorded (or just recorded) by [TrackRecorder], drawn as one line with a light outline. */
@Composable
internal fun TrackLineLayer() {
    val recorder: TrackRecorder = koinInject()
    val state by recorder.state.collectAsStateWithLifecycle()
    val points = state.points
    if (points.size < 2) return

    val source =
        rememberFeatureSource(points) {
            FeatureCollection(
                listOf(
                    Feature<LineString, JsonObject?>(
                        geometry =
                        LineString(points.map { Position(longitude = it.longitude, latitude = it.latitude) }),
                        properties = null,
                    ),
                ),
            )
        }
    LineLayer(
        id = "own-track-outline",
        source = source,
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
        color = const(Color.White),
        width = const((TRACK_WIDTH_DP + TRACK_OUTLINE_DP).dp),
        opacity = const(TRACK_OUTLINE_OPACITY),
    )
    LineLayer(
        id = "own-track",
        source = source,
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
        color = const(TRACK_COLOR),
        width = const(TRACK_WIDTH_DP.dp),
        opacity = const(TRACK_OPACITY),
    )
}

private const val TRACK_WIDTH_DP = 4
private const val TRACK_OUTLINE_DP = 2
private const val TRACK_OUTLINE_OPACITY = 0.8f
private const val TRACK_OPACITY = 0.95f
private val TRACK_COLOR = Color(0xFFE53935)
