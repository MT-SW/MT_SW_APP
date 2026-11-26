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
 */@file:Suppress("ModifierMissing", "LongMethod")

package org.meshtastic.feature.settings.mtsw

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.device_compare_battery
import org.meshtastic.core.resources.device_compare_connected
import org.meshtastic.core.resources.device_compare_empty
import org.meshtastic.core.resources.device_compare_nodes
import org.meshtastic.core.resources.device_compare_not_connected
import org.meshtastic.core.resources.device_compare_no_data
import org.meshtastic.core.resources.device_compare_refresh
import org.meshtastic.core.resources.device_compare_title
import org.meshtastic.core.resources.device_compare_unread
import org.meshtastic.core.resources.device_compare_uptime
import org.meshtastic.core.resources.device_compare_utilization
import org.meshtastic.core.resources.device_history_battery
import org.meshtastic.core.resources.device_history_load
import org.meshtastic.core.resources.device_history_title
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.ModalBottomSheet
import org.meshtastic.core.ui.icon.MeshHub
import org.meshtastic.core.ui.icon.MeshtasticIcons

private const val SECONDS_IN_MINUTE = 60
private const val SECONDS_IN_HOUR = 3_600
private const val SECONDS_IN_DAY = 86_400
private const val PERCENT_ROUND = 10f

/** MT_SW: an entry that opens a window comparing every saved device side by side (battery, load, uptime, mesh). */
@Composable
fun DeviceCompareEntry(viewModel: DeviceCompareViewModel = koinViewModel()) {
    var open by remember { mutableStateOf(false) }
    ListItem(text = stringResource(Res.string.device_compare_title), leadingIcon = MeshtasticIcons.MeshHub) {
        Logger.i { "Porównanie urządzeń: okno otwarte" }
        open = true
    }
    if (open) {
        val rows by viewModel.rows.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { viewModel.refresh() }
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(Res.string.device_compare_title), style = MaterialTheme.typography.titleMedium)
                if (rows.isEmpty()) {
                    Text(stringResource(Res.string.device_compare_empty), style = MaterialTheme.typography.bodyMedium)
                }
                rows.forEach { row -> DeviceCompareCard(row) }
                OutlinedButton(onClick = { viewModel.refresh() }) {
                    Text(stringResource(Res.string.device_compare_refresh))
                }
            }
        }
    }
}

@Composable
private fun DeviceCompareCard(row: DeviceCompareRow) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(row.title, style = MaterialTheme.typography.titleSmall)
        Text(
            text =
            stringResource(
                if (row.connected) Res.string.device_compare_connected else Res.string.device_compare_not_connected,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        val snap = row.snapshot
        if (snap == null) {
            Text(stringResource(Res.string.device_compare_no_data), style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        val battery = snap.batteryLevel?.let { if (it > 100) "USB" else "$it%" } ?: "—"
        val voltage = snap.voltage?.let { " (${(it * PERCENT_ROUND).toInt() / PERCENT_ROUND} V)" }.orEmpty()
        Text(stringResource(Res.string.device_compare_battery, battery + voltage))
        Text(
            stringResource(
                Res.string.device_compare_utilization,
                snap.channelUtilization.percentText(),
                snap.airUtilTx.percentText(),
            ),
        )
        Text(stringResource(Res.string.device_compare_uptime, uptimeText(snap.uptimeSeconds)))
        Text(stringResource(Res.string.device_compare_nodes, snap.onlineCount, snap.nodeCount))
        if (row.unread > 0) Text(stringResource(Res.string.device_compare_unread, row.unread))
        val trend = trendText(row.history)
        if (trend != null) Text(trend, style = MaterialTheme.typography.bodySmall)
    }
}

private fun Float?.percentText(): String = this?.let { "${(it * PERCENT_ROUND).toInt() / PERCENT_ROUND}%" } ?: "—"

private fun uptimeText(seconds: Int?): String =
    when {
        seconds == null -> "—"
        seconds >= SECONDS_IN_DAY -> "${seconds / SECONDS_IN_DAY} d ${seconds % SECONDS_IN_DAY / SECONDS_IN_HOUR} h"
        seconds >= SECONDS_IN_HOUR ->
            "${seconds / SECONDS_IN_HOUR} h ${seconds % SECONDS_IN_HOUR / SECONDS_IN_MINUTE} min"
        else -> "${seconds / SECONDS_IN_MINUTE} min"
    }

/** What changed over the last day: the battery and the load of the channel, against the oldest reading of that day. */
@Composable
private fun trendText(history: List<DeviceHistoryEntry>): String? {
    val last = history.lastOrNull() ?: return null
    val base = history.firstOrNull { last.timeMillis - it.timeMillis <= DAY_MS } ?: return null
    if (base === last) return null
    val battery =
        if (last.batteryLevel != null && base.batteryLevel != null && last.batteryLevel <= MAX_PERCENT) {
            val delta = last.batteryLevel - base.batteryLevel
            stringResource(Res.string.device_history_battery, (if (delta > 0) "+" else "") + "$delta%")
        } else {
            null
        }
    val hours = (last.timeMillis - base.timeMillis) / HOUR_MS
    val load =
        last.channelUtilization?.let { now ->
            base.channelUtilization?.let { was ->
                stringResource(Res.string.device_history_load, now.percentText(), was.percentText())
            }
        }
    val parts = listOfNotNull(battery, load)
    if (parts.isEmpty()) return null
    return stringResource(Res.string.device_history_title, hours.toInt()) + " " + parts.joinToString(", ")
}

private const val DAY_MS = 86_400_000L
private const val HOUR_MS = 3_600_000L
private const val MAX_PERCENT = 100
