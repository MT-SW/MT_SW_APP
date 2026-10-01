/*
 * Copyright (c) 2025 MT_SW contributors
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
package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_chart_label_a
import org.meshtastic.core.resources.planner_chart_label_b
import org.meshtastic.core.resources.planner_chart_legend_fresnel
import org.meshtastic.core.resources.planner_chart_legend_los
import org.meshtastic.core.resources.planner_chart_legend_terrain
import org.meshtastic.core.resources.planner_chart_readout
import org.meshtastic.core.resources.planner_chart_tap_hint
import org.meshtastic.core.resources.planner_chart_y_axis
import org.meshtastic.core.resources.planner_profile_empty
import org.meshtastic.core.resources.planner_section_profile
import org.meshtastic.core.resources.planner_unit_km
import org.meshtastic.core.resources.planner_unit_m
import org.meshtastic.feature.map.planner.ProfileSeries
import org.meshtastic.feature.map.planner.state.PlannerUiState
import kotlin.math.max
import kotlin.math.min

private val CHART_HEIGHT = 260.dp
private val PAD_LEFT = 52.dp
private val PAD_RIGHT = 14.dp
private val PAD_TOP = 22.dp
private val PAD_BOTTOM = 28.dp
private const val KM_THRESHOLD_M = 2000.0
private const val X_TICKS = 6
private const val Y_TICKS = 5
private const val Y_PAD_LOW = 0.05
private const val Y_PAD_HIGH = 0.10

private val TERRAIN_FILL = Color(0xFF8D6E63)
private val TERRAIN_LINE = Color(0xFF5D4037)

private class ChartScale(val series: ProfileSeries, val useKm: Boolean) {
    val xMax: Double = max(1.0, series.distancesM.lastOrNull() ?: 1.0)
    val yMin: Double
    val yMax: Double

    init {
        var lo = Double.MAX_VALUE
        var hi = -Double.MAX_VALUE
        for (i in series.distancesM.indices) {
            lo = min(lo, min(series.groundM[i], series.fresnelLowerM[i]))
            hi = max(hi, max(series.groundM[i], max(series.fresnelUpperM[i], series.losM[i])))
        }
        val span = max(1.0, hi - lo)
        yMin = lo - span * Y_PAD_LOW
        yMax = hi + span * Y_PAD_HIGH
    }

    val xUnitDivisor: Double = if (useKm) 1000.0 else 1.0
    val xTicks: List<Double> = ChartMath.niceTicks(0.0, xMax / xUnitDivisor, X_TICKS)
    val yTicks: List<Double> = ChartMath.niceTicks(yMin, yMax, Y_TICKS)
    val xDecimals: Int = ChartMath.decimalsFor(if (xTicks.size > 1) xTicks[1] - xTicks[0] else 1.0)
    val yDecimals: Int = ChartMath.decimalsFor(if (yTicks.size > 1) yTicks[1] - yTicks[0] else 1.0)
}

/** Terrain profile card of the planner. */
@Composable
internal fun ProfileChartSection(state: PlannerUiState) {
    val series = state.series
    PlannerCard(title = stringResource(Res.string.planner_section_profile), info = PlannerInfoTopic.PROFILE) {
        if (series == null || series.distancesM.size < 2 || !state.isLinkReady || state.link == null) {
            Text(
                text = stringResource(Res.string.planner_profile_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ProfileChart(series = series)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileChart(series: ProfileSeries) {
    val unitM = stringResource(Res.string.planner_unit_m)
    val unitKm = stringResource(Res.string.planner_unit_km)
    val labelA = stringResource(Res.string.planner_chart_label_a)
    val labelB = stringResource(Res.string.planner_chart_label_b)
    val yAxisTitle = stringResource(Res.string.planner_chart_y_axis)
    val scale = remember(series) { ChartScale(series, series.distancesM.last() >= KM_THRESHOLD_M) }
    val xUnit = if (scale.useKm) unitKm else unitM
    val xLabels = scale.xTicks.map { fmt(it, scale.xDecimals) }
    val yLabels = scale.yTicks.map { fmt(it, scale.yDecimals) }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0) }
    var selectedX by remember(series) { mutableStateOf<Double?>(null) }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val losColor = MaterialTheme.colorScheme.primary
    val fresnelColor = MaterialTheme.colorScheme.secondary
    val gridColor = onSurface.copy(alpha = 0.12f)
    val labelStyle = TextStyle(fontSize = 10.sp, color = onSurface)
    val padLeft = with(density) { PAD_LEFT.toPx() }
    val padRight = with(density) { PAD_RIGHT.toPx() }

    fun metersAt(px: Float): Double {
        val plotWidth = max(1f, widthPx - padLeft - padRight)
        val frac = ((px - padLeft) / plotWidth).coerceIn(0f, 1f)
        return frac.toDouble() * scale.xMax
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier =
            Modifier.fillMaxWidth()
                .height(CHART_HEIGHT)
                .onSizeChanged { widthPx = it.width }
                .pointerInput(series) { detectTapGestures { off -> selectedX = metersAt(off.x) } }
                .pointerInput(series) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        selectedX = metersAt(change.position.x)
                    }
                },
        ) {
            val left = padLeft
            val right = size.width - padRight
            val top = PAD_TOP.toPx()
            val bottom = size.height - PAD_BOTTOM.toPx()
            val d = series.distancesM
            val n = d.size

            fun px(xMeters: Double): Float = left + (xMeters / scale.xMax * (right - left)).toFloat()

            fun py(yMeters: Double): Float = bottom - ((yMeters - scale.yMin) / (scale.yMax - scale.yMin) * (bottom - top)).toFloat()

            // Grid and tick labels.
            scale.yTicks.forEachIndexed { i, v ->
                val y = py(v)
                drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
                val layout = textMeasurer.measure(text = yLabels[i], style = labelStyle)
                drawText(layout, topLeft = Offset(left - layout.size.width - 4.dp.toPx(), y - layout.size.height / 2f))
            }
            scale.xTicks.forEachIndexed { i, v ->
                val x = px(v * scale.xUnitDivisor)
                drawLine(gridColor, Offset(x, top), Offset(x, bottom), strokeWidth = 1.dp.toPx())
                val layout = textMeasurer.measure(text = xLabels[i], style = labelStyle)
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, bottom + 4.dp.toPx()))
            }
            val xTitle = textMeasurer.measure(text = xUnit, style = labelStyle)
            drawText(xTitle, topLeft = Offset(right - xTitle.size.width, size.height - xTitle.size.height.toFloat()))
            val yTitle = textMeasurer.measure(text = yAxisTitle, style = labelStyle)
            drawText(yTitle, topLeft = Offset(left, 0f))

            clipRect(left, top, right, bottom) {
                // Terrain (with earth bulge), filled down to the axis.
                val terrain = Path()
                terrain.moveTo(px(d[0]), bottom)
                for (i in 0 until n) terrain.lineTo(px(d[i]), py(series.groundM[i]))
                terrain.lineTo(px(d[n - 1]), bottom)
                terrain.close()
                drawPath(terrain, TERRAIN_FILL.copy(alpha = 0.55f))
                val terrainLine = Path()
                for (i in 0 until n) {
                    if (i == 0) terrainLine.moveTo(px(d[i]), py(series.groundM[i])) else terrainLine.lineTo(px(d[i]), py(series.groundM[i]))
                }
                drawPath(terrainLine, TERRAIN_LINE, style = Stroke(width = 1.5.dp.toPx()))

                // First Fresnel zone.
                val fresnel = Path()
                for (i in 0 until n) {
                    if (i == 0) fresnel.moveTo(px(d[i]), py(series.fresnelUpperM[i])) else fresnel.lineTo(px(d[i]), py(series.fresnelUpperM[i]))
                }
                for (i in n - 1 downTo 0) fresnel.lineTo(px(d[i]), py(series.fresnelLowerM[i]))
                fresnel.close()
                drawPath(fresnel, fresnelColor.copy(alpha = 0.18f))

                // Line of sight.
                drawLine(
                    losColor,
                    Offset(px(d[0]), py(series.losM[0])),
                    Offset(px(d[n - 1]), py(series.losM[n - 1])),
                    strokeWidth = 2.dp.toPx(),
                )
            }

            // Antennas.
            listOf(0 to labelA, n - 1 to labelB).forEach { (i, label) ->
                val x = px(d[i])
                val yTop = py(series.losM[i])
                drawLine(onSurface, Offset(x, py(series.groundM[i])), Offset(x, yTop), strokeWidth = 3.dp.toPx())
                drawCircle(losColor, radius = 4.dp.toPx(), center = Offset(x, yTop))
                val layout = textMeasurer.measure(text = label, style = labelStyle)
                val lx = (x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                drawText(layout, topLeft = Offset(lx, max(0f, yTop - layout.size.height - 6.dp.toPx())))
            }

            // Axes.
            drawLine(onSurface, Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.dp.toPx())
            drawLine(onSurface, Offset(left, top), Offset(left, bottom), strokeWidth = 1.dp.toPx())

            // Selection cursor.
            selectedX?.let { sx ->
                val i = ChartMath.nearestIndex(d, sx)
                val x = px(d[i])
                drawLine(
                    onSurface,
                    Offset(x, top),
                    Offset(x, bottom),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                )
                drawCircle(TERRAIN_LINE, radius = 4.dp.toPx(), center = Offset(x, py(series.groundM[i])))
                drawCircle(losColor, radius = 4.dp.toPx(), center = Offset(x, py(series.losM[i])))
            }
        }

        val sel = selectedX
        Text(
            text =
            if (sel == null) {
                stringResource(Res.string.planner_chart_tap_hint)
            } else {
                val i = ChartMath.nearestIndex(series.distancesM, sel)
                val ground = series.groundM[i]
                val los = series.losM[i]
                stringResource(
                    Res.string.planner_chart_readout,
                    fmtDistance(series.distancesM[i], unitM, unitKm),
                    fmt(ground, 1) + " " + unitM,
                    fmt(los, 1) + " " + unitM,
                    fmt(los - ground, 1) + " " + unitM,
                    fmt(series.fresnelUpperM[i] - los, 1) + " " + unitM,
                )
            },
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LegendDot(TERRAIN_FILL, stringResource(Res.string.planner_chart_legend_terrain))
            LegendDot(losColor, stringResource(Res.string.planner_chart_legend_los))
            LegendDot(fresnelColor.copy(alpha = 0.5f), stringResource(Res.string.planner_chart_legend_fresnel))
        }
    }
}

@Composable
internal fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
