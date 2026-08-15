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

import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/** Standard OSM/Web Mercator tile size in pixels. */
const val TILE_SIZE_PX = 256.0

private const val DEGREES_IN_HALF_TURN = 180.0
private const val DEGREES_IN_FULL_TURN = 360.0

/**
 * Fractional world-pixel coordinate at [zoom] for a given lat/lon, using the standard Web Mercator (EPSG:3857)
 * projection that OSM/most tile servers use. "World pixels" means the whole map at this zoom is
 * `TILE_SIZE_PX * 2^zoom` pixels wide/tall — tile (x, y) covers world-pixel range
 * `[x*TILE_SIZE_PX, (x+1)*TILE_SIZE_PX)`.
 */
fun lonLatToWorldPixel(lon: Double, lat: Double, zoom: Double): WorldPixel {
    val scale = TILE_SIZE_PX * 2.0.pow(zoom)
    val x = (lon + DEGREES_IN_HALF_TURN) / DEGREES_IN_FULL_TURN * scale
    val latRad = lat * PI / DEGREES_IN_HALF_TURN
    val y = (1.0 - asinh(tan(latRad)) / PI) / 2.0 * scale
    return WorldPixel(x, y)
}

/** Inverse of [lonLatToWorldPixel] — recovers lat/lon from a world-pixel coordinate at [zoom]. */
fun worldPixelToLonLat(worldPixel: WorldPixel, zoom: Double): LonLat {
    val scale = TILE_SIZE_PX * 2.0.pow(zoom)
    val lon = worldPixel.x / scale * DEGREES_IN_FULL_TURN - DEGREES_IN_HALF_TURN
    val n = PI - 2.0 * PI * worldPixel.y / scale
    val lat = DEGREES_IN_HALF_TURN / PI * atan(sinh(n))
    return LonLat(lon, lat)
}

/** A point in the whole-map pixel space at a given zoom level (see [lonLatToWorldPixel]). */
data class WorldPixel(val x: Double, val y: Double)

data class LonLat(val lon: Double, val lat: Double)

/** Integer tile coordinate, plus the zoom it was resolved at. */
data class TileId(val x: Int, val y: Int, val zoom: Int)

private const val EARTH_CIRCUMFERENCE_METERS = 40075016.686

/**
 * Real-world meters per on-screen unit at [zoom] and [latDegrees] — Web Mercator distorts scale by `cos(latitude)`, so
 * this must be recomputed per-node, not treated as a single global constant. Note: "screen unit" here means the same
 * unit as the rest of this file's world-pixel math, which [OsmCanvasMap] renders 1:1 as dp (`TILE_SIZE_PX.dp`) rather
 * than raw device pixels.
 */
fun metersPerScreenUnit(latDegrees: Double, zoom: Double): Double {
    val latRad = latDegrees * PI / DEGREES_IN_HALF_TURN
    return EARTH_CIRCUMFERENCE_METERS * cos(latRad) / (TILE_SIZE_PX * 2.0.pow(zoom))
}
