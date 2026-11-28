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
@file:Suppress("LongMethod", "ModifierMissing")

package org.meshtastic.feature.settings.mtsw

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import org.meshtastic.core.common.background.AppBackgroundControls
import org.meshtastic.core.common.background.AutostartControls
import org.meshtastic.core.common.background.SingleInstanceControls
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.model.WhatsNewRequest
import org.meshtastic.core.navigation.ContactsRoute
import org.meshtastic.core.navigation.MapRoute
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ai_section_title
import org.meshtastic.core.resources.antenna_calc_summary
import org.meshtastic.core.resources.antenna_calc_title
import org.meshtastic.core.resources.app_log_title
import org.meshtastic.core.resources.archive_summary
import org.meshtastic.core.resources.archive_title
import org.meshtastic.core.resources.auto_load_chat_images
import org.meshtastic.core.resources.autostart_title
import org.meshtastic.core.resources.background_section_title
import org.meshtastic.core.resources.backup_app_database
import org.meshtastic.core.resources.backup_app_settings
import org.meshtastic.core.resources.clean_node_database_title
import org.meshtastic.core.resources.connection_center_summary
import org.meshtastic.core.resources.connection_center_title
import org.meshtastic.core.resources.console_title
import org.meshtastic.core.resources.data_logs_summary
import org.meshtastic.core.resources.data_logs_title
import org.meshtastic.core.resources.data_notice_settings_title
import org.meshtastic.core.resources.data_notice_shortcut_summary
import org.meshtastic.core.resources.debug_panel
import org.meshtastic.core.resources.element_look_short
import org.meshtastic.core.resources.element_look_title
import org.meshtastic.core.resources.floating_nav_bar
import org.meshtastic.core.resources.folders_title
import org.meshtastic.core.resources.import_menu_in_top_bar
import org.meshtastic.core.resources.intro_mtsw_planner_title
import org.meshtastic.core.resources.kb_summary
import org.meshtastic.core.resources.kb_title
import org.meshtastic.core.resources.local_stats_title
import org.meshtastic.core.resources.look_advanced
import org.meshtastic.core.resources.look_section_bar
import org.meshtastic.core.resources.look_section_basic
import org.meshtastic.core.resources.look_section_layout
import org.meshtastic.core.resources.look_section_theme
import org.meshtastic.core.resources.lora
import org.meshtastic.core.resources.mt_sw_features_links
import org.meshtastic.core.resources.mt_sw_features_links_hint
import org.meshtastic.core.resources.mt_sw_features_lora_summary
import org.meshtastic.core.resources.mt_sw_features_own
import org.meshtastic.core.resources.mt_sw_features_switches
import org.meshtastic.core.resources.mt_sw_features_title
import org.meshtastic.core.resources.mt_sw_group_help
import org.meshtastic.core.resources.mt_sw_group_look
import org.meshtastic.core.resources.mt_sw_group_safety
import org.meshtastic.core.resources.mt_sw_map_filter_summary
import org.meshtastic.core.resources.mt_sw_map_filter_title
import org.meshtastic.core.resources.mt_sw_map_title
import org.meshtastic.core.resources.mt_sw_other_archive_body
import org.meshtastic.core.resources.mt_sw_other_archive_title
import org.meshtastic.core.resources.mt_sw_other_availability_body
import org.meshtastic.core.resources.mt_sw_other_availability_title
import org.meshtastic.core.resources.mt_sw_other_calls_body
import org.meshtastic.core.resources.mt_sw_other_calls_title
import org.meshtastic.core.resources.mt_sw_other_compare_body
import org.meshtastic.core.resources.mt_sw_other_compare_title
import org.meshtastic.core.resources.mt_sw_other_contacts_body
import org.meshtastic.core.resources.mt_sw_other_contacts_title
import org.meshtastic.core.resources.mt_sw_other_coverage_body
import org.meshtastic.core.resources.mt_sw_other_coverage_title
import org.meshtastic.core.resources.mt_sw_other_full_doc
import org.meshtastic.core.resources.mt_sw_other_gpio_body
import org.meshtastic.core.resources.mt_sw_other_gpio_title
import org.meshtastic.core.resources.mt_sw_other_intro
import org.meshtastic.core.resources.mt_sw_other_lna_body
import org.meshtastic.core.resources.mt_sw_other_lna_title
import org.meshtastic.core.resources.mt_sw_other_notification_test_body
import org.meshtastic.core.resources.mt_sw_other_notification_test_title
import org.meshtastic.core.resources.mt_sw_other_polarity_body
import org.meshtastic.core.resources.mt_sw_other_polarity_title
import org.meshtastic.core.resources.mt_sw_other_remote_lists_body
import org.meshtastic.core.resources.mt_sw_other_remote_lists_title
import org.meshtastic.core.resources.mt_sw_other_silence_body
import org.meshtastic.core.resources.mt_sw_other_silence_title
import org.meshtastic.core.resources.mt_sw_other_single_instance_body
import org.meshtastic.core.resources.mt_sw_other_single_instance_title
import org.meshtastic.core.resources.mt_sw_other_sos_body
import org.meshtastic.core.resources.mt_sw_other_sos_title
import org.meshtastic.core.resources.mt_sw_other_summary
import org.meshtastic.core.resources.mt_sw_other_title
import org.meshtastic.core.resources.mt_sw_planner_open_summary
import org.meshtastic.core.resources.mt_sw_terrain_open_summary
import org.meshtastic.core.resources.network_report_setting
import org.meshtastic.core.resources.node_color_pick
import org.meshtastic.core.resources.node_color_summary
import org.meshtastic.core.resources.node_color_title
import org.meshtastic.core.resources.node_layout_section_title
import org.meshtastic.core.resources.presence_device_switch
import org.meshtastic.core.resources.presence_more_summary
import org.meshtastic.core.resources.presence_more_title
import org.meshtastic.core.resources.presence_summary
import org.meshtastic.core.resources.single_instance_title
import org.meshtastic.core.resources.skip_bad_time_chart_points
import org.meshtastic.core.resources.smart_features_description
import org.meshtastic.core.resources.smart_features_summary
import org.meshtastic.core.resources.smart_features_title
import org.meshtastic.core.resources.sniffer_settings_title
import org.meshtastic.core.resources.sos_summary
import org.meshtastic.core.resources.sos_title
import org.meshtastic.core.resources.terrain_horizon_title
import org.meshtastic.core.resources.theme
import org.meshtastic.core.resources.translator_open
import org.meshtastic.core.resources.translator_open_summary
import org.meshtastic.core.resources.ui_scale
import org.meshtastic.core.resources.whats_new_open_desc
import org.meshtastic.core.resources.whats_new_title
import org.meshtastic.core.ui.assistant.SettingHighlight
import org.meshtastic.core.ui.component.FloatingBarUnderlap
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.SosBarScreen
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.core.ui.component.WhatsNewDialog
import org.meshtastic.core.ui.component.floatingBarEndPadding
import org.meshtastic.core.ui.component.verticalScrollWithBar
import org.meshtastic.core.ui.icon.Add
import org.meshtastic.core.ui.icon.Antenna
import org.meshtastic.core.ui.icon.AutoAwesome
import org.meshtastic.core.ui.icon.BarChart
import org.meshtastic.core.ui.icon.BugReport
import org.meshtastic.core.ui.icon.CellTower
import org.meshtastic.core.ui.icon.CenterFocusStrong
import org.meshtastic.core.ui.icon.DisplaySettings
import org.meshtastic.core.ui.icon.FilterList
import org.meshtastic.core.ui.icon.FilterListOff
import org.meshtastic.core.ui.icon.FormatPaint
import org.meshtastic.core.ui.icon.HelpOutline
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.Language
import org.meshtastic.core.ui.icon.LinkIcon
import org.meshtastic.core.ui.icon.List
import org.meshtastic.core.ui.icon.MeshRadio
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Save
import org.meshtastic.core.ui.icon.Serial
import org.meshtastic.core.ui.icon.Tune
import org.meshtastic.core.ui.icon.Upload
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.feature.settings.SettingsViewModel
import org.meshtastic.feature.settings.component.AiSettings
import org.meshtastic.feature.settings.component.AiStorageRow
import org.meshtastic.feature.settings.component.AppBackgroundSettings
import org.meshtastic.feature.settings.component.AppIconSettingsItem
import org.meshtastic.feature.settings.component.AppLogSettings
import org.meshtastic.feature.settings.component.AppUpdateSettings
import org.meshtastic.feature.settings.component.AutostartSettings
import org.meshtastic.feature.settings.component.DatabaseSyncSettingItems
import org.meshtastic.feature.settings.component.ElementAdvancedSettings
import org.meshtastic.feature.settings.component.ElementBasicSettings
import org.meshtastic.feature.settings.component.ExpressiveSection
import org.meshtastic.feature.settings.component.FontSettingsItems
import org.meshtastic.feature.settings.component.LogRetentionPreference
import org.meshtastic.feature.settings.component.NavBarBehaviourSettings
import org.meshtastic.feature.settings.component.NotificationTestSection
import org.meshtastic.feature.settings.component.PhotoFolderSettings
import org.meshtastic.feature.settings.component.PresenceManualAsk
import org.meshtastic.feature.settings.component.PresenceSettings
import org.meshtastic.feature.settings.component.ReportIssuesSection
import org.meshtastic.feature.settings.component.SingleInstanceSettings
import org.meshtastic.feature.settings.component.SupportProjectSection
import org.meshtastic.feature.settings.component.ThemePickerDialog
import org.meshtastic.feature.settings.component.UiScalePickerDialog
import org.meshtastic.feature.settings.component.WidgetStatsSettingItem
import org.meshtastic.feature.settings.component.themeSummary
import org.meshtastic.feature.settings.component.uiScaleLabel

/**
 * The "MT_SW features" screen: every setting that exists only in MT_SW_APP in one place. The switches that belong to
 * the fork live here; settings that have a natural home elsewhere (scaling, LoRa, Sniffer, node database cleaning) stay
 * there and get a shortcut row.
 */
@Composable
fun MtSwFeaturesScreen(settingsViewModel: SettingsViewModel, onBack: () -> Unit, onNavigate: (Route) -> Unit) {
    val autoLoadChatImages by settingsViewModel.autoLoadChatImages.collectAsStateWithLifecycle()
    val skipBadTimes by settingsViewModel.skipBadTimeChartPoints.collectAsStateWithLifecycle()
    val dbStatsSyncEnabled by settingsViewModel.dbStatsSyncEnabled.collectAsStateWithLifecycle()
    val uiOptionsMap by settingsViewModel.uiOptions.collectAsStateWithLifecycle()
    val options = UiOptions(uiOptionsMap)
    val onChange = settingsViewModel::setUiOption
    FloatingBarUnderlap()
    Scaffold(
        topBar = {
            MainAppBar(
                sosScreen = SosBarScreen.SETTINGS,
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
            modifier =
            Modifier.padding(paddingValues)
                .verticalScrollWithBar(rememberScrollState())
                .padding(16.dp)
                .floatingBarEndPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExpressiveSection(title = stringResource(Res.string.mt_sw_features_switches)) {
                SwitchListItem(
                    text = stringResource(Res.string.auto_load_chat_images),
                    leadingIcon = MeshtasticIcons.LinkIcon,
                    checked = autoLoadChatImages,
                    onClick = { settingsViewModel.setAutoLoadChatImages(!autoLoadChatImages) },
                )
                SwitchListItem(
                    text = stringResource(Res.string.skip_bad_time_chart_points),
                    leadingIcon = MeshtasticIcons.FilterListOff,
                    checked = skipBadTimes,
                    onClick = { settingsViewModel.setSkipBadTimeChartPoints(!skipBadTimes) },
                )
                SwitchListItem(
                    text = stringResource(Res.string.network_report_setting),
                    leadingIcon = MeshtasticIcons.BarChart,
                    checked = options[UiOptions.NETWORK_REPORT] == 1,
                    onClick = {
                        val on = options[UiOptions.NETWORK_REPORT] != 1
                        onChange(UiOptions.NETWORK_REPORT, if (on) 1 else 0)
                        // A new start of the counting: the first report comes a week after switching on.
                        onChange(UiOptions.NETWORK_REPORT_LAST, 0)
                    },
                )
                AppUpdateSettings()
            }

            ExpressiveSection(title = stringResource(Res.string.mt_sw_group_look)) {
                ListItem(
                    text = stringResource(Res.string.element_look_title),
                    supportingText = stringResource(Res.string.element_look_short),
                    leadingIcon = MeshtasticIcons.DisplaySettings,
                ) {
                    onNavigate(SettingsRoute.ElementLook)
                }
                ListItem(
                    text = stringResource(Res.string.data_logs_title),
                    supportingText = stringResource(Res.string.data_logs_summary),
                    leadingIcon = MeshtasticIcons.Save,
                ) {
                    onNavigate(SettingsRoute.DataAndLogs)
                }
                ListItem(
                    text = stringResource(Res.string.connection_center_title),
                    supportingText = stringResource(Res.string.connection_center_summary),
                    leadingIcon = MeshtasticIcons.MeshRadio,
                ) {
                    onNavigate(SettingsRoute.ConnectionCenter)
                }
                ListItem(
                    text = stringResource(Res.string.smart_features_title),
                    supportingText = stringResource(Res.string.smart_features_summary),
                    leadingIcon = MeshtasticIcons.AutoAwesome,
                ) {
                    onNavigate(SettingsRoute.SmartFeatures)
                }
            }

            // Planning the network in one place: the link planner (opens for the connected radio), the terrain panorama
            // and the antenna calculator.
            ExpressiveSection(title = stringResource(Res.string.mt_sw_map_title)) {
                ListItem(
                    text = stringResource(Res.string.intro_mtsw_planner_title),
                    supportingText = stringResource(Res.string.mt_sw_planner_open_summary),
                    leadingIcon = MeshtasticIcons.CellTower,
                ) {
                    onNavigate(MapRoute.Map(plannerNodeNum = settingsViewModel.myNodeNum))
                }
                ListItem(
                    text = stringResource(Res.string.terrain_horizon_title),
                    supportingText = stringResource(Res.string.mt_sw_terrain_open_summary),
                    leadingIcon = MeshtasticIcons.CenterFocusStrong,
                ) {
                    onNavigate(MapRoute.MeshHud())
                }
                ListItem(
                    text = stringResource(Res.string.mt_sw_map_filter_title),
                    supportingText = stringResource(Res.string.mt_sw_map_filter_summary),
                    leadingIcon = MeshtasticIcons.FilterList,
                ) {
                    onNavigate(MapRoute.Map())
                }
                ListItem(
                    text = stringResource(Res.string.antenna_calc_title),
                    supportingText = stringResource(Res.string.antenna_calc_summary),
                    leadingIcon = MeshtasticIcons.Antenna,
                ) {
                    onNavigate(SettingsRoute.AntennaCalculator)
                }
            }

            ExpressiveSection(title = stringResource(Res.string.mt_sw_group_safety)) {
                ListItem(
                    text = stringResource(Res.string.sos_title),
                    supportingText = stringResource(Res.string.sos_summary),
                    leadingIcon = MeshtasticIcons.Warning,
                ) {
                    onNavigate(SettingsRoute.Sos)
                }
                PresenceSettings(options = options, onChange = onChange)
                PresenceManualAsk()
                ListItem(
                    text = stringResource(Res.string.presence_device_switch),
                    supportingText = stringResource(Res.string.presence_summary),
                    leadingIcon = MeshtasticIcons.Info,
                ) {
                    onNavigate(SettingsRoute.User)
                }
                ListItem(
                    text = stringResource(Res.string.presence_more_title),
                    supportingText = stringResource(Res.string.presence_more_summary),
                    leadingIcon = MeshtasticIcons.Info,
                ) {}
            }

            // Real settings that live here (not just links elsewhere): looks, the widget and the statistics sync.
            ExpressiveSection(title = stringResource(Res.string.mt_sw_features_own)) {
                WidgetStatsSettingItem()
                DatabaseSyncSettingItems(
                    autoEnabled = dbStatsSyncEnabled,
                    onAutoChange = { settingsViewModel.setDbStatsSyncEnabled(it) },
                    syncNow = { settingsViewModel.syncDbStatsNow() },
                )
            }

            // Only where the platform installs it (iPhone and iPad); hidden elsewhere.
            if (AppBackgroundControls.current != null) {
                ExpressiveSection(title = stringResource(Res.string.background_section_title)) {
                    AppBackgroundSettings()
                }
            }

            // Only on the computer app (installed from its own launcher); hidden elsewhere.
            if (AutostartControls.current != null) {
                ExpressiveSection(title = stringResource(Res.string.autostart_title)) { AutostartSettings() }
            }

            // Only on the computer app: one copy of the program by default, a second one only when allowed.
            if (SingleInstanceControls.current != null) {
                ExpressiveSection(title = stringResource(Res.string.single_instance_title)) { SingleInstanceSettings() }
            }

            ExpressiveSection(title = stringResource(Res.string.mt_sw_features_links)) {
                Text(
                    text = stringResource(Res.string.mt_sw_features_links_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                val pickColor = stringResource(Res.string.node_color_pick)
                ListItem(
                    text = stringResource(Res.string.node_color_title),
                    supportingText = stringResource(Res.string.node_color_summary),
                    leadingIcon = MeshtasticIcons.FormatPaint,
                ) {
                    SettingHighlight.show(pickColor)
                    onNavigate(SettingsRoute.Security)
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
                ListItem(text = stringResource(Res.string.local_stats_title), leadingIcon = MeshtasticIcons.Tune) {
                    onNavigate(SettingsRoute.Telemetry)
                }
                ListItem(
                    text = stringResource(Res.string.kb_title),
                    supportingText = stringResource(Res.string.kb_summary),
                    leadingIcon = MeshtasticIcons.HelpOutline,
                ) {
                    onNavigate(SettingsRoute.KnowledgeBase)
                }
                ListItem(text = stringResource(Res.string.debug_panel), leadingIcon = MeshtasticIcons.BugReport) {
                    onNavigate(SettingsRoute.DebugPanel)
                }
                ListItem(text = stringResource(Res.string.console_title), leadingIcon = MeshtasticIcons.Serial) {
                    onNavigate(SettingsRoute.Console)
                }
                // Our backups sit in the radio settings; the row is shown there and marked.
                val backupSettings = stringResource(Res.string.backup_app_settings)
                ListItem(text = backupSettings, leadingIcon = MeshtasticIcons.Upload) {
                    SettingHighlight.show(backupSettings)
                    onNavigate(SettingsRoute.Settings())
                }
                val backupData = stringResource(Res.string.backup_app_database)
                ListItem(text = backupData, leadingIcon = MeshtasticIcons.Upload) {
                    SettingHighlight.show(backupData)
                    onNavigate(SettingsRoute.Settings())
                }
                ListItem(
                    text = stringResource(Res.string.data_notice_settings_title),
                    supportingText = stringResource(Res.string.data_notice_shortcut_summary),
                    leadingIcon = MeshtasticIcons.Info,
                ) {
                    onNavigate(SettingsRoute.About)
                }
            }

            ExpressiveSection(title = stringResource(Res.string.mt_sw_group_help)) {
                WhatsNewEntry()
                ListItem(
                    text = stringResource(Res.string.mt_sw_other_title),
                    supportingText = stringResource(Res.string.mt_sw_other_summary),
                    leadingIcon = MeshtasticIcons.Info,
                ) {
                    onNavigate(SettingsRoute.MtSwOtherFeatures)
                }
            }

            // Reports of errors and ideas are ours (our groups), so they sit here and not in the original settings.
            ReportIssuesSection()

            SupportProjectSection()
        }
    }
}

/**
 * "Smart features": the assistant, the message and text translator and voice dictation in one place, all of them
 * working on the device itself.
 */
@Composable
fun SmartFeaturesScreen(onBack: () -> Unit, onNavigate: (Route) -> Unit) {
    FloatingBarUnderlap()
    Scaffold(
        topBar = {
            MainAppBar(
                sosScreen = SosBarScreen.SETTINGS,
                title = stringResource(Res.string.smart_features_title),
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
                .verticalScrollWithBar(rememberScrollState())
                .padding(16.dp)
                .floatingBarEndPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.smart_features_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        }
    }
}

/** An information page: where the MT_SW features that live next to a node (not in Settings) are, and what they do. */
@Composable
fun MtSwOtherFeaturesScreen(onBack: () -> Unit, onNavigate: (Route) -> Unit) {
    FloatingBarUnderlap()
    Scaffold(
        topBar = {
            MainAppBar(
                sosScreen = SosBarScreen.SETTINGS,
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
            modifier =
            Modifier.padding(paddingValues)
                .verticalScrollWithBar(rememberScrollState())
                .padding(16.dp)
                .floatingBarEndPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(Res.string.mt_sw_other_intro), style = MaterialTheme.typography.bodyMedium)
            OtherFeature(Res.string.mt_sw_other_lna_title, Res.string.mt_sw_other_lna_body)
            OtherFeature(Res.string.mt_sw_other_polarity_title, Res.string.mt_sw_other_polarity_body)
            OtherFeature(Res.string.mt_sw_other_gpio_title, Res.string.mt_sw_other_gpio_body)
            OtherFeature(Res.string.mt_sw_other_remote_lists_title, Res.string.mt_sw_other_remote_lists_body)
            OtherFeature(Res.string.mt_sw_other_contacts_title, Res.string.mt_sw_other_contacts_body)
            OtherFeature(Res.string.mt_sw_other_calls_title, Res.string.mt_sw_other_calls_body)
            OtherFeature(Res.string.mt_sw_other_archive_title, Res.string.mt_sw_other_archive_body)
            OtherFeature(Res.string.mt_sw_other_sos_title, Res.string.mt_sw_other_sos_body)
            OtherFeature(
                Res.string.mt_sw_other_notification_test_title,
                Res.string.mt_sw_other_notification_test_body,
            )
            OtherFeature(Res.string.mt_sw_other_silence_title, Res.string.mt_sw_other_silence_body)
            OtherFeature(Res.string.mt_sw_other_single_instance_title, Res.string.mt_sw_other_single_instance_body)
            OtherFeature(Res.string.mt_sw_other_availability_title, Res.string.mt_sw_other_availability_body)
            OtherFeature(Res.string.mt_sw_other_coverage_title, Res.string.mt_sw_other_coverage_body)
            OtherFeature(Res.string.mt_sw_other_compare_title, Res.string.mt_sw_other_compare_body)
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

/** The row that opens the list of what is new: since the last stable version, or everything from the start. */
@Composable
private fun WhatsNewEntry() {
    var open by remember { mutableStateOf(false) }
    ListItem(
        text = stringResource(Res.string.whats_new_title),
        supportingText = stringResource(Res.string.whats_new_open_desc),
        leadingIcon = MeshtasticIcons.Info,
    ) {
        open = true
    }
    if (open) {
        WhatsNewDialog(
            // From the settings the whole history is shown at once; "since the last release" is one tap away.
            initialRequest = WhatsNewRequest(full = true, fromRelease = 0),
            onDismiss = { open = false },
        )
    }
}

/**
 * "Look": everything that changes how the app looks, in one place: the theme, size and fonts, simple choices of the
 * look of each part of the app (with the finer settings folded away), the layout of lists and the bottom bar.
 */
@Composable
fun ElementLookScreen(settingsViewModel: SettingsViewModel, onBack: () -> Unit, onNavigate: (Route) -> Unit) {
    val uiOptionsMap by settingsViewModel.uiOptions.collectAsStateWithLifecycle()
    val options = UiOptions(uiOptionsMap)
    val onChange = settingsViewModel::setUiOption
    val importMenuInTopBar by settingsViewModel.importMenuInTopBar.collectAsStateWithLifecycle()
    val floatingNavBar by settingsViewModel.floatingNavBar.collectAsStateWithLifecycle()
    val uiScale by settingsViewModel.uiScale.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle()
    val fontFamily by settingsViewModel.fontFamily.collectAsStateWithLifecycle()
    val fontWeight by settingsViewModel.fontWeightShift.collectAsStateWithLifecycle()
    val fontSize by settingsViewModel.fontSizePercent.collectAsStateWithLifecycle()
    val appIconStyle by settingsViewModel.appIconStyle.collectAsStateWithLifecycle()
    var showUiScalePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    if (showThemePicker) {
        ThemePickerDialog(
            currentMode = currentTheme,
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
    FloatingBarUnderlap()
    Scaffold(
        topBar = {
            MainAppBar(
                sosScreen = SosBarScreen.SETTINGS,
                title = stringResource(Res.string.element_look_title),
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
                .verticalScrollWithBar(rememberScrollState())
                .padding(16.dp)
                .floatingBarEndPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExpressiveSection(title = stringResource(Res.string.look_section_basic)) {
                ElementBasicSettings(options = options, onChange = onChange)
            }
            ExpressiveSection(title = stringResource(Res.string.look_section_theme)) {
                ListItem(
                    text = stringResource(Res.string.theme),
                    supportingText = themeSummary(currentTheme),
                    leadingIcon = MeshtasticIcons.FormatPaint,
                ) {
                    showThemePicker = true
                }
                ListItem(
                    text = stringResource(Res.string.ui_scale),
                    supportingText = uiScaleLabel(uiScale),
                    leadingIcon = MeshtasticIcons.FormatPaint,
                ) {
                    showUiScalePicker = true
                }
                AppIconSettingsItem(current = appIconStyle, onPick = { settingsViewModel.setAppIconStyle(it) })
                FontSettingsItems(
                    family = fontFamily,
                    weight = fontWeight,
                    size = fontSize,
                    onFamily = { settingsViewModel.setFontFamily(it) },
                    onWeight = { settingsViewModel.setFontWeightShift(it) },
                    onSize = { settingsViewModel.setFontSizePercent(it) },
                )
            }
            ExpressiveSection(title = stringResource(Res.string.look_section_layout)) {
                ListItem(
                    text = stringResource(Res.string.node_layout_section_title),
                    leadingIcon = MeshtasticIcons.List,
                ) {
                    onNavigate(SettingsRoute.NodeList)
                }
                SwitchListItem(
                    text = stringResource(Res.string.import_menu_in_top_bar),
                    leadingIcon = MeshtasticIcons.Add,
                    checked = importMenuInTopBar,
                    onClick = { settingsViewModel.setImportMenuInTopBar(!importMenuInTopBar) },
                )
            }
            ExpressiveSection(title = stringResource(Res.string.look_section_bar)) {
                SwitchListItem(
                    text = stringResource(Res.string.floating_nav_bar),
                    leadingIcon = MeshtasticIcons.DisplaySettings,
                    checked = floatingNavBar,
                    onClick = { settingsViewModel.setFloatingNavBar(!floatingNavBar) },
                )
                NavBarBehaviourSettings(options = options, onChange = onChange)
            }
            ExpressiveSection(title = stringResource(Res.string.look_advanced)) {
                ElementAdvancedSettings(options = options, onChange = onChange)
            }
        }
    }
}

/** "Data, logs and diagnostics": collecting and keeping logs, where the app keeps its data, the archive and a test. */
@Composable
fun DataAndLogsScreen(settingsViewModel: SettingsViewModel, onBack: () -> Unit, onNavigate: (Route) -> Unit) {
    val retentionDays by settingsViewModel.meshLogRetentionDays.collectAsStateWithLifecycle()
    FloatingBarUnderlap()
    Scaffold(
        topBar = {
            MainAppBar(
                sosScreen = SosBarScreen.SETTINGS,
                title = stringResource(Res.string.data_logs_title),
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
                .verticalScrollWithBar(rememberScrollState())
                .padding(16.dp)
                .floatingBarEndPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExpressiveSection(title = stringResource(Res.string.app_log_title)) {
                AppLogSettings()
                LogRetentionPreference(
                    retentionDays = retentionDays,
                    onSelected = { settingsViewModel.setMeshLogRetentionDays(it) },
                )
            }

            // All the folders in one place: the app's data and the pictures taken in the app.
            ExpressiveSection(title = stringResource(Res.string.folders_title)) {
                AiStorageRow()
                PhotoFolderSettings()
            }

            ExpressiveSection(title = stringResource(Res.string.archive_title)) {
                ListItem(
                    text = stringResource(Res.string.archive_title),
                    supportingText = stringResource(Res.string.archive_summary),
                    leadingIcon = MeshtasticIcons.Save,
                ) {
                    onNavigate(SettingsRoute.MessageArchive)
                }
            }

            // The three test buttons are right here (the link to the settings page did not lead anywhere useful).
            NotificationTestSection()
        }
    }
}
