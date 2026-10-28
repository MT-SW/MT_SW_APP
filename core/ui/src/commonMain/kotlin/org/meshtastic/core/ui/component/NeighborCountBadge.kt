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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.max
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.neighbor_info
import org.meshtastic.core.ui.icon.Group
import org.meshtastic.core.ui.icon.MeshtasticIcons

/**
 * Grey "neighbours" icon (people) with the count from the node's last saved neighbour list. It only informs, like the
 * other status icons next to it: it is not a button.
 */
@Composable
fun NeighborCountBadge(count: Int, modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = MeshtasticIcons.Group,
            contentDescription = stringResource(Res.string.neighbor_info),
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Puts [badge] in the same column on every card: a fixed distance from the right edge of the header line, left of the
 * icons on the right (more icons push it further left). The name gets the width up to the badge (it is shortened only
 * when there is no room), [trailing] stays at the right edge of this row, and without a badge the name simply takes
 * the space as before. [extraRight] is the width of the icons that sit to the right of this row on the same line.
 */
@Composable
fun NameRowWithCenteredBadge(
    name: @Composable () -> Unit,
    badge: (@Composable () -> Unit)?,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    extraRight: () -> Int = { 0 },
) {
    Layout(
        content = {
            Box { name() }
            Box { badge?.invoke() }
            Box { trailing() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val trailingPlaceable = measurables[2].measure(loose)
        val badgePlaceable = measurables[1].measure(loose)
        val gap = if (badge != null) MIN_BADGE_GAP.roundToPx() else 0
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity
        val extra = extraRight()
        // The badge sits in the same column on every card: a fixed distance from the right edge of the whole header
        // line (this row plus the icons to its right), wide enough for the usual icons; with more icons than that it
        // moves left so it never covers them.
        val zone = max(BADGE_ZONE.roundToPx(), trailingPlaceable.width + extra + gap)
        val badgeRightFromEnd = if (maxWidth == Constraints.Infinity) Constraints.Infinity else maxWidth + extra - zone
        val nameRoom =
            when {
                maxWidth == Constraints.Infinity -> Constraints.Infinity
                badge == null -> (maxWidth - trailingPlaceable.width - gap).coerceAtLeast(0)
                else -> (badgeRightFromEnd - badgePlaceable.width - gap).coerceAtLeast(0)
            }
        val namePlaceable = measurables[0].measure(Constraints(maxWidth = nameRoom))
        val height = max(namePlaceable.height, max(badgePlaceable.height, trailingPlaceable.height))
        val width =
            if (maxWidth == Constraints.Infinity) {
                namePlaceable.width + badgePlaceable.width + trailingPlaceable.width
            } else {
                maxWidth
            }
        val trailingX = width - trailingPlaceable.width
        val badgeX =
            when {
                badge == null -> 0
                maxWidth == Constraints.Infinity -> namePlaceable.width + gap
                else -> (badgeRightFromEnd - badgePlaceable.width).coerceAtLeast(namePlaceable.width + gap)
            }
        layout(width, height) {
            namePlaceable.placeRelative(0, (height - namePlaceable.height) / 2)
            badgePlaceable.placeRelative(badgeX, (height - badgePlaceable.height) / 2)
            trailingPlaceable.placeRelative(trailingX, (height - trailingPlaceable.height) / 2)
        }
    }
}

private val MIN_BADGE_GAP = 4.dp

/** Distance from the right edge of the header line to the right edge of the neighbour badge (room for about four icons). */
private val BADGE_ZONE = 104.dp
