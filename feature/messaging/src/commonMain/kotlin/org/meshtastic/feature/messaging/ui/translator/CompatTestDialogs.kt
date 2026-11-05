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
package org.meshtastic.feature.messaging.ui.translator

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.compat_cancel
import org.meshtastic.core.resources.compat_dictation_body_download
import org.meshtastic.core.resources.compat_dictation_body_have
import org.meshtastic.core.resources.compat_dictation_no
import org.meshtastic.core.resources.compat_dictation_title
import org.meshtastic.core.resources.compat_dictation_yes
import org.meshtastic.core.resources.compat_offer_body
import org.meshtastic.core.resources.compat_offer_no
import org.meshtastic.core.resources.compat_offer_title
import org.meshtastic.core.resources.compat_offer_yes
import org.meshtastic.core.resources.compat_report_note
import org.meshtastic.core.resources.compat_running_title
import org.meshtastic.core.resources.compat_step_dictation
import org.meshtastic.core.resources.compat_step_graphics
import org.meshtastic.core.resources.compat_step_hardware
import org.meshtastic.core.resources.compat_step_question
import org.meshtastic.core.resources.compat_tip_1
import org.meshtastic.core.resources.compat_tip_10
import org.meshtastic.core.resources.compat_tip_2
import org.meshtastic.core.resources.compat_tip_3
import org.meshtastic.core.resources.compat_tip_4
import org.meshtastic.core.resources.compat_tip_5
import org.meshtastic.core.resources.compat_tip_6
import org.meshtastic.core.resources.compat_tip_7
import org.meshtastic.core.resources.compat_tip_8
import org.meshtastic.core.resources.compat_tip_9
import org.meshtastic.core.resources.compat_tips_title
import org.meshtastic.core.ui.component.ReportIssuesDialog
import org.meshtastic.feature.messaging.translation.AssistantViewModel
import org.meshtastic.feature.messaging.translation.CompatStep
import org.meshtastic.feature.messaging.translation.CompatUi

/** The offer, the progress and the report window (with the finished package) of the assistant's compatibility test. */
@Composable
fun CompatTestDialogs(compat: CompatUi, viewModel: AssistantViewModel) {
    when (compat) {
        CompatUi.Idle -> Unit

        CompatUi.Asking ->
            AlertDialog(
                onDismissRequest = { viewModel.declineCompat() },
                title = { Text(stringResource(Res.string.compat_offer_title)) },
                // Long text: scrollable, so nothing is cut off on a small screen.
                text = {
                    Text(
                        text = stringResource(Res.string.compat_offer_body),
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmCompatInfo() }) {
                        Text(stringResource(Res.string.compat_offer_yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.declineCompat() }) {
                        Text(stringResource(Res.string.compat_offer_no))
                    }
                },
            )

        is CompatUi.AskingDictation ->
            AlertDialog(
                onDismissRequest = { viewModel.closeCompat() },
                title = { Text(stringResource(Res.string.compat_dictation_title)) },
                text = {
                    Text(
                        text =
                        stringResource(
                            if (compat.offer.onDevice) {
                                Res.string.compat_dictation_body_have
                            } else {
                                Res.string.compat_dictation_body_download
                            },
                            compat.offer.name,
                            compat.offer.sizeMb,
                        ),
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.startCompat(withDictation = true) }) {
                        Text(stringResource(Res.string.compat_dictation_yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.startCompat(withDictation = false) }) {
                        Text(stringResource(Res.string.compat_dictation_no))
                    }
                },
            )

        is CompatUi.Running -> CompatRunningDialog(compat, onCancel = { viewModel.cancelCompat() })

        is CompatUi.Done ->
            ReportIssuesDialog(
                onDismiss = { viewModel.closeCompat() },
                startNote = stringResource(Res.string.compat_report_note),
                extras = compat.files,
            )
    }
}

private val compatTips: List<StringResource> =
    listOf(
        Res.string.compat_tip_1,
        Res.string.compat_tip_2,
        Res.string.compat_tip_3,
        Res.string.compat_tip_4,
        Res.string.compat_tip_5,
        Res.string.compat_tip_6,
        Res.string.compat_tip_7,
        Res.string.compat_tip_8,
        Res.string.compat_tip_9,
        Res.string.compat_tip_10,
    )

private const val TIP_INTERVAL_MS = 6_000L
private const val TIP_MIN_HEIGHT_DP = 96

/** Rotating short descriptions of app features, shown while the compatibility test runs. */
@Composable
private fun CompatTips() {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TIP_INTERVAL_MS)
            index = (index + 1) % compatTips.size
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.compat_tips_title), style = MaterialTheme.typography.labelLarge)
        Crossfade(targetState = index, modifier = Modifier.heightIn(min = TIP_MIN_HEIGHT_DP.dp)) { i ->
            Text(stringResource(compatTips[i]), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CompatRunningDialog(compat: CompatUi.Running, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(Res.string.compat_running_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        when (compat.step) {
                            CompatStep.QUESTION -> Res.string.compat_step_question
                            CompatStep.HARDWARE -> Res.string.compat_step_hardware
                            CompatStep.GRAPHICS -> Res.string.compat_step_graphics
                            CompatStep.DICTATION -> Res.string.compat_step_dictation
                        },
                    ),
                )
                if (compat.detail.isNotBlank()) Text(compat.detail)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                CompatTips()
            }
        },
        // The window cannot be closed by accident; the only way out is to stop the test.
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(Res.string.compat_cancel))
            }
        },
    )
}
