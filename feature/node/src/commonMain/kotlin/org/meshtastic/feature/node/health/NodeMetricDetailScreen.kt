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
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.ui.icon.ArrowBack
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.theme.GraphColors.Blue
import org.meshtastic.core.ui.theme.GraphColors.Gold
import org.meshtastic.core.ui.theme.GraphColors.Green
import org.meshtastic.core.ui.theme.GraphColors.Orange
import org.meshtastic.core.ui.theme.GraphColors.Red
import org.meshtastic.core.ui.util.LnaCorrection
import org.meshtastic.core.ui.util.LocalLnaCorrection

private val SERIES_COLORS = listOf(Green, Blue, Gold, Orange, Red)

@Composable
fun NodeMetricDetailScreen(
    viewModel: NodeMetricDetailViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rawState by viewModel.uiState.collectAsStateWithLifecycle()
    // LNA gain correction (display-only): RSSI by our own gain, noise floor by the viewed node's gain.
    val lna = LocalLnaCorrection.current
    val nodeNum = viewModel.nodeNum
    val uiState =
        remember(rawState, lna, nodeNum) { rawState.copy(series = rawState.series.map { it.withLnaCorrection(lna, nodeNum) }) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("${uiState.metric.label} — ${uiState.nodeName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(imageVector = MeshtasticIcons.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(16.dp),
        ) {
            NodeMetricHero(uiState = uiState)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                NodeMetricRange.entries.forEach { range ->
                    FilterChip(
                        selected = uiState.range == range,
                        onClick = { viewModel.setRange(range) },
                        label = { Text(range.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                DetailViewMode.entries.forEach { mode ->
                    FilterChip(
                        selected = DetailViewModeState.value == mode,
                        onClick = { DetailViewModeState.value = mode },
                        label = { Text(mode.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val seriesByLabel = uiState.series.associateBy { it.label }
            // Series not covered by seriesGroups() (e.g. the SIGNAL tab's "Przeskoki") are still shown, after the rest.
            val knownLabels = uiState.metric.seriesGroups().flatten().toSet()
            val ordered =
                uiState.metric.seriesGroups().flatten().mapNotNull { seriesByLabel[it] } +
                    uiState.series.filter { it.label !in knownLabels }
            val headlineLabel = headlineLabelFor(uiState)
            val shown =
                ordered.filter { it.points.isNotEmpty() }.sortedBy { if (it.label == headlineLabel) 0 else 1 }

            if (shown.isEmpty()) {
                Text(
                    text = "Brak danych w tym zakresie — wróć za jakiś czas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            shown.forEachIndexed { index, s ->
                // Traffic counters (except RX itself) also show their share of all received packets.
                val rx = seriesByLabel["RX"].takeIf { s.label in RX_SHARE_LABELS }
                SeriesCard(
                    series = s,
                    color = SERIES_COLORS.getOrElse(index) { Color.Gray },
                    rx = rx,
                    scale = scaleFor(uiState.metric, s.label),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/** Which series the top card shows: the noise floor on the signal tab (the colour still comes from the SNR). */
private fun headlineLabelFor(uiState: NodeMetricDetailUiState): String {
    val first = uiState.metric.seriesLabels().first()
    val preferred = if (uiState.metric == NetworkHealthMetric.SIGNAL) "Noise Floor" else first
    return if (uiState.series.any { it.label == preferred && it.points.isNotEmpty() }) {
        preferred
    } else {
        first
    }
}

/** One series: its name and latest (or touched) value, the chart, and min / max / latest underneath. */
@Composable
private fun SeriesCard(series: MetricSeries, color: Color, rx: MetricSeries?, scale: LevelScale?) {
    val palette = levelPalette()
    var touched by remember { mutableStateOf<MetricPoint?>(null) }
    val shownValue = touched?.value ?: series.latestValue
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(
                    text = series.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = shownValue?.let { formatSeriesValue(series.label, it) } ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                    touched?.let {
                        Text(
                            text = chartTimeLabel(it.timestampMillis, withDay = true),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (scale != null) TimeInStateStrip(points = series.points, scale = scale, palette = palette)
            val format = { v: Float -> formatSeriesValue(series.label, v) }
            val chartModifier = Modifier.fillMaxWidth().height(CHART_HEIGHT_DP.dp)
            val enoughPoints = series.points.size >= 2
            when {
                DetailViewModeState.value == DetailViewMode.GAUGE ->
                    GaugeView(series = series, color = color, scale = scale, palette = palette, format = format)

                !enoughPoints ->
                    Text(
                        text = "Za mało pomiarów do wykresu — wróć za jakiś czas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                DetailViewModeState.value == DetailViewMode.BARS ->
                    BucketBarsView(
                        points = series.points,
                        color = color,
                        scale = scale,
                        palette = palette,
                        yLabel = format,
                        onSelect = { touched = it },
                        modifier = chartModifier,
                    )

                DetailViewModeState.value == DetailViewMode.HEAT ->
                    HeatmapView(
                        points = series.points,
                        color = color,
                        scale = scale,
                        palette = palette,
                        onSelect = { touched = it },
                    )

                else ->
                    LineAreaChart(
                        points = series.points,
                        color = color,
                        yLabel = format,
                        onSelect = { touched = it },
                        modifier = chartModifier,
                        scale = scale,
                        palette = palette,
                    )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                StatLabel("min", series.minValue, series.label, color, rx?.minValue)
                StatLabel("max", series.maxValue, series.label, color, rx?.maxValue)
                StatLabel("ostatnia", series.latestValue, series.label, color, rx?.latestValue)
            }
        }
    }
}

private const val CHART_HEIGHT_DP = 170

/** RSSI is corrected by our own LNA gain, "Noise Floor" by the viewed node's gain; other series are untouched. */
private fun MetricSeries.withLnaCorrection(lna: LnaCorrection, nodeNum: Int): MetricSeries {
    val gain =
        when (label) {
            "RSSI" -> lna.localGain
            "Noise Floor" -> lna.gains[nodeNum] ?: 0
            else -> 0
        }
    if (gain == 0) return this
    return copy(
        points = points.map { it.copy(value = it.value - gain) },
        minValue = minValue?.minus(gain),
        maxValue = maxValue?.minus(gain),
        latestValue = latestValue?.minus(gain),
    )
}

private val RX_SHARE_LABELS = setOf("TX", "Duplikaty", "Przekazane", "Uszkodzone")

/** " (2,5%)" — [value] as a share of the received-packet total, or nothing when that total is unknown/zero. */
private fun rxSharePart(value: Float, rxTotal: Float?): String =
    if (rxTotal != null && rxTotal > 0f) " (${MetricFormatter.percent(value * 100f / rxTotal, decimalPlaces = 1)})" else ""

@Composable
private fun StatLabel(
    label: String,
    value: Float?,
    seriesLabel: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    rxTotal: Float? = null,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
            value?.let { formatSeriesValue(seriesLabel, it) + rxSharePart(it, rxTotal) } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
    }
}

/** Formats one series' stat value with the unit appropriate to that metric, via the shared [MetricFormatter]. */
private fun formatSeriesValue(seriesLabel: String, value: Float): String = when (seriesLabel) {
    "Bateria" -> MetricFormatter.percent(value, decimalPlaces = 1)

    "Napięcie" -> MetricFormatter.voltage(value, decimalPlaces = 2)

    "Prąd" -> MetricFormatter.current(value, decimalPlaces = 2)

    "SNR" -> MetricFormatter.snr(value)

    "RSSI" -> MetricFormatter.rssi(value.toInt())

    "Noise Floor" -> "${NumberFormatter.format(value, 1)} dB"

    "WiFi RSSI" -> MetricFormatter.rssi(value.toInt())

    "Zerwania WiFi" -> NumberFormatter.format(value, 0)

    "Ch. Util",
    "Air Util",
    -> MetricFormatter.percent(value, decimalPlaces = 2)

    "Temperatura" -> MetricFormatter.temperature(value, isFahrenheit = false)

    "Wilgotność" -> MetricFormatter.percent(value, decimalPlaces = 1)

    "Ciśnienie" -> MetricFormatter.pressure(value, decimalPlaces = 1)

    "TX",
    "RX",
    "Duplikaty",
    "Przekazane",
    "Uszkodzone",
    "Przeskoki",
    -> "${value.toInt()}"

    "CPU" -> MetricFormatter.percent(value, decimalPlaces = 0)

    "Heap",
    "Flash",
    "PSRAM",
    -> MetricFormatter.percent(value, decimalPlaces = 1)

    else -> NumberFormatter.format(value, 1)
}

private const val HERO_TINT_ALPHA = 0.14f
private const val HERO_ICON_ALPHA = 0.18f
private const val HERO_ICON_DP = 76

private const val BATTERY_GOOD = 50f
private const val BATTERY_MEDIUM = 20f
private const val BATTERY_BAD = 5f

/** Verdict for the headline value of a tab, or [HealthLevel.UNKNOWN] when that tab has no verdict. */
private fun headlineLevel(metric: NetworkHealthMetric, value: Float?): HealthLevel {
    if (value == null) return HealthLevel.UNKNOWN
    return when (metric) {
        NetworkHealthMetric.POWER ->
            when {
                value >= BATTERY_GOOD -> HealthLevel.GOOD
                value >= BATTERY_MEDIUM -> HealthLevel.MEDIUM
                value >= BATTERY_BAD -> HealthLevel.BAD
                else -> HealthLevel.CRITICAL
            }

        NetworkHealthMetric.SIGNAL -> snrLevel(value)
        NetworkHealthMetric.ETHER -> etherLevel(value)
        else -> HealthLevel.UNKNOWN
    }
}

/** Top card of the detail screen: tab icon in the verdict colour, node name, the latest value and a trend line. */
@Composable
private fun NodeMetricHero(uiState: NodeMetricDetailUiState) {
    val headline = uiState.series.firstOrNull { it.label == headlineLabelFor(uiState) }
    val latest = headline?.latestValue
    // The verdict comes from the SNR on the signal tab, even though the big number is the noise floor.
    val verdictValue =
        if (uiState.metric == NetworkHealthMetric.SIGNAL) {
            uiState.series.firstOrNull { it.label == "SNR" }?.latestValue
        } else {
            latest
        }
    val noiseRaw = uiState.series.firstOrNull { it.label == "Noise Floor" }?.latestValue
    // The signal tab shows the noise floor, so the verdict and the ring follow it; without one the SNR decides.
    val level =
        if (uiState.metric == NetworkHealthMetric.SIGNAL && noiseRaw != null) {
            noiseFloorLevel(noiseRaw)
        } else {
            headlineLevel(uiState.metric, verdictValue)
        }
    val gaugeFraction =
        if (uiState.metric == NetworkHealthMetric.SIGNAL && noiseRaw != null) {
            ((NOISE_RING_MAX - noiseRaw) / NOISE_RING_RANGE).coerceIn(0f, 1f)
        } else {
            gaugeFraction(uiState.metric, verdictValue)
        }
    val accent = if (level == HealthLevel.UNKNOWN) MaterialTheme.colorScheme.primary else level.color()
    val base = MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = HERO_TINT_ALPHA).compositeOver(base)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GaugeIcon(icon = uiState.metric.icon(), accent = accent, fraction = gaugeFraction)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = uiState.nodeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = uiState.metric.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                    if (headline != null && latest != null) formatSeriesValue(headline.label, latest) else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                if (level != HealthLevel.UNKNOWN) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        HealthDot(color = accent)
                        Text(
                            text = stringResource(level.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                        )
                    }
                }
            }
            MetricSparkline(
                values = headline?.points?.map { it.value }.orEmpty(),
                color = accent,
                modifier = Modifier.size(width = 90.dp, height = 44.dp),
            )
        }
    }
}

private const val NOISE_RING_MAX = -80f
private const val NOISE_RING_RANGE = 50f
private const val SNR_GAUGE_MIN = -20f
private const val SNR_GAUGE_RANGE = 30f
private const val PERCENT_MAX = 100f

/** How full the ring around the icon is (0 to 1); a full faint ring when the tab has no scale. */
private fun gaugeFraction(metric: NetworkHealthMetric, value: Float?): Float {
    if (value == null) return 0f
    return when (metric) {
        NetworkHealthMetric.POWER,
        NetworkHealthMetric.ETHER,
        -> value / PERCENT_MAX

        NetworkHealthMetric.SIGNAL -> (value - SNR_GAUGE_MIN) / SNR_GAUGE_RANGE
        else -> 1f
    }.coerceIn(0f, 1f)
}

private const val GAUGE_START_ANGLE = 135f
private const val GAUGE_SWEEP = 270f
private const val GAUGE_ANIMATION_MILLIS = 900

/** The tab icon in the middle of a glowing 270 degree gauge that fills to [fraction]. */
@Composable
private fun GaugeIcon(icon: ImageVector, accent: Color, fraction: Float) {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fraction) { target = fraction }
    val filled by animateFloatAsState(target, tween(GAUGE_ANIMATION_MILLIS), label = "gauge")
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Box(modifier = Modifier.size(HERO_ICON_DP.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawCircle(
                brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.30f), Color.Transparent)),
                radius = size.minDimension / 2,
            )
            drawArc(
                color = track,
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP * filled,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(HERO_GLYPH_DP.dp))
    }
}

private const val HERO_GLYPH_DP = 30
