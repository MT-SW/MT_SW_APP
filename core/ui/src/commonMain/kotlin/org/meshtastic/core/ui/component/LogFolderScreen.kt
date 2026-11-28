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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.log.AppLogControl
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.log_folder_required_body
import org.meshtastic.core.resources.log_folder_required_button
import org.meshtastic.core.resources.log_folder_required_title
import org.meshtastic.core.ui.util.rememberOpenDocumentTreeLauncher

private const val MAX_TEXT_WIDTH_DP = 640

/**
 * Shown at start, after the terms, until a folder for the logs is chosen (on every system). The app cannot be used
 * before; the logs of the last 24 hours and the full log are saved there.
 */
@Composable
fun LogFolderScreen(control: AppLogControl, modifier: Modifier = Modifier) {
    val pick = rememberOpenDocumentTreeLauncher { uri -> if (uri != null) control.onFolderPicked(uri.toString()) }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = MAX_TEXT_WIDTH_DP.dp).verticalScrollWithBar(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.log_folder_required_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.log_folder_required_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = pick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.log_folder_required_button))
                }
            }
        }
    }
}

/** True while the log folder still has to be chosen. */
@Composable
fun logFolderMissing(control: AppLogControl?): Boolean {
    if (control == null || !control.folderRequired) return false
    val chosen by control.chosenFolderName.collectAsState()
    return chosen == null
}
