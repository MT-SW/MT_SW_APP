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
package org.meshtastic.app.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.meshtastic.core.ui.util.HorizonMapNode
import org.meshtastic.core.ui.util.MapViewProvider

fun getMapViewProvider(): MapViewProvider = GoogleMapViewProvider()

/** No MapLibre map in this flavour: the terrain panorama draws its own simple map for the "move" mode. */
fun getHorizonPickMap():
    (@Composable (Double, Double, Int, List<HorizonMapNode>, (Double, Double) -> Unit, Modifier) -> Unit)? = null
