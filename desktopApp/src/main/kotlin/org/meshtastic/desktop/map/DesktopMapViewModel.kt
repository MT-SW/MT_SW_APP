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
package org.meshtastic.desktop.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.repository.MapCameraPosition
import org.meshtastic.core.repository.MapPrefs
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.NotificationPrefs
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.feature.map.BaseMapViewModel

/**
 * Desktop's map ViewModel — mirrors the fdroid/google `MapViewModel`s but without the Android-specific map-layer-import
 * feature (URI/InputStream based), which is out of scope for the initial desktop map.
 */
@KoinViewModel
class DesktopMapViewModel(
    mapPrefs: MapPrefs,
    packetRepository: PacketRepository,
    nodeRepository: NodeRepository,
    radioController: RadioController,
    radioConfigRepository: RadioConfigRepository,
    notificationPrefs: NotificationPrefs,
    savedStateHandle: SavedStateHandle,
) : BaseMapViewModel(
    mapPrefs,
    nodeRepository,
    packetRepository,
    radioController,
    radioConfigRepository,
    notificationPrefs,
) {

    private val mutableInitialCameraState = MutableStateFlow<InitialCameraState>(InitialCameraState.Loading)
    internal val initialCameraState: StateFlow<InitialCameraState> = mutableInitialCameraState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableInitialCameraState.value = InitialCameraState.Ready(mapPrefs.awaitCameraPosition())
        }
    }

    fun saveCameraPosition(latitude: Double, longitude: Double, zoom: Double) {
        mapPrefs.setCameraPosition(MapCameraPosition(latitude, longitude, zoom))
    }

    private val _selectedWaypointId = MutableStateFlow(savedStateHandle.get<Int>("waypointId"))
    val selectedWaypointId: StateFlow<Int?> = _selectedWaypointId.asStateFlow()

    fun setWaypointId(id: Int?) {
        if (_selectedWaypointId.value != id) {
            _selectedWaypointId.value = id
        }
    }
}

internal sealed interface InitialCameraState {
    data object Loading : InitialCameraState

    data class Ready(val position: MapCameraPosition?) : InitialCameraState
}

// NOTE (recovery script): DesktopMapViewProvider.kt (the file that actually consumes this ViewModel) evolved to use
// viewModel.nodesWithPosition / viewModel.waypoints / viewModel.mapFilterStateFlow / viewModel.ourNodeInfo /
// viewModel.isConnected / viewModel.myNodeNum / viewModel.deleteWaypoint() / viewModel.sendWaypoint() — all inherited
// from BaseMapViewModel, so no changes needed here for those. This file's own fitToBounds/auto-center handling was
// later done in MapViewportState.fitToBounds() instead of via initialCameraState — see that file's notes.
