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
package org.meshtastic.desktop.map

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val DEFAULT_STROKE_COLOR_ARGB = 0xFF3388FFL
private const val DEFAULT_FILL_COLOR_ARGB = 0x593388FFL // ~35% alpha of the default stroke color
private const val DEFAULT_STROKE_WIDTH_DP = 2f
private const val POINT_RADIUS_DP = 5f

/**
 * Draws parsed GeoJSON/KML [features] (polygons, lines, points) as a single [Canvas] overlay, using each feature's
 * resolved [FeatureStyle] where present and sensible defaults otherwise. Mirrors the Android fork's
 * `DEFAULT_GEOJSON_FILL_OPACITY`/`DEFAULT_GEOJSON_STROKE_WIDTH` fallback approach. Polygon holes are not rendered in v1
 * — only each polygon's outer boundary (first ring).
 */
@Composable
fun MapLayersOverlay(
    features: List<ParsedFeature>,
    toScreenOffset: (lon: Double, lat: Double) -> Offset,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        features.forEach { feature ->
            val strokeColor = Color(feature.style.strokeColorArgb ?: DEFAULT_STROKE_COLOR_ARGB)
            val fillColor = Color(feature.style.fillColorArgb ?: DEFAULT_FILL_COLOR_ARGB)
            val strokeWidthPx = (feature.style.strokeWidth ?: DEFAULT_STROKE_WIDTH_DP).dp.toPx()

            when (val geometry = feature.geometry) {
                is ParsedGeometry.Point -> {
                    val center = toScreenOffset(geometry.lon, geometry.lat)
                    drawCircle(color = strokeColor, radius = POINT_RADIUS_DP.dp.toPx(), center = center)
                }

                is ParsedGeometry.LineString -> {
                    val path = geometry.points.toPath(toScreenOffset, closed = false)
                    drawPath(path, color = strokeColor, style = Stroke(width = strokeWidthPx))
                }

                is ParsedGeometry.Polygon ->
                    drawPolygonOuterRing(geometry, toScreenOffset, fillColor, strokeColor, strokeWidthPx)

                is ParsedGeometry.MultiPolygon ->
                    geometry.polygons.forEach { polygon ->
                        drawPolygonOuterRing(polygon, toScreenOffset, fillColor, strokeColor, strokeWidthPx)
                    }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPolygonOuterRing(
    polygon: ParsedGeometry.Polygon,
    toScreenOffset: (lon: Double, lat: Double) -> Offset,
    fillColor: Color,
    strokeColor: Color,
    strokeWidthPx: Float,
) {
    val outerRing = polygon.rings.firstOrNull() ?: return
    val path = outerRing.toPath(toScreenOffset, closed = true)
    drawPath(path, color = fillColor, style = Fill)
    drawPath(path, color = strokeColor, style = Stroke(width = strokeWidthPx))
}

private fun List<Pair<Double, Double>>.toPath(
    toScreenOffset: (lon: Double, lat: Double) -> Offset,
    closed: Boolean,
): Path {
    val path = Path()
    forEachIndexed { index, (lon, lat) ->
        val point = toScreenOffset(lon, lat)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    if (closed) path.close()
    return path
}
