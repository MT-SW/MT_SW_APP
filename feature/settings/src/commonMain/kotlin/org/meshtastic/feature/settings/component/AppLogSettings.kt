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

package org.meshtastic.feature.settings.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.log.AppLogControls
import org.meshtastic.core.common.log.AppLogLevel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_log_choose_folder
import org.meshtastic.core.resources.app_log_folder
import org.meshtastic.core.resources.app_log_folder_clear
import org.meshtastic.core.resources.app_log_folder_none
import org.meshtastic.core.resources.app_log_level
import org.meshtastic.core.resources.app_log_open_failed
import org.meshtastic.core.resources.app_log_open_folder
import org.meshtastic.core.resources.app_log_recent
import org.meshtastic.core.resources.app_log_recent_summary
import org.meshtastic.core.resources.app_log_summary
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.icon.List
import org.meshtastic.core.ui.icon.MeshtasticIcons

/**
 * The fork's log switch: off by default; once on, the app writes everything it does to log files on its own (on a phone
 * a copy also lands in the Downloads folder), so there is nothing to export by hand. Shown only where the platform has
 * installed its [org.meshtastic.core.common.log.AppLogControl].
 */
@Composable
fun AppLogSettings() {
    val control = AppLogControls.current ?: return
    val level by control.level.collectAsStateWithLifecycle()
    val recent by control.recent.collectAsStateWithLifecycle()
    var failed by remember { mutableStateOf(false) }

    // The basic log of the last 24 hours, kept by the app on its own (on by default), so a problem can be reported with
    // it.
    if (control.canMakeReport) {
        SwitchPreference(
            title = stringResource(Res.string.app_log_recent),
            summary = stringResource(Res.string.app_log_recent_summary),
            checked = recent,
            enabled = true,
            onCheckedChange = { control.setRecent(it) },
        )
    }

    SwitchPreference(
        title = stringResource(Res.string.app_log_level),
        summary = stringResource(Res.string.app_log_summary),
        checked = level != AppLogLevel.OFF,
        enabled = true,
        onCheckedChange = { on -> control.setLevel(if (on) AppLogLevel.FULL else AppLogLevel.OFF) },
    )
    control.folderHint?.let { hint ->
        Text(
            text = stringResource(Res.string.app_log_folder, hint),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    if (control.canChooseFolder) {
        val chosen by control.chosenFolderName.collectAsStateWithLifecycle()
        ListItem(text = stringResource(Res.string.app_log_choose_folder), leadingIcon = MeshtasticIcons.List) {
            control.chooseFolder()
        }
        Text(
            text =
            chosen?.let { stringResource(Res.string.app_log_folder, it) }
                ?: stringResource(Res.string.app_log_folder_none),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (chosen != null) {
            ListItem(text = stringResource(Res.string.app_log_folder_clear), leadingIcon = MeshtasticIcons.List) {
                control.clearChosenFolder()
            }
        }
    }
    if (control.canOpenFolder) {
        ListItem(text = stringResource(Res.string.app_log_open_folder), leadingIcon = MeshtasticIcons.List) {
            failed = !control.openFolder()
        }
    }
    if (failed) {
        Text(
            text = stringResource(Res.string.app_log_open_failed),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
