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
@file:Suppress("CompositionLocalAllowlist", "MagicNumber", "MatchingDeclarationName", "TooManyFunctions")

package org.meshtastic.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme.Companion.expressive
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.Density
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlin.concurrent.Volatile

private val lightScheme =
    lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    )

private val darkScheme =
    darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )

// ─── Monochrome (black and white) themes ───
// Pure greys only. Nothing in these two schemes carries a hue, and AppTheme additionally drains the colour out of
// everything that is drawn with a hard-coded colour (signal quality, charts, node colours, pictures), so the whole
// app really is black and white.
private val monoLightScheme =
    lightColorScheme(
        primary = Color(0xFF000000),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE0E0E0),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFF303030),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8E8E8),
        onSecondaryContainer = Color(0xFF000000),
        tertiary = Color(0xFF505050),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE0E0E0),
        onTertiaryContainer = Color(0xFF000000),
        error = Color(0xFF202020),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFD0D0D0),
        onErrorContainer = Color(0xFF000000),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF000000),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFEBEBEB),
        onSurfaceVariant = Color(0xFF303030),
        outline = Color(0xFF606060),
        outlineVariant = Color(0xFFBDBDBD),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF111111),
        inverseOnSurface = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFDDDDDD),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF5F5F5),
        surfaceContainer = Color(0xFFEFEFEF),
        surfaceContainerHigh = Color(0xFFE8E8E8),
        surfaceContainerHighest = Color(0xFFE0E0E0),
    )

private val monoDarkScheme =
    darkColorScheme(
        primary = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF2E2E2E),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFFD8D8D8),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF2A2A2A),
        onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFFBDBDBD),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF303030),
        onTertiaryContainer = Color(0xFFFFFFFF),
        error = Color(0xFFE0E0E0),
        onError = Color(0xFF000000),
        errorContainer = Color(0xFF3A3A3A),
        onErrorContainer = Color(0xFFFFFFFF),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF242424),
        onSurfaceVariant = Color(0xFFD0D0D0),
        outline = Color(0xFF9E9E9E),
        outlineVariant = Color(0xFF424242),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFF2F2F2),
        inverseOnSurface = Color(0xFF000000),
        inversePrimary = Color(0xFF000000),
        surfaceDim = Color(0xFF000000),
        surfaceBright = Color(0xFF383838),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF0E0E0E),
        surfaceContainer = Color(0xFF161616),
        surfaceContainerHigh = Color(0xFF1F1F1F),
        surfaceContainerHighest = Color(0xFF292929),
    )

/** The dark scheme with pure black backgrounds (and very dark greys for the layers above them), for AMOLED screens. */
private fun ColorScheme.withTrueBlack(): ColorScheme = copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1B1B1B),
    surfaceContainerHighest = Color(0xFF252525),
    surfaceBright = Color(0xFF2E2E2E),
    surfaceVariant = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF333333),
)

/**
 * The light scheme with the whites turned into very light greys: hardly noticeable in the ordinary theme, a little
 * stronger (still bright) in the stronger-colour ones. The coloured parts are left as they are.
 */
private fun ColorScheme.withSoftWhite(strong: Boolean): ColorScheme = if (strong) {
    copy(
        background = Color(0xFFEDEDEE),
        surface = Color(0xFFEDEDEE),
        surfaceDim = Color(0xFFD4D4D5),
        surfaceBright = Color(0xFFEDEDEE),
        surfaceContainerLowest = Color(0xFFF3F3F4),
        surfaceContainerLow = Color(0xFFEBEBEC),
        surfaceContainer = Color(0xFFE6E6E7),
        surfaceContainerHigh = Color(0xFFE1E1E2),
        surfaceContainerHighest = Color(0xFFDCDCDD),
    )
} else {
    copy(
        background = Color(0xFFF3F3F4),
        surface = Color(0xFFF3F3F4),
        surfaceDim = Color(0xFFDADADB),
        surfaceBright = Color(0xFFF3F3F4),
        surfaceContainerLowest = Color(0xFFF8F8F9),
        surfaceContainerLow = Color(0xFFF1F1F2),
        surfaceContainer = Color(0xFFEDEDEE),
        surfaceContainerHigh = Color(0xFFE8E8E9),
        surfaceContainerHighest = Color(0xFFE3E3E4),
    )
}

/** True while one of the two black-and-white themes is active; maps and charts can read it to drop their colour too. */
val LocalMonochrome = compositionLocalOf { false }

/** Colour strength of the chosen theme in percent (100 = as designed); the device colours follow it. */
val LocalColorSaturation = compositionLocalOf { 100 }

/**
 * Counts the native views (maps, camera) that are on screen. They draw on a surface of their own, which the grayscale
 * layer would turn into a black hole, so while one is shown the layer is paused. Those views take care of their own
 * colours (the map is switched to grayscale through [LocalMonochrome]).
 */
private class GrayscaleGate {
    var nativeViews by mutableIntStateOf(0)
}

private val LocalGrayscaleGate = compositionLocalOf<GrayscaleGate?> { null }

/** Call from a composable that shows a native surface (map, camera): pauses the grayscale layer while it is shown. */
@Composable
fun PauseGrayscaleLayer() {
    val gate = LocalGrayscaleGate.current ?: return
    DisposableEffect(gate) {
        gate.nativeViews++
        onDispose { gate.nativeViews-- }
    }
}

/**
 * Whether the black-and-white theme is active, readable outside composition (map layers build their colours as text in
 * plain functions). Set by [AppTheme].
 */
object MonochromeState {
    @Volatile var on: Boolean = false
}

/** [this] as a grey of the same brightness while a black-and-white theme is active; unchanged otherwise. */
fun Color.monoSafe(): Color {
    if (!MonochromeState.on) return this
    val grey = 0.299f * red + 0.587f * green + 0.114f * blue
    return Color(grey, grey, grey, alpha)
}

/**
 * Drains the colour out of this element (and what it contains) in the black-and-white themes. Used for the bars while a
 * map is on screen, because the global grayscale layer is paused then. Never put a native view (map, camera) inside.
 */
@Composable
fun Modifier.monochromeLayer(): Modifier {
    if (!LocalMonochrome.current) return this
    val paint = remember {
        Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    }
    return this.drawWithContent {
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            canvas.restore()
        }
    }
}

/** Draws [content] with all colour drained out of it (saturation 0), including pictures and hard-coded colours. */
@Composable
private fun GrayscaleLayer(content: @Composable () -> Unit) {
    val gate = remember { GrayscaleGate() }
    val paint = remember {
        Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    }
    val paused = gate.nativeViews > 0
    Box(
        modifier =
        if (paused) {
            Modifier
        } else {
            Modifier.drawWithContent {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                    drawContent()
                    canvas.restore()
                }
            }
        },
    ) {
        CompositionLocalProvider(LocalGrayscaleGate provides gate) { content() }
    }
}

@Immutable
data class ColorFamily(val color: Color, val onColor: Color, val colorContainer: Color, val onColorContainer: Color)

val unspecified_scheme = ColorFamily(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    monochrome: Boolean = false,
    /** Strength of the app's own palette in percent (not the wallpaper colours); 100 changes nothing. */
    saturation: Int = 100,
    content:
    @Composable()
    () -> Unit,
) {
    val dynamicScheme = if (!monochrome && dynamicColor) dynamicColorScheme(darkTheme) else null
    val baseScheme =
        if (monochrome) {
            if (darkTheme) monoDarkScheme else monoLightScheme
        } else {
            dynamicScheme ?: (if (darkTheme) darkScheme else lightScheme).withSaturation(saturation)
        }

    // The stronger-colour themes go to true black in the dark: on an AMOLED screen the pixels are then switched off.
    val uiOptions = rememberUiOptions()
    val colorScheme =
        when {
            !monochrome && darkTheme && saturation > 100 -> baseScheme.withTrueBlack()

            // In the light the white always goes slightly grey (a little more in the stronger-colour themes), unlike
            // the original.
            !monochrome && !darkTheme && dynamicScheme == null -> baseScheme.withSoftWhite(strong = saturation > 100)

            else -> baseScheme
        }

    // When a device is on event firmware (and the event theme isn't opted out), swap the whole typescale to the event
    // typeface. Null everywhere else (desktop, F-Droid, non-event, opted out) → default typography.
    val eventFonts = LocalEventTheme.current?.fonts
    val fontSettings by AppFontSettings.state.collectAsState()
    val typography =
        remember(eventFonts, fontSettings) {
            (eventFonts?.let { AppTypography.withEventFonts(it) } ?: AppTypography).withFontSettings(fontSettings)
        }

    // Downloadable (Google) fonts referenced only through the theme's Typography are NOT auto-fetched during text
    // layout — Compose silently renders the fallback. Preloading them into the font cache is the documented way to make
    // them actually apply; once cached, the themed Text re-resolves to the real typeface. No-op when there are no event
    // fonts (desktop / F-Droid / non-event / opted out).
    val fontResolver = LocalFontFamilyResolver.current
    LaunchedEffect(eventFonts, fontResolver) {
        val fonts = eventFonts ?: return@LaunchedEffect
        listOfNotNull(fonts.heading, fonts.body).forEach { family ->
            try {
                fontResolver.preload(family)
            } catch (e: CancellationException) {
                throw e // preload suspends; never swallow structured-concurrency cancellation
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(e) { "Event font preload failed for $family; falling back to the default typeface" }
            }
        }
    }

    MonochromeState.on = monochrome
    val density = LocalDensity.current
    val textDensity =
        remember(density, fontSettings.sizePercent) {
            if (fontSettings.sizePercent <= 0) {
                density
            } else {
                Density(density.density, fontScale = fontSettings.sizePercent / 100f)
            }
        }
    CompositionLocalProvider(LocalDensity provides textDensity) {
        MaterialExpressiveTheme(colorScheme = colorScheme, typography = typography, motionScheme = expressive()) {
            CompositionLocalProvider(
                LocalMonochrome provides monochrome,
                LocalColorSaturation provides saturation,
                LocalUiOptions provides uiOptions,
            ) {
                if (monochrome) GrayscaleLayer(content) else content()
            }
        }
    }
}

const val MODE_DYNAMIC = 6969420

/** The two black-and-white themes: white background with dark text, and black background with light text. */
const val MODE_MONO_LIGHT = 6969421
const val MODE_MONO_DARK = 6969422

/** The saturated themes: the whole palette with stronger colours; follows the system, or forced light or dark. */
const val MODE_VIVID = 6969423
const val MODE_VIVID_LIGHT = 6969424
const val MODE_VIVID_DARK = 6969425

/** The same, stronger still. */
const val MODE_VERY_VIVID = 6969426
const val MODE_VERY_VIVID_LIGHT = 6969427
const val MODE_VERY_VIVID_DARK = 6969428

/** How strong the palette is for [mode], in percent: 100 for the ordinary themes. */
fun saturationFor(mode: Int): Int = when (mode) {
    MODE_VIVID,
    MODE_VIVID_LIGHT,
    MODE_VIVID_DARK,
    -> SATURATION_VIVID

    MODE_VERY_VIVID,
    MODE_VERY_VIVID_LIGHT,
    MODE_VERY_VIVID_DARK,
    -> SATURATION_VERY_VIVID

    else -> 100
}

/**
 * The two colours that show a theme in the picker: its background and its main (accent) colour. Null for the theme from
 * the wallpaper, whose colours are not known until the system gives them.
 */
@Composable
fun themePreviewColors(mode: Int): Pair<Color, Color>? {
    if (mode == MODE_DYNAMIC) return null
    val systemDark = isSystemInDarkTheme()
    val dark =
        when {
            mode == MODE_MONO_DARK || isVividDark(mode) || mode == 2 -> true
            mode == MODE_MONO_LIGHT || isVividLight(mode) || mode == 1 -> false
            else -> systemDark
        }
    val scheme =
        when {
            isMonochromeMode(mode) -> if (dark) monoDarkScheme else monoLightScheme

            else -> {
                val saturation = saturationFor(mode)
                val base = (if (dark) darkScheme else lightScheme).withSaturation(saturation)
                if (dark && saturation > 100) base.withTrueBlack() else base
            }
        }
    return scheme.background to scheme.primary
}

/** True when [mode] is a saturated theme that is always light. */
fun isVividLight(mode: Int): Boolean = mode == MODE_VIVID_LIGHT || mode == MODE_VERY_VIVID_LIGHT

/** True when [mode] is a saturated theme that is always dark. */
fun isVividDark(mode: Int): Boolean = mode == MODE_VIVID_DARK || mode == MODE_VERY_VIVID_DARK

/** True when [mode] is one of the black-and-white themes. */
fun isMonochromeMode(mode: Int): Boolean = mode == MODE_MONO_LIGHT || mode == MODE_MONO_DARK

/** The stored theme preference's light and dark values, which equal AppCompatDelegate's MODE_NIGHT_NO and _YES. */
const val MODE_LIGHT = 1
const val MODE_DARK = 2

/**
 * Whether the stored theme preference [themePref] means dark; any other value follows the system. MT_SW: the
 * black-and-white and the saturated themes that are always light or always dark answer for themselves too.
 */
@Composable
@ReadOnlyComposable
fun isDarkTheme(themePref: Int): Boolean = when {
    themePref == MODE_LIGHT || themePref == MODE_MONO_LIGHT || isVividLight(themePref) -> false
    themePref == MODE_DARK || themePref == MODE_MONO_DARK || isVividDark(themePref) -> true
    else -> isSystemInDarkTheme()
}
