/*
 * Copyright (c) 2025-2026 Meshtastic LLC
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
package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_snapshot_clear
import org.meshtastic.core.resources.planner_snapshot_row
import org.meshtastic.core.resources.planner_snapshot_save
import org.meshtastic.core.resources.planner_snapshot_title
import org.meshtastic.feature.map.planner.state.PlannerUiState
import org.meshtastic.feature.map.planner.state.PlannerViewModel

private const val KM = 1000.0

/**
 * Keeps results of the link next to each other: "remember" stores the current result (distance, margins both ways, loss,
 * radio and antenna heights), and every remembered one is listed, so a change of antenna, height or radio can be
 * compared with what it was before. The list is kept on the device.
 */
@Composable
internal fun SnapshotSection(state: PlannerUiState, vm: PlannerViewModel) {
    if (!state.isLinkReady || state.link == null) return
    PlannerCard(title = stringResource(Res.string.planner_snapshot_title)) {
        OutlinedButton(onClick = vm::saveSnapshot, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.planner_snapshot_save))
        }
        state.snapshots.forEach { s ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text =
                    stringResource(
                        Res.string.planner_snapshot_row,
                        s.label,
                        fmt(s.distanceM / KM, 1),
                        fmt(s.marginAToBDb, 1),
                        fmt(s.marginBToADb, 1),
                        s.spreadingFactor,
                        fmt(s.frequencyMHz, 3),
                        fmt(s.heightAM, 0),
                        fmt(s.heightBM, 0),
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (state.snapshots.isNotEmpty()) {
            TextButton(onClick = vm::clearSnapshots) { Text(stringResource(Res.string.planner_snapshot_clear)) }
        }
    }
}
