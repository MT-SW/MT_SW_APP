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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.db_sync_auto
import org.meshtastic.core.resources.db_sync_done
import org.meshtastic.core.resources.db_sync_failed
import org.meshtastic.core.resources.db_sync_info
import org.meshtastic.core.resources.db_sync_now
import org.meshtastic.core.resources.db_sync_summary
import org.meshtastic.core.resources.db_sync_title
import org.meshtastic.core.resources.db_sync_working
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Refresh

/**
 * Statistics sync between the device databases of this phone: the switch for the automatic sync and a scrollable
 * information window with the button "Synchronizuj teraz". Only nodes known to both databases are synchronized.
 */
@Composable
fun DatabaseSyncSettingItems(
    autoEnabled: Boolean,
    onAutoChange: (Boolean) -> Unit,
    syncNow: suspend () -> Int,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val done = stringResource(Res.string.db_sync_done)
    val failed = stringResource(Res.string.db_sync_failed)
    if (open) {
        MeshtasticDialog(
            title = stringResource(Res.string.db_sync_title),
            onDismiss = { open = false },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(Res.string.db_sync_info))
                    result?.let { Text("\n$it") }
                    if (busy) Text("\n" + stringResource(Res.string.db_sync_working))
                }
            },
            confirmText = stringResource(Res.string.db_sync_now),
            onConfirm = {
                if (!busy) {
                    busy = true
                    scope.launch {
                        result =
                            try {
                                done.replace("%1\$d", syncNow().toString())
                            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                                failed
                            }
                        busy = false
                    }
                }
            },
            dismissText = stringResource(Res.string.close),
        )
    }
    SwitchListItem(
        modifier = modifier,
        text = stringResource(Res.string.db_sync_auto),
        leadingIcon = MeshtasticIcons.Refresh,
        checked = autoEnabled,
        onClick = { onAutoChange(!autoEnabled) },
    )
    ListItem(
        text = stringResource(Res.string.db_sync_title),
        supportingText = stringResource(Res.string.db_sync_summary),
        leadingIcon = MeshtasticIcons.Refresh,
    ) {
        result = null
        open = true
    }
}
