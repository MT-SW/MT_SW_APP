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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.ln

private const val MIN_ZOOM = 2.0
private const val MAX_ZOOM = 18.0
private const val LOG_2 = 0.6931471805599453 // ln(2.0) — named to avoid a raw magic-number literal
private const val SINGLE_NODE_ZOOM = 15.0
private const val BOUNDS_EDGE_PADDING_FRACTION = 0.1

data class GeoBounds(val minLon: Double, val minLat: Double, val maxLon: Double, val maxLat: Double)

/**
 * Mutable camera state for the desktop OSM map. [centerWorldPixel] is kept in world-pixel space (not lon/lat) so
 * panning is a simple pixel-delta subtraction — conversion to/from lon/lat only happens at the edges (initial load,
 * saving camera position, zoom-toward-cursor math).
 *
 * NOTE (recovery script): `fitToBounds()` and `visibleBounds()` below are both fully recovered verbatim from chat
 * history (offline tile-download feature — the "region" to download is just the live viewport re-read on pan/zoom, not
 * a separately drawn rectangle).
 */
class MapViewportState(initialLon: Double, initialLat: Double, initialZoom: Double) {
    var zoom by mutableStateOf(initialZoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set

    var centerWorldPixel by mutableStateOf(lonLatToWorldPixel(initialLon, initialLat, zoom))
        private set

    val centerLonLat: LonLat
        get() = worldPixelToLonLat(centerWorldPixel, zoom)

    /**
     * Pans the map by a screen-pixel [delta] (e.g. from a drag gesture) — dragging right moves the map's center left.
     */
    fun panBy(delta: Offset) {
        centerWorldPixel = WorldPixel(centerWorldPixel.x - delta.x, centerWorldPixel.y - delta.y)
    }

    /** Recenters on [lon]/[lat] without changing zoom. */
    fun centerOn(lon: Double, lat: Double) {
        centerWorldPixel = lonLatToWorldPixel(lon, lat, zoom)
    }

    /**
     * The lon/lat box currently visible for a viewport of [viewportSize] pixels, as (minLon, minLat, maxLon, maxLat).
     */
    fun visibleBounds(viewportSize: Offset): GeoBounds {
        val topLeftWorld =
            WorldPixel(centerWorldPixel.x - viewportSize.x / 2.0, centerWorldPixel.y - viewportSize.y / 2.0)
        val bottomRightWorld =
            WorldPixel(centerWorldPixel.x + viewportSize.x / 2.0, centerWorldPixel.y + viewportSize.y / 2.0)
        val topLeft = worldPixelToLonLat(topLeftWorld, zoom)
        val bottomRight = worldPixelToLonLat(bottomRightWorld, zoom)
        return GeoBounds(minLon = topLeft.lon, minLat = bottomRight.lat, maxLon = bottomRight.lon, maxLat = topLeft.lat)
    }

    /**
     * Centers on the midpoint of [minLon]/[minLat]/[maxLon]/[maxLat] and picks the highest zoom at which that whole
     * bounding box still fits inside [viewportSize] (with a small edge margin) — used to auto-frame all known node
     * positions on first launch, mirroring the Android fork's "always start centered on nodes" behavior.
     */
    fun fitToBounds(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double, viewportSize: Offset) {
        if (viewportSize.x <= 0f || viewportSize.y <= 0f) return
        val centerLon = (minLon + maxLon) / 2.0
        val centerLat = (minLat + maxLat) / 2.0

        if (minLon == maxLon && minLat == maxLat) {
            zoom = SINGLE_NODE_ZOOM.coerceIn(MIN_ZOOM, MAX_ZOOM)
            centerWorldPixel = lonLatToWorldPixel(centerLon, centerLat, zoom)
            return
        }

        // Project both corners at zoom 0 to get the box's pixel span there, then solve for the zoom at which that
        // span, doubling every zoom level, matches the (padded) viewport.
        val topLeftAtZ0 = lonLatToWorldPixel(minLon, maxLat, 0.0)
        val bottomRightAtZ0 = lonLatToWorldPixel(maxLon, minLat, 0.0)
        val spanXAtZ0 = bottomRightAtZ0.x - topLeftAtZ0.x
        val spanYAtZ0 = bottomRightAtZ0.y - topLeftAtZ0.y
        val paddedWidth = viewportSize.x * (1.0 - BOUNDS_EDGE_PADDING_FRACTION * 2.0)
        val paddedHeight = viewportSize.y * (1.0 - BOUNDS_EDGE_PADDING_FRACTION * 2.0)

        val zoomForWidth = if (spanXAtZ0 > 0.0) ln(paddedWidth / spanXAtZ0) / LOG_2 else MAX_ZOOM
        val zoomForHeight = if (spanYAtZ0 > 0.0) ln(paddedHeight / spanYAtZ0) / LOG_2 else MAX_ZOOM

        zoom = minOf(zoomForWidth, zoomForHeight).coerceIn(MIN_ZOOM, MAX_ZOOM)
        centerWorldPixel = lonLatToWorldPixel(centerLon, centerLat, zoom)
    }

    /**
     * Zooms by [factor] (>1 zooms in, <1 zooms out), keeping the world point currently under [focalPoint] (screen
     * pixels relative to the viewport's top-left, size [viewportSize]) visually fixed — the standard "zoom toward
     * cursor/pinch center" feel.
     */
    fun zoomBy(factor: Double, focalPoint: Offset, viewportSize: Offset) {
        val newZoom = (zoom + ln(factor) / LOG_2).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (newZoom == zoom) return

        val topLeftOld =
            WorldPixel(centerWorldPixel.x - viewportSize.x / 2.0, centerWorldPixel.y - viewportSize.y / 2.0)
        val focalWorldOld = WorldPixel(topLeftOld.x + focalPoint.x, topLeftOld.y + focalPoint.y)
        val focalLonLat = worldPixelToLonLat(focalWorldOld, zoom)

        val focalWorldNew = lonLatToWorldPixel(focalLonLat.lon, focalLonLat.lat, newZoom)
        zoom = newZoom
        centerWorldPixel =
            WorldPixel(
                focalWorldNew.x + viewportSize.x / 2.0 - focalPoint.x,
                focalWorldNew.y + viewportSize.y / 2.0 - focalPoint.y,
            )
    }
}

/** Remembers a [MapViewportState] seeded from a saved camera position, or a sensible world-view default. */
@Composable
fun rememberMapViewportState(
    initialLon: Double = DEFAULT_LON,
    initialLat: Double = DEFAULT_LAT,
    initialZoom: Double = DEFAULT_ZOOM,
): MapViewportState = remember { MapViewportState(initialLon, initialLat, initialZoom) }

private const val DEFAULT_LON = 0.0
private const val DEFAULT_LAT = 20.0
private const val DEFAULT_ZOOM = 3.0
