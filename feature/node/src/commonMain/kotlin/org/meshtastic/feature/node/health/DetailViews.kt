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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.meshtastic.core.ui.icon.KeyboardArrowDown
import org.meshtastic.core.ui.icon.KeyboardArrowUp
import org.meshtastic.core.ui.icon.MeshtasticIcons
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Instant

/** The ways the same numbers can be shown on the node detail screen. */
internal enum class DetailViewMode(val label: String) {
    LINE("Linia"),
    GAUGE("Zegar"),
    BARS("Słupki"),
    HEAT("Mapa cieplna"),
}

/** The chosen view is kept while the app runs, so it applies to every node and tab. */
internal object DetailViewModeState {
    var value by mutableStateOf(DetailViewMode.LINE)
}

/** Limits that turn a value into a verdict, for the series that have them (battery, SNR, channel use). */
internal data class LevelScale(
    val good: Float,
    val medium: Float,
    val bad: Float,
    val higherIsBetter: Boolean,
    val min: Float,
    val max: Float,
) {
    fun level(v: Float): HealthLevel = if (higherIsBetter) {
        when {
            v >= good -> HealthLevel.GOOD
            v >= medium -> HealthLevel.MEDIUM
            v >= bad -> HealthLevel.BAD
            else -> HealthLevel.CRITICAL
        }
    } else {
        when {
            v < good -> HealthLevel.GOOD
            v < medium -> HealthLevel.MEDIUM
            v < bad -> HealthLevel.BAD
            else -> HealthLevel.CRITICAL
        }
    }

    /** Value range of each verdict as (verdict, lower bound, upper bound); null means open ended. */
    fun ranges(): List<Triple<HealthLevel, Float?, Float?>> = if (higherIsBetter) {
        listOf(
            Triple(HealthLevel.GOOD, good, null),
            Triple(HealthLevel.MEDIUM, medium, good),
            Triple(HealthLevel.BAD, bad, medium),
            Triple(HealthLevel.CRITICAL, null, bad),
        )
    } else {
        listOf(
            Triple(HealthLevel.GOOD, null, good),
            Triple(HealthLevel.MEDIUM, good, medium),
            Triple(HealthLevel.BAD, medium, bad),
            Triple(HealthLevel.CRITICAL, bad, null),
        )
    }
}

/** Same limits as the rest of the Network Health screens. */
internal fun scaleFor(metric: NetworkHealthMetric, label: String): LevelScale? = when {
    metric == NetworkHealthMetric.POWER && label == "Bateria" -> LevelScale(50f, 20f, 5f, true, 0f, 100f)
    metric == NetworkHealthMetric.SIGNAL && label == "SNR" -> LevelScale(0f, -7.5f, -15f, true, -20f, 10f)
    metric == NetworkHealthMetric.ETHER && label == "Ch. Util" -> LevelScale(15f, 25f, 40f, false, 0f, 60f)
    metric == NetworkHealthMetric.SIGNAL && label == "Noise Floor" -> LevelScale(-100f, -90f, -80f, false, -130f, -60f)
    else -> null
}

/** Colour of every verdict, resolved once. */
@Composable
internal fun levelPalette(): Map<HealthLevel, Color> = mapOf(
    HealthLevel.GOOD to HealthLevel.GOOD.color(),
    HealthLevel.MEDIUM to HealthLevel.MEDIUM.color(),
    HealthLevel.BAD to HealthLevel.BAD.color(),
    HealthLevel.CRITICAL to HealthLevel.CRITICAL.color(),
)

private val LEVEL_ORDER = listOf(HealthLevel.GOOD, HealthLevel.MEDIUM, HealthLevel.BAD, HealthLevel.CRITICAL)
private val LEVEL_WORDS = listOf("dobrze", "średnio", "źle", "bardzo źle")
private const val MAX_GAP_MILLIS = 2 * 60 * 60 * 1000L
private const val PERCENT = 100f
private const val STRIP_HEIGHT_DP = 12
private const val MIN_WEIGHT = 0.0001f

/** One coloured strip: how much of the time the value was in each verdict. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimeInStateStrip(
    points: List<MetricPoint>,
    scale: LevelScale,
    palette: Map<HealthLevel, Color>,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(points) { points.sortedBy { it.timestampMillis } }
    val weights = remember(sorted, scale) { timeInLevels(sorted, scale) }
    val total = weights.sum()
    if (total <= 0f) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Czas w każdym stanie",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier =
            Modifier.fillMaxWidth().height(STRIP_HEIGHT_DP.dp).clip(RoundedCornerShape(STRIP_HEIGHT_DP.dp / 2)),
        ) {
            LEVEL_ORDER.forEachIndexed { i, level ->
                if (weights[i] > 0f) {
                    Box(
                        modifier =
                        Modifier
                            .weight(weights[i].coerceAtLeast(MIN_WEIGHT))
                            .fillMaxHeight()
                            .background(palette[level] ?: Color.Gray),
                    )
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            LEVEL_ORDER.forEachIndexed { i, level ->
                if (weights[i] > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        HealthDot(color = palette[level] ?: Color.Gray)
                        Text(
                            text = "${(weights[i] / total * PERCENT).roundToInt()}% ${LEVEL_WORDS[i]}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

/** Time spent in each verdict (good, fair, poor, very poor); long gaps without data count for at most two hours. */
private fun timeInLevels(sorted: List<MetricPoint>, scale: LevelScale): FloatArray {
    val weights = FloatArray(LEVEL_ORDER.size)
    sorted.forEachIndexed { i, p ->
        val next = sorted.getOrNull(i + 1) ?: return@forEachIndexed
        val dt = min(next.timestampMillis - p.timestampMillis, MAX_GAP_MILLIS)
        weights[LEVEL_ORDER.indexOf(scale.level(p.value))] += dt.toFloat()
    }
    return weights
}

private const val GAUGE_STROKE_DP = 18
private const val GAUGE_HEIGHT_DP = 130
private const val TREND_LIMIT = 0.02f

/** A half-circle gauge with the verdict zones, a marker at the latest value, and min / average / max / trend below. */
@Composable
internal fun GaugeView(
    series: MetricSeries,
    color: Color,
    scale: LevelScale?,
    palette: Map<HealthLevel, Color>,
    format: (Float) -> String,
    modifier: Modifier = Modifier,
) {
    val values = remember(series.points) { series.points.sortedBy { it.timestampMillis }.map { it.value } }
    val latest = series.latestValue ?: values.lastOrNull() ?: return
    val minV = series.minValue ?: values.minOrNull() ?: return
    val maxV = series.maxValue ?: values.maxOrNull() ?: return
    val pad = ((maxV - minV).takeIf { it > 0f } ?: 1f) * 0.1f
    val gMin = scale?.min ?: (minV - pad)
    val gMax = scale?.max ?: (maxV + pad)
    val range = (gMax - gMin).takeIf { it > 0f } ?: 1f
    val fraction = ((latest - gMin) / range).coerceIn(0f, 1f)
    val markerColor = scale?.let { palette[it.level(latest)] } ?: color
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val average = remember(values) { values.average().toFloat() }
    val third = (values.size / 3).coerceAtLeast(1)
    val delta = remember(values) { values.takeLast(third).average().toFloat() - values.take(third).average().toFloat() }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(GAUGE_HEIGHT_DP.dp), contentAlignment = Alignment.BottomCenter) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = GAUGE_STROKE_DP.dp.toPx()
                val radius = min(size.width / 2, size.height) - stroke
                val center = Offset(size.width / 2, size.height - stroke / 2)
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)

                fun frac(v: Float) = ((v - gMin) / range).coerceIn(0f, 1f)
                if (scale != null) {
                    scale.ranges().forEach { (level, lowB, upB) ->
                        val f1 = frac(lowB ?: gMin)
                        val f2 = frac(upB ?: gMax)
                        if (f2 > f1) {
                            drawArc(
                                color = (palette[level] ?: Color.Gray).copy(alpha = 0.85f),
                                startAngle = 180f + f1 * 180f,
                                sweepAngle = (f2 - f1) * 180f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke),
                            )
                        }
                    }
                } else {
                    drawArc(track, 180f, 180f, false, topLeft, arcSize, style = Stroke(width = stroke))
                    drawArc(color, 180f, 180f * fraction, false, topLeft, arcSize, style = Stroke(width = stroke))
                }
                val angle = (180f + fraction * 180f) * (kotlin.math.PI.toFloat() / 180f)
                val marker = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
                val innerR = radius - stroke * 1.4f
                val inner = Offset(center.x + innerR * cos(angle), center.y + innerR * sin(angle))
                drawLine(Color.White, inner, marker, strokeWidth = 5f)
                drawCircle(Color.White, radius = stroke * 0.55f, center = marker)
                drawCircle(markerColor, radius = stroke * 0.38f, center = marker)
            }
            Text(
                text = format(latest),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = markerColor,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GaugeStat("min", format(minV))
            GaugeStat("średnio", format(average))
            GaugeStat("max", format(maxV))
            TrendStat(delta = delta, limit = range * TREND_LIMIT, format = format)
        }
    }
}

@Composable
private fun GaugeStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TrendStat(delta: Float, limit: Float, format: (Float) -> String) {
    Column(horizontalAlignment = Alignment.End) {
        Text("trend", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                delta > limit -> {
                    Icon(MeshtasticIcons.KeyboardArrowUp, null, modifier = Modifier.height(18.dp))
                    Text(format(abs(delta)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                delta < -limit -> {
                    Icon(MeshtasticIcons.KeyboardArrowDown, null, modifier = Modifier.height(18.dp))
                    Text(format(abs(delta)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                else -> Text("stabilnie", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private const val BAR_BUCKETS = 24
private const val BAR_GAP_FRACTION = 0.2f

/** Bars of the average per time slot (24 slots across the chosen range); touching a bar reports its average. */
@Composable
internal fun BucketBarsView(
    points: List<MetricPoint>,
    color: Color,
    scale: LevelScale?,
    palette: Map<HealthLevel, Color>,
    yLabel: (Float) -> String,
    onSelect: (MetricPoint?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(points) { points.filter { it.value.isFinite() }.sortedBy { it.timestampMillis } }
    if (sorted.isEmpty()) return
    val t0 = sorted.first().timestampMillis
    val span = (sorted.last().timestampMillis - t0).coerceAtLeast(1L)
    val buckets =
        remember(sorted) {
            val sums = DoubleArray(BAR_BUCKETS)
            val counts = IntArray(BAR_BUCKETS)
            sorted.forEach { p ->
                val i = (((p.timestampMillis - t0) * BAR_BUCKETS) / span).toInt().coerceIn(0, BAR_BUCKETS - 1)
                sums[i] += p.value.toDouble()
                counts[i]++
            }
            (0 until BAR_BUCKETS).map { i ->
                if (counts[i] == 0) {
                    null
                } else {
                    val mid = t0 + span * (2 * i + 1) / (2 * BAR_BUCKETS)
                    MetricPoint(mid, (sums[i] / counts[i]).toFloat())
                }
            }
        }
    val present = buckets.filterNotNull()
    if (present.isEmpty()) return
    val minV = present.minOf { it.value }
    val maxV = present.maxOf { it.value }
    val lo = if (minV >= 0f) 0f else minV - (maxV - minV).coerceAtLeast(1f) * 0.1f
    val hi = (maxV + (maxV - lo).coerceAtLeast(1f) * 0.1f)

    var selectedX by remember { mutableStateOf<Float?>(null) }
    var width by remember { mutableFloatStateOf(0f) }
    val selectedIndex = selectedX?.let { x -> ((x / width) * BAR_BUCKETS).toInt().coerceIn(0, BAR_BUCKETS - 1) }
    val selected = selectedIndex?.let { buckets[it] }
    LaunchedEffect(selected) { onSelect(selected) }
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
                val slot = size.width / BAR_BUCKETS
                buckets.forEachIndexed { i, bucket ->
                    if (bucket != null) {
                        val barH = ((bucket.value - lo) / (hi - lo)).coerceIn(0f, 1f) * size.height
                        val tint = scale?.let { palette[it.level(bucket.value)] } ?: color
                        val alpha = if (selectedIndex == null || selectedIndex == i) 1f else 0.45f
                        drawRoundRect(
                            color = tint.copy(alpha = alpha),
                            topLeft = Offset(i * slot + slot * BAR_GAP_FRACTION / 2, size.height - barH),
                            size = Size(slot * (1 - BAR_GAP_FRACTION), barH.coerceAtLeast(2f)),
                            cornerRadius = CornerRadius(6f, 6f),
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val withDay = span > 24 * 60 * 60 * 1000L
                Text(chartTimeLabel(t0, withDay), style = labelStyle, color = labelColor)
                Text(chartTimeLabel(t0 + span, withDay), style = labelStyle, color = labelColor)
            }
        }
    }
}

private const val HEAT_HOURS = 24
private const val HEAT_MAX_ROWS = 31
private const val HEAT_CELL_DP = 20
private const val HEAT_LABEL_DP = 46
private const val HEAT_HOUR_STEP = 6
private const val DATE_START = 5
private const val HEAT_CELL_GAP_PX = 2f
private const val HEAT_CELL_CORNER_PX = 5f

/**
 * A grid of hours (across) by days (down); every cell is the average for that hour, coloured by verdict. The whole grid
 * is drawn on one canvas (not hundreds of separate boxes), so even a month of data stays smooth.
 */
@Composable
internal fun HeatmapView(
    points: List<MetricPoint>,
    color: Color,
    scale: LevelScale?,
    palette: Map<HealthLevel, Color>,
    onSelect: (MetricPoint?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val data =
        remember(points) {
            val zone = TimeZone.currentSystemDefault()
            val sums = mutableMapOf<Pair<String, Int>, DoubleArray>()
            val firstTime = mutableMapOf<Pair<String, Int>, Long>()
            points.forEach { p ->
                val dt = Instant.fromEpochMilliseconds(p.timestampMillis).toLocalDateTime(zone)
                val key = dt.date.toString() to dt.hour
                val acc = sums.getOrPut(key) { DoubleArray(2) }
                acc[0] += p.value.toDouble()
                acc[1] += 1.0
                firstTime.getOrPut(key) { p.timestampMillis }
            }
            val averages =
                sums.mapValues { (key, acc) -> MetricPoint(firstTime.getValue(key), (acc[0] / acc[1]).toFloat()) }
            averages to averages.keys.map { it.first }.distinct().sorted().takeLast(HEAT_MAX_ROWS)
        }
    val averages = data.first
    val days = data.second
    if (days.isEmpty()) return
    val minV = averages.values.minOf { it.value }
    val maxV = averages.values.maxOf { it.value }
    val spread = (maxV - minV).takeIf { it > 0f } ?: 1f
    val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(HEAT_LABEL_DP.dp))
            (0 until HEAT_HOURS step HEAT_HOUR_STEP).forEach { hour ->
                Text(
                    text = "$hour:00",
                    style = labelStyle,
                    color = labelColor,
                    modifier = Modifier.weight(HEAT_HOUR_STEP.toFloat()),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.width(HEAT_LABEL_DP.dp)) {
                days.forEach { day ->
                    Box(modifier = Modifier.height(HEAT_CELL_DP.dp), contentAlignment = Alignment.CenterStart) {
                        Text(text = day.substring(DATE_START), style = labelStyle, color = labelColor)
                    }
                }
            }
            Canvas(
                modifier =
                Modifier
                    .weight(1f)
                    .height((HEAT_CELL_DP * days.size).dp)
                    .pointerInput(days, averages) {
                        detectTapGestures { pos ->
                            val col = (pos.x / (size.width / HEAT_HOURS.toFloat())).toInt()
                            val row = (pos.y / (size.height / days.size.toFloat())).toInt()
                            if (col in 0 until HEAT_HOURS && row in days.indices) {
                                onSelect(averages[days[row] to col])
                            }
                        }
                    },
            ) {
                val cw = size.width / HEAT_HOURS
                val ch = size.height / days.size
                days.forEachIndexed { r, day ->
                    for (hour in 0 until HEAT_HOURS) {
                        val cell = averages[day to hour]
                        val tint =
                            when {
                                cell == null -> empty
                                scale != null -> palette[scale.level(cell.value)] ?: color
                                else -> color.copy(alpha = 0.2f + 0.8f * ((cell.value - minV) / spread))
                            }
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(hour * cw + HEAT_CELL_GAP_PX / 2, r * ch + HEAT_CELL_GAP_PX / 2),
                            size = Size(cw - HEAT_CELL_GAP_PX, ch - HEAT_CELL_GAP_PX),
                            cornerRadius = CornerRadius(HEAT_CELL_CORNER_PX, HEAT_CELL_CORNER_PX),
                        )
                    }
                }
            }
        }
    }
}
