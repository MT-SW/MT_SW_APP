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
package org.meshtastic.feature.map.maplibre.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

internal val TRIP_DROP_SIZE = androidx.compose.ui.unit.DpSize(48.dp, 60.dp)

/**
 * MT_SW: a drop-shaped marker with the picture of a stop in its round head. The tip of the drop (the bottom middle) is
 * the place. Without a picture yet the head is plain.
 */
internal class TripDropPainter(private val picture: ImageBitmap?, private val color: Color) : Painter() {
    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val radius = size.width / 2f
        val centre = Offset(radius, radius)
        val drop =
            Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(centre, radius))
                moveTo(centre.x - radius * TAIL_SPREAD, centre.y + radius * TAIL_START)
                lineTo(centre.x, size.height)
                lineTo(centre.x + radius * TAIL_SPREAD, centre.y + radius * TAIL_START)
                close()
            }
        drawPath(drop, Color.White)
        val inner = radius - RIM_PX
        drawCircle(color, inner, centre)
        if (picture != null) {
            val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(centre, inner)) }
            clipPath(circle) {
                drawImage(
                    image = picture,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(picture.width, picture.height),
                    dstOffset = IntOffset((centre.x - inner).toInt(), (centre.y - inner).toInt()),
                    dstSize = IntSize((inner * 2).toInt(), (inner * 2).toInt()),
                )
            }
        }
        drawCircle(Color.White, inner, centre, style = Stroke(width = RIM_PX / 2f))
    }

    private companion object {
        const val TAIL_SPREAD = 0.55f
        const val TAIL_START = 0.6f
        const val RIM_PX = 6f
    }
}
