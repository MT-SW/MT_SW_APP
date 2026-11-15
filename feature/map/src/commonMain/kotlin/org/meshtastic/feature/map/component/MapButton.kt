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
package org.meshtastic.feature.map.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.component.StyledSurface
import org.meshtastic.core.ui.component.glassContentColor
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.theme.rememberUiOptions

/** True inside a glass toolbar: its buttons are then plain icons on the toolbar's own surface (no nested glass). */
internal val LocalMapButtonInToolbar = compositionLocalOf { false }

/**
 * A compact round icon button used in map control overlays (zoom, layers, location...). In the plain look it is a
 * [FilledIconButton]; in the other looks chosen for the map ("look of the elements": with depth, see-through or glass)
 * it is a round surface of that look, the same on both flavors.
 */
@Composable
fun MapButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
) {
    val style = rememberUiOptions().element(UiOptions.GROUP_MAP)
    if (style.look == ElementLook.FLAT) {
        FilledIconButton(onClick = onClick, modifier = modifier) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint ?: IconButtonDefaults.filledIconButtonColors().contentColor,
            )
        }
    } else if (LocalMapButtonInToolbar.current) {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint ?: glassContentColor(UiOptions.GROUP_MAP),
            )
        }
    } else {
        StyledSurface(
            style = style,
            shape = CircleShape,
            modifier = modifier,
            baseColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            floating = true,
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconTint ?: glassContentColor(UiOptions.GROUP_MAP),
                )
            }
        }
    }
}
