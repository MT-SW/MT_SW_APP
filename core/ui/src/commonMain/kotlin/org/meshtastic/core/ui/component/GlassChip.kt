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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

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
