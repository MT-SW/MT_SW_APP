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
@file:OptIn(ExperimentalLayoutApi::class)

package org.meshtastic.feature.node.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.ui.component.InfoButton
import androidx.compose.ui.Alignment
import org.koin.compose.koinInject
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.availability_empty
import org.meshtastic.core.resources.availability_hint
import org.meshtastic.core.resources.availability_longest_silence
import org.meshtastic.core.resources.availability_now
import org.meshtastic.core.resources.availability_summary
import org.meshtastic.core.resources.availability_title
import org.meshtastic.core.resources.mt_sw_other_availability_body
import org.meshtastic.core.resources.formatDuration
import kotlin.time.DurationUnit

/**
 * The availability history of one node: a bar per time slot, filled when a packet from the node was recorded in it
 * and taller the more packets there were. Built from the recorded packets, so it reaches back only as far as the log
 * retention allows. The range is chosen with chips, from a day to a month.
 */
@Composable
fun AvailabilityHistorySection(nodeNum: Int, modifier: Modifier = Modifier) {
    val repository = koinInject<MeshLogRepository>()
    var rangeIndex by remember { mutableStateOf(0) }
    val range = AVAILABILITY_RANGES[rangeIndex]
    // The window is fixed when the range is picked, so the chart does not jump on every recomposition.
    val nowMs = remember(nodeNum, rangeIndex) { nowMillis }
    val times by
        remember(nodeNum, rangeIndex) {
            repository.getReceivedTimesFromNodeSince(nodeNum, nowMs - range.rangeSeconds * 1000)
        }
            .collectAsStateWithLifecycle(emptyList())
    val counts = remember(times, range) { availabilityCounts(times, nowMs, range) }
    val heardBars = counts.count { it > 0 }
    val maxCount = counts.maxOrNull() ?: 0

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.availability_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                InfoButton(Res.string.availability_title, Res.string.mt_sw_other_availability_body)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.availability_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AVAILABILITY_RANGES.forEachIndexed { index, choice ->
                    FilterChip(
                        selected = index == rangeIndex,
                        onClick = { rangeIndex = index },
                        label = { Text(formatDuration(choice.rangeSeconds, DurationUnit.DAYS)) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (times.isEmpty()) {
                Text(
                    text = stringResource(Res.string.availability_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val barColor = MaterialTheme.colorScheme.primary
                val emptyColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    val slot = size.width / counts.size
                    val gap = if (slot > 4f) 1f else 0f
                    counts.forEachIndexed { index, count ->
                        val fraction = if (count == 0 || maxCount == 0) 0f else 0.25f + 0.75f * count / maxCount
                        val x = index * slot
                        if (count == 0) {
                            drawRoundRect(
                                color = emptyColor,
                                topLeft = Offset(x, size.height - 3f),
                                size = Size((slot - gap).coerceAtLeast(1f), 3f),
                                cornerRadius = CornerRadius(1f, 1f),
                            )
                        } else {
                            val h = size.height * fraction
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, size.height - h),
                                size = Size((slot - gap).coerceAtLeast(1f), h),
                                cornerRadius = CornerRadius(2f, 2f),
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = formatDuration(range.rangeSeconds, DurationUnit.DAYS),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(Res.string.availability_now),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text =
                    stringResource(
                        Res.string.availability_summary,
                        heardBars,
                        counts.size,
                        heardBars * PERCENT / counts.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text =
                    stringResource(
                        Res.string.availability_longest_silence,
                        formatDuration(longestSilenceSeconds(times, nowMs), DurationUnit.MINUTES),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private const val PERCENT = 100
