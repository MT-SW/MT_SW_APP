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
@file:Suppress("MatchingDeclarationName", "MagicNumber")

package org.meshtastic.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.choose_theme
import org.meshtastic.core.resources.done
import org.meshtastic.core.resources.dynamic
import org.meshtastic.core.resources.reset
import org.meshtastic.core.resources.theme_color_dark
import org.meshtastic.core.resources.theme_color_hue
import org.meshtastic.core.resources.theme_color_intensity
import org.meshtastic.core.resources.theme_color_intensity_hint
import org.meshtastic.core.resources.theme_color_light
import org.meshtastic.core.resources.theme_color_pick
import org.meshtastic.core.resources.theme_color_presets
import org.meshtastic.core.resources.theme_color_saturation
import org.meshtastic.core.resources.theme_color_summary
import org.meshtastic.core.resources.theme_color_system
import org.meshtastic.core.resources.theme_color_title
import org.meshtastic.core.resources.theme_dark
import org.meshtastic.core.resources.theme_light
import org.meshtastic.core.resources.theme_mono_dark
import org.meshtastic.core.resources.theme_mono_light
import org.meshtastic.core.resources.theme_system
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.theme.DEFAULT_THEME_INTENSITY
import org.meshtastic.core.ui.theme.DEFAULT_THEME_SEED
import org.meshtastic.core.ui.theme.MAX_THEME_INTENSITY
import org.meshtastic.core.ui.theme.MIN_THEME_INTENSITY
import org.meshtastic.core.ui.theme.MODE_COLOR
import org.meshtastic.core.ui.theme.MODE_COLOR_DARK
import org.meshtastic.core.ui.theme.MODE_COLOR_LIGHT
import org.meshtastic.core.ui.theme.MODE_DYNAMIC
import org.meshtastic.core.ui.theme.MODE_MONO_DARK
import org.meshtastic.core.ui.theme.MODE_MONO_LIGHT
import kotlin.math.max
import kotlin.math.min

/** Theme modes that match AppCompatDelegate constants for cross-platform use. */
enum class ThemeOption(val label: StringResource, val mode: Int) {
    DYNAMIC(label = Res.string.dynamic, mode = MODE_DYNAMIC),
    COLOR(label = Res.string.theme_color_system, mode = MODE_COLOR),
    COLOR_LIGHT(label = Res.string.theme_color_light, mode = MODE_COLOR_LIGHT),
    COLOR_DARK(label = Res.string.theme_color_dark, mode = MODE_COLOR_DARK),
    LIGHT(label = Res.string.theme_light, mode = 1), // AppCompatDelegate.MODE_NIGHT_NO
    DARK(label = Res.string.theme_dark, mode = 2), // AppCompatDelegate.MODE_NIGHT_YES
    SYSTEM(label = Res.string.theme_system, mode = -1), // AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    MONO_LIGHT(label = Res.string.theme_mono_light, mode = MODE_MONO_LIGHT),
    MONO_DARK(label = Res.string.theme_mono_dark, mode = MODE_MONO_DARK),
}

/** Ready colours for the colour themes (ARGB). */
private val PRESET_COLORS =
    listOf(
        0xFFF5C839.toInt(), // MT_SW gold
        0xFF2979FF.toInt(), // blue
        0xFF00B8D4.toInt(), // turquoise
        0xFF00C853.toInt(), // green
        0xFFAEEA00.toInt(), // lime
        0xFFFF9100.toInt(), // orange
        0xFFFF3D00.toInt(), // red
        0xFFF50057.toInt(), // pink
        0xFFAA00FF.toInt(), // purple
        0xFF5C6BC0.toInt(), // indigo
    )

/**
 * Shared dialog for picking a theme option. Used by Android, Desktop and iPhone/iPad settings screens. The colour
 * themes work their whole palette out from one colour; "Theme colour…" opens the picker for it.
 */
@Composable
fun ThemePickerDialog(
    onClickTheme: (Int) -> Unit,
    onDismiss: () -> Unit,
    themeSeed: Int,
    themeIntensity: Int,
    onThemeSeedChange: (Int) -> Unit,
    onThemeIntensityChange: (Int) -> Unit,
) {
    var showColorPicker by remember { mutableStateOf(false) }
    if (showColorPicker) {
        ThemeColorDialog(
            seed = themeSeed,
            intensity = themeIntensity,
            onSeedChange = onThemeSeedChange,
            onIntensityChange = onThemeIntensityChange,
            onDismiss = { showColorPicker = false },
        )
        return
    }
    MeshtasticDialog(
        title = stringResource(Res.string.choose_theme),
        onDismiss = onDismiss,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ThemeOption.entries.forEach { option ->
                    ListItem(text = stringResource(option.label), trailingIcon = null) {
                        onClickTheme(option.mode)
                        onDismiss()
                    }
                }
                ListItem(text = stringResource(Res.string.theme_color_pick), trailingIcon = null) {
                    showColorPicker = true
                }
            }
        },
    )
}

/**
 * The colour of the colour themes: ready colours, hue and saturation sliders, and the strength of the colours (from
 * soft to very sharp). Every change applies at once, so the app behind the window shows the result.
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod")
@Composable
private fun ThemeColorDialog(
    seed: Int,
    intensity: Int,
    onSeedChange: (Int) -> Unit,
    onIntensityChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = remember { seed.toHsv() }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var saturation by remember { mutableFloatStateOf(initial[1]) }
    var strength by remember { mutableFloatStateOf(intensity.toFloat()) }

    fun applySeed(h: Float, s: Float) {
        hue = h
        saturation = s
        onSeedChange(Color.hsv(h, s, 1f).toArgbInt())
    }

    MeshtasticDialog(
        title = stringResource(Res.string.theme_color_title),
        onDismiss = onDismiss,
        confirmTextRes = Res.string.done,
        onConfirm = onDismiss,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(Res.string.theme_color_summary),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Box(
                    modifier =
                    Modifier.fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.hsv(hue, saturation, 1f)),
                )
                Text(
                    text = stringResource(Res.string.theme_color_presets),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PRESET_COLORS.forEach { preset ->
                        val hsv = preset.toHsv()
                        Box(
                            modifier =
                            Modifier.size(36.dp)
                                .clip(CircleShape)
                                .background(Color(preset))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { applySeed(hsv[0], hsv[1]) },
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.theme_color_hue),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Slider(value = hue, onValueChange = { applySeed(it, saturation) }, valueRange = 0f..360f)
                Text(
                    text = stringResource(Res.string.theme_color_saturation),
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(value = saturation, onValueChange = { applySeed(hue, it) }, valueRange = 0f..1f)
                Text(
                    text = stringResource(Res.string.theme_color_intensity),
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = strength,
                    onValueChange = {
                        strength = it
                        onIntensityChange(it.toInt())
                    },
                    valueRange = MIN_THEME_INTENSITY.toFloat()..MAX_THEME_INTENSITY.toFloat(),
                )
                Text(
                    text = stringResource(Res.string.theme_color_intensity_hint) + " (${strength.toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                )
                // Brings back the default colour and strength; the window stays open.
                TextButton(
                    onClick = {
                        val default = DEFAULT_THEME_SEED.toHsv()
                        applySeed(default[0], default[1])
                        strength = DEFAULT_THEME_INTENSITY.toFloat()
                        onIntensityChange(DEFAULT_THEME_INTENSITY)
                    },
                ) {
                    Text(stringResource(Res.string.reset))
                }
            }
        },
    )
}

private fun Color.toArgbInt(): Int {
    val a = (alpha * 255f + 0.5f).toInt()
    val r = (red * 255f + 0.5f).toInt()
    val g = (green * 255f + 0.5f).toInt()
    val b = (blue * 255f + 0.5f).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

/** Hue (0..360), saturation and value (0..1) of an ARGB colour. */
private fun Int.toHsv(): FloatArray {
    val r = ((this shr 16) and 0xFF) / 255f
    val g = ((this shr 8) and 0xFF) / 255f
    val b = (this and 0xFF) / 255f
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val d = mx - mn
    val h =
        when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
    return floatArrayOf(if (h < 0f) h + 360f else h, if (mx == 0f) 0f else d / mx, mx)
}
