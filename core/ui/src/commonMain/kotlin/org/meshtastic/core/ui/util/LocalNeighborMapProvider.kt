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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import org.meshtastic.core.model.Node
import org.meshtastic.core.ui.component.PlaceholderScreen

/**
 * Provides the map that shows one node and the neighbours it reported, with a line from each neighbour to the node
 * coloured by the SNR it was heard with. It shows nothing else: no other nodes, no waypoints, no filters. Neighbours
 * without a known position are left out of the map (the host tells the person how many).
 *
 * Parameters: the node the report came from, its neighbours as pairs of node number and SNR in dB, a lookup of the
 * nodes by number (for names and positions) and the modifier of the map.
 */
@Suppress("Wrapping", "CompositionLocalAllowlist")
val LocalNeighborMapProvider =
    compositionLocalOf<
        @Composable (
            centerNum: Int,
            neighbors: List<Pair<Int, Float>>,
            nodeLookup: Map<Int, Node>,
            modifier: Modifier,
        ) -> Unit,
        > {
        { _, _, _, _ -> PlaceholderScreen("Neighbor Map") }
    }
