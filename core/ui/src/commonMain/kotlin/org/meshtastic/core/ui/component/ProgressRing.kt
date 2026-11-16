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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions

private const val START_ANGLE = -90f
private const val FULL_CIRCLE = 360f
private const val TRACK_ALPHA = 0.28f
private const val THIN_STROKE_SHARE = 0.05f

/** True when the round progress rings are drawn as thin iPhone-style outlines (the "Look" setting). */
@Composable fun rememberThinRings(): Boolean = rememberUiOptions()[UiOptions.RING_STYLE] == UiOptions.RING_STYLE_THIN

/**
 * A round progress ring filled to [progress] (0 to 1). In the thin look (default, like on the iPhone) it is a grey
 * outline whose filled part takes the theme colour (rounded ends) as it fills, or takes [color] when one is given; with the
 * classic look chosen in "Look" it is the old thick Material ring.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    color: Color = Color.Unspecified,
    classicStrokeWidth: Dp = 4.dp,
) {
    if (!rememberThinRings()) {
        if (color == Color.Unspecified) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = modifier.size(size),
                strokeWidth = classicStrokeWidth,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        } else {
            CircularProgressIndicator(
                progress = { progress },
                modifier = modifier.size(size),
                strokeWidth = classicStrokeWidth,
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        return
    }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA)
    val arc = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    Canvas(modifier = modifier.size(size)) {
        val stroke = this.size.minDimension * THIN_STROKE_SHARE
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(
            color = track,
            startAngle = START_ANGLE,
            sweepAngle = FULL_CIRCLE,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = arc,
            startAngle = START_ANGLE,
            sweepAngle = FULL_CIRCLE * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/**
 * A horizontal progress bar filled to [progress]: in the thin look a slim rounded grey line whose filled part takes
 * the theme colour (or takes [color]); with the classic look the old Material bar.
 */
@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    if (!rememberThinRings()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        return
    }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA)
    val bar = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    Canvas(modifier = modifier.height(LINE_HEIGHT_DP.dp).fillMaxWidth()) {
        val h = size.height
        drawRoundRect(color = track, size = size, cornerRadius = CornerRadius(h / 2))
        val w = size.width * progress.coerceIn(0f, 1f)
        if (w > 0f) drawRoundRect(color = bar, size = Size(maxOf(w, h), h), cornerRadius = CornerRadius(h / 2))
    }
}

private const val LINE_HEIGHT_DP = 4
