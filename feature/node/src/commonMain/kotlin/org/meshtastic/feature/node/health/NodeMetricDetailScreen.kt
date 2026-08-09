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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.ui.theme.GraphColors.Blue
import org.meshtastic.core.ui.theme.GraphColors.Gold
import org.meshtastic.core.ui.theme.GraphColors.Green
import org.meshtastic.core.ui.theme.GraphColors.Orange
import org.meshtastic.core.ui.theme.GraphColors.Red
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("${uiState.metric.label} — ${uiState.nodeName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(16.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                        StatLabel(label = "${s.label} — min", value = s.minValue, seriesLabel = s.label, color = color)
                        StatLabel(label = "max", value = s.maxValue, seriesLabel = s.label, color = color)
                        StatLabel(label = "ostatnia", value = s.latestValue, seriesLabel = s.label, color = color)
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
                    modifier = Modifier.fillMaxWidth().height(260.dp),
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

@Composable
private fun StatLabel(
    label: String,
    value: Float?,
    seriesLabel: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value?.let { formatSeriesValue(seriesLabel, it) } ?: "—",
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

    else -> NumberFormatter.format(value, 1)
}
