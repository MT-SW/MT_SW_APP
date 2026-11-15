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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodePresence
import org.meshtastic.core.model.NodePresenceState
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.NodeActiveMonitor
import org.meshtastic.core.repository.NodeManager
import org.meshtastic.core.repository.NotificationPrefs
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.repository.nodePresenceFlow
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.node_active_alert_notification
import org.meshtastic.core.resources.unknown_username
import kotlin.time.Clock

/**
 * MT_SW: raises a notification for a node with an "active" alert when its activity status changes to active.
 *
 * It runs only while the app is connected to the radio, and a short pause after every (re)connection lets the status
 * answers arrive first: a node that was already active then is not reported as just become active. The quiet hours of
 * the silence alerts hold the notification back, and it is raised when they end, if the node is still active.
 */
@Single
class NodeActiveMonitorImpl(
    private val nodeManager: NodeManager,
    private val notifications: MeshNotificationManager,
    private val notificationPrefs: NotificationPrefs,
    private val connection: ConnectionStateProvider,
    private val meshLogRepository: MeshLogRepository,
    private val uiPrefs: UiPrefs,
) : NodeActiveMonitor {
    /** Node number → whether it was active at the last check; a node not in the map has not been seen yet. */
    private val wasActive = mutableMapOf<Int, Boolean>()
    private var connectedSince = 0L

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun start(scope: CoroutineScope) {
        // The logs are read only while some node has the alert.
        val presence =
            notificationPrefs.nodeActiveAlerts
                .flatMapLatest { if (it.isEmpty()) flowOf(emptyMap()) else meshLogRepository.nodePresenceFlow() }
                .stateIn(scope, SharingStarted.Eagerly, emptyMap())
        scope.launch {
            while (isActive) {
                try {
                    check(presence.value)
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Logger.e(e) { "Checking the active alerts failed" }
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun check(presence: Map<Int, NodePresence>) {
        val nowMs = nowMillis
        if (connection.connectionState.value !is ConnectionState.Connected) {
            connectedSince = 0L
            wasActive.clear()
            return
        }
        if (connectedSince == 0L) connectedSince = nowMs
        val alerts = notificationPrefs.nodeActiveAlerts.value
        wasActive.keys.retainAll(alerts)
        if (alerts.isEmpty() || nowMs - connectedSince < SETTLE_MILLIS) return

        val steps = NodePresence.EXPIRY_STEPS_MINUTES
        val expiryMillis =
            steps[UiOptions(uiPrefs.uiOptions.value)[UiOptions.PRESENCE_EXPIRY].coerceIn(0, steps.lastIndex)] *
                MILLIS_PER_MINUTE
        val quiet =
            notificationPrefs.nodeSilenceQuietHours.value?.contains(
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour,
            ) == true

        for (nodeNum in alerts) {
            val node = nodeManager.nodeDBbyNodeNum[nodeNum] ?: continue
            val active =
                presence[nodeNum]?.effective(nowMs, node.lastHeard, expiryMillis) == NodePresenceState.ONLINE
            val before = wasActive[nodeNum]
            if (active && before == false && quiet) continue // held back: raised after the quiet hours
            wasActive[nodeNum] = active
            if (active && before == false) raise(node)
        }
    }

    private suspend fun raise(node: Node) {
        val name = node.effectiveLongName.takeIf { it.isNotBlank() } ?: getStringSuspend(Res.string.unknown_username)
        Logger.i { "Node ${node.num} became active; raising the alert" }
        notifications.showNodeSilenceNotification(node, getStringSuspend(Res.string.node_active_alert_notification, name))
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 30_000L
        const val SETTLE_MILLIS = 120_000L
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
