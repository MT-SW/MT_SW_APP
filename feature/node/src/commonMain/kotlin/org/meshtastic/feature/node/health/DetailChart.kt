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

package org.meshtastic.feature.node.health

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.time.Instant

private const val GROW_MILLIS = 900
private const val MAX_DRAWN_POINTS = 300
private const val FILL_ALPHA = 0.35f
private const val ZONE_ALPHA = 0.14f
private const val GRID_ALPHA = 0.18f
private const val RANGE_PADDING = 0.15f
private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
private const val TWO_DIGITS = 2
private const val DATE_START = 5

/** "HH:mm", or "MM-dd HH:mm" when the chart covers more than a day. */
internal fun chartTimeLabel(millis: Long, withDay: Boolean): String {
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    val time = "${dt.hour.toString().padStart(TWO_DIGITS, '0')}:${dt.minute.toString().padStart(TWO_DIGITS, '0')}"
    return if (withDay) "${dt.date.toString().substring(DATE_START)} $time" else time
}

/**
 * A smooth line chart with a soft gradient below the line, dashed grid, value labels on the left, times underneath and
 * a touch cursor: press or drag on the chart and [onSelect] reports the nearest point (null again when released).
 */
@Composable
internal fun LineAreaChart(
    points: List<MetricPoint>,
    color: Color,
    yLabel: (Float) -> String,
    onSelect: (MetricPoint?) -> Unit,
    modifier: Modifier = Modifier,
    scale: LevelScale? = null,
    palette: Map<HealthLevel, Color> = emptyMap(),
) {
    val sorted = remember(points) { points.filter { it.value.isFinite() }.sortedBy { it.timestampMillis } }
    if (sorted.size < 2) return
    val minV = sorted.minOf { it.value }
    val maxV = sorted.maxOf { it.value }
    val pad = ((maxV - minV).takeIf { it > 0f } ?: (abs(maxV).coerceAtLeast(1f) * 0.1f)) * RANGE_PADDING
    val lo = minV - pad
    val hi = maxV + pad
    val t0 = sorted.first().timestampMillis
    val span = (sorted.last().timestampMillis - t0).coerceAtLeast(1L)

    var selectedX by remember { mutableStateOf<Float?>(null) }
    var width by remember { mutableFloatStateOf(0f) }
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { target = 1f }
    val reveal by animateFloatAsState(target, tween(GROW_MILLIS), label = "reveal")

    fun xOf(point: MetricPoint) = (point.timestampMillis - t0).toFloat() / span * width
    val selected = selectedX?.let { x -> sorted.minByOrNull { abs(xOf(it) - x) } }
    LaunchedEffect(selected) { onSelect(selected) }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = GRID_ALPHA)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall

    Row(modifier = modifier) {
        Column(
            modifier = Modifier.width(58.dp).fillMaxHeight().padding(bottom = 18.dp, end = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            listOf(hi, (hi + lo) / 2, lo).forEach {
                Text(text = yLabel(it), style = labelStyle, color = labelColor, textAlign = TextAlign.End, maxLines = 1)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { width = it.width.toFloat() }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            selectedX = down.position.x
                            do {
                                val event = awaitPointerEvent()
                                event.changes.firstOrNull()?.let { selectedX = it.position.x }
                            } while (event.changes.any { it.pressed })
                            selectedX = null
                        }
                    },
            ) {
                drawChart(
                    sorted = sorted,
                    color = color,
                    lo = lo,
                    hi = hi,
                    t0 = t0,
                    span = span,
                    reveal = reveal,
                    gridColor = gridColor,
                    selected = selected,
                    minPoint = sorted.minByOrNull { it.value },
                    maxPoint = sorted.maxByOrNull { it.value },
                    scale = scale,
                    palette = palette,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val withDay = span > MILLIS_PER_DAY
                Text(chartTimeLabel(t0, withDay), style = labelStyle, color = labelColor)
                Text(chartTimeLabel(t0 + span, withDay), style = labelStyle, color = labelColor)
            }
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
private fun DrawScope.drawChart(
    sorted: List<MetricPoint>,
    color: Color,
    lo: Float,
    hi: Float,
    t0: Long,
    span: Long,
    reveal: Float,
    gridColor: Color,
    selected: MetricPoint?,
    minPoint: MetricPoint?,
    maxPoint: MetricPoint?,
    scale: LevelScale?,
    palette: Map<HealthLevel, Color>,
) {
    val w = size.width
    val h = size.height
    fun x(p: MetricPoint) = (p.timestampMillis - t0).toFloat() / span * w
    fun y(v: Float) = h - (v - lo) / (hi - lo) * h
    fun y(p: MetricPoint) = y(p.value)

    // Coloured zones behind the line: gold = good, red = fair, purple = poor, white = very poor.
    scale?.ranges()?.forEach { (level, lowBound, upBound) ->
        val top = (upBound?.let { y(it) } ?: 0f).coerceIn(0f, h)
        val bottom = (lowBound?.let { y(it) } ?: h).coerceIn(0f, h)
        if (bottom > top) {
            drawRect(
                color = (palette[level] ?: Color.Gray).copy(alpha = ZONE_ALPHA),
                topLeft = Offset(0f, top),
                size = Size(w, bottom - top),
            )
        }
    }

    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    listOf(0f, h / 2, h).forEach {
        drawLine(gridColor, Offset(0f, it), Offset(w, it), strokeWidth = 1.5f, pathEffect = dash)
    }

    val stride = (sorted.size / MAX_DRAWN_POINTS).coerceAtLeast(1)
    val drawn = sorted.filterIndexed { index, _ -> index % stride == 0 || index == sorted.lastIndex }
    val line = Path()
    drawn.forEachIndexed { index, p ->
        if (index == 0) {
            line.moveTo(x(p), y(p))
        } else {
            val prev = drawn[index - 1]
            val midX = (x(prev) + x(p)) / 2
            line.cubicTo(midX, y(prev), midX, y(p), x(p), y(p))
        }
    }
    val area =
        Path().apply {
            addPath(line)
            lineTo(x(drawn.last()), h)
            lineTo(x(drawn.first()), h)
            close()
        }
    clipRect(left = 0f, top = 0f, right = w * reveal, bottom = h) {
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = FILL_ALPHA), Color.Transparent)))
        drawPath(
            line,
            color.copy(alpha = 0.25f),
            style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(line, color, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    listOfNotNull(minPoint, maxPoint).distinct().forEach {
        drawCircle(color, radius = 5f, center = Offset(x(it), y(it)))
        drawCircle(Color.White.copy(alpha = 0.9f), radius = 2f, center = Offset(x(it), y(it)))
    }
    val last = sorted.last()
    drawCircle(color.copy(alpha = 0.25f), radius = 13f, center = Offset(x(last), y(last)))
    drawCircle(color, radius = 7f, center = Offset(x(last), y(last)))

    selected?.let {
        drawLine(color.copy(alpha = 0.6f), Offset(x(it), 0f), Offset(x(it), h), strokeWidth = 2f)
        drawCircle(color.copy(alpha = 0.3f), radius = 16f, center = Offset(x(it), y(it)))
        drawCircle(color, radius = 8f, center = Offset(x(it), y(it)))
        drawCircle(Color.White, radius = 3.5f, center = Offset(x(it), y(it)))
    }
}
