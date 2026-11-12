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
@file:OptIn(ExperimentalLayoutApi::class)
@file:Suppress("LongMethod", "ModifierMissing")

package org.meshtastic.feature.settings.mtsw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.SosConfig
import org.meshtastic.core.repository.SosController
import org.meshtastic.core.repository.SosPrefs
import org.meshtastic.core.repository.SosState
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.sos_button
import org.meshtastic.core.resources.sos_button_hold
import org.meshtastic.core.resources.sos_button_tap
import org.meshtastic.core.resources.sos_cancel
import org.meshtastic.core.resources.sos_channel_n
import org.meshtastic.core.resources.sos_hold_seconds
import org.meshtastic.core.resources.sos_hold_tap
import org.meshtastic.core.resources.sos_include_position
import org.meshtastic.core.resources.sos_intro
import org.meshtastic.core.resources.sos_message_label
import org.meshtastic.core.resources.sos_position_attached
import org.meshtastic.core.resources.sos_position_missing
import org.meshtastic.core.resources.sos_repeat_count
import org.meshtastic.core.resources.sos_repeat_every
import org.meshtastic.core.resources.sos_repeat_once
import org.meshtastic.core.resources.sos_result_delivered
import org.meshtastic.core.resources.sos_result_refused
import org.meshtastic.core.resources.sos_result_unconfirmed
import org.meshtastic.core.resources.sos_section_channel
import org.meshtastic.core.resources.sos_section_hold
import org.meshtastic.core.resources.sos_section_message
import org.meshtastic.core.resources.sos_section_repeat
import org.meshtastic.core.resources.sos_status_delivered
import org.meshtastic.core.resources.sos_status_enroute
import org.meshtastic.core.resources.sos_status_error
import org.meshtastic.core.resources.sos_status_queued
import org.meshtastic.core.resources.sos_status_sending
import org.meshtastic.core.resources.sos_stop_when_delivered
import org.meshtastic.core.resources.sos_title
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.feature.settings.component.ExpressiveSection

private val REPEAT_MINUTES = listOf(0, 1, 2, 5, 10, 15, 30)
private val REPEAT_COUNTS = listOf(2, 3, 5, 10, 20, 50)
private val HOLD_SECONDS = listOf(0, 1, 2, 3, 5)
private const val MILLIS = 1000
private const val BUTTON_SIZE = 160
private const val RING_SIZE = 184

/**
 * The SOS mode: a big button that, held for the set time, sends an emergency message with the position to the chosen
 * channel and shows whether it was delivered. Everything about it is set on the same screen.
 */
@Composable
fun SosScreen(onBack: () -> Unit) {
    val prefs = koinInject<SosPrefs>()
    val controller = koinInject<SosController>()
    val radioConfig = koinInject<RadioConfigRepository>()
    val config by prefs.config.collectAsStateWithLifecycle()
    val state by controller.state.collectAsStateWithLifecycle()
    val channelSet by radioConfig.channelSetFlow.collectAsStateWithLifecycle(null)
    val channelNames = channelSet?.settings?.map { it.name }.orEmpty()

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.sos_title),
                canNavigateUp = true,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = {},
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(Res.string.sos_intro), style = MaterialTheme.typography.bodyMedium)

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SosButton(
                    holdSeconds = config.holdSeconds,
                    enabled = !state.running,
                    onTrigger = { controller.start(config) },
                )
            }
            SosStatus(state = state, config = config, onCancel = { controller.cancel() })

            ExpressiveSection(title = stringResource(Res.string.sos_section_channel)) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val indices = if (channelNames.isEmpty()) listOf(0) else channelNames.indices.toList()
                    for (index in indices) {
                        val name = channelNames.getOrNull(index).orEmpty()
                        FilterChip(
                            selected = config.channel == index,
                            onClick = { prefs.setConfig(config.copy(channel = index)) },
                            label = { Text(name.ifBlank { stringResource(Res.string.sos_channel_n, index) }) },
                        )
                    }
                }
            }

            ExpressiveSection(title = stringResource(Res.string.sos_section_message)) {
                var text by remember(config.message) { mutableStateOf(config.message) }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        prefs.setConfig(config.copy(message = it))
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    label = { Text(stringResource(Res.string.sos_message_label)) },
                    minLines = 2,
                    shape = MaterialTheme.shapes.large,
                )
                SwitchListItem(
                    checked = config.includePosition,
                    text = stringResource(Res.string.sos_include_position),
                    onClick = { prefs.setConfig(config.copy(includePosition = !config.includePosition)) },
                )
            }

            ExpressiveSection(title = stringResource(Res.string.sos_section_repeat)) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (minutes in REPEAT_MINUTES) {
                        FilterChip(
                            selected = config.repeatMinutes == minutes,
                            onClick = { prefs.setConfig(config.copy(repeatMinutes = minutes)) },
                            label = {
                                Text(
                                    if (minutes == 0) {
                                        stringResource(Res.string.sos_repeat_once)
                                    } else {
                                        stringResource(Res.string.sos_repeat_every, minutes)
                                    },
                                )
                            },
                        )
                    }
                }
                if (config.repeatMinutes > 0) {
                    Text(
                        text = stringResource(Res.string.sos_repeat_count),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (count in REPEAT_COUNTS) {
                            FilterChip(
                                selected = config.repeatCount == count,
                                onClick = { prefs.setConfig(config.copy(repeatCount = count)) },
                                label = { Text("$count") },
                            )
                        }
                    }
                    SwitchListItem(
                        checked = config.stopWhenDelivered,
                        text = stringResource(Res.string.sos_stop_when_delivered),
                        onClick = { prefs.setConfig(config.copy(stopWhenDelivered = !config.stopWhenDelivered)) },
                    )
                }
            }

            ExpressiveSection(title = stringResource(Res.string.sos_section_hold)) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (seconds in HOLD_SECONDS) {
                        FilterChip(
                            selected = config.holdSeconds == seconds,
                            onClick = { prefs.setConfig(config.copy(holdSeconds = seconds)) },
                            label = {
                                Text(
                                    if (seconds == 0) {
                                        stringResource(Res.string.sos_hold_tap)
                                    } else {
                                        stringResource(Res.string.sos_hold_seconds, seconds)
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/** The round button; with a hold time a ring fills while it is held and the send starts when the ring is full. */
@Composable
private fun SosButton(holdSeconds: Int, enabled: Boolean, onTrigger: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val currentTrigger by rememberUpdatedState(onTrigger)
    LaunchedEffect(pressed) {
        if (pressed && holdSeconds > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = holdSeconds * MILLIS, easing = LinearEasing))
            currentTrigger()
            pressed = false
        } else {
            progress.snapTo(0f)
        }
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(RING_SIZE.dp)) {
        CircularProgressIndicator(progress = { progress.value }, modifier = Modifier.size(RING_SIZE.dp), strokeWidth = 8.dp)
        Box(
            contentAlignment = Alignment.Center,
            modifier =
            Modifier.size(BUTTON_SIZE.dp)
                .clip(CircleShape)
                .background(if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                .pointerInput(enabled, holdSeconds) {
                    detectTapGestures(
                        onPress = {
                            if (enabled) {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                            }
                        },
                        onTap = { if (enabled && holdSeconds == 0) currentTrigger() },
                    )
                },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.sos_button),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                )
                Text(
                    text =
                    stringResource(if (holdSeconds > 0) Res.string.sos_button_hold else Res.string.sos_button_tap),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}

/** What the last run did: progress while sending, then the result. Nothing before the first run. */
@Composable
private fun SosStatus(state: SosState, config: SosConfig, onCancel: () -> Unit) {
    if (state.attempt == 0 && !state.refused) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when {
            state.refused ->
                Text(stringResource(Res.string.sos_result_refused), color = MaterialTheme.colorScheme.error)
            state.running -> {
                Text(
                    text = stringResource(Res.string.sos_status_sending, state.attempt, state.total),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(text = statusText(state.status), style = MaterialTheme.typography.bodyMedium)
            }
            else ->
                Text(
                    text =
                    stringResource(
                        if (state.delivered) Res.string.sos_result_delivered else Res.string.sos_result_unconfirmed,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
        }
        if (config.includePosition && state.attempt > 0) {
            Text(
                text =
                stringResource(if (state.withPosition) Res.string.sos_position_attached else Res.string.sos_position_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.running) Button(onClick = onCancel) { Text(stringResource(Res.string.sos_cancel)) }
    }
}

@Composable
private fun statusText(status: MessageStatus?): String = when (status) {
    MessageStatus.DELIVERED,
    MessageStatus.SFPP_ROUTING,
    MessageStatus.SFPP_CONFIRMED,
    -> stringResource(Res.string.sos_status_delivered)
    MessageStatus.ENROUTE -> stringResource(Res.string.sos_status_enroute)
    MessageStatus.ERROR -> stringResource(Res.string.sos_status_error)
    else -> stringResource(Res.string.sos_status_queued)
}
