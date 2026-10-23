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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.navigation.ContactsRoute
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ai_section_title
import org.meshtastic.core.resources.auto_load_chat_images
import org.meshtastic.core.resources.clean_node_database_title
import org.meshtastic.core.resources.lora
import org.meshtastic.core.resources.mt_sw_features_links
import org.meshtastic.core.resources.mt_sw_features_links_hint
import org.meshtastic.core.resources.mt_sw_features_lora_summary
import org.meshtastic.core.resources.mt_sw_features_switches
import org.meshtastic.core.resources.mt_sw_features_title
import org.meshtastic.core.resources.mt_sw_other_calls_body
import org.meshtastic.core.resources.mt_sw_other_calls_title
import org.meshtastic.core.resources.mt_sw_other_full_doc
import org.meshtastic.core.resources.mt_sw_other_gpio_body
import org.meshtastic.core.resources.mt_sw_other_gpio_title
import org.meshtastic.core.resources.mt_sw_other_intro
import org.meshtastic.core.resources.mt_sw_other_lna_body
import org.meshtastic.core.resources.mt_sw_other_lna_title
import org.meshtastic.core.resources.mt_sw_other_polarity_body
import org.meshtastic.core.resources.mt_sw_other_polarity_title
import org.meshtastic.core.resources.mt_sw_other_contacts_body
import org.meshtastic.core.resources.mt_sw_other_contacts_title
import org.meshtastic.core.resources.mt_sw_other_remote_lists_body
import org.meshtastic.core.resources.mt_sw_other_remote_lists_title
import org.meshtastic.core.resources.mt_sw_other_summary
import org.meshtastic.core.resources.mt_sw_other_title
import org.meshtastic.core.resources.node_layout_section_title
import org.meshtastic.core.resources.debug_panel
import org.meshtastic.core.resources.sniffer_settings_title
import org.meshtastic.core.resources.theme
import org.meshtastic.core.resources.translator_open
import org.meshtastic.core.resources.translator_open_summary
import org.meshtastic.core.resources.ui_scale
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.core.ui.icon.BugReport
import org.meshtastic.core.ui.icon.FormatPaint
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.LinkIcon
import org.meshtastic.core.ui.icon.Language
import org.meshtastic.core.ui.icon.List
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Tune
import org.meshtastic.feature.settings.SettingsViewModel
import org.meshtastic.feature.settings.component.AiSettings
import org.meshtastic.feature.settings.component.AppUpdateSettings
import org.meshtastic.feature.settings.component.ExpressiveSection
import org.meshtastic.feature.settings.component.LogRetentionPreference
import org.meshtastic.feature.settings.component.ThemePickerDialog
import org.meshtastic.feature.settings.component.UiScalePickerDialog
import org.meshtastic.feature.settings.component.uiScaleLabel

/**
 * The "MT_SW features" screen: every setting that exists only in MT_SW_APP in one place. The switches that belong to
 * the fork live here; settings that have a natural home elsewhere (scaling, LoRa, Sniffer, node database cleaning)
 * stay there and get a shortcut row.
 */
@Composable
fun MtSwFeaturesScreen(settingsViewModel: SettingsViewModel, onBack: () -> Unit, onNavigate: (Route) -> Unit) {
    val autoLoadChatImages by settingsViewModel.autoLoadChatImages.collectAsStateWithLifecycle()
    val uiScale by settingsViewModel.uiScale.collectAsStateWithLifecycle()
    val retentionDays by settingsViewModel.meshLogRetentionDays.collectAsStateWithLifecycle()
    var showUiScalePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    if (showThemePicker) {
        ThemePickerDialog(
            onClickTheme = { settingsViewModel.setTheme(it) },
            onDismiss = { showThemePicker = false },
        )
    }
    if (showUiScalePicker) {
        UiScalePickerDialog(
            current = uiScale,
            onClickScale = { settingsViewModel.setUiScale(it) },
            onDismiss = { showUiScalePicker = false },
        )
    }

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.mt_sw_features_title),
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
            ExpressiveSection(title = stringResource(Res.string.mt_sw_features_switches)) {
                SwitchListItem(
                    text = stringResource(Res.string.auto_load_chat_images),
                    leadingIcon = MeshtasticIcons.LinkIcon,
                    checked = autoLoadChatImages,
                    onClick = { settingsViewModel.setAutoLoadChatImages(!autoLoadChatImages) },
                )
                AppUpdateSettings()
                ListItem(
                    text = stringResource(Res.string.node_layout_section_title),
                    leadingIcon = MeshtasticIcons.List,
                ) {
                    onNavigate(SettingsRoute.NodeList)
                }
                LogRetentionPreference(
                    retentionDays = retentionDays,
                    onSelected = { settingsViewModel.setMeshLogRetentionDays(it) },
                )
            }

            ExpressiveSection(title = stringResource(Res.string.translator_open)) {
                ListItem(
                    text = stringResource(Res.string.translator_open),
                    supportingText = stringResource(Res.string.translator_open_summary),
                    leadingIcon = MeshtasticIcons.Language,
                ) {
                    onNavigate(ContactsRoute.Translator)
                }
            }

            ExpressiveSection(title = stringResource(Res.string.ai_section_title)) { AiSettings() }

            ExpressiveSection(title = stringResource(Res.string.mt_sw_features_links)) {
                Text(
                    text = stringResource(Res.string.mt_sw_features_links_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ListItem(
                    text = stringResource(Res.string.ui_scale),
                    supportingText = uiScaleLabel(uiScale),
                    leadingIcon = MeshtasticIcons.FormatPaint,
                ) {
                    showUiScalePicker = true
                }
                ListItem(text = stringResource(Res.string.theme), leadingIcon = MeshtasticIcons.FormatPaint) {
                    showThemePicker = true
                }
                ListItem(
                    text = stringResource(Res.string.lora),
                    supportingText = stringResource(Res.string.mt_sw_features_lora_summary),
                    leadingIcon = MeshtasticIcons.Tune,
                ) {
                    onNavigate(SettingsRoute.LoRa)
                }
                ListItem(
                    text = stringResource(Res.string.clean_node_database_title),
                    leadingIcon = MeshtasticIcons.List,
                ) {
                    onNavigate(SettingsRoute.CleanNodeDb)
                }
                ListItem(text = stringResource(Res.string.sniffer_settings_title), leadingIcon = MeshtasticIcons.Tune) {
                    onNavigate(SettingsRoute.Sniffer)
                }
                ListItem(text = stringResource(Res.string.debug_panel), leadingIcon = MeshtasticIcons.BugReport) {
                    onNavigate(SettingsRoute.DebugPanel)
                }
            }

            ExpressiveSection(title = stringResource(Res.string.mt_sw_other_title)) {
                ListItem(
                    text = stringResource(Res.string.mt_sw_other_title),
                    supportingText = stringResource(Res.string.mt_sw_other_summary),
                    leadingIcon = MeshtasticIcons.Info,
                ) {
                    onNavigate(SettingsRoute.MtSwOtherFeatures)
                }
            }
        }
    }
}

/** An information page: where the MT_SW features that live next to a node (not in Settings) are, and what they do. */
@Composable
fun MtSwOtherFeaturesScreen(onBack: () -> Unit, onNavigate: (Route) -> Unit) {
    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.mt_sw_other_title),
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
            Text(text = stringResource(Res.string.mt_sw_other_intro), style = MaterialTheme.typography.bodyMedium)
            OtherFeature(Res.string.mt_sw_other_lna_title, Res.string.mt_sw_other_lna_body)
            OtherFeature(Res.string.mt_sw_other_polarity_title, Res.string.mt_sw_other_polarity_body)
            OtherFeature(Res.string.mt_sw_other_gpio_title, Res.string.mt_sw_other_gpio_body)
            OtherFeature(Res.string.mt_sw_other_remote_lists_title, Res.string.mt_sw_other_remote_lists_body)
            OtherFeature(Res.string.mt_sw_other_contacts_title, Res.string.mt_sw_other_contacts_body)
            OtherFeature(Res.string.mt_sw_other_calls_title, Res.string.mt_sw_other_calls_body)
            ExpressiveSection(title = stringResource(Res.string.mt_sw_other_full_doc)) {
                ListItem(text = stringResource(Res.string.mt_sw_other_full_doc), leadingIcon = MeshtasticIcons.Info) {
                    onNavigate(SettingsRoute.HelpDocPage("mt-sw-features"))
                }
            }
        }
    }
}

@Composable
private fun OtherFeature(title: StringResource, body: StringResource) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = stringResource(body), style = MaterialTheme.typography.bodyMedium)
    }
}
