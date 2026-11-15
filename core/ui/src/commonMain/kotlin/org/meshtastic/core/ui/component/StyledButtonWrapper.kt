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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
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
 * MT_SW: the buttons of the app. They follow the look chosen for the group of the current screen (see
 * [LocalCardGroup]) the same way the cards do: plain as before, with depth, see-through or glass.
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
@Composable
fun Modifier.styledControl(shape: Shape): Modifier = buttonStyle(LocalCardGroup.current, shape)

@Composable
private fun Modifier.buttonStyle(group: String, shape: Shape): Modifier {
    val options = rememberUiOptions()
    val style = options.element(group)
    if (style.look == ElementLook.FLAT) return this
    val gradient =
        UiOptions.BUTTON_GRADIENT_SHARE[
            options[UiOptions.BUTTON_GRADIENT].coerceIn(0, UiOptions.BUTTON_GRADIENT_SHARE.lastIndex)]
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

/**
 * MT_SW: Material gives every button an invisible 48dp touch box and draws the modifiers on it, so the gradient and
 * the rim looked like a bigger plate behind the button. In the glass and depth looks the box is switched off, so the
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
    val styled = buttonLook(group, colors.containerColor)
    WithoutTouchBox(!flat) {
        MaterialButton(
            onClick = onClick,
            modifier = modifier.buttonStyle(group, shape),
            enabled = enabled,
            shape = shape,
            colors = remember(colors, styled) { colors.copy(containerColor = styled) },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat) elevation else null,
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
    val styled = buttonLook(group, colors.containerColor)
    WithoutTouchBox(!flat) {
        MaterialFilledTonalButton(
            onClick = onClick,
            modifier = modifier.buttonStyle(group, shape),
            enabled = enabled,
            shape = shape,
            colors = remember(colors, styled) { colors.copy(containerColor = styled) },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat) elevation else null,
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
    // The plain outlined button has no fill; with a look it gets a pane of the surface colour, so the shadow, the
    // glass and the rim have something to be drawn on.
    val fill = buttonLook(group, MaterialTheme.colorScheme.surfaceContainerHigh, colors.containerColor)
    WithoutTouchBox(!flat) {
        MaterialOutlinedButton(
            onClick = onClick,
            modifier = modifier.buttonStyle(group, shape),
            enabled = enabled,
            shape = shape,
            colors = if (look == ElementLook.FLAT) colors else remember(colors, fill) { colors.copy(containerColor = fill) },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat) elevation else null,
            border = border,
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
    val look = rememberUiOptions().element(group).look
    val styled = buttonLook(group, colors.containerColor)
    WithoutTouchBox(!flat) {
        MaterialButton(
            onClick = onClick,
            shapes = shapes,
            modifier = modifier.buttonStyle(group, shapes.shape),
            enabled = enabled,
            colors = if (look == ElementLook.FLAT) colors else remember(colors, styled) { colors.copy(containerColor = styled) },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat) elevation else null,
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
    val look = rememberUiOptions().element(group).look
    val styled = buttonLook(group, colors.containerColor)
    WithoutTouchBox(!flat) {
        MaterialFilledTonalButton(
            onClick = onClick,
            shapes = shapes,
            modifier = modifier.buttonStyle(group, shapes.shape),
            enabled = enabled,
            colors = if (look == ElementLook.FLAT) colors else remember(colors, styled) { colors.copy(containerColor = styled) },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat) elevation else null,
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
    val styled = buttonLook(group, MaterialTheme.colorScheme.surfaceContainerHigh, colors.containerColor)
    WithoutTouchBox(!flat) {
        MaterialOutlinedButton(
            onClick = onClick,
            shapes = shapes,
            modifier = modifier.buttonStyle(group, shapes.shape),
            enabled = enabled,
            colors = if (look == ElementLook.FLAT) colors else remember(colors, styled) { colors.copy(containerColor = styled) },
            // No shadow under a button in the glass and depth looks (it looked like a shifted plate).
            elevation = if (flat) elevation else null,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }
}
