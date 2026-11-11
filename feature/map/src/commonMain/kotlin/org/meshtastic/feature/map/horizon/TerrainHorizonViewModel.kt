/*
 * Copyright (c) 2025-2026 Meshtastic LLC
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
@file:Suppress(
    "LongMethod",
    "ReturnCount",
    "SwallowedException",
    "TooManyFunctions",
    "LongParameterList",
    "UnusedPrivateProperty",
)

package org.meshtastic.feature.map.horizon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.util.precisionRadiusMetersOrNull
import org.meshtastic.core.repository.HorizonPrefs
import org.meshtastic.core.repository.MagneticFieldProvider
import org.meshtastic.core.repository.MapPrefs
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PhoneLocationProvider
import org.meshtastic.feature.map.hud.CameraPoseProvider
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.Geodesy
import org.meshtastic.feature.map.planner.data.BuildingStore
import org.meshtastic.feature.map.planner.data.ClutterHeights
import org.meshtastic.feature.map.planner.data.ClutterKind
import org.meshtastic.feature.map.planner.data.ClutterMap
import org.meshtastic.feature.map.planner.data.PlannerClutterException
import org.meshtastic.feature.map.planner.data.PlannerClutterSource
import org.meshtastic.feature.map.planner.data.PlannerElevationException
import org.meshtastic.feature.map.planner.data.PlannerElevationSource
import org.meshtastic.feature.map.planner.data.TerrainStore

enum class TerrainHorizonFailure {
    /** The place to look from has no position (no phone fix, a node that never reported one). */
    NO_POSITION,

    /** The terrain data could not be loaded (no internet, server error). */
    NETWORK,
}

/** Where the panorama is looked from. */
sealed interface HorizonObserver {
    /** The connected radio: its position and its height (the default). */
    data object MyNode : HorizonObserver

    /** This phone's own fix. */
    data object Phone : HorizonObserver

    /** Any node that has reported a position. */
    data class OtherNode(val num: Int) : HorizonObserver

    /** A point typed in by hand. */
    data class Point(val lat: Double, val lon: Double) : HorizonObserver
}

data class HorizonNodeChoice(
    val num: Int,
    val name: String,
    val shortName: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val color: Int = 0,
    val textColor: Int = 0,
    val precisionM: Double? = null,
    val favorite: Boolean = false,
)

data class TerrainHorizonState(
    val loading: Boolean = true,
    val failure: TerrainHorizonFailure? = null,
    val profile: HorizonProfile? = null,
    val targets: List<HorizonTarget> = emptyList(),
    val radiusKm: Int = DEFAULT_RADIUS_KM,
    /** How far the nodes are drawn (km), independent of how far the terrain is loaded. */
    val nodeRangeKm: Int = DEFAULT_NODE_RANGE_KM,
    /** True: a node that hides its exact position gets a circle of the size of the doubt. */
    val precisionCircles: Boolean = true,
    /** True: only the nodes marked as favourites are drawn (the one the view was opened for always is). */
    val onlyFavorites: Boolean = false,
    val observer: HorizonObserver = HorizonObserver.MyNode,
    /** The name of the node (or "") the view is looked from, for the label. */
    val observerName: String = "",
    /** The position the view is looked from, for the move map; null until it is known. */
    val observerLat: Double? = null,
    val observerLon: Double? = null,
    /** True: the antenna height is worked out from the node's reported altitude when it has one. */
    val antennaAuto: Boolean = true,
    /** The antenna height typed or picked (m above the ground), used when the automatic one is off or unknown. */
    val antennaM: Int = DEFAULT_ANTENNA_M,
    /** The height from the node's altitude (m above the ground), or null when it cannot be worked out. */
    val autoAntennaM: Int? = null,
    /** The height actually used. */
    val usedAntennaM: Int = DEFAULT_ANTENNA_M,
    /** The height of the antennas of the nodes seen (m above the ground). */
    val targetAntennaM: Int = DEFAULT_ANTENNA_M,
    /** The earth radius factor of the radio horizon: 4/3 is the usual, 1 is the purely geometric one. */
    val refraction: Double = HorizonPanorama.DEFAULT_K,
    /** The one node the view was opened for (only it is drawn), or null for every node with a position. */
    val focusNodeNum: Int? = null,
    /** Nodes with a position, to pick another place to look from. */
    val nodeChoices: List<HorizonNodeChoice> = emptyList(),
    /** The space the terrain saved on the device takes, in bytes. */
    val savedBytes: Long = 0L,
    /*  Saving the terrain for offline use is running: tiles done of tiles in all. */
    /** How far the terrain is saved (km): the last of the ranges, 100, unless picked otherwise. */
    val saveRadiusKm: Int = SAVE_MAX_RADIUS_KM,
    val saving: Boolean = false,
    val saveDone: Int = 0,
    val saveTotal: Int = 0,
    /** How the last save ended: true saved, false failed (no internet, disk), null none yet. */
    val saveOk: Boolean? = null,
    /** True: the skyline is raised by the saved buildings (off by default; they are read from the device). */
    val buildings: Boolean = false,
    /** The space the saved buildings take, in bytes. */
    val buildingsBytes: Long = 0L,
    /** True when a saved area of buildings covers the place looked from. */
    val buildingsHere: Boolean = false,
    /** How far the buildings are downloaded (km). */
    val buildingsRadiusKm: Int = DEFAULT_BUILDINGS_RADIUS_KM,
    val buildingsSaving: Boolean = false,
    /** How the last download ended: true saved, false failed, null none yet. */
    val buildingsOk: Boolean? = null,
)

const val DEFAULT_RADIUS_KM = 15
const val DEFAULT_NODE_RANGE_KM = 100
val HORIZON_NODE_RANGES_KM = listOf(25, 50, 100, 200, 500, 1000)
const val DEFAULT_ANTENNA_M = 2

/** The ranges the buildings can be downloaded for: dense places are heavy, so far fewer than the terrain's. */
val HORIZON_BUILDING_RADII_KM = listOf(1, 2, 5, 10, 15, 25, 50, 100)

/** Up to this range every building is downloaded, beyond it only the tall ones (the rest are specks that far). */
const val ALL_BUILDINGS_MAX_KM = 15
const val DEFAULT_BUILDINGS_RADIUS_KM = 5
val HORIZON_RADII_KM = listOf(2, 5, 15, 25, 40, 80, 100)

/** The farthest range the terrain can be saved for. */
const val SAVE_MAX_RADIUS_KM = 100
val HORIZON_ANTENNAS_M = listOf(2, 5, 10, 20, 30)
val HORIZON_REFRACTIONS = listOf(HorizonPanorama.DEFAULT_K, 1.0)

private const val FULL_TURN_DEG = 360.0
private const val LOCATION_WAIT_MS = 1_500L
private const val METRES_PER_KM = 1_000.0
private const val MIN_AUTO_HEIGHT_M = 0.5
private const val MAX_AUTO_HEIGHT_M = 100.0
private const val METRES_PER_DEGREE = 111_320.0
private const val HALF_TURN_DEG = 180.0

/** Buildings this close to the place looked from are not counted: it stands on or beside them, not behind them. */
private const val OWN_BUILDING_M = 25.0

/** A place to look from: its position, the name of its node (or ""), and its altitude above sea level when known. */
private data class Spot(val at: GeoPoint, val name: String, val altitudeM: Double?, val nodeNum: Int? = null)

/**
 * The terrain panorama: the skyline all round as seen from a chosen place (the connected radio by default, with its
 * height), with the nodes that have a position placed against it. The terrain comes from the link planner's source.
 */
@KoinViewModel
class TerrainHorizonViewModel(
    private val elevation: PlannerElevationSource,
    private val nodeRepository: NodeRepository,
    private val phoneLocationProvider: PhoneLocationProvider,
    private val terrainStore: TerrainStore,
    private val buildingStore: BuildingStore,
    private val clutter: PlannerClutterSource,
    private val poseProvider: CameraPoseProvider,
    private val magneticFieldProvider: MagneticFieldProvider,
    private val mapPrefs: MapPrefs,
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
        viewModelScope.launch {
            _state.update { it.copy(savedBytes = withContext(dispatchers.io) { terrainStore.sizeBytes() }) }
        }
        viewModelScope.launch {
            _state.update { it.copy(buildingsBytes = withContext(dispatchers.io) { buildingStore.sizeBytes() }) }
        }
        // The range, antenna heights and radio-horizon factor picked last time come back before the first drawing.
        viewModelScope.launch {
            val saved = mapPrefs.awaitHorizonPrefs()
            _state.update {
                it.copy(
                    radiusKm = saved.radiusKm,
                    nodeRangeKm = saved.nodeRangeKm,
                    precisionCircles = saved.precisionCircles,
                    onlyFavorites = saved.onlyFavorites,
                    antennaAuto = saved.antennaAuto,
                    antennaM = saved.antennaM,
                    targetAntennaM = saved.targetAntennaM,
                    refraction = saved.refraction,
                )
            }
            reload()
        }
    }

    fun setRadius(km: Int) = change { it.copy(radiusKm = km) }

    fun setNodeRange(km: Int) = change { it.copy(nodeRangeKm = km) }

    fun setOnlyFavorites(on: Boolean) = change { it.copy(onlyFavorites = on) }

    /** Switches the accuracy circles; nothing is loaded again, they are only drawn or not. */
    fun setPrecisionCircles(on: Boolean) {
        _state.update { it.copy(precisionCircles = on) }
        mapPrefs.updateHorizonPrefs { it.copy(precisionCircles = on) }
    }

    fun setObserver(observer: HorizonObserver) = change { it.copy(observer = observer) }

    /** Picks the antenna height by hand (switches the automatic one off). */
    fun setAntenna(metres: Int) = change { it.copy(antennaM = metres, antennaAuto = false) }

    fun setAntennaAuto() = change { it.copy(antennaAuto = true) }

    fun setTargetAntenna(metres: Int) = change { it.copy(targetAntennaM = metres) }

    fun setRefraction(k: Double) = change { it.copy(refraction = k) }

    fun setBuildings(on: Boolean) = change { it.copy(buildings = on) }

    fun setBuildingsRadius(km: Int) = _state.update { it.copy(buildingsRadiusKm = km, buildingsOk = null) }

    /**
     * Downloads the buildings around the connected radio, up to the chosen range, and keeps them on the device, so
     * the panorama reads them from there from now on.
     */
    fun saveBuildings() {
        val here = _state.value
        // From the connected radio, whichever place the panorama is looked from; the place looked from is the fallback.
        val centre =
            nodeSpot(nodeRepository.ourNodeInfo.value)?.at
                ?: here.observerLat?.let { lat -> here.observerLon?.let { lon -> GeoPoint(lat, lon) } }
                ?: return
        if (here.buildingsSaving) return
        viewModelScope.launch {
            _state.update { it.copy(buildingsSaving = true, buildingsOk = null) }
            val ok =
                try {
                    val map =
                        clutter.buildingsForArea(
                            centre,
                            here.buildingsRadiusKm.toDouble(),
                            tallOnly = here.buildingsRadiusKm > ALL_BUILDINGS_MAX_KM,
                        )
                    val buildings = map.polygons.filter { it.kind == ClutterKind.BUILDING }
                    withContext(dispatchers.io) { buildingStore.write(centre, here.buildingsRadiusKm, buildings) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: PlannerClutterException) {
                    false
                }
            val bytes = withContext(dispatchers.io) { buildingStore.sizeBytes() }
            _state.update { it.copy(buildingsSaving = false, buildingsOk = ok, buildingsBytes = bytes) }
            if (ok && _state.value.buildings) reload()
        }
    }

    /** Deletes the buildings saved on the device. */
    fun clearSavedBuildings() {
        viewModelScope.launch {
            withContext(dispatchers.io) { buildingStore.clear() }
            _state.update { it.copy(buildingsBytes = 0L, buildingsHere = false, buildingsOk = null) }
            if (_state.value.buildings) reload()
        }
    }

    fun retry() = reload()

    fun setSaveRadius(km: Int) = _state.update { it.copy(saveRadiusKm = km, saveOk = null) }

    /**
     * Saves the terrain around the place looked from, up to the chosen range (100 km at most), for use without the
     * internet.
     */
    fun saveTerrain() {
        val here = _state.value
        val lat = here.observerLat ?: return
        val lon = here.observerLon ?: return
        if (here.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, saveDone = 0, saveTotal = 0, saveOk = null) }
            val ok =
                try {
                    elevation.saveArea(GeoPoint(lat, lon), here.saveRadiusKm.toDouble()) { done, total ->
                        _state.update { it.copy(saveDone = done, saveTotal = total) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: PlannerElevationException) {
                    false
                }
            val bytes = withContext(dispatchers.io) { terrainStore.sizeBytes() }
            _state.update { it.copy(saving = false, saveOk = ok, savedBytes = bytes) }
        }
    }

    /** Deletes the terrain saved on the device. */
    fun clearSavedTerrain() {
        viewModelScope.launch {
            withContext(dispatchers.io) { terrainStore.clear() }
            _state.update { it.copy(savedBytes = 0L, saveOk = null) }
        }
    }

    /**
     * The direction (true north, degrees) the phone faces, following its turning; nothing while it has no sensor. The
     * sensor runs only while this is collected.
     */
    val heading: Flow<Float?> =
        poseProvider.poseUpdates().conflate().map { reading ->
            val azimuth = reading.pose?.azimuthDeg ?: return@map null
            val here = _state.value
            val declination =
                if (reading.trueNorth || here.observerLat == null || here.observerLon == null) {
                    0.0
                } else {
                    magneticFieldProvider.getDeclination(here.observerLat, here.observerLon, 0.0, nowMillis).toDouble()
                }
            (((azimuth + declination) % FULL_TURN_DEG + FULL_TURN_DEG) % FULL_TURN_DEG).toFloat()
        }

    private fun change(transform: (TerrainHorizonState) -> TerrainHorizonState) {
        val before = _state.value
        val after = transform(before)
        if (after == before) return
        _state.value = after
        mapPrefs.updateHorizonPrefs {
            HorizonPrefs(
                radiusKm = after.radiusKm,
                nodeRangeKm = after.nodeRangeKm,
                precisionCircles = after.precisionCircles,
                onlyFavorites = after.onlyFavorites,
                antennaAuto = after.antennaAuto,
                antennaM = after.antennaM,
                targetAntennaM = after.targetAntennaM,
                refraction = after.refraction,
            )
        }
        reload()
    }

    private fun reload() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, failure = null) }
            val settings = _state.value
            val spot = spot(settings.observer)
            _state.update { it.copy(nodeChoices = nodeChoices()) }
            if (spot == null) {
                _state.update {
                    it.copy(
                        loading = false,
                        failure = TerrainHorizonFailure.NO_POSITION,
                        profile = null,
                        targets = emptyList(),
                        observerName = "",
                    )
                }
                return@launch
            }
            val nodes = nodes(settings.focusNodeNum, spot.nodeNum, settings.onlyFavorites)
            var radiusKm = settings.radiusKm
            // A node asked for that lies beyond the range gets the smallest range that reaches it.
            if (settings.focusNodeNum != null) {
                nodes.firstOrNull()?.let { (_, _, at) ->
                    val need =
                        HORIZON_RADII_KM.firstOrNull { it * METRES_PER_KM >= Geodesy.distanceM(spot.at, at) }
                            ?: HORIZON_RADII_KM.last()
                    if (need > radiusKm) radiusKm = need
                }
            }
            try {
                val ground = elevation.prepareArea(spot.at, radiusKm.toDouble())
                val auto =
                    spot.altitudeM
                        ?.let { it - ground(spot.at.lat, spot.at.lon) }
                        ?.takeIf { it in MIN_AUTO_HEIGHT_M..MAX_AUTO_HEIGHT_M }
                val height = if (settings.antennaAuto && auto != null) auto else settings.antennaM.toDouble()
                // Buildings come from the device, never from the internet while the panorama opens.
                val savedBuildings = withContext(dispatchers.io) { buildingStore.load(spot.at) }
                val skyline = if (settings.buildings) withBuildings(ground, savedBuildings, spot.at) else ground
                val (profile, targets) =
                    withContext(dispatchers.default) {
                        val built =
                            HorizonPanorama.build(
                                skyline,
                                spot.at,
                                height,
                                radiusKm * METRES_PER_KM,
                                k = settings.refraction,
                            )
                        built to
                            HorizonPanorama.place(
                                built,
                                skyline,
                                nodes,
                                settings.targetAntennaM.toDouble(),
                                settings.refraction,
                                maxOf(settings.nodeRangeKm, radiusKm) * METRES_PER_KM,
                            )
                    }
                _state.update {
                    it.copy(
                        loading = false,
                        profile = profile,
                        targets = targets,
                        radiusKm = radiusKm,
                        observerName = spot.name,
                        observerLat = spot.at.lat,
                        observerLon = spot.at.lon,
                        autoAntennaM = auto?.let { a -> kotlin.math.round(a).toInt() },
                        usedAntennaM = kotlin.math.round(height).toInt(),
                        buildingsHere = !savedBuildings.isEmpty(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: PlannerElevationException) {
                _state.update {
                    it.copy(
                        loading = false,
                        failure = TerrainHorizonFailure.NETWORK,
                        profile = null,
                        targets = emptyList(),
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // Any other failure (a tile that cannot be read, say) must end the loading, never leave it spinning.
                _state.update {
                    it.copy(
                        loading = false,
                        failure = TerrainHorizonFailure.NETWORK,
                        profile = null,
                        targets = emptyList(),
                    )
                }
            }
        }
    }

    private suspend fun spot(observer: HorizonObserver): Spot? = when (observer) {
        HorizonObserver.MyNode -> nodeSpot(nodeRepository.ourNodeInfo.value) ?: phoneSpot()
        HorizonObserver.Phone -> phoneSpot() ?: nodeSpot(nodeRepository.ourNodeInfo.value)
        is HorizonObserver.OtherNode -> nodeSpot(nodeRepository.nodeDBbyNum.value[observer.num])
        is HorizonObserver.Point -> Spot(GeoPoint(observer.lat, observer.lon), "", null)
    }

    private fun nodeSpot(node: Node?): Spot? = node
        ?.takeIf { it.validPosition != null }
        ?.let {
            Spot(
                GeoPoint(it.latitude, it.longitude),
                it.user.short_name.ifBlank { it.user.long_name },
                it.position.altitude?.takeIf { a -> a != 0 }?.toDouble(),
                it.num,
            )
        }

    /**
     * This phone's fix, when the permission is given and there is one; null otherwise (it does not ask for the
     * permission).
     */
    private suspend fun phoneSpot(): Spot? {
        val fix =
            withTimeoutOrNull(LOCATION_WAIT_MS) {
                phoneLocationProvider.locationUpdates().first { it.location != null || !it.permissionGranted }
            }
                ?.location ?: return null
        return Spot(GeoPoint(fix.latitude, fix.longitude), "", fix.mslAltitude)
    }

    private fun nodeChoices(): List<HorizonNodeChoice> = nodeRepository.nodeDBbyNum.value.values
        .filter { it.validPosition != null }
        .map {
            HorizonNodeChoice(
                it.num,
                it.user.long_name.ifBlank { it.user.short_name },
                it.user.short_name,
                it.latitude,
                it.longitude,
                it.colors.second,
                it.colors.first,
                precisionRadiusMetersOrNull(it.position.precision_bits),
                it.isFavorite,
            )
        }
        .sortedBy { it.name.lowercase() }

    private fun nodes(focus: Int?, lookingFrom: Int?, onlyFavorites: Boolean): List<Triple<Int, String, GeoPoint>> =
        nodeRepository.nodeDBbyNum.value.values
            .filter {
                it.num != lookingFrom &&
                    it.validPosition != null &&
                    (focus == null || it.num == focus) &&
                    (!onlyFavorites || focus != null || it.isFavorite)
            }
            .map { node ->
                Triple(
                    node.num,
                    node.user.short_name.ifBlank { node.user.long_name },
                    GeoPoint(node.latitude, node.longitude),
                )
            }
}

/**
 * The ground with the height of the saved buildings added, so they stand in the skyline like hills do. The buildings
 * right at [at] are left out: the place looked from stands on or beside them, not behind them.
 */
private fun withBuildings(
    ground: (lat: Double, lon: Double) -> Double,
    buildings: ClutterMap,
    at: GeoPoint,
): (Double, Double) -> Double {
    if (buildings.isEmpty()) return ground
    val heights = ClutterHeights()
    val metresPerLon = METRES_PER_DEGREE * kotlin.math.cos(at.lat * kotlin.math.PI / HALF_TURN_DEG)
    return { lat, lon ->
        val dy = (lat - at.lat) * METRES_PER_DEGREE
        val dx = (lon - at.lon) * metresPerLon
        val own = dx * dx + dy * dy < OWN_BUILDING_M * OWN_BUILDING_M
        ground(lat, lon) + if (own) 0.0 else buildings.heightAt(lat, lon, heights)
    }
}

