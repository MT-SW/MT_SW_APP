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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.radio_chooser_count
import org.meshtastic.core.resources.radio_chooser_dropped
import org.meshtastic.core.resources.radio_chooser_title
import org.meshtastic.core.resources.radio_chooser_unnamed
import org.meshtastic.core.resources.radio_status_connected
import org.meshtastic.core.resources.radio_status_connecting
import org.meshtastic.core.resources.radio_status_disconnected
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
 * radio in use drops, a soft glow behind the icon pulses (no sound, no window) until it is back or the list is
 * opened. The list only chooses which saved radio is in use; connecting, disconnecting and adding radios is done on
 * the connection card.
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
                // Only the devices that are really connected: connecting and disconnecting is done on the Connect tab.
                state.radios.filter { it.status == RadioStatus.Connected }.forEach { radio ->
                    RadioRow(
                        radio = radio,
                        onChoose = {
                            expanded = false
                            actions.onChoose(radio.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioRow(radio: RadioChoice, onChoose: () -> Unit) {
    Row(
        modifier =
        Modifier.fillMaxWidth().clickable(onClick = onChoose).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The plate of the device: its short name in the colour of its node, like the plate of our node on the bar.
        radio.nodeNum?.let { num ->
            NodeChip(
                node =
                Node(num = num, user = User.Builder().also { it.short_name = radio.shortName.orEmpty() }.build()),
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = radio.name ?: stringResource(Res.string.radio_chooser_unnamed, radio.id),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (radio.active) FontWeight.Bold else FontWeight.Normal,
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
        if (radio.unread > 0) {
            Text(
                text = radio.unread.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onError,
                modifier =
                Modifier.background(MaterialTheme.colorScheme.error, CircleShape)
                    .defaultMinSize(minWidth = 20.dp)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}
