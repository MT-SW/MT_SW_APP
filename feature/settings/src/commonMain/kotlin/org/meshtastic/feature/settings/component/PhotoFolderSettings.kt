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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.photo_folder_auto
import org.meshtastic.core.resources.photo_folder_change
import org.meshtastic.core.resources.photo_folder_current
import org.meshtastic.core.resources.photo_folder_info_1
import org.meshtastic.core.resources.photo_folder_info_2
import org.meshtastic.core.resources.photo_folder_info_title
import org.meshtastic.core.resources.photo_folder_title
import org.meshtastic.core.ui.component.InfoButton
import org.meshtastic.core.ui.theme.StatusColors.StatusYellow
import org.meshtastic.core.ui.util.changeAppPhotoFolder
import org.meshtastic.core.ui.util.currentPhotoFolder
import org.meshtastic.core.ui.util.isDesktopPlatform

/**
 * MT_SW: where the pictures taken with the camera in the app (messages and hikes) are kept. On a phone or tablet the
 * system album "MT_SW" is created by itself; on the computer the folder is chosen at the first picture and can be
 * changed here.
 */
@Composable
fun PhotoFolderSettings() {
    var folder by remember { mutableStateOf(currentPhotoFolder()) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(Res.string.photo_folder_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            InfoButton(
                Res.string.photo_folder_info_title,
                Res.string.photo_folder_info_1,
                Res.string.photo_folder_info_2,
                tint = MaterialTheme.colorScheme.StatusYellow,
            )
        }
        if (isDesktopPlatform) {
            Text(
                text = stringResource(Res.string.photo_folder_current, folder ?: "~/Pictures/MT_SW"),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = {
                    Logger.i { "Folder zdjęć: ustawienia — zmiana folderu" }
                    changeAppPhotoFolder()?.let { folder = it }
                },
            ) {
                Text(stringResource(Res.string.photo_folder_change))
            }
        } else {
            Text(text = stringResource(Res.string.photo_folder_auto), style = MaterialTheme.typography.bodySmall)
        }
    }
}
