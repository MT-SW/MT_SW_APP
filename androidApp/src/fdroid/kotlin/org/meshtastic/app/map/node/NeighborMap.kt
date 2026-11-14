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
package org.meshtastic.app.map.node

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.meshtastic.app.map.androidCustomRasterBasemaps
import org.meshtastic.core.model.Node
import org.meshtastic.feature.map.maplibre.MapLibreNeighborMap

/** Flavor-unified entry point for the map of one node and its reported neighbours. MapLibre implementation. */
@Composable
fun NeighborMap(
    centerNum: Int,
    neighbors: List<Pair<Int, Float>>,
    nodeLookup: Map<Int, Node>,
    modifier: Modifier = Modifier,
) = MapLibreNeighborMap(
    centerNum = centerNum,
    neighbors = neighbors,
    nodeLookup = nodeLookup,
    modifier = modifier,
    customBasemaps = { androidCustomRasterBasemaps() },
)
