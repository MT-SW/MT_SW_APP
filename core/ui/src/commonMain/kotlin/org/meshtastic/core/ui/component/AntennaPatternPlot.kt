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
@file:Suppress("MagicNumber")

package org.meshtastic.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import org.meshtastic.core.model.antenna.AntennaPatterns
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The horizontal pattern of an antenna seen from above: the main direction points up, the rings are 3, 10 and 20 dB
 * below the main direction. An antenna that radiates the same everywhere is a plain circle.
 */
@Composable
fun AntennaPatternPlot(beamwidthDeg: Double, frontToBackDb: Double, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val loss = AntennaPatterns.lossAround(beamwidthDeg, frontToBackDb)
    Canvas(modifier.fillMaxWidth().aspectRatio(1f)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f * 0.92f

        // Radius of a level: the main direction is the outer ring, 30 dB down is the centre.
        fun radius(db: Double): Float = (r * (1.0 - db / 30.0).coerceIn(0.0, 1.0)).toFloat()
        for (db in listOf(0.0, 3.0, 10.0, 20.0)) {
            drawCircle(grid, radius(db), c, style = Stroke(width = 1.5f))
        }
        drawLine(grid, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = 1.5f)
        drawLine(grid, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = 1.5f)
        val path = Path()
        for (deg in 0..360) {
            val rad = radius(loss[deg % 360])
            val a = deg * PI / 180.0
            val p = Offset(c.x + (rad * sin(a)).toFloat(), c.y - (rad * cos(a)).toFloat())
            if (deg == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, line, style = Stroke(width = 4f))
    }
}
