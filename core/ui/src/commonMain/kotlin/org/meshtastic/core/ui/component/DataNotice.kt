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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.data_notice_accept
import org.meshtastic.core.resources.data_notice_body
import org.meshtastic.core.resources.data_notice_close
import org.meshtastic.core.resources.data_notice_copy
import org.meshtastic.core.resources.data_notice_decline
import org.meshtastic.core.resources.data_notice_ready
import org.meshtastic.core.resources.data_notice_scroll_hint
import org.meshtastic.core.resources.data_notice_title
import org.meshtastic.core.resources.data_notice_wait
import org.meshtastic.core.resources.data_wipe_back
import org.meshtastic.core.resources.data_wipe_body
import org.meshtastic.core.resources.data_wipe_close_only
import org.meshtastic.core.resources.data_wipe_confirm
import org.meshtastic.core.resources.data_wipe_confirm_wait
import org.meshtastic.core.resources.data_wipe_title
import org.meshtastic.core.ui.util.LocalTerminateApplication
import org.meshtastic.core.ui.util.LocalWipeApplicationData
import org.meshtastic.core.ui.util.createClipEntry

private const val COUNTDOWN_SECONDS = 10
private const val LAYOUT_SETTLE_MS = 500L
private const val TICK_MS = 1_000L
private const val MAX_TEXT_WIDTH_DP = 640

/**
 * Terms of use and data notice shown before anything else. The accept button stays locked until the whole text was
 * scrolled to the end and a 10-second countdown has run out. Declining opens a warning with its own 10-second
 * countdown before everything can be erased; from there the user can also go back to the terms or just close the app.
 */
@Composable
@Suppress("LongMethod")
fun DataNoticeScreen(onAccept: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    var reachedEnd by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(COUNTDOWN_SECONDS) }
    var showDeclineChoice by remember { mutableStateOf(false) }
    val terminate = LocalTerminateApplication.current

    LaunchedEffect(Unit) {
        // Give the layout a moment to settle; a text that fits without scrolling counts as already read.
        delay(LAYOUT_SETTLE_MS)
        snapshotFlow { !scroll.canScrollForward }.first { it }
        reachedEnd = true
    }
    LaunchedEffect(reachedEnd) {
        if (reachedEnd) {
            while (secondsLeft > 0) {
                delay(TICK_MS)
                secondsLeft--
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.weight(1f).widthIn(max = MAX_TEXT_WIDTH_DP.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.data_notice_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.data_notice_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).verticalScroll(scroll),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text =
                    when {
                        !reachedEnd -> stringResource(Res.string.data_notice_scroll_hint)
                        secondsLeft > 0 -> stringResource(Res.string.data_notice_wait, secondsLeft)
                        else -> stringResource(Res.string.data_notice_ready)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showDeclineChoice = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.data_notice_decline))
                    }
                    Button(
                        onClick = onAccept,
                        enabled = reachedEnd && secondsLeft == 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.data_notice_accept))
                    }
                }
            }
        }
    }

    if (showDeclineChoice) {
        DataWipeDialog(
            secondaryLabel = Res.string.data_wipe_close_only,
            onSecondary = { terminate?.invoke() },
            onBack = { showDeclineChoice = false },
        )
    }
}

/**
 * Asks to erase all app data. The confirm button unlocks after 10 seconds; [onSecondary] is the way out (cancel, or
 * just close the app after declining the terms).
 */
@Composable
fun DataWipeDialog(secondaryLabel: StringResource, onSecondary: () -> Unit, onBack: (() -> Unit)? = null) {
    var secondsLeft by remember { mutableIntStateOf(COUNTDOWN_SECONDS) }
    val wipe = LocalWipeApplicationData.current
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(TICK_MS)
            secondsLeft--
        }
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(Res.string.data_wipe_title)) },
        text = {
            Text(
                text = stringResource(Res.string.data_wipe_body),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = { wipe?.invoke() }, enabled = secondsLeft == 0 && wipe != null) {
                Text(
                    if (secondsLeft > 0) {
                        stringResource(Res.string.data_wipe_confirm_wait, secondsLeft)
                    } else {
                        stringResource(Res.string.data_wipe_confirm)
                    },
                )
            }
        },
        dismissButton = {
            Row {
                // Way back for someone who declined and then thought better of it: nothing is erased.
                onBack?.let { TextButton(onClick = it) { Text(stringResource(Res.string.data_wipe_back)) } }
                TextButton(onClick = onSecondary) { Text(stringResource(secondaryLabel)) }
            }
        },
    )
}

/** The terms of use again, for reading later (About screen). */
@Composable
fun DataNoticeReadDialog(onClose: () -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val title = stringResource(Res.string.data_notice_title)
    val body = stringResource(Res.string.data_notice_body)
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(Res.string.data_notice_title)) },
        text = {
            Text(
                text = stringResource(Res.string.data_notice_body),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(Res.string.data_notice_close)) } },
        dismissButton = {
            TextButton(onClick = { scope.launch { clipboard.setClipEntry(createClipEntry("$title\n\n$body")) } }) {
                Text(stringResource(Res.string.data_notice_copy))
            }
        },
    )
}
