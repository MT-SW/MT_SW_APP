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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.AiModelStore
import org.meshtastic.core.repository.AiStorageMove
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ai_storage_choose_title
import org.meshtastic.core.resources.ai_storage_done
import org.meshtastic.core.resources.ai_storage_download_anyway
import org.meshtastic.core.resources.ai_storage_err_busy
import org.meshtastic.core.resources.ai_storage_err_copy
import org.meshtastic.core.resources.ai_storage_err_create
import org.meshtastic.core.resources.ai_storage_err_env
import org.meshtastic.core.resources.ai_storage_err_inside
import org.meshtastic.core.resources.ai_storage_err_other
import org.meshtastic.core.resources.ai_storage_err_same
import org.meshtastic.core.resources.ai_storage_err_space
import org.meshtastic.core.resources.ai_storage_failed
import org.meshtastic.core.resources.ai_storage_first_body
import org.meshtastic.core.resources.ai_storage_first_title
import org.meshtastic.core.resources.ai_storage_keep
import org.meshtastic.core.resources.ai_storage_low_body
import org.meshtastic.core.resources.ai_storage_low_title
import org.meshtastic.core.resources.ai_storage_moving
import org.meshtastic.core.resources.ai_storage_none_body
import org.meshtastic.core.resources.ai_storage_pick
import org.meshtastic.core.resources.ai_storage_restart_first
import org.meshtastic.core.resources.ai_storage_summary
import org.meshtastic.core.resources.app_data_previous_failed
import org.meshtastic.core.resources.app_data_phone_single
import org.meshtastic.core.resources.app_data_scheduled
import org.meshtastic.core.resources.app_data_summary
import org.meshtastic.core.resources.app_data_summary_fixed
import org.meshtastic.core.resources.app_data_summary_phone
import org.meshtastic.core.resources.app_data_title
import org.meshtastic.core.resources.translator_cancel

/** On a computer, a model download that would leave less than this much free space (50 GB) asks first. */
private const val LOW_SPACE_MB = 50L * 1024L

private const val MB_PER_GB = 1024L

/** "812 MB" or "3.4 GB". */
internal fun formatMb(mb: Long): String =
    if (mb >= MB_PER_GB) {
        val tenths = mb * 10 / MB_PER_GB
        "${tenths / 10}.${tenths % 10} GB"
    } else {
        "$mb MB"
    }

/** The reasons the storage code reports (English, short) as a Polish/English text; unknown ones are shown as they are. */
@Composable
private fun reasonText(reason: String): String =
    when {
        reason.startsWith("not enough free space") -> stringResource(Res.string.ai_storage_err_space)
        reason.startsWith("this folder is already used") -> stringResource(Res.string.ai_storage_err_same)
        reason.contains("inside the") || reason.contains("inside of") -> stringResource(Res.string.ai_storage_err_inside)
        reason.startsWith("the folder cannot be created") -> stringResource(Res.string.ai_storage_err_create)
        reason.startsWith("a file was not copied") -> stringResource(Res.string.ai_storage_err_copy)
        reason.contains("MESHTASTIC_DATA_DIR") -> stringResource(Res.string.ai_storage_err_env)
        reason.startsWith("a model is being downloaded") -> stringResource(Res.string.ai_storage_err_busy)
        else -> stringResource(Res.string.ai_storage_err_other, reason)
    }

/**
 * The row "folder for the app's data" in the MT_SW features. It is shown to everyone, on every system.
 *
 * Computers: everything the app keeps (messages, settings, logs, maps, AI models and programs) moves to the chosen
 * folder at the next start. Phone: the Android system keeps the message database in the app's own memory, so only
 * the AI models can be placed elsewhere (the memory or an SD card).
 */
@Composable
internal fun AiStorageRow() {
    val store = koinInject<AiModelStore>()
    val scope = rememberCoroutineScope()
    val info by store.storage.collectAsStateWithLifecycle()
    val move by store.storageMove.collectAsStateWithLifecycle()
    var chooser by remember { mutableStateOf(false) }
    val i = info ?: return
    val computer = i.choices.isEmpty()
    // Always clickable (a phone without an SD card explains in the dialog why there is only one place to choose).
    val canClick = i.canChange && move !is AiStorageMove.Moving
    Column(
        modifier =
        Modifier.fillMaxWidth()
            .clickable(enabled = canClick) {
                if (computer) {
                    scope.launch { store.pickStorageFolder()?.let { store.moveStorage(it) } }
                } else {
                    chooser = true
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(Res.string.app_data_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        val summary =
            when {
                computer && !i.canChange -> stringResource(Res.string.app_data_summary_fixed, i.folder)
                computer && i.movableMb != null ->
                    stringResource(Res.string.app_data_summary, i.folder, formatMb(i.movableMb ?: 0L), formatMb(i.freeMb))
                computer -> stringResource(Res.string.ai_storage_summary, i.folder, formatMb(i.freeMb))
                else -> stringResource(Res.string.app_data_summary_phone, i.folder, formatMb(i.freeMb))
            }
        Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (val m = move) {
            is AiStorageMove.Moving -> {
                Text(stringResource(Res.string.ai_storage_moving), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { m.fraction }, modifier = Modifier.fillMaxWidth())
            }

            is AiStorageMove.Failed ->
                Text(
                    text = stringResource(Res.string.ai_storage_failed, reasonText(m.reason)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )

            else -> Unit
        }
        val next = i.nextFolder
        if (next != null) {
            Text(
                text =
                if (computer) {
                    stringResource(Res.string.app_data_scheduled, next)
                } else {
                    stringResource(Res.string.ai_storage_done)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        i.previousError?.let { error ->
            Text(
                text = stringResource(Res.string.app_data_previous_failed, reasonText(error)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    if (chooser) {
        AlertDialog(
            onDismissRequest = { chooser = false },
            title = { Text(stringResource(Res.string.ai_storage_choose_title)) },
            text = {
                Column {
                    if (i.choices.size < 2) {
                        Text(
                            text = stringResource(Res.string.app_data_phone_single),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    i.choices.forEach { path ->
                        Row(
                            modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    chooser = false
                                    scope.launch { store.moveStorage(path) }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(selected = path == i.folder, onClick = null)
                            Text(path, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooser = false }) { Text(stringResource(Res.string.translator_cancel)) } },
        )
    }
}

/**
 * The first start on a computer: a short question where to keep the app's data. Shown once; the answer can be changed
 * later in the MT_SW features. Everything is moved at the next start (nothing is copied while the app runs).
 */
@Composable
fun StorageFolderFirstRunDialog() {
    val store = koinInject<AiModelStore>()
    val scope = rememberCoroutineScope()
    val ask by store.askStorageFolder.collectAsStateWithLifecycle()
    val info by store.storage.collectAsStateWithLifecycle()
    var scheduledTo by remember { mutableStateOf<String?>(null) }
    val i = info
    if (ask && i != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(Res.string.ai_storage_first_title)) },
            text = { Text(stringResource(Res.string.ai_storage_first_body, i.folder, formatMb(i.freeMb))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val picked = store.pickStorageFolder()
                            if (picked != null) {
                                store.moveStorage(picked)
                                scheduledTo = store.storage.value?.nextFolder
                            }
                            store.storageFolderAsked()
                        }
                    },
                ) {
                    Text(stringResource(Res.string.ai_storage_pick))
                }
            },
            dismissButton = { TextButton(onClick = { store.storageFolderAsked() }) { Text(stringResource(Res.string.ai_storage_keep)) } },
        )
    }
    scheduledTo?.let { target ->
        AlertDialog(
            onDismissRequest = { scheduledTo = null },
            text = { Text(stringResource(Res.string.app_data_scheduled, target)) },
            confirmButton = { TextButton(onClick = { scheduledTo = null }) { Text("OK") } },
        )
    }
}

/**
 * Wraps a model download: on a computer it asks first when the download would leave less than 50 GB free (and says
 * so plainly when the disk cannot hold the model at all), and it holds the download back while a move of the data
 * folder waits for a restart. Returns the function to call instead of starting the download directly.
 */
@Composable
internal fun rememberDownloadGuard(startDownload: (String) -> Unit): (String) -> Unit {
    val store = koinInject<AiModelStore>()
    val info by store.storage.collectAsStateWithLifecycle()
    var asking by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var restartFirst by remember { mutableStateOf(false) }
    val free = info?.freeMb ?: 0L

    asking?.let { (id, after) ->
        val need = free - after
        if (after < 0) {
            AlertDialog(
                onDismissRequest = { asking = null },
                title = { Text(stringResource(Res.string.ai_storage_low_title)) },
                text = { Text(stringResource(Res.string.ai_storage_none_body, formatMb(need), formatMb(free))) },
                confirmButton = { TextButton(onClick = { asking = null }) { Text("OK") } },
            )
        } else {
            AlertDialog(
                onDismissRequest = { asking = null },
                title = { Text(stringResource(Res.string.ai_storage_low_title)) },
                text = { Text(stringResource(Res.string.ai_storage_low_body, formatMb(after), formatMb(need))) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            asking = null
                            startDownload(id)
                        },
                    ) {
                        Text(stringResource(Res.string.ai_storage_download_anyway))
                    }
                },
                dismissButton = { TextButton(onClick = { asking = null }) { Text(stringResource(Res.string.translator_cancel)) } },
            )
        }
    }
    if (restartFirst) {
        AlertDialog(
            onDismissRequest = { restartFirst = false },
            text = { Text(stringResource(Res.string.ai_storage_restart_first)) },
            confirmButton = { TextButton(onClick = { restartFirst = false }) { Text("OK") } },
        )
    }

    return { id ->
        val current = info
        if (current?.nextFolder != null) {
            restartFirst = true
        } else {
            val after = store.freeSpaceAfterMb(id)
            val computer = current != null && current.choices.isEmpty()
            if (computer && after != null && after < LOW_SPACE_MB) asking = id to after else startDownload(id)
        }
        Unit
    }
}
