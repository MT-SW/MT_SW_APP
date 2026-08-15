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

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlin.math.floor

data class TileCoord(val zoom: Int, val x: Int, val y: Int)

/** Tile coordinates covering the lon/lat box at a single integer [zoom]. */
fun tilesInBounds(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double, zoom: Int): List<TileCoord> {
    val tilesPerAxis = 1 shl zoom
    val topLeft = lonLatToWorldPixel(minLon, maxLat, zoom.toDouble())
    val bottomRight = lonLatToWorldPixel(maxLon, minLat, zoom.toDouble())
    val minTileX = floor(topLeft.x / TILE_SIZE_PX).toInt().coerceIn(0, tilesPerAxis - 1)
    val maxTileX = floor(bottomRight.x / TILE_SIZE_PX).toInt().coerceIn(0, tilesPerAxis - 1)
    val minTileY = floor(topLeft.y / TILE_SIZE_PX).toInt().coerceIn(0, tilesPerAxis - 1)
    val maxTileY = floor(bottomRight.y / TILE_SIZE_PX).toInt().coerceIn(0, tilesPerAxis - 1)
    return buildList { for (x in minTileX..maxTileX) for (y in minTileY..maxTileY) add(TileCoord(zoom, x, y)) }
}

/** Tile coordinates covering the lon/lat box across every zoom level in [zoomRange] (inclusive). */
fun tilesInBounds(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double, zoomRange: IntRange): List<TileCoord> =
    zoomRange.flatMap { tilesInBounds(minLon, minLat, maxLon, maxLat, it) }

private const val OSM_TILE_URL_TEMPLATE = "https://tile.openstreetmap.org/%d/%d/%d.png"

/**
 * Downloads every tile in [tiles] not already present in [TileCache] into it, invoking [onProgress] after each attempt
 * (success or failure) with (completed, total). A failed tile is logged-and-skipped rather than aborting the whole
 * batch — a partial region download is still useful, matching the Android fork's tolerant `CacheManagerCallback`.
 */
suspend fun downloadTiles(httpClient: HttpClient, tiles: List<TileCoord>, onProgress: (completed: Int, total: Int) -> Unit) {
    var completed = 0
    for (tile in tiles) {
        if (!TileCache.hasTile(tile.zoom, tile.x, tile.y)) {
            runCatching {
                val response = httpClient.get(OSM_TILE_URL_TEMPLATE.format(tile.zoom, tile.x, tile.y))
                if (response.status.isSuccess()) {
                    TileCache.saveTile(tile.zoom, tile.x, tile.y, response.bodyAsBytes())
                }
            }
        }
        completed += 1
        onProgress(completed, tiles.size)
    }
}
