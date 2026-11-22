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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions

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

private const val STANDARD_BOTTOM_SHIFT = 0.7f
private const val STANDARD_TOP_ALPHA = 1f
private const val STANDARD_BOTTOM_ALPHA = 0.85f
private const val STANDARD_RIM_LIGHTEN = 0.45f
private const val STANDARD_RIM_ALPHA = 0.7f
private const val STANDARD_RIM_DP = 1.5f

/**
 * The fill of a node chip from top to bottom for the chosen [style] (one of `UiOptions.NODE_CHIP_*`): flat is the plain
 * colour, standard a visible light-to-colour gradient that is a little see-through, glass 3D the full glass gradient.
 */
fun nodeChipFillColors(style: Int, base: Color): List<Color> = when (style) {
    UiOptions.NODE_CHIP_FLAT -> listOf(base, base)
    UiOptions.NODE_CHIP_GLASS_3D -> glassChipColors(base)
    else ->
        listOf(
            // The top is the node's own, clearly visible colour; the bottom is much darker, so the white name stays
            // readable on every colour.
            base.copy(alpha = STANDARD_TOP_ALPHA),
            lerp(base, Color.Black, STANDARD_BOTTOM_SHIFT).copy(alpha = STANDARD_BOTTOM_ALPHA),
        )
}

/**
 * The outline of a node chip for [style]: none for flat, the glass one in the text colour for 3D glass and, for the
 * standard style, a light edge of the node's own colour (as on the cards of the node list); null = none.
 */
fun nodeChipRim(style: Int, foreground: Color, base: Color): Color? = when (style) {
    UiOptions.NODE_CHIP_FLAT -> null
    UiOptions.NODE_CHIP_GLASS_3D -> glassChipRim(foreground)
    else -> lerp(base, Color.White, STANDARD_RIM_LIGHTEN).copy(alpha = STANDARD_RIM_ALPHA)
}

/**
 * The look of a node chip in the lists, the top bar and the chat headers, for the style chosen in "Look". Applied to
 * the visible chip only (never to an area with invisible touch padding).
 */
@Composable
fun nodeChipLook(base: Color, foreground: Color, shape: Shape, style: Int = rememberNodeChipStyle()): Modifier {
    val rim = nodeChipRim(style, foreground, base)
    // The standard style casts the shadow of the cards of the node list, the 3D glass the one of the glass inputs.
    val shadowGroup = if (style == UiOptions.NODE_CHIP_GLASS_3D) UiOptions.GROUP_INPUT else UiOptions.GROUP_CARDS
    val shadow = if (style != UiOptions.NODE_CHIP_FLAT) Modifier.elementLook(shadowGroup, shape) else Modifier
    val rimWidth = if (style == UiOptions.NODE_CHIP_STANDARD) STANDARD_RIM_DP.dp else 1.dp
    val border = if (rim != null) Modifier.border(rimWidth, rim, shape) else Modifier
    val fill = Brush.verticalGradient(nodeChipFillColors(style, base))
    return shadow.background(fill, shape).then(border)
}

/** The colour of the name on a node chip: white on the standard style (its fill is dark enough), else [foreground]. */
fun nodeChipTextColor(style: Int, foreground: Color): Color =
    if (style == UiOptions.NODE_CHIP_STANDARD) Color.White else foreground

/** The node chip style chosen in "Look" (one of `UiOptions.NODE_CHIP_*`). */
@Composable fun rememberNodeChipStyle(): Int = rememberUiOptions()[UiOptions.NODE_CHIP_STYLE]

/** The panorama, antenna plot and choice button look chosen in "Look" (one of `UiOptions.VIEW_STYLE_*`). */
@Composable fun rememberViewStyle(): Int = rememberUiOptions()[UiOptions.VIEW_STYLE]

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
    val glass = rememberViewStyle() == UiOptions.VIEW_STYLE_GLASS
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
            .then(if (glass) Modifier.elementLook(UiOptions.GROUP_INPUT, shape) else Modifier)
            .clip(shape)
            .background(Brush.verticalGradient(if (glass) glassChipColors(base) else listOf(base, base)))
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
