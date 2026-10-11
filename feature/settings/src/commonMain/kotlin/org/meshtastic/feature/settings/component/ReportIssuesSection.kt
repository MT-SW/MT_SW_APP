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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.report_issues_discord_summary
import org.meshtastic.core.resources.report_issues_intro
import org.meshtastic.core.resources.report_issues_local_mesh
import org.meshtastic.core.resources.report_issues_signal_summary
import org.meshtastic.core.resources.report_issues_title
import org.meshtastic.core.resources.report_issues_whatsapp_summary
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.icon.ChatBubbleOutline
import org.meshtastic.core.ui.icon.Link
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.rememberOpenUrl

private const val DISCORD_URL = "https://discord.gg/EJqUHpvVKN"
private const val WHATSAPP_URL = "https://chat.whatsapp.com/HNrO14NTKTx0xiQGKDVX7N"
private const val SIGNAL_URL =
    "https://signal.group/#CjQKIJrjVtcmsLIgJre9nmpUlTwLMpZnvvIXZUH3LSC9yGDtEhCOjGRHVeloUHeC9rTvL--l"

/** Where to report bugs and ideas: our Discord server, the WhatsApp and Signal groups, and the local mesh itself. */
@Composable
internal fun ReportIssuesSection() {
    val openUrl = rememberOpenUrl()
    ExpressiveSection(title = stringResource(Res.string.report_issues_title)) {
        Text(
            text = stringResource(Res.string.report_issues_intro),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
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
        Text(
            text = stringResource(Res.string.report_issues_local_mesh),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
