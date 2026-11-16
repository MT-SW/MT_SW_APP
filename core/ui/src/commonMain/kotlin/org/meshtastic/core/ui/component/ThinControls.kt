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
@file:Suppress("MagicNumber", "LongParameterList")

package org.meshtastic.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions
import androidx.compose.material3.Checkbox as MaterialCheckbox
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.material3.Switch as MaterialSwitch

/**
 * MT_SW: sliders, switches and check boxes of the app. With the thin look (the default, like on the iPhone) every one
 * of them is drawn only as thin outlines with rounded ends and no filled areas; with the classic look chosen in "Look"
 * they are the old Material controls. The three are used everywhere instead of the Material ones, so the whole app
 * looks the same.
 */
@Composable
fun rememberThinControls(): Boolean = rememberUiOptions()[UiOptions.CONTROL_STYLE] == UiOptions.CONTROL_STYLE_THIN

private const val STROKE_DP = 1.5f
private const val DISABLED_ALPHA = 0.38f
private const val EDGE_ALPHA = 0.7f
private const val SWITCH_ANIMATION_MILLIS = 180
private const val SWITCH_WIDTH_DP = 52
private const val SWITCH_HEIGHT_DP = 30
private const val SWITCH_THUMB_INSET_DP = 6
private const val ON_TINT_ALPHA = 0.14f
private const val CHECK_BOX_DP = 22
private const val CHECK_BOX_CORNER_DP = 6
private const val CHECK_BOX_TOUCH_DP = 24
private const val TRACK_HEIGHT_DP = 10
private const val THUMB_DP = 24

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    if (rememberThinControls()) {
        ThinSwitch(checked, onCheckedChange, modifier, enabled)
    } else {
        MaterialSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun ThinSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier, enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val progress by
        animateFloatAsState(if (checked) 1f else 0f, tween(SWITCH_ANIMATION_MILLIS), label = "thinSwitch")
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    val edge = lerp(scheme.outline.copy(alpha = EDGE_ALPHA), scheme.primary, progress).copy(alpha = alpha)
    val tint = scheme.primary.copy(alpha = ON_TINT_ALPHA * progress * alpha)
    val toggle =
        if (onCheckedChange != null) {
            Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
        } else {
            Modifier
        }
    Box(
        modifier =
        modifier
            .then(toggle)
            .size(SWITCH_WIDTH_DP.dp, SWITCH_HEIGHT_DP.dp)
            .drawBehind {
                val stroke = STROKE_DP.dp.toPx()
                val radius = size.height / 2
                drawRoundRect(color = tint, cornerRadius = CornerRadius(radius))
                drawRoundRect(
                    color = edge,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(radius - stroke / 2),
                    style = Stroke(stroke),
                )
                // The thumb is a ring too: it slides from the left end to the right one.
                val thumbRadius = radius - SWITCH_THUMB_INSET_DP.dp.toPx() + stroke
                val centerX = lerp(radius, size.width - radius, progress)
                drawCircle(
                    color = edge,
                    radius = thumbRadius - stroke / 2,
                    center = Offset(centerX, radius),
                    style = Stroke(stroke),
                )
            },
    )
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    if (rememberThinControls()) {
        ThinCheckbox(checked, onCheckedChange, modifier, enabled)
    } else {
        MaterialCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun ThinCheckbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier, enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val progress by
        animateFloatAsState(if (checked) 1f else 0f, tween(SWITCH_ANIMATION_MILLIS), label = "thinCheckbox")
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    val edge = lerp(scheme.outline.copy(alpha = EDGE_ALPHA), scheme.primary, progress).copy(alpha = alpha)
    val mark = scheme.primary.copy(alpha = progress * alpha)
    val touch =
        if (onCheckedChange != null) {
            Modifier.minimumInteractiveComponentSize()
                .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
        } else {
            Modifier.size(CHECK_BOX_TOUCH_DP.dp)
        }
    Box(modifier = modifier.then(touch), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(CHECK_BOX_DP.dp)) {
            val stroke = STROKE_DP.dp.toPx()
            drawRoundRect(
                color = edge,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(CHECK_BOX_CORNER_DP.dp.toPx()),
                style = Stroke(stroke),
            )
            val tick =
                Path().apply {
                    moveTo(size.width * 0.24f, size.height * 0.52f)
                    lineTo(size.width * 0.43f, size.height * 0.70f)
                    lineTo(size.width * 0.78f, size.height * 0.30f)
                }
            drawPath(tick, mark, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    if (rememberThinControls()) {
        MaterialSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
            colors = colors,
            interactionSource = source,
            steps = steps,
            thumb = { ThinSliderThumb(enabled) },
            track = { state -> ThinSliderTrack(state, enabled) },
            valueRange = valueRange,
        )
    } else {
        MaterialSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            colors = colors,
            interactionSource = source,
        )
    }
}

/** An empty rounded outline from [left] to [right] (the two thin edges closed by round ends). */
private fun DrawScope.capsuleOutline(left: Float, right: Float, color: Color) {
    val stroke = STROKE_DP.dp.toPx()
    val height = size.height - stroke
    drawRoundRect(
        color = color,
        topLeft = Offset(left + stroke / 2, stroke / 2),
        size = Size(right - left - stroke, height),
        cornerRadius = CornerRadius(height / 2),
        style = Stroke(stroke),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThinSliderTrack(state: SliderState, enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val span = state.valueRange.endInclusive - state.valueRange.start
    val fraction = if (span > 0f) ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f) else 0f
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    val inactive = scheme.outline.copy(alpha = EDGE_ALPHA * alpha)
    val active = scheme.primary.copy(alpha = alpha)
    Canvas(modifier = Modifier.fillMaxWidth().height(TRACK_HEIGHT_DP.dp)) {
        capsuleOutline(0f, size.width, inactive)
        // The coloured outline grows from the left end up to the middle of the thumb.
        val half = THUMB_DP.dp.toPx() / 2
        val end = (half + fraction * (size.width - 2 * half)).coerceAtLeast(size.height)
        capsuleOutline(0f, end, active)
    }
}

@Composable
private fun ThinSliderThumb(enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val ring = scheme.primary.copy(alpha = if (enabled) 1f else DISABLED_ALPHA)
    val fill = scheme.surface
    Box(
        modifier =
        Modifier.size(THUMB_DP.dp).drawBehind {
            val stroke = STROKE_DP.dp.toPx()
            drawCircle(color = fill, radius = size.minDimension / 2)
            drawCircle(color = ring, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        },
    )
}
