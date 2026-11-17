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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.radio_chooser_add
import org.meshtastic.core.resources.radio_chooser_connect
import org.meshtastic.core.resources.radio_chooser_count
import org.meshtastic.core.resources.radio_chooser_disconnect
import org.meshtastic.core.resources.radio_chooser_dropped
import org.meshtastic.core.resources.radio_chooser_hint
import org.meshtastic.core.resources.radio_chooser_remove
import org.meshtastic.core.resources.radio_chooser_title
import org.meshtastic.core.resources.radio_chooser_unnamed
import org.meshtastic.core.resources.radio_status_connected
import org.meshtastic.core.resources.radio_status_connecting
import org.meshtastic.core.resources.radio_status_disconnected
import org.meshtastic.core.ui.icon.Add
import org.meshtastic.core.ui.icon.Antenna
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.theme.rememberUiOptions
import org.meshtastic.core.ui.viewmodel.RadioChoice
import org.meshtastic.core.ui.viewmodel.RadioChooserState
import org.meshtastic.core.ui.viewmodel.RadioStatus

/** What the radio chooser of the top bar can do; the bar only draws it. */
data class RadioChooserActions(
    val state: RadioChooserState = RadioChooserState(),
    val onChoose: (Int) -> Unit = {},
    val onDisconnect: () -> Unit = {},
    val onRemove: (Int) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onSeen: () -> Unit = {},
)

private const val PULSE_MAX_ALPHA = 0.55f
private const val PULSE_MIN_ALPHA = 0.08f

/**
 * The radio chooser: one grey icon with the number of connected radios. When the radio in use drops, a soft glow
 * behind the icon pulses (no sound, no window) until it is back or the list is opened. The list holds the saved
 * radios; a row is swiped right to connect or disconnect and left to remove it from the list.
 */
@Composable
fun RadioChooserButton(actions: RadioChooserActions, modifier: Modifier = Modifier) {
    val state = actions.state
    if (state.radios.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val options = rememberUiOptions()
    val mode = options[UiOptions.RADIO_ALERT]
    val pulsing = state.dropped && mode == UiOptions.RADIO_ALERT_PULSE && !expanded
    val colorIndex = options[UiOptions.RADIO_ALERT_COLOR].coerceIn(UiOptions.RADIO_ALERT_COLORS.indices)
    val themed = UiOptions.RADIO_ALERT_COLORS[colorIndex] == 0L
    val pulseColor =
        if (themed) MaterialTheme.colorScheme.primary else Color(UiOptions.RADIO_ALERT_COLORS[colorIndex])
    val periods = UiOptions.RADIO_ALERT_PERIOD_MS
    val period = periods[options[UiOptions.RADIO_ALERT_SPEED].coerceIn(periods.indices)]
    val alpha =
        if (pulsing) {
            rememberInfiniteTransition(label = "radio-pulse")
                .animateFloat(
                    initialValue = PULSE_MIN_ALPHA,
                    targetValue = PULSE_MAX_ALPHA,
                    animationSpec = infiniteRepeatable(tween(period / 2, easing = LinearEasing), RepeatMode.Reverse),
                    label = "radio-pulse-alpha",
                )
                .value
        } else {
            0f
        }
    val description =
        stringResource(if (state.dropped) Res.string.radio_chooser_dropped else Res.string.radio_chooser_title)
    Box(modifier = modifier) {
        IconButton(
            onClick = {
                expanded = true
                actions.onSeen()
            },
        ) {
            Box(
                modifier =
                Modifier.size(32.dp).drawBehind { if (alpha > 0f) drawCircle(pulseColor.copy(alpha = alpha)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(MeshtasticIcons.Antenna, contentDescription = description, modifier = Modifier.size(22.dp))
                if (options[UiOptions.RADIO_COUNT] == 1 || mode == UiOptions.RADIO_ALERT_DIGIT) {
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
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(modifier = Modifier.width(300.dp)) {
                Text(
                    text = stringResource(Res.string.radio_chooser_count, state.connectedCount),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                state.radios.forEach { radio ->
                    RadioRow(
                        radio = radio,
                        onChoose = {
                            expanded = false
                            actions.onChoose(radio.id)
                        },
                        onToggleConnection = {
                            if (radio.status == RadioStatus.Disconnected) {
                                actions.onChoose(radio.id)
                            } else {
                                actions.onDisconnect()
                            }
                        },
                        onRemove = { actions.onRemove(radio.id) },
                    )
                }
                if (state.canAdd) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.radio_chooser_add)) },
                        leadingIcon = { Icon(MeshtasticIcons.Add, contentDescription = null) },
                        onClick = {
                            expanded = false
                            actions.onAdd()
                        },
                    )
                }
                Text(
                    text = stringResource(Res.string.radio_chooser_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioRow(
    radio: RadioChoice,
    onChoose: () -> Unit,
    onToggleConnection: () -> Unit,
    onRemove: () -> Unit,
) {
    // The row always settles back (false): the swipe only triggers the action, the list redraws from the state.
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> onToggleConnection()
                    SwipeToDismissBoxValue.EndToStart -> onRemove()
                    SwipeToDismissBoxValue.Settled -> Unit
                }
                false
            },
        )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val toRight = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                modifier =
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp),
                contentAlignment = if (toRight) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Text(
                    text =
                    when {
                        !toRight -> stringResource(Res.string.radio_chooser_remove)
                        radio.status == RadioStatus.Disconnected -> stringResource(Res.string.radio_chooser_connect)
                        else -> stringResource(Res.string.radio_chooser_disconnect)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    ) {
        Row(
            modifier =
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onChoose)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = radio.name ?: stringResource(Res.string.radio_chooser_unnamed, radio.id),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (radio.selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
                Text(
                    text =
                    when (radio.status) {
                        RadioStatus.Connected -> stringResource(Res.string.radio_status_connected)
                        RadioStatus.Connecting -> stringResource(Res.string.radio_status_connecting)
                        RadioStatus.Disconnected -> stringResource(Res.string.radio_status_disconnected)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
