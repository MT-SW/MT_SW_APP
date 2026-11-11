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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.model.util.LocalStatsExtended
import org.meshtastic.core.repository.LiveUpdateMode
import org.meshtastic.core.repository.LiveUpdatePrefs
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.ServiceRepository
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.formatDurationSuspend
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.live_update_signal
import org.meshtastic.core.resources.local_stats_uptime
import org.meshtastic.core.resources.local_stats_utilization
import org.meshtastic.core.resources.widget_card_heard
import org.meshtastic.core.resources.widget_card_hops
import org.meshtastic.core.resources.widget_card_model
import org.meshtastic.core.resources.widget_card_voltage
import org.meshtastic.proto.LocalStats
import kotlin.time.Clock

/** What a widget shows: a title, a short chip (nodes online/all), whether the radio is connected, and the lines. */
data class MeshStatsSnapshot(
    val title: String,
    val chip: String,
    val connected: Boolean,
    val lines: List<String>,
    /** The same readings in titled sections (network, node, radio, resources, connections) for the iPhone widget. */
    val groups: List<MeshStatsLines.Group> = emptyList(),
    /** Readings of our favorite nodes, one card each (only when asked for: the bigger iPhone widgets rotate them). */
    val favorites: List<MeshStatsNodeCard> = emptyList(),
    /** Readings of the other nodes of the mesh (heard most recently first), one card each. */
    val others: List<MeshStatsNodeCard> = emptyList(),
    /** The whole mesh without our node, favorites included (what the smaller widgets show for "the whole network"). */
    val network: List<MeshStatsNodeCard> = emptyList(),
    /** Our node's short name and colours (ARGB), for the chip the widgets draw instead of the online count. */
    val shortName: String = "",
    val color: Int = 0,
    val textColor: Int = 0,
)

/** The readings of one node: its name and the lines (battery, signal, uptime, channel use). */
data class MeshStatsNodeCard(
    val title: String,
    val lines: List<String>,
    /** The node's short name and colours (ARGB) as drawn on its chip. */
    val shortName: String = "",
    val color: Int = 0,
    val textColor: Int = 0,
)

/**
 * The statistics of the permanent notification on Android (mesh status, or one tracked node when chosen in the adaptive
 * notifications) as a flow, for the widgets of every system, so they all show exactly the same lines.
 */
@Single
class MeshStatsFeed(
    private val nodes: NodeRepository,
    private val packets: PacketRepository,
    private val prefs: LiveUpdatePrefs,
    private val service: ServiceRepository,
    private val uiPrefs: UiPrefs,
) {
    private data class Basics(val online: Int, val total: Int, val unread: Int, val connected: Boolean)

    /**
     * [ownChoice]: follow the widget's own choice (Android Settings) instead of the adaptive notification's mode, which
     * the iPhone widget still uses.
     */
    fun snapshots(ownChoice: Boolean = false, withNodes: Boolean = false): Flow<MeshStatsSnapshot> {
        val modeFlow: Flow<LiveUpdateMode> =
            if (ownChoice) {
                uiPrefs.androidWidgetNode.map {
                    if (it == 0) LiveUpdateMode.MeshStatus else LiveUpdateMode.TrackNode(it)
                }
            } else {
                prefs.mode
            }
        val basics: Flow<Basics> =
            combine(
                nodes.onlineNodeCount,
                nodes.totalNodeCount,
                packets.getUnreadCountTotal(),
                service.connectionState,
            ) { online, total, unread, state ->
                Basics(online, total, unread, state is ConnectionState.Connected)
            }
        val stats =
            combine(nodes.localStats, nodes.localStatsExtended, nodes.ourNodeInfo) { s, e, our -> Triple(s, e, our) }
        // The gain of the antenna amplifier is part of the flow so a change of it refreshes the widget at once.
        return combine(basics, stats, modeFlow, nodes.nodeDBbyNum.map { it.size }, uiPrefs.lnaGains) {
                b,
                s,
                mode,
                _,
                _,
            ->
            Triple(b, s, mode)
        }
            .map { (b, s, mode) -> build(b, s.first, s.second, s.third, mode, withNodes) }
    }

    @Suppress("LongParameterList")
    private suspend fun build(
        b: Basics,
        stats: LocalStats,
        extended: LocalStatsExtended,
        our: Node?,
        mode: LiveUpdateMode,
        withNodes: Boolean = false,
    ): MeshStatsSnapshot {
        // Same display-only correction as in the app: the noise floor minus the gain of our own radio.
        val lnaGain = our?.num?.let { uiPrefs.lnaGains.value[it] } ?: 0
        val tracked = (mode as? LiveUpdateMode.TrackNode)?.let { nodes.nodeDBbyNum.value[it.nodeNum] }
        val title: String
        val lines: List<String>
        val groups: List<MeshStatsLines.Group>
        if (tracked != null) {
            title = tracked.effectiveLongName
            lines = trackedLines(tracked)
            groups = listOf(MeshStatsLines.Group("", lines))
        } else {
            title = our?.effectiveLongName?.takeIf { it.isNotBlank() } ?: "MT_SW"
            groups =
                MeshStatsLines.meshStatusGroups(
                    nodesOnline = b.online,
                    nodesTotal = b.total,
                    unread = b.unread,
                    radioBattery = our?.deviceMetrics?.battery_level,
                    localStats = stats.takeIf { it.uptime_seconds != 0 },
                    extended = extended.takeIf { it != LocalStatsExtended() },
                    lnaGain = lnaGain,
                )
            lines = groups.flatMap { it.lines }
        }
        val chip = if (b.connected) "${b.online}/${b.total}" else ""
        val ownShort = our?.user?.short_name.orEmpty()
        val ownColors = our?.colors
        if (!withNodes) {
            return MeshStatsSnapshot(
                title,
                chip,
                b.connected,
                lines,
                groups,
                shortName = ownShort,
                color = ownColors?.second ?: 0,
                textColor = ownColors?.first ?: 0,
            )
        }
        val others = nodes.nodeDBbyNum.value.values.filter { it.num != our?.num }
        val favorites = others.filter { it.isFavorite }.sortedBy { it.effectiveLongName }.take(CARD_LIMIT)
        val rest = others.filter { !it.isFavorite && it.lastHeard > 0 }.sortedByDescending { it.lastHeard }
        return MeshStatsSnapshot(
            title,
            chip,
            b.connected,
            lines,
            groups,
            favorites = favorites.map { card(it) },
            others = rest.take(CARD_LIMIT).map { card(it) },
            network =
            others
                .filter { it.lastHeard > 0 }
                .sortedByDescending { it.lastHeard }
                .take(CARD_LIMIT)
                .map { card(it) },
            shortName = ownShort,
            color = ownColors?.second ?: 0,
            textColor = ownColors?.first ?: 0,
        )
    }

    private suspend fun card(node: Node) = MeshStatsNodeCard(
        title = node.effectiveLongName,
        lines = trackedLines(node),
        shortName = node.user.short_name,
        color = node.colors.second,
        textColor = node.colors.first,
    )

    private suspend fun trackedLines(node: Node): List<String> = buildList {
        node.deviceMetrics.battery_level?.let { add(MeshStatsLines.batteryText(it)) }
        val snr = node.snrOrNull
        val rssi = node.rssiOrNull
        if (snr != null || rssi != null) {
            add(getStringSuspend(Res.string.live_update_signal, MetricFormatter.snr(snr), MetricFormatter.rssi(rssi)))
        }
        val metrics = node.deviceMetrics
        metrics.uptime_seconds?.let {
            add(getStringSuspend(Res.string.local_stats_uptime, formatDurationSuspend(it.toLong())))
        }
        if (metrics.channel_utilization != null || metrics.air_util_tx != null) {
            add(
                getStringSuspend(
                    Res.string.local_stats_utilization,
                    NumberFormatter.format((metrics.channel_utilization ?: 0f).toDouble(), 2),
                    NumberFormatter.format((metrics.air_util_tx ?: 0f).toDouble(), 2),
                ),
            )
        }
        addSensorLines(node)
        if (node.lastHeard != 0) {
            val ago = (Clock.System.now().epochSeconds - node.lastHeard).coerceAtLeast(0)
            add(getStringSuspend(Res.string.widget_card_heard, formatDurationSuspend(ago)))
        }
        if (node.hopsAway >= 0) add(getStringSuspend(Res.string.widget_card_hops, node.hopsAway))
        val role = node.user.role.name
        val model = node.user.hw_model.name
        add(getStringSuspend(Res.string.widget_card_model, role, model))
    }
}

/** Sensor readings of a node: climate, plus voltage and current taken from the sensor, not from the system. */
private suspend fun MutableList<String>.addSensorLines(node: Node) {
    val env = node.environmentMetrics
    val power = node.powerMetrics
    env.temperature?.takeIf { !it.isNaN() }?.let { add(MetricFormatter.temperature(it, false)) }
    env.relative_humidity?.takeIf { it != 0f }?.let { add(MetricFormatter.humidity(it)) }
    env.barometric_pressure?.takeIf { it != 0f }?.let { add(MetricFormatter.pressure(it)) }
    val volts = power.ch1_voltage ?: env.voltage
    val milliAmps = power.ch1_current ?: env.current
    volts?.let { add(getStringSuspend(Res.string.widget_card_voltage, NumberFormatter.format(it.toDouble(), 2))) }
    milliAmps?.let { add(MetricFormatter.current(it)) }
}

/** How many nodes of one kind the bigger widgets rotate through (all of them, within reason). */
private const val CARD_LIMIT = 500
