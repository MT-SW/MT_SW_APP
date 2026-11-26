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

import org.meshtastic.core.ui.component.FastScrollSidebar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import org.meshtastic.core.resources.trips_position_title
import org.meshtastic.core.resources.trips_position_auto
import org.meshtastic.core.resources.trips_position_phone
import org.meshtastic.core.resources.trips_position_device
import org.meshtastic.core.resources.trips_position_both
import org.meshtastic.core.resources.trips_position_hint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.ui.component.AlertDialog
import org.meshtastic.core.ui.component.Checkbox
import org.meshtastic.core.resources.sos_nodes_done
import org.meshtastic.core.resources.sos_nodes_empty
import org.meshtastic.core.resources.sos_nodes_search
import org.meshtastic.core.resources.trips_quiet_exceptions
import org.meshtastic.core.resources.trips_quiet_nodes
import org.meshtastic.core.resources.trips_quiet_pick_title
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
import org.meshtastic.core.resources.trips_rangetest
import org.meshtastic.core.resources.trips_signal_live
import org.meshtastic.core.resources.trips_signal_hint
import org.meshtastic.core.resources.trips_signal_title
import org.meshtastic.core.common.state.HiddenFeaturesUnlock
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
import org.meshtastic.core.resources.trips_quiet_priority
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
        val positionSource = UiOptions(options)[UiOptions.HIKE_POSITION_SOURCE]
        Text(text = stringResource(Res.string.trips_position_title), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                UiOptions.HIKE_POSITION_AUTO to Res.string.trips_position_auto,
                UiOptions.HIKE_POSITION_PHONE to Res.string.trips_position_phone,
                UiOptions.HIKE_POSITION_DEVICE to Res.string.trips_position_device,
                UiOptions.HIKE_POSITION_BOTH to Res.string.trips_position_both,
            )
                .forEach { (value, label) ->
                    FilterChip(
                        selected = positionSource == value,
                        onClick = { prefs.setUiOption(UiOptions.HIKE_POSITION_SOURCE, value) },
                        label = { Text(stringResource(label)) },
                    )
                }
        }
        Text(text = stringResource(Res.string.trips_position_hint), style = MaterialTheme.typography.bodySmall)
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
        val signalEvery = UiOptions(options)[UiOptions.HIKE_SIGNAL_EVERY]
        Text(text = stringResource(Res.string.trips_signal_title), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(0 to "—", 5 to "5 s", 15 to "15 s", 30 to "30 s", 60 to "1 min").forEach { (seconds, label) ->
                FilterChip(
                    selected = signalEvery == seconds,
                    onClick = { prefs.setUiOption(UiOptions.HIKE_SIGNAL_EVERY, seconds) },
                    label = { Text(label) },
                )
            }
        }
        Text(text = stringResource(Res.string.trips_signal_hint), style = MaterialTheme.typography.bodySmall)
        if (state.recording && signalEvery > 0) {
            Text(
                text = stringResource(Res.string.trips_signal_live, state.signals.size),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val hiddenOpen by koinInject<HiddenFeaturesUnlock>().unlocked.collectAsStateWithLifecycle()
        if (hiddenOpen) {
            val testOn = UiOptions(options)[UiOptions.HIKE_RANGE_TEST] == 1
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.trips_rangetest),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = testOn,
                    onCheckedChange = { prefs.setUiOption(UiOptions.HIKE_RANGE_TEST, if (it) 1 else 0) },
                )
            }
            if (testOn) {
                val testEvery = UiOptions(options)[UiOptions.HIKE_RANGE_TEST_EVERY]
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(60 to "1 min", 120 to "2 min", 300 to "5 min", 600 to "10 min").forEach { (seconds, label) ->
                        FilterChip(
                            selected = testEvery == seconds,
                            onClick = { prefs.setUiOption(UiOptions.HIKE_RANGE_TEST_EVERY, seconds) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }
        val quiet by QuietMode.mode.collectAsStateWithLifecycle()
        val exceptFavorites by QuietMode.exceptFavorites.collectAsStateWithLifecycle()
        val exceptNodes by QuietMode.exceptNodes.collectAsStateWithLifecycle()
        val nodeRepository: NodeRepository = koinInject()
        val nodeDb by nodeRepository.nodeDBbyNum.collectAsStateWithLifecycle()
        val myInfo by nodeRepository.myNodeInfo.collectAsStateWithLifecycle()
        var pickingQuietNodes by remember { mutableStateOf(false) }
        if (pickingQuietNodes) {
            QuietNodePicker(
                nodes = nodeDb.values.filter { it.num != myInfo?.myNodeNum },
                selected = exceptNodes,
                onToggle = { num ->
                    QuietMode.setExceptNodes(if (num in exceptNodes) exceptNodes - num else exceptNodes + num)
                },
                onDismiss = { pickingQuietNodes = false },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.trips_quiet_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = quiet == QuietMode.Mode.Silent,
                onCheckedChange = { recorder.setQuiet(if (it) QuietMode.Mode.Silent else QuietMode.Mode.Off) },
            )
        }
        Text(text = stringResource(Res.string.trips_quiet_exceptions), style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.trips_quiet_priority),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = exceptFavorites, onCheckedChange = { QuietMode.setExceptFavorites(it) })
        }
        OutlinedButton(onClick = { pickingQuietNodes = true }) {
            Text(stringResource(Res.string.trips_quiet_nodes, exceptNodes.size))
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

/** The name of a node for the list of exceptions: its long name, else the short one, else its id. */
private fun quietNodeName(node: Node): String {
    val long = node.user.long_name
    val short = node.user.short_name
    return when {
        long.isNotBlank() && short.isNotBlank() -> "$long ($short)"
        long.isNotBlank() -> long
        short.isNotBlank() -> short
        else -> NodeAddress.numToDefaultId(node.num)
    }
}

/** The window to pick, by hand, the nodes that are an exception to the quiet mode. Every known node is listed. */
@Composable
private fun QuietNodePicker(nodes: List<Node>, selected: Set<Int>, onToggle: (Int) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown =
        remember(nodes, query) {
            nodes
                .filter { quietNodeName(it).contains(query.trim(), ignoreCase = true) }
                .sortedBy { quietNodeName(it).lowercase() }
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.trips_quiet_pick_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.sos_nodes_search)) },
                    singleLine = true,
                )
                if (shown.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.sos_nodes_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                val listState = rememberLazyListState()
                Box(modifier = Modifier.padding(top = 8.dp)) {
                LazyColumn(state = listState) {
                    items(shown, key = { it.num }) { node ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onToggle(node.num) },
                        ) {
                            Checkbox(checked = node.num in selected, onCheckedChange = { onToggle(node.num) })
                            Text(text = quietNodeName(node), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                FastScrollSidebar(listState, Modifier.align(Alignment.CenterEnd))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sos_nodes_done)) } },
    )
}
