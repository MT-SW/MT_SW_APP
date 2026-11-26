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
import org.meshtastic.feature.map.trip.TripStore

/** MT_SW: the saved hikes switched on in the Hikes window: their routes, and a dot where a picture was taken. */
@Composable
internal fun TripsLayer() {
    val store: TripStore = koinInject()
    val trips by store.trips.collectAsStateWithLifecycle()
    val visible by store.visibleIds.collectAsStateWithLifecycle()
    val shown = trips.filter { it.id in visible && it.points.size >= 2 }
    if (shown.isEmpty()) return

    val lines =
        rememberFeatureSource(shown.map { it.id }) {
            FeatureCollection(
                shown.map { trip ->
                    Feature<LineString, JsonObject?>(
                        geometry = LineString(trip.points.map { Position(longitude = it.lon, latitude = it.lat) }),
                        properties = null,
                    )
                },
            )
        }
    LineLayer(
        id = "trips-line",
        source = lines,
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
        color = const(TRIP_COLOR),
        width = const(TRIP_WIDTH_DP.dp),
        opacity = const(TRIP_OPACITY),
    )
    val photos = shown.flatMap { it.photos }
    if (photos.isNotEmpty()) {
        val points =
            rememberFeatureSource(photos.size, shown.map { it.id }) {
                FeatureCollection(
                    photos.map { photo ->
                        Feature<Point, JsonObject?>(
                            geometry = Point(Position(longitude = photo.lon, latitude = photo.lat)),
                            properties = null,
                        )
                    },
                )
            }
        CircleLayer(
            id = "trips-photos",
            source = points,
            color = const(TRIP_COLOR),
            radius = const(PHOTO_RADIUS_DP.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp),
        )
    }
}

private const val TRIP_WIDTH_DP = 4
private const val TRIP_OPACITY = 0.9f
private const val PHOTO_RADIUS_DP = 7
private val TRIP_COLOR = Color(0xFF43A047)
