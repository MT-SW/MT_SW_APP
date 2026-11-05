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
@file:Suppress("ModifierMissing")

package org.meshtastic.feature.settings.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.report_issues_title
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.ReportIssuesDialog
import org.meshtastic.core.ui.icon.BugReport
import org.meshtastic.core.ui.icon.MeshtasticIcons

/**
 * The "report a bug or idea" entry of the MT_SW features: one row that opens the report window (where to send it, a
 * note, the log of the last 24 hours the app keeps by itself).
 */
@Composable
fun ReportIssuesSection() {
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
