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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.widget_stats_info
import org.meshtastic.core.resources.widget_stats_summary
import org.meshtastic.core.resources.widget_stats_title
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.icon.FormatPaint
import org.meshtastic.core.ui.icon.MeshtasticIcons

/** The row "Widżet statystyk": a scrollable information window about the widget and how to add it. */
@Composable
fun WidgetStatsSettingItem(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    if (open) {
        MeshtasticDialog(
            title = stringResource(Res.string.widget_stats_title),
            onDismiss = { open = false },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(Res.string.widget_stats_info))
                }
            },
        )
    }
    ListItem(
        modifier = modifier,
        text = stringResource(Res.string.widget_stats_title),
        supportingText = stringResource(Res.string.widget_stats_summary),
        leadingIcon = MeshtasticIcons.FormatPaint,
    ) {
        open = true
    }
}
