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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

private const val MENU_CORNER_DP = 20
private const val MENU_SHADOW_DP = 8

/**
 * MT_SW: the same as the Material pop-up menu `DropdownMenu` (the parameters the app uses), but in the look of our app:
 * rounded, with the shadow (a light glow on a dark theme), the gradient sheen and the thin rim, like the other
 * elements. Use this instead of the Material one so that every menu looks the same.
 */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(MENU_CORNER_DP.dp)
    // Opaque, so the text of the screen under the menu never shows through it.
    val backing = MaterialTheme.colorScheme.surface
    // The window itself is the rounded, filled surface (no transparent margin around a smaller bubble), so there is
    // never a see-through strip above or below the content.
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        // The rim goes on the outer modifier, i.e. around the whole surface including the Material inner padding,
        // so no inset strip is left between the rim and the content.
        modifier = modifier.popupRim(shape),
        offset = offset,
        shape = shape,
        containerColor = backing,
        tonalElevation = 0.dp,
        shadowElevation = MENU_SHADOW_DP.dp,
    ) {
        content()
    }
}
