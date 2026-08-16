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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

private const val ZOOM_STEP = 1.2
private const val OSM_TILE_URL_TEMPLATE = "https://tile.openstreetmap.org/%d/%d/%d.png"

// Neutral "tile still loading" background (close to OSM's default land color). Without this, gaps before a tile
// loads show the surrounding Surface/theme background instead, which looks like black bars in the dark theme.
private val MAP_BACKGROUND_COLOR = Color(0xFFE0DFDB)

/** Builds the standard OSM tile URL for [tileX]/[tileY] at integer [zoom]. */
fun osmTileUrl(tileX: Int, tileY: Int, zoom: Int): String = OSM_TILE_URL_TEMPLATE.format(zoom, tileX, tileY)

/**
 * Renders an OSM tile map for [viewport]. Each tile is an ordinary [AsyncImage] positioned via offset, not a
 * hand-blitted Canvas bitmap, so tile loading/caching rides Coil's existing pipeline (already disk+memory cached in
 * Main.kt) and [content] (markers, overlays) sits above the tiles as normal composables with correct z-order.
 */
@Composable
fun OsmCanvasMap(
    viewport: MapViewportState,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    content: @Composable (toScreenOffset: (lon: Double, lat: Double) -> Offset, viewportSize: Offset) -> Unit =
        { _, _ ->
        },
) {
    var viewportSize by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier =
            modifier
                .clipToBounds()
                .background(MAP_BACKGROUND_COLOR)
                .onSizeChanged { viewportSize = Offset(it.width.toFloat(), it.height.toFloat()) }
                .then(
                    if (interactive) {
                        Modifier
                            .pointerInput(Unit) { detectDragGestures { _, dragAmount -> viewport.panBy(dragAmount) } }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Scroll) {
                                            val change = event.changes.first()
                                            val scrollY = change.scrollDelta.y
                                            if (scrollY != 0f) {
                                                val factor = if (scrollY < 0f) ZOOM_STEP else 1.0 / ZOOM_STEP
                                                viewport.zoomBy(factor, change.position, viewportSize)
                                            }
                                        }
                                    }
                                }
                            }
                    } else {
                        Modifier
                    },
                ),
    ) {
        if (viewportSize != Offset.Zero) {
            MapTiles(viewport, viewportSize)
            val topLeftWorld =
                WorldPixel(
                    viewport.centerWorldPixel.x - viewportSize.x / 2.0,
                    viewport.centerWorldPixel.y - viewportSize.y / 2.0,
                )
            content(
                { lon, lat ->
                    val world = lonLatToWorldPixel(lon, lat, viewport.zoom)
                    Offset((world.x - topLeftWorld.x).toFloat(), (world.y - topLeftWorld.y).toFloat())
                },
                viewportSize,
            )
        }
    }
}

@Composable
private fun MapTiles(viewport: MapViewportState, viewportSize: Offset) {
    val zoomInt = floor(viewport.zoom).toInt()
    val tilesPerAxis = 1 shl zoomInt
    // Tiles are always fetched at the nearest lower integer zoom, then scaled up to visually match the current
    // continuous (fractional) zoom, the same "smooth zoom between tile levels" technique every tile-based web map
    // uses. Without this, tile-grid math (integer zoomInt) and the camera's world-pixel space (continuous zoom, same
    // space markers use) drift apart as soon as the zoom isn't a whole number, which happens almost immediately
    // since each scroll tick changes zoom by a fractional step.
    val scaleFactor = 2.0.pow(viewport.zoom - zoomInt)
    val displayedTileSize = TILE_SIZE_PX * scaleFactor

    val topLeftWorld =
        WorldPixel(
            viewport.centerWorldPixel.x - viewportSize.x / 2.0,
            viewport.centerWorldPixel.y - viewportSize.y / 2.0,
        )
    val bottomRightWorld =
        WorldPixel(
            viewport.centerWorldPixel.x + viewportSize.x / 2.0,
            viewport.centerWorldPixel.y + viewportSize.y / 2.0,
        )

    // Convert the viewport's continuous-zoom world-pixel bounds down into zoomInt-space tile-grid units before
    // dividing by TILE_SIZE_PX, since the tile grid itself lives in zoomInt's (coarser) space.
    val firstTileX = floor(topLeftWorld.x / scaleFactor / TILE_SIZE_PX).toInt()
    val lastTileX = floor(bottomRightWorld.x / scaleFactor / TILE_SIZE_PX).toInt()
    val firstTileY = floor(topLeftWorld.y / scaleFactor / TILE_SIZE_PX).toInt()
    val lastTileY = floor(bottomRightWorld.y / scaleFactor / TILE_SIZE_PX).toInt()

    for (tileX in firstTileX..lastTileX) {
        // Wrap horizontally (the world repeats east-west); vertical tiles never wrap (poles).
        val wrappedX = ((tileX % tilesPerAxis) + tilesPerAxis) % tilesPerAxis
        for (tileY in firstTileY..lastTileY) {
            if (tileY < 0 || tileY >= tilesPerAxis) continue
            val screenX = (tileX * TILE_SIZE_PX * scaleFactor - topLeftWorld.x).roundToInt()
            val screenY = (tileY * TILE_SIZE_PX * scaleFactor - topLeftWorld.y).roundToInt()
            val localTile = TileCache.localTileFile(zoomInt, wrappedX, tileY)
            AsyncImage(
                model =
                    ImageRequest.Builder(LocalPlatformContext.current)
                        .data(localTile ?: osmTileUrl(wrappedX, tileY, zoomInt))
                        .crossfade(false)
                        .build(),
                contentDescription = null,
                // Fit (the AsyncImage default) preserves aspect ratio and can letterbox: leave a thin
                // background-colored gap on one axis, whenever the decoded bitmap isn't a pixel-perfect match for
                // the measured box. For a tile mosaic that must abut edge-to-edge, that shows as a seam at every
                // boundary. FillBounds forces an exact fill regardless of the source bitmap's exact pixel size.
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.offset { IntOffset(screenX, screenY) }.size(displayedTileSize.dp),
            )
        }
    }
}

// NOTE: if OSM tiles fail to load (blank/403), the shared Ktor HttpClient used by Coil may need a custom User-Agent
// header set (OSM tile-usage policy requires one). Flagged as an untested follow-up, check
// org.meshtastic.desktop's Coil/Ktor setup in Main.kt.