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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.support_project_description
import org.meshtastic.core.resources.support_project_summary
import org.meshtastic.core.resources.support_project_title
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.icon.Favorite
import org.meshtastic.core.ui.icon.MeshtasticIcons

private const val SUPPORT_PROJECT_URL = "https://ko-fi.com/spontanicznemajsterkowanie"

/**
 * The "support the project" entry (voluntary donation page of the developer). It is one row that opens the page in the
 * browser, so it works the same on every platform. Shown in the MT_SW features and in the About screen.
 */
@Composable
fun SupportProjectSection() {
    if (!isSupportLinkAllowed()) return
    val uriHandler = LocalUriHandler.current
    ExpressiveSection(title = stringResource(Res.string.support_project_title)) {
        Text(
            text = stringResource(Res.string.support_project_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        ListItem(
            text = stringResource(Res.string.support_project_title),
            supportingText = stringResource(Res.string.support_project_summary),
            leadingIcon = MeshtasticIcons.Favorite,
        ) {
            uriHandler.openUri(SUPPORT_PROJECT_URL)
        }
    }
}
