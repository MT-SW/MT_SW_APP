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
@file:Suppress("MagicNumber", "TooManyFunctions")

package org.meshtastic.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** The colour used when nothing was chosen: the gold of the MT_SW icon. */
const val DEFAULT_THEME_SEED: Int = 0xFFF5C839.toInt()

/** The strength of the colours in percent; 100 is the normal Material You look, below is softer, above is sharper. */
const val DEFAULT_THEME_INTENSITY: Int = 100
const val MIN_THEME_INTENSITY: Int = 30
const val MAX_THEME_INTENSITY: Int = 200

private const val FULL_TURN = 360.0
private const val MIN_ACCENT_CHROMA = 0.12
private const val MAX_ACCENT_CHROMA = 0.30

/**
 * A whole colour scheme worked out from one colour, the way Material You does it from a wallpaper: tonal palettes
 * (primary, secondary, tertiary, neutral) in the perceptual OKLCH space, and the usual tones for every role. Used where
 * the system gives no colours of its own (iPhone, computer) and as the "colour theme" everywhere. [intensity] is in
 * percent: 100 is normal, lower is softer, higher is sharper. The error colours stay those of [base].
 */
fun seedColorScheme(seed: Color, dark: Boolean, intensity: Int, base: ColorScheme): ColorScheme {
    val (_, a, b) = seed.toOklab()
    val seedChroma = sqrt(a * a + b * b)
    val hue = (Math2.degrees(atan2(b, a)) + FULL_TURN) % FULL_TURN
    val k = intensity.coerceIn(MIN_THEME_INTENSITY, MAX_THEME_INTENSITY) / 100.0
    val accent = (maxOf(seedChroma, MIN_ACCENT_CHROMA) * k).coerceAtMost(MAX_ACCENT_CHROMA)

    val primary = Palette(hue, accent)
    val secondary = Palette(hue, accent / 3.0)
    val tertiary = Palette((hue + 60.0) % FULL_TURN, accent * 0.55)
    val neutral = Palette(hue, (0.006 + accent * 0.045).coerceAtMost(0.02))
    val neutralVariant = Palette(hue, (0.012 + accent * 0.09).coerceAtMost(0.04))

    return if (dark) {
        base.copy(
            primary = primary.tone(80),
            onPrimary = primary.tone(20),
            primaryContainer = primary.tone(30),
            onPrimaryContainer = primary.tone(90),
            inversePrimary = primary.tone(40),
            secondary = secondary.tone(80),
            onSecondary = secondary.tone(20),
            secondaryContainer = secondary.tone(30),
            onSecondaryContainer = secondary.tone(90),
            tertiary = tertiary.tone(80),
            onTertiary = tertiary.tone(20),
            tertiaryContainer = tertiary.tone(30),
            onTertiaryContainer = tertiary.tone(90),
            background = neutral.tone(6),
            onBackground = neutral.tone(90),
            surface = neutral.tone(6),
            onSurface = neutral.tone(90),
            surfaceVariant = neutralVariant.tone(30),
            onSurfaceVariant = neutralVariant.tone(80),
            surfaceTint = primary.tone(80),
            inverseSurface = neutral.tone(90),
            inverseOnSurface = neutral.tone(20),
            outline = neutralVariant.tone(60),
            outlineVariant = neutralVariant.tone(30),
            scrim = Color.Black,
            surfaceBright = neutral.tone(24),
            surfaceDim = neutral.tone(6),
            surfaceContainerLowest = neutral.tone(4),
            surfaceContainerLow = neutral.tone(10),
            surfaceContainer = neutral.tone(12),
            surfaceContainerHigh = neutral.tone(17),
            surfaceContainerHighest = neutral.tone(22),
        )
    } else {
        base.copy(
            primary = primary.tone(40),
            onPrimary = primary.tone(100),
            primaryContainer = primary.tone(90),
            onPrimaryContainer = primary.tone(10),
            inversePrimary = primary.tone(80),
            secondary = secondary.tone(40),
            onSecondary = secondary.tone(100),
            secondaryContainer = secondary.tone(90),
            onSecondaryContainer = secondary.tone(10),
            tertiary = tertiary.tone(40),
            onTertiary = tertiary.tone(100),
            tertiaryContainer = tertiary.tone(90),
            onTertiaryContainer = tertiary.tone(10),
            background = neutral.tone(98),
            onBackground = neutral.tone(10),
            surface = neutral.tone(98),
            onSurface = neutral.tone(10),
            surfaceVariant = neutralVariant.tone(90),
            onSurfaceVariant = neutralVariant.tone(30),
            surfaceTint = primary.tone(40),
            inverseSurface = neutral.tone(20),
            inverseOnSurface = neutral.tone(95),
            outline = neutralVariant.tone(50),
            outlineVariant = neutralVariant.tone(80),
            scrim = Color.Black,
            surfaceBright = neutral.tone(98),
            surfaceDim = neutral.tone(87),
            surfaceContainerLowest = neutral.tone(100),
            surfaceContainerLow = neutral.tone(96),
            surfaceContainer = neutral.tone(94),
            surfaceContainerHigh = neutral.tone(92),
            surfaceContainerHighest = neutral.tone(90),
        )
    }
}

/** One hue and chroma in OKLCH; [tone] gives its colour at a Material tone (0 black … 100 white). */
private class Palette(private val hue: Double, private val chroma: Double) {
    fun tone(tone: Int): Color {
        if (tone <= 0) return Color.Black
        if (tone >= 100) return Color.White
        // Material's tone is CIE L*; for a grey that is OKLab lightness (L* + 16) / 116 (a cube root of luminance).
        val lightness = if (tone > 8) (tone + 16) / 116.0 else (tone / 903.3).pow(1.0 / 3.0)
        val radians = Math2.radians(hue)
        // A colour that does not fit the screen's range loses chroma until it does.
        var c = chroma
        repeat(24) {
            val color = oklabToColor(lightness, c * cos(radians), c * sin(radians))
            if (color != null) return color
            c *= 0.9
        }
        return oklabToColor(lightness, 0.0, 0.0) ?: Color.Gray
    }
}

private object Math2 {
    fun degrees(radians: Double) = radians * 180.0 / kotlin.math.PI

    fun radians(degrees: Double) = degrees * kotlin.math.PI / 180.0
}

private fun Color.toOklab(): Triple<Double, Double, Double> {
    val r = srgbToLinear(red.toDouble())
    val g = srgbToLinear(green.toDouble())
    val b = srgbToLinear(blue.toDouble())
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    return Triple(
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
    )
}

/** The colour at this OKLab point, or null when it lies outside the sRGB range. */
private fun oklabToColor(lightness: Double, a: Double, b: Double): Color? {
    val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).let { it * it * it }
    val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).let { it * it * it }
    val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).let { it * it * it }
    val r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    val g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    val bl = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    val eps = 0.0005
    if (r < -eps || r > 1 + eps || g < -eps || g > 1 + eps || bl < -eps || bl > 1 + eps) return null
    return Color(
        red = linearToSrgb(r.coerceIn(0.0, 1.0)).toFloat(),
        green = linearToSrgb(g.coerceIn(0.0, 1.0)).toFloat(),
        blue = linearToSrgb(bl.coerceIn(0.0, 1.0)).toFloat(),
    )
}

private fun srgbToLinear(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

private fun linearToSrgb(x: Double) = if (x <= 0.0031308) x * 12.92 else 1.055 * x.pow(1.0 / 2.4) - 0.055

private fun cbrt(x: Double) = if (x < 0) -abs(x).pow(1.0 / 3.0) else x.pow(1.0 / 3.0)
