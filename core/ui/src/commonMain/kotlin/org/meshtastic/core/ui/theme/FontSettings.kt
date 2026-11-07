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

/** Typeface choices: 0 as designed (the system font), then the generic families every platform has built in. */
val FONT_FAMILY_OPTIONS = listOf(0, 1, 2, 3)

/** Weight steps relative to the design, in hundreds. */
val FONT_WEIGHT_SHIFT_OPTIONS = listOf(-200, -100, 0, 100, 200)

/** Text size choices in percent; 0 follows the phone. */
val FONT_SIZE_OPTIONS = listOf(0, 80, 90, 100, 110, 120, 135, 150)

private const val MIN_WEIGHT = 100
private const val MAX_WEIGHT = 900

/** [family] see [FONT_FAMILY_OPTIONS], [weightShift] see [FONT_WEIGHT_SHIFT_OPTIONS], [sizePercent] 0 = phone's own. */
data class FontSettings(val family: Int = 0, val weightShift: Int = 0, val sizePercent: Int = 0)

/** The current typeface settings of the app; kept in step with the saved settings by the main view model. */
object AppFontSettings {
    val state = MutableStateFlow(FontSettings())
}

private fun TextStyle.withFont(settings: FontSettings): TextStyle {
    val family =
        when (settings.family) {
            1 -> FontFamily.SansSerif
            2 -> FontFamily.Serif
            3 -> FontFamily.Monospace
            else -> null
        }
    val weight =
        if (settings.weightShift == 0) {
            null
        } else {
            FontWeight(((fontWeight?.weight ?: FontWeight.Normal.weight) + settings.weightShift).coerceIn(MIN_WEIGHT, MAX_WEIGHT))
        }
    return copy(fontFamily = family ?: fontFamily, fontWeight = weight ?: fontWeight)
}

/** The whole typescale with the chosen typeface family and weight shift; unchanged for the default settings. */
fun Typography.withFontSettings(settings: FontSettings): Typography {
    if (settings.family == 0 && settings.weightShift == 0) return this
    return copy(
        displayLarge = displayLarge.withFont(settings),
        displayMedium = displayMedium.withFont(settings),
        displaySmall = displaySmall.withFont(settings),
        headlineLarge = headlineLarge.withFont(settings),
        headlineMedium = headlineMedium.withFont(settings),
        headlineSmall = headlineSmall.withFont(settings),
        titleLarge = titleLarge.withFont(settings),
        titleMedium = titleMedium.withFont(settings),
        titleSmall = titleSmall.withFont(settings),
        bodyLarge = bodyLarge.withFont(settings),
        bodyMedium = bodyMedium.withFont(settings),
        bodySmall = bodySmall.withFont(settings),
        labelLarge = labelLarge.withFont(settings),
        labelMedium = labelMedium.withFont(settings),
        labelSmall = labelSmall.withFont(settings),
    )
}
