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
@file:Suppress("LongMethod", "ModifierMissing", "TooManyFunctions", "CyclomaticComplexMethod", "MultipleEmitters")

package org.meshtastic.feature.settings.mtsw

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.navigation.DEEP_LINK_BASE_URI
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.SosAck
import org.meshtastic.core.repository.SosAckEntry
import org.meshtastic.core.repository.SosAckExport
import org.meshtastic.core.repository.SosAcks
import org.meshtastic.core.repository.SosAlarmCenter
import org.meshtastic.core.repository.SosAlarmStyle
import org.meshtastic.core.repository.SosAlert
import org.meshtastic.core.repository.SosBarScreens
import org.meshtastic.core.repository.SosConfig
import org.meshtastic.core.repository.SosController
import org.meshtastic.core.repository.SosHistory
import org.meshtastic.core.repository.SosHistoryEntry
import org.meshtastic.core.repository.SosHistoryExport
import org.meshtastic.core.repository.SosPrefs
import org.meshtastic.core.repository.SosQuietHours
import org.meshtastic.core.repository.SosState
import org.meshtastic.core.repository.SosTargets
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.mt_sw_other_sos_body
import org.meshtastic.core.resources.sos_ack_coming
import org.meshtastic.core.resources.sos_ack_help
import org.meshtastic.core.resources.sos_ack_seen
import org.meshtastic.core.resources.sos_acks_button
import org.meshtastic.core.resources.sos_acks_button_of
import org.meshtastic.core.resources.sos_acks_empty
import org.meshtastic.core.resources.sos_acks_export
import org.meshtastic.core.resources.sos_acks_title
import org.meshtastic.core.resources.sos_alarm_flash_switch
import org.meshtastic.core.resources.sos_alarm_map
import org.meshtastic.core.resources.sos_alarm_nodes_title
import org.meshtastic.core.resources.sos_alarm_only_chosen_note
import org.meshtastic.core.resources.sos_alarm_only_chosen_switch
import org.meshtastic.core.resources.sos_alarm_open
import org.meshtastic.core.resources.sos_alarm_test
import org.meshtastic.core.resources.sos_alarm_test_sender
import org.meshtastic.core.resources.sos_alarm_test_signal
import org.meshtastic.core.resources.sos_alarm_test_text
import org.meshtastic.core.resources.sos_bar_ai
import org.meshtastic.core.resources.sos_bar_connections
import org.meshtastic.core.resources.sos_bar_health
import org.meshtastic.core.resources.sos_bar_hold_note
import org.meshtastic.core.resources.sos_bar_map
import org.meshtastic.core.resources.sos_bar_messages
import org.meshtastic.core.resources.sos_bar_nodes
import org.meshtastic.core.resources.sos_bar_note
import org.meshtastic.core.resources.sos_bar_other
import org.meshtastic.core.resources.sos_bar_settings
import org.meshtastic.core.resources.sos_bar_show
import org.meshtastic.core.resources.sos_button_hold
import org.meshtastic.core.resources.sos_button_tap
import org.meshtastic.core.resources.sos_cancel
import org.meshtastic.core.resources.sos_channel_n
import org.meshtastic.core.resources.sos_channels_note
import org.meshtastic.core.resources.sos_confirm_switch
import org.meshtastic.core.resources.sos_history_clear
import org.meshtastic.core.resources.sos_history_empty
import org.meshtastic.core.resources.sos_history_export
import org.meshtastic.core.resources.sos_history_keep_days
import org.meshtastic.core.resources.sos_history_keep_forever
import org.meshtastic.core.resources.sos_history_keep_title
import org.meshtastic.core.resources.sos_history_no_position
import org.meshtastic.core.resources.sos_history_note
import org.meshtastic.core.resources.sos_history_quiet
import org.meshtastic.core.resources.sos_history_with_position
import org.meshtastic.core.resources.sos_hold_seconds
import org.meshtastic.core.resources.sos_hold_tap
import org.meshtastic.core.resources.sos_include_position
import org.meshtastic.core.resources.sos_intro
import org.meshtastic.core.resources.sos_message_for
import org.meshtastic.core.resources.sos_message_label
import org.meshtastic.core.resources.sos_no_targets
import org.meshtastic.core.resources.sos_nodes_add
import org.meshtastic.core.resources.sos_nodes_done
import org.meshtastic.core.resources.sos_nodes_empty
import org.meshtastic.core.resources.sos_nodes_missing
import org.meshtastic.core.resources.sos_nodes_missing_remove
import org.meshtastic.core.resources.sos_nodes_none
import org.meshtastic.core.resources.sos_nodes_note
import org.meshtastic.core.resources.sos_nodes_pick_title
import org.meshtastic.core.resources.sos_nodes_search
import org.meshtastic.core.resources.sos_notify_on_answer
import org.meshtastic.core.resources.sos_position_attached
import org.meshtastic.core.resources.sos_position_missing
import org.meshtastic.core.resources.sos_quiet_from
import org.meshtastic.core.resources.sos_quiet_nodes_title
import org.meshtastic.core.resources.sos_quiet_note
import org.meshtastic.core.resources.sos_quiet_switch
import org.meshtastic.core.resources.sos_quiet_to
import org.meshtastic.core.resources.sos_receive_note
import org.meshtastic.core.resources.sos_receive_switch
import org.meshtastic.core.resources.sos_repeat_count
import org.meshtastic.core.resources.sos_repeat_every
import org.meshtastic.core.resources.sos_repeat_gate_minutes
import org.meshtastic.core.resources.sos_repeat_gate_note
import org.meshtastic.core.resources.sos_repeat_gate_switch
import org.meshtastic.core.resources.sos_repeat_once
import org.meshtastic.core.resources.sos_repeat_unlimited
import org.meshtastic.core.resources.sos_repeat_unlimited_note
import org.meshtastic.core.resources.sos_resend
import org.meshtastic.core.resources.sos_result_delivered
import org.meshtastic.core.resources.sos_result_refused
import org.meshtastic.core.resources.sos_result_unconfirmed
import org.meshtastic.core.resources.sos_section_bar
import org.meshtastic.core.resources.sos_section_channel
import org.meshtastic.core.resources.sos_section_history
import org.meshtastic.core.resources.sos_section_hold
import org.meshtastic.core.resources.sos_section_message
import org.meshtastic.core.resources.sos_section_nodes
import org.meshtastic.core.resources.sos_section_receive
import org.meshtastic.core.resources.sos_section_repeat
import org.meshtastic.core.resources.sos_section_widget
import org.meshtastic.core.resources.sos_status_answered
import org.meshtastic.core.resources.sos_status_delivered
import org.meshtastic.core.resources.sos_status_enroute
import org.meshtastic.core.resources.sos_status_error
import org.meshtastic.core.resources.sos_status_queued
import org.meshtastic.core.resources.sos_status_sending
import org.meshtastic.core.resources.sos_status_sending_unlimited
import org.meshtastic.core.resources.sos_stop_on_answer
import org.meshtastic.core.resources.sos_stop_when_delivered
import org.meshtastic.core.resources.sos_style_bell
import org.meshtastic.core.resources.sos_style_note
import org.meshtastic.core.resources.sos_style_silent
import org.meshtastic.core.resources.sos_style_siren
import org.meshtastic.core.resources.sos_style_title
import org.meshtastic.core.resources.sos_targets_note
import org.meshtastic.core.resources.sos_title
import org.meshtastic.core.resources.sos_widget_taps_note
import org.meshtastic.core.ui.assistant.AssistantLinks
import org.meshtastic.core.ui.component.AlertDialog
import org.meshtastic.core.ui.component.Button
import org.meshtastic.core.ui.component.Checkbox
import org.meshtastic.core.ui.component.FilterChip
import org.meshtastic.core.ui.component.FloatingBarUnderlap
import org.meshtastic.core.ui.component.InfoButton
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.OutlinedButton
import org.meshtastic.core.ui.component.SosChip
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.core.ui.component.floatingBarEndPadding
import org.meshtastic.core.ui.util.rememberFileExporter
import org.meshtastic.feature.settings.component.ExpressiveSection

private val REPEAT_MINUTES = listOf(0, 1, 2, 5, 10, 15, 30)
private val REPEAT_COUNTS = listOf(2, 3, 5, 10, 20, 50)
private val HOLD_SECONDS = listOf(0, 1, 2, 3, 5)

/** The numbers of quick widget taps to choose from; none is below [SosConfig.WIDGET_MIN_TAPS]. */
private val WIDGET_TAP_CHOICES = listOf(3, 4, 5, 6, 8, 10)
private val GATE_MINUTES = listOf(2, 5, 10, 15, 30)
private val KEEP_DAYS = listOf(0, 7, 30, 90, 365)
private const val QUIET_STEP_MINUTES = 30
private const val MINUTES_PER_HOUR = 60

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
    // Nothing chosen at all: there is nowhere to send. Channels alone, or only nodes (private persons), are fine.
    val noTargets = config.channels.isEmpty() && config.nodes.isEmpty()
    val nodeRepository = koinInject<NodeRepository>()
    val nodeDb by nodeRepository.nodeDBbyNum.collectAsStateWithLifecycle()
    val myInfo by nodeRepository.myNodeInfo.collectAsStateWithLifecycle()
    var pickingNodes by remember { mutableStateOf(false) }
    var pickingAlarmNodes by remember { mutableStateOf(false) }
    var pickingQuietNodes by remember { mutableStateOf(false) }
    val alarmNodes by prefs.alarmNodes.collectAsStateWithLifecycle()
    val alarmStyles by prefs.alarmStyles.collectAsStateWithLifecycle()
    val quietExceptions by prefs.quietExceptions.collectAsStateWithLifecycle()

    if (pickingQuietNodes) {
        SosNodePicker(
            nodes = nodeDb.values.filter { it.num != myInfo?.myNodeNum },
            selected = quietExceptions.toList(),
            onToggle = { num ->
                prefs.setQuietExceptions(if (num in quietExceptions) quietExceptions - num else quietExceptions + num)
            },
            onDismiss = { pickingQuietNodes = false },
        )
    }

    if (pickingAlarmNodes) {
        SosNodePicker(
            nodes = nodeDb.values.filter { it.num != myInfo?.myNodeNum },
            selected = alarmNodes.toList(),
            onToggle = { num ->
                if (num in alarmNodes) {
                    prefs.setAlarmNodes(alarmNodes - num)
                    prefs.setAlarmStyles(alarmStyles - num)
                } else {
                    prefs.setAlarmNodes(alarmNodes + num)
                }
            },
            onDismiss = { pickingAlarmNodes = false },
        )
    }

    if (pickingNodes) {
        SosNodePicker(
            nodes = nodeDb.values.filter { it.num != myInfo?.myNodeNum },
            selected = config.nodes,
            onToggle = { num ->
                val chosen = if (num in config.nodes) config.nodes - num else config.nodes + num
                prefs.setConfig(config.copy(nodes = chosen))
            },
            onDismiss = { pickingNodes = false },
        )
    }

    FloatingBarUnderlap()

    Scaffold(
        topBar = {
            MainAppBar(
                showSos = false,
                title = stringResource(Res.string.sos_title),
                canNavigateUp = true,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = { InfoButton(Res.string.sos_title, Res.string.mt_sw_other_sos_body) },
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            Modifier.padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .floatingBarEndPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(Res.string.sos_intro), style = MaterialTheme.typography.bodyMedium)

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // The same red button as in the top bars (big): hold it and the white line traces the border.
                SosChip(
                    onClick = { if (config.holdSeconds == 0) controller.start(config) },
                    holdSeconds = config.holdSeconds,
                    onHoldComplete = if (config.holdSeconds > 0) ({ controller.start(config) }) else null,
                    large = true,
                    enabled = !state.running && !noTargets,
                    hint =
                    stringResource(
                        if (config.holdSeconds > 0) Res.string.sos_button_hold else Res.string.sos_button_tap,
                    ),
                )
                if (noTargets) {
                    Text(
                        text = stringResource(Res.string.sos_no_targets),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            val sosAcks = koinInject<SosAcks>()
            val ackEntries by sosAcks.entries.collectAsStateWithLifecycle()
            SosStatus(
                state = state,
                config = config,
                acks = ackEntries,
                onCancel = { controller.cancel() },
                onResend = { controller.start(config) },
            )

            ExpressiveSection(title = stringResource(Res.string.sos_section_channel)) {
                Text(
                    text = stringResource(Res.string.sos_channels_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val indices = if (channelNames.isEmpty()) listOf(0) else channelNames.indices.toList()
                    for (index in indices) {
                        val name = channelNames.getOrNull(index).orEmpty()
                        FilterChip(
                            selected = index in config.channels,
                            onClick = {
                                val on = index in config.channels
                                val chosen = if (on) config.channels - index else config.channels + index
                                prefs.setConfig(config.copy(channels = chosen.sorted()))
                            },
                            label = { Text(name.ifBlank { stringResource(Res.string.sos_channel_n, index) }) },
                        )
                    }
                }
            }

            ExpressiveSection(title = stringResource(Res.string.sos_section_nodes)) {
                Text(
                    text = stringResource(Res.string.sos_nodes_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (config.nodes.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.sos_nodes_none),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (num in config.nodes) {
                            FilterChip(
                                selected = true,
                                onClick = { prefs.setConfig(config.copy(nodes = config.nodes - num)) },
                                label = { Text(sosNodeName(nodeDb[num], num) + "  ✕") },
                            )
                        }
                    }
                    // A chosen node that is gone from the node list (cleaned away, or never heard again): the alarm is
                    // still sent to it, but the person should know. Only judged once the list has been loaded.
                    val missing = if (nodeDb.isEmpty()) emptyList() else config.nodes.filter { it !in nodeDb }
                    if (missing.isNotEmpty()) {
                        Text(
                            text =
                            stringResource(
                                Res.string.sos_nodes_missing,
                                missing.joinToString { NodeAddress.numToDefaultId(it) },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        TextButton(
                            onClick = { prefs.setConfig(config.copy(nodes = config.nodes - missing.toSet())) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Text(stringResource(Res.string.sos_nodes_missing_remove))
                        }
                    }
                }
                Button(onClick = { pickingNodes = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(stringResource(Res.string.sos_nodes_add))
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
                val targets =
                    config.channels.map { index ->
                        val name = channelNames.getOrNull(index).orEmpty()
                        SosTargets.channelKey(index) to name.ifBlank { stringResource(Res.string.sos_channel_n, index) }
                    } + config.nodes.map { num -> SosTargets.nodeKey(num) to sosNodeName(nodeDb[num], num) }
                if (targets.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.sos_targets_note),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                for ((key, label) in targets) {
                    var own by
                        remember(key, config.targetMessages[key]) {
                            mutableStateOf(config.targetMessages[key].orEmpty())
                        }
                    OutlinedTextField(
                        value = own,
                        onValueChange = {
                            own = it
                            prefs.setConfig(config.copy(targetMessages = config.targetMessages + (key to it)))
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        label = { Text(stringResource(Res.string.sos_message_for, label)) },
                        shape = MaterialTheme.shapes.large,
                    )
                }
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
                        FilterChip(
                            selected = config.repeatCount == SosConfig.UNLIMITED_REPEATS,
                            onClick = { prefs.setConfig(config.copy(repeatCount = SosConfig.UNLIMITED_REPEATS)) },
                            label = { Text(stringResource(Res.string.sos_repeat_unlimited)) },
                        )
                    }
                    if (config.repeatCount == SosConfig.UNLIMITED_REPEATS) {
                        Text(
                            text = stringResource(Res.string.sos_repeat_unlimited_note),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    SwitchListItem(
                        checked = config.stopWhenDelivered,
                        text = stringResource(Res.string.sos_stop_when_delivered),
                        onClick = { prefs.setConfig(config.copy(stopWhenDelivered = !config.stopWhenDelivered)) },
                    )
                    val stopOnAnswer by prefs.stopOnAnswer.collectAsStateWithLifecycle()
                    SwitchListItem(
                        checked = stopOnAnswer,
                        text = stringResource(Res.string.sos_stop_on_answer),
                        onClick = { prefs.setStopOnAnswer(!stopOnAnswer) },
                    )
                    val notifyOnAnswer by prefs.notifyOnAnswer.collectAsStateWithLifecycle()
                    SwitchListItem(
                        checked = notifyOnAnswer,
                        text = stringResource(Res.string.sos_notify_on_answer),
                        onClick = { prefs.setNotifyOnAnswer(!notifyOnAnswer) },
                    )
                }
            }

            val receiveAlarm by prefs.receiveAlarm.collectAsStateWithLifecycle()
            val alarmCenter = koinInject<SosAlarmCenter>()
            val testSender = stringResource(Res.string.sos_alarm_test_sender)
            val testText = stringResource(Res.string.sos_alarm_test_text)
            ExpressiveSection(title = stringResource(Res.string.sos_section_receive)) {
                Text(
                    text = stringResource(Res.string.sos_receive_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SwitchListItem(
                    checked = receiveAlarm,
                    text = stringResource(Res.string.sos_receive_switch),
                    onClick = { prefs.setReceiveAlarm(!receiveAlarm) },
                )
                val alarmFlash by prefs.alarmFlash.collectAsStateWithLifecycle()
                SwitchListItem(
                    checked = alarmFlash,
                    text = stringResource(Res.string.sos_alarm_flash_switch),
                    onClick = { prefs.setAlarmFlash(!alarmFlash) },
                )
                val confirmToSender by prefs.confirmToSender.collectAsStateWithLifecycle()
                SwitchListItem(
                    checked = confirmToSender,
                    text = stringResource(Res.string.sos_confirm_switch),
                    onClick = { prefs.setConfirmToSender(!confirmToSender) },
                )
                val onlyChosen by prefs.alarmOnlyChosen.collectAsStateWithLifecycle()
                SwitchListItem(
                    checked = onlyChosen,
                    text = stringResource(Res.string.sos_alarm_only_chosen_switch),
                    onClick = { prefs.setAlarmOnlyChosen(!onlyChosen) },
                )
                Text(
                    text = stringResource(Res.string.sos_alarm_only_chosen_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                val repeatGate by prefs.repeatGate.collectAsStateWithLifecycle()
                val gateMinutes by prefs.repeatGateMinutes.collectAsStateWithLifecycle()
                SwitchListItem(
                    checked = repeatGate,
                    text = stringResource(Res.string.sos_repeat_gate_switch),
                    onClick = { prefs.setRepeatGate(!repeatGate) },
                )
                Text(
                    text = stringResource(Res.string.sos_repeat_gate_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (repeatGate) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (minutes in GATE_MINUTES) {
                            FilterChip(
                                selected = gateMinutes == minutes,
                                onClick = { prefs.setRepeatGateMinutes(minutes) },
                                label = { Text(stringResource(Res.string.sos_repeat_gate_minutes, minutes)) },
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(Res.string.sos_alarm_nodes_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (alarmNodes.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.sos_nodes_none),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.sos_style_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.sos_style_note),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (num in alarmNodes) {
                            FilterChip(
                                selected = true,
                                onClick = {
                                    prefs.setAlarmNodes(alarmNodes - num)
                                    prefs.setAlarmStyles(alarmStyles - num)
                                },
                                label = { Text(sosNodeName(nodeDb[num], num) + "  ✕") },
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (style in SosAlarmStyle.entries) {
                                    FilterChip(
                                        selected = (alarmStyles[num] ?: SosAlarmStyle.SIREN) == style,
                                        onClick = { prefs.setAlarmStyles(alarmStyles + (num to style)) },
                                        label = { Text(stringResource(sosStyleLabel(style))) },
                                    )
                                }
                            }
                        }
                    }
                }
                Button(onClick = { pickingAlarmNodes = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(stringResource(Res.string.sos_nodes_add))
                }
                val quiet by prefs.quietHours.collectAsStateWithLifecycle()
                SosQuietHoursEditor(
                    hours = quiet,
                    onChange = { prefs.setQuietHours(it) },
                    exceptions = quietExceptions.map { sosNodeName(nodeDb[it], it) to it },
                    onRemoveException = { prefs.setQuietExceptions(quietExceptions - it) },
                    onPickExceptions = { pickingQuietNodes = true },
                )
                // One plain button tries the alarm as it is for an SOS from any node; the three below try a signal.
                Button(
                    onClick = {
                        alarmCenter.raise(
                            SosAlert(
                                id = nowMillis,
                                contactKey = ContactKey.broadcast(0).value,
                                senderName = testSender,
                                text = testText,
                                test = true,
                            ),
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(Res.string.sos_alarm_test))
                }
                Text(
                    text = stringResource(Res.string.sos_alarm_test_signal),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (style in SosAlarmStyle.entries) {
                        Button(
                            onClick = {
                                alarmCenter.raise(
                                    SosAlert(
                                        id = nowMillis,
                                        contactKey = ContactKey.broadcast(0).value,
                                        senderName = testSender,
                                        text = testText,
                                        test = true,
                                        style = style,
                                    ),
                                )
                            },
                        ) {
                            Text(stringResource(sosStyleLabel(style)))
                        }
                    }
                }
            }

            val sosHistory = koinInject<SosHistory>()
            val historyEntries by sosHistory.entries.collectAsStateWithLifecycle()
            ExpressiveSection(title = stringResource(Res.string.sos_section_history)) {
                val exportHistory =
                    rememberFileExporter(content = { SosHistoryExport.csv(historyEntries).encodeToByteArray() })
                val keepDays by sosHistory.keepDays.collectAsStateWithLifecycle()
                Text(
                    text = stringResource(Res.string.sos_history_keep_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (days in KEEP_DAYS) {
                        FilterChip(
                            selected = keepDays == days,
                            onClick = { sosHistory.setKeepDays(days) },
                            label = {
                                Text(
                                    if (days == 0) {
                                        stringResource(Res.string.sos_history_keep_forever)
                                    } else {
                                        stringResource(Res.string.sos_history_keep_days, days)
                                    },
                                )
                            },
                        )
                    }
                }
                SosHistoryList(
                    entries = historyEntries,
                    onClear = { sosHistory.clear() },
                    onExport = { exportHistory("sos_history.csv", "text/csv") },
                )
            }

            val barScreens by prefs.barScreens.collectAsStateWithLifecycle()
            ExpressiveSection(title = stringResource(Res.string.sos_section_bar)) {
                Text(
                    text = stringResource(Res.string.sos_bar_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SwitchListItem(
                    checked = barScreens.isNotEmpty(),
                    text = stringResource(Res.string.sos_bar_show),
                    onClick = {
                        prefs.setBarScreens(if (barScreens.isEmpty()) SosBarScreens.ALL.toSet() else emptySet())
                    },
                )
                if (barScreens.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (screen in SosBarScreens.ALL) {
                            FilterChip(
                                selected = screen in barScreens,
                                onClick = {
                                    // The last card cannot be switched off here; the switch above turns the button off.
                                    val chosen = if (screen in barScreens) barScreens - screen else barScreens + screen
                                    if (chosen.isNotEmpty()) prefs.setBarScreens(chosen)
                                },
                                label = { Text(stringResource(sosBarScreenLabel(screen))) },
                            )
                        }
                    }
                }
            }

            ExpressiveSection(title = stringResource(Res.string.sos_section_widget)) {
                val widgetTaps by prefs.widgetTaps.collectAsStateWithLifecycle()
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (taps in WIDGET_TAP_CHOICES) {
                        FilterChip(
                            selected = widgetTaps == taps,
                            onClick = { prefs.setWidgetTaps(taps) },
                            label = { Text("$taps×") },
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.sos_widget_taps_note, SosConfig.WIDGET_MIN_TAPS),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
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
                Text(
                    text = stringResource(Res.string.sos_bar_hold_note, SosConfig.BAR_MIN_HOLD_SECONDS),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** The quiet hours: a switch and, when it is on, the start and the end, each moved by half an hour with − and +. */
@Composable
private fun SosQuietHoursEditor(
    hours: SosQuietHours,
    onChange: (SosQuietHours) -> Unit,
    exceptions: List<Pair<String, Int>>,
    onRemoveException: (Int) -> Unit,
    onPickExceptions: () -> Unit,
) {
    SwitchListItem(
        checked = hours.enabled,
        text = stringResource(Res.string.sos_quiet_switch),
        onClick = { onChange(hours.copy(enabled = !hours.enabled)) },
    )
    if (!hours.enabled) return
    Text(
        text = stringResource(Res.string.sos_quiet_note),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    QuietTimeRow(
        label = stringResource(Res.string.sos_quiet_from, clockText(hours.fromMinutes)),
        onStep = { onChange(hours.copy(fromMinutes = stepMinutes(hours.fromMinutes, it))) },
    )
    QuietTimeRow(
        label = stringResource(Res.string.sos_quiet_to, clockText(hours.toMinutes)),
        onStep = { onChange(hours.copy(toMinutes = stepMinutes(hours.toMinutes, it))) },
    )
    Text(
        text = stringResource(Res.string.sos_quiet_nodes_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    if (exceptions.isEmpty()) {
        Text(
            text = stringResource(Res.string.sos_nodes_none),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    } else {
        FlowRow(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((name, num) in exceptions) {
                FilterChip(selected = true, onClick = { onRemoveException(num) }, label = { Text("$name  ✕") })
            }
        }
    }
    Button(onClick = onPickExceptions, modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(Res.string.sos_nodes_add))
    }
}

@Composable
private fun QuietTimeRow(label: String, onStep: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onStep(-QUIET_STEP_MINUTES) }) { Text("−") }
        OutlinedButton(onClick = { onStep(QUIET_STEP_MINUTES) }, modifier = Modifier.padding(start = 8.dp)) {
            Text("+")
        }
    }
}

private fun clockText(minutes: Int): String {
    val hour = (minutes / MINUTES_PER_HOUR).toString().padStart(2, '0')
    val minute = (minutes % MINUTES_PER_HOUR).toString().padStart(2, '0')
    return "$hour:$minute"
}

/** [value] moved by [delta] minutes round the clock. */
private fun stepMinutes(value: Int, delta: Int): Int =
    ((value + delta) % SosQuietHours.DAY_MINUTES + SosQuietHours.DAY_MINUTES) % SosQuietHours.DAY_MINUTES

/** The received SOS, newest first: who, when, what, and shortcuts to the conversation and to the sender on the map. */
@Composable
private fun SosHistoryList(entries: List<SosHistoryEntry>, onClear: () -> Unit, onExport: () -> Unit) {
    Text(
        text = stringResource(Res.string.sos_history_note),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    if (entries.isEmpty()) {
        Text(
            text = stringResource(Res.string.sos_history_empty),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        return
    }
    for (entry in entries) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = entry.senderName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = DateFormatter.formatDateTime(entry.time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.text.isNotBlank()) Text(text = entry.text, style = MaterialTheme.typography.bodyMedium)
            if (!entry.alarmed) {
                Text(
                    text = stringResource(Res.string.sos_history_quiet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val hasPosition = entry.latitude != null && entry.longitude != null
            Text(
                text =
                stringResource(
                    if (hasPosition) Res.string.sos_history_with_position else Res.string.sos_history_no_position,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                TextButton(onClick = { AssistantLinks.open("$DEEP_LINK_BASE_URI/messages/${entry.contactKey}") }) {
                    Text(stringResource(Res.string.sos_alarm_open))
                }
                val sender = entry.senderNum
                if (hasPosition && sender != null) {
                    TextButton(onClick = { AssistantLinks.open("$DEEP_LINK_BASE_URI/map/focus/$sender") }) {
                        Text(stringResource(Res.string.sos_alarm_map))
                    }
                }
            }
        }
    }
    Row(modifier = Modifier.padding(horizontal = 8.dp)) {
        TextButton(onClick = onExport) { Text(stringResource(Res.string.sos_history_export)) }
        TextButton(onClick = onClear) { Text(stringResource(Res.string.sos_history_clear)) }
    }
}

private fun sosStyleLabel(style: SosAlarmStyle): StringResource = when (style) {
    SosAlarmStyle.SIREN -> Res.string.sos_style_siren
    SosAlarmStyle.BELL -> Res.string.sos_style_bell
    SosAlarmStyle.SILENT -> Res.string.sos_style_silent
}

private fun sosBarScreenLabel(key: String): StringResource = when (key) {
    SosBarScreens.CONNECTIONS -> Res.string.sos_bar_connections
    SosBarScreens.MESSAGES -> Res.string.sos_bar_messages
    SosBarScreens.NODES -> Res.string.sos_bar_nodes
    SosBarScreens.MAP -> Res.string.sos_bar_map
    SosBarScreens.SETTINGS -> Res.string.sos_bar_settings
    SosBarScreens.AI -> Res.string.sos_bar_ai
    SosBarScreens.HEALTH -> Res.string.sos_bar_health
    else -> Res.string.sos_bar_other
}

/** The name of a node for the list of SOS recipients: its long name, else the short one, else its id. */
private fun sosNodeName(node: Node?, num: Int): String {
    val long = node?.user?.long_name.orEmpty()
    val short = node?.user?.short_name.orEmpty()
    return when {
        long.isNotBlank() && short.isNotBlank() -> "$long ($short)"
        long.isNotBlank() -> long
        short.isNotBlank() -> short
        else -> NodeAddress.numToDefaultId(num)
    }
}

/**
 * The window to pick the nodes that get the SOS as a direct message. Every node the app knows is listed, the ones that
 * are marked as not monitored too: nothing is left out.
 */
@Composable
private fun SosNodePicker(nodes: List<Node>, selected: List<Int>, onToggle: (Int) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown =
        remember(nodes, query) {
            nodes
                .filter { node -> sosNodeName(node, node.num).contains(query.trim(), ignoreCase = true) }
                .sortedBy { sosNodeName(it, it.num).lowercase() }
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sos_nodes_pick_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.sos_nodes_search)) },
                    singleLine = true,
                )
                if (shown.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.sos_nodes_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(shown, key = { it.num }) { node ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onToggle(node.num) },
                        ) {
                            Checkbox(checked = node.num in selected, onCheckedChange = { onToggle(node.num) })
                            Text(text = sosNodeName(node, node.num), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sos_nodes_done)) } },
    )
}

/** What the last run did: progress while sending, then the result. Nothing before the first run. */
@Composable
private fun SosStatus(
    state: SosState,
    config: SosConfig,
    acks: List<SosAckEntry>,
    onCancel: () -> Unit,
    onResend: () -> Unit,
) {
    if (state.attempt == 0 && !state.refused) return
    var showAcks by remember { mutableStateOf(false) }
    val exportAcks = rememberFileExporter(content = { SosAckExport.csv(acks).encodeToByteArray() })
    if (showAcks) {
        SosAcksDialog(
            acks = acks,
            onExport = { exportAcks("sos_confirmations.csv", "text/csv") },
            onDismiss = { showAcks = false },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when {
            state.refused ->
                Text(stringResource(Res.string.sos_result_refused), color = MaterialTheme.colorScheme.error)

            state.running -> {
                Text(
                    text =
                    if (state.total == 0) {
                        stringResource(Res.string.sos_status_sending_unlimited, state.attempt)
                    } else {
                        stringResource(Res.string.sos_status_sending, state.attempt, state.total)
                    },
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
        if (state.answered) {
            Text(
                text = stringResource(Res.string.sos_status_answered),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (config.includePosition && state.attempt > 0) {
            Text(
                text =
                stringResource(
                    if (state.withPosition) Res.string.sos_position_attached else Res.string.sos_position_missing,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.attempt > 0) {
            // With chosen nodes the button counts how many of them answered; otherwise how many answers there are.
            val answeredChosen = acks.map { it.nodeNum }.distinct().count { it in config.nodes }
            OutlinedButton(onClick = { showAcks = true }) {
                Text(
                    if (config.nodes.isEmpty()) {
                        stringResource(Res.string.sos_acks_button, acks.size)
                    } else {
                        stringResource(Res.string.sos_acks_button_of, answeredChosen, config.nodes.size)
                    },
                )
            }
        }
        if (state.running) Button(onClick = onCancel) { Text(stringResource(Res.string.sos_cancel)) }
        // Nobody confirmed it (or nothing could be sent): one tap sends the whole alarm once more.
        if (!state.running && !state.delivered) {
            Button(onClick = onResend) { Text(stringResource(Res.string.sos_resend)) }
        }
    }
}

/** Who answered the SOS sent last (newest first): name, what they answered and when. Scrolls when long. */
@Composable
private fun SosAcksDialog(acks: List<SosAckEntry>, onExport: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sos_acks_title)) },
        text = {
            if (acks.isEmpty()) {
                Text(text = stringResource(Res.string.sos_acks_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(acks) { entry ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(text = entry.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text =
                                "${stringResource(sosAckLabel(entry.ack))} · " +
                                    DateFormatter.formatDateTime(entry.time),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } },
        dismissButton = {
            if (acks.isNotEmpty()) TextButton(onClick = onExport) { Text(stringResource(Res.string.sos_acks_export)) }
        },
    )
}

private fun sosAckLabel(ack: SosAck): StringResource = when (ack) {
    SosAck.SEEN -> Res.string.sos_ack_seen
    SosAck.COMING -> Res.string.sos_ack_coming
    SosAck.CALLING_HELP -> Res.string.sos_ack_help
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
