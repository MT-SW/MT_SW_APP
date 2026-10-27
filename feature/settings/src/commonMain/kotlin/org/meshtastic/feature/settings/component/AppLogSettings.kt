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
package org.meshtastic.feature.settings.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.log.AppLogControls
import org.meshtastic.core.common.log.AppLogLevel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_log_export
import org.meshtastic.core.resources.app_log_export_done
import org.meshtastic.core.resources.app_log_export_failed
import org.meshtastic.core.resources.app_log_level
import org.meshtastic.core.resources.app_log_level_basic
import org.meshtastic.core.resources.app_log_level_detailed
import org.meshtastic.core.resources.app_log_level_full
import org.meshtastic.core.resources.app_log_level_off
import org.meshtastic.core.resources.app_log_open_folder
import org.meshtastic.core.resources.app_log_summary
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.icon.BugReport
import org.meshtastic.core.ui.icon.List
import org.meshtastic.core.ui.icon.MeshtasticIcons

/**
 * The fork's log files: off by default, the level is remembered, and the logs can be packed into one zip for sending.
 * Shown only where the platform has installed its [org.meshtastic.core.common.log.AppLogControl].
 */
@Composable
fun AppLogSettings() {
    val control = AppLogControls.current ?: return
    val level by control.level.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    val failedText = stringResource(Res.string.app_log_export_failed)
    val labels =
        mapOf(
            AppLogLevel.OFF to stringResource(Res.string.app_log_level_off),
            AppLogLevel.BASIC to stringResource(Res.string.app_log_level_basic),
            AppLogLevel.DETAILED to stringResource(Res.string.app_log_level_detailed),
            AppLogLevel.FULL to stringResource(Res.string.app_log_level_full),
        )

    DropDownPreference(
        title = stringResource(Res.string.app_log_level),
        enabled = true,
        items = AppLogLevel.entries.map { it.ordinal.toLong() to labels.getValue(it) },
        selectedItem = level.ordinal.toLong(),
        onItemSelected = { selected: Long -> control.setLevel(AppLogLevel.entries[selected.toInt()]) },
        summary = stringResource(Res.string.app_log_summary),
    )
    ListItem(text = stringResource(Res.string.app_log_export), leadingIcon = MeshtasticIcons.BugReport) {
        scope.launch {
            val where = control.exportForSending()
            message = where?.let { getString(Res.string.app_log_export_done, it) } ?: failedText
        }
    }
    if (control.canOpenFolder) {
        ListItem(text = stringResource(Res.string.app_log_open_folder), leadingIcon = MeshtasticIcons.List) {
            if (!control.openFolder()) message = failedText
        }
    }
    message?.let {
        Text(text = it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
    }
}
