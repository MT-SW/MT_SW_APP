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
@file:Suppress("MagicNumber")

package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.route_playback_pause
import org.meshtastic.core.resources.route_playback_play
import org.meshtastic.core.resources.route_playback_reading
import org.meshtastic.core.resources.route_playback_relay
import org.meshtastic.core.resources.route_playback_stop
import org.meshtastic.core.ui.component.Slider
import org.meshtastic.feature.map.playback.RoutePlayback

private const val PANEL_ALPHA = 0.85f

/** MT_SW: the panel of the virtual walk: which node was heard at this point and how strongly, play, speed, position. */
@Composable
internal fun RoutePlaybackControls(modifier: Modifier = Modifier) {
    val playback: RoutePlayback = koinInject()
    val state by playback.state.collectAsStateWithLifecycle()
    val now = state.current
    if (!state.active || now == null) return
    Column(
        modifier =
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = PANEL_ALPHA))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        val tripId = state.tripId
        val slide = now.slides.getOrNull(state.slide)
        if (tripId != null) {
            // A hike: the title of the place, and its picture with the caption while the walk is there.
            Text(text = now.name, style = MaterialTheme.typography.titleSmall)
            if (slide != null) {
                TripSlideImage(tripId = tripId, file = slide.file, modifier = Modifier.fillMaxWidth())
                if (slide.caption.isNotBlank()) Text(text = slide.caption, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val snr = now.snr?.let { " · SNR ${(it * 10).toInt() / 10f}" }.orEmpty()
            Text(
                text = stringResource(Res.string.route_playback_reading, now.name, now.rssi, snr),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (tripId == null && !now.direct) {
            Text(stringResource(Res.string.route_playback_relay), style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = state.index.toFloat(),
            onValueChange = { playback.seek(it.toInt()) },
            valueRange = 0f..state.steps.lastIndex.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { playback.togglePause() }) {
                Text(
                    stringResource(
                        if (state.playing) Res.string.route_playback_pause else Res.string.route_playback_play,
                    ),
                )
            }
            TextButton(
                onClick = {
                    val speeds = RoutePlayback.SPEEDS
                    playback.setSpeed(speeds[(speeds.indexOf(state.speed) + 1) % speeds.size])
                },
            ) {
                Text("${state.speed}x")
            }
            TextButton(onClick = { playback.stop() }) { Text(stringResource(Res.string.route_playback_stop)) }
        }
    }
}
