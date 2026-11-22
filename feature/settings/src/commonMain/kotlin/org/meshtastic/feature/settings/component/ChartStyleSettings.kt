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
@file:Suppress("MagicNumber", "ModifierMissing")

package org.meshtastic.feature.settings.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.chart_style
import org.meshtastic.core.resources.chart_style_flat
import org.meshtastic.core.resources.chart_style_glass
import org.meshtastic.core.resources.chart_style_glow
import org.meshtastic.core.resources.chart_style_ribbon
import org.meshtastic.core.resources.chart_style_gradient
import org.meshtastic.core.resources.chart_style_health
import org.meshtastic.core.resources.chart_style_node
import org.meshtastic.core.resources.chart_style_outline
import org.meshtastic.core.resources.chart_style_preview_hint
import org.meshtastic.core.resources.chart_style_same
import org.meshtastic.core.resources.chart_style_summary
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.drawChartArc
import org.meshtastic.core.ui.component.drawChartBar

private val CHART_STYLES =
    listOf(
        UiOptions.CHART_STYLE_RIBBON,
        UiOptions.CHART_STYLE_GLASS,
        UiOptions.CHART_STYLE_GRADIENT,
        UiOptions.CHART_STYLE_OUTLINE,
        UiOptions.CHART_STYLE_GLOW,
        UiOptions.CHART_STYLE_FLAT,
    )

private val BAR_SHARES = listOf(0.45f, 0.8f, 0.6f, 1f)

/**
 * MT_SW: the style of the data charts. A preview of all styles on one screen (tap one to choose it for all charts),
 * then the general style and the own style of "Network health" and of the node details.
 */
@Composable
fun ChartStyleSettings(options: UiOptions, onChange: (String, Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val general = options[UiOptions.CHART_STYLE]
        Text(
            text = stringResource(Res.string.chart_style_preview_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (style in CHART_STYLES) {
                ChartStyleSample(
                    label = chartStyleName(style),
                    style = style,
                    selected = style == general,
                    onClick = { onChange(UiOptions.CHART_STYLE, style) },
                )
            }
        }
        val styleItems = CHART_STYLES.map { it to chartStyleName(it) }
        DropDownPreference(
            title = stringResource(Res.string.chart_style),
            enabled = true,
            items = styleItems,
            selectedItem = general,
            onItemSelected = { onChange(UiOptions.CHART_STYLE, it) },
            summary = stringResource(Res.string.chart_style_summary),
        )
        val scopeItems = listOf(UiOptions.CHART_STYLE_SAME to stringResource(Res.string.chart_style_same)) + styleItems
        DropDownPreference(
            title = stringResource(Res.string.chart_style_health),
            enabled = true,
            items = scopeItems,
            selectedItem = options[UiOptions.CHART_STYLE_HEALTH],
            onItemSelected = { onChange(UiOptions.CHART_STYLE_HEALTH, it) },
            summary = null,
        )
        DropDownPreference(
            title = stringResource(Res.string.chart_style_node),
            enabled = true,
            items = scopeItems,
            selectedItem = options[UiOptions.CHART_STYLE_NODE],
            onItemSelected = { onChange(UiOptions.CHART_STYLE_NODE, it) },
            summary = null,
        )
    }
}

@Composable
private fun chartStyleName(style: Int): String = when (style) {
    UiOptions.CHART_STYLE_GLASS -> stringResource(Res.string.chart_style_glass)
    UiOptions.CHART_STYLE_GRADIENT -> stringResource(Res.string.chart_style_gradient)
    UiOptions.CHART_STYLE_OUTLINE -> stringResource(Res.string.chart_style_outline)
    UiOptions.CHART_STYLE_GLOW -> stringResource(Res.string.chart_style_glow)
    UiOptions.CHART_STYLE_RIBBON -> stringResource(Res.string.chart_style_ribbon)
    else -> stringResource(Res.string.chart_style_flat)
}

/** One row of the preview: the name of a style with sample bars and a half-round gauge drawn in that style. */
@Composable
private fun ChartStyleSample(label: String, style: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val border = if (selected) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant)
    val palette = listOf(colors.primary, colors.tertiary, colors.secondary, colors.error)
    val track = colors.surfaceVariant
    Row(
        modifier =
        Modifier.fillMaxWidth().clip(shape).border(border, shape).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Canvas(modifier = Modifier.width(140.dp).height(40.dp)) {
            val barWidth = 14.dp.toPx()
            val gap = 6.dp.toPx()
            BAR_SHARES.forEachIndexed { index, share ->
                val h = size.height * share
                drawChartBar(
                    style = style,
                    color = palette[index],
                    topLeft = Offset(index * (barWidth + gap), size.height - h),
                    size = Size(barWidth, h),
                    corner = 6.dp.toPx(),
                )
            }
            val stroke = 8.dp.toPx()
            val radius = 18.dp.toPx()
            val center = Offset(size.width - radius - stroke / 2, size.height - 2.dp.toPx())
            drawChartArc(style, track, 180f, 180f, center, radius, stroke)
            drawChartArc(style, palette[0], 180f, 60f, center, radius, stroke)
            drawChartArc(style, palette[1], 240f, 60f, center, radius, stroke)
            drawChartArc(style, palette[3], 300f, 60f, center, radius, stroke)
        }
    }
}
