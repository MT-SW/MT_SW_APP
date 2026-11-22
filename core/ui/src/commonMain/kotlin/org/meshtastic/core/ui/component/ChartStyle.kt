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
@file:Suppress("TooManyFunctions")

package org.meshtastic.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions
import kotlin.math.min

private const val LIGHTEN = 0.35f
private const val DARKEN = 0.30f
private const val GLOSS_TOP_ALPHA = 0.45f
private const val GLOSS_BOTTOM_ALPHA = 0.03f
private const val GLOSS_HEIGHT_SHARE = 0.45f
private const val GLOSS_MIN_HEIGHT_DP = 6
private const val RIM_TOP_ALPHA = 0.75f
private const val RIM_BOTTOM_ALPHA = 0.12f
private const val SOFT_FILL_ALPHA = 0.16f
private const val HALO_NEAR_ALPHA = 0.20f
private const val HALO_FAR_ALPHA = 0.10f
private const val HALF = 0.5f
private const val MAX_BAND_START = 0.98f
private const val GLOSS_INSET_SHARE = 0.12f
private const val OUTLINE_WIDTH_DP = 1.5f
private const val HALO_FAR_DP = 4
private const val HALO_NEAR_DP = 2
private const val TUBE_SHINE_OFFSET = 0.22f
private const val TUBE_SHINE_WIDTH = 0.16f
private const val TUBE_LIGHT_STOP = 0.78f
private const val HALO_FAR_SCALE = 1.6f
private const val HALO_NEAR_SCALE = 1.25f

/**
 * The chart style chosen in "Look" (one of the `UiOptions.CHART_STYLE_*` values). [scope] is the own style of a part of
 * the app ([UiOptions.CHART_STYLE_HEALTH] or [UiOptions.CHART_STYLE_NODE]); when it says "same" the general style is
 * used.
 */
@Composable
fun rememberChartStyle(scope: String = UiOptions.CHART_STYLE): Int {
    val options = rememberUiOptions()
    val own = options[scope]
    return if (own >= 0) own else options[UiOptions.CHART_STYLE]
}

private fun lighter(c: Color) = lerp(c, Color.White, LIGHTEN)

private fun darker(c: Color) = lerp(c, Color.Black, DARKEN)

private fun fade(c: Color, alpha: Float) = c.copy(alpha = alpha)

/**
 * Draws one bar or heat-map cell in the chosen chart [style]: flat colour (the old look), glossy 3D glass (gradient,
 * highlight and bright rim), a soft gradient, a thin outline, or a gradient with a glow. The colour's own transparency
 * is kept.
 */
fun DrawScope.drawChartBar(style: Int, color: Color, topLeft: Offset, size: Size, corner: Float) {
    when (style) {
        UiOptions.CHART_STYLE_GLASS,
        UiOptions.CHART_STYLE_RIBBON,
        -> glassBar(color, topLeft, size, corner)

        UiOptions.CHART_STYLE_GRADIENT -> gradientBar(color, topLeft, size, corner)

        UiOptions.CHART_STYLE_OUTLINE -> outlineBar(color, topLeft, size, corner)

        UiOptions.CHART_STYLE_GLOW -> glowBar(color, topLeft, size, corner)

        else ->
            drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = CornerRadius(corner, corner))
    }
}

private fun DrawScope.glassBar(color: Color, topLeft: Offset, size: Size, corner: Float) {
    val radius = CornerRadius(corner, corner)
    val alpha = color.alpha
    val base = color.copy(alpha = 1f)
    val top = topLeft.y
    val bottom = topLeft.y + size.height
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(lighter(base), base, darker(base)), startY = top, endY = bottom),
        topLeft = topLeft,
        size = size,
        cornerRadius = radius,
        alpha = alpha,
    )
    if (size.height > GLOSS_MIN_HEIGHT_DP.dp.toPx() && size.width > 2f) {
        val inset = min(size.width * GLOSS_INSET_SHARE, 3.dp.toPx())
        drawRoundRect(
            brush =
            Brush.verticalGradient(
                listOf(Color.White.copy(GLOSS_TOP_ALPHA), Color.White.copy(GLOSS_BOTTOM_ALPHA)),
                startY = top,
                endY = top + size.height * GLOSS_HEIGHT_SHARE,
            ),
            topLeft = Offset(topLeft.x + inset, top + inset),
            size = Size(size.width - 2 * inset, size.height * GLOSS_HEIGHT_SHARE),
            cornerRadius = radius,
            alpha = alpha,
        )
    }
    drawRoundRect(
        brush =
        Brush.verticalGradient(
            listOf(Color.White.copy(RIM_TOP_ALPHA), Color.White.copy(RIM_BOTTOM_ALPHA)),
            startY = top,
            endY = bottom,
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = radius,
        alpha = alpha,
        style = Stroke(width = 1.dp.toPx()),
    )
}

private fun DrawScope.gradientBar(color: Color, topLeft: Offset, size: Size, corner: Float) {
    val radius = CornerRadius(corner, corner)
    val alpha = color.alpha
    val base = color.copy(alpha = 1f)
    val top = topLeft.y
    val bottom = topLeft.y + size.height
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(base, fade(base, HALF)), startY = top, endY = bottom),
        topLeft = topLeft,
        size = size,
        cornerRadius = radius,
        alpha = alpha,
    )
}

private fun DrawScope.outlineBar(color: Color, topLeft: Offset, size: Size, corner: Float) {
    val radius = CornerRadius(corner, corner)
    val alpha = color.alpha
    val base = color.copy(alpha = 1f)
    drawRoundRect(
        color = fade(base, SOFT_FILL_ALPHA * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = radius,
    )
    drawRoundRect(
        color = fade(base, alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = radius,
        style = Stroke(width = OUTLINE_WIDTH_DP.dp.toPx()),
    )
}

private fun DrawScope.glowBar(color: Color, topLeft: Offset, size: Size, corner: Float) {
    val radius = CornerRadius(corner, corner)
    val alpha = color.alpha
    val base = color.copy(alpha = 1f)
    val top = topLeft.y
    val bottom = topLeft.y + size.height
    val far = HALO_FAR_DP.dp.toPx()
    val near = HALO_NEAR_DP.dp.toPx()
    drawRoundRect(
        color = fade(base, HALO_FAR_ALPHA * alpha),
        topLeft = Offset(topLeft.x - far, top - far),
        size = Size(size.width + 2 * far, size.height + 2 * far),
        cornerRadius = CornerRadius(corner + far, corner + far),
    )
    drawRoundRect(
        color = fade(base, HALO_NEAR_ALPHA * alpha),
        topLeft = Offset(topLeft.x - near, top - near),
        size = Size(size.width + 2 * near, size.height + 2 * near),
        cornerRadius = CornerRadius(corner + near, corner + near),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(lighter(base), base), startY = top, endY = bottom),
        topLeft = topLeft,
        size = size,
        cornerRadius = radius,
        alpha = alpha,
    )
}

@Suppress("LongParameterList")
private fun DrawScope.ringArc(
    brush: Brush? = null,
    color: Color = Color.Unspecified,
    startAngle: Float,
    sweep: Float,
    center: Offset,
    radius: Float,
    width: Float,
    alpha: Float = 1f,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2, radius * 2)
    if (brush != null) {
        drawArc(brush, startAngle, sweep, false, topLeft, arcSize, alpha, Stroke(width = width))
    } else {
        drawArc(color, startAngle, sweep, false, topLeft, arcSize, alpha, Stroke(width = width))
    }
}

/** The numbers of one piece of a round gauge band, shared by the drawing of every chart style. */
private class ArcBand(
    val alpha: Float,
    val startAngle: Float,
    val sweep: Float,
    val center: Offset,
    val radius: Float,
    val stroke: Float,
) {
    val outer = radius + stroke / 2
    val inner = radius - stroke / 2

    /** A radial gradient over the band: each stop is (share of the band thickness, colour). */
    @Suppress("SpreadOperator")
    fun brush(vararg stops: Pair<Float, Color>): Brush {
        val start = (inner / outer).coerceIn(0f, MAX_BAND_START)
        val first = stops.first().second
        val all = listOf(0f to first, start to first) + stops.map { (start + (1f - start) * it.first) to it.second }
        return Brush.radialGradient(*all.toTypedArray(), center = center, radius = outer)
    }
}

private fun DrawScope.piece(
    band: ArcBand,
    radius: Float = band.radius,
    width: Float = band.stroke,
    color: Color = Color.Unspecified,
    brush: Brush? = null,
) = ringArc(
    brush = brush,
    color = color,
    startAngle = band.startAngle,
    sweep = band.sweep,
    center = band.center,
    radius = radius,
    width = width,
    alpha = band.alpha,
)

/**
 * Draws a piece of a round gauge band (centre line at [radius], thickness [stroke]) in the chosen chart [style]: flat
 * colour (the old look), a glossy 3D tube, a soft gradient, a thin outline or a gradient with a glow.
 */
@Suppress("LongParameterList")
fun DrawScope.drawChartArc(
    style: Int,
    color: Color,
    startAngle: Float,
    sweep: Float,
    center: Offset,
    radius: Float,
    stroke: Float,
) {
    val band = ArcBand(color.alpha, startAngle, sweep, center, radius, stroke)
    val base = color.copy(alpha = 1f)
    when (style) {
        UiOptions.CHART_STYLE_GLASS,
        UiOptions.CHART_STYLE_RIBBON,
        -> glassArc(band, base)

        UiOptions.CHART_STYLE_GRADIENT -> piece(band, brush = band.brush(0f to fade(base, HALF), 1f to base))

        UiOptions.CHART_STYLE_OUTLINE -> outlineArc(band, base)

        UiOptions.CHART_STYLE_GLOW -> glowArc(band, base)

        else -> piece(band, color = base)
    }
}

private fun DrawScope.glassArc(band: ArcBand, base: Color) {
    val tube = band.brush(0f to darker(base), HALF to base, TUBE_LIGHT_STOP to lighter(base), 1f to base)
    piece(band, brush = tube)
    piece(
        band,
        radius = band.radius + band.stroke * TUBE_SHINE_OFFSET,
        width = band.stroke * TUBE_SHINE_WIDTH,
        color = Color.White.copy(GLOSS_TOP_ALPHA),
    )
    piece(band, radius = band.outer, width = 1.dp.toPx(), color = Color.White.copy(RIM_TOP_ALPHA * HALF))
}

private fun DrawScope.outlineArc(band: ArcBand, base: Color) {
    piece(band, color = fade(base, SOFT_FILL_ALPHA))
    val line = OUTLINE_WIDTH_DP.dp.toPx()
    piece(band, radius = band.outer - line / 2, width = line, color = base)
    piece(band, radius = band.inner + line / 2, width = line, color = base)
}

private fun DrawScope.glowArc(band: ArcBand, base: Color) {
    piece(band, width = band.stroke * HALO_FAR_SCALE, color = fade(base, HALO_FAR_ALPHA))
    piece(band, width = band.stroke * HALO_NEAR_SCALE, color = fade(base, HALO_NEAR_ALPHA))
    piece(band, brush = band.brush(0f to base, 1f to lighter(base)))
}
