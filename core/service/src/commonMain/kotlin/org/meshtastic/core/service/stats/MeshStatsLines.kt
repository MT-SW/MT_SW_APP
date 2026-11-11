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
package org.meshtastic.core.service.stats

import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.noiseFloorOrNull
import org.meshtastic.core.model.util.LocalStatsExtended
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.formatDurationSuspend
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.live_update_ble
import org.meshtastic.core.resources.live_update_cpu
import org.meshtastic.core.resources.live_update_flash
import org.meshtastic.core.resources.live_update_nodes_online
import org.meshtastic.core.resources.live_update_psram
import org.meshtastic.core.resources.live_update_ram
import org.meshtastic.core.resources.live_update_unread
import org.meshtastic.core.resources.live_update_wifi
import org.meshtastic.core.resources.local_stats_bad
import org.meshtastic.core.resources.local_stats_battery
import org.meshtastic.core.resources.local_stats_diagnostics_prefix
import org.meshtastic.core.resources.local_stats_dropped
import org.meshtastic.core.resources.local_stats_heap
import org.meshtastic.core.resources.local_stats_heap_value_kb
import org.meshtastic.core.resources.local_stats_noise
import org.meshtastic.core.resources.local_stats_relays
import org.meshtastic.core.resources.local_stats_traffic
import org.meshtastic.core.resources.local_stats_uptime
import org.meshtastic.core.resources.local_stats_utilization
import org.meshtastic.core.resources.powered
import org.meshtastic.core.resources.widget_group_connections
import org.meshtastic.core.resources.widget_group_network
import org.meshtastic.core.resources.widget_group_node
import org.meshtastic.core.resources.widget_group_radio
import org.meshtastic.core.resources.widget_group_resources
import org.meshtastic.proto.LocalStats

private const val MAX_BATTERY_LEVEL = 100
private const val BYTES_PER_KB = 1024.0

/**
 * The lines of the "mesh status" statistics: the same text on every system (the Android permanent notification and the
 * widgets of iPhone/iPad and the computer).
 */
object MeshStatsLines {
    private const val RIGHT_COLUMN = 1

    /**
     * One titled block of statistics, for the widgets that show them in sections. [column] says which of two columns
     * it belongs to when the widget is wide enough for two: 0 the left one, 1 the right one.
     */
    data class Group(val title: String, val lines: List<String>, val column: Int = 0)

    /** The flat lines, in the order of the groups of [meshStatusGroups]. */
    @Suppress("LongParameterList")
    suspend fun meshStatus(
        nodesOnline: Int,
        nodesTotal: Int,
        unread: Int,
        radioBattery: Int?,
        localStats: LocalStats?,
        extended: LocalStatsExtended?,
        lnaGain: Int = 0,
    ): List<String> = meshStatusGroups(
        nodesOnline,
        nodesTotal,
        unread,
        radioBattery,
        localStats,
        extended,
        lnaGain,
    )
        .flatMap { it.lines }

    /**
     * The statistics grouped as: the network (nodes, unread), the node itself (power, uptime), the radio (airtime,
     * traffic, diagnostics), the resources (CPU, RAM, flash, PSRAM) and the connections (Wi-Fi, Bluetooth). Empty
     * groups are left out. [lnaGain] is the display-only correction of the noise floor (the same as in the app).
     */
    @Suppress("LongParameterList")
    suspend fun meshStatusGroups(
        nodesOnline: Int,
        nodesTotal: Int,
        unread: Int,
        radioBattery: Int?,
        localStats: LocalStats?,
        extended: LocalStatsExtended?,
        lnaGain: Int = 0,
    ): List<Group> {
        val network =
            listOf(
                getStringSuspend(Res.string.live_update_nodes_online, nodesOnline, nodesTotal),
                getStringSuspend(Res.string.live_update_unread, unread),
            )
        val node = buildList {
            radioBattery?.let { add(batteryText(it)) }
            localStats?.let {
                add(getStringSuspend(Res.string.local_stats_uptime, formatDurationSuspend(it.uptime_seconds.toLong())))
            }
        }
        val radio = localStats?.let { radioLines(it, lnaGain) }.orEmpty()
        val heapLine = localStats?.let { heapLine(it) }
        val resources = resourceLines(extended, ramAlreadyShown = heapLine != null, ramLine = heapLine)
        val connections = connectionLines(extended)
        return listOf(
            Group(getStringSuspend(Res.string.widget_group_network), network),
            Group(getStringSuspend(Res.string.widget_group_node), node, RIGHT_COLUMN),
            Group(getStringSuspend(Res.string.widget_group_radio), radio),
            Group(getStringSuspend(Res.string.widget_group_resources), resources, RIGHT_COLUMN),
            Group(getStringSuspend(Res.string.widget_group_connections), connections, RIGHT_COLUMN),
        )
            .filter { it.lines.isNotEmpty() }
    }

    suspend fun batteryText(level: Int): String = if (level > MAX_BATTERY_LEVEL) {
        getStringSuspend(Res.string.powered)
    } else {
        getStringSuspend(Res.string.local_stats_battery, level)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private suspend fun radioLines(stats: LocalStats, lnaGain: Int): List<String> = buildList {
        add(
            getStringSuspend(
                Res.string.local_stats_utilization,
                NumberFormatter.format(stats.channel_utilization.toDouble(), 2),
                NumberFormatter.format(stats.air_util_tx.toDouble(), 2),
            ),
        )
        if (stats.num_packets_tx > 0 || stats.num_packets_rx > 0) {
            add(
                getStringSuspend(
                    Res.string.local_stats_traffic,
                    stats.num_packets_tx,
                    stats.num_packets_rx,
                    stats.num_rx_dupe,
                ),
            )
        }
        if (stats.num_tx_relay > 0) {
            add(getStringSuspend(Res.string.local_stats_relays, stats.num_tx_relay, stats.num_tx_relay_canceled))
        }
        val diagnostics = mutableListOf<String>()
        stats.noiseFloorOrNull?.let { diagnostics.add(getStringSuspend(Res.string.local_stats_noise, it - lnaGain)) }
        if (stats.num_packets_rx_bad > 0) {
            diagnostics.add(getStringSuspend(Res.string.local_stats_bad, stats.num_packets_rx_bad))
        }
        if (stats.num_tx_dropped > 0) {
            diagnostics.add(getStringSuspend(Res.string.local_stats_dropped, stats.num_tx_dropped))
        }
        if (diagnostics.isNotEmpty()) {
            add(getStringSuspend(Res.string.local_stats_diagnostics_prefix, diagnostics.joinToString(" | ")))
        }
    }

    /** The RAM line from the node's own counters (used and total heap), or null when the node does not report it. */
    private suspend fun heapLine(stats: LocalStats): String? {
        if (stats.heap_free_bytes <= 0 && stats.heap_total_bytes <= 0) return null
        val used = (stats.heap_total_bytes - stats.heap_free_bytes).coerceAtLeast(0)
        return getStringSuspend(Res.string.local_stats_heap) +
            ": " +
            getStringSuspend(
                Res.string.local_stats_heap_value_kb,
                NumberFormatter.format(used / BYTES_PER_KB, 2),
                NumberFormatter.format(stats.heap_total_bytes / BYTES_PER_KB, 2),
            ) +
            " (" +
            MetricFormatter.usedPercent(used.toLong(), stats.heap_total_bytes.toLong()) +
            ")"
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun resourceLines(
        ext: LocalStatsExtended?,
        ramAlreadyShown: Boolean,
        ramLine: String? = null,
    ): List<String> {
        if (ext == null) return listOfNotNull(ramLine)
        return buildList {
            if (ext.cpuUsagePercent > 0) {
                add(
                    getStringSuspend(
                        Res.string.live_update_cpu,
                        MetricFormatter.percent(ext.cpuUsagePercent.toFloat(), 0),
                    ),
                )
            }
            ramLine?.let { add(it) }
            if (!ramAlreadyShown && ext.memoryTotal > 0) {
                val used = (ext.memoryTotal - ext.memoryFreeCheap).toLong()
                add(
                    getStringSuspend(
                        Res.string.live_update_ram,
                        MetricFormatter.usedPercent(used, ext.memoryTotal.toLong()),
                    ),
                )
            }
            if (ext.flashTotalBytes > 0) {
                add(
                    getStringSuspend(
                        Res.string.live_update_flash,
                        MetricFormatter.usedPercent(ext.flashUsedBytes.toLong(), ext.flashTotalBytes.toLong()),
                    ),
                )
            }
            if (ext.memoryPsramTotal > 0) {
                val used = (ext.memoryPsramTotal - ext.memoryPsramFree).toLong()
                add(
                    getStringSuspend(
                        Res.string.live_update_psram,
                        MetricFormatter.usedPercent(used, ext.memoryPsramTotal.toLong()),
                    ),
                )
            }
        }
    }

    private suspend fun connectionLines(ext: LocalStatsExtended?): List<String> {
        if (ext == null) return emptyList()
        return buildList {
            if (ext.wifiRssi != 0) add(getStringSuspend(Res.string.live_update_wifi, ext.wifiRssi))
            if (ext.bleRssi != 0) add(getStringSuspend(Res.string.live_update_ble, ext.bleRssi))
        }
    }
}
