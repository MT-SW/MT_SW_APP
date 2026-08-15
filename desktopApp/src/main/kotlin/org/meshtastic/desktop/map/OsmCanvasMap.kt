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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

private const val ZOOM_STEP = 1.2
private const val OSM_TILE_URL_TEMPLATE = "https://tile.openstreetmap.org/%d/%d/%d.png"

/** Builds the standard OSM tile URL for [tileX]/[tileY] at integer [zoom]. */
private fun osmTileUrl(tileX: Int, tileY: Int, zoom: Int): String = OSM_TILE_URL_TEMPLATE.format(zoom, tileX, tileY)

/**
 * Renders an OSM tile map for [viewport]. Each tile is an ordinary [AsyncImage] positioned via offset — not a
 * hand-blitted Canvas bitmap — so tile loading/caching rides Coil's existing pipeline (already disk+memory cached in
 * Main.kt) and [content] (markers, overlays) sits above the tiles as normal composables with correct z-order.
 *
 * NOTE (recovery script): the `content` lambda signature (toScreenOffset + viewportSize) and the fractional-zoom
 * tile/marker drift fix are both fully recovered and applied below, verbatim from chat history. One later change is
 * NOT included here: the offline tile-cache feature changed `MapTiles`' tile `AsyncImage` to check a local cached
 * tile file first — `TileCache.get(tileX, tileY, zoomInt)` — and use that `File` as Coil's `model` instead of the
 * network URL when present. See TileCache.kt in this bundle and re-wire that check into the `AsyncImage` call in
 * `MapTiles()` below if you want offline caching back.
 */
@Composable
fun OsmCanvasMap(
    viewport: MapViewportState,
    modifier: Modifier = Modifier,
    content: @Composable (toScreenOffset: (lon: Double, lat: Double) -> Offset, viewportSize: Offset) -> Unit = { _, _ -> },
) {
    var viewportSize by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier =
        modifier
            .onSizeChanged { viewportSize = Offset(it.width.toFloat(), it.height.toFloat()) }
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
            },
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
    // continuous (fractional) zoom — the same "smooth zoom between tile levels" technique every tile-based web map
    // uses. Without this, tile-grid math (integer zoomInt) and the camera's world-pixel space (continuous zoom, same
    // space markers use) drift apart as soon as the zoom isn't a whole number — which happens almost immediately
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
                model = localTile ?: osmTileUrl(wrappedX, tileY, zoomInt),
                contentDescription = null,
                modifier = Modifier.offset { IntOffset(screenX, screenY) }.size(displayedTileSize.dp),
            )
        }
    }
}

// NOTE (recovery script): if OSM tiles fail to load (blank/403), the shared Ktor HttpClient used by Coil likely
// needs a custom User-Agent header set (OSM tile-usage policy requires one) — this was flagged as an untested
// follow-up in the original session, check org.meshtastic.desktop's Coil/Ktor setup in Main.kt.
