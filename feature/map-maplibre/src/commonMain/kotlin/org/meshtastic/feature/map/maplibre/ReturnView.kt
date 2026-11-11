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

import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MapState

/**
 * The view the map had before it flew in on a node or a cluster, so a tap on the empty map can take the user back to it
 * (the view they had on entering the map). Only the first view is kept while zooming in step by step.
 */
internal object ReturnView {
    private val saved = mutableMapOf<MapState, CameraPosition>()

    fun remember(state: MapState) {
        if (state !in saved) saved[state] = state.cameraPosition
    }

    /** The view the map settled on when it opened: where the Map tab goes back to after zooming out or panning. */
    private val home = mutableMapOf<MapState, CameraPosition>()

    fun setHome(state: MapState) {
        if (state !in home) home[state] = state.cameraPosition
    }

    /** The view before the last fly-in, or else the opening view; the opening view stays for the next press. */
    fun take(state: MapState): CameraPosition? = saved.remove(state) ?: home[state]

    fun forget(state: MapState) {
        saved.remove(state)
        home.remove(state)
    }
}
