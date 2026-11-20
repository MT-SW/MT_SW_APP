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
@file:Suppress(
    "CompositionLocalAllowlist",
    "ContentSlotReused",
    "MagicNumber",
    "MissingReadOnlyComposable",
    "TooManyFunctions",
)

package org.meshtastic.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.ElementStyle
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions

private const val PERCENT = 100f
private const val MIN_ALPHA = 0.12f
private const val GLASS_TINT_SHARE = 0.85f
private const val DEPTH_SHADOW_BASE_DP = 2f
private const val DEPTH_SHADOW_RANGE_DP = 10f
private const val LIGHT_TOP_ALPHA = 0.18f
private const val SHADE_BOTTOM_ALPHA = 0.08f
private const val RIM_TOP_ALPHA = 0.5f
private const val RIM_BOTTOM_ALPHA = 0.06f
private const val FLAT_SHADOW_DP = 14f
private const val GLASS_FALLBACK_MIN_ALPHA = 0.88f

/** The least density of the toolbars lying on the map: they stay clearly more see-through than other glass. */
const val MAP_GLASS_MIN_ALPHA = 0.6f
private const val READABLE_LUMINANCE = 0.5f
private val READABLE_DARK = Color(0xFF1B1B1F)
private const val GLASS_TINT_LIGHT = 1
private const val GLASS_TINT_DARK = 2
private const val GLASS_TINT_ACCENT = 3
private const val GLASS_TINT_MIX = 0.6f

/** The share of the element's own colour that is left when [strength] percent of it is made see-through. */
private fun seeThroughAlpha(strength: Int): Float = (1f - strength / PERCENT).coerceIn(MIN_ALPHA, 1f)

/**
 * Draws this element with [ElementLook.DEPTH] when the person chose it for the group named [group] (one of the
 * `UiOptions.GROUP_` names): a soft shadow, light along the upper edge, a little shade below and a thin bright rim.
 * Every other look leaves the element exactly as it is.
 */
@Composable
fun Modifier.elementLook(group: String, shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val options = rememberUiOptions()
    val style = options.element(group)
    return when (style.look) {
        ElementLook.DEPTH -> depthLook(style.strength, shape, options.rimDp())

        // MT_SW: glass casts a soft shadow of its own ("Shadow under glass"); on a dark theme a black shadow cannot
        // be seen, so there it is a soft light glow that lifts the element off the background.
        ElementLook.GLASS ->
            if (group in CARD_GROUPS) {
                cardShadow(options.glassShadowDp(), shape, isDarkSurface())
            } else {
                softShadow(options.glassShadowDp(), shape, isDarkSurface())
            }

        // The cards are see-through ("transparent") by default, and glass-looking as well: they cast the same shadow.
        ElementLook.TRANSPARENT ->
            if (group in CARD_GROUPS) cardShadow(options.glassShadowDp(), shape, isDarkSurface()) else this

        else -> this
    }
}

/** Whether the current theme is dark, judged by its surface colour. */
@Composable internal fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < READABLE_LUMINANCE

/** How much stronger the sheen of glass is drawn: a dark theme needs more of it to show any depth. */
@Composable internal fun glassSheenBoost(): Float = if (isDarkSurface()) DARK_SHEEN_BOOST else LIGHT_SHEEN_BOOST

private const val DARK_SHEEN_BOOST = 1.8f
private const val LIGHT_SHEEN_BOOST = 1.3f
private const val DARK_GLOW_REACH = 1.3f

/** The groups of cards: they cast the stronger hand-drawn [cardShadow]. */
internal val CARD_GROUPS =
    setOf(
        UiOptions.GROUP_CARDS,
        UiOptions.GROUP_DETAILS,
        UiOptions.GROUP_SETTINGS,
        UiOptions.GROUP_HEALTH,
        UiOptions.GROUP_ASSISTANT,
    )

/** A soft shadow of [dp] under [shape]: black on a light theme, a light glow on a dark one. No shadow for 0. */
internal fun Modifier.softShadow(dp: Float, shape: Shape, dark: Boolean): Modifier = when {
    dp <= 0f -> this

    dark ->
        shadow(
            elevation = (dp * DARK_GLOW_REACH).dp,
            shape = shape,
            clip = false,
            ambientColor = Color.White,
            spotColor = Color.White,
        )

    else -> shadow(elevation = dp.dp, shape = shape, clip = false)
}

/**
 * The shadow of a card: [dp] under [shape], black on a light theme, a light glow on a dark one (a black shadow cannot
 * be seen there). No shadow for 0.
 *
 * It is drawn by hand as a stack of fading outlines, not with [shadow]: the platform shadow is very faint on desktop
 * and on translucent panes, which made glass look flat. Nothing is drawn inside the shape itself, so see-through glass
 * stays clean.
 */
internal fun Modifier.cardShadow(dp: Float, shape: Shape, dark: Boolean): Modifier =
    if (dp <= 0f) {
        this
    } else {
        drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val radius =
                when (outline) {
                    is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
                    else -> 0f
                }
            val reach = dp.dp.toPx() * CARD_SHADOW_REACH * (if (dark) DARK_GLOW_REACH else 1f)
            val shift = reach * SHADOW_DROP
            val tint = if (dark) Color.White else Color.Black
            val layerAlpha = (if (dark) DARK_GLOW_ALPHA else SHADOW_ALPHA) / SHADOW_LAYERS
            val hole =
                Path().apply {
                    when (outline) {
                        is Outline.Rectangle -> addRect(outline.rect)
                        is Outline.Rounded -> addRoundRect(outline.roundRect)
                        is Outline.Generic -> addPath(outline.path)
                    }
                }
            clipPath(hole, ClipOp.Difference) {
                for (i in SHADOW_LAYERS downTo 1) {
                    val grow = reach * i / SHADOW_LAYERS
                    drawRoundRect(
                        color = tint.copy(alpha = layerAlpha),
                        topLeft = Offset(-grow, -grow + shift),
                        size = Size(size.width + 2 * grow, size.height + 2 * grow),
                        cornerRadius = CornerRadius(radius + grow, radius + grow),
                    )
                }
            }
        }
    }

private const val SHADOW_LAYERS = 10
private const val CARD_SHADOW_REACH = 0.7f
private const val SHADOW_ALPHA = 0.4f
private const val DARK_GLOW_ALPHA = 0.26f
private const val SHADOW_DROP = 0.35f

/** The thickness of the rim chosen in the settings, in dp. */
internal fun UiOptions.rimDp(): Float =
    UiOptions.RIM_STEPS_DP[get(UiOptions.RIM).coerceIn(0, UiOptions.RIM_STEPS_DP.lastIndex)]

/** The depth of the shadow under glass chosen in the settings, in dp. */
internal fun UiOptions.glassShadowDp(): Float =
    UiOptions.GLASS_SHADOW_STEPS_DP[get(UiOptions.GLASS_SHADOW).coerceIn(0, UiOptions.GLASS_SHADOW_STEPS_DP.lastIndex)]

/** The colour of the glass: the given [base] colour changed by the "colour of the glass" option. */
@Composable
internal fun UiOptions.glassBase(base: Color): Color = when (get(UiOptions.GLASS_TINT)) {
    GLASS_TINT_LIGHT -> lerp(base, Color.White, GLASS_TINT_MIX)
    GLASS_TINT_DARK -> lerp(base, Color.Black, GLASS_TINT_MIX)
    GLASS_TINT_ACCENT -> lerp(base, MaterialTheme.colorScheme.primary, GLASS_TINT_MIX)
    else -> base
}

/**
 * The colour for icons and letters placed on an element of [group] that follows the chosen look: the "colour of the
 * glass" option can make the glass nearly white or black, so the usual text colour of the theme could vanish on it;
 * this one is always dark on light glass and white on dark glass.
 */
@Composable
fun glassContentColor(group: String, base: Color = MaterialTheme.colorScheme.surfaceContainerHigh): Color {
    val options = rememberUiOptions()
    if (options.element(group).look != ElementLook.GLASS) return MaterialTheme.colorScheme.contentColorFor(base)
    val tinted = options.glassBase(base)
    return if (tinted.luminance() > READABLE_LUMINANCE) READABLE_DARK else Color.White
}

/**
 * The room a list must leave at its sides so the shadow of the depth look is not cut off: a shadow is drawn outside its
 * card, and a list clips everything that leaves it, so cards touching the edge of a list showed a flat, cut-off side.
 * Zero for the other looks (they draw nothing outside the card).
 */
@Composable
fun rememberShadowInset(group: String = LocalCardGroup.current): Dp {
    val options = rememberUiOptions()
    val style = options.element(group)
    val depth =
        when (style.look) {
            ElementLook.DEPTH -> {
                val s = style.strength.coerceIn(0, PERCENT.toInt()) / PERCENT
                ((DEPTH_SHADOW_BASE_DP + DEPTH_SHADOW_RANGE_DP * s) * SHADOW_REACH).dp
            }

            // MT_SW: glass has its own soft shadow, so its lists keep room for it as well.
            ElementLook.GLASS -> (options.glassShadowDp() * DARK_GLOW_REACH * SHADOW_REACH).dp

            ElementLook.TRANSPARENT ->
                if (group in CARD_GROUPS) {
                    (options.glassShadowDp() * CARD_SHADOW_REACH * DARK_GLOW_REACH * SHADOW_REACH).dp
                } else {
                    0.dp
                }

            else -> 0.dp
        }
    // On a computer or a tablet every look keeps at least [SHADOW_ROOM_DP]: the glow of the outlined buttons and the
    // soft shadows of the other looks are drawn outside their element too.
    val floor = if (isAdaptiveTwoPaneSplit()) SHADOW_ROOM_DP.dp else 0.dp
    return maxOf(depth, floor)
}

/** [this] with [inset] added at the left and the right, so a list keeps the shadow of its cards inside its bounds. */
@Composable
fun PaddingValues.withShadowInset(inset: Dp): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction) + inset,
        top = calculateTopPadding(),
        end = calculateEndPadding(direction) + inset,
        bottom = calculateBottomPadding(),
    )
}

private const val SHADOW_REACH = 0.8f

/**
 * The room, in dp, that a pane gives the shadows and glow of the elements at its sides. The pane is made wider by this
 * much at each side out of the empty margin and gap around it, and its content is inset by the same amount, so the
 * elements keep their size and place while their shadows have room to be drawn inside the pane.
 */
const val SHADOW_ROOM_DP = 14

/** The depth look of [elementLook], with its strength 0-100. */
fun Modifier.depthLook(strength: Int, shape: Shape, rimDp: Float = 1f): Modifier {
    val s = strength.coerceIn(0, PERCENT.toInt()) / PERCENT
    return this.shadow(elevation = (DEPTH_SHADOW_BASE_DP + DEPTH_SHADOW_RANGE_DP * s).dp, shape = shape, clip = false)
        .drawWithContent {
            drawContent()
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                brush =
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = LIGHT_TOP_ALPHA * s),
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = SHADE_BOTTOM_ALPHA * s),
                ),
            )
            drawOutline(
                outline = outline,
                brush =
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = RIM_TOP_ALPHA * s),
                        Color.White.copy(alpha = RIM_BOTTOM_ALPHA * s),
                    ),
                ),
                style = Stroke(width = rimDp.dp.toPx()),
            )
        }
}

/**
 * A surface drawn in the look the person chose for [group]: plain, with depth, see-through, or (for a surface that
 * floats above the content, with a [hazeState] to blur) frosted glass.
 *
 * @param floating adds the shadow a floating surface has in the plain look.
 */
@Composable
fun StyledSurface(
    group: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    floating: Boolean = false,
    content: @Composable () -> Unit,
) {
    val options = rememberUiOptions()
    StyledSurface(
        options.element(group),
        shape,
        modifier,
        hazeState,
        options.glassBase(baseColor),
        floating,
        content = content,
    )
}

/** The same as above for an already known [style]. */
@Composable
fun StyledSurface(
    style: ElementStyle,
    shape: Shape,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    floating: Boolean = false,
    rimDp: Float? = null,
    fallbackMinAlpha: Float = GLASS_FALLBACK_MIN_ALPHA,
    /** No darker shade at the bottom of the glass: the sheen just fades out (a small, low pane such as the bar). */
    flatBottom: Boolean = false,
    content: @Composable () -> Unit,
) {
    val options = rememberUiOptions()
    val rimWidth = rimDp ?: options.rimDp()
    // MT_SW: glass casts a soft shadow of its own (the "Shadow under glass" option); a floating surface keeps at least
    // the shadow it has in the plain look.
    val glassShadow = if (style.look == ElementLook.GLASS) options.glassShadowDp() else 0f
    val shadowDp = maxOf(glassShadow, if (floating) FLAT_SHADOW_DP else 0f)
    val dark = isDarkSurface()
    val boost = glassSheenBoost()
    when {
        style.look == ElementLook.GLASS && hazeState != null -> {
            val tint = baseColor.copy(alpha = seeThroughAlpha(style.strength) * GLASS_TINT_SHARE)
            val blur = HazeBlurStyle {
                blurRadius(style.blur.coerceIn(1, MAX_BLUR_DP).dp)
                colorEffects(listOf(HazeColorEffect.tint(tint)))
            }
            Box(
                modifier =
                modifier
                    .softShadow(shadowDp, shape, dark)
                    .clip(shape)
                    .hazeBlur(input = HazeInput.Sources(hazeState), style = blur)
                    .glassEdge(shape, rimWidth, flatBottom, boost),
            ) {
                content()
            }
        }

        style.look == ElementLook.GLASS || style.look == ElementLook.TRANSPARENT ->
            Surface(
                modifier =
                modifier
                    .let { if (style.look == ElementLook.GLASS) it.softShadow(shadowDp, shape, dark) else it }
                    .glassEdge(shape, rimWidth, flatBottom, boost),
                shape = shape,
                // Glass that cannot blur (over a map, on an old Android) imitates a frosted pane: it stays dense enough
                // to read what is on it.
                color =
                baseColor.copy(
                    alpha =
                    if (style.look == ElementLook.GLASS) {
                        maxOf(seeThroughAlpha(style.strength), fallbackMinAlpha)
                    } else {
                        seeThroughAlpha(style.strength)
                    },
                ),
                shadowElevation = if (style.look != ElementLook.GLASS && floating) FLAT_SHADOW_DP.dp else 0.dp,
                content = content,
            )

        style.look == ElementLook.DEPTH ->
            Box(modifier = modifier.depthLook(style.strength, shape).clip(shape).background(baseColor)) { content() }

        else ->
            Surface(
                modifier = modifier,
                shape = shape,
                color = baseColor,
                shadowElevation = if (floating) FLAT_SHADOW_DP.dp else 0.dp,
                content = content,
            )
    }
}

/** The biggest blur the settings offer, in dp. */
const val MAX_BLUR_DP = 60

/**
 * The group of looks that the cards ([StyledCard], [TitledCard] and the like) of the current screen follow. Screens
 * with their own group of looks (node details, the assistant, network health, settings) provide it; everywhere else it
 * is the general "cards" group.
 */
val LocalCardGroup = staticCompositionLocalOf { UiOptions.GROUP_CARDS }

/** The colour of a card of [group]: [base], made see-through when the group is transparent or glass. */
@Composable
fun cardContainerColor(base: Color, group: String = LocalCardGroup.current): Color {
    val options = rememberUiOptions()
    val style = options.element(group)
    return when (style.look) {
        ElementLook.GLASS -> options.glassBase(base).copy(alpha = seeThroughAlpha(style.strength))
        ElementLook.TRANSPARENT -> base.copy(alpha = seeThroughAlpha(style.strength))
        else -> base
    }
}

/**
 * A card in the look chosen for its group (see [LocalCardGroup]): plain, with depth (shadow, light along the upper
 * edge, a thin rim) or see-through with a thin rim.
 */
@Composable
fun StyledCard(
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    shape: Shape = MaterialTheme.shapes.medium,
    group: String = LocalCardGroup.current,
    content: @Composable ColumnScope.() -> Unit,
) {
    val options = rememberUiOptions()
    val style = options.element(group)
    val seeThrough = style.look == ElementLook.TRANSPARENT || style.look == ElementLook.GLASS
    val look = modifier.elementLook(group, shape)
    Card(
        modifier = if (seeThrough) look.glassEdge(shape, options.rimDp(), sheenBoost = glassSheenBoost()) else look,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor(baseColor, group)),
        content = content,
    )
}

private const val SHEEN_TOP_ALPHA = 0.22f
private const val SHEEN_MID_STOP = 0.45f
private const val SHEEN_BOTTOM_ALPHA = 0.10f
private const val EDGE_LIGHT_ALPHA = 0.8f
private const val EDGE_MID_ALPHA = 0.1f
private const val EDGE_FAR_ALPHA = 0.4f
private const val EDGE_INNER_SHARE = 0.5f

/**
 * The edge of a glass surface, like the glass of iPhone: a soft sheen across the upper part (light falling from the
 * top), a faint shade at the bottom, and a thin bright edge that is strongest on the upper left and lower right, with a
 * second finer line just inside it, so the glass seems to have thickness.
 */
internal fun Modifier.glassEdge(
    shape: Shape,
    rimDp: Float,
    flatBottom: Boolean = false,
    sheenBoost: Float = 1f,
): Modifier = drawWithContent {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(
        outline = outline,
        brush =
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = (SHEEN_TOP_ALPHA * sheenBoost).coerceAtMost(1f)),
            SHEEN_MID_STOP to Color.Transparent,
            1f to
                if (flatBottom) {
                    Color.Transparent
                } else {
                    Color.Black.copy(alpha = (SHEEN_BOTTOM_ALPHA * sheenBoost).coerceAtMost(1f))
                },
        ),
    )
    drawContent()
    if (rimDp > 0f) {
        val width = rimDp.dp.toPx()
        val edge =
            Brush.linearGradient(
                colors =
                listOf(
                    Color.White.copy(alpha = EDGE_LIGHT_ALPHA),
                    Color.White.copy(alpha = EDGE_MID_ALPHA),
                    Color.White.copy(alpha = EDGE_FAR_ALPHA),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        drawOutline(outline = outline, brush = edge, style = Stroke(width = width))
        inset(width) {
            val inner = shape.createOutline(this.size, layoutDirection, this)
            drawOutline(
                outline = inner,
                brush = edge,
                alpha = EDGE_INNER_SHARE,
                style = Stroke(width = width * EDGE_INNER_SHARE),
            )
        }
    }
}
