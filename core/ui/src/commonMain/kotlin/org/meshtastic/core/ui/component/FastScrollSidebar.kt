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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.meshtastic.core.ui.util.isDesktopPlatform

private const val MIN_THUMB_HEIGHT_FRACTION = 0.05f

/**
 * A small draggable thumb along the trailing edge of [listState]'s list for fast-scrolling through a long list -- built
 * from scratch rather than a platform scrollbar API since it needs to look and behave identically on Android, Desktop
 * and iOS. It behaves like a classic desktop scrollbar: dragging the thumb scrolls the list continuously, and clicking
 * anywhere else on the faint track jumps straight there (the thumb is centred on the click, so clicking the very bottom
 * of the track scrolls to the end and the very top to the start) -- most useful on Desktop, where there's no
 * platform-native touch scrollbar and long lists (nodes, messages, channels, sniffer logs, ...) are otherwise
 * mouse-wheel-only.
 *
 * Originally built for the combined Sniffer panel (see `feature/settings/sniffer/SnifferSettingsScreen.kt` for the
 * canonical usage: wrap the `LazyColumn` in a `Box`, place this as a sibling aligned `CenterEnd`, and reserve a little
 * extra `end` content padding on the list so rows don't render under the thumb) and promoted here so every long list
 * across the app can use the exact same affordance.
 *
 * @param reverseLayout Pass `true` for a list built with `LazyColumn(reverseLayout = true, ...)` (e.g. a chat view,
 *   newest item at the visual bottom): flips both the thumb position and the click/drag target so the top of the track
 *   still means "start of the list" (oldest) and the bottom still means "end" (newest), matching what the user sees on
 *   screen rather than the underlying (reversed) item index order.
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
    val thumbColor = scrollbarColor(MaterialTheme.colorScheme.onSurface)
    val coroutineScope = rememberCoroutineScope()
    val sizeCache = remember(listState) { LazyItemSizeCache() }
    // Everything that changes while scrolling is read inside the draw / pointer lambdas below (never in composition),
    // so scrolling only redraws the 4dp thumb instead of recomposing this function (and its caller) on every frame.
    val hasItems by
        remember(listState, itemCount) {
            derivedStateOf {
                // Only when there really is something to scroll: a list that fits has no bar.
                (itemCount ?: listState.layoutInfo.totalItemsCount) > 1 &&
                    (listState.canScrollForward || listState.canScrollBackward)
            }
        }
    if (!hasItems) return

    Box(
        modifier =
        modifier
            .fillMaxHeight()
            // Kept off the very top and bottom, so the bar is whole and never disappears under the rounded corners
            // of
            // a window.
            .padding(vertical = END_PADDING_DP.dp)
            .width(20.dp)
            .pointerInput(listState, itemCount, reverseLayout) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val total = itemCount ?: listState.layoutInfo.totalItemsCount
                    val trackPx = size.height.toFloat()
                    val metrics = lazyScrollMetrics(listState, total, reverseLayout, sizeCache)
                    if (trackPx <= 0f || total <= 1 || metrics == null) return@awaitEachGesture
                    down.consume()

                    val minThumbPx = MIN_THUMB_DP.dp.toPx().coerceAtMost(trackPx)
                    val thumbPx = (trackPx * metrics.thumbFraction).coerceIn(minThumbPx, trackPx)
                    val movable = (trackPx - thumbPx).coerceAtLeast(1f)
                    val thumbTop = movable * metrics.scrollFraction
                    val onThumb = down.position.y in thumbTop..(thumbTop + thumbPx)

                    if (!onThumb) {
                        // Click on the bare track: jump so the thumb is centred under the pointer (ends hit the
                        // ends).
                        val along = ((down.position.y - thumbPx / 2f) / movable).coerceIn(0f, 1f)
                        val raw = if (reverseLayout) 1f - along else along
                        val targetPx = raw * metrics.maxScrollPx
                        val (index, offset) = if (raw >= 1f) (total - 1) to 0 else sizeCache.itemAt(targetPx, total)
                        coroutineScope.launch { listState.scrollToItem(index, offset) }
                    }

                    // Relative drag in real pixels: moving the thumb by N px scrolls the list by
                    // N * (scrollable content / free track) px. Continuous (no item-by-item jumps) and
                    // non-suspending, so it never queues up.
                    drag(down.id) { change ->
                        change.consume()
                        val delta = (change.position.y - change.previousPosition.y) * metrics.maxScrollPx / movable
                        listState.dispatchRawDelta(if (reverseLayout) -delta else delta)
                    }
                }
            }
            .drawBehind {
                val total = itemCount ?: listState.layoutInfo.totalItemsCount
                if (total <= 1 || size.height <= 0f) return@drawBehind
                val metrics = lazyScrollMetrics(listState, total, reverseLayout, sizeCache) ?: return@drawBehind
                drawTrack(thumbColor)
                drawThumb(thumbColor, metrics.scrollFraction, metrics.thumbFraction)
            },
    )
}

private const val MIN_THUMB_DP = 24
private const val END_PADDING_DP = 24

/**
 * Remembers the pixel height of every row of a lazy list that has been on screen. A lazy list only measures what it
 * shows, so counting visible rows (as this used to) made the thumb grow and shrink with the height of the rows in view.
 * With the sizes remembered, the content height is estimated as known rows plus the average for unseen ones, which
 * settles quickly and is exact once the whole list has been seen -- so the thumb keeps one size and only moves.
 */
internal class LazyItemSizeCache {
    private val sizes = HashMap<Int, Int>()

    fun record(listState: LazyListState) {
        listState.layoutInfo.visibleItemsInfo.forEach { sizes[it.index] = it.size }
    }

    private fun average(): Float = if (sizes.isEmpty()) 0f else sizes.values.sum().toFloat() / sizes.size

    private fun sizeOf(index: Int, average: Float): Float = sizes[index]?.toFloat() ?: average

    /** Pixels before the row at [index]: remembered sizes, with the average standing in for rows not yet seen. */
    fun offsetOf(index: Int): Float {
        val avg = average()
        var sum = 0f
        for (i in 0 until index) sum += sizeOf(i, avg)
        return sum
    }

    /** The row [px] pixels down the content and how far into it that is, for jumping to a place on the track. */
    fun itemAt(px: Float, total: Int): Pair<Int, Int> {
        val avg = average()
        var acc = 0f
        for (i in 0 until total) {
            val size = sizeOf(i, avg)
            if (acc + size > px) return i to (px - acc).toInt().coerceAtLeast(0)
            acc += size
        }
        return (total - 1).coerceAtLeast(0) to 0
    }
}

private class LazyScrollMetrics(val thumbFraction: Float, val scrollFraction: Float, val maxScrollPx: Float)

/** Thumb size and position (0 = top, 1 = bottom) for [listState], from remembered row sizes; null before any layout. */
private fun lazyScrollMetrics(
    listState: LazyListState,
    total: Int,
    reverseLayout: Boolean,
    cache: LazyItemSizeCache,
): LazyScrollMetrics? {
    cache.record(listState)
    val info = listState.layoutInfo
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    val content = cache.offsetOf(total)
    if (viewport <= 0f || content <= 0f) return null
    val maxScroll = (content - viewport).coerceAtLeast(1f)
    val scrolled = cache.offsetOf(listState.firstVisibleItemIndex) + listState.firstVisibleItemScrollOffset
    val raw = (scrolled / maxScroll).coerceIn(0f, 1f)
    return LazyScrollMetrics(
        thumbFraction = (viewport / content).coerceIn(MIN_THUMB_HEIGHT_FRACTION, 1f),
        scrollFraction = if (reverseLayout) 1f - raw else raw,
        maxScrollPx = maxScroll,
    )
}

/**
 * Overload for a [Modifier.verticalScroll]-driven `Column` rather than a `LazyColumn` -- most of this app's settings
 * screens are short, static preference forms built that way instead of as a LazyColumn, so this mirrors the same
 * scrollbar behaviour (drag the thumb, or click the track to jump there) for [ScrollState].
 *
 * Unlike the [LazyListState] overload there's no item count to reason about: [ScrollState] already reports the scrolled
 * pixel range directly ([ScrollState.maxValue], [ScrollState.viewportSize]), which this maps onto the same thumb-track
 * visuals. Desktop-only for the same reason -- see the primary overload's doc.
 */
@Composable
fun FastScrollSidebar(scrollState: ScrollState, modifier: Modifier = Modifier) {
    if (!isDesktopPlatform) return
    val thumbColor = scrollbarColor(MaterialTheme.colorScheme.onSurface)
    val coroutineScope = rememberCoroutineScope()
    val canScroll by remember(scrollState) { derivedStateOf { scrollState.maxValue > 0 } }
    if (!canScroll) return

    Box(
        modifier =
        modifier
            .fillMaxHeight()
            // Kept off the very top and bottom, so the bar is whole and never disappears under the rounded corners
            // of
            // a window.
            .padding(vertical = END_PADDING_DP.dp)
            .width(20.dp)
            .pointerInput(scrollState) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val max = scrollState.maxValue
                    val trackPx = size.height.toFloat()
                    if (trackPx <= 0f || max <= 0) return@awaitEachGesture
                    down.consume()

                    val thumbFraction =
                        (scrollState.viewportSize.toFloat() / (max + scrollState.viewportSize)).coerceIn(
                            MIN_THUMB_HEIGHT_FRACTION,
                            1f,
                        )
                    val minThumbPx = MIN_THUMB_DP.dp.toPx().coerceAtMost(trackPx)
                    val thumbPx = (trackPx * thumbFraction).coerceIn(minThumbPx, trackPx)
                    val thumbTop = (trackPx - thumbPx) * (scrollState.value.toFloat() / max).coerceIn(0f, 1f)
                    val onThumb = down.position.y in thumbTop..(thumbTop + thumbPx)

                    if (!onThumb) {
                        // Click on the bare track: jump so the thumb is centred under the pointer.
                        val along = ((down.position.y - thumbPx / 2f) / (trackPx - thumbPx).coerceAtLeast(1f))
                        val target = (along.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
                        coroutineScope.launch { scrollState.scrollTo(target) }
                    }

                    // Moving the thumb by N px scrolls the content by N * (content / track) px.
                    drag(down.id) { change ->
                        change.consume()
                        val ratio = (max + scrollState.viewportSize) / trackPx
                        scrollState.dispatchRawDelta((change.position.y - change.previousPosition.y) * ratio)
                    }
                }
            }
            .drawBehind {
                val max = scrollState.maxValue
                if (max <= 0 || size.height <= 0f) return@drawBehind
                val totalContentPx = max + scrollState.viewportSize
                val scrollFraction = (scrollState.value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                val thumbFraction =
                    (scrollState.viewportSize.toFloat() / totalContentPx.toFloat()).coerceIn(
                        MIN_THUMB_HEIGHT_FRACTION,
                        1f,
                    )
                drawTrack(thumbColor)
                drawThumb(thumbColor, scrollFraction, thumbFraction)
            },
    )
}

/**
 * White on a dark theme, the text colour on a light one: the solid thumb has to stand out from the track. Judged by the
 * text colour, not the surface: inside our windows the surface is made transparent, which would always read as dark.
 */
private fun scrollbarColor(text: Color): Color = if (text.luminance() > LIGHT_TEXT_LUMINANCE) Color.White else text

private const val LIGHT_TEXT_LUMINANCE = 0.5f
private const val TRACK_WIDTH_DP = 8
private const val TRACK_EDGE_DP = 1.5f
private const val THUMB_WIDTH_DP = 4
private const val TRACK_EDGE_ALPHA = 0.55f

/**
 * MT_SW: the groove is an outline only: two long lines joined into one capsule with rounded ends. The thumb is the
 * solid bar inside it.
 */
private fun DrawScope.drawTrack(color: Color) {
    val trackWidth = TRACK_WIDTH_DP.dp.toPx()
    val edge = TRACK_EDGE_DP.dp.toPx()
    val inset = edge / 2f
    drawRoundRect(
        color = color.copy(alpha = TRACK_EDGE_ALPHA),
        topLeft = Offset((size.width - trackWidth) / 2f + inset, inset),
        size = Size(trackWidth - edge, size.height - edge),
        cornerRadius = CornerRadius((trackWidth - edge) / 2f),
        style = Stroke(width = edge),
    )
}

private fun DrawScope.drawThumb(color: Color, scrollFraction: Float, thumbFraction: Float) {
    val minThumbPx = MIN_THUMB_DP.dp.toPx()
    val thumbHeight = (size.height * thumbFraction).coerceAtLeast(minThumbPx).coerceAtMost(size.height)
    val thumbWidth = THUMB_WIDTH_DP.dp.toPx()
    val top = (size.height - thumbHeight) * scrollFraction
    drawRoundRect(
        color = color,
        topLeft = Offset((size.width - thumbWidth) / 2f, top),
        size = Size(thumbWidth, thumbHeight),
        cornerRadius = CornerRadius(thumbWidth / 2f),
    )
}

private const val BAR_WIDTH_DP = 14

/**
 * MT_SW: the same bar as [FastScrollSidebar], drawn on the trailing edge of the scrolled area itself. It takes the
 * pointer before the content does, but only inside the narrow strip of the bar and only when there is something to
 * scroll.
 */
internal fun Modifier.scrollStateBar(state: ScrollState, text: Color): Modifier =
    pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val max = state.maxValue
            if (max <= 0 || !inBarStrip(down, size.width, BAR_WIDTH_DP.dp.toPx())) return@awaitEachGesture
            down.consume()
            val ratio = (max + state.viewportSize).toFloat() / size.height.coerceAtLeast(1)
            // A press outside the thumb jumps there; dragging then moves on from that place.
            val thumbTop = state.value / ratio
            if (down.position.y !in thumbTop..(thumbTop + state.viewportSize / ratio)) {
                state.dispatchRawDelta(down.position.y * ratio - state.viewportSize / 2f - state.value)
            }
            dragBar(down) { delta -> state.dispatchRawDelta(delta * ratio) }
        }
    }
        .drawWithContent {
            drawContent()
            val max = state.maxValue
            if (max <= 0 || max == Int.MAX_VALUE || size.height <= 0f) return@drawWithContent
            val thumbFraction =
                (state.viewportSize.toFloat() / (max + state.viewportSize)).coerceIn(MIN_THUMB_HEIGHT_FRACTION, 1f)
            val fraction = (state.value.toFloat() / max).coerceIn(0f, 1f)
            drawBar(scrollbarColor(text), fraction, thumbFraction)
        }

/** MT_SW: [scrollStateBar] for a lazy list. */
internal fun Modifier.lazyListBar(
    state: LazyListState,
    text: Color,
    reverseLayout: Boolean,
    cache: LazyItemSizeCache,
): Modifier =
    pointerInput(state, reverseLayout) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!state.canScrollForward && !state.canScrollBackward) return@awaitEachGesture
            if (!inBarStrip(down, size.width, BAR_WIDTH_DP.dp.toPx())) return@awaitEachGesture
            val total = state.layoutInfo.totalItemsCount
            val metrics = lazyScrollMetrics(state, total, reverseLayout, cache) ?: return@awaitEachGesture
            down.consume()
            val trackPx = size.height.coerceAtLeast(1).toFloat()
            val thumbPx = trackPx * metrics.thumbFraction
            val movable = (trackPx - thumbPx).coerceAtLeast(1f)
            val thumbTop = movable * metrics.scrollFraction
            val direction = if (reverseLayout) -1f else 1f
            if (down.position.y !in thumbTop..(thumbTop + thumbPx)) {
                val jump = (down.position.y - thumbPx / 2f - thumbTop) * metrics.maxScrollPx / movable
                state.dispatchRawDelta(direction * jump)
            }
            dragBar(down) { delta -> state.dispatchRawDelta(direction * delta * metrics.maxScrollPx / movable) }
        }
    }
        .drawWithContent {
            drawContent()
            if (!state.canScrollForward && !state.canScrollBackward) return@drawWithContent
            val total = state.layoutInfo.totalItemsCount
            val metrics = lazyScrollMetrics(state, total, reverseLayout, cache) ?: return@drawWithContent
            drawBar(scrollbarColor(text), metrics.scrollFraction, metrics.thumbFraction)
        }

/**
 * MT_SW: [scrollStateBar] for a lazy grid. The content height is estimated from the visible cells: cells per row and the
 * height of the first visible one.
 */
internal fun Modifier.lazyGridBar(state: LazyGridState, text: Color): Modifier =
    pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!inBarStrip(down, size.width, BAR_WIDTH_DP.dp.toPx())) return@awaitEachGesture
            val metrics = gridScrollMetrics(state) ?: return@awaitEachGesture
            down.consume()
            val trackPx = size.height.coerceAtLeast(1).toFloat()
            val movable = (trackPx * (1f - metrics.thumbFraction)).coerceAtLeast(1f)
            val ratio = metrics.maxScrollPx / movable
            val thumbTop = movable * metrics.scrollFraction
            if (down.position.y !in thumbTop..(thumbTop + trackPx * metrics.thumbFraction)) {
                state.dispatchRawDelta((down.position.y - trackPx * metrics.thumbFraction / 2f - thumbTop) * ratio)
            }
            dragBar(down) { delta -> state.dispatchRawDelta(delta * ratio) }
        }
    }
        .drawWithContent {
            drawContent()
            val metrics = gridScrollMetrics(state) ?: return@drawWithContent
            drawBar(scrollbarColor(text), metrics.scrollFraction, metrics.thumbFraction)
        }

/** Null when the grid fits (nothing to scroll) or has not been laid out yet. */
private fun gridScrollMetrics(state: LazyGridState): LazyScrollMetrics? {
    if (!state.canScrollForward && !state.canScrollBackward) return null
    val info = state.layoutInfo
    val visible = info.visibleItemsInfo
    val last = visible.lastOrNull() ?: return null
    val perRow = visible.maxOf { it.column } + 1
    val rowPx = last.size.height.coerceAtLeast(1)
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    val content = ((info.totalItemsCount + perRow - 1) / perRow * rowPx).toFloat()
    val maxScroll = (content - viewport).coerceAtLeast(1f)
    val scrolled = state.firstVisibleItemIndex / perRow * rowPx + state.firstVisibleItemScrollOffset
    return LazyScrollMetrics(
        thumbFraction = (viewport / content).coerceIn(MIN_THUMB_HEIGHT_FRACTION, 1f),
        scrollFraction = (scrolled / maxScroll).coerceIn(0f, 1f),
        maxScrollPx = maxScroll,
    )
}

private fun inBarStrip(down: PointerInputChange, width: Int, strip: Float): Boolean =
    down.position.x >= width - strip

private suspend fun AwaitPointerEventScope.dragBar(
    down: PointerInputChange,
    onDelta: (Float) -> Unit,
) {
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return
        if (!change.pressed) return
        onDelta(change.position.y - change.previousPosition.y)
        change.consume()
    }
}

/** MT_SW: the groove and the thumb in the strip on the right, kept off the top and bottom like the stand-alone bar. */
private fun DrawScope.drawBar(color: Color, scrollFraction: Float, thumbFraction: Float) {
    val width = BAR_WIDTH_DP.dp.toPx()
    // A low window cannot give up the full margin at both ends.
    val margin = minOf(END_PADDING_DP.dp.toPx(), size.height / 10f)
    val height = size.height - 2 * margin
    translate(left = size.width - width, top = margin) {
        inset(0f, 0f, size.width - width, size.height - height) {
            drawTrack(color)
            drawThumb(color, scrollFraction, thumbFraction)
        }
    }
}
