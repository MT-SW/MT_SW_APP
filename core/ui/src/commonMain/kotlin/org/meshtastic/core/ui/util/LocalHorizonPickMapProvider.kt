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
@file:Suppress("MatchingDeclarationName")

package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** A node on the map used to pick the place the terrain panorama is looked from: its point and its colours (ARGB). */
data class HorizonMapNode(
    val latitude: Double,
    val longitude: Double,
    val shortName: String,
    val background: Int,
    val foreground: Int,
    /** Radius (m) of the doubt about its position, or null: no circle is drawn. */
    val precisionM: Double? = null,
)

/**
 * The same map as the main one (basemap, panning, zooming, the nodes drawn as chips), with a fixed cross in the middle.
 * It opens on [centerLatitude] / [centerLongitude] at a zoom that fits [radiusKm]; when the user stops moving it,
 * `onCenterChanged` reports the point under the cross. Null where the platform has no such map; the panorama then draws
 * its own simple one.
 */
@Suppress("Wrapping", "CompositionLocalAllowlist")
val LocalHorizonPickMapProvider =
    compositionLocalOf<
        (
            @Composable (
                centerLatitude: Double,
                centerLongitude: Double,
                radiusKm: Int,
                nodes: List<HorizonMapNode>,
                onCenterChanged: (latitude: Double, longitude: Double) -> Unit,
                modifier: Modifier,
            ) -> Unit
        )?,
        > {
        null
    }
