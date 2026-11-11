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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.propagation_hour_ahead
import org.meshtastic.core.resources.propagation_hour_now
import org.meshtastic.feature.map.propagation.PropagationController

private const val OVERLAY_BACKGROUND_ALPHA = 0.85f
private const val OVERLAY_WIDTH_DP = 200

/**
 * The legend of the propagation layer and the hour of the forecast, small, right on the map, so the hour can be changed
 * without opening the layer settings. Nothing is drawn while the layer is off or has no forecast yet.
 */
@Composable
internal fun PropagationOverlay(modifier: Modifier = Modifier) {
    val controller: PropagationController = koinInject()
    val state by controller.state.collectAsStateWithLifecycle()
    val grid = state.grid
    if (!state.enabled || grid == null) return
    Column(
        modifier =
        modifier
            .width(OVERLAY_WIDTH_DP.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = OVERLAY_BACKGROUND_ALPHA))
            .padding(8.dp),
    ) {
        val ahead = state.hoursAhead
        Text(
            text =
            if (ahead == 0) {
                stringResource(Res.string.propagation_hour_now)
            } else {
                stringResource(Res.string.propagation_hour_ahead, ahead)
            },
            style = MaterialTheme.typography.labelMedium,
        )
        val last = (grid.hours - 1 - grid.hourAt(nowMillis)).coerceAtLeast(1)
        Slider(
            value = ahead.toFloat(),
            onValueChange = { controller.setHoursAhead(it.toInt()) },
            valueRange = 0f..last.toFloat(),
            steps = (last - 1).coerceAtLeast(0),
        )
        Legend()
    }
}
