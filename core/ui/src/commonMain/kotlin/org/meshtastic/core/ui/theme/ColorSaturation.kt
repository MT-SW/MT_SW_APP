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
@file:Suppress("MagicNumber")

package org.meshtastic.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow

/** Strength of the colours in percent for the saturated themes; 100 is the palette as designed. */
const val SATURATION_VIVID: Int = 150
const val SATURATION_VERY_VIVID: Int = 230

private const val GAMUT_STEPS = 20
private const val GAMUT_EDGE = 0.0005
private const val BACK_OFF = 0.8

/**
 * The same palette with stronger colours: the chroma of every colour of the scheme is multiplied in the perceptual
 * OKLCH space, hue and lightness stay, so gold stays gold and a text stays as readable. Backgrounds, surfaces, outlines
 * and the grey secondary colours stay untouched, so grey stays grey; only accent colours get stronger. [percent] 100
 * changes nothing.
 */
fun ColorScheme.withSaturation(percent: Int): ColorScheme {
    val k = percent / 100.0
    if (k == 1.0) return this
    return copy(
        primary = primary.saturated(k),
        onPrimary = onPrimary.saturated(k),
        primaryContainer = primaryContainer.saturated(k),
        onPrimaryContainer = onPrimaryContainer.saturated(k),
        inversePrimary = inversePrimary.saturated(k),
        tertiary = tertiary.saturated(k),
        onTertiary = onTertiary.saturated(k),
        tertiaryContainer = tertiaryContainer.saturated(k),
        onTertiaryContainer = onTertiaryContainer.saturated(k),
        surfaceTint = surfaceTint.saturated(k),
        error = error.saturated(k),
        onError = onError.saturated(k),
        errorContainer = errorContainer.saturated(k),
        onErrorContainer = onErrorContainer.saturated(k),
    )
}

/** This colour with its chroma multiplied by [k]; when that leaves the screen's range, as much as still fits. */
fun Color.saturated(k: Double): Color {
    val (l, a, b) = toOklab()
    var factor = k
    repeat(GAMUT_STEPS) {
        val color = oklabToColor(l, a * factor, b * factor)
        if (color != null) return color.copy(alpha = alpha)
        // Move back towards the original chroma until the colour fits.
        factor = 1.0 + (factor - 1.0) * BACK_OFF
    }
    return this
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
    if (!fits(r) || !fits(g) || !fits(bl)) return null
    return Color(
        red = linearToSrgb(r.coerceIn(0.0, 1.0)).toFloat(),
        green = linearToSrgb(g.coerceIn(0.0, 1.0)).toFloat(),
        blue = linearToSrgb(bl.coerceIn(0.0, 1.0)).toFloat(),
    )
}

private fun fits(channel: Double) = channel >= -GAMUT_EDGE && channel <= 1.0 + GAMUT_EDGE

private fun srgbToLinear(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

private fun linearToSrgb(x: Double) = if (x <= 0.0031308) x * 12.92 else 1.055 * x.pow(1.0 / 2.4) - 0.055

private fun cbrt(x: Double) = if (x < 0) -abs(x).pow(1.0 / 3.0) else x.pow(1.0 / 3.0)
