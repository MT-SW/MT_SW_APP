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
@file:Suppress("ComposableParamOrder", "LongMethod")

package org.meshtastic.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.repository.SosBarScreens
import org.meshtastic.core.repository.SosConfig
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.sos_bar_hold_desc
import org.meshtastic.core.resources.sos_button
import org.meshtastic.core.resources.sos_open
import org.meshtastic.core.ui.util.rememberHoldVibration

/**
 * What every top bar shows beside its own buttons, provided once at the app root: the plate of our own node (when the
 * radio is connected), and the red SOS button that opens the SOS screen. [sosScreens] are the keys of the cards (see
 * [SosBarScreens]) that show the SOS button; a null [onOpenSos] means there is nowhere to open it (previews, tests).
 */
data class AppBarExtras(
    val ourNode: Node? = null,
    val connected: Boolean = false,
    val sosScreens: Set<String> = emptySet(),
    val onOpenSos: (() -> Unit)? = null,
    /** Sends the alarm; the SOS button of the bars calls it after being held for [sosHoldSeconds]. Null: no sending. */
    val onSendSos: (() -> Unit)? = null,
    val sosHoldSeconds: Int = SosConfig.BAR_MIN_HOLD_SECONDS,
    /** The radio chooser shown before the plate of our node; empty when no radio is saved. */
    val radios: RadioChooserActions = RadioChooserActions(),
)

@Suppress("CompositionLocalAllowlist")
val LocalAppBarExtras = compositionLocalOf { AppBarExtras() }

/** The cards of the app that can show the SOS button; the key is what is stored in the settings. */
enum class SosBarScreen(val key: String) {
    CONNECTIONS(SosBarScreens.CONNECTIONS),
    MESSAGES(SosBarScreens.MESSAGES),
    NODES(SosBarScreens.NODES),
    MAP(SosBarScreens.MAP),
    SETTINGS(SosBarScreens.SETTINGS),
    AI(SosBarScreens.AI),
    HEALTH(SosBarScreens.HEALTH),
    OTHER(SosBarScreens.OTHER),
}

/**
 * Decides which of the top bars on the screen shows the SOS button. A wide window (desktop, tablet) shows several cards
 * side by side, each with its own bar, and every bar would otherwise draw the button. Each bar reports where its button
 * would be; the one furthest to the right wins (the top one on a tie), so the button sits at the window's right edge
 * and appears once. A bar that has not reported yet shows the button only while no other bar is known.
 */
@Stable
class SosBarSlots {
    private val places = mutableStateMapOf<Any, Pair<Float, Float>>()

    fun report(id: Any, right: Float, top: Float) {
        val old = places[id]
        if (old == null || old.first != right || old.second != top) places[id] = right to top
    }

    fun release(id: Any) {
        places.remove(id)
    }

    private val barEdges = mutableStateMapOf<Any, Pair<Float, Float>>()

    /** A top bar reports where it stands (left and right edge in the window), so neighbours can join up. */
    fun reportBar(id: Any, left: Float, right: Float) {
        val old = barEdges[id]
        if (old == null || old.first != left || old.second != right) barEdges[id] = left to right
    }

    fun releaseBar(id: Any) {
        barEdges.remove(id)
    }

    /** The gap in px between the bar [id] and the nearest bar at its left; null when there is none. */
    fun gapToLeft(id: Any): Float? {
        val mine = barEdges[id] ?: return null
        return barEdges.filter { it.key != id && it.value.second <= mine.first + 1f }
            .minOfOrNull { mine.first - it.value.second }
    }

    /** The gap in px between the bar [id] and the nearest bar at its right; null when there is none. */
    fun gapToRight(id: Any): Float? {
        val mine = barEdges[id] ?: return null
        return barEdges.filter { it.key != id && it.value.first >= mine.second - 1f }
            .minOfOrNull { it.value.first - mine.second }
    }

    /** How many bars are on screen now (more than one: the window shows several cards side by side). */
    val barCount: Int
        get() = places.size

    fun shows(id: Any): Boolean {
        if (id !in places) return places.isEmpty()
        val mine = places.getValue(id)
        return places.none { (other, place) -> other != id && place.beats(mine) }
    }

    private fun Pair<Float, Float>.beats(other: Pair<Float, Float>): Boolean =
        if (kotlin.math.abs(first - other.first) > SAME_EDGE_PX) first > other.first else second < other.second

    private companion object {
        /** Bars of one column differ by a few pixels at most (their other buttons); a pane is far wider. */
        const val SAME_EDGE_PX = 24f
    }
}

/** Null (previews, tests): no bar has to give way, every bar shows the button. */
@Suppress("CompositionLocalAllowlist")
val LocalSosBarSlots = compositionLocalOf<SosBarSlots?> { null }

/** Wraps the SOS button of a top bar so that, with several bars on screen, only one of them draws it. */
@Composable
private fun SosBarSlot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val slots = LocalSosBarSlots.current
    val id = remember { Any() }
    if (slots != null) DisposableEffect(slots, id) { onDispose { slots.release(id) } }
    val placed =
        if (slots == null) {
            modifier
        } else {
            modifier.onGloballyPositioned {
                val bounds = it.boundsInWindow()
                slots.report(id, bounds.right, bounds.top)
            }
        }
    Box(placed) { if (slots == null || slots.shows(id)) content() }
}

/** The part of a bar's width the plate and the radio chooser may take when several bars are side by side. */
private const val SIDE_BY_SIDE_SHARE = 0.4f

/** Same as [LocalSosBarSlots] for the plate of our node and the device list: they too appear once for the window. */
@Suppress("CompositionLocalAllowlist")
val LocalDeviceBarSlots = compositionLocalOf<SosBarSlots?> { null }

/**
 * Wraps the plate of our node and the device list of a top bar so that, with several bars on screen (desktop, tablet),
 * only the one furthest to the right draws them, like the SOS button.
 */
@Composable
fun DeviceBarSlot(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val slots = LocalDeviceBarSlots.current
    val id = remember { Any() }
    if (slots != null) DisposableEffect(slots, id) { onDispose { slots.release(id) } }
    val position =
        if (slots == null) {
            Modifier
        } else {
            Modifier.onGloballyPositioned {
                val bounds = it.boundsInWindow()
                slots.report(id, bounds.right, bounds.top)
            }
        }
    // With several bars side by side each one is only a column wide: the plate and the radio chooser may take at most a
    // part of the bar, so the title and the buttons of the screen are never pushed out of the window.
    val share = if (slots != null && slots.barCount > 1) SIDE_BY_SIDE_SHARE else 1f
    Row(
        modifier
            .then(position)
            .layout { measurable, constraints ->
                val capped = share < 1f && constraints.hasBoundedWidth
                val cap = if (capped) (constraints.maxWidth * share).toInt() else constraints.maxWidth
                val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = cap))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (slots == null || slots.shows(id)) content()
    }
}

/** The SOS button as the top bars draw it: one for the whole window, whatever number of cards are side by side. */
@Composable
fun SosBarChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    holdSeconds: Int = SosConfig.BAR_MIN_HOLD_SECONDS,
    onHoldComplete: (() -> Unit)? = null,
) {
    SosBarSlot(modifier) { SosChip(onClick = onClick, holdSeconds = holdSeconds, onHoldComplete = onHoldComplete) }
}

private val SosRed = Color(0xFFD32F2F)
private val SosGold = Color(0xFFF5C839)
private val SosGrey = Color(0xFF757575)

/**
 * The red SOS button, the same everywhere: on the top bar of every card ([large] false) and as the big button of the
 * SOS screen ([large] true). It is red inside with bold white letters and a gold border; while it is held, the border
 * is traced around the button in white, and when the line closes the alarm is sent ([onHoldComplete]), so it never
 * fires by accident. A tap calls [onClick] (on the bars it opens the SOS screen).
 */
@Composable
fun SosChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    holdSeconds: Int = SosConfig.BAR_MIN_HOLD_SECONDS,
    onHoldComplete: (() -> Unit)? = null,
    large: Boolean = false,
    enabled: Boolean = true,
    /** A short line under the letters of the large button (for example "Hold to send"). */
    hint: String? = null,
) {
    val label = stringResource(Res.string.sos_open)
    val holdLabel = stringResource(Res.string.sos_bar_hold_desc, holdSeconds)
    var pressed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val currentHold by rememberUpdatedState(onHoldComplete)
    val vibration = rememberHoldVibration()
    LaunchedEffect(pressed, holdSeconds, enabled) {
        if (pressed && enabled && currentHold != null) {
            progress.snapTo(0f)
            // The phone buzzes while the button is held, until the line closes or the finger is lifted.
            try {
                vibration.start()
                progress.animateTo(1f, tween(durationMillis = holdSeconds * MILLIS, easing = LinearEasing))
            } finally {
                vibration.stop()
            }
            currentHold?.invoke()
            pressed = false
        } else {
            progress.snapTo(0f)
        }
    }
    val corner = if (large) LARGE_CORNER else SMALL_CORNER
    val stroke = if (large) LARGE_STROKE else SMALL_STROKE
    Box(
        modifier =
        modifier
            .clip(RoundedCornerShape(corner))
            .background(if (enabled) SosRed else SosGrey)
            .then(
                if (large) {
                    Modifier.defaultMinSize(minWidth = LARGE_MIN_WIDTH, minHeight = LARGE_MIN_HEIGHT)
                } else {
                    Modifier
                },
            )
            .drawWithContent {
                drawContent()
                drawSosRing(progress.value, stroke.toPx(), corner.toPx())
            }
            .pointerInput(enabled, onHoldComplete != null) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        }
                    },
                    onTap = { if (enabled) onClick() },
                )
            }
            .semantics {
                contentDescription = if (onHoldComplete != null) holdLabel else label
                this.onClick(label = label) {
                    if (enabled) onClick()
                    enabled
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
            Modifier.width(IntrinsicSize.Min)
                .defaultMinSize(minWidth = if (large) 0.dp else 48.dp, minHeight = if (large) 0.dp else 28.dp)
                .padding(horizontal = if (large) 24.dp else 8.dp, vertical = if (large) 16.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.sos_button),
                style = if (large) MaterialTheme.typography.displayMedium else MaterialTheme.typography.labelLarge,
                fontWeight = if (large) FontWeight.Bold else FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
            )
            if (large && hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The SOS button for the top bar of a screen that builds its own bar instead of using [MainAppBar]; it is the same
 * button, shown when the settings of the SOS screen allow it on the card [screen] (all cards by default).
 */
@Composable
fun SosBarAction(screen: SosBarScreen = SosBarScreen.OTHER, modifier: Modifier = Modifier.padding(horizontal = 4.dp)) {
    val extras = LocalAppBarExtras.current
    val open = extras.onOpenSos ?: return
    if (screen.key !in extras.sosScreens) return
    SosBarChip(
        onClick = open,
        modifier = modifier,
        holdSeconds = extras.sosHoldSeconds,
        onHoldComplete = extras.onSendSos,
    )
}

/** The gold border of the button, and while it is held the white line that traces it around ([progress] 0..1). */
private fun DrawScope.drawSosRing(progress: Float, strokePx: Float, cornerPx: Float) {
    val half = strokePx / 2f
    val radius = CornerRadius((cornerPx - half).coerceAtLeast(0f))
    val path = Path().apply { addRoundRect(RoundRect(half, half, size.width - half, size.height - half, radius)) }
    drawPath(path, if (progress > 0f) SosGold.copy(alpha = DIM_ALPHA) else SosGold, style = Stroke(width = strokePx))
    if (progress > 0f) {
        val measure = PathMeasure().apply { setPath(path, false) }
        val traced = Path()
        measure.getSegment(0f, measure.length * progress, traced, true)
        drawPath(traced, Color.White, style = Stroke(width = strokePx))
    }
}

private val SMALL_CORNER = 8.dp
private val LARGE_CORNER = 24.dp
private val SMALL_STROKE = 3.dp
private val LARGE_STROKE = 7.dp
private val LARGE_MIN_WIDTH = 240.dp
private val LARGE_MIN_HEIGHT = 130.dp
private const val MILLIS = 1000
private const val DIM_ALPHA = 0.35f
