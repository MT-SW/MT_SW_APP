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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

        is CompatUi.Running ->
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
                    }
                },
                // The window cannot be closed by accident; the only way out is to stop the test.
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelCompat() }) {
                        Text(stringResource(Res.string.compat_cancel))
                    }
                },
            )

        is CompatUi.Done ->
            ReportIssuesDialog(
                onDismiss = { viewModel.closeCompat() },
                startNote = stringResource(Res.string.compat_report_note),
                extras = compat.files,
            )
    }
}
