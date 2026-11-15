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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions

/** The share of the icon's width that the line of the outline and of the bold style takes. */
private const val OUTLINE_LINE_SHARE = 0.05f
private const val BOLD_LINE_SHARE = 0.022f

/**
 * MT_SW: loads an icon the same way [vectorResource] does, and redraws it in the style the person chose ("icon style"
 * in the look of the elements): as it is, bolder, or as a thin outline (close to the line icons of iPhone). The same on
 * every platform, because only the vector is rebuilt.
 */
@Composable
fun styledVectorResource(resource: DrawableResource): ImageVector {
    val vector = vectorResource(resource)
    val style = rememberUiOptions()[UiOptions.ICON_STYLE]
    return remember(vector, style) { vector.restyledFor(style) }
}

/** A copy of this icon drawn in the given [style] (see [UiOptions.ICON_STYLE_BOLD] and the outline). */
internal fun ImageVector.restyled(style: Int): ImageVector {
    val line = viewportWidth * if (style == UiOptions.ICON_STYLE_OUTLINE) OUTLINE_LINE_SHARE else BOLD_LINE_SHARE
    val builder =
        ImageVector.Builder(
            name = name,
            defaultWidth = defaultWidth,
            defaultHeight = defaultHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            tintColor = tintColor,
            tintBlendMode = tintBlendMode,
            autoMirror = autoMirror,
        )
    root.copyInto(builder, outline = style == UiOptions.ICON_STYLE_OUTLINE, line = line)
    return builder.build()
}

private fun VectorGroup.copyInto(builder: ImageVector.Builder, outline: Boolean, line: Float) {
    for (node in this) {
        when (node) {
            is VectorPath -> {
                // A path may have a fill that is fully transparent (the thin line icons are drawn only with a stroke):
                // it counts as "no fill", otherwise its line would take the transparent colour and vanish.
                val fillBrush = node.fill
                val fillShown = fillBrush != null && !(fillBrush is SolidColor && fillBrush.value.alpha == 0f)
                val hasStroke = node.stroke != null
                val lineBrush = node.stroke ?: fillBrush ?: SolidColor(Color.White)
                builder.addPath(
                    pathData = node.pathData,
                    pathFillType = node.pathFillType,
                    name = node.name,
                    fill = if (outline) null else fillBrush,
                    fillAlpha = node.fillAlpha,
                    stroke = lineBrush,
                    strokeAlpha = if (hasStroke) node.strokeAlpha else node.fillAlpha,
                    strokeLineWidth = if (hasStroke) maxOf(node.strokeLineWidth, if (fillShown) line else 0f) else line,
                    strokeLineCap = node.strokeLineCap,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = node.strokeLineMiter,
                    trimPathStart = node.trimPathStart,
                    trimPathEnd = node.trimPathEnd,
                    trimPathOffset = node.trimPathOffset,
                )
            }

            is VectorGroup -> {
                builder.addGroup(
                    name = node.name,
                    rotate = node.rotation,
                    pivotX = node.pivotX,
                    pivotY = node.pivotY,
                    scaleX = node.scaleX,
                    scaleY = node.scaleY,
                    translationX = node.translationX,
                    translationY = node.translationY,
                    clipPathData = node.clipPathData,
                )
                node.copyInto(builder, outline, line)
                builder.clearGroup()
            }
        }
    }
}
