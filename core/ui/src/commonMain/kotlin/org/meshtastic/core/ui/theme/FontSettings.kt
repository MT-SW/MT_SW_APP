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
package org.meshtastic.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.MutableStateFlow

/** Weight slider range relative to the design, in steps of [FONT_WEIGHT_STEP]. */
const val FONT_WEIGHT_MIN = -300
const val FONT_WEIGHT_MAX = 300
const val FONT_WEIGHT_STEP = 50

/** Text size choices in percent; 0 follows the phone. */
val FONT_SIZE_OPTIONS = listOf(0, 50, 60, 70, 80, 90, 100, 110, 120, 135, 150)

private const val MIN_WEIGHT = 100
private const val MAX_WEIGHT = 900

/**
 * [family] name of an installed font, empty for the design's own; [weightShift] see [FONT_WEIGHT_MIN]; [sizePercent] 0
 * follows the phone.
 */
data class FontSettings(val family: String = "", val weightShift: Int = 0, val sizePercent: Int = 0)

/** The current typeface settings of the app; kept in step with the saved settings by the main view model. */
object AppFontSettings {
    val state = MutableStateFlow(FontSettings())
}

private fun TextStyle.withFont(settings: FontSettings, cache: MutableMap<Int, FontFamily?>): TextStyle {
    val weight =
        ((fontWeight?.weight ?: FontWeight.Normal.weight) + settings.weightShift).coerceIn(MIN_WEIGHT, MAX_WEIGHT)
    val family =
        if (settings.family.isBlank()) {
            fontFamily
        } else {
            cache.getOrPut(weight) { systemFontFamily(settings.family, weight) } ?: fontFamily
        }
    return copy(fontFamily = family, fontWeight = FontWeight(weight))
}

/** The whole typescale with the chosen installed font and weight shift; unchanged for the default settings. */
fun Typography.withFontSettings(settings: FontSettings): Typography {
    if (settings.family.isBlank() && settings.weightShift == 0) return this
    val cache = mutableMapOf<Int, FontFamily?>()

    fun TextStyle.f() = withFont(settings, cache)
    return copy(
        displayLarge = displayLarge.f(),
        displayMedium = displayMedium.f(),
        displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f(),
        headlineMedium = headlineMedium.f(),
        headlineSmall = headlineSmall.f(),
        titleLarge = titleLarge.f(),
        titleMedium = titleMedium.f(),
        titleSmall = titleSmall.f(),
        bodyLarge = bodyLarge.f(),
        bodyMedium = bodyMedium.f(),
        bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f(),
        labelMedium = labelMedium.f(),
        labelSmall = labelSmall.f(),
    )
}
