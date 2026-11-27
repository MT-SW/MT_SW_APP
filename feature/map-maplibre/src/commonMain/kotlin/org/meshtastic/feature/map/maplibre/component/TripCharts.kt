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
@file:Suppress("MagicNumber", "ModifierMissing", "TooManyFunctions")

package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.trips_chart_climbed
import org.meshtastic.core.resources.trips_chart_distance
import org.meshtastic.core.resources.trips_chart_height
import org.meshtastic.core.resources.trips_chart_pace
import org.meshtastic.core.resources.trips_chart_signal
import org.meshtastic.core.resources.trips_chart_speed
import org.meshtastic.core.ui.component.elementLook
import org.meshtastic.core.ui.component.rememberChartStyle
import org.meshtastic.feature.map.trip.Trip
import org.meshtastic.feature.map.trip.TripStore

private const val MAX_CHART_POINTS = 200
private const val CHART_HEIGHT_DP = 150
private const val MS_PER_HOUR = 3_600_000.0
private const val METERS_PER_KM = 1000.0
private const val MIN_PACE_KMH = 0.5
private const val MINUTES_PER_HOUR = 60.0
private const val MS_PER_MINUTE = 60_000L
private const val MAX_DROPS = 80
private const val CARD_TOP_ALPHA = 0.9f
private const val CARD_BOTTOM_ALPHA = 0.55f
private const val CARD_RIM_ALPHA = 0.6f
private const val AREA_TOP_ALPHA = 0.45f
private const val AREA_BOTTOM_ALPHA = 0.05f
private const val DROP_LIGHTEN = 0.35f
private const val DROP_RIM_ALPHA = 0.85f
private const val GRID_ALPHA = 0.5f
private const val GUIDE_ALPHA = 0.3f
private const val AXIS_FONT_SP = 10
private const val MARKER_FONT_SP = 12
private val CARD_CORNER = 16.dp

/** One value of a chart: the time (in milliseconds) and the measured value. */
private class Sample(val t: Long, val v: Double)

/**
 * MT_SW: the charts of a hike, in the look of the charts of the node statistics: a glass card, gradient under the line,
 * glass drops, a value axis on the left and a time axis below. All charts share the same time axis (from the start to
 * the end of the hike), so a place on one chart is the same moment on every other. A touch or a drag shows the value at
 * that moment.
 */
@Composable
internal fun TripCharts(trip: Trip) {
    val start = trip.points.firstOrNull()?.t ?: return
    val end = trip.points.lastOrNull()?.t ?: return
    val heights =
        remember(trip.id, trip.points.size) {
            sample(trip.points.mapNotNull { p -> p.ele?.let { Sample(p.t, it) } })
        }
    val speeds =
        remember(trip.id, trip.points.size) {
            sample(
                trip.points.mapIndexedNotNull { i, p ->
                    if (i == 0) return@mapIndexedNotNull null
                    val q = trip.points[i - 1]
                    val dt = p.t - q.t
                    if (dt <= 0L) {
                        null
                    } else {
                        Sample(p.t, TripStore.meters(q.lat, q.lon, p.lat, p.lon) / METERS_PER_KM / (dt / MS_PER_HOUR))
                    }
                },
            )
        }
    // The distance walked so far, by time: a flat part is a rest.
    val distance =
        remember(trip.id, trip.points.size) {
            var total = 0.0
            sample(
                trip.points.mapIndexed { i, p ->
                    if (i > 0) {
                        val q = trip.points[i - 1]
                        total += TripStore.meters(q.lat, q.lon, p.lat, p.lon)
                    }
                    Sample(p.t, total / METERS_PER_KM)
                },
            )
        }
    // The pace in minutes per kilometre, from the speed (a standing still is left out).
    val pace = remember(speeds) { speeds.filter { it.v > MIN_PACE_KMH }.map { Sample(it.t, MINUTES_PER_HOUR / it.v) } }
    // The height gained so far.
    val climbed =
        remember(heights) {
            var up = 0.0
            heights.mapIndexed { i, h ->
                if (i > 0) up += (h.v - heights[i - 1].v).coerceAtLeast(0.0)
                Sample(h.t, up)
            }
        }
    // The best signal heard in each minute of the walk.
    val signal =
        remember(trip.id, trip.signals.size) {
            trip.signals
                .groupBy { it.t / MS_PER_MINUTE }
                .entries
                .sortedBy { it.key }
                .map { entry -> Sample(entry.key * MS_PER_MINUTE, entry.value.maxOf { it.rssi }.toDouble()) }
        }
    LaunchedEffect(trip.id, trip.points.size) {
        Logger.i {
            "Wędrówki: wykresy (wysokość ${heights.size}, prędkość ${speeds.size}, dystans ${distance.size}, " +
                "sygnał ${signal.size} punktów)"
        }
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    if (heights.size >= 2) Chart(stringResource(Res.string.trips_chart_height), heights, "m", 0, primary, start, end)
    if (climbed.size >= 2 && climbed.last().v > 0.0) {
        Chart(stringResource(Res.string.trips_chart_climbed), climbed, "m", 0, tertiary, start, end)
    }
    if (speeds.size >= 2) Chart(stringResource(Res.string.trips_chart_speed), speeds, "km/h", 1, secondary, start, end)
    if (pace.size >= 2) Chart(stringResource(Res.string.trips_chart_pace), pace, "min/km", 1, primary, start, end)
    if (distance.size >= 2) {
        Chart(stringResource(Res.string.trips_chart_distance), distance, "km", 2, tertiary, start, end)
    }
    if (signal.size >= 2) {
        // Normally the signal shares the time axis of the route. When its times lie outside the route (the clock of
        // the readings and the clock of the position disagree, e.g. on an emulator) the chart uses its own range, so
        // the readings are not drawn off the card.
        val outside = signal.last().t < start || signal.first().t > end
        val from = if (outside) signal.first().t else start
        val to = if (outside) signal.last().t else end
        if (outside) Logger.w { "Wędrówki: czasy sygnału poza trasą, wykres sygnału ma własną oś czasu" }
        Chart(stringResource(Res.string.trips_chart_signal), signal, "dBm", 0, secondary, from, to)
    }
}

/** At most [MAX_CHART_POINTS] values: a long route is thinned out. */
private fun sample(values: List<Sample>): List<Sample> {
    if (values.size <= MAX_CHART_POINTS) return values
    val step = values.size.toDouble() / MAX_CHART_POINTS
    return List(MAX_CHART_POINTS) { values[(it * step).toInt()] }
}

/** The glass card under a chart: the gradient, the thin rim and the shadow of the cards of the node statistics. */
@Composable
private fun Modifier.glassChartCard(): Modifier {
    val shape = RoundedCornerShape(CARD_CORNER)
    val colors = MaterialTheme.colorScheme
    return this.elementLook(UiOptions.GROUP_CARDS, shape)
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    colors.surfaceContainerHighest.copy(alpha = CARD_TOP_ALPHA),
                    colors.surfaceContainerLow.copy(alpha = CARD_BOTTOM_ALPHA),
                ),
            ),
        )
        .border(1.dp, colors.outlineVariant.copy(alpha = CARD_RIM_ALPHA), shape)
}

@Composable
private fun Chart(
    title: String,
    values: List<Sample>,
    unit: String,
    decimals: Int,
    colour: Color,
    startT: Long,
    endT: Long,
) {
    val low = values.minOf { it.v }
    val high = values.maxOf { it.v }
    val style = rememberChartStyle(UiOptions.CHART_STYLE_NODE)
    val glassCard = style == UiOptions.CHART_STYLE_RIBBON
    val drops = style == UiOptions.CHART_STYLE_RIBBON && values.size <= MAX_DROPS
    val area = style != UiOptions.CHART_STYLE_FLAT
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(color = colors.onSurfaceVariant, fontSize = AXIS_FONT_SP.sp)
    val markerStyle = TextStyle(color = colors.onSurface, fontSize = MARKER_FONT_SP.sp)
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    Column {
        Text(
            text = "$title: ${NumberFormatter.format(low, decimals)}–${NumberFormatter.format(high, decimals)} $unit",
            style = MaterialTheme.typography.bodySmall,
        )
        Canvas(
            modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 4.dp)
                .height(CHART_HEIGHT_DP.dp)
                .then(if (glassCard) Modifier.glassChartCard() else Modifier)
                .pointerInput(values, startT, endT) {
                    detectTapGestures { offset ->
                        selected = nearest(values, offset.x, size.width.toFloat(), startT, endT)
                    }
                }
                .pointerInput(values, startT, endT) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        selected = nearest(values, change.position.x, size.width.toFloat(), startT, endT)
                    }
                },
        ) {
            val span = (high - low).coerceAtLeast(1e-6)
            val spanT = (endT - startT).coerceAtLeast(1L).toFloat()
            val labelText = listOf(high, (high + low) / 2, low).map { NumberFormatter.format(it, decimals) }
            val labels = labelText.map { measurer.measure(it, axisStyle) }
            val timeHeight = measurer.measure("00:00", axisStyle).size.height
            val left = labels.maxOf { it.size.width } + 10.dp.toPx()
            val right = 10.dp.toPx()
            val top = 10.dp.toPx()
            val bottom = size.height - timeHeight - 10.dp.toPx()
            val plotW = size.width - left - right
            val plotH = bottom - top
            fun xOf(t: Long) = left + plotW * ((t - startT) / spanT)
            fun yOf(v: Double) = top + plotH * (1f - ((v - low) / span).toFloat())

            // The grid and the value axis on the left.
            labels.forEachIndexed { i, text ->
                val y = top + plotH * i / 2f
                drawLine(colors.outlineVariant.copy(alpha = GRID_ALPHA), Offset(left, y), Offset(left + plotW, y))
                drawText(text, topLeft = Offset(left - 6.dp.toPx() - text.size.width, y - text.size.height / 2f))
            }
            // The time axis below: the start, the middle and the end of the hike.
            listOf(0f, 0.5f, 1f).forEach { f ->
                val text = measurer.measure(DateFormatter.formatTime(startT + (spanT * f).toLong()), axisStyle)
                val x = (left + plotW * f - text.size.width * f).coerceAtLeast(0f)
                drawText(text, topLeft = Offset(x, bottom + 6.dp.toPx()))
            }

            val line = Path()
            values.forEachIndexed { i, s ->
                if (i == 0) line.moveTo(xOf(s.t), yOf(s.v)) else line.lineTo(xOf(s.t), yOf(s.v))
            }
            if (area) {
                val fill = Path().apply {
                    addPath(line)
                    lineTo(xOf(values.last().t), bottom)
                    lineTo(xOf(values.first().t), bottom)
                    close()
                }
                drawPath(
                    fill,
                    Brush.verticalGradient(
                        listOf(colour.copy(alpha = AREA_TOP_ALPHA), colour.copy(alpha = AREA_BOTTOM_ALPHA)),
                        startY = top,
                        endY = bottom,
                    ),
                )
            }
            drawPath(line, colour, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            // A small glass drop for each measurement: a lighter fill of the colour with a bright rim.
            if (drops) {
                values.forEach { s ->
                    val centre = Offset(xOf(s.t), yOf(s.v))
                    drawCircle(lerp(colour, Color.White, DROP_LIGHTEN), 3.5.dp.toPx(), centre)
                    val rim = Color.White.copy(alpha = DROP_RIM_ALPHA)
                    drawCircle(rim, 3.5.dp.toPx(), centre, style = Stroke(1.dp.toPx()))
                }
            }
            // The touched moment: a guide line, a bigger drop and the value in a small label.
            selected?.let { index ->
                values.getOrNull(index)?.let { s ->
                    val x = xOf(s.t)
                    val centre = Offset(x, yOf(s.v))
                    drawLine(colors.onSurfaceVariant.copy(alpha = GUIDE_ALPHA), Offset(x, top), Offset(x, bottom))
                    drawCircle(lerp(colour, Color.White, DROP_LIGHTEN), 7.dp.toPx(), centre)
                    val rim = Color.White.copy(alpha = DROP_RIM_ALPHA)
                    drawCircle(rim, 7.dp.toPx(), centre, style = Stroke(2.dp.toPx()))
                    val label =
                        measurer.measure(
                            "${NumberFormatter.format(s.v, decimals)} $unit · ${DateFormatter.formatTime(s.t)}",
                            markerStyle,
                        )
                    val padX = 8.dp.toPx()
                    val padY = 4.dp.toPx()
                    val boxW = label.size.width + 2 * padX
                    val boxH = label.size.height + 2 * padY
                    val boxX = (x - boxW / 2).coerceIn(0f, (size.width - boxW).coerceAtLeast(0f))
                    val boxY = (centre.y - boxH - 10.dp.toPx()).coerceAtLeast(0f)
                    drawRoundRect(
                        colors.surfaceContainerHigh,
                        Offset(boxX, boxY),
                        Size(boxW, boxH),
                        CornerRadius(6.dp.toPx()),
                    )
                    drawRoundRect(
                        colors.outlineVariant,
                        Offset(boxX, boxY),
                        Size(boxW, boxH),
                        CornerRadius(6.dp.toPx()),
                        style = Stroke(1.dp.toPx()),
                    )
                    drawText(label, topLeft = Offset(boxX + padX, boxY + padY))
                }
            }
        }
    }
}

/** The index of the value whose moment is nearest to the touched place [x] on a chart [width] pixels wide. */
private fun nearest(values: List<Sample>, x: Float, width: Float, startT: Long, endT: Long): Int? {
    if (values.isEmpty() || width <= 0f) return null
    val t = startT + ((endT - startT) * (x / width)).toLong()
    return values.indices.minByOrNull { kotlin.math.abs(values[it].t - t) }
}
