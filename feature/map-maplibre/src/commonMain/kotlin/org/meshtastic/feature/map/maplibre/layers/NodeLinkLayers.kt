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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.LineLayer
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.model.Node
import org.meshtastic.core.ui.component.Quality
import org.meshtastic.core.ui.component.determineSignalQuality
import org.meshtastic.core.ui.util.LocalModemPreset
import org.meshtastic.feature.map.MapLink
import org.meshtastic.feature.map.MapLinkKind
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.feature.map.maplibre.geojson.toCssHex
import org.meshtastic.proto.Config.LoRaConfig.ModemPreset

/**
 * Lines between nodes that exchange packets, coloured by how good the link is (the same signal palette as the node
 * list and the traceroute map; grey when no signal reading is known).
 *
 * Two layers, so the links the radio only deduced by hearing a node directly stay thinner and fainter than the ones a
 * node reported (neighbour info) or a traceroute walked. Drawn under the node chips and clusters, so a line never
 * covers a node.
 */
@Composable
internal fun NodeLinkLayers(links: List<MapLink>, nodes: List<Node>) {
    if (links.isEmpty()) return

    val preset = LocalModemPreset.current
    val palette = Quality.entries.associateWith { it.color.invoke().toArgb().toCssHex() }
    val reported = remember(links) { links.filter { it.kind != MapLinkKind.DIRECT } }
    val heard = remember(links) { links.filter { it.kind == MapLinkKind.DIRECT } }
    val reportedSource = rememberFeatureSource(reported, nodes, preset, palette) {
        linksToLines(reported, nodes, preset, palette)
    }
    val heardSource = rememberFeatureSource(heard, nodes, preset, palette) {
        linksToLines(heard, nodes, preset, palette)
    }

    LineLayer(
        id = "node-links-direct",
        source = heardSource,
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
        color = feature[COLOR].convertToColor(const(Color.Gray)),
        width = const(HEARD_WIDTH_DP.dp),
        opacity = const(HEARD_OPACITY),
    )
    LineLayer(
        id = "node-links-reported",
        source = reportedSource,
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
        color = feature[COLOR].convertToColor(const(Color.Gray)),
        width = const(REPORTED_WIDTH_DP.dp),
        opacity = const(REPORTED_OPACITY),
    )
}

/** One line per link whose two nodes both have a position; the others cannot be placed on the map. */
private fun linksToLines(
    links: List<MapLink>,
    nodes: List<Node>,
    preset: ModemPreset?,
    palette: Map<Quality, String>,
): FeatureCollection<LineString, JsonObject?> {
    val byNum = nodes.associateBy { it.num }
    return FeatureCollection(
        links.mapNotNull { link ->
            val a = byNum[link.nodeA]?.takeIf { it.validPosition != null } ?: return@mapNotNull null
            val b = byNum[link.nodeB]?.takeIf { it.validPosition != null } ?: return@mapNotNull null
            val color =
                link.snrDb?.let { palette.getValue(determineSignalQuality(it, preset)) } ?: UNKNOWN_LINK_COLOR
            Feature<LineString, JsonObject?>(
                geometry =
                LineString(
                    listOf(
                        Position(longitude = a.longitude, latitude = a.latitude),
                        Position(longitude = b.longitude, latitude = b.latitude),
                    ),
                ),
                properties = buildJsonObject { put(COLOR, color) },
            )
        },
    )
}

private const val COLOR = "color"
private const val UNKNOWN_LINK_COLOR = "#9E9E9E"
private const val REPORTED_WIDTH_DP = 3
private const val REPORTED_OPACITY = 0.8f
private const val HEARD_WIDTH_DP = 2
private const val HEARD_OPACITY = 0.5f
