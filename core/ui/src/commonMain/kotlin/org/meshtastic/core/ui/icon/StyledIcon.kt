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
package org.meshtastic.core.ui.icon

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions
import androidx.compose.material3.Icon as MaterialIcon

/**
 * MT_SW: the app's `Icon`. It draws every icon of the whole app (settings, lists, buttons, map...) in the style the
 * person chose ("icon style" in the look of the elements): as it is, bolder, or as a thin outline close to the line
 * icons of iPhone. Every other kind of icon is passed on to the standard one unchanged.
 */
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val style = rememberUiOptions()[UiOptions.ICON_STYLE]
    val drawn = remember(imageVector, style) { imageVector.restyledFor(style) }
    MaterialIcon(imageVector = drawn, contentDescription = contentDescription, modifier = modifier, tint = tint)
}

@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = MaterialIcon(painter = painter, contentDescription = contentDescription, modifier = modifier, tint = tint)

@Composable
fun Icon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = MaterialIcon(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier, tint = tint)

/**
 * A painter for [vector] in the chosen icon style: for the small icons that are painted over another icon (a badge, the
 * connection type) and so do not go through [Icon].
 */
@Composable
fun rememberStyledPainter(vector: ImageVector): Painter {
    val style = rememberUiOptions()[UiOptions.ICON_STYLE]
    return rememberVectorPainter(remember(vector, style) { vector.restyledFor(style) })
}

private fun ImageVector.restyledFor(style: Int): ImageVector =
    if (style == UiOptions.ICON_STYLE_STANDARD) this else restyled(style)
