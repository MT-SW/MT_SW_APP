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
package org.meshtastic.core.data.manager

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.NodeManager
import org.meshtastic.core.repository.NodeSilenceMonitor
import org.meshtastic.core.repository.NotificationPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.formatDurationSuspend
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.node_silence_alert_group
import org.meshtastic.core.resources.node_silence_alert_notification
import org.meshtastic.core.resources.node_silence_test_notification
import org.meshtastic.core.resources.unknown_username
import kotlin.time.Clock
import kotlin.time.DurationUnit

/**
 * Raises a notification for a node whose radio signal has been missing for the time chosen for it in the node details
 * (from an hour up to many days).
 *
 * It judges by the time the radio last heard the node, so it runs only while the app is connected to the radio: without
 * the connection nothing is heard from anybody. A short pause after every (re)connection lets the radio hand over its
 * node list first, so a node is never reported silent just because the list was not up to date yet. One notification is
 * raised per silence: it is raised again only after the node was heard and went silent once more.
 */
@Single
class NodeSilenceMonitorImpl(
    private val nodeManager: NodeManager,
    private val notifications: MeshNotificationManager,
    private val notificationPrefs: NotificationPrefs,
    private val connection: ConnectionStateProvider,
) : NodeSilenceMonitor {
    /** Node number → the time (seconds) it was last heard when the alert for it was raised. */
    private val alerted = mutableMapOf<Int, Int>()
    private var connectedSince = 0L

    override fun start(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                try {
                    check()
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Logger.e(e) { "Checking the node silence alerts failed" }
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    override suspend fun sendTest(nodeNum: Int) {
        val node = nodeManager.nodeDBbyNodeNum[nodeNum] ?: return
        val minutes = notificationPrefs.nodeSilenceAlerts.value[nodeNum] ?: DEFAULT_TEST_MINUTES
        val title =
            getStringSuspend(
                Res.string.node_silence_test_notification,
                displayName(node),
                formatDurationSuspend(minutes * SECONDS_PER_MINUTE, DurationUnit.MINUTES),
            )
        notifications.showNodeSilenceNotification(node, title)
    }

    private suspend fun check() {
        val now = nowSeconds
        if (connection.connectionState.value !is ConnectionState.Connected) {
            connectedSince = 0L
            return
        }
        if (connectedSince == 0L) connectedSince = now
        if (now - connectedSince < SETTLE_SECONDS) return

        val alerts = notificationPrefs.nodeSilenceAlerts.value
        // An alert that was removed (or its node removed) also ends its notification.
        for (nodeNum in alerted.keys.toList()) {
            if (nodeNum !in alerts) {
                alerted.remove(nodeNum)
                notifications.cancelNodeSilenceNotification(nodeNum)
            }
        }
        val countRelayed = notificationPrefs.nodeSilenceCountRelayed.value
        val quiet =
            notificationPrefs.nodeSilenceQuietHours.value?.contains(
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour,
            ) == true
        // Nodes that went silent in this round: several at once (a power cut, a bad hour on the band) are reported
        // with one notification instead of a burst.
        @Suppress("LoopWithTooManyJumpStatements")
        val newlySilent = mutableListOf<Pair<Node, Long>>()
        for ((nodeNum, minutes) in alerts) {
            val node = nodeManager.nodeDBbyNodeNum[nodeNum] ?: continue
            var lastHeard = node.lastHeard
            if (nodeNum in countRelayed) lastHeard = maxOf(lastHeard, relayedSeconds(nodeNum))
            // A node never heard has no moment to count from.
            if (lastHeard == 0) continue
            val silentSeconds = now - lastHeard
            if (silentSeconds >= minutes * SECONDS_PER_MINUTE) {
                // In quiet hours the alert is held back and not remembered, so it is raised once they end.
                if (alerted[nodeNum] != lastHeard && !quiet) {
                    alerted[nodeNum] = lastHeard
                    newlySilent += node to silentSeconds
                }
            } else if (alerted.remove(nodeNum) != null) {
                // Heard again: the old alert is out of date.
                notifications.cancelNodeSilenceNotification(nodeNum)
            }
        }
        raise(newlySilent)
    }

    private suspend fun raise(newlySilent: List<Pair<Node, Long>>) {
        if (newlySilent.isEmpty()) return
        Logger.i { "${newlySilent.size} node(s) silent; raising the silence alert" }
        if (newlySilent.size >= GROUP_FROM) {
            val names = newlySilent.map { (node, _) -> displayName(node) }
            val more = if (names.size > GROUP_NAMES_SHOWN) ", …" else ""
            val shown = names.take(GROUP_NAMES_SHOWN).joinToString(", ") + more
            val title = getStringSuspend(Res.string.node_silence_alert_group, newlySilent.size, shown)
            notifications.showNodeSilenceNotification(newlySilent.first().first, title)
            return
        }
        for ((node, silentSeconds) in newlySilent) {
            val title =
                getStringSuspend(
                    Res.string.node_silence_alert_notification,
                    displayName(node),
                    formatDurationSuspend(silentSeconds, DurationUnit.MINUTES),
                )
            notifications.showNodeSilenceNotification(node, title)
        }
    }

    private suspend fun displayName(node: Node): String =
        node.effectiveLongName.takeIf { it.isNotBlank() } ?: getStringSuspend(Res.string.unknown_username)

    /**
     * When a packet last came through [nodeNum], or 0. The packet names its relay by one byte only, so it is counted
     * only when no other known node ends with the same byte; otherwise it could belong to someone else.
     */
    private fun relayedSeconds(nodeNum: Int): Int {
        val suffix = nodeNum and SUFFIX_MASK
        val sameSuffix = nodeManager.nodeDBbyNodeNum.keys.count { it != nodeNum && (it and SUFFIX_MASK) == suffix }
        return if (sameSuffix == 0) RelayActivityTracker.lastRelayedSeconds(nodeNum) else 0
    }

    private companion object {
        const val SUFFIX_MASK = 0xFF
        const val CHECK_INTERVAL_MS = 60_000L
        const val SETTLE_SECONDS = 120L
        const val SECONDS_PER_MINUTE = 60L
        const val DEFAULT_TEST_MINUTES = 60

        /** From this many nodes silent in one round, one notification names them all. */
        const val GROUP_FROM = 3
        const val GROUP_NAMES_SHOWN = 3
    }
}
