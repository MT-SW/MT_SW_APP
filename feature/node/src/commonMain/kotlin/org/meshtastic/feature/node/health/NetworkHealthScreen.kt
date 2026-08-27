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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.meshtastic.core.common.util.MetricFormatter

@Composable
fun NetworkHealthScreen(
    viewModel: NetworkHealthViewModel,
    onNodeClick: (Int, NetworkHealthMetric) -> Unit,
    onNeighborInfoNodeClick: (Int) -> Unit,
    onSummaryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Zdrowie sieci") },
                actions = {
                    androidx.compose.material3.IconButton(onClick = onSummaryClick) {
                        Text("📋", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding)) {
            androidx.compose.material3.OutlinedTextField(
                value = uiState.searchText,
                onValueChange = viewModel::setSearchText,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Szukaj węzła…") },
                singleLine = true,
            )

            ScrollableTabRow(selectedTabIndex = uiState.activeMetric.ordinal) {
                NetworkHealthMetric.entries.forEach { metric ->
                    Tab(
                        selected = uiState.activeMetric == metric,
                        onClick = { viewModel.setActiveMetric(metric) },
                        text = { Text(metric.label) },
                    )
                }
            }

            NetworkHealthSortControls(
                sortField = uiState.sortField,
                sortAscending = uiState.sortAscending,
                favoritesFirst = uiState.favoritesFirst,
                hideWithoutData = uiState.hideWithoutData,
                onSortFieldChange = viewModel::setSortField,
                onToggleDirection = viewModel::toggleSortDirection,
                onToggleFavoritesFirst = viewModel::toggleFavoritesFirst,
                onToggleHideWithoutData = viewModel::toggleHideWithoutData,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(uiState.nodes, key = { it.num }) { node ->
                    NodeHealthRow(
                        node = node,
                        metric = uiState.activeMetric,
                        onClick = {
                            if (uiState.activeMetric == NetworkHealthMetric.NEIGHBORS) {
                                onNeighborInfoNodeClick(node.num)
                            } else {
                                onNodeClick(node.num, uiState.activeMetric)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkHealthSortControls(
    sortField: NetworkHealthSortField,
    sortAscending: Boolean,
    favoritesFirst: Boolean,
    hideWithoutData: Boolean,
    onSortFieldChange: (NetworkHealthSortField) -> Unit,
    onToggleDirection: () -> Unit,
    onToggleFavoritesFirst: () -> Unit,
    onToggleHideWithoutData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        androidx.compose.material3.FilterChip(
            selected = sortField == NetworkHealthSortField.METRIC_VALUE,
            onClick = { onSortFieldChange(NetworkHealthSortField.METRIC_VALUE) },
            label = { Text("Wg wartości") },
        )
        androidx.compose.material3.FilterChip(
            selected = sortField == NetworkHealthSortField.LAST_HEARD,
            onClick = { onSortFieldChange(NetworkHealthSortField.LAST_HEARD) },
            label = { Text("Wg aktywności") },
        )
        IconButton(onClick = onToggleDirection) {
            Text(if (sortAscending) "↑" else "↓", style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = onToggleFavoritesFirst) {
            Text(
                text = "★",
                style = MaterialTheme.typography.titleMedium,
                color =
                if (favoritesFirst) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(onClick = onToggleHideWithoutData) {
            Text(
                text = "⦸",
                style = MaterialTheme.typography.titleMedium,
                color =
                if (hideWithoutData) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun NodeHealthRow(
    node: NodeHealthInfo,
    metric: NetworkHealthMetric,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    if (node.isFavorite) {
                        Text(
                            text = "★ ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(text = node.longName.ifBlank { node.shortName }, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = if (node.isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                    if (node.isOnline) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            MetricSparkline(values = node.metricHistory, modifier = Modifier.size(width = 70.dp, height = 24.dp))
            Text(
                text = formatMetricValue(metric, node.valueFor(metric)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun formatMetricValue(metric: NetworkHealthMetric, value: Float?): String {
    if (value == null) return "—"
    return when (metric) {
        NetworkHealthMetric.POWER -> MetricFormatter.percent(value.toInt())
        NetworkHealthMetric.SIGNAL -> MetricFormatter.snr(value)
        NetworkHealthMetric.ETHER -> MetricFormatter.percent(value, decimalPlaces = 2)
        NetworkHealthMetric.ENVIRONMENT -> MetricFormatter.temperature(value, isFahrenheit = false)
        NetworkHealthMetric.TRAFFIC -> "${value.toInt()}"
        NetworkHealthMetric.NEIGHBORS -> "${value.toInt()}"
        NetworkHealthMetric.RESOURCES -> "${value.toInt()}%"
    }
}

/** Minimal line chart of recent values for the active metric; draws nothing when fewer than 2 points are available. */
@Composable
private fun MetricSparkline(values: List<Float>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - minV) / range) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 3f))
    }
}
