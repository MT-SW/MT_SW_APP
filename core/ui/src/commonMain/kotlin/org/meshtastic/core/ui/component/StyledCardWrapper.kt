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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.ui.theme.rememberUiOptions
import androidx.compose.material3.Card as MaterialCard

/**
 * MT_SW: the app's `Card`. Every card of the app (connections, details, settings, lists...) follows the look chosen for
 * the group of the current screen (see [LocalCardGroup]): plain as before, with depth, see-through or glass. With the
 * plain look it is exactly the standard card.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val group = LocalCardGroup.current
    val options = rememberUiOptions()
    val look = options.element(group).look
    if (look == ElementLook.FLAT) {
        MaterialCard(modifier, shape, colors, elevation, border, content)
    } else {
        MaterialCard(
            modifier = modifier.styledLook(group, shape, look, options.rimDp()),
            shape = shape,
            colors = colors.copy(containerColor = cardContainerColor(colors.containerColor, group)),
            elevation = elevation,
            border = border,
            content = content,
        )
    }
}

/** The clickable card, in the same look as [Card]. */
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val group = LocalCardGroup.current
    val options = rememberUiOptions()
    val look = options.element(group).look
    if (look == ElementLook.FLAT) {
        MaterialCard(onClick, modifier, enabled, shape, colors, elevation, border, content = content)
    } else {
        MaterialCard(
            onClick = onClick,
            modifier = modifier.styledLook(group, shape, look, options.rimDp()),
            enabled = enabled,
            shape = shape,
            colors = colors.copy(containerColor = cardContainerColor(colors.containerColor, group)),
            elevation = elevation,
            border = border,
            content = content,
        )
    }
}

@Composable
private fun Modifier.styledLook(group: String, shape: Shape, look: Int, rimDp: Float): Modifier {
    val withDepth = elementLook(group, shape)
    val seeThrough = look == ElementLook.TRANSPARENT || look == ElementLook.GLASS
    return if (seeThrough) withDepth.glassEdge(shape, rimDp) else withDepth
}
