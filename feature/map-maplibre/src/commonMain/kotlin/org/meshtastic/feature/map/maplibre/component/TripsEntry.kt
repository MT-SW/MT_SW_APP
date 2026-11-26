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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.hide_layer
import org.meshtastic.core.resources.show_layer
import org.meshtastic.core.resources.trips_empty
import org.meshtastic.core.resources.trips_item
import org.meshtastic.core.resources.trips_title
import org.meshtastic.core.ui.component.StyledBottomSheet
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Visibility
import org.meshtastic.core.ui.icon.VisibilityOff
import org.meshtastic.core.ui.component.ListItem as StyledListItem
import org.meshtastic.core.ui.util.TripsWindowRequest
import org.meshtastic.feature.map.trip.TripStore

/**
 * MT_SW: the "Hikes" entry of the map layers menu. It opens a window with everything about hikes: recording the own
 * route, pictures taken on the way, and the list of saved hikes (show on the map, delete).
 */
@Composable
internal fun TripsEntry() {
    StyledListItem(text = stringResource(Res.string.trips_title)) {
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

@Composable
private fun TripsWindow(onDismiss: () -> Unit) {
    val store: TripStore = koinInject()
    val trips by store.trips.collectAsStateWithLifecycle()
    val visible by store.visibleIds.collectAsStateWithLifecycle()
    var deleteId by remember { mutableStateOf<String?>(null) }
    StyledBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(Res.string.trips_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
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
                    headlineContent = { Text(trip.name) },
                    supportingContent = {
                        Text(stringResource(Res.string.trips_item, trip.distanceMeters.toInt(), trip.photoCount))
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
        }
    }
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
