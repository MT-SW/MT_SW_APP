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
@file:Suppress("TooManyFunctions")

package org.meshtastic.feature.map.maplibre.layers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.koin.compose.koinInject
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.feature.map.trip.TripStore
import org.meshtastic.feature.map.trip.TripViewRequest

private const val KEY_STOP = "sid"
private const val MAX_DROPS = 60
private const val MILLIS_PER_SECOND = 1000
private const val NEAR_DEGREES = 0.002

/** MT_SW: the saved hikes switched on in the Hikes window: their routes, and a drop with the picture at each stop. */
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

    val stops =
        shown.flatMap { trip -> trip.stops.filter { it.photos.isNotEmpty() }.map { trip.id to it } }.take(MAX_DROPS)
    TripDrops(layerId = "trips-drops", stops = stops)
}

/**
 * MT_SW: the saved hikes' stops that fall on a node's position track (the own node's position history): a drop with
 * the picture at each, taken from the hikes by time and nearness to the track.
 */
@Composable
internal fun TripStopsOnTrack(track: List<Pair<Position, Int>>) {
    val store: TripStore = koinInject()
    val trips by store.trips.collectAsStateWithLifecycle()
    if (track.isEmpty() || trips.isEmpty()) return
    val from = track.minOf { it.second }
    val to = track.maxOf { it.second }
    val stops =
        trips
            .flatMap { trip -> trip.stops.filter { it.photos.isNotEmpty() }.map { trip.id to it } }
            .filter { (_, stop) ->
                val seconds = stop.t / MILLIS_PER_SECOND
                (from == 0 || seconds in from..to) &&
                    track.any { (p, _) ->
                        kotlin.math.abs(p.latitude - stop.lat) < NEAR_DEGREES &&
                            kotlin.math.abs(p.longitude - stop.lon) < NEAR_DEGREES
                    }
            }
            .take(MAX_DROPS)
    TripDrops(layerId = "trip-track-drops", stops = stops)
}

@Composable
private fun TripDrops(layerId: String, stops: List<Pair<String, org.meshtastic.feature.map.trip.TripStop>>) {
    if (stops.isEmpty()) return
    val painters = rememberDrops(stops)
    if (painters.isEmpty()) return
    val points =
        rememberFeatureSource(layerId, stops.map { it.second.id to it.second.lat to it.second.lon }) {
            FeatureCollection(
                stops.map { (_, stop) ->
                    Feature<Point, JsonObject?>(
                        geometry = Point(Position(longitude = stop.lon, latitude = stop.lat)),
                        properties = buildJsonObject { put(KEY_STOP, stop.id) },
                    )
                },
            )
        }
    val owner = stops.associate { it.second.id to it.first }
    SymbolLayer(
        id = layerId,
        source = points,
        iconImage =
        switch(
            input = feature[KEY_STOP].asString(const("")),
            *painters.map { (id, painter) -> case(id, image(painter, TRIP_DROP_SIZE)) }.toTypedArray(),
            fallback = image(painters.values.first(), TRIP_DROP_SIZE),
        ),
        // The tip of the drop is the place.
        iconAnchor = const(SymbolAnchor.Bottom),
        iconAllowOverlap = const(true),
        onClick = { features ->
            val id = features.firstOrNull()?.properties?.get(KEY_STOP)?.jsonPrimitive?.content
            val tripId = id?.let(owner::get)
            if (id != null && tripId != null) {
                TripViewRequest.stop.value = tripId to id
                ClickResult.Consume
            } else {
                ClickResult.Pass
            }
        },
    )
}

/** The painter of each drop. A picture arrives a moment after the drop does, and then the painter is replaced. */
@Composable
private fun rememberDrops(stops: List<Pair<String, org.meshtastic.feature.map.trip.TripStop>>): Map<String, Painter> {
    val store: TripStore = koinInject()
    val pictures = remember { mutableStateMapOf<String, ImageBitmap>() }
    stops.forEach { (tripId, stop) ->
        val photo = stop.photos.getOrNull(stop.shareIndex) ?: stop.photos.first()
        key(stop.id, photo.file) {
            LaunchedEffect(Unit) {
                val bitmap =
                    withContext(Dispatchers.Default) {
                        store.photoBytes(tripId, photo.file, small = true)?.let {
                            runCatching { it.decodeToImageBitmap() }.getOrNull()
                        }
                    }
                if (bitmap != null) pictures[stop.id] = bitmap
            }
        }
    }
    val cache = remember { mutableMapOf<String, Pair<ImageBitmap?, Painter>>() }
    return stops.associate { (_, stop) ->
        val picture = pictures[stop.id]
        val old = cache[stop.id]
        val painter =
            if (old != null && old.first === picture) {
                old.second
            } else {
                TripDropPainter(picture, TRIP_COLOR).also { cache[stop.id] = picture to it }
            }
        stop.id to painter
    }
}

private const val TRIP_WIDTH_DP = 4
private const val TRIP_OPACITY = 0.9f
private val TRIP_COLOR = Color(0xFF43A047)
