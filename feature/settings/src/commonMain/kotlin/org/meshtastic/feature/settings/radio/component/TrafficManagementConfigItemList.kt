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
package org.meshtastic.feature.settings.radio.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.traffic_management
import org.meshtastic.core.resources.traffic_management_config
import org.meshtastic.core.resources.traffic_management_nodeinfo_direct_response_max_hops
import org.meshtastic.core.resources.traffic_management_position_min_interval
import org.meshtastic.core.resources.traffic_management_rate_limit_max_packets
import org.meshtastic.core.resources.traffic_management_rate_limit_window
import org.meshtastic.core.resources.traffic_management_unknown_packet_threshold
import org.meshtastic.core.ui.component.EditTextPreference
import org.meshtastic.core.ui.component.TitledCard
import org.meshtastic.feature.settings.radio.RadioConfigViewModel
import org.meshtastic.proto.ModuleConfig

/**
 * Traffic Management module config.
 *
 * All fields use the "non-zero implies enabled" convention: a value of 0 disables that particular sub-feature, any
 * value above 0 enables it using that value as the threshold/interval/window. There is no separate master on/off switch
 * and no separate per-feature enable toggles — matches the upstream protobuf shape (see module_config.proto), which
 * dropped the old bool toggles and the position_precision_bits field (precision is now derived from the channel's own
 * position_precision).
 */
@Suppress("LongMethod")
@Composable
fun TrafficManagementConfigScreen(viewModel: RadioConfigViewModel, onBack: () -> Unit) {
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()
    val tmConfig = state.moduleConfig.traffic_management ?: ModuleConfig.TrafficManagementConfig()
    val formState = rememberConfigState(initialValue = tmConfig)
    val focusManager = LocalFocusManager.current

    LaunchedEffect(tmConfig) { formState.value = tmConfig }

    RadioConfigScreenList(
        title = stringResource(Res.string.traffic_management),
        onBack = onBack,
        configState = formState,
        enabled = state.connected,
        responseState = state.responseState,
        onDismissPacketResponse = viewModel::clearPacketResponse,
        onSave = {
            val config = ModuleConfig(traffic_management = it)
            viewModel.setModuleConfig(config)
        },
    ) {
        item {
            TitledCard(title = stringResource(Res.string.traffic_management_config)) {
                EditTextPreference(
                    title = stringResource(Res.string.traffic_management_position_min_interval),
                    value = formState.value.position_min_interval_secs,
                    enabled = state.connected,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    onValueChanged = { formState.value = formState.value.copy(position_min_interval_secs = it) },
                )
                HorizontalDivider()
                EditTextPreference(
                    title = stringResource(Res.string.traffic_management_nodeinfo_direct_response_max_hops),
                    value = formState.value.nodeinfo_direct_response_max_hops,
                    enabled = state.connected,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    onValueChanged = { formState.value = formState.value.copy(nodeinfo_direct_response_max_hops = it) },
                )
                HorizontalDivider()
                EditTextPreference(
                    title = stringResource(Res.string.traffic_management_rate_limit_window),
                    value = formState.value.rate_limit_window_secs,
                    enabled = state.connected,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    onValueChanged = { formState.value = formState.value.copy(rate_limit_window_secs = it) },
                )
                HorizontalDivider()
                EditTextPreference(
                    title = stringResource(Res.string.traffic_management_rate_limit_max_packets),
                    value = formState.value.rate_limit_max_packets,
                    enabled = state.connected,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    onValueChanged = { formState.value = formState.value.copy(rate_limit_max_packets = it) },
                )
                HorizontalDivider()
                EditTextPreference(
                    title = stringResource(Res.string.traffic_management_unknown_packet_threshold),
                    value = formState.value.unknown_packet_threshold,
                    enabled = state.connected,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    onValueChanged = { formState.value = formState.value.copy(unknown_packet_threshold = it) },
                )
            }
        }
    }
}
