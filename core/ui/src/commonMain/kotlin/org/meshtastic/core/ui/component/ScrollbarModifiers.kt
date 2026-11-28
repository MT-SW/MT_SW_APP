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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
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
    if (isDesktopPlatform) {
        // `maxValue` is MAX_VALUE until the first layout, which is not "something to scroll".
        scrollStateBar(state, MaterialTheme.colorScheme.onSurface)
            .barGutter(state) { state.maxValue in 1 until Int.MAX_VALUE }
    } else {
        this
    }

/** MT_SW: our scroll bar for a lazy list or grid built by hand. Does nothing outside the computer. */
@Composable
fun Modifier.desktopScrollbar(state: LazyListState, reverseLayout: Boolean = false): Modifier =
    if (isDesktopPlatform) {
        val sizes = remember(state) { LazyItemSizeCache() }
        lazyListBar(state, MaterialTheme.colorScheme.onSurface, reverseLayout, sizes)
            .barGutter(state) { state.canScrollForward || state.canScrollBackward }
    } else {
        this
    }

/** MT_SW: our scroll bar for a lazy grid. Does nothing outside the computer. */
@Composable
fun Modifier.desktopScrollbar(state: LazyGridState): Modifier =
    if (isDesktopPlatform) {
        lazyGridBar(state, MaterialTheme.colorScheme.onSurface)
            .barGutter(state) { state.canScrollForward || state.canScrollBackward }
    } else {
        this
    }

/**
 * MT_SW: room of its own for the scroll bar, so it never covers text or switches. Once there is something to scroll,
 * the content is measured narrower and the same width is added back: a window that wraps its content gets wider, and
 * one already as wide as it may be gets narrower content. The room is kept for as long as the window lives, so
 * content whose height follows its width (a picture) cannot make the layout jump back and forth.
 */
@Composable
private fun Modifier.barGutter(key: Any, canScroll: () -> Boolean): Modifier {
    var needed by remember(key) { mutableStateOf(false) }
    val scrollable by remember(key) { derivedStateOf(canScroll) }
    LaunchedEffect(key, scrollable) { if (scrollable) needed = true }
    return if (needed) {
        layout { measurable, constraints ->
            val gutter = SCROLLBAR_GUTTER_DP.dp.roundToPx()
            val placeable = measurable.measure(constraints.offset(horizontal = -gutter))
            layout(constraints.constrainWidth(placeable.width + gutter), placeable.height) {
                placeable.placeRelative(0, 0)
            }
        }
    } else {
        this
    }
}

private const val SCROLLBAR_GUTTER_DP = 14
