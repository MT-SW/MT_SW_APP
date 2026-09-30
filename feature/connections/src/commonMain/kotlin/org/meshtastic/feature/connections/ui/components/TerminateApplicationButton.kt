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

package org.meshtastic.feature.connections.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.terminate_app
import org.meshtastic.core.resources.terminate_app_message
import org.meshtastic.core.ui.component.MeshtasticResourceDialog
import org.meshtastic.core.ui.util.LocalTerminateApplication

/**
 * "Force stop" for the whole app, after a confirmation: disconnects, stops the background service and ends the
 * process. It saves digging through system settings when the app keeps reconnecting to a device that is switched off.
 * Renders nothing on platforms that provide no [LocalTerminateApplication].
 */
@Composable
fun TerminateApplicationButton(modifier: Modifier = Modifier) {
    val terminate = LocalTerminateApplication.current ?: return
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    if (showConfirm) {
        MeshtasticResourceDialog(
            titleRes = Res.string.terminate_app,
            messageRes = Res.string.terminate_app_message,
            onConfirm = {
                showConfirm = false
                terminate()
            },
            onDismiss = { showConfirm = false },
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        onClick = { showConfirm = true },
    ) {
        Text(stringResource(Res.string.terminate_app))
    }
}
