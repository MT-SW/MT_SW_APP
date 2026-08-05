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
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.meshtastic.feature.node.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.database.entity.FirmwareRelease
import org.meshtastic.core.database.entity.asDeviceVersion
import org.meshtastic.core.model.DeviceVersion
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.SessionStatus
import org.meshtastic.core.repository.EventFirmwareRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.add
import org.meshtastic.core.resources.add_contact
import org.meshtastic.core.resources.administration
import org.meshtastic.core.resources.connect_radio_for_remote_admin
import org.meshtastic.core.resources.establishing_session
import org.meshtastic.core.resources.favorite
import org.meshtastic.core.resources.firmware
import org.meshtastic.core.resources.firmware_edition
import org.meshtastic.core.resources.gpio
import org.meshtastic.core.resources.gpio_mask_display
import org.meshtastic.core.resources.gpio_off
import org.meshtastic.core.resources.gpio_on
import org.meshtastic.core.resources.gpio_pin
import org.meshtastic.core.resources.gpio_read
import org.meshtastic.core.resources.ignore
import org.meshtastic.core.resources.installed_firmware_version
import org.meshtastic.core.resources.latest_alpha_firmware
import org.meshtastic.core.resources.latest_stable_firmware
import org.meshtastic.core.resources.long_name
import org.meshtastic.core.resources.node_id
import org.meshtastic.core.resources.refresh_metadata
import org.meshtastic.core.resources.remote_admin
import org.meshtastic.core.resources.remove
import org.meshtastic.core.resources.session_active
import org.meshtastic.core.resources.session_refresh_required
import org.meshtastic.core.resources.short_name
import org.meshtastic.core.ui.component.BasicListItem
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.icon.ForkLeft
import org.meshtastic.core.ui.icon.Icecream
import org.meshtastic.core.ui.icon.Memory
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Settings
import org.meshtastic.core.ui.theme.StatusColors.StatusGreen
import org.meshtastic.core.ui.theme.StatusColors.StatusOrange
import org.meshtastic.core.ui.theme.StatusColors.StatusRed
import org.meshtastic.core.ui.theme.StatusColors.StatusYellow
import org.meshtastic.feature.node.model.MetricsState
import org.meshtastic.feature.node.model.NodeDetailAction
import org.meshtastic.proto.FirmwareEdition

@Composable
fun AdministrationSection(
    node: Node,
    metricsState: MetricsState,
    onAction: (NodeDetailAction) -> Unit,
    onFirmwareSelect: (FirmwareRelease) -> Unit,
    sessionStatus: SessionStatus,
    isEnsuringSession: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SectionCard(title = Res.string.administration) {
            Column {
                // Local nodes don't need a session — they short-circuit straight to the settings screen.
                if (metricsState.isLocal) {
                    ListItem(
                        text = stringResource(Res.string.remote_admin),
                        leadingIcon = MeshtasticIcons.Settings,
                        onClick = { onAction(NodeDetailAction.OpenRemoteAdmin(node.num)) },
                    )
                } else {
                    RemoteAdminListItem(
                        nodeNum = node.num,
                        sessionStatus = sessionStatus,
                        isEnsuringSession = isEnsuringSession,
                        onAction = onAction,
                    )

                    if (sessionStatus is SessionStatus.Active) {
                        SectionDivider()
                        RemoteNodeManagementCard(
                            destNum = node.num,
                            isEnsuringSession = isEnsuringSession,
                            onAction = onAction,
                        )
                    }

                    SectionDivider()

                    ListItem(
                        text = stringResource(Res.string.refresh_metadata),
                        leadingIcon = MeshtasticIcons.Memory,
                        trailingIcon = null,
                        enabled = !isEnsuringSession,
                        onClick = { onAction(NodeDetailAction.RefreshMetadata(node.num)) },
                    )
                }
            }
        }

        SectionCard(title = Res.string.gpio) {
            Column {
                RemoteHardwareCard(destNum = node.num, onAction = onAction)
                SectionDivider()
                QuickCommandsCard(node = node, onAction = onAction)
            }
        }

        val firmwareVersion = node.metadata?.firmware_version
        val firmwareEdition = metricsState.firmwareEdition
        if (firmwareVersion != null || (firmwareEdition != null && metricsState.isLocal)) {
            FirmwareSection(metricsState, firmwareEdition, firmwareVersion, onFirmwareSelect)
        }
    }
}

/**
 * Single primary affordance for opening the remote-admin screen. Replaces the prior two-row, no-feedback flow that
 * required the user to know they had to tap "Metadata" first to populate `node.metadata` before "Remote Administration"
 * un-greyed out. The session passkey freshness — not the metadata insert — is the real gate (see
 * `firmware/src/modules/AdminModule.cpp:1460-1481`), and is now reflected via an [AssistChip] + inline progress.
 */
@Composable
private fun RemoteAdminListItem(
    nodeNum: Int,
    sessionStatus: SessionStatus,
    isEnsuringSession: Boolean,
    onAction: (NodeDetailAction) -> Unit,
) {
    val supportingTextRes =
        when (sessionStatus) {
            SessionStatus.NoSession -> Res.string.connect_radio_for_remote_admin
            is SessionStatus.Active -> null
            is SessionStatus.Stale -> Res.string.session_refresh_required
        }
    val chipLabelRes =
        when (sessionStatus) {
            SessionStatus.NoSession -> null
            is SessionStatus.Active -> Res.string.session_active
            is SessionStatus.Stale -> Res.string.session_refresh_required
        }

    Column {
        BasicListItem(
            text = stringResource(Res.string.remote_admin),
            leadingIcon = MeshtasticIcons.Settings,
            supportingText = supportingTextRes?.let { stringResource(it) },
            enabled = !isEnsuringSession,
            trailingContent =
            chipLabelRes?.let { res ->
                {
                    AssistChip(
                        onClick = { onAction(NodeDetailAction.OpenRemoteAdmin(nodeNum)) },
                        label = { androidx.compose.material3.Text(stringResource(res)) },
                        enabled = !isEnsuringSession,
                        colors =
                        if (sessionStatus is SessionStatus.Active) {
                            AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            )
                        } else {
                            AssistChipDefaults.assistChipColors()
                        },
                    )
                }
            },
            onClick = { onAction(NodeDetailAction.OpenRemoteAdmin(nodeNum)) },
        )
        AnimatedVisibility(visible = isEnsuringSession) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.Text(
                    text = stringResource(Res.string.establishing_session),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private enum class RemoteNodeListType {
    FAVORITE,
    IGNORE,
}

/**
 * Lets the user, while an admin session with [destNum] is active, tell that remote radio to favorite or ignore a node
 * by its numeric ID — the target node need not be in the local node DB, since this command is administered entirely on
 * the remote radio's own node database over the mesh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteNodeManagementCard(destNum: Int, isEnsuringSession: Boolean, onAction: (NodeDetailAction) -> Unit) {
    val targetIdState = rememberTextFieldState("")
    val longNameState = rememberTextFieldState("")
    val shortNameState = rememberTextFieldState("")
    // Accept the app's standard hex node ID format ("!a1b2c3d4", with or without the "!") — matches how IDs are
    // displayed everywhere else in the app, rather than a raw decimal number the user would have to convert by hand.
    val targetNodeNum = NodeAddress.idToNum(targetIdState.text.toString())
    // Which list on the remote radio we're operating on. Chosen explicitly, then Add/Remove performs the action
    // against it — there's no way to read the remote radio's current state first (the admin protocol only exposes
    // set/unset — no getter), so the app can't offer a toggle reflecting the real current state.
    var listType by remember { mutableStateOf(RemoteNodeListType.FAVORITE) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            state = targetIdState,
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.node_id)) },
            placeholder = { Text("!a1b2c3d4") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            enabled = !isEnsuringSession,
            modifier = Modifier.fillMaxWidth(),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                onClick = { listType = RemoteNodeListType.FAVORITE },
                selected = listType == RemoteNodeListType.FAVORITE,
                enabled = !isEnsuringSession,
                label = { Text(stringResource(Res.string.favorite)) },
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                onClick = { listType = RemoteNodeListType.IGNORE },
                selected = listType == RemoteNodeListType.IGNORE,
                enabled = !isEnsuringSession,
                label = { Text(stringResource(Res.string.ignore)) },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = !isEnsuringSession && targetNodeNum != null,
                onClick = {
                    val id = targetNodeNum ?: return@Button
                    when (listType) {
                        RemoteNodeListType.FAVORITE -> onAction(NodeDetailAction.SetRemoteFavorite(destNum, id, true))
                        RemoteNodeListType.IGNORE -> onAction(NodeDetailAction.SetRemoteIgnored(destNum, id, true))
                    }
                },
            ) {
                Text(stringResource(Res.string.add))
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = !isEnsuringSession && targetNodeNum != null,
                onClick = {
                    val id = targetNodeNum ?: return@Button
                    when (listType) {
                        RemoteNodeListType.FAVORITE -> onAction(NodeDetailAction.SetRemoteFavorite(destNum, id, false))
                        RemoteNodeListType.IGNORE -> onAction(NodeDetailAction.SetRemoteIgnored(destNum, id, false))
                    }
                },
            ) {
                Text(stringResource(Res.string.remove))
            }
        }

        // Manual contact add is a separate action from Favorite/Ignore above — the admin protocol has no
        // "remove contact" counterpart, so it doesn't fit the Add/Remove button pair and gets its own row.
        SectionDivider()
        OutlinedTextField(
            state = longNameState,
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.long_name)) },
            enabled = !isEnsuringSession,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            state = shortNameState,
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.short_name)) },
            enabled = !isEnsuringSession,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Button(
            enabled = !isEnsuringSession && targetNodeNum != null && longNameState.text.isNotBlank(),
            onClick = {
                val id = targetNodeNum ?: return@Button
                onAction(
                    NodeDetailAction.AddRemoteContact(
                        destNum,
                        id,
                        longNameState.text.toString(),
                        shortNameState.text.toString(),
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(Res.string.add_contact))
        }
    }
}

/**
 * Remote Hardware module control: type a GPIO pin number, the app computes the bit mask (`1 shl pin`, matching
 * https://meshtastic.org/docs/configuration/module/remote-hardware/#masks), and On/Off/Read send the corresponding
 * WRITE_GPIOS/READ_GPIOS command to [destNum]. Requires the Remote Hardware module enabled and a shared channel on both
 * ends — the app can't verify that from here, so a failed command surfaces only as "no response" on Read.
 */
@Composable
private fun RemoteHardwareCard(destNum: Int, onAction: (NodeDetailAction) -> Unit) {
    val pinState: TextFieldState = rememberTextFieldState("")
    val pinNumber = pinState.text.toString().toIntOrNull()
    val gpioMask = pinNumber?.takeIf { it in 0..62 }?.let { 1L shl it }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            state = pinState,
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.gpio_pin)) },
            supportingText =
            gpioMask?.let { mask ->
                { Text(stringResource(Res.string.gpio_mask_display, "0x" + mask.toString(16))) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = gpioMask != null,
                onClick = { gpioMask?.let { onAction(NodeDetailAction.WriteGpio(destNum, it, it)) } },
            ) {
                Text(stringResource(Res.string.gpio_on))
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = gpioMask != null,
                onClick = { gpioMask?.let { onAction(NodeDetailAction.WriteGpio(destNum, it, 0L)) } },
            ) {
                Text(stringResource(Res.string.gpio_off))
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = gpioMask != null,
                onClick = { gpioMask?.let { onAction(NodeDetailAction.ReadGpio(destNum, it)) } },
            ) {
                Text(stringResource(Res.string.gpio_read))
            }
        }
    }
}

/**
 * Quick preset text commands sent as a private message to [node] — independent of the normal chat entry point, so these
 * work even for nodes whose role would otherwise hide "Message" from their menu (see [isUnmessageableRole]). Enabled
 * only once we've exchanged a valid public key with [node] ([Node.hasPKC] && ![Node.mismatchKey]), so the command
 * actually goes out PKI-encrypted rather than silently falling back to weaker channel encryption.
 */
@Composable
private fun QuickCommandsCard(node: Node, onAction: (NodeDetailAction) -> Unit) {
    val canSend = node.hasPKC && !node.mismatchKey
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        listOf("/ping", "/hello", "/test").forEach { command ->
            Button(
                modifier = Modifier.weight(1f),
                enabled = canSend,
                onClick = { onAction(NodeDetailAction.SendQuickMessage(node, command)) },
            ) {
                Text(command)
            }
        }
    }
}

/** Substring the Świętokrzyskie firmware build appends to its version string (e.g. "2.7.11.abc12345-MTSW"). */
private const val MT_SW_VERSION_MARKER = "MTSW"

/** Name shown in the UI for a build carrying [MT_SW_VERSION_MARKER]. */
private const val MT_SW_DISPLAY_NAME = "FW_MT_SW"

@Composable
private fun FirmwareSection(
    metricsState: MetricsState,
    firmwareEdition: FirmwareEdition?,
    firmwareVersion: String?,
    onFirmwareSelect: (FirmwareRelease) -> Unit,
) {
    // `firmwareEdition` comes from MyNodeInfo, which only ever describes the locally connected radio — showing it on
    // a remote node's screen would label that node with our own firmware. The version-string marker travels in
    // DeviceMetadata, so it *is* per-node and stays valid for remote nodes.
    val isMtSwBuild = firmwareVersion?.contains(MT_SW_VERSION_MARKER, ignoreCase = true) == true
    val edition = firmwareEdition?.takeIf { metricsState.isLocal }
    val showEdition = edition != null || isMtSwBuild

    SectionCard(title = Res.string.firmware) {
        Column {
            if (showEdition) {
                val eventFirmwareRepository = koinInject<EventFirmwareRepository>()
                val icon =
                    when {
                        isMtSwBuild -> MeshtasticIcons.ForkLeft
                        edition == FirmwareEdition.VANILLA -> MeshtasticIcons.Icecream
                        else -> MeshtasticIcons.ForkLeft
                    }
                val displayName by
                    produceState(edition?.name ?: MT_SW_DISPLAY_NAME, edition, isMtSwBuild) {
                        value =
                            when {
                                isMtSwBuild || edition == FirmwareEdition.DIY_EDITION -> MT_SW_DISPLAY_NAME
                                edition == null -> MT_SW_DISPLAY_NAME
                                edition == FirmwareEdition.VANILLA -> edition.name
                                else -> eventFirmwareRepository.getEdition(edition.name)?.displayName ?: edition.name
                            }
                    }
                ListItem(
                    text = stringResource(Res.string.firmware_edition),
                    leadingIcon = icon,
                    supportingText = displayName,
                    copyable = true,
                    trailingIcon = null,
                )
            }

            firmwareVersion?.let { version ->
                FirmwareVersionItems(metricsState, version, showEdition, onFirmwareSelect)
            }
        }
    }
}

@Composable
private fun FirmwareVersionItems(
    metricsState: MetricsState,
    version: String,
    hasEdition: Boolean,
    onFirmwareSelect: (FirmwareRelease) -> Unit,
) {
    val latestStable = metricsState.latestStableFirmware
    val latestAlpha = metricsState.latestAlphaFirmware

    val deviceVersion = DeviceVersion(version.substringBeforeLast("."))
    val statusColor = deviceVersion.determineFirmwareStatusColor(latestStable, latestAlpha)

    if (hasEdition) SectionDivider()

    ListItem(
        text = stringResource(Res.string.installed_firmware_version),
        leadingIcon = MeshtasticIcons.Memory,
        supportingText = version.substringBeforeLast("."),
        copyable = true,
        leadingIconTint = statusColor,
        trailingIcon = null,
    )

    SectionDivider()

    ListItem(
        text = stringResource(Res.string.latest_stable_firmware),
        leadingIcon = MeshtasticIcons.Memory,
        supportingText = latestStable.id.substringBeforeLast(".").replace("v", ""),
        copyable = true,
        leadingIconTint = MaterialTheme.colorScheme.StatusGreen,
        trailingIcon = null,
        onClick = { onFirmwareSelect(latestStable) },
    )

    SectionDivider()

    ListItem(
        text = stringResource(Res.string.latest_alpha_firmware),
        leadingIcon = MeshtasticIcons.Memory,
        supportingText = latestAlpha.id.substringBeforeLast(".").replace("v", ""),
        copyable = true,
        leadingIconTint = MaterialTheme.colorScheme.StatusYellow,
        trailingIcon = null,
        onClick = { onFirmwareSelect(latestAlpha) },
    )
}

@Composable
private fun DeviceVersion.determineFirmwareStatusColor(
    latestStable: FirmwareRelease,
    latestAlpha: FirmwareRelease,
): Color {
    val stableVersion = latestStable.asDeviceVersion()
    val alphaVersion = latestAlpha.asDeviceVersion()
    return when {
        this < stableVersion -> MaterialTheme.colorScheme.StatusRed
        this == stableVersion -> MaterialTheme.colorScheme.StatusGreen
        this in stableVersion..alphaVersion -> MaterialTheme.colorScheme.StatusYellow
        this > alphaVersion -> MaterialTheme.colorScheme.StatusOrange
        else -> MaterialTheme.colorScheme.onSurface
    }
}
