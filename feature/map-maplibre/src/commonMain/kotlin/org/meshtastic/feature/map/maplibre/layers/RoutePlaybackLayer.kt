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
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.feature.map.playback.RoutePlayback

/** MT_SW: the part of the replayed route already walked, and a dot where the walk is now. */
@Composable
internal fun RoutePlaybackLayer() {
    val playback: RoutePlayback = koinInject()
    val state by playback.state.collectAsStateWithLifecycle()
    val now = state.current ?: return
    val walked = state.steps.subList(0, state.index + 1)

    if (walked.size >= 2) {
        val trail =
            rememberFeatureSource(walked.size, state.steps) {
                FeatureCollection(
                    listOf(
                        Feature<LineString, JsonObject?>(
                            geometry =
                            LineString(walked.map { Position(longitude = it.longitude, latitude = it.latitude) }),
                            properties = null,
                        ),
                    ),
                )
            }
        LineLayer(
            id = "route-playback-trail",
            source = trail,
            cap = const(LineCap.Round),
            join = const(LineJoin.Round),
            color = const(TRAIL_COLOR),
            width = const(TRAIL_WIDTH_DP.dp),
            opacity = const(TRAIL_OPACITY),
        )
    }
    val dot =
        rememberFeatureSource(now) {
            FeatureCollection(
                listOf(
                    Feature<Point, JsonObject?>(
                        geometry = Point(Position(longitude = now.longitude, latitude = now.latitude)),
                        properties = null,
                    ),
                ),
            )
        }
    CircleLayer(
        id = "route-playback-dot",
        source = dot,
        color = const(TRAIL_COLOR),
        radius = const(DOT_RADIUS_DP.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(3.dp),
    )
}

private const val TRAIL_WIDTH_DP = 5
private const val TRAIL_OPACITY = 0.9f
private const val DOT_RADIUS_DP = 9
private val TRAIL_COLOR = Color(0xFF1E88E5)
