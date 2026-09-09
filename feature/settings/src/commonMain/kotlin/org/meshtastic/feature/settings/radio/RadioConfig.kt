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
package org.meshtastic.feature.settings.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.navigation.DiscoveryRoute
import org.meshtastic.core.navigation.FirmwareRoute
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.administration
import org.meshtastic.core.resources.advanced_title
import org.meshtastic.core.resources.backup_restore
import org.meshtastic.core.resources.clean_node_database_title
import org.meshtastic.core.resources.configuration
import org.meshtastic.core.resources.debug_panel
import org.meshtastic.core.resources.device_configuration
import org.meshtastic.core.resources.discovery_local_mesh
import org.meshtastic.core.resources.export_configuration
import org.meshtastic.core.resources.factory_reset
import org.meshtastic.core.resources.firmware_update_title
import org.meshtastic.core.resources.ic_power_settings_new
import org.meshtastic.core.resources.ic_restart_alt
import org.meshtastic.core.resources.ic_restore
import org.meshtastic.core.resources.ic_schedule
import org.meshtastic.core.resources.ic_storage
import org.meshtastic.core.resources.import_configuration
import org.meshtastic.core.resources.message_device_managed
import org.meshtastic.core.resources.module_settings
import org.meshtastic.core.resources.nodedb_reset
import org.meshtastic.core.resources.reboot
import org.meshtastic.core.resources.set_time
import org.meshtastic.core.resources.shutdown
import org.meshtastic.core.resources.sniffer_enabled_summary
import org.meshtastic.core.resources.sniffer_enabled_title
import org.meshtastic.core.resources.sniffer_warning_accept
import org.meshtastic.core.resources.sniffer_warning_cancel
import org.meshtastic.core.resources.sniffer_warning_compatibility
import org.meshtastic.core.resources.sniffer_warning_message
import org.meshtastic.core.resources.sniffer_warning_title
import org.meshtastic.core.resources.tak_server
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.icon.AdminPanelSettings
import org.meshtastic.core.ui.icon.AppSettingsAlt
import org.meshtastic.core.ui.icon.BugReport
import org.meshtastic.core.ui.icon.ChevronRight
import org.meshtastic.core.ui.icon.CleaningServices
import org.meshtastic.core.ui.icon.Download
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PermScanWifi
import org.meshtastic.core.ui.icon.Settings
import org.meshtastic.core.ui.icon.SystemUpdate
import org.meshtastic.core.ui.icon.Upload
import org.meshtastic.feature.settings.component.ExpressiveSection
import org.meshtastic.feature.settings.navigation.ConfigRoute
import org.meshtastic.core.resources.sniffer_log_title
import org.meshtastic.core.ui.icon.Rssi

@Composable
fun RadioConfigItemList(
    state: RadioConfigState,
    isManaged: Boolean,
    isOtaCapable: Boolean = false,
    onRouteClick: (Enum<*>) -> Unit = {},
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
    onNavigate: (Route) -> Unit,
    onSetSnifferEnabled: (Boolean) -> Unit = {},
    onClearResponse: () -> Unit = {},
) {
    val enabled = state.connected && !isManaged
    val snifferLoading = state.responseState.isWaiting()
    // nodemodadmin is cleared at the start of every fresh handshake (MeshConfigFlowManagerImpl.handleMyInfo)
    // and only repopulated if the connected firmware actually streams that module config section during the
    // Stage 1 sync. Its presence — not the firmware version — is therefore the reliable signal that THIS
    // specific connected device (any firmware, not just this fork's) actually has the Sniffer module. Sending
    // the toggle to firmware that never advertised it is what caused the desktop freeze.
    val snifferSupported = state.moduleConfig.nodemodadmin != null

    LaunchedEffect(state.responseState) {
        if (state.responseState is ResponseState.Success || state.responseState is ResponseState.Error) {
            onClearResponse()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ExpressiveSection(title = stringResource(Res.string.configuration)) {
            if (isManaged) {
                ManagedMessage()
            }
            RadioConfigContent(enabled, onRouteClick)
            DeviceConfigContent(enabled, onNavigate)
            ModuleSettingsContent(enabled, onNavigate)
            AdministrationContent(enabled, onNavigate)
        }

        if (state.isLocal) {
            BackupRestoreSection(isManaged, enabled, onImport, onExport)
            AdvancedSection(
                isManaged = isManaged,
                isOtaCapable = isOtaCapable,
                enabled = enabled,
                onNavigate = onNavigate,
                snifferEnabled = state.moduleConfig.nodemodadmin?.sniffer_enabled == true,
                snifferLoading = snifferLoading,
                snifferSupported = snifferSupported,
                onSetSnifferEnabled = onSetSnifferEnabled,
            )
        }
    }
}

@Composable
private fun ColumnScope.RadioConfigContent(enabled: Boolean, onRouteClick: (Enum<*>) -> Unit) {
    ConfigRoute.radioConfigRoutes.forEach {
        ListItem(
            text = stringResource(it.title),
            leadingIcon = it.icon?.let { res -> vectorResource(res) },
            enabled = enabled,
        ) {
            onRouteClick(it)
        }
    }
}

@Composable
private fun ColumnScope.DeviceConfigContent(enabled: Boolean, onNavigate: (Route) -> Unit) {
    ListItem(
        text = stringResource(Res.string.device_configuration),
        leadingIcon = MeshtasticIcons.AppSettingsAlt,
        trailingIcon = MeshtasticIcons.ChevronRight,
        enabled = enabled,
    ) {
        onNavigate(SettingsRoute.DeviceConfiguration)
    }
}

@Composable
private fun ColumnScope.ModuleSettingsContent(enabled: Boolean, onNavigate: (Route) -> Unit) {
    ListItem(
        text = stringResource(Res.string.module_settings),
        leadingIcon = MeshtasticIcons.Settings,
        trailingIcon = MeshtasticIcons.ChevronRight,
        enabled = enabled,
    ) {
        onNavigate(SettingsRoute.ModuleConfiguration)
    }
}

@Composable
private fun BackupRestoreSection(isManaged: Boolean, enabled: Boolean, onImport: () -> Unit, onExport: () -> Unit) {
    ExpressiveSection(title = stringResource(Res.string.backup_restore)) {
        if (isManaged) {
            ManagedMessage()
        }

        ListItem(
            text = stringResource(Res.string.import_configuration),
            leadingIcon = MeshtasticIcons.Download,
            enabled = enabled,
            onClick = onImport,
        )
        ListItem(
            text = stringResource(Res.string.export_configuration),
            leadingIcon = MeshtasticIcons.Upload,
            enabled = enabled,
            onClick = onExport,
        )
    }
}

@Composable
private fun ColumnScope.AdministrationContent(enabled: Boolean, onNavigate: (Route) -> Unit) {
    ListItem(
        text = stringResource(Res.string.administration),
        leadingIcon = MeshtasticIcons.AdminPanelSettings,
        trailingIcon = MeshtasticIcons.ChevronRight,
        leadingIconTint = MaterialTheme.colorScheme.error,
        textColor = MaterialTheme.colorScheme.error,
        trailingIconTint = MaterialTheme.colorScheme.error,
        enabled = enabled,
    ) {
        onNavigate(SettingsRoute.Administration)
    }
}

@Composable
private fun AdvancedSection(
    isManaged: Boolean,
    isOtaCapable: Boolean,
    enabled: Boolean,
    onNavigate: (Route) -> Unit,
    snifferEnabled: Boolean,
    snifferLoading: Boolean,
    snifferSupported: Boolean,
    onSetSnifferEnabled: (Boolean) -> Unit,
) {
    ExpressiveSection(title = stringResource(Res.string.advanced_title)) {
        if (isManaged) {
            ManagedMessage()
        }

        if (isOtaCapable) {
            ListItem(
                text = stringResource(Res.string.firmware_update_title),
                leadingIcon = MeshtasticIcons.SystemUpdate,
                enabled = enabled,
                onClick = { onNavigate(FirmwareRoute.FirmwareUpdate) },
            )
        }

        ListItem(
            text = stringResource(Res.string.clean_node_database_title),
            leadingIcon = MeshtasticIcons.CleaningServices,
            enabled = enabled,
            onClick = { onNavigate(SettingsRoute.CleanNodeDb) },
        )

        ListItem(
            text = stringResource(Res.string.tak_server),
            leadingIcon = MeshtasticIcons.Settings,
            enabled = enabled,
            onClick = { onNavigate(SettingsRoute.TakServer) },
        )

        ListItem(
            text = stringResource(Res.string.discovery_local_mesh),
            leadingIcon = MeshtasticIcons.PermScanWifi,
            enabled = enabled,
            onClick = { onNavigate(DiscoveryRoute.DiscoveryGraph) },
        )

        // Always enabled: the Debug Panel reads app-local logs only — no radio connection,
        // pending config response, or managed-mode restriction applies to it.
        ListItem(
            text = stringResource(Res.string.debug_panel),
            leadingIcon = MeshtasticIcons.BugReport,
            onClick = { onNavigate(SettingsRoute.DebugPanel) },
        )

        var showSnifferWarning by remember { mutableStateOf(false) }

        SwitchPreference(
            title = stringResource(Res.string.sniffer_enabled_title),
            enabled = enabled && !snifferLoading && snifferSupported,
            loading = snifferLoading,
            checked = snifferEnabled,
            onCheckedChange = { checked ->
                if (checked) {
                    showSnifferWarning = true
                } else {
                    onSetSnifferEnabled(false)
                }
            },
            summary =
                if (snifferSupported) {
                    stringResource(Res.string.sniffer_enabled_summary)
                } else {
                    stringResource(Res.string.sniffer_not_supported_summary)
                },
        )

        if (showSnifferWarning) {
            SnifferWarningDialog(
                onConfirm = {
                    showSnifferWarning = false
                    onSetSnifferEnabled(true)
                },
                onDismiss = { showSnifferWarning = false },
            )
        }

        ListItem(
            text = stringResource(Res.string.sniffer_log_title),
            leadingIcon = MeshtasticIcons.Rssi,
            onClick = { onNavigate(SettingsRoute.SnifferLog) },
        )
    }
}

enum class AdminRoute(val icon: DrawableResource, val title: StringResource) {
    SET_TIME(Res.drawable.ic_schedule, Res.string.set_time),
    REBOOT(Res.drawable.ic_restart_alt, Res.string.reboot),
    SHUTDOWN(Res.drawable.ic_power_settings_new, Res.string.shutdown),
    FACTORY_RESET(Res.drawable.ic_restore, Res.string.factory_reset),
    NODEDB_RESET(Res.drawable.ic_storage, Res.string.nodedb_reset),
}

@Composable
private fun ManagedMessage() {
    Text(
        text = stringResource(Res.string.message_device_managed),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun SnifferWarningDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sniffer_warning_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.sniffer_warning_message))
                Text(
                    text = stringResource(Res.string.sniffer_warning_compatibility),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.sniffer_warning_accept)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sniffer_warning_cancel)) } },
    )
}
