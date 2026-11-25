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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.map.MapState
import org.meshtastic.core.ui.component.LocalFloatingBarInset
import org.meshtastic.feature.map.component.MapZoomControls
import org.meshtastic.feature.map.maplibre.ZOOM_STEP
import org.meshtastic.feature.map.maplibre.style.Basemap
import org.meshtastic.feature.map.maplibre.style.zoomRange
import org.meshtastic.feature.map.maplibre.zoomBy

/** Inset from the trailing edge, matching the toolbar's own inset from the top. */
private const val ZOOM_INSET = 8

/**
 * Lift above the credit strip (up to three lines) and the attribution button, which share this corner; the propagation
 * legend is lifted by the same amount. More than the cards need on the other side, where only the 23dp wordmark sits.
 *
 * The button is there either way — the small maps only collapse its credit, see [CollapsedAttributionOrnaments] — so
 * this clears it whether it is expanded or not.
 */
internal const val ZOOM_BOTTOM_INSET = 88

/** The pitch the 2D/3D button tilts the map to (the original iPhone app uses the same). */
private const val TILT_3D_DEGREES = 55.0

/** Above this pitch the map counts as tilted, so the button offers the way back to flat. */
private const val TILTED_ABOVE_DEGREES = 1.0

/**
 * Zoom controls in the lower trailing corner of a map, where Google Maps draws its own.
 *
 * Every MapLibre surface calls this, so the control sits in the same place on all of them. Written as a [BoxScope]
 * extension because the placement is the point: a caller that had to align it itself would eventually align it
 * somewhere else.
 *
 * [centered] is for a map too short for the lifted corner (see [MapZoom] callers). Lifted clear of the wordmark and
 * attribution button along the bottom edge, which every map shows. See [meshMapOrnaments] for why the main map carries
 * the credit outright and the small maps behind the button.
 */
@Composable
internal fun BoxScope.MapZoom(
    mapState: MapState,
    basemap: Basemap,
    centered: Boolean = false,
    tiltSwitch: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val zoomRange = basemap.zoomRange()
    val tilted = mapState.cameraPosition.pitch > TILTED_ABOVE_DEGREES

    MapZoomControls(
        compact = centered,
        onZoomIn = { scope.launch { mapState.zoomBy(ZOOM_STEP, zoomRange) } },
        onZoomOut = { scope.launch { mapState.zoomBy(-ZOOM_STEP, zoomRange) } },
        onToggleTilt =
        if (tiltSwitch) {
            {
                val target = if (tilted) 0.0 else TILT_3D_DEGREES
                Logger.i { "Mapa: przycisk widoku 3D, pochylenie ${mapState.cameraPosition.pitch} -> $target" }
                scope.launch { mapState.animateCamera(CameraUpdate(pitch = target), CameraAnimation.Ease.Standard) }
            }
        } else {
            null
        },
        tilted = tilted,
        modifier =
        if (centered) {
            // A map too short for the lifted corner (the thumbnail in the node details): a smaller pair in the
            // middle of the
            // trailing edge: small enough to clear the scale (top corner) and the credit button (bottom corner).
            Modifier.align(Alignment.CenterEnd).padding(end = ZOOM_INSET.dp)
        } else {
            Modifier.align(Alignment.BottomEnd)
                .padding(end = ZOOM_INSET.dp, bottom = ZOOM_BOTTOM_INSET.dp + LocalFloatingBarInset.current)
        },
    )
}
