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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.util.isDesktopPlatform

/**
 * MT_SW: `verticalScroll` with our scroll bar on the computer. The bar is drawn by the modifier itself, so it works in
 * any window without wrapping the content in a `Box`. On phones and tablets it is a plain `verticalScroll`.
 */
@Composable
fun Modifier.verticalScrollWithBar(state: ScrollState): Modifier = desktopScrollbar(state).verticalScroll(state)

/** MT_SW: a `LazyColumn` with our scroll bar on the computer. Same parameters as `LazyColumn`. */
@Composable
fun ScrollbarLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.desktopScrollbar(state, reverseLayout),
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        userScrollEnabled = userScrollEnabled,
        content = content,
    )
}

@Composable
private fun Modifier.desktopScrollbar(state: ScrollState): Modifier =
    if (isDesktopPlatform) scrollStateBar(state, MaterialTheme.colorScheme.onSurface) else this

/** MT_SW: our scroll bar for a lazy list or grid built by hand. Does nothing outside the computer. */
@Composable
fun Modifier.desktopScrollbar(state: LazyListState, reverseLayout: Boolean = false): Modifier =
    if (isDesktopPlatform) {
        val sizes = remember(state) { LazyItemSizeCache() }
        lazyListBar(state, MaterialTheme.colorScheme.onSurface, reverseLayout, sizes)
    } else {
        this
    }

/** MT_SW: our scroll bar for a lazy grid. Does nothing outside the computer. */
@Composable
fun Modifier.desktopScrollbar(state: LazyGridState): Modifier =
    if (isDesktopPlatform) lazyGridBar(state, MaterialTheme.colorScheme.onSurface) else this
