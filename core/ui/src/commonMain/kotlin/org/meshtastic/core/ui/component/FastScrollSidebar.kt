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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.meshtastic.core.ui.util.isDesktopPlatform

private const val MIN_THUMB_HEIGHT_FRACTION = 0.05f

/**
 * A small draggable thumb along the trailing edge of [listState]'s list for fast-scrolling through a long list -- built
 * from scratch rather than a platform scrollbar API since it needs to look and behave identically on Android, Desktop
 * and iOS. Dragging anywhere on the track jumps the list to the proportional item rather than scrolling incrementally,
 * which is the point of a *fast*-scroll affordance -- most useful on Desktop, where there's no platform-native touch
 * scrollbar and long lists (nodes, messages, channels, sniffer logs, ...) are otherwise mouse-wheel-only.
 *
 * Originally built for the combined Sniffer panel (see `feature/settings/sniffer/SnifferSettingsScreen.kt` for the
 * canonical usage: wrap the `LazyColumn` in a `Box`, place this as a sibling aligned `CenterEnd`, and reserve a little
 * extra `end` content padding on the list so rows don't render under the thumb) and promoted here so every long list
 * across the app can use the exact same affordance.
 *
 * @param reverseLayout Pass `true` for a list built with `LazyColumn(reverseLayout = true, ...)` (e.g. a chat view,
 *   newest item at the visual bottom): flips both the thumb position and the drag target so the top of the track still
 *   means "start of the list" (oldest) and the bottom still means "end" (newest), matching what the user sees on screen
 *   rather than the underlying (reversed) item index order.
 *
 * Desktop-only by design (see [isDesktopPlatform]): Android/iOS already scroll by touch and fling, and this strip would
 * otherwise sit on the same trailing edge as swipe gestures (mute/delete on a contact or message row). Call sites don't
 * need to guard on platform themselves -- this is a no-op everywhere else.
 *
 * @param itemCount Defaults to [listState]'s own [androidx.compose.foundation.lazy.LazyListLayoutInfo]
 *   (`totalItemsCount`), which is right for most lists; pass it explicitly only when the caller already tracks a count
 *   that's cheaper or more accurate to read directly (e.g. a backing `List.size`).
 */
@Composable
fun FastScrollSidebar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    itemCount: Int = listState.layoutInfo.totalItemsCount,
    reverseLayout: Boolean = false,
) {
    if (!isDesktopPlatform || itemCount <= 1) return
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    val minThumbHeightPx = with(density) { 24.dp.toPx() }

    val visibleCount = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    val maxFirstIndex = (itemCount - visibleCount).coerceAtLeast(1)
    val rawScrollFraction = (listState.firstVisibleItemIndex.toFloat() / maxFirstIndex.toFloat()).coerceIn(0f, 1f)
    val scrollFraction = if (reverseLayout) 1f - rawScrollFraction else rawScrollFraction
    val thumbHeightFraction = (visibleCount.toFloat() / itemCount.toFloat()).coerceIn(MIN_THUMB_HEIGHT_FRACTION, 1f)

    Box(
        modifier =
        modifier
            .fillMaxHeight()
            .width(20.dp)
            .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
            .pointerInput(itemCount, reverseLayout) {
                detectDragGestures { change, _ ->
                    change.consume()
                    if (trackHeightPx <= 0f) return@detectDragGestures
                    val fraction = (change.position.y / trackHeightPx).coerceIn(0f, 1f)
                    val orderedFraction = if (reverseLayout) 1f - fraction else fraction
                    val targetIndex = (orderedFraction * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
                    coroutineScope.launch { listState.scrollToItem(targetIndex) }
                }
            },
    ) {
        val thumbHeightDp =
            with(density) { (trackHeightPx * thumbHeightFraction).coerceAtLeast(minThumbHeightPx).toDp() }
        val thumbOffsetPx = (trackHeightPx - with(density) { thumbHeightDp.toPx() }) * scrollFraction
        Box(
            modifier =
            Modifier.align(Alignment.TopCenter)
                .offset { IntOffset(0, thumbOffsetPx.toInt()) }
                .width(4.dp)
                .height(thumbHeightDp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline),
        )
    }
}

/**
 * Overload for a [Modifier.verticalScroll]-driven `Column` rather than a `LazyColumn` -- most of this app's settings
 * screens are short, static preference forms built that way instead of as a LazyColumn, so this mirrors the same
 * draggable fast-scroll affordance for [ScrollState].
 *
 * Unlike the [LazyListState] overload there's no item count to reason about: [ScrollState] already reports the scrolled
 * pixel range directly ([ScrollState.maxValue], [ScrollState.viewportSize]), which this maps onto the same thumb-track
 * visuals and drag-to-jump behavior. Desktop-only for the same reason -- see the primary overload's doc.
 */
@Composable
fun FastScrollSidebar(scrollState: ScrollState, modifier: Modifier = Modifier) {
    if (!isDesktopPlatform || scrollState.maxValue <= 0) return
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    val minThumbHeightPx = with(density) { 24.dp.toPx() }

    val totalContentPx = scrollState.maxValue + scrollState.viewportSize
    val scrollFraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
    val thumbHeightFraction =
        (scrollState.viewportSize.toFloat() / totalContentPx.toFloat()).coerceIn(MIN_THUMB_HEIGHT_FRACTION, 1f)

    Box(
        modifier =
        modifier
            .fillMaxHeight()
            .width(20.dp)
            .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
            .pointerInput(scrollState.maxValue) {
                detectDragGestures { change, _ ->
                    change.consume()
                    if (trackHeightPx <= 0f) return@detectDragGestures
                    val fraction = (change.position.y / trackHeightPx).coerceIn(0f, 1f)
                    val target = (fraction * scrollState.maxValue).toInt().coerceIn(0, scrollState.maxValue)
                    coroutineScope.launch { scrollState.scrollTo(target) }
                }
            },
    ) {
        val thumbHeightDp =
            with(density) { (trackHeightPx * thumbHeightFraction).coerceAtLeast(minThumbHeightPx).toDp() }
        val thumbOffsetPx = (trackHeightPx - with(density) { thumbHeightDp.toPx() }) * scrollFraction
        Box(
            modifier =
            Modifier.align(Alignment.TopCenter)
                .offset { IntOffset(0, thumbOffsetPx.toInt()) }
                .width(4.dp)
                .height(thumbHeightDp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline),
        )
    }
}
