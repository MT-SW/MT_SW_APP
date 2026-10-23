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
package org.meshtastic.feature.map.maplibre.style

import androidx.compose.ui.graphics.Color
import org.meshtastic.core.ui.theme.monoSafe

/**
 * Colours shared across the MapLibre map surfaces.
 *
 * Named here rather than inline so a theme pass has one place to change. Route colours are not here: those live in
 * [org.meshtastic.core.ui.theme.TracerouteColors], which the traceroute screen's own legend also reads — naming them
 * twice is how the lines came to disagree with the legend describing them.
 */
internal object MapColors {
    /** Cluster bubbles and traceroute hop markers. */
    private val SlateBase = Color(0xFF2C2D3C)
    val Slate: Color get() = SlateBase.monoSafe()

    /** Geofence zones and the currently selected track point — carried over from the OSMdroid map. */
    private val HighlightBase = Color(0xFFFF8C00)
    val Highlight: Color get() = HighlightBase.monoSafe()

    /** A node the scanner heard itself. The same green the Google discovery map uses. */
    private val DiscoveryDirectBase = Color(0xFF4CAF50)
    val DiscoveryDirect: Color get() = DiscoveryDirectBase.monoSafe()

    /**
     * A node that reached the scanner through the mesh. Blue, as on the Google discovery map — the distinction is the
     * point of the screen, so it belongs on the markers and not only on the links.
     */
    private val DiscoveryMeshBase = Color(0xFF2196F3)
    val DiscoveryMesh: Color get() = DiscoveryMeshBase.monoSafe()

    /** The scanner itself. */
    private val DiscoveryUserBase = Color(0xFFFF9800)
    val DiscoveryUser: Color get() = DiscoveryUserBase.monoSafe()
}
