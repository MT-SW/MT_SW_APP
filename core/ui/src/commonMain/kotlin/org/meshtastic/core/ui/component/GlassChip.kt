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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.UiOptions

/**
 * MT_SW: the glass look of a node chip, shared by the chip in the lists and the top bar ([NodeChip]) and the chip the
 * map draws, so they look the same everywhere: a lighter top fading into the node's own colour, and a thin outline.
 */
private const val GLASS_TOP_LIGHTEN = 0.30f
private const val GLASS_BOTTOM_DARKEN = 0.12f
private const val GLASS_TOP_ALPHA = 0.94f
private const val GLASS_MIDDLE_ALPHA = 0.90f
private const val GLASS_BOTTOM_ALPHA = 0.92f
private const val RIM_ALPHA = 0.55f

/** The colours of the glass fill from top to bottom, for `Brush.verticalGradient`. */
fun glassChipColors(base: Color): List<Color> = listOf(
    lerp(base, Color.White, GLASS_TOP_LIGHTEN).copy(alpha = GLASS_TOP_ALPHA),
    base.copy(alpha = GLASS_MIDDLE_ALPHA),
    lerp(base, Color.Black, GLASS_BOTTOM_DARKEN).copy(alpha = GLASS_BOTTOM_ALPHA),
)

/** The thin outline of a glass chip: the colour of its text, half see-through, so it shows on pale and dark chips. */
fun glassChipRim(foreground: Color): Color = foreground.copy(alpha = RIM_ALPHA)

private const val CHOICE_CHIP_CORNER_DP = 12
private const val CHOICE_CHIP_SELECTED_TINT = 0.18f
private const val CHOICE_CHIP_SELECTED_RIM_DP = 2

/**
 * MT_SW: a choice button in the glass look (gradient fill, thin outline, the soft shadow of the chosen look). The
 * chosen one has the yellow outline and a faint yellow tint; [outline] gives an unchosen one a colour of its own.
 * Drawn by hand, without the invisible touch area of the Material chip, so the shadow hugs the visible button.
 */
@Composable
fun GlassChoiceChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    outline: Color? = null,
    label: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(CHOICE_CHIP_CORNER_DP.dp)
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val base = if (selected) lerp(surface, primary, CHOICE_CHIP_SELECTED_TINT) else surface
    val rim =
        when {
            selected -> primary
            outline != null -> outline
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    val rimWidth = if (selected) CHOICE_CHIP_SELECTED_RIM_DP.dp else 1.dp
    Box(
        modifier =
        modifier
            .elementLook(UiOptions.GROUP_INPUT, shape)
            .clip(shape)
            .background(Brush.verticalGradient(glassChipColors(base)))
            .border(rimWidth, rim, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val content = if (selected) primary else MaterialTheme.colorScheme.onSurface
        CompositionLocalProvider(LocalContentColor provides content) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { label() }
        }
    }
}
