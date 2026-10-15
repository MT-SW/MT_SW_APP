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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.network_health_hero_no_data
import org.meshtastic.core.resources.network_health_hero_reporting
import org.meshtastic.core.resources.network_health_hero_subtitle
import org.meshtastic.core.resources.network_health_hero_title
import org.meshtastic.core.resources.network_health_info_intro
import org.meshtastic.core.resources.network_health_info_title
import org.meshtastic.core.resources.network_health_online
import org.meshtastic.core.resources.network_health_problems_only
import org.meshtastic.core.resources.okay
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.icon.BatteryEmpty
import org.meshtastic.core.ui.icon.ChannelUtilization
import org.meshtastic.core.ui.icon.DataUsage
import org.meshtastic.core.ui.icon.Favorite
import org.meshtastic.core.ui.icon.FilterAlt
import org.meshtastic.core.ui.icon.FilterAltOff
import org.meshtastic.core.ui.icon.KeyboardArrowDown
import org.meshtastic.core.ui.icon.KeyboardArrowUp
import org.meshtastic.core.ui.icon.Memory
import org.meshtastic.core.ui.icon.MeshHub
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.NotFavorite
import org.meshtastic.core.ui.icon.SignalCellular4Bar
import org.meshtastic.core.ui.icon.Temperature
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline

private const val RING_SIZE_DP = 96
private const val RING_STROKE_DP = 12
private const val RING_ANIMATION_MILLIS = 900
private const val FULL_CIRCLE = 360f
private const val RING_START_ANGLE = -90f
private const val PERCENT = 100
private const val ROW_TINT_ALPHA = 0.10f
private const val SPARKLINE_FILL_ALPHA = 0.35f
private const val SPARKLINE_STROKE_PX = 3f
private const val SPARKLINE_DOT_RADIUS_PX = 5f

// Range of the SNR bar and the neighbor-count bar: the bar is full at the top of its range.
private const val LEVEL_CHIP_RADIUS_DP = 8
private const val NOISE_BAR_MIN = -130f
private const val NOISE_BAR_MAX = -60f
private const val NEIGHBOR_BAR_MAX = 6f

/** The verdict levels that are shown in the summary, best to worst. */
private val RANKED_LEVELS = listOf(HealthLevel.GOOD, HealthLevel.MEDIUM, HealthLevel.BAD, HealthLevel.CRITICAL)

/** A small round colour marker. It has a thin outline so a white one stays visible on a light background. */
@Composable
internal fun HealthDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f), CircleShape),
    )
}

/** The icon of a metric tab. */
@Composable
internal fun NetworkHealthMetric.icon(): ImageVector = when (this) {
    NetworkHealthMetric.POWER -> MeshtasticIcons.BatteryEmpty
    NetworkHealthMetric.SIGNAL -> MeshtasticIcons.SignalCellular4Bar
    NetworkHealthMetric.ETHER -> MeshtasticIcons.ChannelUtilization
    NetworkHealthMetric.ENVIRONMENT -> MeshtasticIcons.Temperature
    NetworkHealthMetric.RESOURCES -> MeshtasticIcons.Memory
    NetworkHealthMetric.TRAFFIC -> MeshtasticIcons.DataUsage
    NetworkHealthMetric.NEIGHBORS -> MeshtasticIcons.MeshHub
}

/** Tab title: the metric icon, tinted with the worst verdict among all nodes in that tab, and its name. */
@Composable
internal fun NetworkHealthTabLabel(metric: NetworkHealthMetric, worst: HealthLevel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            imageVector = metric.icon(),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = worst.color(),
        )
        Text(text = metric.label)
    }
}

/** The big card at the top: a ring with the share of well-doing nodes, the count per verdict and who is online. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NetworkHealthHero(
    uiState: NetworkHealthUiState,
    onLevelClick: (HealthLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val counts = uiState.levelCounts
    val verdictTotal = RANKED_LEVELS.sumOf { counts[it] ?: 0 }
    val metric = uiState.activeMetric
    val rated = metric.hasVerdict()
    // Nodes without data are counted too (blue), so the ring and chips add up to all nodes. Tabs without limits only
    // tell "has data" from "no data".
    val shownLevels =
        when {
            !rated -> listOf(HealthLevel.GOOD, HealthLevel.UNKNOWN)
            else -> RANKED_LEVELS + HealthLevel.UNKNOWN
        }
    val good = counts[HealthLevel.GOOD] ?: 0
    val brush =
        Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), MaterialTheme.colorScheme.surfaceContainer),
        )

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier.background(brush).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!rated) {
                HealthRing(
                    segments = shownLevels.map { it to (counts[it] ?: 0) },
                    centerText = "${uiState.withDataCount}",
                )
            } else if (verdictTotal > 0) {
                HealthRing(
                    segments = shownLevels.map { it to (counts[it] ?: 0) },
                    centerText = "${good * PERCENT / verdictTotal}%",
                )
            } else {
                // A rated tab where no node has any data yet: the whole ring is "no data".
                HealthRing(segments = shownLevels.map { it to (counts[it] ?: 0) }, centerText = "—")
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(Res.string.network_health_hero_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                    if (rated && verdictTotal > 0) {
                        stringResource(Res.string.network_health_hero_subtitle, good, verdictTotal)
                    } else if (uiState.withDataCount > 0) {
                        stringResource(
                            Res.string.network_health_hero_reporting,
                            uiState.withDataCount,
                            uiState.totalCount,
                        )
                    } else {
                        stringResource(Res.string.network_health_hero_no_data)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HealthDot(color = MaterialTheme.colorScheme.StatusOnline)
                    Text(
                        text =
                        stringResource(Res.string.network_health_online, uiState.onlineCount, uiState.totalCount),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // A tap on a count lists only the nodes with that verdict; a second tap shows all again.
                    shownLevels.forEach { level ->
                        val selected = uiState.levelFilter == level
                        Row(
                            modifier =
                            Modifier.clip(RoundedCornerShape(LEVEL_CHIP_RADIUS_DP.dp))
                                .background(
                                    if (selected) level.color().copy(alpha = ROW_TINT_ALPHA * 2) else Color.Transparent,
                                )
                                .clickable { onLevelClick(level) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            HealthDot(color = level.color())
                            Text(
                                text = "${counts[level] ?: 0} ${level.labelFor(metric)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A donut split into one coloured arc per verdict, drawn in with a short animation. */
@Composable
private fun HealthRing(segments: List<Pair<HealthLevel, Int>>, centerText: String) {
    DonutChart(segments = segments.map { it.first.color() to it.second }, centerText = centerText)
}

/** A donut with one arc per (colour, count) pair, drawn in with a short animation. */
@Composable
internal fun DonutChart(
    segments: List<Pair<Color, Int>>,
    centerText: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = RING_SIZE_DP,
) {
    val total = segments.sumOf { it.second }
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { target = 1f }
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(RING_ANIMATION_MILLIS),
        label = "donut",
    )
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(modifier = modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = RING_STROKE_DP.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = FULL_CIRCLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (total > 0) {
                var start = RING_START_ANGLE
                segments.forEach { (color, count) ->
                    if (count > 0) {
                        val sweep = FULL_CIRCLE * count / total * progress
                        drawArc(
                            color = color,
                            startAngle = start,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke),
                        )
                        start += sweep
                    }
                }
            }
        }
        Text(text = centerText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

/** Sorting, filtering and "problems only" controls. The coloured icons light up when their option is on. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NetworkHealthControls(
    uiState: NetworkHealthUiState,
    onSortFieldChange: (NetworkHealthSortField) -> Unit,
    onToggleDirection: () -> Unit,
    onToggleFavoritesFirst: () -> Unit,
    onToggleHideWithoutData: () -> Unit,
    onToggleProblemsOnly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = MaterialTheme.colorScheme.StatusOnline
    val off = MaterialTheme.colorScheme.onSurfaceVariant
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        FilterChip(
            selected = uiState.sortField == NetworkHealthSortField.METRIC_VALUE,
            onClick = { onSortFieldChange(NetworkHealthSortField.METRIC_VALUE) },
            label = { Text("Wg wartości") },
        )
        FilterChip(
            selected = uiState.sortField == NetworkHealthSortField.LAST_HEARD,
            onClick = { onSortFieldChange(NetworkHealthSortField.LAST_HEARD) },
            label = { Text("Wg aktywności") },
        )
        FilterChip(
            selected = uiState.problemsOnly,
            onClick = onToggleProblemsOnly,
            label = { Text(stringResource(Res.string.network_health_problems_only)) },
            leadingIcon = {
                Icon(
                    imageVector = MeshtasticIcons.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                    tint = HealthLevel.BAD.color(),
                )
            },
        )
        IconButton(onClick = onToggleDirection) {
            Icon(
                imageVector =
                if (uiState.sortAscending) MeshtasticIcons.KeyboardArrowUp else MeshtasticIcons.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onToggleFavoritesFirst) {
            Icon(
                imageVector = if (uiState.favoritesFirst) MeshtasticIcons.Favorite else MeshtasticIcons.NotFavorite,
                contentDescription = null,
                tint = if (uiState.favoritesFirst) gold else off,
            )
        }
        IconButton(onClick = onToggleHideWithoutData) {
            Icon(
                imageVector = if (uiState.hideWithoutData) MeshtasticIcons.FilterAlt else MeshtasticIcons.FilterAltOff,
                contentDescription = null,
                tint = if (uiState.hideWithoutData) gold else off,
            )
        }
    }
}

/** One node: colour accent on the left, a round name badge, a filled trend line, the value and its verdict. */
@Composable
internal fun NodeHealthRow(
    node: NodeHealthInfo,
    metric: NetworkHealthMetric,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val level = metric.levelFor(node)
    val accent = level.color()
    val base = MaterialTheme.colorScheme.surfaceContainer
    val container = if (level == HealthLevel.UNKNOWN) base else accent.copy(alpha = ROW_TINT_ALPHA).compositeOver(base)
    val valueColor = if (level == HealthLevel.UNKNOWN) MaterialTheme.colorScheme.onSurface else accent
    // The history already holds the noise floor after the LNA correction.
    val shownValue = node.valueFor(metric)

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(accent))
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NameBadge(text = node.shortName.ifBlank { "?" }.take(NAME_BADGE_CHARS), color = accent)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (node.isFavorite) {
                                Icon(
                                    imageVector = MeshtasticIcons.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.StatusOnline,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = node.longName.ifBlank { node.shortName },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            HealthDot(
                                color =
                                if (node.isOnline) {
                                    MaterialTheme.colorScheme.StatusOnline
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                },
                            )
                            Text(
                                text =
                                buildString {
                                    append(if (node.isOnline) "Online" else "Offline")
                                    if (metric == NetworkHealthMetric.SIGNAL && node.snr != null) {
                                        append(" · SNR ${MetricFormatter.snr(node.snr)}")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    MetricSparkline(
                        values = node.metricHistory,
                        color = accent,
                        modifier = Modifier.size(width = 72.dp, height = 28.dp),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatMetricValue(metric, shownValue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = valueColor,
                        )
                        Text(
                            text = level.labelFor(metric),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                        )
                    }
                }
                metric.barFraction(node)?.let { LevelBar(fraction = it, color = accent) }
            }
        }
    }
}

private const val NAME_BADGE_CHARS = 4

/** Round badge with the node's short name, ringed in the verdict colour. */
@Composable
private fun NameBadge(text: String, color: Color) {
    Box(
        modifier =
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Thin bar that shows how full the value is (battery level, channel use, signal strength, neighbor count). */
@Composable
private fun LevelBar(fraction: Float, color: Color) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
    ) {
        Box(modifier = Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

/** How full the bar under a row is, or null when this tab has no bar. */
private fun NetworkHealthMetric.barFraction(node: NodeHealthInfo): Float? = when (this) {
    NetworkHealthMetric.POWER ->
        if (node.isPowered || (node.batteryLevel ?: 0) > PERCENT) {
            1f
        } else {
            node.batteryLevel?.let { it / PERCENT.toFloat() }
        }

    // Full bar = quiet band (-130 dBm), empty = very noisy; a node without a noise floor gets no bar.
    NetworkHealthMetric.SIGNAL ->
        node.noiseFloor?.let { ((NOISE_BAR_MAX - it) / (NOISE_BAR_MAX - NOISE_BAR_MIN)).coerceIn(0f, 1f) }
    NetworkHealthMetric.ETHER -> node.channelUtilization?.let { it / PERCENT }
    NetworkHealthMetric.NEIGHBORS -> node.neighborCount?.let { it / NEIGHBOR_BAR_MAX }
    NetworkHealthMetric.ENVIRONMENT,
    NetworkHealthMetric.RESOURCES,
    NetworkHealthMetric.TRAFFIC,
    -> null
}

/** Line chart of recent values with a soft fill below it and a dot on the latest point; nothing under 2 points. */
@Composable
internal fun MetricSparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val line = Path()
        var lastPoint = Offset.Zero
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - minV) / range) * size.height
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
            lastPoint = Offset(x, y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(listOf(color.copy(alpha = SPARKLINE_FILL_ALPHA), Color.Transparent)),
        )
        drawPath(path = line, color = color, style = Stroke(width = SPARKLINE_STROKE_PX))
        drawCircle(color = color, radius = SPARKLINE_DOT_RADIUS_PX, center = lastPoint)
    }
}

/** Explains the colours: the legend, and this tab's limits. */
@Composable
internal fun NetworkHealthInfoDialog(metric: NetworkHealthMetric, onDismiss: () -> Unit) {
    MeshtasticDialog(
        title = stringResource(Res.string.network_health_info_title),
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.network_health_info_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                (RANKED_LEVELS + HealthLevel.UNKNOWN).forEach { level ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HealthDot(color = level.color())
                        Text(text = stringResource(level.labelRes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(text = stringResource(metric.infoRes), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmTextRes = Res.string.okay,
        onConfirm = onDismiss,
        onDismiss = onDismiss,
    )
}
