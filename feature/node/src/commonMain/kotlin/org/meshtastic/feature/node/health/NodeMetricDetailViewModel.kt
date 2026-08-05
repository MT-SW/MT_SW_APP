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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.proto.PortNum

/** Time window options for the metric detail chart. */
enum class NodeMetricRange(val label: String, val millis: Long) {
    DAY("24h", 24 * 60 * 60 * 1000L),
    WEEK("7 dni", 7 * 24 * 60 * 60 * 1000L),
    MONTH("30 dni", 30L * 24 * 60 * 60 * 1000L),
}

/** A single plotted point: milliseconds-since-epoch timestamp paired with the metric's value at that time. */
data class MetricPoint(val timestampMillis: Long, val value: Float)

/** One plotted series (e.g. "Bateria", "Napięcie") with its own points and summary stats. */
data class MetricSeries(
    val label: String,
    val points: List<MetricPoint>,
    val minValue: Float? = null,
    val maxValue: Float? = null,
    val latestValue: Float? = null,
)

data class NodeMetricDetailUiState(
    val nodeName: String = "",
    val metric: NetworkHealthMetric = NetworkHealthMetric.POWER,
    val range: NodeMetricRange = NodeMetricRange.DAY,
    val series: List<MetricSeries> = emptyList(),
)

@KoinViewModel
class NodeMetricDetailViewModel(
    private val nodeNum: Int,
    private val initialMetric: NetworkHealthMetric,
    private val nodeRepository: NodeRepository,
    private val meshLogRepository: MeshLogRepository,
) : ViewModel() {

    private val rangeFlow = MutableStateFlow(NodeMetricRange.DAY)
    val range: StateFlow<NodeMetricRange> = rangeFlow.asStateFlow()

    private val metricFlow = MutableStateFlow(initialMetric)

    private val _uiState = MutableStateFlow(NodeMetricDetailUiState(metric = initialMetric))
    val uiState: StateFlow<NodeMetricDetailUiState> = _uiState.asStateFlow()

    init {
        combine(rangeFlow, metricFlow) { range, metric -> range to metric }
            .flatMapLatest { (range, metric) ->
                val since = nowMillis - range.millis
                combine(
                    meshLogRepository.getLogsByPortNumSince(PortNum.TELEMETRY_APP.value, since),
                    meshLogRepository.getLogsFromNodeSince(nodeNum, since),
                    meshLogRepository.getLogsFromNodeSince(0, since),
                    meshLogRepository.getMyNodeInfo(),
                    nodeRepository.getNodes(),
                ) { telemetryLogs, nodeLogs, localNodeLogs, myNodeInfo, nodes ->
                    // MeshLog normalizes the locally-connected device's own logs to node number 0 — if this screen
                    // is showing details for whichever node is currently connected, also pull the 0-numbered logs.
                    val isViewingLocalNode = myNodeInfo?.my_node_num == nodeNum
                    val extraLocalLogs = if (isViewingLocalNode) localNodeLogs else emptyList()
                    val telemetryHistory =
                        telemetryLogs
                            .filter { it.fromNum == nodeNum || (isViewingLocalNode && it.fromNum == 0) }
                            .mapNotNull { it.toHistoryEntry() }
                    val signalHistory =
                        (nodeLogs + extraLocalLogs).distinctBy { it.uuid }.mapNotNull { it.toSignalHistoryEntry() }
                    val history = signalHistory + telemetryHistory
                    val node = nodes.firstOrNull { it.num == nodeNum }
                    val sortedHistory = history.sortedBy { it.timestamp }

                    val series =
                        metric.seriesLabels().map { seriesLabel ->
                            val points =
                                sortedHistory.mapNotNull { entry ->
                                    metric.historySeriesValue(seriesLabel, entry)?.let {
                                        MetricPoint(entry.timestamp, it)
                                    }
                                }
                            MetricSeries(
                                label = seriesLabel,
                                points = points,
                                minValue = points.minOfOrNull { it.value },
                                maxValue = points.maxOfOrNull { it.value },
                                latestValue = points.lastOrNull()?.value,
                            )
                        }

                    // No direct-signal SNR/RSSI anywhere in this window (node only ever heard via relay) — swap
                    // just those two for a hop-count-over-time series. Noise Floor is a self-reported telemetry
                    // stat, not a radio measurement of directness, so it's kept whenever the node reports it,
                    // independent of whether SNR/RSSI have direct data.
                    val hasDirectSignal =
                        series.any { (it.label == "SNR" || it.label == "RSSI") && it.points.isNotEmpty() }
                    val finalSeries =
                        if (metric == NetworkHealthMetric.SIGNAL && !hasDirectSignal) {
                            val hopPoints =
                                nodeLogs.mapNotNull { log ->
                                    val packet = log.meshPacket ?: return@mapNotNull null
                                    if (packet.hop_start == 0 || packet.hop_limit > packet.hop_start) {
                                        return@mapNotNull null
                                    }
                                    MetricPoint(log.received_date, (packet.hop_start - packet.hop_limit).toFloat())
                                }
                            val hopsSeries =
                                MetricSeries(
                                    label = "Przeskoki",
                                    points = hopPoints,
                                    minValue = hopPoints.minOfOrNull { it.value },
                                    maxValue = hopPoints.maxOfOrNull { it.value },
                                    latestValue = hopPoints.lastOrNull()?.value,
                                )
                            val noiseFloorSeries = series.find { it.label == "Noise Floor" && it.points.isNotEmpty() }
                            listOfNotNull(hopsSeries, noiseFloorSeries)
                        } else {
                            series
                        }

                    NodeMetricDetailUiState(
                        nodeName = node?.user?.long_name?.ifBlank { node.user.short_name } ?: "",
                        metric = metric,
                        range = range,
                        series = finalSeries,
                    )
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun setRange(range: NodeMetricRange) {
        rangeFlow.value = range
    }
}
