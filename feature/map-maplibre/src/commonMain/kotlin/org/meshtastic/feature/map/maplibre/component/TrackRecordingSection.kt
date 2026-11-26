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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.meshtastic.core.resources.trips_health_live
import org.meshtastic.core.resources.trips_health_auto_hint
import org.meshtastic.core.resources.trips_health_auto
import org.meshtastic.core.ui.util.rememberHealthSync
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.model.UiOptions
import androidx.compose.ui.Alignment
import androidx.compose.material3.Switch
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
import org.meshtastic.core.resources.trips_album_change
import org.meshtastic.core.resources.trips_album_folder
import org.meshtastic.core.resources.trips_album_note
import org.meshtastic.core.resources.trips_photo
import org.meshtastic.core.resources.trips_quiet_hint
import org.meshtastic.core.resources.trips_quiet_off
import org.meshtastic.core.resources.trips_quiet_priority
import org.meshtastic.core.resources.trips_quiet_silent
import org.meshtastic.core.resources.trips_quiet_title
import org.koin.compose.koinInject
import org.meshtastic.core.repository.QuietMode
import org.meshtastic.feature.map.trip.TripStore
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PhotoCamera
import org.meshtastic.core.ui.util.defaultAlbumFolder
import org.meshtastic.core.ui.util.rememberAlbumFolderPicker
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
                    text = stringResource(Res.string.trips_photo, state.stops.sumOf { it.photos.size }),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        val store: TripStore = koinInject()
        val folder by store.albumFolder.collectAsStateWithLifecycle()
        val pickFolder = rememberAlbumFolderPicker { store.setAlbumFolder(it) }
        Text(text = stringResource(Res.string.trips_album_note), style = MaterialTheme.typography.bodySmall)
        if (pickFolder != null) {
            Text(
                text = stringResource(Res.string.trips_album_folder, folder ?: defaultAlbumFolder().orEmpty()),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = pickFolder) { Text(stringResource(Res.string.trips_album_change)) }
        }
        val prefs: UiPrefs = koinInject()
        val options by prefs.uiOptions.collectAsStateWithLifecycle()
        val healthAuto = UiOptions(options)[UiOptions.HIKE_HEALTH_AUTO] == 1
        if (rememberHealthSync() != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.trips_health_auto),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = healthAuto,
                    onCheckedChange = { prefs.setUiOption(UiOptions.HIKE_HEALTH_AUTO, if (it) 1 else 0) },
                )
            }
            Text(text = stringResource(Res.string.trips_health_auto_hint), style = MaterialTheme.typography.bodySmall)
            val live by recorder.liveHealth.collectAsStateWithLifecycle()
            live?.steps?.let {
                if (state.recording) {
                    Text(
                        text = stringResource(Res.string.trips_health_live, it),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        val quiet by QuietMode.mode.collectAsStateWithLifecycle()
        Text(text = stringResource(Res.string.trips_quiet_title), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                QuietMode.Mode.Off to Res.string.trips_quiet_off,
                QuietMode.Mode.Silent to Res.string.trips_quiet_silent,
                QuietMode.Mode.Priority to Res.string.trips_quiet_priority,
            )
                .forEach { (mode, label) ->
                    FilterChip(
                        selected = quiet == mode,
                        onClick = { recorder.setQuiet(mode) },
                        label = { Text(stringResource(label)) },
                    )
                }
        }
        Text(text = stringResource(Res.string.trips_quiet_hint), style = MaterialTheme.typography.bodySmall)
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
