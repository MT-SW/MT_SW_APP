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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.compose.expressions.dsl.asNumber
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
 * signal indicators elsewhere in the app), with an arrowhead at its far end and the SNR written along it. The two
 * directions run side by side: each line is shifted a few pixels to the right of its own direction of travel, so the
 * pair always sits at equal distances on either side of the axis between the nodes, at every zoom and orientation.
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

    DirectionLayers(id = "traceroute-forward", edges = forwardEdges, preset = preset, palette = palette)
    DirectionLayers(id = "traceroute-return", edges = returnEdges, preset = preset, palette = palette)
}

/** One direction's lines, arrowheads and labels. */
@Composable
private fun DirectionLayers(
    id: String,
    edges: List<TracerouteEdge>,
    preset: ModemPreset?,
    palette: Map<Quality, String>,
) {
    val lines = rememberFeatureSource(edges, preset, palette) { edgesToLines(edges, preset, palette) }
    val arrows = rememberFeatureSource(edges, preset, palette) { edgesToArrows(edges, preset, palette) }

    LineLayer(
        id = "$id-line",
        source = lines,
        cap = const(LineCap.Butt),
        join = const(LineJoin.Round),
        color = feature[COLOR].convertToColor(const(Color.White)),
        width = const(LINE_WIDTH_DP.dp),
        // Positive offsets are to the right of the line's own direction, so the opposite direction lands on the far side.
        offset = const(SEPARATION_DP.dp),
    )
    SymbolLayer(
        id = "$id-arrow",
        source = arrows,
        textField = feature[ARROW].asString(),
        textFont = const(listOf("Noto Sans Regular")),
        textColor = feature[COLOR].convertToColor(const(Color.White)),
        textHaloColor = const(Color.Black),
        textHaloWidth = const(1.dp),
        textSize = const(ARROW_TEXT_SIZE_SP.sp),
        textRotate = feature[ROTATION].asNumber(),
        // The offset turns with the glyph: back from the node so the tip lands on it, and to the right onto the line.
        textOffset = textOffset((-ARROW_BACK_EM).em, SEPARATION_EM.em),
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true),
    )
    // Text turned half a circle to stay upright has its left and right swapped, so those labels need the opposite side.
    LabelLayer("$id-label", edges.filterNot { it.labelFlipped }, preset, palette, LABEL_SIDE_EM)
    LabelLayer("$id-label-flipped", edges.filter { it.labelFlipped }, preset, palette, -LABEL_SIDE_EM)
}

@Composable
private fun LabelLayer(
    id: String,
    edges: List<TracerouteEdge>,
    preset: ModemPreset?,
    palette: Map<Quality, String>,
    sideEm: Float,
) {
    val labels = rememberFeatureSource(edges, preset, palette) { edgesToLabels(edges, preset, palette) }
    SymbolLayer(
        id = id,
        source = labels,
        textField = feature[LABEL].asString(),
        textFont = const(listOf("Noto Sans Regular")),
        textColor = feature[COLOR].convertToColor(const(Color.White)),
        textHaloColor = const(Color.Black),
        textHaloWidth = const(1.5.dp),
        textSize = const(LABEL_TEXT_SIZE_SP.sp),
        textRotate = feature[ROTATION].asNumber(),
        // On the outer side of its own line, where the other direction's label cannot reach.
        textOffset = textOffset(0.em, sideEm.em),
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
            geometry = LineString(listOf(e.lineStart, e.lineEnd)),
            properties = buildJsonObject { put(COLOR, edgeColor(e, preset, palette)) },
        )
    },
)

private fun edgesToArrows(
    edges: List<TracerouteEdge>,
    preset: ModemPreset?,
    palette: Map<Quality, String>,
): FeatureCollection<Point, JsonObject?> = FeatureCollection(
    edges.map { e ->
        Feature(
            geometry = Point(e.lineEnd),
            properties =
            buildJsonObject {
                put(COLOR, edgeColor(e, preset, palette))
                put(ARROW, ARROW_GLYPH)
                put(ROTATION, e.arrowRotationDeg)
            },
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
                put(ROTATION, e.labelRotationDeg)
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
private const val ARROW = "arrow"
private const val ROTATION = "rotation"
private const val ARROW_GLYPH = "→"
private const val UNKNOWN_COLOR = "#9E9E9E"
private const val LINE_WIDTH_DP = 3
private const val SEPARATION_DP = 3
private const val SEPARATION_EM = 0.125f
private const val ARROW_BACK_EM = 0.45f
private const val LABEL_SIDE_EM = 0.95f
private const val LABEL_TEXT_SIZE_SP = 12
private const val ARROW_TEXT_SIZE_SP = 24
private const val RGB_MASK = 0xFFFFFF
private const val HEX_RADIX = 16
private const val HEX_DIGITS = 6
