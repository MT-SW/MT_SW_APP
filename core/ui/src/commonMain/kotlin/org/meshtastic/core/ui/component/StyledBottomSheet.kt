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

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.MeshtasticIcons

private const val SHEET_CORNER_DP = 28
private const val WINDOW_MAX_WIDTH_DP = 560
private const val WINDOW_MARGIN_DP = 16
private const val WINDOW_MAX_HEIGHT_PERCENT = 0.85f

/** How far (in dp) a downward swipe has to go to close the window. */
private const val SWIPE_CLOSE_DP = 40

/** How far up the wheel must pull, after the rest at the top, to close the window. */
private const val WHEEL_AGAIN_TICKS = 0.1f
private const val WHEEL_PAUSE_MS = 5L

/** Events further apart than this belong to separate pushes of the wheel or trackpad. */
private const val WHEEL_GAP_MS = 25L

/**
 * MT_SW: the real (opaque) surface colour of the window we are inside of. Inside a window the theme's surface colour is
 * made transparent, so a window opened from within another window (a node picker over a list window, a hike over the
 * hikes list) would otherwise take that transparent colour for its own background and show the window below through.
 */
@Suppress("CompositionLocalAllowlist")
internal val LocalWindowSurface = compositionLocalOf<Color?> { null }

/**
 * MT_SW: told when a window was closed by the wheel or trackpad. The rest of that scroll still arrives and would act on
 * what was under the window (the map zooms), so a platform can set this to swallow it.
 */
object WheelCloseTail {
    var onWindowClosed: () -> Unit = {}
}

/** Width kept free at the right of a window's first row for the close cross (an icon button plus its margin). */
const val SHEET_CROSS_INSET_DP = 60

/** How far the close cross stays from the right edge of the window. */
private const val CROSS_END_DP = 12

/**
 * MT_SW: the same as the Material bottom sheet `ModalBottomSheet` (the parameters the app uses), but in the look of our
 * app: the shadow (a light glow on a dark theme) and the thin rim along the rounded top edge, like the other elements.
 * Use this instead of the Material one so that every sheet looks the same.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER", "UnnecessaryLaunchedEffect")
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState? = null,
    closeCross: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current.density
    // MT_SW: everywhere (phone and computer) the sheet is a pop-up window in the middle, in our look, not a panel
    // coming up from the bottom edge. The content scrolls by itself where it needs to; the window only limits
    // its height (an outer scroll would clash with the content's own one).
    val windowShape = RoundedCornerShape(SHEET_CORNER_DP.dp)
    val windowSurface = LocalWindowSurface.current ?: MaterialTheme.colorScheme.surface
    val overWindow = LocalWindowSurface.current != null
    LaunchedEffect(Unit) { Logger.i { "Okno: otwarte, tło ${if (overWindow) "z okna pod spodem" else "z motywu"}" } }
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier =
            modifier
                .padding(WINDOW_MARGIN_DP.dp)
                .widthIn(max = WINDOW_MAX_WIDTH_DP.dp)
                .fillMaxWidth()
                .heightIn(max = (containerSize.height / density * WINDOW_MAX_HEIGHT_PERCENT).dp)
                .background(windowSurface, windowShape)
                .elementLook(UiOptions.GROUP_DIALOGS, windowShape)
                .popupRim(windowShape)
                // A swipe down anywhere on the window closes it.
                .windowSwipeDownToClose(onDismissRequest)
                // Like the sheet it replaces, the window can be dismissed through accessibility and tests.
                .semantics { dismiss { onDismissRequest().let { true } } },
            shape = windowShape,
            color = Color.Transparent,
        ) {
            // Rows such as ListItem paint the theme's surface colour by default, which would hide the window's
            // gradient behind a flat fill. Inside the window that colour is transparent, so the whole window has
            // one background with one gradient.
            // MT_SW: bez tego na iPhonie efekt gumowy u góry listy sam zjada ruch (consumed != 0),
            // więc przeciągnięcie w dół od góry listy nigdy nie dochodziło do progu zamknięcia okna.
            CompositionLocalProvider(
                LocalWindowSurface provides windowSurface,
                LocalOverscrollFactory provides null,
                // A window opened from a dialog must not inherit the dialog's place for the scroll bar.
                LocalScrollbarOutset provides 0.dp,
            ) {
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Transparent)) {
                    Box {
                        Column { content() }
                        // A close cross in the top right corner, laid over the window (it takes no row of its own, so
                        // there is no empty strip above the title). Headers with buttons of their own leave
                        // SHEET_CROSS_INSET_DP free at the right. Windows that have their own original cross in their
                        // header pass closeCross = false.
                        if (closeCross) {
                            IconButton(
                                onClick = {
                                    Logger.i { "Okno: zamknięte krzyżykiem" }
                                    onDismissRequest()
                                },
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = CROSS_END_DP.dp),
                            ) {
                                Icon(
                                    imageVector = MeshtasticIcons.Close,
                                    contentDescription = stringResource(Res.string.close),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * MT_SW: a swipe down anywhere on the window closes it. Two cases are covered: content that scrolls (the swipe closes
 * the window only when the content is already at its top and has nothing left to scroll, reported by the nested
 * scroll), and content that does not scroll (a plain downward drag that nobody else used). A drag that scrolls the
 * content, or a tap on a button, never closes the window.
 */
@Composable
private fun Modifier.windowSwipeDownToClose(onClose: () -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { SWIPE_CLOSE_DP.dp.toPx() }
    val close = rememberUpdatedState(onClose)
    val gesture = remember { SwipeGesture() }
    val connection = remember {
        object : NestedScrollConnection {
            private var dragged = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // A list received a downward drag. On iPhone its rubber-band effect then swallows the rest of the
                // drag, so the pointer watcher below must keep counting even though the list consumed the move.
                if (source == NestedScrollSource.UserInput && available.y > 0f) gesture.scrollSeen = true
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y != 0f) gesture.contentMoved = true
                // The wheel and the trackpad are judged below (a rest and a new scroll are needed), on every system.
                val fromTouch = !gesture.wheeling && source == NestedScrollSource.UserInput
                if (fromTouch && available.y > 0f && consumed.y == 0f) {
                    dragged += available.y
                    if (dragged > threshold) {
                        dragged = Float.NEGATIVE_INFINITY
                        gesture.closeOnce("Okno: zamknięte przesunięciem listy w dół (lista na górze)", close.value)
                    }
                } else {
                    // MT_SW: diagnostyka - niewykorzystany ruch w dół pojawił się (lista jest blisko góry), ale coś
                    // go przerwało zanim doszedł do progu (np. lista "zjadła" go sama efektem gumowym na iPhonie).
                    if (available.y > 0f && !gesture.closed) {
                        Logger.d {
                            "Okno: przesunięcie listy przerwane (zebrane ${dragged.toInt()}px z " +
                                "${threshold.toInt()}px progu, zabrane przez listę=${consumed.y.toInt()}px, " +
                                "zostało=${available.y.toInt()}px, źródło=$source)"
                        }
                    }
                    dragged = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                dragged = 0f
                return Velocity.Zero
            }
        }
    }
    return nestedScroll(connection)
        .pointerInput(Unit) {
            // Seen before the content scrolls: tells a wheel or trackpad apart from a finger.
            awaitPointerEventScope {
                while (true) {
                    when (awaitPointerEvent(PointerEventPass.Initial).type) {
                        PointerEventType.Scroll -> gesture.wheeling = true
                        PointerEventType.Press -> gesture.wheeling = false
                    }
                }
            }
        }
        .pointerInput(Unit) {
            // The mouse wheel and the trackpad (computer): see [WheelClose].
            awaitPointerEventScope {
                val wheel = WheelClose()
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull()
                    // Empty and sideways events (a trackpad sends them) say nothing and must not break the rest.
                    if (event.type == PointerEventType.Scroll && change != null && change.scrollDelta.y != 0f) {
                        if (wheel.closes(change.uptimeMillis, -change.scrollDelta.y, change.isConsumed)) {
                            Logger.i { "Okno: zamknięte przewinięciem do góry" }
                            WheelCloseTail.onWindowClosed()
                            close.value()
                        }
                    }
                }
            }
        }
        .pointerInput(threshold) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                gesture.reset()
                var dragged = 0f
                var following = true
                var endReason = "palec podniesiony"
                while (following) {
                    val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull()
                    // A move taken by something that is not a list (a slider, a reorder handle) is not ours; a move
                    // taken by a list that is already at its top (iPhone rubber band) still counts. The move is read
                    // with what a list consumed (positionChange() reports zero for it, so a scrolling window never
                    // closed).
                    val takenByOther = change != null && change.isConsumed && !gesture.scrollSeen
                    if (change == null || !change.pressed || takenByOther) {
                        endReason =
                            when {
                                change == null -> "brak zdarzenia"
                                takenByOther -> "ruch zabrany przez inny element"
                                else -> "palec podniesiony"
                            }
                        following = false
                    } else {
                        dragged += change.positionChangeIgnoreConsumed().y
                        if (dragged > threshold && !gesture.contentMoved) {
                            gesture.closeOnce("Okno: zamknięte przeciągnięciem w dół", close.value)
                            following = false
                        }
                    }
                }
                // MT_SW: diagnostyka gestu, gdy ruch był wyraźny, ale okno się NIE zamknęło (np. zgłoszenie z
                // iPhone'a, że przeciągnięcie w dół nie działa) - widać tu dokładnie, co przerwało śledzenie gestu.
                if (!gesture.closed && dragged > threshold * GESTURE_LOG_SHARE) {
                    Logger.d {
                        "Okno: przeciągnięcie w dół nie zamknęło okna (powód: $endReason, przesunięcie " +
                            "${dragged.toInt()}px z ${threshold.toInt()}px progu, treść poruszona=" +
                            "${gesture.contentMoved}, ruch z listy=${gesture.scrollSeen})"
                    }
                }
            }
        }
}

/**
 * Closing by scrolling with a wheel or trackpad, on every system. Scrolling from the bottom only reaches the top, however
 * fast. The window closes when a new push up starts (the events stopped for [WHEEL_GAP_MS]) at least [WHEEL_PAUSE_MS]
 * after the content last moved. A trackpad keeps sending a fading tail after the fingers are lifted, so the rest is
 * counted from the last move of the content and not from the last event.
 */
private class WheelClose {
    private var lastEvent: Long? = null
    private var restFrom: Long? = null
    private var armed = false
    private var ticks = 0f

    fun closes(time: Long, up: Float, usedByContent: Boolean): Boolean {
        val gap = lastEvent?.let { time - it }
        lastEvent = time
        var close = false
        if (usedByContent) {
            restFrom = time
            armed = false
        } else {
            val still = time - (restFrom ?: time.also { restFrom = it })
            if (gap != null && gap >= WHEEL_GAP_MS) {
                armed = still >= WHEEL_PAUSE_MS
                ticks = 0f
                Logger.i { "Okno: nowe przewinięcie na krańcu (przerwa ${gap}ms, treść stoi ${still}ms, ruch $up)" }
            }
            if (armed) {
                // Weak pulls add up; a small move back only takes away from them.
                ticks = (ticks + up).coerceAtLeast(0f)
                close = ticks >= WHEEL_AGAIN_TICKS
            }
        }
        if (close) armed = false
        return close
    }
}

/** Jak duża część progu zamknięcia musi być pokonana, żeby nieudana próba trafiła do logu (pomija drobne drgnięcia). */
private const val GESTURE_LOG_SHARE = 0.3f

/** State of one drag on the window, shared by the list watcher and the pointer watcher. */
private class SwipeGesture {
    var wheeling = false
    var scrollSeen = false
    var contentMoved = false
    var closed = false
        private set

    fun reset() {
        scrollSeen = false
        contentMoved = false
        closed = false
    }

    fun closeOnce(reason: String, close: () -> Unit) {
        if (!closed) {
            closed = true
            Logger.i { reason }
            close()
        }
    }
}
