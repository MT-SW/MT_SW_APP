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
package org.meshtastic.feature.map.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.component.MAP_GLASS_MIN_ALPHA
import org.meshtastic.core.ui.component.StyledSurface
import org.meshtastic.core.ui.theme.rememberUiOptions
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.zoom_in
import org.meshtastic.core.resources.zoom_out
import org.meshtastic.core.ui.icon.Add
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Remove
import org.meshtastic.core.ui.theme.monochromeLayer

/**
 * Zoom in and out, stacked, for the corner of a map.
 *
 * Where Google Maps puts its own: `MapUiSettings(zoomControlsEnabled = true)` draws a vertical pair against the lower
 * trailing edge, clear of the top of the map, and every Google-flavor map surface in this app turns it on. MapLibre
 * publishes no zoom ornament at all, so its maps draw this instead — placed the same way, so the two flavors feel alike
 * and the reachable half of a phone screen is where the frequent control lives.
 *
 * Separate from [MapControlsOverlay] on purpose. That toolbar holds the occasional controls — filter, basemap, layers —
 * and belongs at the top; zoom belongs where the thumb is.
 */
@Composable
fun MapZoomControls(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier = Modifier) {
    val style = rememberUiOptions().element(UiOptions.GROUP_MAP)
    val buttons: @Composable () -> Unit = {
        MapButton(
            icon = MeshtasticIcons.Add,
            contentDescription = stringResource(Res.string.zoom_in),
            onClick = onZoomIn,
            modifier = if (style.look == ElementLook.FLAT) Modifier else Modifier.size(ZOOM_BUTTON_DP.dp),
        )
        MapButton(
            icon = MeshtasticIcons.Remove,
            contentDescription = stringResource(Res.string.zoom_out),
            onClick = onZoomOut,
            modifier = if (style.look == ElementLook.FLAT) Modifier else Modifier.size(ZOOM_BUTTON_DP.dp),
        )
    }
    if (style.look == ElementLook.FLAT) {
        VerticalFloatingToolbar(
            expanded = true,
            // Grey in the black-and-white themes, like the other buttons over the map.
            modifier = modifier.monochromeLayer(),
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        ) {
            buttons()
        }
    } else {
        // Same see-through look as the toolbar at the top (the map must show through, so no solid plate).
        StyledSurface(
            style = style,
            shape = CircleShape,
            modifier = modifier,
            baseColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            floating = true,
            fallbackMinAlpha = MAP_GLASS_MIN_ALPHA,
        ) {
            Column(modifier = Modifier.padding(ZOOM_PADDING_DP.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CompositionLocalProvider(LocalMapButtonInToolbar provides true) { buttons() }
            }
        }
    }
}

/** Size of one zoom button and the room around the pair (bigger than a toolbar icon: it is pressed often, by thumb). */
private const val ZOOM_BUTTON_DP = 52
private const val ZOOM_PADDING_DP = 4
