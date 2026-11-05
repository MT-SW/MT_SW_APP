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
@file:Suppress("LongMethod", "ModifierMissing")

package org.meshtastic.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.log.AppLogControls
import org.meshtastic.core.common.log.ReportTarget
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.report_done_computer
import org.meshtastic.core.resources.report_done_phone
import org.meshtastic.core.resources.report_failed
import org.meshtastic.core.resources.report_issues_discord_summary
import org.meshtastic.core.resources.report_issues_headline
import org.meshtastic.core.resources.report_issues_intro
import org.meshtastic.core.resources.report_issues_local_mesh
import org.meshtastic.core.resources.report_issues_signal_summary
import org.meshtastic.core.resources.report_issues_tips
import org.meshtastic.core.resources.report_issues_whatsapp_summary
import org.meshtastic.core.resources.report_note_hint
import org.meshtastic.core.resources.report_send
import org.meshtastic.core.resources.report_send_to
import org.meshtastic.core.resources.report_sending
import org.meshtastic.core.resources.report_with_logs
import org.meshtastic.core.resources.report_with_logs_hint
import org.meshtastic.core.ui.icon.BugReport
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.rememberOpenUrl

private const val DISCORD_URL = "https://discord.gg/EJqUHpvVKN"
private const val WHATSAPP_URL = "https://chat.whatsapp.com/HNrO14NTKTx0xiQGKDVX7N"
private const val SIGNAL_URL =
    "https://signal.group/#CjQKIJrjVtcmsLIgJre9nmpUlTwLMpZnvvIXZUH3LSC9yGDtEhCOjGRHVeloUHeC9rTvL--l"

private fun ReportTarget.url(): String = when (this) {
    ReportTarget.DISCORD -> DISCORD_URL
    ReportTarget.WHATSAPP -> WHATSAPP_URL
    ReportTarget.SIGNAL -> SIGNAL_URL
}

private fun ReportTarget.label(): String = when (this) {
    ReportTarget.DISCORD -> "Discord"
    ReportTarget.WHATSAPP -> "WhatsApp"
    ReportTarget.SIGNAL -> "Signal"
}

/**
 * The report window. The person picks one of the three places (Discord, WhatsApp, Signal), may add a note and keeps the
 * switch for the logs of the last 24 hours; "Send" builds the package and hands it to the app of that place (on a
 * phone, with the group's link in the text) or opens the folder with it and the group (on a computer). [extras] are
 * added to the package (for example the results of a test) and [startNote] fills the note.
 */
@Composable
fun ReportIssuesDialog(
    onDismiss: () -> Unit,
    startNote: String = "",
    extras: List<Pair<String, ByteArray>> = emptyList(),
) {
    val openUrl = rememberOpenUrl()
    val scope = rememberCoroutineScope()
    val control = AppLogControls.current
    var target by rememberSaveable { mutableStateOf(ReportTarget.WHATSAPP) }
    var note by rememberSaveable { mutableStateOf(startNote) }
    var withLogs by rememberSaveable { mutableStateOf(true) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var failed by rememberSaveable { mutableStateOf(false) }
    MeshtasticDialog(
        title = stringResource(Res.string.report_issues_headline),
        icon = MeshtasticIcons.BugReport,
        confirmText = stringResource(Res.string.report_send),
        onConfirm =
        if (busy) {
            null
        } else {
            {
                busy = true
                failed = false
                result = null
                scope.launch {
                    val report = control?.takeIf { it.canMakeReport }?.buildReport(note, withLogs, extras)
                    if (control == null || report == null) {
                        failed = true
                        // Without a package the group still opens, so the report can be written there by hand.
                        openUrl(target.url())
                    } else {
                        // What goes into the message is what the person wrote, not the group's name and link.
                        val text = note.trim()
                        val handed = control.shareReport(report, target, text)
                        if (!control.sharesFileToApp || !handed) openUrl(target.url())
                        result =
                            if (control.sharesFileToApp && handed) {
                                getString(Res.string.report_done_phone, report.sizeKb.toString(), report.shownPath)
                            } else {
                                getString(
                                    Res.string.report_done_computer,
                                    report.sizeKb.toString(),
                                    report.shownPath,
                                )
                            }
                    }
                    busy = false
                }
            }
        },
        dismissText = stringResource(Res.string.close),
        onDismiss = onDismiss,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(Res.string.report_issues_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.report_send_to),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                ReportTarget.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { target = option },
                    ) {
                        RadioButton(selected = target == option, onClick = { target = option })
                        Column {
                            Text(option.label(), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text =
                                stringResource(
                                    when (option) {
                                        ReportTarget.DISCORD -> Res.string.report_issues_discord_summary
                                        ReportTarget.WHATSAPP -> Res.string.report_issues_whatsapp_summary
                                        ReportTarget.SIGNAL -> Res.string.report_issues_signal_summary
                                    },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(Res.string.report_note_hint)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (control?.canMakeReport == true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(Res.string.report_with_logs),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(Res.string.report_with_logs_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = withLogs, onCheckedChange = { withLogs = it })
                    }
                }
                if (busy) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(stringResource(Res.string.report_sending), style = MaterialTheme.typography.bodySmall)
                }
                result?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
                if (failed) {
                    Text(
                        text = stringResource(Res.string.report_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.report_issues_local_mesh),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.report_issues_tips),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
