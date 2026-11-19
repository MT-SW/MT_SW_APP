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
@file:Suppress("LongMethod")

package org.meshtastic.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ic_meshtastic
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.ui.icon.ArrowBack
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.theme.LocalEventTheme
import org.meshtastic.core.ui.theme.monochromeLayer
import org.meshtastic.core.ui.theme.rememberUiOptions
import androidx.compose.material3.Icon as StandardIcon

/** Alpha for the ambient event accent wash over the app bar — subtle enough to keep title text legible. */
private const val EVENT_ACCENT_ALPHA = 0.12f
private const val BAR_CORNER_DP = 20
private const val BAR_SHADOW_ROOM_DP = 24
private const val BAR_SHADOW_DP = 8

/** Height of the event brand rule under the app bar. A hairline: brand presence without stealing vertical space. */
private val EVENT_BRAND_RULE_HEIGHT = 3.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    ourNode: Node?,
    showNodeChip: Boolean,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    onClickChip: (Node) -> Unit,
    // The card this bar belongs to, which decides whether the red SOS button is shown (a setting of the person), and
    // whether this bar may carry it at all (the SOS screen itself does not).
    sosScreen: SosBarScreen = SosBarScreen.OTHER,
    showSos: Boolean = true,
    // Trailing slot: a @Composable content lambda, not an event handler (detekt LambdaParameterEventTrailing).
    actions: @Composable () -> Unit,
) {
    // Ambient event theming: when connected to event firmware (and not opted out), tint the bar with a faint wash of
    // the edition's accent color. Gated with the app-wide fonts via LocalEventTheme / the "Use event theme" toggle.
    val eventTheme = LocalEventTheme.current
    val accent = eventTheme?.accent
    val colors =
        if (accent != null) {
            TopAppBarDefaults.topAppBarColors(
                containerColor =
                accent.copy(alpha = EVENT_ACCENT_ALPHA).compositeOver(MaterialTheme.colorScheme.surface),
            )
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    // The look chosen for the top bar: plain as before, or a surface with depth / see-through / glass (rounded below).
    val barStyle = rememberUiOptions().element(UiOptions.GROUP_TOPBAR)
    val plain = barStyle.look == ElementLook.FLAT
    val barColors = if (plain) colors else TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    // With several bars side by side (desktop, tablet) each one reports where it stands: the columns touch, so the bars
    // reach the edges of the window and join in the middle (their inner bottom corners are straight). Should a gap
    // remain between two bars, each one grows over half of it (the content of the bar keeps its place).
    val slots = LocalDeviceBarSlots.current
    val barId = remember { Any() }
    if (slots != null) DisposableEffect(slots, barId) { onDispose { slots.releaseBar(barId) } }
    val reported =
        if (slots == null) {
            modifier
        } else {
            modifier.onGloballyPositioned {
                val bounds = it.boundsInWindow()
                slots.reportBar(barId, bounds.left, bounds.right)
            }
        }
    val tallestDp = with(LocalDensity.current) { (slots?.tallestBar ?: 0f).toDp() }
    val barContent: @Composable () -> Unit = {
        Column(
            modifier =
            if (plain) {
                reported.monochromeLayer()
            } else {
                Modifier.monochromeLayer().onSizeChanged { slots?.reportBarHeight(barId, it.height.toFloat()) }
            },
        ) {
            TopAppBar(
                colors = barColors,
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                subtitle = {
                    subtitle?.let {
                        Text(
                            text = it,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon =
                if (canNavigateUp) {
                    {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = MeshtasticIcons.ArrowBack,
                                contentDescription = stringResource(Res.string.navigate_back),
                            )
                        }
                    }
                } else {
                    // The Meshtastic logo is never swapped for event branding — the app's identity stays put. Event
                    // firmware is surfaced on the Connections screen instead (EventFirmwareCard).
                    {
                        // Our logo always stays as it is, whatever icon style is chosen.
                        StandardIcon(
                            imageVector = vectorResource(Res.drawable.ic_meshtastic),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    TopBarActions(
                        ourNode = ourNode,
                        showNodeChip = showNodeChip,
                        sosScreen = sosScreen.takeIf { showSos },
                        actions = actions,
                        onClickChip = onClickChip,
                    )
                },
            )
            EventPaletteStrip(palette = eventTheme?.palette.orEmpty(), height = EVENT_BRAND_RULE_HEIGHT)
        }
    }
    if (plain) {
        barContent()
    } else {
        // Wide windows (desktop, tablet): one straight bar over the whole width of its card, right to the edges of the
        // window; side by side the bars have the same height and touch, so they read as a single bar, and the shadow
        // falls only below it. On a phone the bar stays as it was, with rounded bottom corners.
        val wide = isAdaptiveTwoPaneSplit()
        // A single flat colour all over the bar (no light edge, no sheen, nothing lighter behind the texts or icons),
        // whatever look is chosen; only the shadow under it. Wide windows (desktop, tablet): a straight bar to the
        // edges
        // of the window, side by side equally tall, the shadow only below. A phone keeps the rounded bottom corners.
        Surface(
            modifier =
            if (wide) {
                reported.heightIn(min = tallestDp).drawWithContent {
                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height + BAR_SHADOW_ROOM_DP.dp.toPx(),
                    ) {
                        this@drawWithContent.drawContent()
                    }
                }
            } else {
                reported
            },
            shape =
            if (wide) {
                RectangleShape
            } else {
                RoundedCornerShape(bottomStart = BAR_CORNER_DP.dp, bottomEnd = BAR_CORNER_DP.dp)
            },
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = BAR_SHADOW_DP.dp,
        ) {
            barContent()
        }
    }
}

@Composable
private fun TopBarActions(
    ourNode: Node?,
    showNodeChip: Boolean,
    sosScreen: SosBarScreen?,
    actions: @Composable () -> Unit,
    onClickChip: (Node) -> Unit,
) {
    // The same two things on every card: the plate of our node while the radio is connected, and the SOS button. A bar
    // that was given no node of its own takes the one from the app root, so no card is left without them.
    val extras = LocalAppBarExtras.current
    val chipNode = ourNode ?: extras.ourNode
    // The plate is only for the main cards (they say so with showNodeChip); the others show just the SOS button.
    val chipVisible = chipNode != null && showNodeChip && (ourNode != null || extras.connected)
    val openSos = extras.onOpenSos?.takeIf { sosScreen != null && sosScreen.key in extras.sosScreens }

    if (openSos != null) {
        SosBarChip(
            onClick = openSos,
            modifier = Modifier.padding(start = 8.dp),
            holdSeconds = extras.sosHoldSeconds,
            onHoldComplete = extras.onSendSos,
        )
    }
    // With several cards side by side (desktop, tablet) the plate and the device list appear once, on the right bar.
    DeviceBarSlot {
        AnimatedVisibility(visible = chipVisible, enter = fadeIn(), exit = fadeOut()) {
            chipNode?.let { node ->
                NodeChip(modifier = Modifier.padding(start = 16.dp), node = node, onClick = onClickChip)
            }
        }
        // The radio chooser sits right next to the plate of our node (after the SOS button when there is no plate).
        RadioChooserButton(actions = extras.radios, modifier = Modifier.padding(end = 8.dp))
    }

    actions()
}
