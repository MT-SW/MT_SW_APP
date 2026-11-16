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
@file:Suppress("TooManyFunctions")

package org.meshtastic.feature.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** A node's chip as on the map: its colour with its short name on it. Nothing is drawn without a short name. */
@Composable
internal fun NodeChip(shortName: String, color: Int, textColor: Int) {
    if (shortName.isEmpty()) return
    Text(
        text = shortName,
        maxLines = 1,
        style = TextStyle(color = ColorProvider(Color(textColor)), fontSize = 12.sp, fontWeight = FontWeight.Bold),
        modifier =
        GlanceModifier.background(ColorProvider(Color(color)))
            .cornerRadius(CHIP_RADIUS)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

private val CHIP_RADIUS = 8.dp

/** The MT_SW mark, in its own colours (the fork's logo is not a single-colour shape, so it is not tinted). */
@Composable
internal fun WidgetMark(size: Dp) {
    Image(
        provider = ImageProvider(R.drawable.widget_app_icon),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
    )
}

/**
 * The brand header both widgets carry from two cells tall up: the mark and the widget's name, with an optional trailing
 * action. One-row sizes have no header; they lead their first row with the mark instead.
 */
@Composable
internal fun WidgetHeader(title: String, compact: Boolean, action: (@Composable () -> Unit)? = null) {
    // Padding rather than a fixed height, so a large font scale grows the header instead of clipping the title.
    val padding =
        when {
            action != null -> 0.dp
            compact -> 4.dp
            else -> 6.dp
        }
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetMark(if (compact) 16.dp else 20.dp)
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = title,
            maxLines = 1,
            style =
            TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = if (compact) WidgetSizes.Type.compactTitle else WidgetSizes.Type.title,
                fontWeight = FontWeight.Medium,
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
        action?.invoke()
    }
}

/**
 * The node chip with the battery as the widget's hero value. With a [trailing] pair the hero sits beside the chip and
 * the pair takes the right edge; without one the hero takes the right edge. The hero carries no visible label, so
 * [heroDescription] is what a screen reader announces for it.
 */
@Composable
internal fun IdentityRow(
    chip: @Composable () -> Unit,
    hero: String?,
    heroIsWord: Boolean,
    heroDescription: String?,
    isStale: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chip()
        if (trailing != null) {
            Spacer(GlanceModifier.width(8.dp))
            hero?.let { HeroText(it, heroIsWord, heroDescription, isStale) }
            Spacer(GlanceModifier.defaultWeight())
            trailing()
        } else {
            Spacer(GlanceModifier.defaultWeight())
            hero?.let { HeroText(it, heroIsWord, heroDescription, isStale) }
        }
    }
}

@Composable
private fun HeroText(text: String, isWord: Boolean, description: String?, isStale: Boolean) {
    Text(
        text = text,
        maxLines = 1,
        style =
        TextStyle(
            color = valueColor(isStale),
            fontSize = if (isWord) WidgetSizes.Type.heroWord else WidgetSizes.Type.hero,
            fontWeight = FontWeight.Medium,
        ),
        modifier = description?.let { GlanceModifier.semantics { contentDescription = it } } ?: GlanceModifier,
    )
}

/** One labeled reading, the unit a [StatGrid] lays out. */
internal data class Reading(val label: String, val value: String)

/** A label on the left and its value on the right, the one reading row both widgets use. */
@Composable
internal fun StatRow(
    label: String,
    value: String,
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth(),
    isStale: Boolean = false,
) {
    Row(modifier = modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = WidgetSizes.Type.label),
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(GlanceModifier.width(4.dp))
        ValueText(value, isStale)
    }
}

/**
 * [readings] as [StatRow]s, up to [columns] to a line, in reading order across then down. A short last line keeps its
 * cells the width of the others. Readings that all fit in one column of [lines] take one, so a tall cell fills with
 * lines rather than with gaps between sections, and when they still leave lines over, the lines spread across the
 * height [modifier] gives the grid rather than leaving a block under them.
 */
@Composable
internal fun StatGrid(
    readings: List<Reading>,
    columns: Int,
    lines: Int,
    modifier: GlanceModifier = GlanceModifier,
    isStale: Boolean = false,
) {
    val perLine = if (readings.size <= lines) 1 else columns
    val chunks = readings.chunked(perLine)
    val spread = chunks.size < lines
    Column(modifier = modifier.fillMaxWidth()) {
        chunks.forEach { line ->
            Row(
                modifier = if (spread) GlanceModifier.fillMaxWidth().defaultWeight() else GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(perLine) { index ->
                    if (index > 0) Spacer(GlanceModifier.width(GRID_GUTTER))
                    val reading = line.getOrNull(index)
                    if (reading == null) {
                        Spacer(GlanceModifier.defaultWeight())
                    } else {
                        StatRow(reading.label, reading.value, GlanceModifier.defaultWeight(), isStale)
                    }
                }
            }
        }
    }
}

private val GRID_GUTTER = 12.dp

/**
 * A [StatRow] over a 0..1 bar, for utilization. With [thin] (the "Thin progress bars" look) the bar is two thin lines,
 * empty inside, like the bars in the app; otherwise it is the old filled bar.
 */
@Composable
internal fun BarRow(
    label: String,
    value: String,
    progress: Float,
    modifier: GlanceModifier = GlanceModifier,
    thin: Boolean = true,
) {
    Column(modifier = modifier) {
        StatRow(label, value)
        Spacer(GlanceModifier.height(2.dp))
        if (thin) {
            OutlinedBar(progress)
        } else {
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.surfaceVariant,
            )
        }
    }
}

private val OUTLINED_BAR_HEIGHT = 8.dp
private const val OUTLINED_BAR_WIDTH_DP = 160
private const val OUTLINED_BAR_STROKE_DP = 1.2f
private const val OUTLINED_BAR_GAP_DP = 2.2f
private const val OUTLINED_BAR_TRACK_ALPHA = 0.55f

/** A grey capsule outline with the theme-coloured outlined progress inside it (Glance cannot draw a hollow bar). */
@Composable
private fun OutlinedBar(progress: Float) {
    val context = LocalContext.current
    val track = GlanceTheme.colors.outline.getColor(context).copy(alpha = OUTLINED_BAR_TRACK_ALPHA).toArgb()
    val fill = GlanceTheme.colors.primary.getColor(context).toArgb()
    val bitmap = remember(progress, track, fill) { outlinedBarBitmap(context.resources.displayMetrics.density, progress, track, fill) }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = GlanceModifier.fillMaxWidth().height(OUTLINED_BAR_HEIGHT),
    )
}

private fun outlinedBarBitmap(density: Float, progress: Float, trackColor: Int, fillColor: Int): Bitmap {
    val width = (OUTLINED_BAR_WIDTH_DP * density).toInt().coerceAtLeast(1)
    val height = (OUTLINED_BAR_HEIGHT.value * density).toInt().coerceAtLeast(1)
    val stroke = OUTLINED_BAR_STROKE_DP * density
    val gap = OUTLINED_BAR_GAP_DP * density
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
    }
    val half = stroke / 2f
    val outer = RectF(half, half, width - half, height - half)
    paint.color = trackColor
    canvas.drawRoundRect(outer, outer.height() / 2f, outer.height() / 2f, paint)
    val value = progress.coerceIn(0f, 1f)
    if (value > 0f) {
        val inset = half + gap
        val inner = RectF(inset, inset, width - inset, height - inset)
        val minWidth = inner.height()
        val end = inner.left + (inner.width() * value).coerceAtLeast(minWidth)
        val piece = RectF(inner.left, inner.top, end.coerceAtMost(inner.right), inner.bottom)
        paint.color = fillColor
        canvas.drawRoundRect(piece, piece.height() / 2f, piece.height() / 2f, paint)
    }
    return bitmap
}

/**
 * An inline label and value that size to their text, for rows that hold two readings; [label] null shows the value
 * alone.
 */
@Composable
internal fun PairText(label: String?, value: String, isStale: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (label != null) {
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = WidgetSizes.Type.label),
            )
            Spacer(GlanceModifier.width(4.dp))
        }
        ValueText(value, isStale)
    }
}

@Composable
internal fun ValueText(value: String, isStale: Boolean = false, contentDescription: String? = null) {
    Text(
        text = value,
        maxLines = 1,
        style =
        TextStyle(color = valueColor(isStale), fontSize = WidgetSizes.Type.value, fontWeight = FontWeight.Medium),
        modifier =
        contentDescription?.let { description ->
            GlanceModifier.semantics { this.contentDescription = description }
        } ?: GlanceModifier,
    )
}

/** A centered line of small print, such as when the readings were last updated. */
@Composable
internal fun Footnote(text: String) {
    Text(
        text = text,
        maxLines = 1,
        style =
        TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = WidgetSizes.Type.footnote,
            textAlign = TextAlign.Center,
        ),
        modifier = GlanceModifier.fillMaxWidth(),
    )
}

/**
 * The body when there is nothing to read: disconnected, connecting, sleeping or an unknown node. At one row it sits
 * beside the mark, since that size has no header.
 */
@Composable
internal fun StatusBody(text: String, connecting: Boolean, oneRow: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (oneRow) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        if (oneRow) {
            WidgetMark(16.dp)
            Spacer(GlanceModifier.width(8.dp))
        }
        if (connecting) {
            CircularProgressIndicator(modifier = GlanceModifier.size(16.dp))
            Spacer(GlanceModifier.width(8.dp))
        }
        Text(
            text = text,
            maxLines = 2,
            style =
            TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = WidgetSizes.Type.value,
                fontWeight = FontWeight.Medium,
                textAlign = if (oneRow) TextAlign.Start else TextAlign.Center,
            ),
        )
    }
}
