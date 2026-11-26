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
@file:Suppress("ModifierMissing")

package org.meshtastic.feature.settings.mtsw

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.connection_center_intro
import org.meshtastic.core.resources.connection_center_title
import org.meshtastic.core.resources.console_summary
import org.meshtastic.core.resources.console_title
import org.meshtastic.core.resources.device_compare_title
import org.meshtastic.core.resources.radio_bridge_title
import org.meshtastic.core.resources.radio_chooser_settings
import org.meshtastic.core.ui.component.FloatingBarUnderlap
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.SosBarScreen
import org.meshtastic.core.ui.component.floatingBarEndPadding
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Serial
import org.meshtastic.feature.settings.SettingsViewModel
import org.meshtastic.feature.settings.component.ExpressiveSection
import org.meshtastic.feature.settings.component.RadioChooserSettings

/**
 * The connection center (MT_SW): everything about several radios in one place under one name: the radio chooser of the
 * top bar (signal, number, background collection), the TCP bridge that shares a radio with other programs, and the
 * console.
 */
@Composable
fun ConnectionCenterScreen(settingsViewModel: SettingsViewModel, onBack: () -> Unit, onNavigate: (Route) -> Unit) {
    val uiOptionsMap by settingsViewModel.uiOptions.collectAsStateWithLifecycle()
    val options = UiOptions(uiOptionsMap)
    FloatingBarUnderlap()
    Scaffold(
        topBar = {
            MainAppBar(
                sosScreen = SosBarScreen.SETTINGS,
                title = stringResource(Res.string.connection_center_title),
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
            modifier =
            Modifier.padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .floatingBarEndPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(Res.string.connection_center_intro), style = MaterialTheme.typography.bodyMedium)
            ExpressiveSection(title = stringResource(Res.string.radio_chooser_settings)) {
                RadioChooserSettings(options = options, onChange = settingsViewModel::setUiOption)
            }
            ExpressiveSection(title = stringResource(Res.string.radio_bridge_title)) { RadioBridgeSettings() }
            ExpressiveSection(title = stringResource(Res.string.device_compare_title)) { DeviceCompareEntry() }
            ExpressiveSection(title = stringResource(Res.string.console_title)) {
                ListItem(
                    text = stringResource(Res.string.console_title),
                    supportingText = stringResource(Res.string.console_summary),
                    leadingIcon = MeshtasticIcons.Serial,
                ) {
                    onNavigate(SettingsRoute.Console)
                }
            }
        }
    }
}
