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
@file:Suppress("MatchingDeclarationName")

package org.meshtastic.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.radio_chooser_dropped
import org.meshtastic.core.resources.radio_chooser_title
import org.meshtastic.core.resources.radio_chooser_unnamed
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.MeshRadio
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.theme.rememberUiOptions
import org.meshtastic.core.ui.viewmodel.RadioChoice
import org.meshtastic.core.ui.viewmodel.RadioChooserState
import org.meshtastic.core.ui.viewmodel.RadioStatus
import org.meshtastic.proto.User

/**
 * What the radio chooser of the top bar and the connection card can do; they only draw it. The top bar only uses
 * [onChoose] and [onSeen]; the connection card also connects, disconnects and forgets a saved radio, and says which
 * place the next picked device goes to.
 */
data class RadioChooserActions(
    val state: RadioChooserState = RadioChooserState(),
    val onChoose: (Int) -> Unit = {},
    val onConnect: (Int) -> Unit = {},
    val onDisconnect: (Int) -> Unit = {},
    val onRemove: (Int) -> Unit = {},
    val onTargetNextPick: (Int) -> Unit = {},
    val onSeen: () -> Unit = {},
)

private const val PULSE_MAX_ALPHA = 0.55f
private const val PULSE_MIN_ALPHA = 0.08f
private const val UNREAD_MAX = 99

/**
 * The radio chooser: one grey radio icon with the number of connected radios, next to the plate of our node. When the
 * radio in use drops, a soft glow behind the icon pulses (no sound, no window) until it is back or the list is opened.
 * The list only chooses which saved radio is in use; connecting, disconnecting and adding radios is done on the
 * connection card.
 */
@Composable
fun RadioChooserButton(actions: RadioChooserActions, modifier: Modifier = Modifier) {
    val state = actions.state
    if (state.radios.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val options = rememberUiOptions()
    val mode = options[UiOptions.RADIO_ALERT]
    val pulsing = state.dropped && mode == UiOptions.RADIO_ALERT_PULSE && !expanded
    val pulseColor = radioPulseColor(options)
    val alpha = if (pulsing) rememberRadioPulseAlpha(options) else 0f
    val description =
        stringResource(if (state.dropped) Res.string.radio_chooser_dropped else Res.string.radio_chooser_title)
    Box(modifier = modifier) {
        IconButton(
            onClick = {
                expanded = true
                actions.onSeen()
            },
        ) {
            RadioChooserIcon(
                state = state,
                description = description,
                pulseAlpha = alpha,
                pulseColor = pulseColor,
                showCount = options[UiOptions.RADIO_COUNT] == 1 || mode == UiOptions.RADIO_ALERT_DIGIT,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RadioChooserList(state) { radio ->
                expanded = false
                actions.onChoose(radio.id)
            }
        }
    }
}

/** The grey radio icon: the glow of a dropped radio behind it, the new-messages circle and the connected count. */
@Composable
private fun RadioChooserIcon(
    state: RadioChooserState,
    description: String,
    pulseAlpha: Float,
    pulseColor: Color,
    showCount: Boolean,
) {
    Box(
        modifier =
        Modifier.size(32.dp).drawBehind { if (pulseAlpha > 0f) drawCircle(pulseColor.copy(alpha = pulseAlpha)) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(MeshtasticIcons.MeshRadio, contentDescription = description, modifier = Modifier.size(22.dp))
        // The circle with the number of new messages on the devices that are not shown (nothing when zero).
        if (state.unreadTotal > 0) {
            Box(
                modifier =
                Modifier.align(Alignment.TopEnd)
                    .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.unreadTotal > UNREAD_MAX) "$UNREAD_MAX+" else state.unreadTotal.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(horizontal = 3.dp),
                )
            }
        }
        if (showCount) {
            Text(
                text = state.connectedCount.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

/**
 * Only the devices that are really connected, side by side, as small as their plates: connecting and disconnecting is
 * done on the Connect tab.
 */
@Composable
private fun RadioChooserList(state: RadioChooserState, onChoose: (RadioChoice) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.radios
            .filter { it.status == RadioStatus.Connected }
            .forEach { radio -> RadioItem(radio = radio, onChoose = { onChoose(radio) }) }
    }
}

@Composable
@ReadOnlyComposable
private fun radioPulseColor(options: UiOptions): Color {
    val colorIndex = options[UiOptions.RADIO_ALERT_COLOR].coerceIn(UiOptions.RADIO_ALERT_COLORS.indices)
    val themed = UiOptions.RADIO_ALERT_COLORS[colorIndex] == 0L
    return if (themed) MaterialTheme.colorScheme.primary else Color(UiOptions.RADIO_ALERT_COLORS[colorIndex])
}

@Composable
private fun rememberRadioPulseAlpha(options: UiOptions): Float {
    val periods = UiOptions.RADIO_ALERT_PERIOD_MS
    val period = periods[options[UiOptions.RADIO_ALERT_SPEED].coerceIn(periods.indices)]
    return rememberInfiniteTransition(label = "radio-pulse")
        .animateFloat(
            initialValue = PULSE_MIN_ALPHA,
            targetValue = PULSE_MAX_ALPHA,
            animationSpec = infiniteRepeatable(tween(period / 2, easing = LinearEasing), RepeatMode.Reverse),
            label = "radio-pulse-alpha",
        )
        .value
}

@Composable
private fun RadioItem(radio: RadioChoice, onChoose: () -> Unit) {
    Box(modifier = Modifier.clickable(onClick = onChoose).padding(horizontal = 4.dp, vertical = 6.dp)) {
        RadioPlate(radio)
        if (radio.unread > 0) {
            Text(
                text = radio.unread.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onError,
                modifier =
                Modifier.align(Alignment.TopEnd)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
                    .defaultMinSize(minWidth = 16.dp)
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

/**
 * The device by its plate only: the short name in the colour of its node, with no other name. A device whose node is
 * not known yet shows its saved name.
 */
@Composable
fun RadioPlate(radio: RadioChoice, modifier: Modifier = Modifier) {
    val num = radio.nodeNum
    if (num != null) {
        NodeChip(
            node = Node(num = num, user = User.Builder().also { it.short_name = radio.shortName.orEmpty() }.build()),
            modifier = modifier,
        )
    } else {
        Text(
            text = radio.name ?: stringResource(Res.string.radio_chooser_unnamed, radio.id),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            modifier = modifier,
        )
    }
}
