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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.MeshtasticIcons
import androidx.compose.material3.ExtendedFloatingActionButton as MaterialExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton as MaterialFilledIconButton
import androidx.compose.material3.FilledTonalIconButton as MaterialFilledTonalIconButton
import androidx.compose.material3.FloatingActionButton as MaterialFloatingActionButton

/**
 * MT_SW: the floating buttons and the filled icon buttons of the app. They follow the choice of "Buttons" in "Look" the
 * same way [Button] does: a thin outline on clear glass (the default), filled glass or the plain Material look.
 */
internal fun Modifier.decorated(decor: FilledDecor, shape: Shape): Modifier = when {
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

private const val FLOATING_BACKING_ALPHA = 0.65f

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

/**
 * The short message bar at the bottom of the screens. With the glass or outline look of "Buttons" it is drawn like the
 * floating buttons: clear glass with an outline in the colour of the theme (dark with a light text in the dark theme,
 * light with a dark text in the light theme); with the plain Material look it stays the plain bar.
 */
@Composable
fun StyledSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val decor = rememberDecorFor(accent, accent, enabled = true)
    if (!decor.custom) {
        Snackbar(snackbarData = data, modifier = modifier)
        return
    }
    val shape = RoundedCornerShape(SNACKBAR_CORNER_DP.dp)
    val textColor = decor.outline?.text ?: MaterialTheme.colorScheme.onSurface
    val visuals = data.visuals
    Row(
        modifier =
        modifier
            .padding(SNACKBAR_MARGIN_DP.dp)
            .fillMaxWidth()
            .floatingBacking(shape, decor)
            .decorated(decor, shape)
            .padding(
                start = SNACKBAR_PADDING_DP.dp,
                top = SNACKBAR_VERTICAL_DP.dp,
                bottom = SNACKBAR_VERTICAL_DP.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SNACKBAR_GAP_DP.dp),
    ) {
        Text(
            text = visuals.message,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        visuals.actionLabel?.let { label ->
            TextButton(onClick = data::performAction) { Text(text = label, color = accent) }
        }
        if (visuals.withDismissAction) {
            MaterialIconButtonDismiss(onClick = data::dismiss, tint = textColor)
        }
    }
}

@Composable
private fun MaterialIconButtonDismiss(onClick: () -> Unit, tint: Color) {
    IconButton(onClick = onClick) {
        Icon(imageVector = MeshtasticIcons.Close, contentDescription = null, tint = tint)
    }
}

private const val SNACKBAR_CORNER_DP = 16
private const val SNACKBAR_MARGIN_DP = 12
private const val SNACKBAR_PADDING_DP = 16
private const val SNACKBAR_VERTICAL_DP = 8
private const val SNACKBAR_GAP_DP = 8
