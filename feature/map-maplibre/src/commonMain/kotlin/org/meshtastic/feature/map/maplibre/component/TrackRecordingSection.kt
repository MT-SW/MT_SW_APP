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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.track_rec_clear
import org.meshtastic.core.resources.track_rec_hint
import org.meshtastic.core.resources.track_rec_save
import org.meshtastic.core.resources.track_rec_start
import org.meshtastic.core.resources.track_rec_status
import org.meshtastic.core.resources.track_rec_stop
import org.meshtastic.core.resources.track_rec_title
import org.meshtastic.core.resources.trips_photo
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PhotoCamera
import org.meshtastic.core.ui.util.rememberCameraLauncher
import org.meshtastic.core.ui.util.rememberSaveFileLauncher
import org.meshtastic.feature.map.track.TrackRecorder

/**
 * MT_SW: records the person's own route and saves it as a GPX file. The buttons only start, stop, save and delete; the
 * work is done by [TrackRecorder].
 */
@Composable
internal fun TrackRecordingSection() {
    val recorder: TrackRecorder = koinInject()
    val state by recorder.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val saveFile = rememberSaveFileLauncher { uri -> scope.launch { recorder.save(uri) } }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(Res.string.track_rec_title), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(Res.string.track_rec_hint), style = MaterialTheme.typography.bodySmall)
        if (state.points.isNotEmpty() || state.recording) {
            Text(
                text = stringResource(Res.string.track_rec_status, state.points.size, state.distanceMeters.toInt()),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val camera = rememberCameraLauncher { bytes, _ -> recorder.addPhoto(bytes) }
        if (state.recording && camera != null) {
            OutlinedButton(onClick = camera) {
                Icon(imageVector = MeshtasticIcons.PhotoCamera, contentDescription = null)
                Text(
                    text = stringResource(Res.string.trips_photo, state.photos.size),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.recording) {
                Button(onClick = recorder::stop) { Text(stringResource(Res.string.track_rec_stop)) }
            } else {
                Button(onClick = recorder::start) { Text(stringResource(Res.string.track_rec_start)) }
            }
            OutlinedButton(
                enabled = state.points.size >= 2,
                onClick = { saveFile("trasa.gpx", "application/gpx+xml") },
            ) {
                Text(stringResource(Res.string.track_rec_save))
            }
            OutlinedButton(
                enabled = state.points.isNotEmpty() || state.recording,
                onClick = recorder::clear,
            ) {
                Text(stringResource(Res.string.track_rec_clear))
            }
        }
    }
}
