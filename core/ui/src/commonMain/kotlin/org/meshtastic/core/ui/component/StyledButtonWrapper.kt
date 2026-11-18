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
@file:Suppress("MagicNumber", "ReturnCount", "TooManyFunctions")

package org.meshtastic.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.FilledTonalButton as MaterialFilledTonalButton
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton

/** The least share of a button's colour that stays when the look makes it see-through, so its letters stay readable. */
private const val BUTTON_MIN_ALPHA = 0.8f

/**
 * MT_SW: the buttons of the app. They follow the look chosen for the group of the current screen (see [LocalCardGroup])
 * the same way the cards do: plain as before, with depth, see-through or glass.
 */
@Composable
private fun buttonLook(group: String, base: Color, fallback: Color = base): Color {
    val look = rememberUiOptions().element(group).look
    if (look == ElementLook.FLAT || look == ElementLook.DEPTH) return fallback
    val seeThrough = cardContainerColor(base, group)
    return seeThrough.copy(alpha = maxOf(seeThrough.alpha, BUTTON_MIN_ALPHA))
}

/**
 * MT_SW: gives any control (a row of segmented buttons, a chip row...) the look chosen for the group of the current
 * screen: depth, see-through or glass with a thin rim; with the plain look it changes nothing.
 */
@Composable fun Modifier.styledControl(shape: Shape): Modifier = buttonStyle(LocalCardGroup.current, shape)

@Composable
private fun Modifier.buttonStyle(group: String, shape: Shape): Modifier {
    val options = rememberUiOptions()
    val style = options.element(group)
    if (style.look == ElementLook.FLAT) return this
    val gradient =
        UiOptions.BUTTON_GRADIENT_SHARE[
            options[UiOptions.BUTTON_GRADIENT].coerceIn(0, UiOptions.BUTTON_GRADIENT_SHARE.lastIndex),
        ]
    if (gradient <= 0f) return this
    // Buttons get no shadow plate and no double glass rim (they looked like a shifted layer): only a gentle gradient
    // (light above, a little shade below) with one thin rim, a soft 3D feeling in every look.
    return softGradient(shape, style.strength, gradient)
}

private const val GRADIENT_TOP_ALPHA = 0.16f
private const val GRADIENT_BOTTOM_ALPHA = 0.18f
private const val GRADIENT_RIM_TOP_ALPHA = 0.28f
private const val GRADIENT_RIM_BOTTOM_ALPHA = 0.05f
private const val GRADIENT_MIN_SHARE = 0.4f
private const val GRADIENT_PERCENT = 100f
private const val GRADIENT_MIDDLE = 0.55f

/** A delicate vertical gradient with a thin rim, drawn over a button or a control. */
private fun Modifier.softGradient(shape: Shape, strength: Int, gradient: Float): Modifier {
    val share =
        (GRADIENT_MIN_SHARE + (1f - GRADIENT_MIN_SHARE) * strength.coerceIn(0, 100) / GRADIENT_PERCENT) * gradient
    return drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(
            outline = outline,
            brush =
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = GRADIENT_TOP_ALPHA * share),
                GRADIENT_MIDDLE to Color.Transparent,
                1f to Color.Black.copy(alpha = GRADIENT_BOTTOM_ALPHA * share),
            ),
        )
        drawOutline(
            outline = outline,
            brush =
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = GRADIENT_RIM_TOP_ALPHA * share),
                1f to Color.White.copy(alpha = GRADIENT_RIM_BOTTOM_ALPHA * share),
            ),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

private const val GLASS_LIGHT_SHARE = 0.28f
private const val GLASS_DARK_SHARE = 0.22f
private const val GLASS_GLOSS_ALPHA = 0.30f
private const val GLASS_GLOSS_HEIGHT = 0.5f
private const val GLASS_RIM_TOP_ALPHA = 0.55f
private const val GLASS_RIM_BOTTOM_ALPHA = 0.12f

/** How a filled button is drawn: as glass, as a thin outline on clear glass, or (both null) the plain way. */
internal data class FilledDecor(
    val glass: Color? = null,
    val outline: OutlineGlassLook? = null,
    val outlineWidthDp: Float = 1.5f,
    val glow: Float = 1f,
) {
    /** True when the button draws its own surface, so the Material container is cleared. */
    val custom: Boolean
        get() = glass != null || outline != null
}

/** The colours of an outline-glass button: the accent of a working one, or the grey of a switched-off one. */
internal data class OutlineGlassLook(val accent: Color, val active: Boolean, val text: Color, val disabledText: Color)

private const val OUTLINE_TEXT_MIX = 0.25f
private const val OUTLINE_DISABLED_ALPHA = 0.38f

/** True for the red colours of the theme: only a button that disconnects or deletes may keep them. */
private fun isDangerColor(color: Color, scheme: ColorScheme): Boolean = color == scheme.error ||
    color == scheme.errorContainer ||
    color == scheme.onErrorContainer ||
    color == scheme.onError

/**
 * MT_SW: the look of a filled button. With the default "outline and glass" style it is a thin outline on a clear glass
 * pane, with a soft glow in the colour of the theme while the button works and grey when it is switched off. "Look" can
 * choose filled glass or the plain Material buttons instead.
 */
@Composable
private fun rememberFilledDecor(colors: ButtonColors, enabled: Boolean): FilledDecor =
    rememberDecorFor(colors.containerColor, colors.disabledContainerColor, enabled)

/** The same for any filled control (a floating button, a filled icon button) from its [container] colours. */
@Composable
internal fun rememberDecorFor(container: Color, disabledContainer: Color, enabled: Boolean): FilledDecor {
    val options = rememberUiOptions()
    val scheme = MaterialTheme.colorScheme
    return when (options[UiOptions.BUTTON_STYLE]) {
        UiOptions.BUTTON_STYLE_GLASS -> {
            val base = if (enabled) container else disabledContainer
            FilledDecor(glass = base.takeIf { it.isSpecified && it.alpha > 0f })
        }

        UiOptions.BUTTON_STYLE_OUTLINE_GLASS -> {
            // A working button glows in the colour of the theme; only a dangerous action (disconnecting) is red.
            val accent =
                when {
                    !enabled -> scheme.onSurface
                    container.isSpecified && container.alpha > 0f && isDangerColor(container, scheme) -> container
                    else -> scheme.primary
                }
            val steps = UiOptions.BUTTON_GLOW_SHARE
            val widths = UiOptions.BUTTON_OUTLINE_DP
            FilledDecor(
                outline =
                OutlineGlassLook(
                    accent = accent,
                    active = enabled,
                    text = lerp(accent, scheme.onSurface, OUTLINE_TEXT_MIX),
                    disabledText = scheme.onSurface.copy(alpha = OUTLINE_DISABLED_ALPHA),
                ),
                outlineWidthDp = widths[options[UiOptions.BUTTON_OUTLINE].coerceIn(0, widths.lastIndex)],
                glow = steps[options[UiOptions.BUTTON_GLOW].coerceIn(0, steps.lastIndex)],
            )
        }

        else -> FilledDecor()
    }
}

/**
 * The outline-glass look for an outlined button: its outline takes the colour of the button's own text (the theme
 * colour, or red for a dangerous action) and glows while it works. In the other styles it stays as it was.
 */
@Composable
private fun rememberOutlinedDecor(colors: ButtonColors, enabled: Boolean): FilledDecor {
    val options = rememberUiOptions()
    if (options[UiOptions.BUTTON_STYLE] != UiOptions.BUTTON_STYLE_OUTLINE_GLASS) return FilledDecor()
    val scheme = MaterialTheme.colorScheme
    val content = colors.contentColor
    val danger = content.isSpecified && content.alpha > 0f && isDangerColor(content, scheme)
    val accent = if (danger) content else scheme.primary
    return rememberDecorFor(container = accent, disabledContainer = scheme.onSurface, enabled = enabled)
}

/** An outlined button that draws its own outline and glass: clear container, text in the colour of the outline. */
private fun ButtonColors.outlinedColors(decor: FilledDecor): ButtonColors = copy(
    containerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    contentColor = decor.outline?.text ?: contentColor,
    disabledContentColor = decor.outline?.disabledText ?: disabledContentColor,
)

/** The colours of a filled button: clear when the button draws its own surface, otherwise the styled colour. */
private fun ButtonColors.filledColors(styled: Color, decor: FilledDecor): ButtonColors = when {
    decor.outline != null ->
        copy(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = decor.outline.text,
            disabledContentColor = decor.outline.disabledText,
        )

    decor.glass != null -> copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent)

    else -> copy(containerColor = styled)
}

@Composable
private fun Modifier.filledLook(decor: FilledDecor, group: String, shape: Shape): Modifier = when {
    decor.outline != null -> outlineGlassSurface(shape, decor)
    decor.glass != null -> glassSurface(shape, decor.glass)
    else -> buttonStyle(group, shape)
}

/**
 * MT_SW: a small indicator (a dot of a page indicator) in the look of the chosen button style: a thin outline on clear
 * glass (the shown one glows in the theme colour, the others are grey), filled glass, or plain circles.
 */
@Composable
fun Modifier.styledIndicator(shape: Shape, active: Boolean): Modifier {
    val scheme = MaterialTheme.colorScheme
    val decor = rememberDecorFor(scheme.primary, scheme.outlineVariant, active)
    return when {
        decor.outline != null -> outlineGlassSurface(shape, decor)
        decor.glass != null -> glassSurface(shape, decor.glass)
        else -> background(if (active) scheme.primary else scheme.outlineVariant, shape)
    }
}

private fun outlinePath(outline: Outline): Path = when (outline) {
    is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
    is Outline.Generic -> outline.path
}

/**
 * MT_SW: a glass button, like on the iPhone: the colour goes from lighter at the top to darker at the bottom, a soft
 * gloss lies on the upper half and a thin light rim runs around it. It is drawn behind the button's content.
 */
internal fun Modifier.glassSurface(shape: Shape, base: Color): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(
        outline = outline,
        brush =
        Brush.verticalGradient(
            0f to lerp(base, Color.White, GLASS_LIGHT_SHARE),
            1f to lerp(base, Color.Black, GLASS_DARK_SHARE),
        ),
    )
    clipPath(outlinePath(outline)) {
        drawRect(
            brush =
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = GLASS_GLOSS_ALPHA),
                1f to Color.Transparent,
                endY = size.height * GLASS_GLOSS_HEIGHT,
            ),
            size = Size(size.width, size.height * GLASS_GLOSS_HEIGHT),
        )
    }
    drawOutline(
        outline = outline,
        brush =
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = GLASS_RIM_TOP_ALPHA),
            1f to Color.White.copy(alpha = GLASS_RIM_BOTTOM_ALPHA),
        ),
        style = Stroke(width = 1.dp.toPx()),
    )
}

private const val OUTLINE_FILL_TOP_ALPHA = 0.26f
private const val OUTLINE_FILL_BOTTOM_ALPHA = 0.08f
private const val OUTLINE_OFF_FILL_ALPHA = 0.05f
private const val OUTLINE_OFF_EDGE_ALPHA = 0.28f
private const val OUTLINE_GLOSS_ALPHA = 0.16f
private const val OUTLINE_EDGE_BOTTOM_ALPHA = 0.65f
private const val OUTLINE_GLOW_LAYERS = 5
private const val OUTLINE_GLOW_STEP_DP = 2f
private const val OUTLINE_GLOW_LAYER_ALPHA = 0.055f

/**
 * MT_SW: a thin outline on a clear glass pane. A working button has a soft glow in its colour (a few wide, faint
 * strokes around the edge), a gloss on the upper half and a bright outline; a switched-off one is grey, without a glow.
 */
internal fun Modifier.outlineGlassSurface(shape: Shape, decor: FilledDecor): Modifier = drawBehind {
    val look = decor.outline ?: return@drawBehind
    val accent = look.accent
    val outline = shape.createOutline(size, layoutDirection, this)
    val edge = decor.outlineWidthDp.dp.toPx()
    if (look.active && decor.glow > 0f) {
        for (layer in OUTLINE_GLOW_LAYERS downTo 1) {
            drawOutline(
                outline = outline,
                color = accent.copy(alpha = (OUTLINE_GLOW_LAYER_ALPHA * decor.glow).coerceAtMost(1f)),
                style = Stroke(width = edge + layer * OUTLINE_GLOW_STEP_DP.dp.toPx()),
            )
        }
    }
    drawOutline(
        outline = outline,
        brush =
        Brush.verticalGradient(
            0f to accent.copy(alpha = if (look.active) OUTLINE_FILL_TOP_ALPHA else OUTLINE_OFF_FILL_ALPHA),
            1f to accent.copy(alpha = if (look.active) OUTLINE_FILL_BOTTOM_ALPHA else OUTLINE_OFF_FILL_ALPHA),
        ),
    )
    if (look.active) {
        clipPath(outlinePath(outline)) {
            drawRect(
                brush =
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = OUTLINE_GLOSS_ALPHA),
                    1f to Color.Transparent,
                    endY = size.height * GLASS_GLOSS_HEIGHT,
                ),
                size = Size(size.width, size.height * GLASS_GLOSS_HEIGHT),
            )
        }
    }
    drawOutline(
        outline = outline,
        brush =
        if (look.active) {
            Brush.verticalGradient(0f to accent, 1f to accent.copy(alpha = OUTLINE_EDGE_BOTTOM_ALPHA))
        } else {
            SolidColor(accent.copy(alpha = OUTLINE_OFF_EDGE_ALPHA))
        },
        style = Stroke(width = edge),
    )
}

/**
 * MT_SW: Material gives every button an invisible 48dp touch box and draws the modifiers on it, so the gradient and the
 * rim looked like a bigger plate behind the button. In the glass and depth looks the box is switched off, so the
 * decoration is drawn exactly on the visible button.
 */
@Composable
private fun WithoutTouchBox(off: Boolean, content: @Composable () -> Unit) {
    if (off) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified, content = content)
    } else {
        content()
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val group = LocalCardGroup.current
    val flat = rememberUiOptions().element(group).look == ElementLook.FLAT
    val decor = rememberFilledDecor(colors, enabled)
    val styled = buttonLook(group, colors.containerColor)
    WithoutTouchBox(!flat || decor.custom) {
        MaterialButton(
            onClick = onClick,
            modifier = modifier.filledLook(decor, group, shape),
            enabled = enabled,
            shape = shape,
            colors = remember(colors, styled, decor) { colors.filledColors(styled, decor) },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat && !decor.custom) elevation else null,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val group = LocalCardGroup.current
    val flat = rememberUiOptions().element(group).look == ElementLook.FLAT
    val decor = rememberFilledDecor(colors, enabled)
    val styled = buttonLook(group, colors.containerColor)
    WithoutTouchBox(!flat || decor.custom) {
        MaterialFilledTonalButton(
            onClick = onClick,
            modifier = modifier.filledLook(decor, group, shape),
            enabled = enabled,
            shape = shape,
            colors = remember(colors, styled, decor) { colors.filledColors(styled, decor) },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat && !decor.custom) elevation else null,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val group = LocalCardGroup.current
    val flat = rememberUiOptions().element(group).look == ElementLook.FLAT
    val look = rememberUiOptions().element(group).look
    val decor = rememberOutlinedDecor(colors, enabled)
    // The plain outlined button has no fill; with a look it gets a pane of the surface colour, so the shadow, the
    // glass and the rim have something to be drawn on.
    val fill = buttonLook(group, MaterialTheme.colorScheme.surfaceContainerHigh, colors.containerColor)
    WithoutTouchBox(!flat || decor.custom) {
        MaterialOutlinedButton(
            onClick = onClick,
            modifier =
            if (decor.custom) {
                modifier.outlineGlassSurface(shape, decor)
            } else {
                modifier.buttonStyle(group, shape)
            },
            enabled = enabled,
            shape = shape,
            colors =
            when {
                decor.custom -> remember(colors, decor) { colors.outlinedColors(decor) }
                look == ElementLook.FLAT -> colors
                else -> remember(colors, fill) { colors.copy(containerColor = fill) }
            },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat && !decor.custom) elevation else null,
            // The outline-glass look draws its own outline.
            border = if (decor.custom) null else border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Button(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val group = LocalCardGroup.current
    val flat = rememberUiOptions().element(group).look == ElementLook.FLAT
    val decor = rememberFilledDecor(colors, enabled)
    val styled = buttonLook(group, colors.containerColor)
    WithoutTouchBox(!flat || decor.custom) {
        MaterialButton(
            onClick = onClick,
            shapes = shapes,
            modifier = modifier.filledLook(decor, group, shapes.shape),
            enabled = enabled,
            colors = remember(colors, styled, decor) { colors.filledColors(styled, decor) },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat && !decor.custom) elevation else null,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val group = LocalCardGroup.current
    val flat = rememberUiOptions().element(group).look == ElementLook.FLAT
    val decor = rememberFilledDecor(colors, enabled)
    val styled = buttonLook(group, colors.containerColor)
    WithoutTouchBox(!flat || decor.custom) {
        MaterialFilledTonalButton(
            onClick = onClick,
            shapes = shapes,
            modifier = modifier.filledLook(decor, group, shapes.shape),
            enabled = enabled,
            colors = remember(colors, styled, decor) { colors.filledColors(styled, decor) },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat && !decor.custom) elevation else null,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val group = LocalCardGroup.current
    val flat = rememberUiOptions().element(group).look == ElementLook.FLAT
    val look = rememberUiOptions().element(group).look
    val decor = rememberOutlinedDecor(colors, enabled)
    val styled = buttonLook(group, MaterialTheme.colorScheme.surfaceContainerHigh, colors.containerColor)
    WithoutTouchBox(!flat || decor.custom) {
        MaterialOutlinedButton(
            onClick = onClick,
            shapes = shapes,
            modifier =
            if (decor.custom) {
                modifier.outlineGlassSurface(shapes.shape, decor)
            } else {
                modifier.buttonStyle(group, shapes.shape)
            },
            enabled = enabled,
            colors =
            when {
                decor.custom -> remember(colors, decor) { colors.outlinedColors(decor) }
                look == ElementLook.FLAT -> colors
                else -> remember(colors, styled) { colors.copy(containerColor = styled) }
            },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat && !decor.custom) elevation else null,
            // The outline-glass look draws its own outline.
            border = if (decor.custom) null else border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }
}
