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

package org.meshtastic.feature.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.log.AppLogControls
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_log_folder
import org.meshtastic.core.resources.app_log_folder_clear
import org.meshtastic.core.resources.app_log_folder_none
import org.meshtastic.core.resources.log_folder_app_button
import org.meshtastic.core.resources.log_folder_app_info
import org.meshtastic.core.resources.log_folder_title
import org.meshtastic.core.resources.photo_folder_change
import org.meshtastic.core.ui.component.InfoButton
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Notes
import org.meshtastic.core.ui.theme.StatusColors.StatusYellow
import org.meshtastic.core.ui.util.rememberOpenDocumentTreeLauncher

/** MT_SW: where the log packages are saved, next to the other folders of the app. */
@Composable
fun LogFolderSettings() {
    val control = AppLogControls.current ?: return
    if (!control.canChooseFolder) return
    val chosen by control.chosenFolderName.collectAsStateWithLifecycle()
    val pickFolder = rememberOpenDocumentTreeLauncher { uri ->
        if (uri != null) control.onFolderPicked(uri.toString())
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(imageVector = MeshtasticIcons.Notes, contentDescription = null)
            Text(
                text = stringResource(Res.string.log_folder_title),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            InfoButton(
                Res.string.log_folder_title,
                Res.string.log_folder_app_info,
                tint = MaterialTheme.colorScheme.StatusYellow,
            )
        }
        Text(
            text =
            chosen?.let { stringResource(Res.string.app_log_folder, it) }
                ?: stringResource(Res.string.app_log_folder_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = {
                Logger.i { "Log folder: settings, change of folder" }
                pickFolder()
            },
        ) {
            Text(stringResource(Res.string.photo_folder_change))
        }
        OutlinedButton(onClick = control::useAppFolder) { Text(stringResource(Res.string.log_folder_app_button)) }
        if (chosen != null) {
            OutlinedButton(onClick = control::clearChosenFolder) {
                Text(stringResource(Res.string.app_log_folder_clear))
            }
        }
    }
}
