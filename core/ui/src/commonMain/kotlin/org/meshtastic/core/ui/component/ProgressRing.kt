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
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
@file:Suppress("TooManyFunctions")

package org.meshtastic.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val START_ANGLE = -90f
private const val FULL_CIRCLE = 360f
private const val HALF_CIRCLE = 180f
private const val DARK_TRACK_ALPHA = 0.55f
private const val LIGHT_TRACK_ALPHA = 0.85f
private const val DARK_THEME_LUMINANCE = 0.5f
private val LIGHT_THEME_START = Color(0xFF9E9E9E)
private val DARK_THEME_START = Color(0xFFBDBDBD)
private const val BAND_SHARE = 0.14f
private const val LINE_SHARE = 0.028f
private const val MIN_LINE_DP = 1f
private const val FULL_PROGRESS = 0.999f
private const val SPINNER_ARC = 100f
private const val SPINNER_MILLIS = 1100
private const val LINE_HEIGHT_DP = 14
private const val LINE_STROKE_DP = 2
private const val LINE_STROKE_DIVISOR = 7f
private const val LOOP_SHARE = 0.3f
private const val LOOP_MILLIS = 1400

/**
 * MT_SW: the empty part (the track) of the thin progress bars, rings and sliders: black on the light theme, light grey
 * on the dark theme, so it is always clearly visible.
 */
@Composable
@ReadOnlyComposable
internal fun thinTrackColor(): Color = if (MaterialTheme.colorScheme.surface.luminance() < DARK_THEME_LUMINANCE) {
    Color.White.copy(alpha = DARK_TRACK_ALPHA)
} else {
    Color.Black.copy(alpha = LIGHT_TRACK_ALPHA)
}

/**
 * MT_SW: the colour of the filled outline of the thin bars, rings and sliders. A chosen [color] stays as it is;
 * otherwise it grows from grey to the colour of the theme (yellow in the default theme) as [progress] goes from 0 to 1.
 */
@Composable
@ReadOnlyComposable
internal fun thinFillColor(progress: Float, color: Color = Color.Unspecified): Color {
    if (color != Color.Unspecified) return color
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_THEME_LUMINANCE
    val start = if (dark) DARK_THEME_START else LIGHT_THEME_START
    return lerp(start, MaterialTheme.colorScheme.primary, progress.coerceIn(0f, 1f))
}

/** True when the progress indicators are drawn as thin iPhone-style outlines (the "Look" setting). */
@Composable fun rememberThinRings(): Boolean = rememberUiOptions()[UiOptions.RING_STYLE] == UiOptions.RING_STYLE_THIN

/** Radii (in pixels) of the two thin lines that bound a round band: the outer and the inner edge. */
private class BandGeometry(val center: Offset, val outer: Float, val inner: Float, val line: Float)

private fun DrawScope.bandGeometry(): BandGeometry {
    val full = size.minDimension
    val line = max(MIN_LINE_DP.dp.toPx(), full * LINE_SHARE)
    val outer = full / 2 - line / 2
    val inner = outer - full * BAND_SHARE
    return BandGeometry(Offset(size.width / 2, size.height / 2), outer, inner, line)
}

private fun DrawScope.drawBandTrack(g: BandGeometry, color: Color) {
    drawCircle(color = color, radius = g.outer, center = g.center, style = Stroke(width = g.line))
    drawCircle(color = color, radius = g.inner, center = g.center, style = Stroke(width = g.line))
}

/** An outlined (empty inside) piece of the round band: two thin arcs joined by rounded ends. */
private fun DrawScope.drawBandPiece(
    g: BandGeometry,
    color: Color,
    startDeg: Float,
    sweepDeg: Float,
    rounded: Boolean = true,
) {
    val mid = (g.outer + g.inner) / 2
    val capRadius = (g.outer - g.inner) / 2
    val endDeg = startDeg + sweepDeg

    fun ring(radius: Float) = Rect(g.center.x - radius, g.center.y - radius, g.center.x + radius, g.center.y + radius)

    fun cap(deg: Float): Rect {
        val rad = deg * PI / HALF_CIRCLE
        val cx = g.center.x + mid * cos(rad).toFloat()
        val cy = g.center.y + mid * sin(rad).toFloat()
        return Rect(cx - capRadius, cy - capRadius, cx + capRadius, cy + capRadius)
    }
    val path =
        Path().apply {
            arcTo(ring(g.outer), startDeg, sweepDeg, true)
            if (rounded) arcTo(cap(endDeg), endDeg, HALF_CIRCLE, false)
            arcTo(ring(g.inner), endDeg, -sweepDeg, false)
            if (rounded) arcTo(cap(startDeg), startDeg + HALF_CIRCLE, HALF_CIRCLE, false)
            close()
        }
    drawPath(path = path, color = color, style = Stroke(width = g.line, join = StrokeJoin.Round))
}

/**
 * A round progress ring filled to [progress] (0 to 1). In the thin look (default, like on the iPhone) it is a wide band
 * drawn only as two thin grey lines (outer and inner edge, empty inside); the filled part is the same kind of outlined
 * band with rounded ends in the theme colour (or [color]) that grows along the grey lines. With the classic look chosen
 * in "Look" it is the old thick Material ring.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    color: Color = Color.Unspecified,
    classicStrokeWidth: Dp = 4.dp,
) {
    if (rememberThinRings()) {
        ThinProgressRing(progress, size, color, modifier)
    } else {
        ClassicProgressRing(progress, size, color, classicStrokeWidth, modifier)
    }
}

@Composable
private fun ClassicProgressRing(
    progress: Float,
    size: Dp,
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    if (color == Color.Unspecified) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = modifier.size(size),
            strokeWidth = strokeWidth,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    } else {
        CircularProgressIndicator(
            progress = { progress },
            modifier = modifier.size(size),
            strokeWidth = strokeWidth,
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun ThinProgressRing(progress: Float, size: Dp, color: Color, modifier: Modifier = Modifier) {
    val track = thinTrackColor()
    val edge = thinFillColor(progress, color)
    Canvas(modifier = modifier.size(size)) {
        val g = bandGeometry()
        drawBandTrack(g, track)
        val fraction = progress.coerceIn(0f, 1f)
        when {
            fraction >= FULL_PROGRESS -> drawBandTrack(g, edge)
            fraction > 0f -> drawBandPiece(g, edge, START_ANGLE, FULL_CIRCLE * fraction)
        }
    }
}

/**
 * A round chart made of several coloured parts, in the thin outlined look of [ProgressRing] (use it only when
 * [rememberThinRings] is true). Each part is an outlined piece of the band in its own colour, so multi-colour values
 * keep their colours; [segments] are (colour, share of the whole circle from 0 to 1) pairs drawn clockwise from the
 * top.
 */
@Composable
fun SegmentedProgressRing(segments: List<Pair<Color, Float>>, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val track = thinTrackColor()
    Canvas(modifier = modifier.size(size)) {
        val g = bandGeometry()
        drawBandTrack(g, track)
        var start = START_ANGLE
        segments.forEach { (color, share) ->
            val sweep = FULL_CIRCLE * share.coerceIn(0f, 1f)
            when {
                sweep >= FULL_CIRCLE * FULL_PROGRESS -> drawBandTrack(g, color)
                sweep > 0f -> drawBandPiece(g, color, start, sweep, rounded = false)
            }
            start += sweep
        }
    }
}

/**
 * A round indeterminate indicator (waiting, connecting). In the thin look it is the same two thin grey lines as
 * [ProgressRing] with an outlined piece of the band (theme colour or [color]) that spins along them. With the classic
 * look it is the old wavy Material circle.
 */
@Composable
fun ProgressSpinner(modifier: Modifier = Modifier, size: Dp = 48.dp, color: Color = Color.Unspecified) {
    if (rememberThinRings()) {
        ThinProgressSpinner(size, color, modifier)
    } else if (color == Color.Unspecified) {
        CircularWavyProgressIndicator(modifier = modifier.size(size))
    } else {
        CircularWavyProgressIndicator(modifier = modifier.size(size), color = color)
    }
}

@Composable
private fun ThinProgressSpinner(size: Dp, color: Color, modifier: Modifier = Modifier) {
    val track = thinTrackColor()
    val edge = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    val turn by
        rememberInfiniteTransition(label = "spinner")
            .animateFloat(
                initialValue = 0f,
                targetValue = FULL_CIRCLE,
                animationSpec = infiniteRepeatable(tween(SPINNER_MILLIS, easing = LinearEasing)),
                label = "spinnerTurn",
            )
    Canvas(modifier = modifier.size(size)) {
        val g = bandGeometry()
        drawBandTrack(g, track)
        drawBandPiece(g, edge, START_ANGLE + turn, SPINNER_ARC)
    }
}

private fun DrawScope.drawLineOutline(color: Color, left: Float, width: Float, stroke: Float) {
    val h = size.height - stroke
    drawRoundRect(
        color = color,
        topLeft = Offset(left + stroke / 2, stroke / 2),
        size = Size(width - stroke, h),
        cornerRadius = CornerRadius(h / 2),
        style = Stroke(width = stroke),
    )
}

/**
 * A horizontal progress bar filled to [progress]. In the thin look it is an empty rounded outline (not a filled bar):
 * two grey edges closed by round ends. As the progress grows, a coloured outline (the theme colour, or [color]) starts
 * at the left end as a small rounded loop and its front, rounded too, moves along both grey edges until it reaches the
 * other end. With the classic look it is the old Material bar.
 */
@Composable
fun ProgressLine(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    height: Dp = LINE_HEIGHT_DP.dp,
) {
    if (!rememberThinRings()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        return
    }
    val track = thinTrackColor()
    val edge = thinFillColor(progress, color)
    Canvas(modifier = modifier.height(height).fillMaxWidth()) {
        val stroke = (height.toPx() / LINE_STROKE_DIVISOR).coerceIn(MIN_LINE_DP.dp.toPx(), LINE_STROKE_DP.dp.toPx())
        drawLineOutline(track, 0f, size.width, stroke)
        val fraction = progress.coerceIn(0f, 1f)
        if (fraction > 0f) {
            drawLineOutline(edge, 0f, max(size.width * fraction, size.height), stroke)
        }
    }
}

/**
 * A horizontal indeterminate bar (waiting, connecting). In the thin look it is an empty rounded outline of two thin
 * grey edges with a shorter outlined capsule (theme colour or [color]) that slides back and forth along them. With the
 * classic look it is the old wavy Material bar.
 */
@Composable
fun IndeterminateProgressLine(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    if (!rememberThinRings()) {
        LinearWavyProgressIndicator(modifier = modifier)
        return
    }
    val track = thinTrackColor()
    val edge = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    val slide by
        rememberInfiniteTransition(label = "line")
            .animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                infiniteRepeatable(tween(LOOP_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "lineSlide",
            )
    Canvas(modifier = modifier.height(LINE_HEIGHT_DP.dp).fillMaxWidth()) {
        val stroke = LINE_STROKE_DP.dp.toPx()
        drawLineOutline(track, 0f, size.width, stroke)
        val loop = max(size.width * LOOP_SHARE, size.height)
        drawLineOutline(edge, (size.width - loop) * slide, loop, stroke)
    }
}

/**
 * A wide bar split into coloured parts ([segments] are (colour, weight) pairs), in the thin outlined look: an empty
 * rounded outline of two thin grey edges where each part is the same outline in its own colour, so multi-colour values
 * keep their colours. The parts fill [fillFraction] (0 to 1) of the bar, left to right.
 */
@Composable
fun SegmentedProgressLine(
    segments: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
    fillFraction: Float = 1f,
    height: Dp = LINE_HEIGHT_DP.dp,
) {
    val track = thinTrackColor()
    Canvas(modifier = modifier.height(height).fillMaxWidth()) {
        val stroke = (height.toPx() / LINE_STROKE_DIVISOR).coerceIn(MIN_LINE_DP.dp.toPx(), LINE_STROKE_DP.dp.toPx())
        drawLineOutline(track, 0f, size.width, stroke)
        val total = segments.sumOf { it.second.toDouble() }.toFloat()
        if (total <= 0f) return@Canvas
        var x = 0f
        segments.forEach { (color, weight) ->
            val part = size.width * weight / total * fillFraction.coerceIn(0f, 1f)
            if (part > 0f) {
                clipRect(left = x, top = 0f, right = x + part, bottom = size.height) {
                    drawLineOutline(color, 0f, size.width, stroke)
                }
                if (x > 0f) drawLine(track, Offset(x, stroke), Offset(x, size.height - stroke), strokeWidth = stroke)
            }
            x += part
        }
    }
}
