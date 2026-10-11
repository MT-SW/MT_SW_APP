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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.report_issues_discord_summary
import org.meshtastic.core.resources.report_issues_find_us
import org.meshtastic.core.resources.report_issues_headline
import org.meshtastic.core.resources.report_issues_intro
import org.meshtastic.core.resources.report_issues_local_mesh
import org.meshtastic.core.resources.report_issues_signal_summary
import org.meshtastic.core.resources.report_issues_tips
import org.meshtastic.core.resources.report_issues_title
import org.meshtastic.core.resources.report_issues_whatsapp_summary
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.icon.BugReport
import org.meshtastic.core.ui.icon.ChatBubbleOutline
import org.meshtastic.core.ui.icon.Link
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.rememberOpenUrl

private const val DISCORD_URL = "https://discord.gg/EJqUHpvVKN"
private const val WHATSAPP_URL = "https://chat.whatsapp.com/HNrO14NTKTx0xiQGKDVX7N"
private const val SIGNAL_URL =
    "https://signal.group/#CjQKIJrjVtcmsLIgJre9nmpUlTwLMpZnvvIXZUH3LSC9yGDtEhCOjGRHVeloUHeC9rTvL--l"

/**
 * The "report a bug or idea" entry of the settings: one row that opens a page with where to reach us (Discord, the
 * WhatsApp and Signal groups, the local mesh) and what to include in a bug report.
 */
@Composable
internal fun ReportIssuesSection() {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    ExpressiveSection(title = stringResource(Res.string.report_issues_title)) {
        ListItem(text = stringResource(Res.string.report_issues_title), leadingIcon = MeshtasticIcons.BugReport) {
            showDialog = true
        }
    }
    if (showDialog) {
        ReportIssuesDialog(onDismiss = { showDialog = false })
    }
}

@Composable
private fun ReportIssuesDialog(onDismiss: () -> Unit) {
    val openUrl = rememberOpenUrl()
    MeshtasticDialog(
        title = stringResource(Res.string.report_issues_headline),
        icon = MeshtasticIcons.BugReport,
        dismissText = stringResource(Res.string.close),
        onDismiss = onDismiss,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(Res.string.report_issues_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.report_issues_find_us),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                ListItem(
                    text = "Discord",
                    supportingText = stringResource(Res.string.report_issues_discord_summary),
                    leadingIcon = MeshtasticIcons.ChatBubbleOutline,
                    trailingIcon = MeshtasticIcons.Link,
                ) {
                    openUrl(DISCORD_URL)
                }
                ListItem(
                    text = "WhatsApp",
                    supportingText = stringResource(Res.string.report_issues_whatsapp_summary),
                    leadingIcon = MeshtasticIcons.ChatBubbleOutline,
                    trailingIcon = MeshtasticIcons.Link,
                ) {
                    openUrl(WHATSAPP_URL)
                }
                ListItem(
                    text = "Signal",
                    supportingText = stringResource(Res.string.report_issues_signal_summary),
                    leadingIcon = MeshtasticIcons.ChatBubbleOutline,
                    trailingIcon = MeshtasticIcons.Link,
                ) {
                    openUrl(SIGNAL_URL)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.report_issues_local_mesh),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.report_issues_tips),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
