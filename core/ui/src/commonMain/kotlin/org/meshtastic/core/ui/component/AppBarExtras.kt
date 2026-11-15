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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
    LaunchedEffect(pressed, holdSeconds, enabled) {
        if (pressed && enabled && currentHold != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = holdSeconds * MILLIS, easing = LinearEasing))
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
    SosChip(
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
