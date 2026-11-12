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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.ui.util.snrRgb
import org.meshtastic.feature.map.coverage.CoveragePoint
import org.meshtastic.feature.map.coverage.CoverageState
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import kotlin.math.roundToInt

/** The layer id, also the key of its opacity setting. */
internal const val COVERAGE_LAYER_ID = "coverage"

/** How opaque the layer is until the user moves its slider. */
internal const val COVERAGE_DEFAULT_OPACITY = 0.8f

private const val SNR_FLOOR = -20f
private const val SNR_CEILING = 5f
private const val OPAQUE = 0xFF000000.toInt()
private const val DOT_RADIUS_DP = 12
private const val DOT_STROKE_ALPHA = 0.6f

/**
 * The measured coverage: one dot for every position the radio heard directly, coloured by the SNR it was heard with
 * (the same colours as the track of a node, red weak to green strong).
 *
 * The dots are grouped by colour (a step of 1 dB each), and every group is one layer with a plain colour. This is on
 * purpose: a colour worked out by the map from a property of each dot did not draw anything on the phone, a layer with
 * a fixed colour always does.
 */
@Composable
internal fun CoverageLayer(state: CoverageState, opacity: Float) {
    if (!state.enabled || state.points.isEmpty()) return
    val groups =
        remember(state.points) {
            state.points.groupBy { point -> snrRgb(point.snr.coerceIn(SNR_FLOOR, SNR_CEILING).roundToInt().toFloat()) }
        }
    for ((rgb, points) in groups) {
        key(rgb) { CoverageDots(rgb = rgb, points = points, opacity = opacity) }
    }
}

/** The dots of one colour. */
@Composable
private fun CoverageDots(rgb: Int, points: List<CoveragePoint>, opacity: Float) {
    val source =
        rememberFeatureSource(points) {
            FeatureCollection(
                points.map { point ->
                    Feature<Point, JsonObject?>(
                        geometry = Point(Position(longitude = point.longitude, latitude = point.latitude)),
                        properties = null,
                    )
                },
            )
        }
    CircleLayer(
        id = "$COVERAGE_LAYER_ID-points-$rgb",
        source = source,
        color = const(Color(OPAQUE or rgb)),
        radius = const(DOT_RADIUS_DP.dp),
        strokeColor = const(Color.White.copy(alpha = DOT_STROKE_ALPHA)),
        strokeWidth = const(1.dp),
        opacity = const(opacity),
    )
}
