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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
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
    itemCount: Int? = null,
    reverseLayout: Boolean = false,
) {
    if (!isDesktopPlatform) return
    val thumbColor = MaterialTheme.colorScheme.outline
    // Everything that changes while scrolling is read inside the draw / pointer lambdas below (never in composition),
    // so scrolling only redraws the 4dp thumb instead of recomposing this function (and its caller) on every frame.
    val hasItems by remember(listState, itemCount) {
        derivedStateOf { (itemCount ?: listState.layoutInfo.totalItemsCount) > 1 }
    }
    if (!hasItems) return

    Box(
        modifier =
        modifier
            .fillMaxHeight()
            .width(20.dp)
            .pointerInput(listState, itemCount, reverseLayout) {
                // Relative drag in real pixels: moving the thumb by N px scrolls the list by N * (content / track) px.
                // That is continuous (no item-by-item jumps) and non-suspending, so it never queues up behind frames.
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val total = itemCount ?: listState.layoutInfo.totalItemsCount
                    val info = listState.layoutInfo
                    val visible = info.visibleItemsInfo
                    if (size.height <= 0 || total <= 1 || visible.isEmpty()) return@detectDragGestures
                    val avgItemPx = visible.sumOf { it.size }.toFloat() / visible.size
                    val contentPx = avgItemPx * total
                    val ratio = contentPx / size.height
                    val delta = dragAmount.y * ratio
                    listState.dispatchRawDelta(if (reverseLayout) -delta else delta)
                }
            }
            .drawBehind {
                val total = itemCount ?: listState.layoutInfo.totalItemsCount
                if (total <= 1 || size.height <= 0f) return@drawBehind
                val visible = listState.layoutInfo.visibleItemsInfo
                val visibleCount = visible.size.coerceAtLeast(1)
                val maxFirstIndex = (total - visibleCount).coerceAtLeast(1)
                // Fractional position (item index + how far into the first item we are) so the thumb glides
                // instead of stepping one whole item at a time -- matters for tall rows such as chat messages.
                val first = visible.firstOrNull()
                val partial =
                    if (first != null && first.size > 0) {
                        (listState.firstVisibleItemScrollOffset.toFloat() / first.size).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                val position = listState.firstVisibleItemIndex + partial
                val raw = (position / maxFirstIndex.toFloat()).coerceIn(0f, 1f)
                val scrollFraction = if (reverseLayout) 1f - raw else raw
                val thumbFraction = (visibleCount.toFloat() / total.toFloat()).coerceIn(MIN_THUMB_HEIGHT_FRACTION, 1f)
                drawThumb(thumbColor, scrollFraction, thumbFraction)
            },
    )
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
    if (!isDesktopPlatform) return
    val thumbColor = MaterialTheme.colorScheme.outline
    val coroutineScope = rememberCoroutineScope()
    val canScroll by remember(scrollState) { derivedStateOf { scrollState.maxValue > 0 } }
    if (!canScroll) return

    Box(
        modifier =
        modifier
            .fillMaxHeight()
            .width(20.dp)
            .pointerInput(scrollState) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val max = scrollState.maxValue
                    if (size.height <= 0 || max <= 0) return@detectDragGestures
                    val fraction = (change.position.y / size.height).coerceIn(0f, 1f)
                    val target = (fraction * max).toInt().coerceIn(0, max)
                    coroutineScope.launch { scrollState.scrollTo(target) }
                }
            }
            .drawBehind {
                val max = scrollState.maxValue
                if (max <= 0 || size.height <= 0f) return@drawBehind
                val totalContentPx = max + scrollState.viewportSize
                val scrollFraction = (scrollState.value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                val thumbFraction =
                    (scrollState.viewportSize.toFloat() / totalContentPx.toFloat()).coerceIn(MIN_THUMB_HEIGHT_FRACTION, 1f)
                drawThumb(thumbColor, scrollFraction, thumbFraction)
            },
    )
}

private fun DrawScope.drawThumb(
    color: Color,
    scrollFraction: Float,
    thumbFraction: Float,
) {
    val minThumbPx = 24.dp.toPx()
    val thumbHeight = (size.height * thumbFraction).coerceAtLeast(minThumbPx).coerceAtMost(size.height)
    val thumbWidth = 4.dp.toPx()
    val top = (size.height - thumbHeight) * scrollFraction
    drawRoundRect(
        color = color,
        topLeft = Offset((size.width - thumbWidth) / 2f, top),
        size = Size(thumbWidth, thumbHeight),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
}
