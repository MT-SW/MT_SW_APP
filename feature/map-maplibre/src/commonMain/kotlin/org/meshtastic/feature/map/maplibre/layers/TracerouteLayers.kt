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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.textOffset
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.meshtastic.core.model.Node
import org.meshtastic.core.ui.component.Quality
import org.meshtastic.core.ui.component.determineSignalQuality
import org.meshtastic.core.ui.util.LocalModemPreset
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.proto.Config.LoRaConfig.ModemPreset

/**
 * Forward and return traceroute paths, hop by hop.
 *
 * Every hop is its own line in the colour of the signal quality its SNR rates (the same bands and palette as the
 * signal indicators elsewhere in the app), with the way the packet went and the SNR written next to it. The two
 * directions run side by side, so a route that comes back the same way, or a direct link, shows two separate arrows,
 * one per direction.
 *
 * The separation is a screen-space translate rather than an offset of the geometry, so it stays legible at every zoom.
 * A translate only separates lines that are not parallel to it, so east-west hops are pushed apart vertically and the
 * others horizontally.
 */
@Composable
internal fun TracerouteLayers(
    forwardRoute: List<Int>,
    returnRoute: List<Int>,
    forwardSnr: List<Int>,
    returnSnr: List<Int>,
    nodeLookup: Map<Int, Node>,
) {
    val preset = LocalModemPreset.current
    val palette = Quality.entries.associateWith { it.color.invoke().toCssHex() }
    val forwardEdges = remember(forwardRoute, forwardSnr, nodeLookup) { tracerouteEdges(forwardRoute, forwardSnr, nodeLookup) }
    val returnEdges = remember(returnRoute, returnSnr, nodeLookup) { tracerouteEdges(returnRoute, returnSnr, nodeLookup) }

    DirectionLayers(id = "traceroute-forward", edges = forwardEdges, side = -1, preset = preset, palette = palette)
    DirectionLayers(id = "traceroute-return", edges = returnEdges, side = 1, preset = preset, palette = palette)
}

/** One direction's lines and labels; [side] is -1 for the upper/left copy and 1 for the lower/right one. */
@Composable
private fun DirectionLayers(
    id: String,
    edges: List<TracerouteEdge>,
    side: Int,
    preset: ModemPreset?,
    palette: Map<Quality, String>,
) {
    val eastWest = edges.filter { it.isMostlyEastWest }
    val other = edges.filterNot { it.isMostlyEastWest }
    EdgeLayers("$id-ew", eastWest, preset, palette, dx = 0, dy = side)
    EdgeLayers("$id-ns", other, preset, palette, dx = side, dy = 0)
}

@Composable
private fun EdgeLayers(
    id: String,
    edges: List<TracerouteEdge>,
    preset: ModemPreset?,
    palette: Map<Quality, String>,
    dx: Int,
    dy: Int,
) {
    val lines = rememberFeatureSource(edges, preset, palette) { edgesToLines(edges, preset, palette) }
    val labels = rememberFeatureSource(edges, preset, palette) { edgesToLabels(edges, preset, palette) }

    LineLayer(
        id = "$id-line",
        source = lines,
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
        color = feature[COLOR].convertToColor(const(Color.White)),
        width = const(LINE_WIDTH_DP.dp),
        translate = const(DpOffset((dx * SEPARATION_DP).dp, (dy * SEPARATION_DP).dp)),
    )
    SymbolLayer(
        id = "$id-label",
        source = labels,
        textField = feature[LABEL].asString(),
        textFont = const(listOf("Noto Sans Regular")),
        textColor = feature[COLOR].convertToColor(const(Color.White)),
        textHaloColor = const(Color.Black),
        textHaloWidth = const(1.5.dp),
        // Labels sit on their own side of the line, so the two directions never print on top of each other.
        textOffset = textOffset((dx * LABEL_SIDE_X_EM).em, (dy * LABEL_SIDE_Y_EM).em),
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true),
    )
}

private fun edgeColor(edge: TracerouteEdge, preset: ModemPreset?, palette: Map<Quality, String>): String {
    val snr = edge.snrDb ?: return UNKNOWN_COLOR
    return palette.getValue(determineSignalQuality(snr, preset))
}

private fun edgesToLines(
    edges: List<TracerouteEdge>,
    preset: ModemPreset?,
    palette: Map<Quality, String>,
): FeatureCollection<LineString, JsonObject?> = FeatureCollection(
    edges.map { e ->
        Feature(
            geometry = LineString(listOf(e.from, e.to)),
            properties = buildJsonObject { put(COLOR, edgeColor(e, preset, palette)) },
        )
    },
)

private fun edgesToLabels(
    edges: List<TracerouteEdge>,
    preset: ModemPreset?,
    palette: Map<Quality, String>,
): FeatureCollection<Point, JsonObject?> = FeatureCollection(
    edges.map { e ->
        Feature(
            geometry = Point(e.midpoint),
            properties =
            buildJsonObject {
                put(COLOR, edgeColor(e, preset, palette))
                put(LABEL, e.label)
            },
        )
    },
)

/** "#RRGGBB"; the colours of the signal palette are opaque, and MapLibre reads this form. */
private fun Color.toCssHex(): String {
    val rgb = toArgb() and RGB_MASK
    return "#" + rgb.toString(HEX_RADIX).padStart(HEX_DIGITS, '0')
}

private const val COLOR = "color"
private const val LABEL = "label"
private const val UNKNOWN_COLOR = "#9E9E9E"
private const val LINE_WIDTH_DP = 4
private const val SEPARATION_DP = 5
private const val LABEL_SIDE_Y_EM = 0.9f
private const val LABEL_SIDE_X_EM = 3.6f
private const val RGB_MASK = 0xFFFFFF
private const val HEX_RADIX = 16
private const val HEX_DIGITS = 6
