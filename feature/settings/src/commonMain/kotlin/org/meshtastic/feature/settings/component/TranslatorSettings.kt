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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.di.ApplicationCoroutineScope
import org.meshtastic.core.repository.TranslatorModelManager
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.translator_cancel
import org.meshtastic.core.resources.translator_cancel_download
import org.meshtastic.core.resources.translator_delete
import org.meshtastic.core.resources.translator_download
import org.meshtastic.core.resources.translator_downloading
import org.meshtastic.core.resources.translator_failed
import org.meshtastic.core.resources.translator_ready
import org.meshtastic.core.resources.translator_summary
import org.meshtastic.core.resources.translator_title
import org.meshtastic.core.resources.translator_warning_body
import org.meshtastic.core.resources.translator_warning_enable
import org.meshtastic.core.resources.translator_warning_title
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.SwitchPreference

/**
 * The switch and model controls of the optional offline translator. It is off by default; switching it on shows a
 * warning about the download size and RAM use, and only then starts the download.
 */
@Composable
internal fun TranslatorSettings() {
    val uiPrefs = koinInject<UiPrefs>()
    val manager = koinInject<TranslatorModelManager>()
    val appScope = koinInject<ApplicationCoroutineScope>()
    val enabled by uiPrefs.translatorEnabled.collectAsStateWithLifecycle()
    val state by manager.state.collectAsStateWithLifecycle()
    var showWarning by remember { mutableStateOf(false) }

    SwitchPreference(
        title = stringResource(Res.string.translator_title),
        summary = stringResource(Res.string.translator_summary),
        checked = enabled,
        enabled = true,
        onCheckedChange = { on ->
            if (on) {
                showWarning = true
            } else {
                uiPrefs.setTranslatorEnabled(false)
            }
        },
    )

    if (enabled) {
        when (val s = state) {
            TranslatorModelState.NotDownloaded ->
                ListItem(
                    text = stringResource(Res.string.translator_download, manager.approximateSizeMb),
                    trailingIcon = null,
                ) {
                    appScope.launch { manager.download() }
                }

            is TranslatorModelState.Downloading ->
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(Res.string.translator_downloading, (s.progress * 100).toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { manager.cancelDownload() }) {
                        Text(stringResource(Res.string.translator_cancel_download))
                    }
                }

            TranslatorModelState.Ready -> {
                Text(
                    text = stringResource(Res.string.translator_ready),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ListItem(text = stringResource(Res.string.translator_delete), trailingIcon = null) {
                    appScope.launch { manager.deleteModel() }
                }
            }

            is TranslatorModelState.Failed ->
                ListItem(text = stringResource(Res.string.translator_failed), trailingIcon = null) {
                    appScope.launch { manager.download() }
                }
        }
    }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            title = { Text(stringResource(Res.string.translator_warning_title)) },
            text = {
                Text(
                    stringResource(
                        Res.string.translator_warning_body,
                        manager.approximateSizeMb,
                        manager.approximateRamMb,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWarning = false
                        uiPrefs.setTranslatorEnabled(true)
                        if (state != TranslatorModelState.Ready) appScope.launch { manager.download() }
                    },
                ) {
                    Text(stringResource(Res.string.translator_warning_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarning = false }) { Text(stringResource(Res.string.translator_cancel)) }
            },
        )
    }
}
