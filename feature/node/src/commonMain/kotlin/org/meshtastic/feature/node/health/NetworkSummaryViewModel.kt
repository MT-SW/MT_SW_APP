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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.NeighborInfo
import org.meshtastic.proto.PortNum

data class AttentionItem(val nodeName: String, val reason: String)

data class UptimeLeader(val nodeName: String, val uptimeSeconds: Int)

data class QuietNode(val nodeName: String, val hoursSilent: Long)

data class PositionSender(val nodeName: String, val positionCount: Int)

data class SignalLeader(val nodeName: String, val snr: Float)

data class ClosestNode(val nodeName: String, val distanceMeters: Int)

data class DataVolumeLeader(val nodeName: String, val packetCount: Int)

data class ChannelMessageCount(val channelIndex: Int, val weekCount: Int, val todayCount: Int)

data class MessageSender(val nodeName: String, val count: Int)

data class NetworkSummaryUiState(
    val onlineCount: Int = 0,
    val totalCount: Int = 0,
    val quietestNodes: List<QuietNode> = emptyList(),
    val averageBatteryLevel: Int? = null,
    val lowBatteryCount: Int = 0,
    val mainsPoweredCount: Int = 0,
    val topSignalNodes: List<SignalLeader> = emptyList(),
    val weakSignalCount: Int = 0,
    val averageNoiseFloor: Int? = null,
    val averageChannelUtilization: Float? = null,
    val maxChannelUtilization: Float? = null,
    val averageAirUtilTx: Float? = null,
    val totalPacketsTx: Int = 0,
    val totalPacketsRx: Int = 0,
    val totalDupes: Int = 0,
    val totalBad: Int = 0,
    val topRelayNodeName: String? = null,
    val topRelayCount: Int = 0,
    val neighborReportingCount: Int = 0,
    val averageNeighborCount: Double? = null,
    val uptimeLeaders: List<UptimeLeader> = emptyList(),
    val topPositionSenders: List<PositionSender> = emptyList(),
    val closestNodes: List<ClosestNode> = emptyList(),
    val topTelemetryVolumeSenders: List<DataVolumeLeader> = emptyList(),
    val topAllPacketVolumeSenders: List<DataVolumeLeader> = emptyList(),
    val channelMessageCounts: List<ChannelMessageCount> = emptyList(),
    val totalMessagesWeek: Int = 0,
    val totalMessagesToday: Int = 0,
    val topMessageSendersWeek: List<MessageSender> = emptyList(),
    val topMessageSendersToday: List<MessageSender> = emptyList(),
    val attentionItems: List<AttentionItem> = emptyList(),
)

@KoinViewModel
class NetworkSummaryViewModel(
    private val nodeRepository: NodeRepository,
    private val meshLogRepository: MeshLogRepository,
) : ViewModel() {

    val uiState: StateFlow<NetworkSummaryUiState> =
        combine(
            nodeRepository.getNodes(),
            meshLogRepository.getLogsByPortNumSince(PortNum.TELEMETRY_APP.value, nowMillis - HISTORY_WINDOW_MILLIS),
            meshLogRepository.getLogsByPortNumSince(
                PortNum.NEIGHBORINFO_APP.value,
                nowMillis - HISTORY_WINDOW_MILLIS,
            ),
            meshLogRepository.getLogsByPortNumSince(PortNum.POSITION_APP.value, nowMillis - HISTORY_WINDOW_MILLIS),
            meshLogRepository.getAllLogsSince(nowMillis - HISTORY_WINDOW_MILLIS),
            meshLogRepository.getLogsByPortNumSince(PortNum.TEXT_MESSAGE_APP.value, nowMillis - WEEK_WINDOW_MILLIS),
            meshLogRepository.getMyNodeInfo(),
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val nodes = values[0] as List<org.meshtastic.core.model.Node>

            @Suppress("UNCHECKED_CAST")
            val telemetryLogs = values[1] as List<org.meshtastic.core.model.MeshLog>

            @Suppress("UNCHECKED_CAST")
            val neighborLogs = values[2] as List<org.meshtastic.core.model.MeshLog>

            @Suppress("UNCHECKED_CAST")
            val positionLogs = values[3] as List<org.meshtastic.core.model.MeshLog>

            @Suppress("UNCHECKED_CAST")
            val allLogs = values[4] as List<org.meshtastic.core.model.MeshLog>

            @Suppress("UNCHECKED_CAST")
            val messageLogsWeek = values[5] as List<org.meshtastic.core.model.MeshLog>
            val myNodeInfo = values[6] as org.meshtastic.proto.MyNodeInfo?

            val myNodeNum = myNodeInfo?.my_node_num
            fun remapLocal(num: Int): Int = if (myNodeNum != null && num == 0) myNodeNum else num

            val nameByNum = nodes.associate { it.num to it.user.long_name.ifBlank { it.user.short_name } }
            val history =
                telemetryLogs.mapNotNull {
                    it.toHistoryEntry()?.let { entry -> entry.copy(num = remapLocal(entry.num)) }
                }
            val historyByNode = history.groupBy { it.num }
            val latestPerNode = historyByNode.mapValues { (_, entries) -> entries.maxByOrNull { it.timestamp } }

            val batteryLevels = nodes.mapNotNull { it.deviceMetrics.battery_level }
            val lowBatteryNodes = nodes.filter { (it.deviceMetrics.battery_level ?: 100) < LOW_BATTERY_THRESHOLD }
            val mainsPowered = nodes.count { (it.deviceMetrics.battery_level ?: 0) > MAINS_POWERED_THRESHOLD }

            val snrByNode = nodes.filter { it.snr != Float.MAX_VALUE }
            val topSignalNodes =
                snrByNode
                    .sortedByDescending { it.snr }
                    .take(TOP_LIST_COUNT)
                    .map { SignalLeader(nameByNum[it.num] ?: "?", it.snr) }
            val weakSignalNodes = snrByNode.filter { it.snr < WEAK_SNR_THRESHOLD }

            val noiseFloors = latestPerNode.values.mapNotNull { it?.noiseFloor }
            val channelUtils = latestPerNode.values.mapNotNull { it?.channelUtilization }
            val airUtils = latestPerNode.values.mapNotNull { it?.airUtilTx }

            val totalTx = latestPerNode.values.sumOf { it?.packetsTx ?: 0 }
            val totalRx = latestPerNode.values.sumOf { it?.packetsRx ?: 0 }
            val totalDupes = latestPerNode.values.sumOf { it?.rxDupe ?: 0 }
            val totalBad = latestPerNode.values.sumOf { it?.packetsRxBad ?: 0 }
            val topRelay =
                latestPerNode.entries.filter { (it.value?.txRelay ?: 0) > 0 }.maxByOrNull { it.value?.txRelay ?: 0 }

            val neighborCounts =
                neighborLogs
                    .distinctBy { it.fromNum }
                    .mapNotNull { log ->
                        val payload = log.fromRadio.packet?.decoded?.payload ?: return@mapNotNull null
                        val info =
                            runCatching { NeighborInfo.ADAPTER.decode(payload) }.getOrNull()
                                ?: return@mapNotNull null
                        info.neighbors.size
                    }

            val uptimeLeaders =
                historyByNode
                    .mapNotNull { (num, entries) ->
                        // Pick the most recent entry that actually reports uptime (only local_stats packets do),
                        // not just this node's overall-latest telemetry packet — that could be a device_metrics or
                        // environment_metrics packet with no uptime field, wrongly excluding a long-running node.
                        entries
                            .filter { it.uptimeSeconds != null }
                            .maxByOrNull { it.timestamp }
                            ?.let { latest -> num to latest.uptimeSeconds!! }
                    }
                    .sortedByDescending { it.second }
                    .take(TOP_LIST_COUNT)
                    .map { (num, uptime) -> UptimeLeader(nameByNum[num] ?: "?", uptime) }

            val quietestNodes =
                nodes
                    .filter { it.lastHeard != 0 }
                    .sortedBy { it.lastHeard }
                    .take(TOP_LIST_COUNT)
                    .map { node ->
                        val hours = ((nowMillis / MILLIS_PER_SEC) - node.lastHeard) / SECONDS_PER_HOUR
                        QuietNode(nameByNum[node.num] ?: "?", hours)
                    }

            val topPositionSenders =
                positionLogs
                    .map { remapLocal(it.fromNum) }
                    .filter { myNodeNum == null || it != myNodeNum }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(TOP_LIST_COUNT)
                    .map { (num, count) -> PositionSender(nameByNum[num] ?: "?", count) }

            val myNode = nodes.firstOrNull { it.num == myNodeNum }
            val closestNodes =
                myNode
                    ?.let { me ->
                        nodes
                            .filter { it.num != me.num }
                            .mapNotNull { other -> me.distance(other)?.let { other to it } }
                            .sortedBy { it.second }
                            .take(TOP_LIST_COUNT)
                            .map { (node, distance) -> ClosestNode(nameByNum[node.num] ?: "?", distance) }
                    }
                    .orEmpty()

            // "Own data" leaderboards: telemetry-only volume, and every packet type combined.
            val topTelemetryVolumeSenders =
                history
                    .filter { myNodeNum == null || it.num != myNodeNum }
                    .groupingBy { it.num }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(TOP_LIST_COUNT)
                    .map { (num, count) -> DataVolumeLeader(nameByNum[num] ?: "?", count) }
            val topAllPacketVolumeSenders =
                allLogs
                    .map { remapLocal(it.fromNum) }
                    // The locally-connected device logs a lot of internal phone↔radio traffic (link-status
                    // heartbeats, etc.) that isn't comparable to real mesh transmission volume from other nodes
                    // — excluded entirely from this ranking rather than trying to filter out every such port type.
                    .filter { myNodeNum == null || it != myNodeNum }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(TOP_LIST_COUNT)
                    .map { (num, count) -> DataVolumeLeader(nameByNum[num] ?: "?", count) }

            // Messages: "today" is the last 24h, a subset of the already-fetched 7-day window.
            val todayCutoff = nowMillis - HISTORY_WINDOW_MILLIS
            val messageLogsToday = messageLogsWeek.filter { it.received_date >= todayCutoff }

            val channelMessageCounts =
                messageLogsWeek
                    .mapNotNull { it.meshPacket?.channel }
                    .groupingBy { it }
                    .eachCount()
                    .keys
                    .sorted()
                    .map { channelIndex ->
                        val week = messageLogsWeek.count { it.meshPacket?.channel == channelIndex }
                        val today = messageLogsToday.count { it.meshPacket?.channel == channelIndex }
                        ChannelMessageCount(channelIndex, week, today)
                    }

            val topMessageSendersWeek =
                messageLogsWeek
                    .map { remapLocal(it.fromNum) }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(TOP_LIST_COUNT)
                    .map { (num, count) -> MessageSender(nameByNum[num] ?: "?", count) }
            val topMessageSendersToday =
                messageLogsToday
                    .map { remapLocal(it.fromNum) }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(TOP_LIST_COUNT)
                    .map { (num, count) -> MessageSender(nameByNum[num] ?: "?", count) }

            val attentionItems = buildList {
                lowBatteryNodes.forEach {
                    add(AttentionItem(nameByNum[it.num] ?: "?", "Bateria ${it.deviceMetrics.battery_level}%"))
                }
                weakSignalNodes.forEach {
                    add(AttentionItem(nameByNum[it.num] ?: "?", "Słaby sygnał: ${MetricFormatter.snr(it.snr)}"))
                }
                latestPerNode.entries
                    .filter { (it.value?.channelUtilization ?: 0f) > HIGH_CHANNEL_UTIL_THRESHOLD }
                    .forEach { (num, entry) ->
                        add(
                            AttentionItem(
                                nameByNum[num] ?: "?",
                                "Wysokie zajęcie kanału: ${MetricFormatter.percent(
                                    entry?.channelUtilization ?: 0f,
                                )}",
                            ),
                        )
                    }
            }

            NetworkSummaryUiState(
                onlineCount = nodes.count { it.isOnline },
                totalCount = nodes.size,
                quietestNodes = quietestNodes,
                averageBatteryLevel = if (batteryLevels.isEmpty()) null else batteryLevels.average().toInt(),
                lowBatteryCount = lowBatteryNodes.size,
                mainsPoweredCount = mainsPowered,
                topSignalNodes = topSignalNodes,
                weakSignalCount = weakSignalNodes.size,
                averageNoiseFloor = if (noiseFloors.isEmpty()) null else noiseFloors.average().toInt(),
                averageChannelUtilization = if (channelUtils.isEmpty()) null else channelUtils.average().toFloat(),
                maxChannelUtilization = channelUtils.maxOrNull(),
                averageAirUtilTx = if (airUtils.isEmpty()) null else airUtils.average().toFloat(),
                totalPacketsTx = totalTx,
                totalPacketsRx = totalRx,
                totalDupes = totalDupes,
                totalBad = totalBad,
                topRelayNodeName = topRelay?.key?.let { nameByNum[it] },
                topRelayCount = topRelay?.value?.txRelay ?: 0,
                neighborReportingCount = neighborCounts.size,
                averageNeighborCount = if (neighborCounts.isEmpty()) null else neighborCounts.average(),
                uptimeLeaders = uptimeLeaders,
                topPositionSenders = topPositionSenders,
                closestNodes = closestNodes,
                topTelemetryVolumeSenders = topTelemetryVolumeSenders,
                topAllPacketVolumeSenders = topAllPacketVolumeSenders,
                channelMessageCounts = channelMessageCounts,
                totalMessagesWeek = messageLogsWeek.size,
                totalMessagesToday = messageLogsToday.size,
                topMessageSendersWeek = topMessageSendersWeek,
                topMessageSendersToday = topMessageSendersToday,
                attentionItems = attentionItems,
            )
        }
            .stateInWhileSubscribed(initialValue = NetworkSummaryUiState())

    companion object {
        private const val HISTORY_WINDOW_MILLIS = 24 * 60 * 60 * 1000L
        private const val WEEK_WINDOW_MILLIS = 7 * 24 * 60 * 60 * 1000L
        private const val LOW_BATTERY_THRESHOLD = 20
        private const val MAINS_POWERED_THRESHOLD = 100
        private const val WEAK_SNR_THRESHOLD = -10f
        private const val HIGH_CHANNEL_UTIL_THRESHOLD = 25f
        private const val TOP_LIST_COUNT = 3
        private const val MILLIS_PER_SEC = 1000L
        private const val SECONDS_PER_HOUR = 3600L
    }
}
