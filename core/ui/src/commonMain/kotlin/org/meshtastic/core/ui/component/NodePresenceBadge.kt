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
@file:Suppress("ModifierWithoutDefault", "ReturnCount")

package org.meshtastic.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.NodePresence
import org.meshtastic.core.model.NodePresenceState
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.presence_active
import org.meshtastic.core.resources.presence_active_hours
import org.meshtastic.core.resources.presence_active_min
import org.meshtastic.core.resources.presence_inactive
import org.meshtastic.core.resources.presence_inactive_hours
import org.meshtastic.core.resources.presence_inactive_min
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Person
import org.meshtastic.core.ui.theme.StatusColors.StatusGreen
import org.meshtastic.core.ui.theme.rememberUiOptions
import org.meshtastic.core.ui.util.LocalNodePresence

private const val MINUTES_PER_HOUR = 60
private const val MILLIS_PER_MINUTE = 60_000L

/**
 * MT_SW: what to show for the activity status of node [nodeNum] right now: `null` while nothing should be shown (the
 * icon is off, the node shares no status, or an "active" report has gone stale). Reads the shared holder, so only the
 * caller (the small badge) is redrawn when a status changes.
 */
@Composable
fun rememberNodePresence(nodeNum: Int, lastHeardSeconds: Int): Pair<NodePresence, NodePresenceState>? {
    val options = rememberUiOptions()
    val holder = LocalNodePresence.current ?: return null
    if (!options.flag(UiOptions.PRESENCE_SHOW)) return null
    val presence = holder.value[nodeNum] ?: return null
    val expiryMinutes =
        NodePresence.EXPIRY_STEPS_MINUTES[
            options[UiOptions.PRESENCE_EXPIRY].coerceIn(0, NodePresence.EXPIRY_STEPS_MINUTES.lastIndex),
        ]
    val state = presence.effective(nowMillis, lastHeardSeconds, expiryMinutes * MILLIS_PER_MINUTE) ?: return null
    return presence to state
}

/** The small activity status icon of a node, in the look chosen in the settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodePresenceBadge(presence: NodePresence, state: NodePresenceState, modifier: Modifier = Modifier) {
    val options = rememberUiOptions()
    val active = state == NodePresenceState.ONLINE
    val activeColor =
        if (options[UiOptions.PRESENCE_COLOR] == 0) {
            MaterialTheme.colorScheme.StatusGreen
        } else {
            MaterialTheme.colorScheme.primary
        }
    val inactiveColor = LocalContentColor.current.copy(alpha = INACTIVE_ALPHA)
    val color = if (active) activeColor else inactiveColor
    val outline = MaterialTheme.colorScheme.surface

    val minutes = ((nowMillis - presence.sinceMillis) / MILLIS_PER_MINUTE).coerceAtLeast(0).toInt()
    val shortLabel = stringResource(if (active) Res.string.presence_active else Res.string.presence_inactive)
    val tooltip =
        when {
            minutes <= 0 -> shortLabel

            minutes < MINUTES_PER_HOUR ->
                stringResource(
                    if (active) Res.string.presence_active_min else Res.string.presence_inactive_min,
                    minutes,
                )

            else ->
                stringResource(
                    if (active) Res.string.presence_active_hours else Res.string.presence_inactive_hours,
                    minutes / MINUTES_PER_HOUR,
                )
        }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        Box(modifier = modifier.size(20.dp), contentAlignment = Alignment.Center) {
            when (options[UiOptions.PRESENCE_ICON]) {
                UiOptions.PRESENCE_ICON_PERSON -> {
                    Icon(
                        imageVector = MeshtasticIcons.Person,
                        contentDescription = shortLabel,
                        modifier = Modifier.size(20.dp),
                        tint = if (active) activeColor else inactiveColor,
                    )
                    Dot(active, color, outline, Modifier.size(9.dp).align(Alignment.BottomEnd))
                }

                UiOptions.PRESENCE_ICON_RING -> {
                    val ringColor = color
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val stroke = 1.5.dp.toPx()
                        drawCircle(
                            color = ringColor,
                            radius = size.minDimension / 2 - stroke / 2,
                            style = Stroke(width = stroke),
                        )
                        if (active) {
                            drawCircle(
                                color = ringColor,
                                radius = size.minDimension / 4,
                                center = Offset(size.width / 2, size.height / 2),
                            )
                        }
                    }
                }

                else -> Dot(active, color, outline, Modifier.size(12.dp))
            }
        }
    }
}

/** Active: a filled dot with a light outline so it stays visible on any card. Inactive: a grey outline only. */
@Composable
private fun Dot(active: Boolean, color: Color, outline: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.5.dp.toPx()
        if (active) {
            drawCircle(color = outline, radius = size.minDimension / 2)
            drawCircle(color = color, radius = size.minDimension / 2 - stroke)
        } else {
            drawCircle(color = color, radius = size.minDimension / 2 - stroke / 2, style = Stroke(width = stroke))
        }
    }
}

private const val INACTIVE_ALPHA = 0.6f
