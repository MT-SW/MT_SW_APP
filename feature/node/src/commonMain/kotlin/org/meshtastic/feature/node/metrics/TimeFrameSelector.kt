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
package org.meshtastic.feature.node.metrics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.feature.node.model.TimeFrame

@Composable
fun TimeFrameSelector(
    selectedTimeFrame: TimeFrame,
    availableTimeFrames: List<TimeFrame>,
    onTimeFrameSelected: (TimeFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availableTimeFrames.size <= 1) return

    // Plain scrollable row of FilterChips instead of Material3's experimental ButtonGroup: that API throws
    // IllegalArgumentException("maxWidth must be >= than minWidth") whenever the fixed items plus its overflow
    // indicator don't fit the available width — which longer Polish labels hit reliably. Horizontal scroll takes
    // the place of the overflow menu. Same pattern NodeMetricDetailScreen already uses for its own range selector.
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
    ) {
        availableTimeFrames.forEach { timeFrame ->
            val isSelected = timeFrame == selectedTimeFrame
            FilterChip(
                selected = isSelected,
                onClick = { onTimeFrameSelected(timeFrame) },
                label = {
                    Text(text = stringResource(timeFrame.strRes), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                modifier =
                Modifier.semantics {
                    role = Role.RadioButton
                    selected = isSelected
                },
            )
        }
    }
}
