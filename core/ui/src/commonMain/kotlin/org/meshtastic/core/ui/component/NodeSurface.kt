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

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import org.meshtastic.core.ui.theme.LocalColorSaturation
import org.meshtastic.core.ui.theme.saturated
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Shared node-tinted surface treatment so node cards and message bubbles match. A node's identity color is applied as a
 * faint wash over the neutral card surface plus a node-colored outline — the more accessible alternative to a heavily
 * saturated fill, since content keeps the surface/onSurface AA pairing while still reading as "this node".
 */
const val NODE_TINT_NORMAL = 0.08f
const val NODE_TINT_EMPHASIZED = 0.16f
const val NODE_TINT_MUTED = 0.04f

private const val NODE_BORDER_ACTIVE_ALPHA = 0.65f
private const val NODE_BORDER_INACTIVE_ALPHA = 0.3f

/** Neutral card surface washed [fraction] toward [nodeColor]; falls back to the plain surface when transparent. */
@Composable
fun nodeTintedContainer(nodeColor: Color, fraction: Float = NODE_TINT_NORMAL): Color {
    val base = CardDefaults.cardColors().containerColor
    if (nodeColor == Color.Transparent) return base
    val strength = LocalColorSaturation.current / PERCENT
    return lerp(base, nodeColor.saturated(strength), (fraction * strength.toFloat()).coerceAtMost(MAX_TINT))
}

private const val PERCENT = 100.0
private const val MAX_TINT = 0.4f

/** Node-colored outline; [active] (selected / online) uses a stronger alpha. Null when there's no node color. */
@Composable
fun nodeBorderStroke(nodeColor: Color, active: Boolean): BorderStroke? {
    if (nodeColor == Color.Transparent) return null
    val strength = LocalColorSaturation.current / PERCENT
    val alpha = if (active) NODE_BORDER_ACTIVE_ALPHA else NODE_BORDER_INACTIVE_ALPHA
    return BorderStroke(
        1.5.dp,
        nodeColor.saturated(strength).copy(alpha = (alpha * strength.toFloat()).coerceAtMost(1f)),
    )
}
