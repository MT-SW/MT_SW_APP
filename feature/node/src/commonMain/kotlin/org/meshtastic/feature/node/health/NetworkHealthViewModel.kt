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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.util.decodeLocalStatsExtended
import org.meshtastic.core.model.util.isDirectSignal
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.NeighborInfo
import org.meshtastic.proto.PortNum

/** One tab on the Network Health screen; each tab owns one metric's value + history. */
enum class NetworkHealthMetric(val label: String) {
    POWER("Zasilanie"),
    SIGNAL("Sygnał"),
    ETHER("Eter"),
    ENVIRONMENT("Środowisko"),
    RESOURCES("Zasoby"),
    TRAFFIC("Ruch"),
    NEIGHBORS("Sąsiedzi"),
}

/** What determines row order, independent of which metric tab is active. */
enum class NetworkHealthSortField {
    METRIC_VALUE,
    LAST_HEARD,
}

private data class SortSettings(
    val sortField: NetworkHealthSortField,
    val sortAscending: Boolean,
    val favoritesFirst: Boolean,
    val hideWithoutData: Boolean,
    val searchText: String,
)

/** A single row's worth of health data, derived from [Node] plus accumulated history. */
data class NodeHealthInfo(
    val num: Int,
    val shortName: String,
    val longName: String,
    val lastHeard: Int,
    val isOnline: Boolean,
    val isFavorite: Boolean,
    val batteryLevel: Int?,
    val isPowered: Boolean,
    val snr: Float?,
    val rssi: Int?,
    val channelUtilization: Float?,
    val airUtilTx: Float?,
    val temperature: Float?,
    val relativeHumidity: Float?,
    val barometricPressure: Float?,
    val metricHistory: List<Float> = emptyList(),
    val neighborCount: Int? = null,
) {
    /** Returns this row's value for [metric], or null if that metric has never been reported. */
    fun valueFor(metric: NetworkHealthMetric): Float? = when (metric) {
        NetworkHealthMetric.POWER -> batteryLevel?.toFloat()

        // Signal tab sorts/displays by SNR; RSSI and noise floor are extra series in the detail chart.
        NetworkHealthMetric.SIGNAL -> snr

        // Ether tab sorts/displays by channel utilization; air util TX is an extra series in the detail chart.
        NetworkHealthMetric.ETHER -> channelUtilization

        // / Environment tab sorts/displays by temperature; humidity and pressure are extra series in the detail
        // chart.
        NetworkHealthMetric.ENVIRONMENT -> temperature

        // Traffic tab sorts/displays by packets received; the rest are extra series in the detail chart.
        NetworkHealthMetric.TRAFFIC -> null

        NetworkHealthMetric.NEIGHBORS -> neighborCount?.toFloat()

        // Resources tab (fw+ local_stats_extended) isn't part of the live Node model — like Traffic, its sparkline
        // and detail chart are driven entirely by history, not a "current" column value.
        NetworkHealthMetric.RESOURCES -> null
    }
}

data class NetworkHealthUiState(
    val nodes: List<NodeHealthInfo> = emptyList(),
    val activeMetric: NetworkHealthMetric = NetworkHealthMetric.POWER,
    val sortField: NetworkHealthSortField = NetworkHealthSortField.LAST_HEARD,
    val sortAscending: Boolean = false,
    val favoritesFirst: Boolean = true,
    val hideWithoutData: Boolean = false,
    val searchText: String = "",
)

@KoinViewModel
class NetworkHealthViewModel(
    private val nodeRepository: NodeRepository,
    private val meshLogRepository: MeshLogRepository,
) : ViewModel() {

    private val _activeMetric = MutableStateFlow(NetworkHealthMetric.POWER)
    val activeMetric: StateFlow<NetworkHealthMetric> = _activeMetric.asStateFlow()

    private val sortFieldFlow = MutableStateFlow(NetworkHealthSortField.LAST_HEARD)
    private val sortAscendingFlow = MutableStateFlow(false)
    private val favoritesFirstFlow = MutableStateFlow(true)
    private val hideWithoutDataFlow = MutableStateFlow(false)
    private val searchTextFlow = MutableStateFlow("")

    private val historySince: Long
        get() = nowMillis - HISTORY_WINDOW_MILLIS

    val uiState: StateFlow<NetworkHealthUiState> =
        combine(
            nodeRepository.getNodes(),
            combine(
                meshLogRepository.getAllLogsSince(historySince),
                meshLogRepository.getLogsByPortNumSince(PortNum.TELEMETRY_APP.value, historySince),
                meshLogRepository.getMyNodeInfo(),
            ) { allLogs, telemetryLogs, myNodeInfo ->
                // MeshLog normalizes the locally-connected device's own logs to node number 0 (so log continuity
                // survives a hardware swap) — remap those back to whichever node is actually connected right now.
                val myNodeNum = myNodeInfo?.my_node_num
                val remap:
                    (
                        org.meshtastic.core.model.NodeMetricsHistoryEntry,
                    ) -> org.meshtastic.core.model.NodeMetricsHistoryEntry =
                    { entry ->
                        if (myNodeNum != null && entry.num == 0) entry.copy(num = myNodeNum) else entry
                    }
                allLogs.mapNotNull { it.toSignalHistoryEntry()?.let(remap) } +
                    telemetryLogs.mapNotNull { it.toHistoryEntry()?.let(remap) }
            },
            meshLogRepository.getLogsByPortNum(PortNum.NEIGHBORINFO_APP.value, NEIGHBOR_LOG_LIMIT),
            _activeMetric,
            combine(
                sortFieldFlow,
                sortAscendingFlow,
                favoritesFirstFlow,
                hideWithoutDataFlow,
                searchTextFlow,
                ::SortSettings,
            ),
        ) { nodes, history, neighborLogs, metric, sortSettings ->
            val neighborCountByNode =
                neighborLogs
                    .distinctBy { it.fromNum }
                    .mapNotNull { log ->
                        val payload = log.fromRadio.packet?.decoded?.payload ?: return@mapNotNull null
                        val info =
                            runCatching { NeighborInfo.ADAPTER.decode(payload) }.getOrNull()
                                ?: return@mapNotNull null
                        log.fromNum to info.neighbors.size
                    }
                    .toMap()
            val (sortField, sortAscending, favoritesFirst, hideWithoutData, searchText) = sortSettings
            val historyByNode = history.groupBy { it.num }

            val healthInfos =
                nodes.map { node ->
                    val nodeHistory = historyByNode[node.num].orEmpty()
                    NodeHealthInfo(
                        num = node.num,
                        shortName = node.user.short_name,
                        longName = node.user.long_name,
                        lastHeard = node.lastHeard,
                        isOnline = node.isOnline,
                        isFavorite = node.isFavorite,
                        batteryLevel = node.deviceMetrics.battery_level,
                        isPowered =
                        node.deviceMetrics.voltage != null &&
                            (node.deviceMetrics.battery_level ?: 0) > BATTERY_POWERED_THRESHOLD,
                        snr = node.snr.takeIf { it != Float.MAX_VALUE },
                        rssi = node.rssi.takeIf { it != Int.MAX_VALUE },
                        channelUtilization = node.deviceMetrics.channel_utilization,
                        airUtilTx = node.deviceMetrics.air_util_tx,
                        temperature = node.environmentMetrics.temperature,
                        relativeHumidity = node.environmentMetrics.relative_humidity,
                        barometricPressure = node.environmentMetrics.barometric_pressure,
                        metricHistory =
                        nodeHistory.sortedBy { it.timestamp }.mapNotNull { metric.historyValue(it) },
                        neighborCount = neighborCountByNode[node.num],
                    )
                }

            val withoutDataFiltered =
                if (hideWithoutData) {
                    healthInfos.filter { it.valueFor(metric) != null || it.metricHistory.isNotEmpty() }
                } else {
                    healthInfos
                }
            val filtered =
                if (searchText.isBlank()) {
                    withoutDataFiltered
                } else {
                    withoutDataFiltered.filter {
                        it.longName.contains(searchText, ignoreCase = true) ||
                            it.shortName.contains(searchText, ignoreCase = true)
                    }
                }

            val baseSorted =
                when (sortField) {
                    NetworkHealthSortField.LAST_HEARD -> filtered.sortedBy { it.lastHeard }

                    NetworkHealthSortField.METRIC_VALUE ->
                        filtered.sortedWith(compareBy(nullsLast()) { it.valueFor(metric) })
                }
            val directed = if (sortAscending) baseSorted else baseSorted.reversed()
            val sorted = if (favoritesFirst) directed.sortedByDescending { it.isFavorite } else directed

            NetworkHealthUiState(
                nodes = sorted,
                activeMetric = metric,
                sortField = sortField,
                sortAscending = sortAscending,
                favoritesFirst = favoritesFirst,
                hideWithoutData = hideWithoutData,
                searchText = searchText,
            )
        }
            .stateInWhileSubscribed(initialValue = NetworkHealthUiState())

    fun setActiveMetric(metric: NetworkHealthMetric) {
        _activeMetric.value = metric
    }

    fun setSortField(field: NetworkHealthSortField) {
        sortFieldFlow.value = field
    }

    fun toggleSortDirection() {
        sortAscendingFlow.value = !sortAscendingFlow.value
    }

    fun toggleFavoritesFirst() {
        favoritesFirstFlow.value = !favoritesFirstFlow.value
    }

    fun toggleHideWithoutData() {
        hideWithoutDataFlow.value = !hideWithoutDataFlow.value
    }

    fun setSearchText(text: String) {
        searchTextFlow.value = text
    }

    companion object {
        private const val BATTERY_POWERED_THRESHOLD = 100
        private const val HISTORY_WINDOW_MILLIS = 24 * 60 * 60 * 1000L
        private const val NEIGHBOR_LOG_LIMIT = 2000
    }
}

/**
 * Decodes a MeshLog's raw telemetry packet into the same shape as our own history entries, so both sources can be
 * combined and processed identically downstream. Uses the log's own received_date (phone clock at arrival time, already
 * reliable) rather than the packet's self-reported timestamp. SNR/RSSI are intentionally left null here — those come
 * from the separate, denser per-packet recorder in MeshDataHandlerImpl, not from telemetry logs.
 */
internal fun org.meshtastic.core.model.MeshLog.toHistoryEntry(): org.meshtastic.core.model.NodeMetricsHistoryEntry? {
    val payload = fromRadio.packet?.decoded?.payload ?: return null
    val telemetry = runCatching { org.meshtastic.proto.Telemetry.ADAPTER.decode(payload) }.getOrNull() ?: return null
    val voltage = telemetry.power_metrics?.ch3_voltage ?: telemetry.environment_metrics?.voltage
    val current = telemetry.power_metrics?.ch3_current ?: telemetry.environment_metrics?.current
    val localStatsExtended = telemetry.unknownFields.decodeLocalStatsExtended()
    val heapTotal = telemetry.local_stats?.heap_total_bytes ?: 0
    val heapFree = telemetry.local_stats?.heap_free_bytes ?: 0
    return org.meshtastic.core.model.NodeMetricsHistoryEntry(
        num = fromNum,
        timestamp = received_date,
        batteryLevel = telemetry.device_metrics?.battery_level,
        voltage = voltage,
        current = current,
        channelUtilization = telemetry.device_metrics?.channel_utilization,
        airUtilTx = telemetry.device_metrics?.air_util_tx,
        temperature = telemetry.environment_metrics?.temperature,
        relativeHumidity = telemetry.environment_metrics?.relative_humidity,
        barometricPressure = telemetry.environment_metrics?.barometric_pressure,
        noiseFloor = telemetry.local_stats?.noise_floor?.takeUnless { it == 0 },
        packetsTx = telemetry.local_stats?.num_packets_tx,
        packetsRx = telemetry.local_stats?.num_packets_rx,
        rxDupe = telemetry.local_stats?.num_rx_dupe,
        txRelay = telemetry.local_stats?.num_tx_relay,
        packetsRxBad = telemetry.local_stats?.num_packets_rx_bad,
        uptimeSeconds = telemetry.local_stats?.uptime_seconds,
        cpuUsagePercent = localStatsExtended?.cpuUsagePercent,
        heapFreePercent = if (heapTotal > 0) heapFree.toFloat() / heapTotal * PERCENT_MULTIPLIER else null,
        flashUsedPercent =
            localStatsExtended
                ?.takeIf { it.flashTotalBytes > 0 }
                ?.let { it.flashUsedBytes.toFloat() / it.flashTotalBytes * PERCENT_MULTIPLIER },
        psramFreePercent =
            localStatsExtended
                ?.takeIf { it.memoryPsramTotal > 0 }
                ?.let { it.memoryPsramFree.toFloat() / it.memoryPsramTotal * PERCENT_MULTIPLIER },
    )
}

private const val PERCENT_MULTIPLIER = 100f

/** Extracts this metric's value from a raw history entry, for sparkline plotting. */

/**
 * Decodes SNR/RSSI directly from a MeshLog's raw packet header (any port — this data lives in every packet, not just
 * telemetry), filtered to direct-signal-only packets so a relay's link quality never gets misattributed to the packet's
 * origin node. See [MeshPacket.isDirectSignal].
 */
internal fun org.meshtastic.core.model.MeshLog.toSignalHistoryEntry():
    org.meshtastic.core.model.NodeMetricsHistoryEntry? {
    val packet = meshPacket ?: return null
    if (!packet.isDirectSignal()) return null
    val snr = packet.rx_snr.takeUnless { it.isNaN() }
    val rssi = packet.rx_rssi
    if (snr == null && rssi == null) return null
    return org.meshtastic.core.model.NodeMetricsHistoryEntry(
        num = fromNum,
        timestamp = received_date,
        snr = snr,
        rssi = rssi,
    )
}

/** Extracts this metric's value from a raw history entry, for sparkline plotting. */
internal fun NetworkHealthMetric.historyValue(entry: org.meshtastic.core.model.NodeMetricsHistoryEntry): Float? =
    when (this) {
        NetworkHealthMetric.POWER -> entry.batteryLevel?.toFloat()

        NetworkHealthMetric.SIGNAL -> entry.snr

        NetworkHealthMetric.ETHER -> entry.channelUtilization

        NetworkHealthMetric.ENVIRONMENT -> entry.temperature

        NetworkHealthMetric.TRAFFIC -> entry.packetsRx?.toFloat()

        // Neighbor counts aren't tracked in this table; there's no sparkline/history for this tab.
        NetworkHealthMetric.NEIGHBORS -> null

        NetworkHealthMetric.RESOURCES -> entry.cpuUsagePercent?.toFloat()
    }

/** Sub-series labels for a metric tab's detail chart. Most metrics have one; several tabs have multiple. */
internal fun NetworkHealthMetric.seriesLabels(): List<String> = when (this) {
    NetworkHealthMetric.POWER -> listOf("Bateria", "Napięcie", "Prąd")
    NetworkHealthMetric.SIGNAL -> listOf("SNR", "RSSI", "Noise Floor")
    NetworkHealthMetric.ETHER -> listOf("Ch. Util", "Air Util")
    NetworkHealthMetric.ENVIRONMENT -> listOf("Temperatura", "Wilgotność", "Ciśnienie")
    NetworkHealthMetric.TRAFFIC -> listOf("TX", "RX", "Duplikaty", "Przekazane", "Uszkodzone")
    NetworkHealthMetric.RESOURCES -> listOf("CPU", "Heap", "Flash", "PSRAM")
    else -> listOf(label)
}

/**
 * Groups of series (by label) that get plotted together on one chart with a real value axis — at most 2 per group,
 * since Vico's dual-axis (start/end) approach only stays accurate up to 2 series sharing a chart. Grouped by similar
 * magnitude/units so both axes stay readable (e.g. SNR+RSSI are both dB-ish; pressure is ~1000 hPa and would dwarf
 * temperature/humidity, so it gets its own chart).
 */
internal fun NetworkHealthMetric.seriesGroups(): List<List<String>> = when (this) {
    NetworkHealthMetric.POWER -> listOf(listOf("Napięcie", "Prąd"), listOf("Bateria"))
    NetworkHealthMetric.SIGNAL -> listOf(listOf("SNR"), listOf("RSSI", "Noise Floor"))
    NetworkHealthMetric.ETHER -> listOf(listOf("Ch. Util", "Air Util"))
    NetworkHealthMetric.ENVIRONMENT -> listOf(listOf("Temperatura", "Wilgotność"), listOf("Ciśnienie"))
    NetworkHealthMetric.TRAFFIC -> listOf(listOf("TX", "RX"), listOf("Duplikaty", "Przekazane", "Uszkodzone"))
    NetworkHealthMetric.RESOURCES -> listOf(listOf("CPU", "Heap"), listOf("Flash", "PSRAM"))
    else -> listOf(listOf(label))
}

/** Extracts the value for one named sub-series (from [seriesLabels]) out of a raw history entry. */
internal fun NetworkHealthMetric.historySeriesValue(
    seriesLabel: String,
    entry: org.meshtastic.core.model.NodeMetricsHistoryEntry,
): Float? = when (this) {
    NetworkHealthMetric.POWER ->
        when (seriesLabel) {
            "Bateria" -> entry.batteryLevel?.toFloat()
            "Napięcie" -> entry.voltage
            "Prąd" -> entry.current
            else -> null
        }

    NetworkHealthMetric.SIGNAL ->
        when (seriesLabel) {
            "SNR" -> entry.snr
            "RSSI" -> entry.rssi?.toFloat()
            "Noise Floor" -> entry.noiseFloor?.toFloat()
            else -> null
        }

    NetworkHealthMetric.ETHER ->
        when (seriesLabel) {
            "Ch. Util" -> entry.channelUtilization
            "Air Util" -> entry.airUtilTx
            else -> null
        }

    NetworkHealthMetric.ENVIRONMENT ->
        when (seriesLabel) {
            "Temperatura" -> entry.temperature
            "Wilgotność" -> entry.relativeHumidity
            "Ciśnienie" -> entry.barometricPressure
            else -> null
        }

    NetworkHealthMetric.TRAFFIC ->
        when (seriesLabel) {
            "TX" -> entry.packetsTx?.toFloat()
            "RX" -> entry.packetsRx?.toFloat()
            "Duplikaty" -> entry.rxDupe?.toFloat()
            "Przekazane" -> entry.txRelay?.toFloat()
            "Uszkodzone" -> entry.packetsRxBad?.toFloat()
            else -> null
        }

    NetworkHealthMetric.RESOURCES ->
        when (seriesLabel) {
            "CPU" -> entry.cpuUsagePercent?.toFloat()
            "Heap" -> entry.heapFreePercent
            "Flash" -> entry.flashUsedPercent
            "PSRAM" -> entry.psramFreePercent
            else -> null
        }

    else -> historyValue(entry)
}
