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

package org.meshtastic.feature.node.health

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline

private const val GROW_MILLIS = 800
private const val MIN_SEGMENT_WEIGHT = 0.0001f
private const val HEADER_ICON_ALPHA = 0.18f
private const val TRACK_ALPHA = 0.10f
private const val CHART_HEIGHT_DP = 96

/** The verdicts in the order used by the distribution counts: good, fair, poor, very poor, no data. */
private val DISTRIBUTION_LEVELS =
    listOf(HealthLevel.GOOD, HealthLevel.MEDIUM, HealthLevel.BAD, HealthLevel.CRITICAL, HealthLevel.UNKNOWN)

/** Goes from 0 to 1 shortly after the screen shows, so charts grow in. */
@Composable
private fun rememberGrow(): Float {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { target = 1f }
    val value by animateFloatAsState(target, tween(GROW_MILLIS), label = "grow")
    return value
}

/** A card with a coloured icon and a title; the chart or list goes inside. */
@Composable
internal fun SummarySection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.StatusOnline,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier =
                    Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = HEADER_ICON_ALPHA)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

/** A square-ish tile with a big number in the verdict colour. */
@Composable
internal fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One wide bar split into good / fair / poor / very poor / no data parts, with a legend underneath. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DistributionBar(counts: List<Int>, modifier: Modifier = Modifier) {
    val total = counts.sum()
    val grow = rememberGrow()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA)),
        ) {
            if (total > 0) {
                DISTRIBUTION_LEVELS.forEachIndexed { index, level ->
                    val count = counts.getOrElse(index) { 0 }
                    if (count > 0) {
                        Box(
                            modifier =
                            Modifier
                                .weight((count.toFloat() * grow).coerceAtLeast(MIN_SEGMENT_WEIGHT))
                                .fillMaxHeight()
                                .background(level.color()),
                        )
                    }
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DISTRIBUTION_LEVELS.forEachIndexed { index, level ->
                // "No data" is listed only when some node has none.
                if (level == HealthLevel.UNKNOWN && counts.getOrElse(index) { 0 } == 0) return@forEachIndexed
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    HealthDot(color = level.color())
                    Text(
                        text = "${counts.getOrElse(index) { 0 }} ${levelWord(level)}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

private fun levelWord(level: HealthLevel): String = when (level) {
    HealthLevel.GOOD -> "dobrze"
    HealthLevel.MEDIUM -> "średnio"
    HealthLevel.BAD -> "źle"
    HealthLevel.CRITICAL -> "bardzo źle"
    HealthLevel.UNKNOWN -> "brak danych"
}

/** Horizontal bars, one per entry; the longest entry fills the width. */
@Composable
internal fun BarList(bars: List<ReportBar>, color: Color, modifier: Modifier = Modifier) {
    val max = bars.maxOfOrNull { it.value } ?: 0f
    val grow = rememberGrow()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bars.forEachIndexed { index, bar ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${medal(index)}${bar.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = bar.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                ProgressTrack(fraction = if (max > 0f) bar.value / max * grow else 0f, color = color)
            }
        }
    }
}

/** 1st, 2nd, 3rd place marks; nothing after that. */
private fun medal(index: Int): String = when (index) {
    0 -> "1. "
    1 -> "2. "
    2 -> "3. "
    else -> ""
}

/** A thin rounded bar filled to [fraction] (0 to 1). */
@Composable
internal fun ProgressTrack(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Int = 8) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA)),
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color))),
        )
    }
}

/** A labelled gauge for a percentage, coloured by its verdict. */
@Composable
internal fun GaugeRow(label: String, percentText: String, fraction: Float, color: Color) {
    val grow = rememberGrow()
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = percentText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        ProgressTrack(fraction = fraction * grow, color = color, height = 10)
    }
}

/** Vertical bars with a number on top and a short label below; the last bar is highlighted. */
@Composable
internal fun ColumnChart(values: List<Int>, labels: List<String>, color: Color, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val grow = rememberGrow()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val isLast = index == values.lastIndex
            val tint = if (isLast) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                )
                Box(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .height((CHART_HEIGHT_DP * value / max * grow).dp.coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.5f)))),
                )
                Text(
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
