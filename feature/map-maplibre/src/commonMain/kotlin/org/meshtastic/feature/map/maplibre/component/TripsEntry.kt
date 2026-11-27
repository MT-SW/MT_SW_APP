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
@file:Suppress("ModifierMissing", "LongMethod")

package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import org.meshtastic.core.ui.component.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.meshtastic.core.repository.FileService
import org.meshtastic.core.resources.trips_import_json
import org.meshtastic.core.ui.util.rememberOpenFileLauncher
import org.meshtastic.feature.map.trip.TripShare
import org.koin.compose.koinInject
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.hide_layer
import org.meshtastic.core.resources.route_playback_play
import org.meshtastic.core.resources.show_layer
import org.meshtastic.core.resources.trips_empty
import org.meshtastic.core.resources.trips_info_1
import org.meshtastic.core.resources.trips_info_2
import org.meshtastic.core.resources.trips_info_3
import org.meshtastic.core.resources.trips_info_4
import org.meshtastic.core.resources.trips_info_title
import org.meshtastic.core.resources.trips_item
import org.meshtastic.core.resources.trips_title
import org.meshtastic.core.ui.component.InfoButton
import org.meshtastic.core.ui.component.ModalBottomSheet
import org.meshtastic.core.ui.component.SHEET_CROSS_INSET_DP
import org.meshtastic.core.ui.component.ScrollWithBar
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.Hiking
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PlayArrow
import org.meshtastic.core.ui.icon.Visibility
import org.meshtastic.core.ui.icon.VisibilityOff
import org.meshtastic.core.ui.theme.StatusColors.StatusYellow
import org.meshtastic.core.ui.util.TripsWindowRequest
import org.meshtastic.feature.map.playback.RoutePlayback
import org.meshtastic.feature.map.trip.TripStore
import org.meshtastic.feature.map.trip.stats
import org.meshtastic.core.ui.component.ListItem as StyledListItem

/**
 * MT_SW: the "Hikes" entry of the map layers menu. It opens a window with everything about hikes: recording the own
 * route, pictures taken on the way, and the list of saved hikes (show on the map, delete).
 */
@Composable
internal fun TripsEntry() {
    StyledListItem(text = stringResource(Res.string.trips_title), leadingIcon = MeshtasticIcons.Hiking) {
        Logger.i { "Wędrówki: okno otwarte z menu warstw" }
        TripsWindowRequest.requested.value = true
    }
}

/** Shows the Hikes window on the map while it is asked for (by the layers menu or by the own node's details). */
@Composable
internal fun TripsHost() {
    val requested by TripsWindowRequest.requested.collectAsStateWithLifecycle()
    if (requested) TripsWindow(onDismiss = { TripsWindowRequest.requested.value = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripsWindow(onDismiss: () -> Unit) {
    val store: TripStore = koinInject()
    val playback: RoutePlayback = koinInject()
    val trips by store.trips.collectAsStateWithLifecycle()
    val visible by store.visibleIds.collectAsStateWithLifecycle()
    var deleteId by remember { mutableStateOf<String?>(null) }
    var openId by remember { mutableStateOf<String?>(null) }
    val tripShare: TripShare = koinInject()
    val fileService: FileService = koinInject()
    val scope = rememberCoroutineScope()
    val pickFile =
        rememberOpenFileLauncher { uri ->
            if (uri == null) {
                Logger.d { "Wędrówki: wybór pliku do wczytania przerwany" }
            } else {
                scope.launch {
                    var text = ""
                    val read = fileService.read(uri) { text = it.readUtf8() }
                    if (!read) {
                        Logger.w { "Wędrówki: nie udało się odczytać wybranego pliku" }
                        return@launch
                    }
                    try {
                        val id = tripShare.importFile(text)
                        Logger.i { "Wędrówki: wczytana z pliku, id $id" }
                    } catch (e: Exception) {
                        Logger.w(e) { "Wędrówki: plik nie jest poprawną wędrówką: ${e.message}" }
                    }
                }
            }
        }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ScrollWithBar {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = SHEET_CROSS_INSET_DP.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.trips_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                )
                InfoButton(
                    Res.string.trips_info_title,
                    Res.string.trips_info_1,
                    Res.string.trips_info_2,
                    Res.string.trips_info_3,
                    Res.string.trips_info_4,
                    tint = MaterialTheme.colorScheme.StatusYellow,
                )
            }
            TrackRecordingSection()
            HorizontalDivider()
            if (trips.isEmpty()) {
                Text(
                    text = stringResource(Res.string.trips_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            trips.forEach { trip ->
                ListItem(
                    modifier = Modifier.clickable { openId = trip.id },
                    headlineContent = { Text(trip.name) },
                    supportingContent = {
                        val stats = trip.stats()
                        Text(
                            stringResource(Res.string.trips_item, trip.distanceMeters.toInt(), trip.photoCount) +
                                " · " +
                                stats.durationText,
                        )
                    },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            val shown = trip.id in visible
                            IconToggleButton(checked = shown, onCheckedChange = { store.setVisible(trip.id, it) }) {
                                androidx.compose.material3.Icon(
                                    imageVector =
                                    if (shown) MeshtasticIcons.Visibility else MeshtasticIcons.VisibilityOff,
                                    contentDescription =
                                    stringResource(if (shown) Res.string.hide_layer else Res.string.show_layer),
                                )
                            }
                            IconButton(
                                onClick = {
                                    store.setVisible(trip.id, true)
                                    playback.playTrip(trip.id)
                                    onDismiss()
                                },
                                enabled = trip.points.size > 1,
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = MeshtasticIcons.PlayArrow,
                                    contentDescription = stringResource(Res.string.route_playback_play),
                                )
                            }
                            IconButton(onClick = { deleteId = trip.id }) {
                                androidx.compose.material3.Icon(
                                    imageVector = MeshtasticIcons.Delete,
                                    contentDescription = stringResource(Res.string.delete),
                                )
                            }
                        }
                    },
                )
            }
            TextButton(
                onClick = {
                    Logger.d { "Wędrówki: wybór pliku wędrówki do wczytania" }
                    pickFile("*/*")
                },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(stringResource(Res.string.trips_import_json))
            }
        }
    }
    openId?.let { id -> TripDetailWindow(tripId = id, onDismiss = { openId = null }) }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text(stringResource(Res.string.delete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.delete(id)
                        deleteId = null
                    },
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}
