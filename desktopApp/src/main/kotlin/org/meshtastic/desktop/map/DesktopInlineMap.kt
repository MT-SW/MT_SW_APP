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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlin.math.floor
import kotlin.math.roundToInt
import org.meshtastic.core.model.Node

private const val INLINE_MAP_ZOOM = 15
private const val INLINE_MARKER_RADIUS_PX = 7f
private val INLINE_MARKER_COLOR = Color(0xFF2196F3)
private val INLINE_MAP_BACKGROUND_COLOR = Color(0xFFE0DFDB)

/**
 * Static, non-interactive single-node preview map, desktop implementation of
 * [org.meshtastic.core.ui.util.LocalInlineMapProvider], used by the Position card embedded directly in the main Node
 * Detail screen.
 *
 * Deliberately does NOT reuse [OsmCanvasMap] (each tile as its own positioned [coil3.compose.AsyncImage]). Confirmed
 * empirically (plain colored Boxes reproduced the same gap; content scale, density, clipping, and GPU render-backend
 * fixes all failed to close it) that Compose Desktop fails to seamlessly abut several independently placed/sized
 * composables here, a known upstream rendering issue: github.com/JetBrains/compose-multiplatform/issues/3917. This
 * component sidesteps that entirely by fetching each tile's bitmap via Coil directly (still hitting Coil's normal
 * memory/disk cache) and drawing all of them, plus the marker, in a single [Canvas] with direct [drawImage] calls -
 * one paint operation, so there is nothing left for the layout engine to misalign.
 */
@Composable
fun DesktopInlineMap(node: Node, modifier: Modifier = Modifier) {
    var viewportSize by remember { mutableStateOf(Offset.Zero) }
    val tileBitmaps = remember { mutableStateMapOf<TileId, ImageBitmap>() }
    val context = LocalPlatformContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }

    val tilesPerAxis = 1 shl INLINE_MAP_ZOOM
    val centerWorld =
        remember(node.longitude, node.latitude) {
            lonLatToWorldPixel(node.longitude, node.latitude, INLINE_MAP_ZOOM.toDouble())
        }

    Box(
        modifier =
            modifier
                .background(INLINE_MAP_BACKGROUND_COLOR)
                .onSizeChanged { viewportSize = Offset(it.width.toFloat(), it.height.toFloat()) },
    ) {
        if (viewportSize != Offset.Zero) {
            val topLeftWorld =
                WorldPixel(centerWorld.x - viewportSize.x / 2.0, centerWorld.y - viewportSize.y / 2.0)
            val bottomRightWorld =
                WorldPixel(centerWorld.x + viewportSize.x / 2.0, centerWorld.y + viewportSize.y / 2.0)

            val firstTileX = floor(topLeftWorld.x / TILE_SIZE_PX).toInt()
            val lastTileX = floor(bottomRightWorld.x / TILE_SIZE_PX).toInt()
            val firstTileY = floor(topLeftWorld.y / TILE_SIZE_PX).toInt()
            val lastTileY = floor(bottomRightWorld.y / TILE_SIZE_PX).toInt()

            val neededTiles =
                remember(firstTileX, lastTileX, firstTileY, lastTileY) {
                    buildList {
                        for (tileX in firstTileX..lastTileX) {
                            val wrappedX = ((tileX % tilesPerAxis) + tilesPerAxis) % tilesPerAxis
                            for (tileY in firstTileY..lastTileY) {
                                if (tileY < 0 || tileY >= tilesPerAxis) continue
                                add(TileId(wrappedX, tileY, INLINE_MAP_ZOOM))
                            }
                        }
                    }
                }

            for (id in neededTiles) {
                key(id) {
                    LaunchedEffect(id) {
                        if (id !in tileBitmaps) {
                            val localTile = TileCache.localTileFile(id.zoom, id.x, id.y)
                            val request =
                                ImageRequest.Builder(context)
                                    .data(localTile ?: osmTileUrl(id.x, id.y, id.zoom))
                                    .build()
                            val result = imageLoader.execute(request)
                            if (result is SuccessResult) {
                                tileBitmaps[id] = result.image.toBitmap().asComposeImageBitmap()
                            }
                        }
                    }
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                for (tileX in firstTileX..lastTileX) {
                    val wrappedX = ((tileX % tilesPerAxis) + tilesPerAxis) % tilesPerAxis
                    for (tileY in firstTileY..lastTileY) {
                        if (tileY < 0 || tileY >= tilesPerAxis) continue
                        val bitmap = tileBitmaps[TileId(wrappedX, tileY, INLINE_MAP_ZOOM)] ?: continue
                        val screenX = (tileX * TILE_SIZE_PX - topLeftWorld.x).roundToInt()
                        val screenY = (tileY * TILE_SIZE_PX - topLeftWorld.y).roundToInt()
                        val tileSizePx = TILE_SIZE_PX.roundToInt()
                        drawImage(
                            image = bitmap,
                            dstOffset = IntOffset(screenX, screenY),
                            dstSize = IntSize(tileSizePx, tileSizePx),
                        )
                    }
                }

                val markerOffset =
                    Offset(
                        (centerWorld.x - topLeftWorld.x).toFloat(),
                        (centerWorld.y - topLeftWorld.y).toFloat(),
                    )
                drawCircle(color = INLINE_MARKER_COLOR, radius = INLINE_MARKER_RADIUS_PX, center = markerOffset)
            }
        }
    }
}