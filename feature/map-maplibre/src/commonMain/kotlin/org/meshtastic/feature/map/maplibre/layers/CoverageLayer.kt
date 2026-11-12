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
package org.meshtastic.feature.map.maplibre.layers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.FillLayer
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Polygon
import org.meshtastic.feature.map.coverage.COVERAGE_LEVELS_DB
import org.meshtastic.feature.map.coverage.CoverageArea
import org.meshtastic.feature.map.coverage.CoverageState
import org.meshtastic.feature.map.coverage.coverageRgb
import org.meshtastic.feature.map.maplibre.geojson.destination
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import kotlin.math.max

/** The layer id, also the key of its opacity setting. */
internal const val COVERAGE_LAYER_ID = "coverage"

/** How opaque the layer is until the user moves its slider. */
internal const val COVERAGE_DEFAULT_OPACITY = 0.8f

/** The share of the layer's opacity each colour step gets: the steps lie over each other, so the centre is denser. */
private const val LEVEL_OPACITY_SHARE = 0.5f
private const val OPAQUE = 0xFF000000.toInt()
private const val MIN_RADIUS_M = 60.0
private const val FULL_TURN_DEG = 360.0

/**
 * The estimated reach of the nodes, as coloured areas: weak signal purple at the edge, strong yellow near the node.
 *
 * Every colour step is one layer with a plain colour, drawn from the widest to the narrowest, so the areas of nodes
 * that reach the same place add up and the well covered places stand out.
 */
@Composable
internal fun CoverageLayer(state: CoverageState, opacity: Float) {
    if (!state.enabled || state.areas.isEmpty()) return
    COVERAGE_LEVELS_DB.forEachIndexed { level, marginDb ->
        key(level) {
            CoverageLevel(
                level = level,
                color = Color(OPAQUE or coverageRgb(marginDb)),
                areas = state.areas,
                opacity = opacity * LEVEL_OPACITY_SHARE,
            )
        }
    }
}

/** The areas of one colour step of every node. */
@Composable
private fun CoverageLevel(level: Int, color: Color, areas: List<CoverageArea>, opacity: Float) {
    val source =
        rememberFeatureSource(areas, level) {
            FeatureCollection(
                areas.mapNotNull { area ->
                    areaPolygon(area, level)?.let { Feature<Polygon, JsonObject?>(geometry = it, properties = null) }
                },
            )
        }
    FillLayer(id = "$COVERAGE_LAYER_ID-level-$level", source = source, color = const(color), opacity = const(opacity))
}

/** The outline of how far colour step [level] reaches from the node, or null when it reaches nowhere. */
private fun areaPolygon(area: CoverageArea, level: Int): Polygon? {
    val reach = area.reachM[level]
    if (reach.all { it <= 0.0 }) return null
    val ring = reach.mapIndexed { radial, distance ->
        destination(area.latitude, area.longitude, max(distance, MIN_RADIUS_M), radial * FULL_TURN_DEG / reach.size)
    }
    // Closed by hand: `ring + ring.first()` would pick the Iterable overload of plus, a Position being a collection.
    val closed = ring.toMutableList().also { it.add(ring.first()) }
    return Polygon(listOf(closed))
}
