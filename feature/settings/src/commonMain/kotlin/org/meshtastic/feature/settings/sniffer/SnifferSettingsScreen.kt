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
package org.meshtastic.feature.settings.sniffer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.datastore.SnifferSource
import org.meshtastic.core.domain.usecase.settings.SnifferControlUseCase
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.mqtt_sniffer_warning_message
import org.meshtastic.core.resources.mqtt_sniffer_warning_title
import org.meshtastic.core.resources.mqtt_status_connected
import org.meshtastic.core.resources.mqtt_status_connecting
import org.meshtastic.core.resources.mqtt_status_disconnected_with_reason
import org.meshtastic.core.resources.mqtt_status_inactive
import org.meshtastic.core.resources.mqtt_status_reconnecting
import org.meshtastic.core.resources.mqtt_status_reconnecting_with_attempt
import org.meshtastic.core.resources.sniffer_dismiss_loaded_file
import org.meshtastic.core.resources.sniffer_enable_failed
import org.meshtastic.core.resources.sniffer_load_failed
import org.meshtastic.core.resources.sniffer_settings_title
import org.meshtastic.core.resources.sniffer_viewing_loaded_file
import org.meshtastic.core.resources.sniffer_warning_accept
import org.meshtastic.core.resources.sniffer_warning_cancel
import org.meshtastic.core.resources.sniffer_warning_compatibility
import org.meshtastic.core.resources.sniffer_warning_message
import org.meshtastic.core.resources.sniffer_warning_title
import org.meshtastic.core.ui.component.FastScrollSidebar
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.feature.settings.export.rememberLogExportSaver
import org.meshtastic.feature.settings.export.rememberLogImportPicker
import org.meshtastic.feature.settings.radio.RadioConfigViewModel
import org.meshtastic.feature.settings.sniffer.mqtt.GroupedMqttSniffedPacket
import org.meshtastic.feature.settings.sniffer.mqtt.MqttSniffedPacket
import org.meshtastic.feature.settings.sniffer.mqtt.MqttSnifferLogViewModel
import org.meshtastic.feature.settings.sniffer.mqtt.groupedByGateway
import org.meshtastic.mqtt.ConnectionState

/**
 * The Sniffer panel, reached directly from Settings -> Advanced -> Sniffer -- one combined log view for both the Radio
 * Sniffer (overhears local LoRa traffic via the connected node) and the MQTT Sniffer (watches traffic on the broker
 * this node is configured to use), replacing what used to be two separate settings cards and two separate log screens.
 * Only one of the two can be watching at a time -- see [SnifferPanelViewModel] -- selected from the gear menu alongside
 * grouping, auto-scroll, decryption and export-format preferences, and the ability to load a previously saved log back
 * in for viewing. The top bar carries three actions: save the currently displayed log, open that gear menu, and clear
 * the panel.
 */
@Composable
fun SnifferSettingsScreen(
    radioConfigViewModel: RadioConfigViewModel,
    radioLogViewModel: SnifferLogViewModel,
    mqttViewModel: MqttSnifferLogViewModel,
    panelViewModel: SnifferPanelViewModel,
    onBack: () -> Unit,
) {
    val state by radioConfigViewModel.radioConfigState.collectAsStateWithLifecycle()
    val radioPackets by radioLogViewModel.sniffedPackets.collectAsStateWithLifecycle()
    val mqttPackets by mqttViewModel.packets.collectAsStateWithLifecycle()
    val mqttSessionActive by mqttViewModel.active.collectAsStateWithLifecycle()
    val mqttConnectionState by mqttViewModel.connectionState.collectAsStateWithLifecycle()

    val activeSource by panelViewModel.activeSource.collectAsStateWithLifecycle()
    val displaySource by panelViewModel.displaySource.collectAsStateWithLifecycle()
    val freezeAtMillis by panelViewModel.freezeAtMillis.collectAsStateWithLifecycle()
    val groupByGateway by panelViewModel.groupByGateway.collectAsStateWithLifecycle()
    val autoScroll by panelViewModel.autoScroll.collectAsStateWithLifecycle()
    val decryptPayloads by panelViewModel.decryptPayloads.collectAsStateWithLifecycle()
    val exportFormat by panelViewModel.exportFormat.collectAsStateWithLifecycle()
    val mqttConfigured by panelViewModel.mqttConfigured.collectAsStateWithLifecycle()
    val clearedAtMillis by panelViewModel.clearedAtMillis.collectAsStateWithLifecycle()
    val loadedLog by panelViewModel.loadedLog.collectAsStateWithLifecycle()

    val snifferSupported =
        state.snifferEnabled != null ||
            (state.fwPlusVersion?.let { it >= SnifferControlUseCase.MIN_FW_PLUS_VERSION_FOR_SNIFFER } == true)
    val radioSelectable = state.connected && !state.snifferLoading && snifferSupported

    // Reconcile the persisted selection against live truth once each becomes known -- a fresh process (or a sniffer
    // left running from outside this screen, e.g. still-active from before a navigation-away-and-back) should show
    // what's actually happening rather than a stale preference. See SnifferPanelViewModel's doc for the rest of the
    // exclusivity contract.
    LaunchedEffect(state.snifferEnabled) {
        if (state.snifferEnabled == true && activeSource != SnifferSource.RADIO) {
            panelViewModel.selectSource(SnifferSource.RADIO)
        }
    }
    LaunchedEffect(mqttSessionActive) {
        if (mqttSessionActive && activeSource != SnifferSource.MQTT) panelViewModel.selectSource(SnifferSource.MQTT)
    }

    var pendingSource by remember { mutableStateOf<SnifferSource?>(null) }

    when (pendingSource) {
        SnifferSource.RADIO ->
            RadioSnifferWarningDialog(
                // Only request the enable here -- selecting RADIO (and so showing/unfreezing the panel) is
                // left entirely to the state.snifferEnabled LaunchedEffect below, once firmware actually
                // confirms it. Selecting optimistically here used to let ordinary, non-sniffed MeshLog traffic
                // show up in the panel for the few seconds before an unsupported-firmware timeout reverted it,
                // making it look like sniffing had briefly worked when it never did.
                onConfirm = {
                    pendingSource = null
                    radioConfigViewModel.setSnifferEnabled(true)
                },
                onDismiss = { pendingSource = null },
            )

        SnifferSource.MQTT ->
            MqttSnifferWarningDialog(
                onConfirm = {
                    pendingSource = null
                    if (state.snifferEnabled == true) radioConfigViewModel.setSnifferEnabled(false)
                    panelViewModel.selectSource(SnifferSource.MQTT)
                },
                onDismiss = { pendingSource = null },
            )

        SnifferSource.OFF,
        null,
        -> {}
    }

    // ---- what to display: a loaded file overrides the live stream; otherwise follow activeSource ----
    val display =
        rememberSnifferDisplayState(
            loadedLog = loadedLog,
            mqttPackets = mqttPackets,
            radioPackets = radioPackets,
            clearedAtMillis = clearedAtMillis,
            freezeAtMillis = freezeAtMillis,
            groupByGateway = groupByGateway,
            displaySource = displaySource,
        )

    val exportSaver = rememberLogExportSaver()
    val importPicker =
        rememberLogImportPicker(onResult = { imported -> panelViewModel.loadLog(imported.content, imported.fileName) })

    val snackbarHostState = remember { SnackbarHostState() }
    val loadFailedMessage = stringResource(Res.string.sniffer_load_failed)
    LaunchedEffect(panelViewModel) {
        panelViewModel.loadFailed.collect { snackbarHostState.showSnackbar(loadFailedMessage) }
    }
    val snifferEnableFailedMessage = stringResource(Res.string.sniffer_enable_failed)
    LaunchedEffect(radioConfigViewModel, panelViewModel) {
        radioConfigViewModel.snifferEnableFailed.collect {
            panelViewModel.selectSource(SnifferSource.OFF)
            snackbarHostState.showSnackbar(snifferEnableFailedMessage)
        }
    }

    var showGearSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(display.newestKey) { if (autoScroll && display.newestKey != null) listState.animateScrollToItem(0) }

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.sniffer_settings_title),
                subtitle = null,
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onBack,
                actions = {
                    SnifferTopBarActions(
                        display = display,
                        exportFormat = exportFormat,
                        exportSaver = exportSaver,
                        onOpenGearSheet = { showGearSheet = true },
                        onClear = panelViewModel::clearDisplayedLogs,
                    )
                },
                onClickChip = {},
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            loadedLog?.let { log ->
                LoadedLogBanner(fileName = log.fileName, onDismiss = panelViewModel::dismissLoadedLog)
            }
            if (loadedLog == null && activeSource == SnifferSource.MQTT) {
                MqttConnectionStatusRow(mqttConnectionState)
            }

            if (display.itemCount == 0) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = snifferEmptyStateText(loadedLog, activeSource),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 28.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (display.showingMqtt) {
                            items(display.groupedMqttPackets, key = { it.listKey() }) { grouped ->
                                var isExpanded by remember(grouped) { mutableStateOf(false) }
                                GroupedMqttPacketCard(
                                    grouped = grouped,
                                    decryptPayloads = decryptPayloads,
                                    isExpanded = isExpanded,
                                    onClick = { isExpanded = !isExpanded },
                                )
                            }
                        } else if (display.showingRadio) {
                            items(
                                display.groupedRadioPackets,
                                key = {
                                    "${it.packet.packetId}-${it.packet.receivedAtMillis}-" +
                                        "${it.packet.fromId}-${it.packet.toId}"
                                },
                            ) { grouped ->
                                var isExpanded by remember(grouped) { mutableStateOf(false) }
                                GroupedRadioPacketCard(
                                    grouped = grouped,
                                    decryptPayloads = decryptPayloads,
                                    isExpanded = isExpanded,
                                    onClick = { isExpanded = !isExpanded },
                                )
                            }
                        }
                    }
                    FastScrollSidebar(
                        listState = listState,
                        itemCount = display.itemCount,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
                    )
                }
            }
        }
    }

    if (showGearSheet) {
        SnifferGearSheet(
            onDismissRequest = { showGearSheet = false },
            activeSource = activeSource,
            onSelectSource = {
                handleSourceRequest(
                    target = it,
                    activeSource = activeSource,
                    snifferEnabled = state.snifferEnabled,
                    radioConfigViewModel = radioConfigViewModel,
                    panelViewModel = panelViewModel,
                    onPending = { pendingSource = it },
                )
                showGearSheet = false
            },
            radioSelectable = radioSelectable,
            radioLoading = state.snifferLoading,
            mqttConfigured = mqttConfigured,
            groupByGateway = groupByGateway,
            onGroupByGatewayChange = panelViewModel::setGroupByGateway,
            autoScroll = autoScroll,
            onAutoScrollChange = panelViewModel::setAutoScroll,
            decryptPayloads = decryptPayloads,
            onDecryptPayloadsChange = panelViewModel::setDecryptPayloads,
            exportFormat = exportFormat,
            onExportFormatChange = panelViewModel::setExportFormat,
            onLoadLogClick = {
                showGearSheet = false
                importPicker.pick()
            },
        )
    }
}

@Composable
private fun LoadedLogBanner(fileName: String, onDismiss: () -> Unit) {
    Row(
        modifier =
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.sniffer_viewing_loaded_file, fileName),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sniffer_dismiss_loaded_file)) }
    }
}

@Composable
private fun RadioSnifferWarningDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sniffer_warning_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.sniffer_warning_message))
                Text(text = stringResource(Res.string.sniffer_warning_compatibility), fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.sniffer_warning_accept)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sniffer_warning_cancel)) } },
    )
}

@Composable
private fun MqttSnifferWarningDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.mqtt_sniffer_warning_title)) },
        text = { Text(stringResource(Res.string.mqtt_sniffer_warning_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.sniffer_warning_accept)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sniffer_warning_cancel)) } },
    )
}

@Composable
private fun MqttConnectionStatusRow(state: ConnectionState) {
    val (label, color) =
        when (state) {
            is ConnectionState.Disconnected ->
                if (state.reason != null) {
                    stringResource(Res.string.mqtt_status_disconnected_with_reason, state.reason?.message.orEmpty()) to
                        MaterialTheme.colorScheme.error
                } else {
                    stringResource(Res.string.mqtt_status_inactive) to MaterialTheme.colorScheme.outline
                }

            is ConnectionState.Connecting ->
                stringResource(Res.string.mqtt_status_connecting) to MaterialTheme.colorScheme.tertiary

            is ConnectionState.Connected ->
                stringResource(Res.string.mqtt_status_connected) to MaterialTheme.colorScheme.primary

            is ConnectionState.Reconnecting -> {
                val err = state.lastError?.message
                val text =
                    if (err != null) {
                        stringResource(Res.string.mqtt_status_reconnecting_with_attempt, state.attempt, err)
                    } else {
                        stringResource(Res.string.mqtt_status_reconnecting)
                    }
                text to MaterialTheme.colorScheme.tertiary
            }
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Derives everything the panel needs to know about what to show -- pulled out of [SnifferSettingsScreen] itself so that
 * function's own branching stays under detekt's CyclomaticComplexMethod limit. A loaded file overrides the live stream;
 * otherwise [displaySource] decides -- see [SnifferPanelViewModel.displaySource] for why that isn't just activeSource.
 */
@Composable
private fun rememberSnifferDisplayState(
    loadedLog: LoadedSnifferLog?,
    mqttPackets: List<MqttSniffedPacket>,
    radioPackets: List<SniffedPacket>,
    clearedAtMillis: Long,
    freezeAtMillis: Long,
    groupByGateway: Boolean,
    displaySource: SnifferSource,
): SnifferDisplayState {
    val loadedIsMqtt = loadedLog?.rows?.firstOrNull()?.source == "mqtt"
    val loadedIsRadio = loadedLog != null && !loadedIsMqtt
    val showingMqtt = if (loadedLog != null) loadedIsMqtt else displaySource == SnifferSource.MQTT
    val showingRadio = if (loadedLog != null) loadedIsRadio else displaySource == SnifferSource.RADIO

    val displayedMqttPackets =
        remember(loadedLog, mqttPackets, clearedAtMillis, freezeAtMillis) {
            computeDisplayedMqttPackets(loadedLog, mqttPackets, clearedAtMillis, freezeAtMillis)
        }
    val displayedRadioPackets =
        remember(loadedLog, radioPackets, clearedAtMillis, freezeAtMillis) {
            computeDisplayedRadioPackets(loadedLog, radioPackets, clearedAtMillis, freezeAtMillis)
        }
    val groupedMqttPackets =
        remember(displayedMqttPackets, groupByGateway) {
            if (groupByGateway) {
                displayedMqttPackets.groupedByGateway()
            } else {
                displayedMqttPackets.map { GroupedMqttSniffedPacket(it, listOfNotNull(it.gatewayId)) }
            }
        }
    val groupedRadioPackets =
        remember(displayedRadioPackets, groupByGateway) {
            if (groupByGateway) {
                displayedRadioPackets.groupedByRelay()
            } else {
                displayedRadioPackets.map { GroupedSniffedPacket(it, listOfNotNull(it.relayId)) }
            }
        }

    val itemCount =
        if (showingMqtt) {
            groupedMqttPackets.size
        } else if (showingRadio) {
            groupedRadioPackets.size
        } else {
            0
        }

    val newestKey =
        when {
            showingMqtt -> groupedMqttPackets.firstOrNull()?.let { "${it.packet.receivedAtMillis}-${it.packet.topic}" }

            showingRadio ->
                groupedRadioPackets.firstOrNull()?.let {
                    "${it.packet.receivedAtMillis}-${it.packet.fromId}-${it.packet.toId}"
                }

            else -> null
        }

    return SnifferDisplayState(
        showingMqtt = showingMqtt,
        showingRadio = showingRadio,
        displayedMqttPackets = displayedMqttPackets,
        displayedRadioPackets = displayedRadioPackets,
        groupedMqttPackets = groupedMqttPackets,
        groupedRadioPackets = groupedRadioPackets,
        itemCount = itemCount,
        newestKey = newestKey,
    )
}

/**
 * The packets currently backing the MQTT side of the display -- a loaded file's rows, or the live stream restricted to
 * timestamps after [clearedAtMillis] and at or before [freezeAtMillis] (see [SnifferPanelViewModel.freezeAtMillis] for
 * why an upper bound is needed too, not just the trash icon's lower one).
 */
private fun computeDisplayedMqttPackets(
    loadedLog: LoadedSnifferLog?,
    mqttPackets: List<MqttSniffedPacket>,
    clearedAtMillis: Long,
    freezeAtMillis: Long,
): List<MqttSniffedPacket> = if (loadedLog != null) {
    loadedLog.rows.flatMap { it.toMqttSniffedPackets() }.sortedByDescending { it.receivedAtMillis }
} else {
    mqttPackets.filter { it.receivedAtMillis > clearedAtMillis && it.receivedAtMillis <= freezeAtMillis }
}

/** The Radio equivalent of [computeDisplayedMqttPackets]. */
private fun computeDisplayedRadioPackets(
    loadedLog: LoadedSnifferLog?,
    radioPackets: List<SniffedPacket>,
    clearedAtMillis: Long,
    freezeAtMillis: Long,
): List<SniffedPacket> = if (loadedLog != null) {
    loadedLog.rows.mapNotNull { it.toSniffedPacket() }.sortedByDescending { it.receivedAtMillis }
} else {
    radioPackets.filter { it.receivedAtMillis > clearedAtMillis && it.receivedAtMillis <= freezeAtMillis }
}

private fun GroupedMqttSniffedPacket.listKey(): String =
    "${packet.packetId}-${packet.receivedAtMillis}-${packet.topic}-${packet.fromId}"

/**
 * Applies a source selection from the gear menu -- pulled out of [SnifferSettingsScreen] both to keep its own
 * complexity down and because a plain top-level function is a safer bet than a local one for detekt's K2-based
 * analyzer.
 */
private fun handleSourceRequest(
    target: SnifferSource,
    activeSource: SnifferSource,
    snifferEnabled: Boolean?,
    radioConfigViewModel: RadioConfigViewModel,
    panelViewModel: SnifferPanelViewModel,
    onPending: (SnifferSource) -> Unit,
) {
    if (target == activeSource) return
    when (target) {
        SnifferSource.OFF -> {
            if (snifferEnabled == true) radioConfigViewModel.setSnifferEnabled(false)
            panelViewModel.selectSource(SnifferSource.OFF)
        }

        SnifferSource.RADIO,
        SnifferSource.MQTT,
        -> onPending(target)
    }
}
