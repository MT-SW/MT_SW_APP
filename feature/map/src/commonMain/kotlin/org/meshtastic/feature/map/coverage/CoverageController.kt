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
package org.meshtastic.feature.map.coverage

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.formatString
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.model.util.precisionRadiusMetersOrNull
import org.meshtastic.core.repository.MapPrefs
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.feature.map.planner.Coverage
import org.meshtastic.feature.map.planner.CoverageInput
import org.meshtastic.feature.map.planner.CoverageLayer
import org.meshtastic.feature.map.planner.CoverageResult
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.Geodesy
import org.meshtastic.feature.map.planner.data.PlannerElevationException
import org.meshtastic.feature.map.planner.data.PlannerElevationSource
import org.meshtastic.feature.map.planner.state.PlannerUiState

/** The signal margins (dB above what the radio can still decode) the layer colours, weakest first. */
val COVERAGE_LEVELS_DB: List<Float> = listOf(0f, 6f, 12f, 18f, 24f)

/** `0xRRGGBB` of the planner's colour for a signal margin of [marginDb]. */
fun coverageRgb(marginDb: Float): Int =
    CoverageLayer.colorOf(CoverageLayer.classOf(marginDb)).removePrefix("#").toInt(16)

/**
 * The estimated reach of one node: for every compass direction (radial i points at 360 * i / radials degrees) how far
 * a signal of each of the [COVERAGE_LEVELS_DB] still arrives, in metres. [latitude] and [longitude] are where the reach
 * is counted from, which for a node with a rounded position is the highest ground nearby, not the reported spot.
 */
data class CoverageArea(
    val nodeNum: Int,
    val latitude: Double,
    val longitude: Double,
    val lastHeard: Int,
    val reachM: List<List<Double>>,
)

/** A node that has a position, for the node filter. */
data class CoverageSource(val nodeNum: Int, val name: String)

/** What the coverage layer of the main map shows. */
data class CoverageState(
    val enabled: Boolean = false,
    val loading: Boolean = false,
    /** How many nodes are done and how many there are to do, while [loading]. */
    val done: Int = 0,
    val toDo: Int = 0,
    /** The reach of the nodes the filter lets through. */
    val areas: List<CoverageArea> = emptyList(),
    /** How many nodes have a reach before the filter. */
    val total: Int = 0,
    /** The nodes that have a position, by name. */
    val sources: List<CoverageSource> = emptyList(),
    /** The filter on when a node was last heard, in days; 0 shows all of them. */
    val days: Int = 0,
    /** The filter on one node; null shows all nodes. */
    val nodeFilter: Int? = null,
    /** When the reach was last counted, in milliseconds; 0 when it was not yet. */
    val readMillis: Long = 0L,
)

/**
 * The coverage layer of the main map: the estimated reach of every node that has a position.
 *
 * The reach is counted like in the link planner (terrain, no buildings or forests) for a typical node: the planner's
 * default antenna, power and radio settings. It is counted when the layer is switched on and on request, and kept, so a
 * node is counted again only when it has moved. A node that reports a rounded position is counted from the highest
 * ground within the rounding (at most [MAX_SNAP_M]), because such a node most likely stands on a peak and not on the
 * slope next to it.
 */
@Single
class CoverageController(
    private val nodeRepository: NodeRepository,
    private val elevation: PlannerElevationSource,
    private val mapPrefs: MapPrefs,
    private val dispatchers: CoroutineDispatchers,
) {
    private val mutable = MutableStateFlow(CoverageState())
    val state: StateFlow<CoverageState> = mutable.asStateFlow()
    private var restored = false
    private var all: List<CoverageArea> = emptyList()
    private val counted = HashMap<String, CoverageArea>()
    private val radio = PlannerUiState()

    /** Switches the layer on or off and remembers it, so it is still on after the app is opened again. */
    fun setEnabled(on: Boolean) {
        restored = true
        mutable.update { it.copy(enabled = on) }
        mapPrefs.setCoverageEnabled(on)
    }

    /** Takes over the saved settings, once, when the map is first shown. */
    suspend fun restore() {
        if (restored) return
        restored = true
        val saved = mapPrefs.awaitCoverageEnabled()
        val days = mapPrefs.awaitCoverageDays()
        val node = mapPrefs.awaitCoverageNode()
        mutable.update { it.copy(enabled = saved, days = days, nodeFilter = node) }
    }

    /** Counts the reach again. A count that is already going on is not started a second time. */
    @Suppress("TooGenericExceptionCaught")
    suspend fun refresh() {
        if (mutable.value.loading) return
        val nodes = nodeRepository.nodeDBbyNum.value.values.filter { it.validPosition != null }
        mutable.update { state ->
            val sources = nodes.map { CoverageSource(it.num, nameOf(it)) }.sortedBy { it.name.lowercase() }
            val chosen = state.nodeFilter?.takeIf { saved -> sources.any { it.nodeNum == saved } }
            state.copy(loading = true, done = 0, toDo = nodes.size, sources = sources, nodeFilter = chosen)
        }
        val shown = ArrayList<CoverageArea>()
        try {
            for ((index, node) in nodes.withIndex()) {
                try {
                    shown += areaOf(node)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: PlannerElevationException) {
                    Logger.w(e) { "Coverage: no terrain data, the count is stopped" }
                    break
                } catch (e: Exception) {
                    Logger.w(e) { "Coverage: node ${node.num} skipped" }
                }
                all = shown.toList()
                mutable.update { it.copy(done = index + 1, readMillis = nowMillis) }
                applyFilter()
            }
            Logger.i { "Coverage: the reach of ${shown.size} of ${nodes.size} nodes is counted" }
        } finally {
            mutable.update { it.copy(loading = false) }
        }
    }

    /** Shows only the nodes heard in the last [days] days (0: all of them). */
    fun setDays(days: Int) {
        mutable.update { it.copy(days = days) }
        applyFilter()
        mapPrefs.setCoverageFilter(days, mutable.value.nodeFilter)
    }

    /** Shows only [nodeNum] (null: all nodes). */
    fun setNode(nodeNum: Int?) {
        mutable.update { it.copy(nodeFilter = nodeNum) }
        applyFilter()
        mapPrefs.setCoverageFilter(mutable.value.days, nodeNum)
    }

    private fun applyFilter() {
        mutable.update { state ->
            val since = if (state.days > 0) nowMillis / MILLIS_PER_SECOND - state.days * SECONDS_PER_DAY else 0L
            val shown =
                all.filter { (state.nodeFilter == null || it.nodeNum == state.nodeFilter) && it.lastHeard >= since }
            state.copy(areas = shown, total = all.size)
        }
    }

    private fun nameOf(node: Node): String = node.effectiveLongName.ifBlank { formatString("!%08x", node.num) }

    private suspend fun areaOf(node: Node): CoverageArea {
        val key = "${node.num}:${(node.latitude * PLACE_GRID).toLong()}:${(node.longitude * PLACE_GRID).toLong()}"
        val known = counted[key]
        if (known != null) return known.copy(lastHeard = node.lastHeard)
        val reported = GeoPoint(node.latitude, node.longitude)
        val ground = elevation.prepareArea(reported, RANGE_KM)
        val roundedWithin = precisionRadiusMetersOrNull(node.position.precision_bits)
        val center = if (roundedWithin != null) highestPoint(reported, roundedWithin, ground) else reported
        val result = withContext(dispatchers.default) { Coverage.compute(inputAt(center, ground), ground) }
        val area =
            CoverageArea(
                nodeNum = node.num,
                latitude = center.lat,
                longitude = center.lon,
                lastHeard = node.lastHeard,
                reachM = COVERAGE_LEVELS_DB.map { result.reach(it) },
            )
        counted[key] = area
        return area
    }

    /** The highest ground within [radiusM] of [around] (at most [MAX_SNAP_M]); [around] when nothing is higher. */
    private fun highestPoint(around: GeoPoint, radiusM: Double, ground: (Double, Double) -> Double): GeoPoint {
        val reach = minOf(radiusM, MAX_SNAP_M)
        val step = maxOf(reach / SNAP_RINGS, MIN_SNAP_STEP_M)
        var best = around
        var bestHeight = ground(around.lat, around.lon)
        var distance = step
        while (distance <= reach) {
            val spots = maxOf(SNAP_MIN_SPOTS, (SNAP_SPOTS_PER_KM * distance / METRES_PER_KM).toInt())
            for (i in 0 until spots) {
                val spot = Geodesy.destination(around, FULL_TURN_DEG * i / spots, distance)
                val height = ground(spot.lat, spot.lon)
                if (height > bestHeight) {
                    best = spot
                    bestHeight = height
                }
            }
            distance += step
        }
        return best
    }

    private fun inputAt(center: GeoPoint, ground: (Double, Double) -> Double): CoverageInput {
        val end = radio.a
        return CoverageInput(
            center = center,
            groundAltM = ground(center.lat, center.lon),
            antennaHeightM = end.antennaHeightM,
            txPowerDbm = end.txPowerDbm,
            antennaGainDbi = end.antennaGainDbi,
            feederLossDb = end.feederLossDb,
            rxAntennaHeightM = radio.coverageRxHeightM,
            rxGainDbi = radio.coverageRxGainDbi,
            frequencyMHz = radio.frequencyMHz,
            bandwidthKhz = radio.bandwidthKhz,
            spreadingFactor = radio.spreadingFactor,
            noiseFigureDb = radio.noiseFigureDb,
            kFactor = radio.kFactor,
            surfaceRefractivity = radio.surfaceRefractivity,
            extraLossDb = radio.extraLossDb,
            maxRangeKm = RANGE_KM,
            radials = RADIALS,
            stepM = STEP_M,
            rangeSteps = RINGS,
        )
    }

    /** Per radial, the distance of the farthest ring that still has at least [levelDb] of margin (0 when none). */
    private fun CoverageResult.reach(levelDb: Float): List<Double> = List(radials) { radial ->
        var far = 0.0
        for (ring in ringsM.indices) {
            if (marginDb[radial][ring] >= levelDb) far = ringsM[ring]
        }
        far
    }

    private companion object {
        const val RANGE_KM = 20.0
        const val RADIALS = 36
        const val RINGS = 20
        const val STEP_M = 300.0
        const val PLACE_GRID = 1_000 // about 100 m
        const val MAX_SNAP_M = 2_500.0
        const val MIN_SNAP_STEP_M = 40.0
        const val SNAP_RINGS = 8
        const val SNAP_MIN_SPOTS = 8
        const val SNAP_SPOTS_PER_KM = 24
        const val METRES_PER_KM = 1_000.0
        const val FULL_TURN_DEG = 360.0
        const val MILLIS_PER_SECOND = 1_000L
        const val SECONDS_PER_DAY = 86_400L
    }
}
