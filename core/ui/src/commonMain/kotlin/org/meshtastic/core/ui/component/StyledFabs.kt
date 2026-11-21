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
@file:Suppress("LongParameterList")

package org.meshtastic.core.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExtendedFloatingActionButton as MaterialExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton as MaterialFilledIconButton
import androidx.compose.material3.FilledTonalIconButton as MaterialFilledTonalIconButton
import androidx.compose.material3.FloatingActionButton as MaterialFloatingActionButton

/**
 * MT_SW: the floating buttons and the filled icon buttons of the app. They follow the choice of "Buttons" in "Look" the
 * same way [Button] does: a thin outline on clear glass (the default), filled glass or the plain Material look.
 */
private fun Modifier.decorated(decor: FilledDecor, shape: Shape): Modifier = when {
    decor.outline != null -> outlineGlassSurface(shape, decor)
    decor.glass != null -> glassSurface(shape, decor.glass)
    else -> this
}

/**
 * A floating button sits over lists and maps, so its clear glass lets the content behind it show through too much. A
 * little of the surface colour behind the glass keeps it readable; the glass itself and its outline are drawn on top.
 */
@Composable
internal fun Modifier.floatingBacking(shape: Shape, decor: FilledDecor): Modifier {
    if (!decor.custom) return this
    val color = MaterialTheme.colorScheme.surface.copy(alpha = FLOATING_BACKING_ALPHA)
    return drawBehind { drawOutline(outline = shape.createOutline(size, layoutDirection, this), color = color) }
}

private const val FLOATING_BACKING_ALPHA = 0.45f

/** The strong colour that goes with a "container" colour of the theme, so a floating button has a clear outline. */
@Composable
@ReadOnlyComposable
private fun accentOf(container: Color): Color {
    val scheme = MaterialTheme.colorScheme
    return when (container) {
        scheme.primaryContainer -> scheme.primary

        scheme.secondaryContainer -> scheme.secondary

        scheme.tertiaryContainer -> scheme.tertiary

        scheme.surfaceContainerHigh,
        scheme.surfaceContainer,
        scheme.surfaceContainerHighest,
        scheme.surface,
        -> scheme.outline

        else -> container
    }
}

@Composable
private fun noShadow(): FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(
    defaultElevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
)

@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val accent = accentOf(containerColor)
    val decor = rememberDecorFor(accent, accent, enabled = true)
    MaterialFloatingActionButton(
        onClick = onClick,
        modifier = modifier.floatingBacking(shape, decor).decorated(decor, shape),
        shape = shape,
        containerColor = if (decor.custom) Color.Transparent else containerColor,
        contentColor = decor.outline?.text ?: contentColor,
        elevation = if (decor.custom) noShadow() else elevation,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) {
    val accent = accentOf(containerColor)
    val decor = rememberDecorFor(accent, accent, enabled = true)
    MaterialExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier.floatingBacking(shape, decor).decorated(decor, shape),
        shape = shape,
        containerColor = if (decor.custom) Color.Transparent else containerColor,
        contentColor = decor.outline?.text ?: contentColor,
        elevation = if (decor.custom) noShadow() else elevation,
        interactionSource = interactionSource,
    )
}

/** The colours of a filled icon button when it draws its own surface: clear, with the text colour of the look. */
private fun IconButtonColors.styled(decor: FilledDecor): IconButtonColors = when {
    decor.outline != null ->
        copy(
            containerColor = Color.Transparent,
            contentColor = decor.outline.text,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = decor.outline.disabledText,
        )

    decor.glass != null -> copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent)

    else -> this
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val decor = rememberDecorFor(colors.containerColor, colors.disabledContainerColor, enabled)
    MaterialFilledIconButton(
        onClick = onClick,
        modifier = modifier.decorated(decor, shape),
        enabled = enabled,
        shape = shape,
        colors = colors.styled(decor),
        interactionSource = interactionSource,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val decor = rememberDecorFor(colors.containerColor, colors.disabledContainerColor, enabled)
    MaterialFilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.decorated(decor, shape),
        enabled = enabled,
        shape = shape,
        colors = colors.styled(decor),
        interactionSource = interactionSource,
        content = content,
    )
}
