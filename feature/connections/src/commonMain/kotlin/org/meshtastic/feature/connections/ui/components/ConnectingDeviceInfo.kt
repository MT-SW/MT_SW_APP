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
package org.meshtastic.feature.connections.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.TransportPhase
import org.meshtastic.core.model.TransportStatus
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.connected
import org.meshtastic.core.resources.connected_sleeping
import org.meshtastic.core.resources.connecting
import org.meshtastic.core.resources.connection_retry_in
import org.meshtastic.core.resources.connection_retry_now
import org.meshtastic.core.resources.disconnect
import org.meshtastic.core.resources.connection_progress_requesting_config
import org.meshtastic.core.resources.connection_progress_device_config
import org.meshtastic.core.resources.connection_progress_module_config
import org.meshtastic.core.resources.connection_progress_channels
import org.meshtastic.core.resources.connection_progress_channels_of
import org.meshtastic.core.resources.connection_progress_nodes
import org.meshtastic.core.resources.connection_progress_loading_nodes
import org.meshtastic.core.resources.node_restarting
import org.meshtastic.core.resources.not_connected
import org.meshtastic.core.resources.reconnecting
import org.meshtastic.core.resources.stop_connecting
import org.meshtastic.core.ui.component.ProgressSpinner
import org.meshtastic.core.ui.icon.Check
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.bleScanRequiresLocationServices
import org.meshtastic.core.ui.viewmodel.ConnectionStatus
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Displays the currently connecting (or connected) device with its name, address, connection status, and a disconnect
 * button. While the transport reports a [transportStatus], the status line names the step in progress or the retry
 * countdown, and the line under it says why the last attempt failed.
 */
@Suppress("LongMethod")
@Composable
fun ConnectingDeviceInfo(
    deviceName: String,
    deviceAddress: String,
    connectionStatus: ConnectionStatus,
    connectionProgress: String?,
    transportStatus: TransportStatus?,
    onClickDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now by
        produceState(nowMillis, transportStatus) {
            while (transportStatus?.phase == TransportPhase.WaitingToRetry) {
                value = nowMillis
                delay(1.seconds)
            }
        }
    val detail = transportStatus.toConnectionStatusDetail(now, bleScanRequiresLocationServices)
    // The handshake texts are written in English by the service; the card shows them in the app's language.
    val progress = localizedProgress(connectionProgress)

    val statusLabel =
        when (connectionStatus) {
            ConnectionStatus.CONNECTED -> stringResource(Res.string.connected)
            ConnectionStatus.CONNECTING -> progress ?: stringResource(Res.string.connecting)
            ConnectionStatus.RECONNECTING -> stringResource(Res.string.reconnecting)
            ConnectionStatus.RESTARTING -> stringResource(Res.string.node_restarting)
            ConnectionStatus.CONNECTED_SLEEPING -> stringResource(Res.string.connected_sleeping)
            ConnectionStatus.NOT_CONNECTED -> stringResource(Res.string.not_connected)
        }

    // This card can also render while the transport is already CONNECTED but node info has not arrived yet, so only
    // the not-yet-established lifecycle states get "Stop Connecting".
    val disconnectLabel =
        when (connectionStatus) {
            ConnectionStatus.CONNECTED,
            ConnectionStatus.CONNECTED_SLEEPING,
            -> stringResource(Res.string.disconnect)

            ConnectionStatus.CONNECTING,
            ConnectionStatus.RECONNECTING,
            ConnectionStatus.RESTARTING,
            ConnectionStatus.NOT_CONNECTED,
            -> stringResource(Res.string.stop_connecting)
        }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProgressSpinner(size = 40.dp)

            Column {
                Text(text = deviceName, style = MaterialTheme.typography.headlineSmall)
                Text(text = deviceAddress, style = MaterialTheme.typography.bodyLarge)
                val retry = detail?.retry
                val phaseLabel = rememberSettledPhase(detail?.phase)?.let { stringResource(it) }
                val primaryLabel =
                    when {
                        retry == null -> phaseLabel ?: statusLabel

                        retry.seconds > 0 ->
                            pluralStringResource(
                                Res.plurals.connection_retry_in,
                                retry.seconds,
                                retry.seconds,
                                retry.attempt,
                            )

                        else -> stringResource(Res.string.connection_retry_now, retry.attempt)
                    }
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                // The handshake's own counters ("Channels (3 / 8)") say more than its phase name does.
                progress
                    ?.takeIf { phaseLabel != null && it.isNotBlank() && it != phaseLabel }
                    ?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
                detail?.failure?.let { failure ->
                    Text(
                        text = stringResource(failure),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // MT_SW: the steps of this attempt as a short list, finished ones ticked.
        ConnectionSteps(transportStatus)

        DisconnectButton(onClick = onClickDisconnect, label = disconnectLabel)
    }
}

/** The handshake text the service wrote ("Nodes (3)"), in the app's language; any other text is shown as it is. */
@Composable
private fun localizedProgress(text: String?): String? {
    if (text == null) return null
    fun number(pattern: String) = Regex(pattern).matchEntire(text)?.groupValues?.drop(1)?.map { it.toInt() }
    return when {
        text == "Requesting config\u2026" || text == "Requesting config..." ->
            stringResource(Res.string.connection_progress_requesting_config)

        text == "Device config received" -> stringResource(Res.string.connection_progress_device_config)

        text == "Module config received" -> stringResource(Res.string.connection_progress_module_config)

        text == "Loading node list" -> stringResource(Res.string.connection_progress_loading_nodes)

        text == "Reconnecting\u2026" -> stringResource(Res.string.reconnecting)

        number("Channels \\((\\d+) / (\\d+)\\)") != null ->
            number("Channels \\((\\d+) / (\\d+)\\)")!!.let {
                stringResource(Res.string.connection_progress_channels_of, it[0], it[1])
            }

        number("Channels \\((\\d+)\\)") != null ->
            stringResource(Res.string.connection_progress_channels, number("Channels \\((\\d+)\\)")!![0])

        number("Nodes \\((\\d+)\\)") != null ->
            stringResource(Res.string.connection_progress_nodes, number("Nodes \\((\\d+)\\)")!![0])

        else -> text
    }
}

/** How long a phase must last before the card names it, so a fast connect does not flicker through every step. */
private val PHASE_SETTLE_TIME = 400.milliseconds

/**
 * [phase] once it has held for [PHASE_SETTLE_TIME]; until then the phase shown before it. The first phase and the end
 * of the phases (null, a retry countdown or a connected link) show at once. Failures are a separate line and are never
 * held back.
 */
@Composable
private fun rememberSettledPhase(phase: StringResource?): StringResource? {
    var shown by remember { mutableStateOf(phase) }
    LaunchedEffect(phase) {
        if (phase != null && shown != null) delay(PHASE_SETTLE_TIME)
        shown = phase
    }
    return shown
}

/**
 * The steps the connection attempt has gone through so far, in the order they happened: finished ones carry a tick, the
 * one in progress a small spinner. Only steps the transport really reports are listed, so a network connection shows
 * its own, shorter list. The list starts again with each new attempt.
 */
@Composable
private fun ConnectionSteps(transportStatus: TransportStatus?) {
    val phase = transportStatus?.phase
    var steps by remember { mutableStateOf(emptyList<TransportPhase>()) }
    // The body does not suspend, so it is a plain side effect (it gives the same list again when nothing changed).
    SideEffect {
        steps =
            when {
                phase == null || phase == TransportPhase.WaitingToRetry -> emptyList()
                phase == TransportPhase.Connected -> steps
                phase in steps -> steps
                else -> steps + phase
            }
    }
    if (steps.isEmpty()) return
    val inProgress = steps.last().takeIf { phase != TransportPhase.Connected }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEach { step ->
            val label = step.label() ?: return@forEach
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step == inProgress) {
                    ProgressSpinner(size = 16.dp)
                } else {
                    Icon(
                        imageVector = MeshtasticIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(text = stringResource(label), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
