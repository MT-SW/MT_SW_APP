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

package org.meshtastic.feature.settings.debugging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.debug_default_search
import org.meshtastic.core.resources.debug_device_logs_empty
import org.meshtastic.core.resources.debug_logs_export
import org.meshtastic.core.resources.debug_logs_export_warning
import org.meshtastic.core.ui.component.FastScrollSidebar
import org.meshtastic.core.ui.component.MeshtasticResourceDialog
import org.meshtastic.core.ui.icon.FileDownload
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.isDesktopPlatform
import org.meshtastic.feature.settings.debugging.DebugViewModel.UiMeshLog

/** Message type under which the firmware's log records (its serial console output) are stored in the mesh log. */
internal const val DEVICE_LOG_MESSAGE_TYPE = "LogRecord"

/** The device's own log lines ("D/source: message"), newest first, filtered by a case-insensitive [query]. */
internal fun deviceLogLines(logs: List<UiMeshLog>, query: String): List<String> = logs
    .asSequence()
    .filter { it.messageType == DEVICE_LOG_MESSAGE_TYPE }
    .map { "${it.formattedReceivedDate}  ${it.deviceLogLine ?: it.logMessage}" }
    .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
    .toList()

@Composable
private fun deviceLogLineColor(line: String): Color {
    // Lines look like "<date>  L/source: message", where L is the level letter.
    val level = Regex("\\s([TDIWEC])/").find(line)?.groupValues?.getOrNull(1)
    return when (level) {
        "E",
        "C",
        -> MaterialTheme.colorScheme.error

        "W" -> MaterialTheme.colorScheme.tertiary

        else -> MaterialTheme.colorScheme.onSurface
    }
}

/** Live view of the log the radio streams to the app (the same text as on its serial console). */
@Suppress("LongMethod")
@Composable
fun DeviceLogsContent(logs: List<UiMeshLog>, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val lines = remember(logs, query) { deviceLogLines(logs, query) }

    val export = rememberLogExporter { redactText(deviceLogLines(logs, "").reversed().joinToString("\n")) }
    var showExportWarning by rememberSaveable { mutableStateOf(false) }
    if (showExportWarning) {
        MeshtasticResourceDialog(
            titleRes = Res.string.debug_logs_export,
            messageRes = Res.string.debug_logs_export_warning,
            onConfirm = {
                showExportWarning = false
                export(timestampedExportName("meshtastic_device_log"))
            },
            onDismiss = { showExportWarning = false },
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(Res.string.debug_default_search)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showExportWarning = true }) {
                Icon(MeshtasticIcons.FileDownload, contentDescription = stringResource(Res.string.debug_logs_export))
            }
        }
        if (lines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.debug_device_logs_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = if (isDesktopPlatform) 20.dp else 0.dp),
                    ) {
                        items(lines) { line ->
                            Text(
                                text = line,
                                color = deviceLogLineColor(line),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                FastScrollSidebar(
                    listState = listState,
                    itemCount = lines.size,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
                )
            }
        }
    }
}
