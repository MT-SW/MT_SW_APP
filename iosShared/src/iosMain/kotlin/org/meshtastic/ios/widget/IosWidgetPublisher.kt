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

package org.meshtastic.ios.widget

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.repository.LiveUpdateMode
import org.meshtastic.core.repository.LiveUpdatePrefs
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.ServiceRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.formatDurationSuspend
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.live_update_signal
import org.meshtastic.core.resources.local_stats_uptime
import org.meshtastic.core.resources.local_stats_utilization
import org.meshtastic.core.service.stats.MeshStatsLines
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToURL
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/** The shared folder of the app and its widget (App Group). */
internal const val WIDGET_APP_GROUP = "group.pl.sw.mt"

/** The file in that folder the widget reads. */
private const val WIDGET_FILE = "widget-stats.json"

/** Name of the in-process notification the Swift side listens to, to reload the widget. */
private const val WIDGET_REFRESH = "MTSWWidgetRefresh"

private val MIN_PUBLISH_INTERVAL = 30.seconds

/**
 * Publishes the statistics for the widget of iPhone/iPad: the same lines as the permanent notification on Android
 * (mesh status, or one tracked node when chosen in "powiadomienia adaptacyjne"). Without the App Group (for example a
 * build signed without it) nothing is written and the widget shows a hint to open the app.
 */
@Suppress("LongParameterList")
class IosWidgetPublisher(
    private val nodes: NodeRepository,
    private val packets: PacketRepository,
    private val prefs: LiveUpdatePrefs,
    private val service: ServiceRepository,
) {
    private data class Basics(val online: Int, val total: Int, val unread: Int, val connected: Boolean)

    /** Runs for as long as the caller lives: writes a new file whenever the statistics change (at most every 30 s). */
    suspend fun run() {
        val basics: Flow<Basics> =
            combine(
                nodes.onlineNodeCount,
                nodes.totalNodeCount,
                packets.getUnreadCountTotal(),
                service.connectionState,
            ) { online, total, unread, state ->
                Basics(online, total, unread, state is ConnectionState.Connected)
            }
        val stats = combine(nodes.localStats, nodes.localStatsExtended, nodes.ourNodeInfo) { s, e, our -> Triple(s, e, our) }
        combine(basics, stats, prefs.mode, nodes.nodeDBbyNum.map { it.size }) { b, s, mode, _ -> Triple(b, s, mode) }
            .conflate()
            .collect { (b, s, mode) ->
                runCatching { publish(b, s.first, s.second, s.third?.deviceMetrics?.battery_level, s.third?.effectiveLongName, mode) }
                    .onFailure { Logger.w(it) { "Could not publish the widget statistics" } }
                delay(MIN_PUBLISH_INTERVAL)
            }
    }

    @Suppress("LongParameterList")
    private suspend fun publish(
        b: Basics,
        stats: org.meshtastic.proto.LocalStats,
        extended: org.meshtastic.core.model.util.LocalStatsExtended,
        battery: Int?,
        ourName: String?,
        mode: LiveUpdateMode,
    ) {
        val tracked = (mode as? LiveUpdateMode.TrackNode)?.let { nodes.nodeDBbyNum.value[it.nodeNum] }
        val title: String
        val lines: List<String>
        if (tracked != null) {
            title = tracked.effectiveLongName
            lines = trackedLines(tracked)
        } else {
            title = ourName?.takeIf { it.isNotBlank() } ?: "MT_SW"
            lines =
                MeshStatsLines.meshStatus(
                    nodesOnline = b.online,
                    nodesTotal = b.total,
                    unread = b.unread,
                    radioBattery = battery,
                    localStats = stats.takeIf { it.uptime_seconds != 0 },
                    extended = extended.takeIf { it != org.meshtastic.core.model.util.LocalStatsExtended() },
                )
        }
        val chip = if (b.connected) "${b.online}/${b.total}" else ""
        write(json(title, chip, lines, b.connected))
    }

    private suspend fun trackedLines(node: org.meshtastic.core.model.Node): List<String> = buildList {
        node.deviceMetrics.battery_level?.let { add(MeshStatsLines.batteryText(it)) }
        val snr = node.snrOrNull
        val rssi = node.rssiOrNull
        if (snr != null || rssi != null) {
            add(
                getStringSuspend(
                    Res.string.live_update_signal,
                    org.meshtastic.core.common.util.MetricFormatter.snr(snr),
                    org.meshtastic.core.common.util.MetricFormatter.rssi(rssi),
                ),
            )
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
    }

    private fun json(title: String, chip: String, lines: List<String>, connected: Boolean): String =
        "{\"title\":${quote(title)},\"chip\":${quote(chip)},\"connected\":$connected," +
            "\"updated\":${Clock.System.now().epochSeconds},\"lines\":[${lines.joinToString(",") { quote(it) }}]}"

    private fun quote(text: String): String = buildString {
        append('"')
        for (c in text) {
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c.code < 0x20 -> append(' ')
                else -> append(c)
            }
        }
        append('"')
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun write(json: String) {
        val folder = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(WIDGET_APP_GROUP)
        if (folder == null) {
            Logger.w { "The widget folder (App Group) is not available" }
            return
        }
        val url: NSURL = folder.URLByAppendingPathComponent(WIDGET_FILE) ?: return
        NSString.create(string = json).writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        NSNotificationCenter.defaultCenter.postNotificationName(WIDGET_REFRESH, null)
    }
}
