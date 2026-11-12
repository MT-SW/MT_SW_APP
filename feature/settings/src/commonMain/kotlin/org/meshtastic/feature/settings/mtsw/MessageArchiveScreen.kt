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
@file:OptIn(ExperimentalLayoutApi::class)
@file:Suppress("LongMethod", "ModifierMissing")

package org.meshtastic.feature.settings.mtsw

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.repository.MessageArchiveConfig
import org.meshtastic.core.repository.MessageArchivePrefs
import org.meshtastic.core.repository.MessageArchiveRestoreResult
import org.meshtastic.core.repository.MessageArchiver
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.archive_choose_folder
import org.meshtastic.core.resources.archive_encrypt
import org.meshtastic.core.resources.archive_encrypt_hint
import org.meshtastic.core.resources.archive_encrypt_info
import org.meshtastic.core.resources.archive_encrypt_on
import org.meshtastic.core.resources.archive_encrypt_remove
import org.meshtastic.core.resources.archive_encrypt_save
import org.meshtastic.core.resources.archive_password
import org.meshtastic.core.resources.archive_restore
import org.meshtastic.core.resources.archive_restore_button
import org.meshtastic.core.resources.archive_restore_csv_only
import org.meshtastic.core.resources.archive_restore_hint
import org.meshtastic.core.resources.archive_restore_info
import org.meshtastic.core.resources.archive_restore_missing_channels
import org.meshtastic.core.resources.archive_restore_preview_button
import org.meshtastic.core.resources.archive_restore_preview_result
import org.meshtastic.core.resources.archive_restore_result
import org.meshtastic.core.resources.archive_enabled
import org.meshtastic.core.resources.archive_folder
import org.meshtastic.core.resources.archive_folder_chosen
import org.meshtastic.core.resources.archive_folder_none
import org.meshtastic.core.resources.archive_format
import org.meshtastic.core.resources.archive_format_csv
import org.meshtastic.core.resources.archive_format_markdown
import org.meshtastic.core.resources.archive_format_text
import org.meshtastic.core.resources.archive_grouping
import org.meshtastic.core.resources.archive_grouping_conversation
import org.meshtastic.core.resources.archive_grouping_day
import org.meshtastic.core.resources.archive_interval
import org.meshtastic.core.resources.archive_interval_minutes
import org.meshtastic.core.resources.archive_intro
import org.meshtastic.core.resources.archive_last_failed
import org.meshtastic.core.resources.archive_last_ok
import org.meshtastic.core.resources.archive_never
import org.meshtastic.core.resources.archive_now
import org.meshtastic.core.resources.archive_title
import org.meshtastic.core.resources.mt_sw_other_archive_body
import org.meshtastic.core.ui.component.EditPasswordPreference
import org.meshtastic.core.ui.component.InfoButton
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Save
import org.meshtastic.core.ui.util.rememberOpenDocumentTreeLauncher
import org.meshtastic.feature.settings.component.ExpressiveSection

private val INTERVALS = listOf(5, 15, 30, 60, 180)
private const val MAX_PASSWORD_LENGTH = 128

/** The automatic message archive: where to, in which format, how often, plus a button to write it right now. */
@Composable
fun MessageArchiveScreen(onBack: () -> Unit) {
    val prefs = koinInject<MessageArchivePrefs>()
    val archiver = koinInject<MessageArchiver>()
    val config by prefs.config.collectAsStateWithLifecycle()
    val state by archiver.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var password by rememberSaveable { mutableStateOf("") }
    var restoring by rememberSaveable { mutableStateOf(false) }
    var restoreResult by remember { mutableStateOf<MessageArchiveRestoreResult?>(null) }
    val pickFolder =
        rememberOpenDocumentTreeLauncher { uri ->
            if (uri != null) prefs.setConfig(config.copy(folder = uri.toString(), enabled = true))
        }

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.archive_title),
                canNavigateUp = true,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = {
                    InfoButton(
                        Res.string.archive_title,
                        Res.string.mt_sw_other_archive_body,
                        Res.string.archive_encrypt_info,
                        Res.string.archive_restore_info,
                    )
                },
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(Res.string.archive_intro), style = MaterialTheme.typography.bodyMedium)

            ExpressiveSection(title = stringResource(Res.string.archive_folder)) {
                ListItem(
                    text = stringResource(Res.string.archive_choose_folder),
                    supportingText = folderLabel(config.folder),
                    leadingIcon = MeshtasticIcons.Save,
                ) {
                    pickFolder()
                }
                SwitchListItem(
                    checked = config.enabled && config.folder.isNotBlank(),
                    text = stringResource(Res.string.archive_enabled),
                    enabled = config.folder.isNotBlank(),
                    onClick = { prefs.setConfig(config.copy(enabled = !config.enabled)) },
                )
            }

            ExpressiveSection(title = stringResource(Res.string.archive_format)) {
                ChipRow {
                    FORMATS.forEach { (format, label) ->
                        FilterChip(
                            selected = config.format == format,
                            onClick = { prefs.setConfig(config.copy(format = format)) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
            }

            ExpressiveSection(title = stringResource(Res.string.archive_grouping)) {
                ChipRow {
                    FilterChip(
                        selected = !config.perConversation,
                        onClick = { prefs.setConfig(config.copy(perConversation = false)) },
                        label = { Text(stringResource(Res.string.archive_grouping_day)) },
                    )
                    FilterChip(
                        selected = config.perConversation,
                        onClick = { prefs.setConfig(config.copy(perConversation = true)) },
                        label = { Text(stringResource(Res.string.archive_grouping_conversation)) },
                    )
                }
            }

            ExpressiveSection(title = stringResource(Res.string.archive_interval)) {
                ChipRow {
                    INTERVALS.forEach { minutes ->
                        FilterChip(
                            selected = config.intervalMinutes == minutes,
                            onClick = { prefs.setConfig(config.copy(intervalMinutes = minutes)) },
                            label = { Text(stringResource(Res.string.archive_interval_minutes, minutes)) },
                        )
                    }
                }
            }

            ExpressiveSection(title = stringResource(Res.string.archive_encrypt)) {
                Text(
                    text =
                    stringResource(
                        if (config.encrypted) Res.string.archive_encrypt_on else Res.string.archive_encrypt_hint,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                EditPasswordPreference(
                    title = stringResource(Res.string.archive_password),
                    value = password,
                    maxSize = MAX_PASSWORD_LENGTH,
                    enabled = true,
                    keyboardActions = KeyboardActions {},
                    onValueChanged = { password = it },
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            prefs.setConfig(config.copy(password = password))
                            password = ""
                        },
                        enabled = password.isNotEmpty(),
                    ) {
                        Text(stringResource(Res.string.archive_encrypt_save))
                    }
                    if (config.encrypted) {
                        OutlinedButton(onClick = { prefs.setConfig(config.copy(password = "")) }) {
                            Text(stringResource(Res.string.archive_encrypt_remove))
                        }
                    }
                }
            }

            ExpressiveSection(title = stringResource(Res.string.archive_restore)) {
                Text(
                    text = stringResource(Res.string.archive_restore_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (config.format != MessageArchiveConfig.FORMAT_CSV) {
                    Text(
                        text = stringResource(Res.string.archive_restore_csv_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                OutlinedButton(
                    onClick = {
                        restoring = true
                        scope.launch {
                            restoreResult = archiver.restore(preview = true)
                            restoring = false
                        }
                    },
                    enabled = config.folder.isNotBlank() && !restoring,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(Res.string.archive_restore_preview_button))
                }
                Button(
                    onClick = {
                        restoring = true
                        scope.launch {
                            restoreResult = archiver.restore()
                            restoring = false
                        }
                    },
                    enabled = config.folder.isNotBlank() && !restoring,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(Res.string.archive_restore_button))
                }
                restoreResult?.let { result ->
                    val resultText =
                        if (result.preview) {
                            Res.string.archive_restore_preview_result
                        } else {
                            Res.string.archive_restore_result
                        }
                    Text(
                        text =
                        stringResource(
                            resultText,
                            result.restored,
                            result.alreadyThere,
                            result.noChannel,
                            result.unreadable,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    if (result.missingChannels.isNotEmpty()) {
                        Text(
                            text =
                            stringResource(
                                Res.string.archive_restore_missing_channels,
                                result.missingChannels.joinToString(", "),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            Button(
                onClick = { scope.launch { archiver.archiveNow() } },
                enabled = config.folder.isNotBlank() && !state.running,
                modifier = Modifier.padding(horizontal = 0.dp),
            ) {
                Text(stringResource(Res.string.archive_now))
            }
            Text(
                text =
                when {
                    state.lastRunMillis == 0L -> stringResource(Res.string.archive_never)
                    state.lastFailed -> stringResource(Res.string.archive_last_failed)
                    else ->
                        stringResource(
                            Res.string.archive_last_ok,
                            DateFormatter.formatDateTime(state.lastRunMillis),
                            state.lastFileCount,
                        )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val FORMATS =
    listOf(
        MessageArchiveConfig.FORMAT_TEXT to Res.string.archive_format_text,
        MessageArchiveConfig.FORMAT_CSV to Res.string.archive_format_csv,
        MessageArchiveConfig.FORMAT_MARKDOWN to Res.string.archive_format_markdown,
    )

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

/** The folder in words: its last name when the address carries one, otherwise just that one is chosen. */
@Composable
private fun folderLabel(folder: String): String {
    if (folder.isBlank()) return stringResource(Res.string.archive_folder_none)
    if (folder.startsWith("bookmark:")) return stringResource(Res.string.archive_folder_chosen)
    val decoded = folder.replace("%3A", ":").replace("%2F", "/").trimEnd('/')
    return decoded.substringAfterLast('/').substringAfterLast(':').ifBlank { decoded }
}
