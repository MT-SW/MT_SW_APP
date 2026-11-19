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

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.heightIn
import kotlin.math.roundToInt
import androidx.compose.ui.unit.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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
    val gapLeft = if (plain) null else slots?.gapToLeft(barId)
    val gapRight = if (plain) null else slots?.gapToRight(barId)
    val joinStart = (gapLeft ?: 0f) / 2f
    val joinEnd = (gapRight ?: 0f) / 2f
    val tallestDp = with(LocalDensity.current) { (slots?.tallestBar ?: 0f).toDp() }
    val joinStartDp = with(LocalDensity.current) { joinStart.toDp() }
    val joinEndDp = with(LocalDensity.current) { joinEnd.toDp() }
    val barContent: @Composable () -> Unit = {
        Column(
            modifier =
            if (plain) {
                reported.monochromeLayer()
            } else {
                Modifier.monochromeLayer()
                    .padding(start = joinStartDp, end = joinEndDp)
                    .onSizeChanged { slots?.reportBarHeight(barId, it.height.toFloat()) }
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
        StyledSurface(
            style = barStyle,
            shape =
            RoundedCornerShape(
                bottomStart = if (gapLeft != null) 0.dp else BAR_CORNER_DP.dp,
                bottomEnd = if (gapRight != null) 0.dp else BAR_CORNER_DP.dp,
            ),
            modifier = reported.growSideways(joinStart, joinEnd).heightIn(min = tallestDp),
            baseColor = MaterialTheme.colorScheme.surface,
            floating = true,
        ) {
            barContent()
        }
    }
}

/**
 * The bar is drawn wider by [startPx] at the left and [endPx] at the right, but still reports its own width: it grows
 * over the gap next to it, so its neighbour's bar and this one meet in the middle.
 */
private fun Modifier.growSideways(startPx: Float, endPx: Float): Modifier = if (startPx <= 0f && endPx <= 0f) {
    this
} else {
    layout { measurable, constraints ->
        val start = startPx.roundToInt()
        val extra = start + endPx.roundToInt()
        val placeable = measurable.measure(constraints.offset(horizontal = extra))
        layout((placeable.width - extra).coerceAtLeast(0), placeable.height) { placeable.placeRelative(-start, 0) }
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
