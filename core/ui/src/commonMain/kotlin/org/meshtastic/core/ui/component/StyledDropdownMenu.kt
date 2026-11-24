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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.UiOptions

private const val MENU_CORNER_DP = 20
private const val MENU_MARGIN_DP = 14
private const val MENU_BACKING_ALPHA = 0.97f
private const val MENU_PADDING_DP = 8

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
    val backing = MaterialTheme.colorScheme.surface.copy(alpha = MENU_BACKING_ALPHA * popupFade())
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = RoundedCornerShape(0.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        // The margin gives the shadow room, so the menu window does not cut the edges of the menu.
        Box(modifier = Modifier.padding(MENU_MARGIN_DP.dp)) {
            Column(
                modifier =
                modifier
                    .elementLook(UiOptions.GROUP_DIALOGS, shape)
                    .background(backing, shape)
                    .popupRim(shape)
                    .clip(shape)
                    .padding(vertical = MENU_PADDING_DP.dp)
                    .width(IntrinsicSize.Max),
            ) {
                content()
            }
        }
    }
}
