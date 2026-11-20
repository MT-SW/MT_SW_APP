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
@file:Suppress(
    "CompositionLocalAllowlist",
    "LongMethod",
    "MatchingDeclarationName",
    "MissingReadOnlyComposable",
    "UnnecessaryLaunchedEffect",
)

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.ElementStyle
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions
import org.meshtastic.core.ui.util.hideKeyboardOnTapOrScroll
import org.meshtastic.core.ui.util.rememberTapHaptic

/** What the floating bar tells the screens drawn next to it. */
@Stable
class FloatingBarState {
    /** Whether the bar is out (it hides while a page is scrolled down, when that option is on). */
    var shown by mutableStateOf(true)

    /** How many screens draw under the bar and pad their own list ends (see [FloatingBarUnderlap]). */
    var underlapCount by mutableIntStateOf(0)

    /** How many screens (the map) ask for a somewhat darker bar, see [FloatingBarDarker]. */
    var darkerCount by mutableIntStateOf(0)

    /**
     * The width in pixels of the conversations list when it stands beside an open conversation (0 when it does not).
     * The bar then moves under that list, so it does not cover the message field of the conversation.
     */
    var sideWidthPx by mutableIntStateOf(0)
}

internal val LocalFloatingBar = staticCompositionLocalOf<FloatingBarState?> { null }

/**
 * The height the floating bar takes from the bottom of the screen, in dp (0 when there is no such bar or it is out). A
 * screen that calls [FloatingBarUnderlap] adds this at the end of its lists.
 */
val LocalFloatingBarInset = compositionLocalOf { 0.dp }

/** The room at the end of a scrolling page for the floating bar, so the last item can be scrolled above it. */
@Composable fun Modifier.floatingBarEndPadding(): Modifier = padding(bottom = LocalFloatingBarInset.current)

/** Called by a screen (the map) that wants the floating bar a little darker than on the other cards. */
@Composable
fun FloatingBarDarker() {
    val state = LocalFloatingBar.current
    DisposableEffect(state) {
        state?.let { it.darkerCount++ }
        onDispose { state?.let { it.darkerCount-- } }
    }
}

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

/** The height of the room for a button name (as before), and the smaller size of a name broken into two lines. */
private const val LABEL_LINE_HEIGHT_SP = 16
private const val SMALL_LABEL_SIZE_SP = 8
private const val TINY_LABEL_SIZE_SP = 7

/** Room kept free across a button name (both sides together), so the corners of the highlight do not cut it. */
private const val LABEL_SIDE_ROOM_DP = 6

private const val BAR_PADDING_DP = 6
private const val ITEM_SIDE_PADDING_DP = 1
private const val MIN_STEP_PX = 1f

/**
 * The floating navigation bar over the screen: the content fills the whole area (and is what the glass blurs), the bar
 * floats at the bottom. The bar's look, hiding and vibration follow the "look of the elements" options.
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
    val tapHaptic = rememberTapHaptic()
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
    // Beside an open conversation the bar stands under the conversations list only (the list's width), at the left.
    val sideWidth = if (state.sideWidthPx > 0) with(density) { state.sideWidthPx.toDp() } else null
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
                    // Only the glass look blurs the content, so only then is it recorded as a source (saves the work).
                    .then(if (style.look == ElementLook.GLASS) Modifier.hazeSource(hazeState) else Modifier)
                    .nestedScroll(scrollConnection)
                    .padding(bottom = if (state.underlapCount > 0 || sideWidth != null) 0.dp else inset)
                    .then(if (visible) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
            ) {
                // Only a screen that runs under the bar pads its own list ends; the others end above it (padded below).
                val padInset = if (state.underlapCount > 0 || sideWidth != null) inset else 0.dp
                CompositionLocalProvider(LocalFloatingBar provides state, LocalFloatingBarInset provides padInset) {
                    Row(Modifier.fillMaxSize()) { content() }
                }
            }
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(if (sideWidth != null) Alignment.BottomStart else Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                FloatingNavBar(
                    entries = entries,
                    style = style,
                    hazeState = hazeState,
                    // On the iPhone the home indicator lives below the bar, so it floats a little higher there.
                    darker = state.darkerCount > 0,
                    selectStyle = options[UiOptions.NAV_SELECT],
                    shadow = options.flag(UiOptions.NAV_SHADOW),
                    rimDp =
                    UiOptions.RIM_STEPS_DP[
                        options[UiOptions.NAV_RIM].coerceIn(0, UiOptions.RIM_STEPS_DP.lastIndex),
                    ],
                    sideWidth = sideWidth,
                    bottomMargin = if (barHeight != null) barBottomGap + BAR_LIFT_DP.dp else BAR_MARGIN_DP.dp,
                    onSelect = { entry ->
                        if (vibrate && !entry.selected) tapHaptic()
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
    darker: Boolean,
    rimDp: Float,
    shadow: Boolean,
    selectStyle: Int,
    sideWidth: Dp?,
    bottomMargin: Dp,
    onSelect: (NavEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
        modifier
            .then(if (sideWidth != null) Modifier.width(sideWidth) else Modifier.fillMaxWidth())
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = bottomMargin),
        contentAlignment = Alignment.Center,
    ) {
        // On the map the bar is denser (it has a moving picture under it that cannot be blurred).
        StyledSurface(
            style = if (darker) style.copy(strength = (style.strength - MAP_BAR_DENSER).coerceAtLeast(0)) else style,
            shape = RoundedCornerShape(BAR_CORNER_DP.dp),
            modifier = Modifier.widthIn(max = BAR_MAX_WIDTH_DP.dp).fillMaxWidth(),
            hazeState = hazeState,
            floating = true,
            castShadow = shadow,
            baseColor =
            if (darker) {
                lerp(colorScheme.surfaceContainerHigh, Color.Black, DARKER_SHARE)
            } else {
                colorScheme.surfaceContainerHigh
            },
            rimDp = rimDp,
            // A low bar: a darker band under the middle of it showed as an odd strip around the names.
            flatBottom = true,
        ) {
            Row(
                modifier = Modifier.padding(BAR_PADDING_DP.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (entry in entries) {
                    FloatingNavItem(
                        entry = entry,
                        selectStyle = selectStyle,
                        onSelect = { onSelect(entry) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * One button of the floating bar: the icon with its short name under it. The open one is marked by a rounded highlight,
 * by a short line under its name, or by both ([UiOptions.NAV_SELECT]); in every case it has the colour of the theme
 * while the others are white.
 */
@Composable
private fun FloatingNavItem(entry: NavEntry, selectStyle: Int, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (entry.selected) colorScheme.primary else colorScheme.onSurface
    val bubble = selectStyle != UiOptions.NAV_SELECT_LINE
    val line = selectStyle != UiOptions.NAV_SELECT_BUBBLE
    Column(
        modifier =
        modifier
            .clip(RoundedCornerShape(ITEM_CORNER_DP.dp))
            .background(
                if (entry.selected && bubble) {
                    colorScheme.onSurface.copy(alpha = SELECTED_ALPHA)
                } else {
                    Color.Transparent
                },
            )
            .selectable(selected = entry.selected, onClick = onSelect, role = Role.Tab)
            .padding(horizontal = ITEM_SIDE_PADDING_DP.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            entry.icon()
            val areaHeight = with(LocalDensity.current) { LABEL_LINE_HEIGHT_SP.sp.toDp() }
            Box(
                modifier = Modifier.padding(top = 2.dp).fillMaxWidth().height(areaHeight),
                contentAlignment = Alignment.Center,
            ) {
                FitLabel(entry.labelText)
            }
        }
        if (line) {
            // The line keeps its place when it is not drawn, so the buttons do not jump when another one is opened.
            Box(
                modifier =
                Modifier.padding(top = 3.dp)
                    .width(SELECT_LINE_WIDTH_DP.dp)
                    .height(SELECT_LINE_HEIGHT_DP.dp)
                    .background(if (entry.selected) colorScheme.primary else Color.Transparent, CircleShape),
            )
        }
    }
}

private const val SELECT_LINE_WIDTH_DP = 24
private const val SELECT_LINE_HEIGHT_DP = 3

private const val SELECTED_ALPHA = 0.12f

/** How much darker the bar is on the map card. */
private const val DARKER_SHARE = 0.22f

/** How much denser (less see-through, in strength points) the bar is on the map card. */
private const val MAP_BAR_DENSER = 30

/**
 * The name of a button, never cut off. It is first drawn normally; when the real layout reports that it did not fit the
 * width of the button, it is drawn again smaller - broken at the space into two lines that together take the height of
 * one normal line (so the bar is no taller), and, if even that does not fit, smaller still. Decided from the real
 * layout, not from a guess of the text width, so it holds on every system and font.
 */
@Composable
private fun FitLabel(text: String) {
    var step by remember(text) { mutableIntStateOf(0) }
    val marginPx = with(LocalDensity.current) { LABEL_SIDE_ROOM_DP.dp.toPx() }
    val small = step > 0
    val shown = if (small && ' ' in text) text.replaceFirst(' ', '\n') else text
    BasicText(
        text = shown,
        style =
        navLabelStyle(small = small, twoLines = small && ' ' in text, tiny = step > 1)
            .copy(color = LocalContentColor.current),
        maxLines = 2,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = Modifier.fillMaxWidth(),
        onTextLayout = { layout ->
            // A name that only just fits is still cut: the rounded corners of the button's highlight take the first and
            // the last letter. So a little room is kept free on both sides.
            var widest = 0f
            for (line in 0 until layout.lineCount) {
                widest = maxOf(widest, layout.getLineRight(line) - layout.getLineLeft(line))
            }
            val overflowing = layout.didOverflowWidth || widest > layout.size.width - marginPx
            if (overflowing && step < 2) step++
        },
    )
}

/** The text style of a button name (the colour is added where it is drawn). */
@Composable
private fun navLabelStyle(small: Boolean = false, twoLines: Boolean = false, tiny: Boolean = false): TextStyle =
    MaterialTheme.typography.labelSmall.copy(
        fontSize =
        (
            if (tiny) {
                TINY_LABEL_SIZE_SP
            } else if (small) {
                SMALL_LABEL_SIZE_SP
            } else {
                LABEL_SIZE_SP
            }
            )
            .sp,
        // Two small lines fill exactly the room of one normal line.
        lineHeight = (if (twoLines) LABEL_LINE_HEIGHT_SP / 2 else LABEL_LINE_HEIGHT_SP).sp,
        textAlign = TextAlign.Center,
    )
