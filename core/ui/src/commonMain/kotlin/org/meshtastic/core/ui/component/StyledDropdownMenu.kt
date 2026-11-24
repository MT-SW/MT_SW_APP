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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuGroupShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.UiOptions

private const val MENU_CORNER_DP = 20
private const val MENU_PADDING_DP = 8

/**
 * MT_SW: the same as the Material pop-up menu `DropdownMenu` (the parameters the app uses), but in the look of our app.
 * Only the position and the open/close behaviour come from Material ([DropdownMenuPopup]); the surface is drawn here,
 * so the shadow, the gradient sheen and the rim belong to the menu itself and there is no reserved empty area around
 * it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(MENU_CORNER_DP.dp)
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest, offset = offset) {
        Box(
            modifier =
            modifier
                // The fill goes first so the look's own gradient and sheen are drawn over it, not hidden under it.
                .background(MaterialTheme.colorScheme.surface, shape)
                .elementLook(UiOptions.GROUP_DIALOGS, shape)
                .popupRim(shape)
                .clip(shape),
        ) {
            Column(
                modifier =
                Modifier.width(IntrinsicSize.Max)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = MENU_PADDING_DP.dp),
            ) {
                content()
            }
        }
    }
}

/**
 * MT_SW: replaces the Material menu block `DropdownMenuGroup`, which draws its own rounded box. Here the block adds
 * nothing of its own: the items sit directly on the menu surface, with a small space between the blocks.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun DropdownMenuGroup(
    shapes: MenuGroupShapes,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(vertical = 2.dp)) { content() }
}
