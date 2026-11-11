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
@file:Suppress("MagicNumber")

package org.meshtastic.feature.map.maplibre.layers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.RasterLayer
import org.maplibre.compose.sources.rememberImageSource
import org.maplibre.compose.util.PositionQuad
import org.maplibre.compose.util.PreparedImage
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.feature.map.propagation.GeoBounds
import org.meshtastic.feature.map.propagation.PropagationColors
import org.meshtastic.feature.map.propagation.PropagationKind
import org.meshtastic.feature.map.propagation.PropagationRegion
import org.meshtastic.feature.map.propagation.PropagationState
import org.meshtastic.feature.map.propagation.TropoGrid
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

/** The layer id, also the key of its opacity setting. */
internal const val PROPAGATION_LAYER_ID = "propagation"

/** How opaque the layer is until the user moves its slider. */
internal const val PROPAGATION_DEFAULT_OPACITY = 0.6f

private const val BITMAP_SIZE = 160
private const val OUTLINE_ALPHA = 0.7f

/**
 * The propagation forecast: one picture per region, each over the area it was downloaded for.
 *
 * Regions may overlap. The one with the finer grid is drawn on top, and the coarser pictures are left empty where a
 * finer one covers them, so the opacity is not added up twice. The pictures are drawn off the main thread and only
 * again when a forecast, the hour or the kind changes, never per map frame.
 */
@Composable
internal fun PropagationLayer(state: PropagationState, opacity: Float) {
    if (!state.enabled) return
    // Coarsest first, finest last: later layers are drawn above earlier ones.
    val ordered = state.regions.sortedByDescending { it.cellArea }
    ordered.forEachIndexed { index, region ->
        key(region.id) {
            val finer = ordered.subList(index + 1, ordered.size).map { it.grid.bounds }
            RegionLayer(region, finer, state.hoursAhead, state.kind, opacity)
        }
    }
    // With several regions their edges are drawn too, so it is clear where each one lies and where they overlap.
    if (ordered.size > 1) RegionOutlines(ordered)
}

@Composable
private fun RegionOutlines(regions: List<PropagationRegion>) {
    val source =
        rememberFeatureSource(regions.map { it.grid.bounds }) {
            FeatureCollection(
                regions.map { region ->
                    val b = region.grid.bounds
                    Feature<LineString, JsonObject?>(
                        geometry =
                        LineString(
                            listOf(
                                Position(longitude = b.west, latitude = b.south),
                                Position(longitude = b.east, latitude = b.south),
                                Position(longitude = b.east, latitude = b.north),
                                Position(longitude = b.west, latitude = b.north),
                                Position(longitude = b.west, latitude = b.south),
                            ),
                        ),
                        properties = null,
                    )
                },
            )
        }
    LineLayer(
        id = "$PROPAGATION_LAYER_ID-outline",
        source = source,
        color = const(Color.White.copy(alpha = OUTLINE_ALPHA)),
        width = const(1.dp),
    )
}

@Suppress("InjectDispatcher")
@Composable
private fun RegionLayer(
    region: PropagationRegion,
    finer: List<GeoBounds>,
    hoursAhead: Int,
    kind: PropagationKind,
    opacity: Float,
) {
    val grid = region.grid
    val hour = (grid.hourAt(nowMillis) + hoursAhead).coerceAtMost(grid.hours - 1)
    val image by
        produceState<PreparedImage?>(initialValue = null, grid, hour, kind, finer) {
            value =
                withContext(Dispatchers.Default) {
                    PreparedImage.fromBitmap(renderPropagation(grid, hour, kind, finer))
                }
        }
    val prepared = image ?: return
    val quad =
        remember(grid.bounds) {
            val b = grid.bounds
            PositionQuad(
                topLeft = Position(longitude = b.west, latitude = b.north),
                topRight = Position(longitude = b.east, latitude = b.north),
                bottomRight = Position(longitude = b.east, latitude = b.south),
                bottomLeft = Position(longitude = b.west, latitude = b.south),
            )
        }
    val source = rememberImageSource(quad, prepared)
    RasterLayer(id = "$PROPAGATION_LAYER_ID-${region.id}", source = source, opacity = const(opacity))
}

private fun mercatorY(latDeg: Double): Double = ln(tan(PI / 4 + latDeg * PI / 360))

private fun latFromMercator(y: Double): Double = (2 * atan(exp(y)) - PI / 2) * 180 / PI

/**
 * Paints [grid] for [hour]: rows follow the map's own projection, so the picture lines up with the tiles. Places inside
 * one of the [finer] areas are left empty, because a finer picture is drawn there.
 */
private fun renderPropagation(
    grid: TropoGrid,
    hour: Int,
    kind: PropagationKind,
    finer: List<GeoBounds>,
): ImageBitmap {
    val bitmap = ImageBitmap(BITMAP_SIZE, BITMAP_SIZE)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    val b = grid.bounds
    val yTop = mercatorY(b.north)
    val yBottom = mercatorY(b.south)
    for (py in 0 until BITMAP_SIZE) {
        val lat = latFromMercator(yTop + (yBottom - yTop) * (py + 0.5) / BITMAP_SIZE)
        for (px in 0 until BITMAP_SIZE) {
            val lon = b.west + b.lonSpan * (px + 0.5) / BITMAP_SIZE
            if (finer.any { lat in it.south..it.north && lon in it.west..it.east }) continue
            val argb = PropagationColors.argb(kind, grid.sample(hour, lat, lon).toDouble())
            if (argb ushr 24 == 0) continue
            paint.color = Color(argb)
            canvas.drawRect(Rect(px.toFloat(), py.toFloat(), px + 1f, py + 1f), paint)
        }
    }
    return bitmap
}
