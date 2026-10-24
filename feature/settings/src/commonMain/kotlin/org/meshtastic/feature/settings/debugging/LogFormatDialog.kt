/*
 * Copyright (c) 2025 MT_SW contributors
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
package org.meshtastic.feature.settings.debugging

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.sniffer_export_format_json
import org.meshtastic.core.resources.sniffer_export_format_title
import org.meshtastic.core.resources.sniffer_export_format_txt

/** Asks in which form the logs are saved: as text or as JSON. */
@Composable
internal fun LogFormatDialog(onText: () -> Unit, onJson: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sniffer_export_format_title)) },
        confirmButton = { TextButton(onClick = onJson) { Text(stringResource(Res.string.sniffer_export_format_json)) } },
        dismissButton = { TextButton(onClick = onText) { Text(stringResource(Res.string.sniffer_export_format_txt)) } },
    )
}
