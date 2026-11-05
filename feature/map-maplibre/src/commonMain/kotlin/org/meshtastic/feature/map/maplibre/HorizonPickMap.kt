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
package org.meshtastic.feature.map.maplibre

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.meshtastic.core.ui.util.HorizonMapNode
import org.meshtastic.feature.map.maplibre.component.customRasterBasemaps
import org.meshtastic.feature.map.maplibre.component.rememberBasemapSelection
import org.meshtastic.feature.map.maplibre.geojson.MapChipKey
import org.meshtastic.feature.map.maplibre.geojson.NodeFeatureKeys
import org.meshtastic.feature.map.maplibre.geojson.circlePolygon
import org.meshtastic.feature.map.maplibre.geojson.featureValue
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.feature.map.maplibre.geojson.toCssHex
import org.meshtastic.feature.map.maplibre.layers.MapChipLayer
import org.maplibre.spatialk.geojson.Position as GeoPosition

/**
 * The map the terrain panorama's "move" mode is picked on: the app's own basemap with the nodes drawn as on the main
 * map, and a fixed cross in the middle. When the user lets go of the map, [onCenterChanged] gets the point under the
 * cross. Opens on [centerLatitude] / [centerLongitude]; when those later change to somewhere else than the cross (the
 * way back to the user's own node), the camera follows.
 */
@Composable
fun MapLibreHorizonPickMap(
    centerLatitude: Double,
    centerLongitude: Double,
    radiusKm: Int,
    nodes: List<HorizonMapNode>,
    onCenterChanged: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
    customBasemaps: @Composable () -> List<org.meshtastic.feature.map.maplibre.style.Basemap.Raster> = {
        customRasterBasemaps()
    },
) {
    val basemaps = rememberBasemapSelection(customBasemaps()) ?: return
    val located = remember(nodes) { nodes.filter { it.latitude != 0.0 || it.longitude != 0.0 } }
    val chips =
        remember(located) {
            located.map {
                MapChipKey(label = it.shortName.ifBlank { "?" }, background = it.background, foreground = it.foreground)
            }
        }
    val centre = GeoPosition(longitude = centerLongitude, latitude = centerLatitude)

    val mapState =
        rememberSecondaryMapState(
            basemaps,
            CameraPosition(center = centre, zoom = zoomForRadius(radiusKm, centerLatitude)),
        ) {
            val source =
                rememberFeatureSource(located) {
                    FeatureCollection(
                        located.mapIndexed { index, node ->
                            Feature<Point, JsonObject?>(
                                geometry = Point(GeoPosition(longitude = node.longitude, latitude = node.latitude)),
                                properties =
                                buildJsonObject {
                                    put(NodeFeatureKeys.BACKGROUND, node.background.toCssHex())
                                    put(NodeFeatureKeys.CHIP, chips[index].featureValue())
                                },
                            )
                        },
                    )
                }
            // As on the main map: the ground circle of a node that hides its exact position, under everything else.
            val circles = located.filter { it.precisionM != null }
            val circleSource =
                rememberFeatureSource(circles) {
                    FeatureCollection(
                        circles.map { node ->
                            Feature<Polygon, JsonObject?>(
                                geometry = circlePolygon(node.latitude, node.longitude, node.precisionM ?: 0.0),
                                properties =
                                buildJsonObject { put(NodeFeatureKeys.BACKGROUND, node.background.toCssHex()) },
                            )
                        },
                    )
                }
            FillLayer(
                id = "horizon-node-precision",
                source = circleSource,
                color = feature[NodeFeatureKeys.BACKGROUND].convertToColor(const(Color.Gray)),
                opacity = const(PRECISION_OPACITY),
                outlineColor = feature[NodeFeatureKeys.BACKGROUND].convertToColor(const(Color.Gray)),
            )
            // As on the main map: the node's own dot under its chip.
            CircleLayer(
                id = "horizon-node-dot",
                source = source,
                color = feature[NodeFeatureKeys.BACKGROUND].convertToColor(const(Color.Gray)),
                radius = const(NODE_DOT_RADIUS.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
            )
            MapChipLayer(id = "horizon-node-chip", source = source, chips = chips)
        }

    val latestOnCenterChanged by rememberUpdatedState(onCenterChanged)
    val givenCentre by rememberUpdatedState(centerLatitude to centerLongitude)
    // Reports the point under the cross once the camera has stopped, not every frame of a drag.
    LaunchedEffect(mapState) {
        snapshotFlow { mapState.cameraPosition.takeUnless { mapState.isCameraMoving } }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { position ->
                val c = position.center
                if (!sameSpot(c.latitude, c.longitude, givenCentre.first, givenCentre.second)) {
                    latestOnCenterChanged(c.latitude, c.longitude)
                }
            }
    }
    // A new place that is not where the cross already is (the way back to the user's own node) moves the camera.
    LaunchedEffect(centerLatitude, centerLongitude) {
        val now = mapState.cameraPosition.center
        if (!sameSpot(now.latitude, now.longitude, centerLatitude, centerLongitude)) {
            mapState.animateCamera(CameraUpdate(center = centre), CameraAnimation.Ease.Standard)
        }
    }

    Box(modifier = modifier) {
        SecondaryMapSurface(mapState = mapState, basemaps = basemaps)
        SecondaryMapChrome(mapState = mapState, basemaps = basemaps)
        Canvas(Modifier.align(Alignment.Center).size(CROSS_SIZE.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color.White, radius = size.width / 2f, center = c, style = Stroke(3f))
            drawCircle(Color.Black, radius = size.width / 2f - 3f, center = c, style = Stroke(1.5f))
            drawLine(Color.Black, Offset(c.x, 0f), Offset(c.x, size.height), strokeWidth = 2f)
            drawLine(Color.Black, Offset(0f, c.y), Offset(size.width, c.y), strokeWidth = 2f)
        }
    }
}

/** About the zoom at which a circle of [radiusKm] fills a phone-sized map. */
private fun zoomForRadius(radiusKm: Int, latitude: Double): Double {
    val metres = radiusKm.coerceAtLeast(1) * METRES_PER_KM
    val cosLat = kotlin.math.cos(latitude * kotlin.math.PI / HALF_TURN).coerceAtLeast(MIN_COS)
    val zoom =
        kotlin.math.ln(EQUATOR_METRES_AT_ZOOM_0 * cosLat * FIT_PIXELS / (metres * 2.0 * TILE_PIXELS)) /
            kotlin.math.ln(2.0)
    return zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
}

private fun sameSpot(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean =
    kotlin.math.abs(lat1 - lat2) < SAME_SPOT_DEG && kotlin.math.abs(lon1 - lon2) < SAME_SPOT_DEG

private const val NODE_DOT_RADIUS = 4
private const val CROSS_SIZE = 28
private const val METRES_PER_KM = 1000.0
private const val HALF_TURN = 180.0
private const val MIN_COS = 0.01
private const val EQUATOR_METRES_AT_ZOOM_0 = 40_075_016.0
private const val FIT_PIXELS = 700.0
private const val TILE_PIXELS = 256.0
private const val MIN_ZOOM = 4.0
private const val MAX_ZOOM = 14.0
private const val SAME_SPOT_DEG = 1e-6

private const val PRECISION_OPACITY = 0.15f
