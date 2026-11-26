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
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.NetworkReportMonitor
import org.meshtastic.core.repository.NodeManager
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.network_report_message
import org.meshtastic.core.resources.network_report_title

/**
 * MT_SW: the weekly report of the network. When switched on, the day it was switched on is remembered and a week later
 * (and then every week) a notification tells how many nodes were heard in the last 7 days and how many are active now.
 * It is posted only while the radio is connected and its node list is ready, so the numbers are never half read.
 */
@Single
class NetworkReportMonitorImpl(
    private val nodeManager: NodeManager,
    private val notifications: MeshNotificationManager,
    private val uiPrefs: UiPrefs,
    private val connection: ConnectionStateProvider,
) : NetworkReportMonitor {
    override fun start(scope: CoroutineScope) {
        scope.launch {
            delay(FIRST_CHECK_MS)
            while (isActive) {
                try {
                    check()
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Logger.w { "Raport tygodniowy: sprawdzenie nie udało się: ${e.message}" }
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun check() {
        val options = UiOptions(uiPrefs.uiOptions.value)
        if (options[UiOptions.NETWORK_REPORT] != 1) return
        val now = nowSeconds
        val today = (now / SECONDS_PER_DAY).toInt()
        val last = options[UiOptions.NETWORK_REPORT_LAST]
        if (last == 0) {
            uiPrefs.setUiOption(UiOptions.NETWORK_REPORT_LAST, today)
            Logger.i { "Raport tygodniowy: włączony, pierwszy za $DAYS_BETWEEN dni" }
            return
        }
        if (today - last < DAYS_BETWEEN) return
        if (connection.connectionState.value !is ConnectionState.Connected || !nodeManager.isNodeDbReady.value) return
        val nodes = nodeManager.nodeDBbyNodeNum.values
        if (nodes.size < 2) return
        val weekAgo = now - DAYS_BETWEEN * SECONDS_PER_DAY
        val heard = nodes.count { it.lastHeard > weekAgo }
        val active = nodes.count { it.isOnline }
        val posted =
            notifications.showNetworkReportNotification(
                getStringSuspend(Res.string.network_report_title),
                getStringSuspend(Res.string.network_report_message, heard, active, nodes.size),
            )
        val result = if (posted) "wysłany" else "nie przyjęty przez system"
        Logger.i { "Raport tygodniowy: $result, węzłów ${nodes.size}" }
        if (posted) uiPrefs.setUiOption(UiOptions.NETWORK_REPORT_LAST, today)
    }

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
        const val DAYS_BETWEEN = 7
        const val FIRST_CHECK_MS = 2 * 60_000L
        const val CHECK_INTERVAL_MS = 60 * 60_000L
    }
}
