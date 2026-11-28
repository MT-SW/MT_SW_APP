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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger

/**
 * MT_SW: the window of an "i" button, the same as our other windows: a close cross at the top, closing by a swipe down,
 * the title scrolling with the text and the scroll bar when the text is longer than the window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoWindow(
    title: String,
    onDismiss: () -> Unit,
    /** False for content that scrolls by itself (a list): two scrolls inside each other do not work. */
    scrolls: Boolean = true,
    /** The size of a standard dialog (a neighbour list, a traceroute) instead of nearly the whole screen width. */
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        standardWidth = compact,
        onDismissRequest = {
            Logger.i { "Okno informacji: zamknięte" }
            onDismiss()
        },
    ) {
        val scroll = rememberScrollState()
        Column(
            modifier =
            (if (compact) Modifier.widthIn(min = STANDARD_MIN_WIDTH_DP.dp) else Modifier.fillMaxWidth())
                .then(if (scrolls) Modifier.verticalScrollWithBar(scroll, sideMargin = SIDE_DP.dp) else Modifier)
                .padding(horizontal = SIDE_DP.dp)
                .padding(bottom = SIDE_DP.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp, end = (SHEET_CROSS_INSET_DP - SIDE_DP).dp),
            )
            content()
        }
    }
}

private const val SIDE_DP = 24

/** The smallest width of a standard dialog. */
private const val STANDARD_MIN_WIDTH_DP = 280
