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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.meshtastic.core.model.ElementStyle
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions
import org.meshtastic.core.ui.util.hideKeyboardOnTapOrScroll

/** What the floating bar tells the screens drawn next to it. */
@Stable
class FloatingBarState {
    /** Whether the bar is out (it hides while a page is scrolled down, when that option is on). */
    var shown by mutableStateOf(true)

    /** How many screens draw under the bar and pad their own list ends (see [FloatingBarUnderlap]). */
    var underlapCount by mutableIntStateOf(0)
}

internal val LocalFloatingBar = staticCompositionLocalOf<FloatingBarState?> { null }

/**
 * The height the floating bar takes from the bottom of the screen, in dp (0 when there is no such bar or it is out).
 * A screen that calls [FloatingBarUnderlap] adds this at the end of its lists.
 */
val LocalFloatingBarInset = compositionLocalOf { 0.dp }

/**
 * Called by a scrolling screen to let its content run under the floating bar (so it shows through the glass): the
 * screen then pads the end of its lists with [LocalFloatingBarInset]. A screen that does not call this simply ends
 * above the bar.
 */
@Composable
fun FloatingBarUnderlap() {
    val state = LocalFloatingBar.current
    DisposableEffect(state) {
        state?.let { it.underlapCount++ }
        onDispose { state?.let { it.underlapCount-- } }
    }
}

private const val BAR_MAX_WIDTH_DP = 560
private const val BAR_CORNER_DP = 32
private const val ITEM_CORNER_DP = 26
private const val BAR_MARGIN_DP = 8
private const val BAR_LIFT_DP = 8
private const val LABEL_SIZE_SP = 10
private const val MIN_STEP_PX = 1f

/**
 * The floating navigation bar over the screen: the content fills the whole area (and is what the glass blurs), the
 * bar floats at the bottom. The bar's look, hiding and vibration follow the "look of the elements" options.
 */
@Composable
internal fun FloatingNavBarHost(
    entries: List<NavEntry>,
    destinationKey: Any?,
    barHeight: Dp?,
    barBottomGap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val options = rememberUiOptions()
    val style = options.element(UiOptions.GROUP_NAV)
    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    val state = remember { FloatingBarState() }
    val keyboardUp = WindowInsets.ime.getBottom(density) > 0
    val hideStep = options[UiOptions.NAV_HIDE]
    val showStep = options[UiOptions.NAV_SHOW]
    val haptic = LocalHapticFeedback.current
    val vibrate = options.flag(UiOptions.NAV_HAPTICS)

    // Another tab, or turning the hiding off, always brings the bar back.
    LaunchedEffect(destinationKey, hideStep) { state.shown = true }

    val hideAfterPx =
        with(density) { UiOptions.HIDE_STEPS_DP[hideStep.coerceIn(0, UiOptions.HIDE_STEPS_DP.lastIndex)].dp.toPx() }
    val showAfterPx =
        with(density) { UiOptions.SHOW_STEPS_DP[showStep.coerceIn(0, UiOptions.SHOW_STEPS_DP.lastIndex)].dp.toPx() }
    val scrollConnection =
        remember(hideStep, showStep, hideAfterPx, showAfterPx) {
            object : NestedScrollConnection {
                private var down = 0f
                private var up = 0f

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (hideStep == 0) return Offset.Zero
                    val dy = available.y
                    if (dy < -MIN_STEP_PX) {
                        up = 0f
                        down -= dy
                        if (down >= hideAfterPx) state.shown = false
                    } else if (dy > MIN_STEP_PX) {
                        down = 0f
                        if (showStep != UiOptions.SHOW_ONLY_AT_TOP) {
                            up += dy
                            if (up >= showAfterPx) state.shown = true
                        }
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    // What the page could not scroll any more is its top: the bar comes back there, in every mode.
                    if (hideStep != 0 && available.y > 0f) state.shown = true
                    return Offset.Zero
                }
            }
        }

    val visible = !keyboardUp && (state.shown || hideStep == 0)
    var barHeightPx by remember { mutableIntStateOf(0) }
    val inset by animateDpAsState(if (visible) with(density) { barHeightPx.toDp() } else 0.dp, label = "BarInset")

    Surface(
        modifier = modifier.hideKeyboardOnTapOrScroll(),
        color = NavigationSuiteScaffoldDefaults.containerColor,
        contentColor = NavigationSuiteScaffoldDefaults.contentColor,
    ) {
        Box(Modifier.fillMaxSize()) {
            // The content is the source the glass blurs; it runs under the bar only for screens that say so.
            Box(
                Modifier.fillMaxSize()
                    .hazeSource(hazeState)
                    .nestedScroll(scrollConnection)
                    .padding(bottom = if (state.underlapCount > 0) 0.dp else inset)
                    .then(if (visible) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
            ) {
                // Only a screen that runs under the bar pads its own list ends; the others end above it (padded below).
                val padInset = if (state.underlapCount > 0) inset else 0.dp
                CompositionLocalProvider(LocalFloatingBar provides state, LocalFloatingBarInset provides padInset) {
                    Row(Modifier.fillMaxSize()) { content() }
                }
            }
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                FloatingNavBar(
                    entries = entries,
                    style = style,
                    hazeState = hazeState,
                    // On the iPhone the home indicator lives below the bar, so it floats a little higher there.
                    bottomMargin = if (barHeight != null) barBottomGap + BAR_LIFT_DP.dp else BAR_MARGIN_DP.dp,
                    onSelect = { entry ->
                        if (vibrate && !entry.selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        entry.onClick()
                    },
                    modifier = Modifier.onSizeChanged { barHeightPx = it.height },
                )
            }
        }
    }
}

/** The floating navigation bar: all the buttons in one rounded island, the open one marked by a rounded highlight. */
@Composable
private fun FloatingNavBar(
    entries: List<NavEntry>,
    style: ElementStyle,
    hazeState: HazeState,
    bottomMargin: Dp,
    onSelect: (NavEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = bottomMargin),
        contentAlignment = Alignment.Center,
    ) {
        StyledSurface(
            style = style,
            shape = RoundedCornerShape(BAR_CORNER_DP.dp),
            modifier = Modifier.widthIn(max = BAR_MAX_WIDTH_DP.dp).fillMaxWidth(),
            hazeState = hazeState,
            floating = true,
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (entry in entries) {
                    FloatingNavItem(entry = entry, onSelect = { onSelect(entry) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** One button of the floating bar: the icon with its short name under it. */
@Composable
private fun FloatingNavItem(entry: NavEntry, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (entry.selected) colorScheme.primary else colorScheme.onSurfaceVariant
    Column(
        modifier =
        modifier
            .clip(RoundedCornerShape(ITEM_CORNER_DP.dp))
            .background(
                if (entry.selected) colorScheme.onSurface.copy(alpha = SELECTED_ALPHA) else Color.Transparent,
            )
            .selectable(selected = entry.selected, onClick = onSelect, role = Role.Tab)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            entry.icon()
            Box(modifier = Modifier.padding(top = 2.dp)) { FitLabel(entry.labelText) }
        }
    }
}

private const val SELECTED_ALPHA = 0.12f

private const val MIN_LABEL_SP = 5f
private const val LABEL_FIT_MARGIN = 0.96f

/**
 * The name of a button on one line, as big as fits: it is measured at the normal size and, when wider than the room
 * the button has, scaled down to that room (so "Network health" is never cut off).
 */
@Composable
private fun FitLabel(text: String) {
    val measurer = rememberTextMeasurer()
    val base = MaterialTheme.typography.labelSmall.copy(fontSize = LABEL_SIZE_SP.sp)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val room = constraints.maxWidth
        val wanted = remember(text, base) { measurer.measure(text, base, softWrap = false, maxLines = 1).size.width }
        val sizeSp =
            if (room == Constraints.Infinity || wanted <= room) {
                LABEL_SIZE_SP.toFloat()
            } else {
                (LABEL_SIZE_SP * room * LABEL_FIT_MARGIN / wanted).coerceAtLeast(MIN_LABEL_SP)
            }
        Text(
            text = text,
            style = base,
            fontSize = sizeSp.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}
