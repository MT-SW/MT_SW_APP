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
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.feature.node.metrics.withBatterySwap
import org.meshtastic.feature.node.metrics.withCurrentSign
import org.meshtastic.proto.PortNum

/** Time window options for the metric detail chart. */
enum class NodeMetricRange(val label: String, val millis: Long?) {
    HOUR("1 h", 60 * 60 * 1000L),
    DAY("24 h", 24 * 60 * 60 * 1000L),
    TWO_DAYS("2 dni", 2 * 24 * 60 * 60 * 1000L),
    WEEK("7 dni", 7 * 24 * 60 * 60 * 1000L),
    TWO_WEEKS("2 tyg.", 14L * 24 * 60 * 60 * 1000L),
    MONTH("30 dni", 30L * 24 * 60 * 60 * 1000L),
    ALL("Wszystko", null),
    ;

    /** Start of the window in epoch millis; 0 means "from the very beginning". */
    fun since(now: Long): Long = millis?.let { (now - it).coerceAtLeast(0L) } ?: 0L
}

/** More points than this make no visible difference on a phone screen, but make long ranges slow. */
private const val MAX_POINTS_PER_SERIES = 1200

/** Thins a long series by averaging neighbouring points; min / max / latest are taken from the full data first. */
private fun List<MetricPoint>.thinned(): List<MetricPoint> {
    if (size <= MAX_POINTS_PER_SERIES) return this
    val step = (size + MAX_POINTS_PER_SERIES - 1) / MAX_POINTS_PER_SERIES
    return chunked(step).map { chunk ->
        MetricPoint(chunk[chunk.size / 2].timestampMillis, chunk.sumOf { it.value.toDouble() }.toFloat() / chunk.size)
    }
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
    /** False until the first calculation has finished; the screen shows a loading indicator meanwhile. */
    val isLoaded: Boolean = false,
    /** True from the moment another range is picked until its numbers are ready; the chips stay usable. */
    val isRefreshing: Boolean = false,
)

@KoinViewModel
class NodeMetricDetailViewModel(
    @InjectedParam val nodeNum: Int,
    @InjectedParam private val initialMetric: NetworkHealthMetric,
    private val nodeRepository: NodeRepository,
    private val meshLogRepository: MeshLogRepository,
    private val uiPrefs: UiPrefs,
) : ViewModel() {

    private val rangeFlow = MutableStateFlow(NodeMetricRange.DAY)
    val range: StateFlow<NodeMetricRange> = rangeFlow.asStateFlow()

    private val metricFlow = MutableStateFlow(initialMetric)

    private val _uiState = MutableStateFlow(NodeMetricDetailUiState(metric = initialMetric))
    val uiState: StateFlow<NodeMetricDetailUiState> = _uiState.asStateFlow()

    init {
        combine(rangeFlow, metricFlow) { range, metric -> range to metric }
            .flatMapLatest { (range, metric) ->
                val since = range.since(nowMillis)
                // Only this node's own logs are read (never every node's telemetry): over 30 days the network-wide
                // query held far too much in memory and could crash the app.
                combine(
                    meshLogRepository.getLogsFromNodeSinceSlim(nodeNum, since).calmed(),
                    meshLogRepository.getMyNodeInfo().flatMapLatest { myNodeInfo ->
                        // MeshLog normalizes the locally-connected device's own logs to node number 0.
                        if (myNodeInfo?.my_node_num == nodeNum) {
                            meshLogRepository.getLogsFromNodeSinceSlim(0, since).calmed()
                        } else {
                            flowOf(emptyList())
                        }
                    },
                    meshLogRepository.getMyNodeInfo(),
                    combine(nodeRepository.getNodes(), uiPrefs.currentInverted, uiPrefs.batteryInverted, ::Triple),
                ) { nodeLogs, localNodeLogs, myNodeInfo, nodesAndInverted ->
                    val (nodes, invertedNodes, batteryInvertedNodes) = nodesAndInverted
                    val isViewingLocalNode = myNodeInfo?.my_node_num == nodeNum
                    val extraLocalLogs = if (isViewingLocalNode) localNodeLogs else emptyList()
                    val telemetryLogs =
                        (nodeLogs + extraLocalLogs).filter { it.portNum == PortNum.TELEMETRY_APP.value }
                    val telemetryHistory =
                        telemetryLogs
                            .filter { it.fromNum == nodeNum || (isViewingLocalNode && it.fromNum == 0) }
                            .mapNotNull {
                                it.toHistoryEntry()
                                    ?.withCurrentSign(nodeNum in invertedNodes)
                                    ?.withBatterySwap(nodeNum in batteryInvertedNodes)
                            }
                    val signalHistory =
                        (nodeLogs + extraLocalLogs).distinctBy { it.uuid }.mapNotNull { it.toSignalHistoryEntry() }
                    val history = signalHistory + telemetryHistory
                    val node = nodes.firstOrNull { it.num == nodeNum }
                    val sortedHistory = history.sortedBy { it.timestamp }

                    val series =
                        metric.seriesLabels().map { seriesLabel ->
                            val points =
                                sortedHistory.mapNotNull { entry ->
                                    // A NaN / infinite reading would poison the chart's min, max and path maths.
                                    metric.historySeriesValue(seriesLabel, entry)?.takeIf { it.isFinite() }?.let {
                                        MetricPoint(entry.timestamp, it)
                                    }
                                }
                            MetricSeries(
                                label = seriesLabel,
                                points = points.thinned(),
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
                                    points = hopPoints.thinned(),
                                    minValue = hopPoints.minOfOrNull { it.value },
                                    maxValue = hopPoints.maxOfOrNull { it.value },
                                    latestValue = hopPoints.lastOrNull()?.value,
                                )
                            val noiseFloorSeries = series.find { it.label == "Noise Floor" && it.points.isNotEmpty() }
                            // The WiFi series are the device's own report too, so they stay as well.
                            val wifiSeries =
                                series.filter {
                                    (it.label == "WiFi RSSI" || it.label == "Zerwania WiFi") && it.points.isNotEmpty()
                                }
                            listOfNotNull(hopsSeries, noiseFloorSeries) + wifiSeries
                        } else {
                            series
                        }

                    NodeMetricDetailUiState(
                        nodeName = node?.user?.long_name?.ifBlank { node.user.short_name } ?: "",
                        metric = metric,
                        range = range,
                        series = finalSeries,
                        isLoaded = true,
                    )
                }
            }
            .flowOn(Dispatchers.Default)
            .catch { Logger.e(it) { "Metric detail failed" } }
            // A result for a range that is no longer selected must not end the loading of the newly picked one.
            .onEach { if (it.range == rangeFlow.value) _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun setRange(range: NodeMetricRange) {
        if (rangeFlow.value == range) return
        _uiState.update { it.copy(range = range, isRefreshing = true) }
        rangeFlow.value = range
    }
}
