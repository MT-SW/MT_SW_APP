/*
 * Copyright (c) 2025 MT_SW contributors
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
@file:OptIn(ExperimentalMaterial3Api::class)

package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_copy_a_to_b
import org.meshtastic.core.resources.planner_copy_a_to_b_hint
import org.meshtastic.core.resources.planner_incomplete
import org.meshtastic.core.resources.planner_point_not_set
import org.meshtastic.core.resources.planner_point_unnamed
import org.meshtastic.core.resources.planner_summary_radio
import org.meshtastic.core.resources.planner_swap_ab
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.feature.map.planner.state.PlannerEnd
import org.meshtastic.feature.map.planner.state.PlannerSide
import org.meshtastic.feature.map.planner.state.PlannerUiState
import org.meshtastic.feature.map.planner.state.PlannerViewModel

/** A | B switch, the always visible summary of both ends and the copy / swap actions. */
@Composable
internal fun SideSelector(state: PlannerUiState, vm: PlannerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PlannerSide.entries.forEachIndexed { index, side ->
                SegmentedButton(
                    selected = state.selectedSide == side,
                    onClick = { vm.selectSide(side) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = PlannerSide.entries.size),
                ) {
                    Text(sideName(side))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PlannerSide.entries.forEach { side ->
                SideSummaryCard(
                    side = side,
                    state = state,
                    selected = state.selectedSide == side,
                    onClick = { vm.selectSide(side) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { vm.copyAToB() }) { Text(stringResource(Res.string.planner_copy_a_to_b)) }
            TextButton(onClick = { vm.swapAB() }) { Text(stringResource(Res.string.planner_swap_ab)) }
        }
        Text(
            text = stringResource(Res.string.planner_copy_a_to_b_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SideSummaryCard(
    side: PlannerSide,
    state: PlannerUiState,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val end = state.end(side)
    val incomplete = state.hintsBySide[side].orEmpty().isNotEmpty() || !end.isComplete
    Card(
        onClick = onClick,
        modifier = modifier,
        colors =
        CardDefaults.cardColors(
            containerColor =
            if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = sideName(side),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (incomplete) {
                    Icon(
                        imageVector = MeshtasticIcons.Warning,
                        contentDescription = stringResource(Res.string.planner_incomplete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = summaryName(end),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summaryCoordinates(end),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summaryRadio(end),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun summaryName(end: PlannerEnd): String =
    end.name.ifBlank { stringResource(Res.string.planner_point_unnamed) }

@Composable
private fun summaryCoordinates(end: PlannerEnd): String {
    val lat = end.lat
    val lon = end.lon
    return if (lat != null && lon != null) {
        PlannerInput.formatCoordinates(lat, lon, 4)
    } else {
        stringResource(Res.string.planner_point_not_set)
    }
}

@Composable
private fun summaryRadio(end: PlannerEnd): String = stringResource(
    Res.string.planner_summary_radio,
    fmt(end.txPowerDbm, 1),
    fmt(end.antennaGainDbi, 1),
    fmt(end.antennaHeightM, 1),
    fmt(end.feederLossDb, 1),
)
