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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.OfflineShare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuFAB(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<MenuFABItem>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    testTag: String? = null,
) {
    val stateAwareDescription = if (expanded) stringResource(Res.string.close) else contentDescription
    // MT_SW: the same glass look as the other buttons (outline and glass, filled glass or plain Material).
    val accent = MaterialTheme.colorScheme.primary
    val decor = rememberDecorFor(accent, accent, enabled = true)
    val textColor = decor.outline?.text ?: MaterialTheme.colorScheme.onPrimary
    FloatingActionButtonMenu(
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        expanded = expanded,
        button = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { stateAwareDescription?.let { PlainTooltip { Text(it) } } },
                state = rememberTooltipState(),
            ) {
                val imageVector = if (expanded) MeshtasticIcons.Close else MeshtasticIcons.OfflineShare
                if (decor.custom) {
                    // The glass button is drawn here and not by the Material toggle button: that one adds its own
                    // shadow, which showed as a different coloured square in the middle of a see-through button.
                    val corner by animateDpAsState(if (expanded) MENU_FAB_ROUND_DP.dp else MENU_FAB_CORNER_DP.dp)
                    val shape = RoundedCornerShape(corner)
                    Box(
                        modifier =
                        Modifier.size(MENU_FAB_SIZE_DP.dp)
                            .glassLook(shape, decor)
                            .clip(shape)
                            .toggleable(value = expanded, role = Role.Switch, onValueChange = onExpandedChange),
                        contentAlignment = Alignment.Center,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides textColor) {
                            Icon(imageVector = imageVector, contentDescription = stateAwareDescription)
                        }
                    }
                } else {
                    ToggleFloatingActionButton(
                        checked = expanded,
                        onCheckedChange = onExpandedChange,
                        content = { Icon(imageVector = imageVector, contentDescription = stateAwareDescription) },
                    )
                }
            }
        },
        horizontalAlignment = Alignment.End,
    ) {
        items.forEach { item ->
            val select = {
                item.onClick()
                onExpandedChange(false)
            }
            val itemModifier = if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier
            if (decor.custom) {
                val shape = CircleShape
                Row(
                    modifier =
                    itemModifier
                        .height(MENU_FAB_SIZE_DP.dp)
                        .glassLook(shape, decor)
                        .clip(shape)
                        .clickable(role = Role.Button, onClick = select)
                        .padding(horizontal = MENU_ITEM_PADDING_DP.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MENU_ITEM_GAP_DP.dp),
                ) {
                    CompositionLocalProvider(LocalContentColor provides textColor) {
                        Icon(item.icon, contentDescription = null)
                        Text(item.label)
                    }
                }
            } else {
                FloatingActionButtonMenuItem(
                    modifier = itemModifier,
                    onClick = select,
                    icon = { Icon(item.icon, contentDescription = null) },
                    text = { Text(item.label) },
                )
            }
        }
    }
}

private fun Modifier.glassLook(shape: Shape, decor: FilledDecor): Modifier = when {
    decor.outline != null -> outlineGlassSurface(shape, decor)
    decor.glass != null -> glassSurface(shape, decor.glass)
    else -> this
}

data class MenuFABItem(val label: String, val icon: ImageVector, val onClick: () -> Unit, val testTag: String? = null)

private const val MENU_FAB_CORNER_DP = 16
private const val MENU_FAB_ROUND_DP = 28
private const val MENU_FAB_SIZE_DP = 56
private const val MENU_ITEM_PADDING_DP = 20
private const val MENU_ITEM_GAP_DP = 12
