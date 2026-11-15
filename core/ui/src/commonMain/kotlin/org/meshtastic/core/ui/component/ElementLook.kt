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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.ElementStyle
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions

private const val PERCENT = 100f
private const val MIN_ALPHA = 0.12f
private const val GLASS_TINT_SHARE = 0.85f
private const val DEPTH_SHADOW_BASE_DP = 2f
private const val DEPTH_SHADOW_RANGE_DP = 10f
private const val LIGHT_TOP_ALPHA = 0.18f
private const val SHADE_BOTTOM_ALPHA = 0.08f
private const val RIM_TOP_ALPHA = 0.5f
private const val RIM_BOTTOM_ALPHA = 0.06f
private const val FLAT_SHADOW_DP = 8f
private const val RIM_ALPHA_ON_GLASS = 0.35f
private const val GLASS_TINT_LIGHT = 1
private const val GLASS_TINT_DARK = 2
private const val GLASS_TINT_ACCENT = 3
private const val GLASS_TINT_MIX = 0.6f

/** The share of the element's own colour that is left when [strength] percent of it is made see-through. */
private fun seeThroughAlpha(strength: Int): Float = (1f - strength / PERCENT).coerceIn(MIN_ALPHA, 1f)

/**
 * Draws this element with [ElementLook.DEPTH] when the person chose it for the group named [group] (one of the
 * `UiOptions.GROUP_` names): a soft shadow, light along the upper edge, a little shade below and a thin bright rim.
 * Every other look leaves the element exactly as it is.
 */
@Composable
fun Modifier.elementLook(group: String, shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val options = rememberUiOptions()
    val style = options.element(group)
    return if (style.look == ElementLook.DEPTH) depthLook(style.strength, shape, options.rimDp()) else this
}

/** The thickness of the rim chosen in the settings, in dp. */
internal fun UiOptions.rimDp(): Float =
    UiOptions.RIM_STEPS_DP[get(UiOptions.RIM).coerceIn(0, UiOptions.RIM_STEPS_DP.lastIndex)]

/** The colour of the glass: the given [base] colour changed by the "colour of the glass" option. */
@Composable
internal fun UiOptions.glassBase(base: Color): Color = when (get(UiOptions.GLASS_TINT)) {
    GLASS_TINT_LIGHT -> lerp(base, Color.White, GLASS_TINT_MIX)
    GLASS_TINT_DARK -> lerp(base, Color.Black, GLASS_TINT_MIX)
    GLASS_TINT_ACCENT -> lerp(base, MaterialTheme.colorScheme.primary, GLASS_TINT_MIX)
    else -> base
}

/** The depth look of [elementLook], with its strength 0-100. */
fun Modifier.depthLook(strength: Int, shape: Shape, rimDp: Float = 1f): Modifier {
    val s = strength.coerceIn(0, PERCENT.toInt()) / PERCENT
    return this.shadow(elevation = (DEPTH_SHADOW_BASE_DP + DEPTH_SHADOW_RANGE_DP * s).dp, shape = shape, clip = false)
        .drawWithContent {
            drawContent()
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                brush =
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = LIGHT_TOP_ALPHA * s),
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = SHADE_BOTTOM_ALPHA * s),
                ),
            )
            drawOutline(
                outline = outline,
                brush =
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = RIM_TOP_ALPHA * s), Color.White.copy(alpha = RIM_BOTTOM_ALPHA * s)),
                ),
                style = Stroke(width = rimDp.dp.toPx()),
            )
        }
}

/**
 * A surface drawn in the look the person chose for [group]: plain, with depth, see-through, or (for a surface that
 * floats above the content, with a [hazeState] to blur) frosted glass.
 *
 * @param floating adds the shadow a floating surface has in the plain look.
 */
@Composable
fun StyledSurface(
    group: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    floating: Boolean = false,
    content: @Composable () -> Unit,
) {
    val options = rememberUiOptions()
    StyledSurface(options.element(group), shape, modifier, hazeState, options.glassBase(baseColor), floating, content)
}

/** The same as above for an already known [style]. */
@Composable
fun StyledSurface(
    style: ElementStyle,
    shape: Shape,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    floating: Boolean = false,
    content: @Composable () -> Unit,
) {
    val rimWidth = rememberUiOptions().rimDp()
    val rim = BorderStroke(rimWidth.dp, Color.White.copy(alpha = RIM_ALPHA_ON_GLASS))
    when {
        style.look == ElementLook.GLASS && hazeState != null -> {
            val tint = baseColor.copy(alpha = seeThroughAlpha(style.strength) * GLASS_TINT_SHARE)
            val blur = HazeBlurStyle {
                blurRadius(style.blur.coerceIn(1, MAX_BLUR_DP).dp)
                colorEffects(listOf(HazeColorEffect.tint(tint)))
            }
            Box(
                modifier =
                modifier
                    .shadow(if (floating) FLAT_SHADOW_DP.dp else 0.dp, shape, clip = false)
                    .clip(shape)
                    .hazeBlur(input = HazeInput.Sources(hazeState), style = blur)
                    .border(rim, shape),
            ) {
                content()
            }
        }

        style.look == ElementLook.GLASS || style.look == ElementLook.TRANSPARENT ->
            Surface(
                modifier = modifier,
                shape = shape,
                color = baseColor.copy(alpha = seeThroughAlpha(style.strength)),
                border = rim,
                shadowElevation = if (floating) FLAT_SHADOW_DP.dp else 0.dp,
                content = content,
            )

        style.look == ElementLook.DEPTH ->
            Box(modifier = modifier.depthLook(style.strength, shape).clip(shape).background(baseColor)) { content() }

        else ->
            Surface(
                modifier = modifier,
                shape = shape,
                color = baseColor,
                shadowElevation = if (floating) FLAT_SHADOW_DP.dp else 0.dp,
                content = content,
            )
    }
}

/** The biggest blur the settings offer, in dp. */
const val MAX_BLUR_DP = 60
