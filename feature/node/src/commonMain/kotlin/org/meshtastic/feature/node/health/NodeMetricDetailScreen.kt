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

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
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
import org.meshtastic.feature.node.metrics.ChartStyling
import org.meshtastic.feature.node.metrics.CommonCharts
import org.meshtastic.feature.node.metrics.GenericMetricChart
import org.meshtastic.feature.node.metrics.MetricChartScaffold

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NodeMetricRange.entries.forEach { range ->
                    FilterChip(
                        selected = uiState.range == range,
                        onClick = { viewModel.setRange(range) },
                        label = { Text(range.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val seriesByLabel = uiState.series.associateBy { it.label }
            // Groups (from seriesGroups()) share magnitude, so group 0 → left axis, group 1 → right axis; all
            // series within a group plot on that one real-value axis since their values are already close. Any
            // series not covered by seriesGroups() (e.g. the SIGNAL tab's "Przeskoki" fallback) still gets shown,
            // as its own singleton group, rather than silently disappearing.
            val definedGroups = uiState.metric.seriesGroups().map { labels -> labels.mapNotNull { seriesByLabel[it] } }
            val knownLabels = uiState.metric.seriesGroups().flatten().toSet()
            val extraGroups = uiState.series.filter { it.label !in knownLabels }.map { listOf(it) }
            val allGroups = definedGroups + extraGroups
            // Stats show with just 1 reading; the chart line itself needs 2+ points to draw anything meaningful.
            val statGroups = allGroups.map { g -> g.filter { it.points.isNotEmpty() } }.filter { it.isNotEmpty() }
            val groups = allGroups.map { g -> g.filter { it.points.size >= 2 } }.filter { it.isNotEmpty() }

            groups.forEachIndexed { groupIndex, groupSeries ->
                groupSeries.forEach { s ->
                    val color = colorFor(groups, groupIndex, s.label)
                    // Traffic counters (except RX itself) also show their share of all received packets.
                    val rx = seriesByLabel["RX"].takeIf { s.label in RX_SHARE_LABELS }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                Text(
                                    text = s.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                StatLabel(
                                    label = "min",
                                    value = s.minValue,
                                    seriesLabel = s.label,
                                    color = color,
                                    rxTotal = rx?.minValue,
                                )
                                StatLabel(
                                    label = "max",
                                    value = s.maxValue,
                                    seriesLabel = s.label,
                                    color = color,
                                    rxTotal = rx?.maxValue,
                                )
                                StatLabel(
                                    label = "ostatnia",
                                    value = s.latestValue,
                                    seriesLabel = s.label,
                                    color = color,
                                    rxTotal = rx?.latestValue,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (groups.isEmpty()) {
                Text(
                    text = "Za mało danych do wykresu — wróć za jakiś czas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MetricChartScaffold(
                    isEmpty = false,
                    legendData = emptyList(),
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                ) { modelProducer, chartModifier ->
                    LaunchedEffect(groups) {
                        modelProducer.runTransaction {
                            groups.forEach { groupSeries ->
                                groupSeries.forEach { s ->
                                    lineModel {
                                        series(
                                            x = s.points.map { it.timestampMillis / MILLIS_PER_SEC },
                                            y = s.points.map { it.value },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    val layers =
                        groups.flatMapIndexed { groupIndex, groupSeries ->
                            groupSeries.map { s ->
                                val color = colorFor(groups, groupIndex, s.label)
                                rememberLineCartesianLayer(
                                    lineProvider =
                                    LineCartesianLayer.LineProvider.series(ChartStyling.createBoldLine(color)),
                                    verticalAxisPosition =
                                    if (groupIndex == 0) {
                                        Axis.Position.Vertical.Start
                                    } else {
                                        Axis.Position.Vertical.End
                                    },
                                )
                            }
                        }
                    val startColor = groups.getOrNull(0)?.firstOrNull()?.let { colorFor(groups, 0, it.label) }
                    val endColor = groups.getOrNull(1)?.firstOrNull()?.let { colorFor(groups, 1, it.label) }
                    GenericMetricChart(
                        modelProducer = modelProducer,
                        modifier = chartModifier,
                        layers = layers,
                        startAxis =
                        startColor?.let {
                            VerticalAxis.rememberStart(label = ChartStyling.rememberAxisLabel(color = it))
                        },
                        endAxis =
                        endColor?.let {
                            VerticalAxis.rememberEnd(label = ChartStyling.rememberAxisLabel(color = it))
                        },
                        bottomAxis = CommonCharts.rememberBottomTimeAxis(),
                    )
                }
            }
        }
    }
}

/** Assigns a distinct color to each series across all groups, offsetting by how many series preceded it. */
private fun colorFor(groups: List<List<MetricSeries>>, groupIndex: Int, label: String): Color {
    var flatIndex = 0
    for ((gi, g) in groups.withIndex()) {
        for (s in g) {
            if (gi == groupIndex && s.label == label) return SERIES_COLORS.getOrElse(flatIndex) { Color.Gray }
            flatIndex++
        }
    }
    return Color.Gray
}

private const val MILLIS_PER_SEC = 1000L

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
private const val HERO_ICON_DP = 56

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
    val headline = uiState.series.firstOrNull { it.label == uiState.metric.seriesLabels().first() }
    val latest = headline?.latestValue
    val level = headlineLevel(uiState.metric, latest)
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
            Box(
                modifier =
                Modifier.size(HERO_ICON_DP.dp).clip(CircleShape).background(accent.copy(alpha = HERO_ICON_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = uiState.metric.icon(),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(30.dp),
                )
            }
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
