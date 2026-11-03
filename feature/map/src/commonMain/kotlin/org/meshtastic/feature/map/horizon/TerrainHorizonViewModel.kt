/*
 * Copyright (c) 2025 MT_SW contributors
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
package org.meshtastic.feature.map.horizon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.Node
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PhoneLocationProvider
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.Geodesy
import org.meshtastic.feature.map.planner.data.PlannerElevationException
import org.meshtastic.feature.map.planner.data.PlannerElevationSource

enum class TerrainHorizonFailure {
    /** Neither this phone nor the connected radio has a position to look from. */
    NO_POSITION,

    /** The terrain data could not be loaded (no internet, server error). */
    NETWORK,
}

data class TerrainHorizonState(
    val loading: Boolean = true,
    val failure: TerrainHorizonFailure? = null,
    val profile: HorizonProfile? = null,
    val targets: List<HorizonTarget> = emptyList(),
    val radiusKm: Int = DEFAULT_RADIUS_KM,
    val antennaM: Int = DEFAULT_ANTENNA_M,
    /** The one node the view was opened for (only it is drawn), or null for every node with a position. */
    val focusNodeNum: Int? = null,
)

const val DEFAULT_RADIUS_KM = 15
const val DEFAULT_ANTENNA_M = 2
val HORIZON_RADII_KM = listOf(2, 5, 15, 40)
val HORIZON_ANTENNAS_M = listOf(2, 5, 10, 20)

private const val TARGET_ANTENNA_M = 2.0
private const val LOCATION_WAIT_MS = 1_500L
private const val METRES_PER_KM = 1_000.0

/**
 * The terrain panorama: the skyline all round as seen from where this phone stands (else from the connected radio), with
 * the nodes that have a position placed against it. The terrain comes from the same source as the link planner.
 */
@KoinViewModel
class TerrainHorizonViewModel(
    private val elevation: PlannerElevationSource,
    private val nodeRepository: NodeRepository,
    private val phoneLocationProvider: PhoneLocationProvider,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val _state = MutableStateFlow(TerrainHorizonState())
    val state: StateFlow<TerrainHorizonState> = _state.asStateFlow()

    private var job: Job? = null
    private var opened = false

    /** Starts (once) for [focusNodeNum]: one node, or null for the whole mesh. */
    fun open(focusNodeNum: Int?) {
        if (opened && _state.value.focusNodeNum == focusNodeNum) return
        opened = true
        _state.update { it.copy(focusNodeNum = focusNodeNum) }
        reload()
    }

    fun setRadius(km: Int) {
        if (km == _state.value.radiusKm) return
        _state.update { it.copy(radiusKm = km) }
        reload()
    }

    fun setAntenna(metres: Int) {
        if (metres == _state.value.antennaM) return
        _state.update { it.copy(antennaM = metres) }
        reload()
    }

    fun retry() = reload()

    private fun reload() {
        job?.cancel()
        job =
            viewModelScope.launch {
                _state.update { it.copy(loading = true, failure = null) }
                val centre = centre()
                if (centre == null) {
                    _state.update { it.copy(loading = false, failure = TerrainHorizonFailure.NO_POSITION, profile = null, targets = emptyList()) }
                    return@launch
                }
                var settings = _state.value
                val nodes = nodes(settings.focusNodeNum)
                // A node asked for that lies beyond the range gets the smallest range that reaches it.
                if (settings.focusNodeNum != null) {
                    nodes.firstOrNull()?.let { (_, _, at) ->
                        val need = HORIZON_RADII_KM.firstOrNull { it * METRES_PER_KM >= Geodesy.distanceM(centre, at) } ?: HORIZON_RADII_KM.last()
                        if (need > settings.radiusKm) {
                            _state.update { it.copy(radiusKm = need) }
                            settings = _state.value
                        }
                    }
                }
                try {
                    val ground = elevation.prepareArea(centre, settings.radiusKm.toDouble())
                    val (profile, targets) =
                        withContext(dispatchers.default) {
                            val built = HorizonPanorama.build(ground, centre, settings.antennaM.toDouble(), settings.radiusKm * METRES_PER_KM)
                            built to HorizonPanorama.place(built, ground, nodes, TARGET_ANTENNA_M)
                        }
                    _state.update { it.copy(loading = false, profile = profile, targets = targets) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: PlannerElevationException) {
                    _state.update { it.copy(loading = false, failure = TerrainHorizonFailure.NETWORK, profile = null, targets = emptyList()) }
                }
            }
    }

    /** Where this phone is (a fix, when the permission is given), else where the connected radio is. */
    private suspend fun centre(): GeoPoint? {
        val fix =
            withTimeoutOrNull(LOCATION_WAIT_MS) {
                phoneLocationProvider.locationUpdates().first { it.location != null || !it.permissionGranted }
            }?.location
        if (fix != null) return GeoPoint(fix.latitude, fix.longitude)
        val own: Node? = nodeRepository.ourNodeInfo.value
        return own?.takeIf { it.validPosition != null }?.let { GeoPoint(it.latitude, it.longitude) }
    }

    private fun nodes(focus: Int?): List<Triple<Int, String, GeoPoint>> {
        val own = nodeRepository.ourNodeInfo.value?.num
        return nodeRepository.nodeDBbyNum.value.values
            .filter { it.num != own && it.validPosition != null && (focus == null || it.num == focus) }
            .map { node ->
                val name = node.user.short_name.ifBlank { node.user.long_name }
                Triple(node.num, name, GeoPoint(node.latitude, node.longitude))
            }
    }
}
