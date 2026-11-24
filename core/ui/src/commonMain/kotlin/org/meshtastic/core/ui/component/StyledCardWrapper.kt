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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.ui.theme.rememberUiOptions

/** MT_SW: the look chosen for the group of cards of the current screen, put on a card with its own colours. */
@Composable
private fun Modifier.cardGroupLook(shape: Shape): Modifier {
    val options = rememberUiOptions()
    val group = LocalCardGroup.current
    val style = options.element(group)
    val look = elementLook(group, shape)
    val seeThrough = style.look == ElementLook.TRANSPARENT || style.look == ElementLook.GLASS
    return if (seeThrough) look.glassEdge(shape, options.rimDp(), sheenBoost = glassSheenBoost()) else look
}

@Composable
private fun CardColors.inCardGroup(): CardColors = copy(containerColor = cardContainerColor(containerColor))

/**
 * MT_SW: the same as the Material `Card`, but in the look chosen in "Look" for the group of cards of the screen
 * (plain, with depth, see-through or glass, with the shadow and the thin rim). Use this instead of the Material one so
 * that every card can be adjusted by the person.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) = androidx.compose.material3.Card(
    modifier = modifier.cardGroupLook(shape),
    shape = shape,
    colors = colors.inCardGroup(),
    elevation = elevation,
    border = border,
    content = content,
)

/** The clickable card, see the other [Card]. */
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) = androidx.compose.material3.Card(
    onClick = onClick,
    modifier = modifier.cardGroupLook(shape),
    enabled = enabled,
    shape = shape,
    colors = colors.inCardGroup(),
    elevation = elevation,
    border = border,
    interactionSource = interactionSource,
    content = content,
)

/** The raised card, see [Card]. */
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit,
) = androidx.compose.material3.ElevatedCard(
    modifier = modifier.cardGroupLook(shape),
    shape = shape,
    colors = colors.inCardGroup(),
    elevation = elevation,
    content = content,
)

/** The clickable raised card, see [Card]. */
@Composable
fun ElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) = androidx.compose.material3.ElevatedCard(
    onClick = onClick,
    modifier = modifier.cardGroupLook(shape),
    enabled = enabled,
    shape = shape,
    colors = colors.inCardGroup(),
    elevation = elevation,
    interactionSource = interactionSource,
    content = content,
)
