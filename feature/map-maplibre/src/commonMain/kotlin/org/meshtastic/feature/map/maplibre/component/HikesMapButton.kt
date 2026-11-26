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

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.trips_title
import org.meshtastic.core.ui.icon.Hiking
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.TripsWindowRequest
import org.meshtastic.feature.map.component.MapButton
import org.meshtastic.feature.map.track.TrackRecorder

/**
 * MT_SW: the "Hikes" button of the map toolbar. A tap opens the Hikes window (recording the own route, pictures, saved
 * hikes); a red dot on the button shows that a route is being recorded right now.
 */
@Composable
internal fun HikesMapButton() {
    val recorder: TrackRecorder = koinInject()
    val state by recorder.state.collectAsStateWithLifecycle()
    BadgedBox(badge = { if (state.recording) Badge() }) {
        MapButton(
            icon = MeshtasticIcons.Hiking,
            contentDescription = stringResource(Res.string.trips_title),
            onClick = {
                Logger.i { "Wędrówki: okno otwarte z przycisku na mapie" }
                TripsWindowRequest.requested.value = true
            },
        )
    }
}
