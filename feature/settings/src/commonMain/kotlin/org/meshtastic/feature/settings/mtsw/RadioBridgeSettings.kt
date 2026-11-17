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
package org.meshtastic.feature.settings.mtsw

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.repository.RadioBridgeConfig
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.radio_bridge_address
import org.meshtastic.core.resources.radio_bridge_block_admin
import org.meshtastic.core.resources.radio_bridge_block_messages
import org.meshtastic.core.resources.radio_bridge_enabled
import org.meshtastic.core.resources.radio_bridge_lan
import org.meshtastic.core.resources.radio_bridge_max_clients
import org.meshtastic.core.resources.radio_bridge_no_radio
import org.meshtastic.core.resources.radio_bridge_port
import org.meshtastic.core.resources.radio_bridge_port_summary
import org.meshtastic.core.resources.radio_bridge_radio
import org.meshtastic.core.resources.radio_bridge_read_only
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.EditTextPreference
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.core.ui.icon.MeshtasticIcons

/**
 * The settings of the TCP bridge of the chosen radio. Everything is off by default; the restrictions only limit what
 * the connected programs may send.
 */
@Composable
fun RadioBridgeSettings(viewModel: RadioBridgeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val slotId = state.slotId
    if (slotId == null) {
        Text(stringResource(Res.string.radio_bridge_no_radio))
        return
    }
    val config = state.config
    Text(stringResource(Res.string.radio_bridge_radio, state.name.orEmpty()))
    SwitchListItem(
        text = stringResource(Res.string.radio_bridge_enabled),
        leadingIcon = MeshtasticIcons.Language,
        checked = config.enabled,
        onClick = { viewModel.update { it.copy(enabled = !it.enabled) } },
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
            viewModel.update { it.copy(port = value.coerceIn(0, RadioBridgeConfig.MAX_PORT)) }
        },
    )
    SwitchListItem(
        text = stringResource(Res.string.radio_bridge_lan),
        leadingIcon = MeshtasticIcons.Language,
        checked = config.lanAccess,
        onClick = { viewModel.update { it.copy(lanAccess = !it.lanAccess) } },
    )
    DropDownPreference(
        title = stringResource(Res.string.radio_bridge_max_clients),
        enabled = true,
        items = RadioBridgeConfig.MAX_CLIENTS_CHOICES.map { it to it.toString() },
        selectedItem = config.maxClients,
        onItemSelected = { value -> viewModel.update { it.copy(maxClients = value) } },
    )
    SwitchListItem(
        text = stringResource(Res.string.radio_bridge_read_only),
        leadingIcon = MeshtasticIcons.Language,
        checked = config.readOnly,
        onClick = { viewModel.update { it.copy(readOnly = !it.readOnly) } },
    )
    SwitchListItem(
        text = stringResource(Res.string.radio_bridge_block_admin),
        leadingIcon = MeshtasticIcons.Language,
        checked = config.blockAdmin,
        onClick = { viewModel.update { it.copy(blockAdmin = !it.blockAdmin) } },
    )
    SwitchListItem(
        text = stringResource(Res.string.radio_bridge_block_messages),
        leadingIcon = MeshtasticIcons.Language,
        checked = config.blockMessages,
        onClick = { viewModel.update { it.copy(blockMessages = !it.blockMessages) } },
    )
}
