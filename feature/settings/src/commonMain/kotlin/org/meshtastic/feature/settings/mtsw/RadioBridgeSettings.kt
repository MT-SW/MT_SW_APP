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
@file:Suppress("ModifierMissing", "MultipleEmitters")

package org.meshtastic.feature.settings.mtsw

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.Node
import org.meshtastic.core.repository.RadioBridgeConfig
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.radio_bridge_address
import org.meshtastic.core.resources.radio_bridge_block_admin
import org.meshtastic.core.resources.radio_bridge_block_messages
import org.meshtastic.core.resources.radio_bridge_config_age
import org.meshtastic.core.resources.radio_bridge_config_age_connection
import org.meshtastic.core.resources.radio_bridge_config_age_hours
import org.meshtastic.core.resources.radio_bridge_config_age_minutes
import org.meshtastic.core.resources.radio_bridge_device_number
import org.meshtastic.core.resources.radio_bridge_enabled
import org.meshtastic.core.resources.radio_bridge_lan
import org.meshtastic.core.resources.radio_bridge_max_clients
import org.meshtastic.core.resources.radio_bridge_no_radio
import org.meshtastic.core.resources.radio_bridge_port
import org.meshtastic.core.resources.radio_bridge_port_summary
import org.meshtastic.core.resources.radio_bridge_read_only
import org.meshtastic.core.resources.radio_bridge_refresh_now
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.EditTextPreference
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.NodeChip
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.core.ui.icon.AdminPanelSettings
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Message
import org.meshtastic.core.ui.icon.Refresh
import org.meshtastic.core.ui.icon.Share
import org.meshtastic.core.ui.icon.Udp
import org.meshtastic.core.ui.icon.Visibility
import org.meshtastic.proto.User

/**
 * The settings of the TCP bridges: one block for every saved device, so several devices can be shared at the same time,
 * each on its own port. Everything is off by default; the restrictions only limit what the connected programs may send.
 * The name of a device that is not connected is not shown.
 */
@Composable
fun RadioBridgeSettings(viewModel: RadioBridgeViewModel = koinViewModel()) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    if (devices.isEmpty()) {
        Text(stringResource(Res.string.radio_bridge_no_radio))
        return
    }
    devices.forEach { device ->
        RadioBridgeDeviceSettings(device, onRefresh = { viewModel.refreshConfiguration(device.slotId) }) { change ->
            viewModel.update(device.slotId, change)
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun RadioBridgeDeviceSettings(
    device: RadioBridgeDevice,
    onRefresh: () -> Unit,
    update: ((RadioBridgeConfig) -> RadioBridgeConfig) -> Unit,
) {
    val slotId = device.slotId
    val config = device.config
    if (device.name != null && device.nodeNum != null) {
        NodeChip(
            node = Node(num = device.nodeNum, user = User.Builder().also { it.short_name = device.name }.build()),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    } else {
        Text(stringResource(Res.string.radio_bridge_device_number, slotId))
    }
    SwitchListItem(
        text = stringResource(Res.string.radio_bridge_enabled),
        leadingIcon = MeshtasticIcons.Share,
        checked = config.enabled,
        onClick = { update { it.copy(enabled = !it.enabled) } },
    )
    if (!config.enabled) return
    val port = config.portFor(slotId)
    Text(stringResource(Res.string.radio_bridge_address, if (config.lanAccess) "0.0.0.0" else "127.0.0.1", port))
    EditTextPreference(
        title = stringResource(Res.string.radio_bridge_port),
        summary = stringResource(Res.string.radio_bridge_port_summary, RadioBridgeConfig.defaultPort(slotId)),
        value = config.port,
        enabled = true,
        isError = config.port != 0 && config.port !in RadioBridgeConfig.MIN_PORT..RadioBridgeConfig.MAX_PORT,
        keyboardActions = KeyboardActions {},
        onValueChanged = { value ->
            update { it.copy(port = value.coerceIn(0, RadioBridgeConfig.MAX_PORT)) }
        },
    )
    SwitchListItem(
        text = stringResource(Res.string.radio_bridge_lan),
        leadingIcon = MeshtasticIcons.Udp,
        checked = config.lanAccess,
        onClick = { update { it.copy(lanAccess = !it.lanAccess) } },
    )
    DropDownPreference(
        title = stringResource(Res.string.radio_bridge_max_clients),
        enabled = true,
        items = RadioBridgeConfig.MAX_CLIENTS_CHOICES.map { it to it.toString() },
        selectedItem = config.maxClients,
        onItemSelected = { value -> update { it.copy(maxClients = value) } },
    )
    DropDownPreference(
        title = stringResource(Res.string.radio_bridge_config_age),
        enabled = true,
        items =
        RadioBridgeConfig.CONFIG_MAX_AGE_CHOICES.map { minutes ->
            minutes to
                when (minutes) {
                    0 -> stringResource(Res.string.radio_bridge_config_age_connection)

                    in 1..MINUTES_IN_HOUR_LESS ->
                        stringResource(Res.string.radio_bridge_config_age_minutes, minutes)

                    else -> stringResource(Res.string.radio_bridge_config_age_hours, minutes / MINUTES_IN_HOUR)
                }
        },
        selectedItem = config.configMaxAgeMinutes,
        onItemSelected = { value -> update { it.copy(configMaxAgeMinutes = value) } },
    )
    ListItem(
        text = stringResource(Res.string.radio_bridge_refresh_now),
        leadingIcon = MeshtasticIcons.Refresh,
        onClick = onRefresh,
    )
    SwitchListItem(
        text = stringResource(Res.string.radio_bridge_read_only),
        leadingIcon = MeshtasticIcons.Visibility,
        checked = config.readOnly,
        onClick = { update { it.copy(readOnly = !it.readOnly) } },
    )
    SwitchListItem(
        text = stringResource(Res.string.radio_bridge_block_admin),
        leadingIcon = MeshtasticIcons.AdminPanelSettings,
        checked = config.blockAdmin,
        onClick = { update { it.copy(blockAdmin = !it.blockAdmin) } },
    )
    SwitchListItem(
        text = stringResource(Res.string.radio_bridge_block_messages),
        leadingIcon = MeshtasticIcons.Message,
        checked = config.blockMessages,
        onClick = { update { it.copy(blockMessages = !it.blockMessages) } },
    )
}

private const val MINUTES_IN_HOUR = 60
private const val MINUTES_IN_HOUR_LESS = 59
