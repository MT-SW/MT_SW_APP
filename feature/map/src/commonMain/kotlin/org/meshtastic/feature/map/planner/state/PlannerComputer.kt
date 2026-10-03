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
package org.meshtastic.feature.map.planner.state

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.feature.map.planner.Atmosphere
import org.meshtastic.feature.map.planner.Coverage
import org.meshtastic.feature.map.planner.CoverageInput
import org.meshtastic.feature.map.planner.CoverageResult
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.Geodesy
import org.meshtastic.feature.map.planner.LinkBudget
import org.meshtastic.feature.map.planner.LinkEnd
import org.meshtastic.feature.map.planner.LinkInput
import org.meshtastic.feature.map.planner.LinkResult
import org.meshtastic.feature.map.planner.PathProfile
import org.meshtastic.feature.map.planner.ProfileSeries
import org.meshtastic.feature.map.planner.data.ClutterMap
import org.meshtastic.feature.map.planner.data.NoClutterSource
import org.meshtastic.feature.map.planner.data.PlannerClutter
import org.meshtastic.feature.map.planner.data.ClutterProgress
import org.meshtastic.feature.map.planner.data.PlannerClutterException
import org.meshtastic.feature.map.planner.data.PlannerClutterFailure
import org.meshtastic.feature.map.planner.data.PlannerClutterSource
import org.meshtastic.feature.map.planner.data.PlannerElevationException
import org.meshtastic.feature.map.planner.data.PlannerElevationFailure
import org.meshtastic.feature.map.planner.data.PlannerElevationSource
import org.meshtastic.feature.map.planner.data.PlannerWeatherResult
import org.meshtastic.feature.map.planner.data.PlannerWeatherSource
import org.meshtastic.feature.map.planner.data.PropagationConditions
import kotlin.math.max
import kotlin.math.min

/** Output of [PlannerComputer.compute]; the ViewModel merges it into [PlannerUiState]. */
data class PlannerResults(
    val link: LinkResult?,
    val series: ProfileSeries?,
    /** Terrain heights at the ends from the terrain model (null when not available). */
    val groundAltA: Double?,
    val groundAltB: Double?,
    val weather: PlannerWeatherStatus,
    val kFactor: Double,
    val surfaceRefractivity: Double,
    val atmosphericLossDb: Double,
    val comparison: MeasuredComparison?,
    val error: PlannerError?,
    val clutter: PlannerClutterStatus = PlannerClutterStatus.Idle,
)

/**
 * The calculation pipeline as a plain suspend function (no ViewModel involved, so it is testable with fakes):
 * terrain -> optional weather -> ITM link budget -> chart series -> comparison with a measured node.
 *
 * @param computeDispatcher where the CPU heavy ITM runs.
 * @param clockMs time source for the weather cache.
 * @param clutter buildings and forests, asked only when [PlannerUiState.preciseTerrain] is on.
 */
class PlannerComputer(
    private val elevation: PlannerElevationSource,
    private val weather: PlannerWeatherSource,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clockMs: () -> Long = { nowMillis },
    private val clutter: PlannerClutterSource = NoClutterSource,
) {
    /** Progress of a large OpenStreetMap download (null when none runs); shown while the coverage is being prepared. */
    val clutterProgress: StateFlow<ClutterProgress?> get() = clutter.progress

    private var cachedKey: String? = null
    private var cachedAtMs: Long = 0L
    private var cachedResult: PlannerWeatherResult? = null

    /** Computes everything for [state]. Never throws except for cancellation. */
    suspend fun compute(state: PlannerUiState, nodes: List<PlannerNodeOption> = emptyList()): PlannerResults {
        val pa = state.a.point
        val pb = state.b.point
        if (pa == null || pb == null) return incomplete(pa, pb)
        if (Geodesy.distanceM(pa, pb) < MIN_DISTANCE_M) {
            return empty(PlannerError.POINTS_TOO_CLOSE)
        }

        // Weather first: failures fall back silently to the standard atmosphere.
        var status: PlannerWeatherStatus = PlannerWeatherStatus.Idle
        var k = PlannerUiState.DEFAULT_K
        var n0 = PlannerUiState.DEFAULT_N0
        var conditions: PropagationConditions? = null
        if (state.useWeather) {
            val mid = Geodesy.interpolate(pa, pb, 0.5)
            when (val r = fetchWeather(mid)) {
                is PlannerWeatherResult.Success -> {
                    conditions = r.conditions
                    status = PlannerWeatherStatus.Ready(r.conditions)
                    k = r.conditions.analysis.kFactor
                    n0 = r.conditions.analysis.seaLevelN
                }
                is PlannerWeatherResult.Failure -> status = PlannerWeatherStatus.Failed(r.error)
            }
        }

        val profile: PathProfile = try {
            elevation.profile(pa, pb)
        } catch (e: CancellationException) {
            throw e
        } catch (e: PlannerElevationException) {
            return empty(mapElevation(e), status, k, n0)
        } catch (e: Exception) {
            return empty(PlannerError.COMPUTE_FAILED, status, k, n0)
        }

        val n = profile.intervals
        val autoA = profile.groundM[0]
        val autoB = profile.groundM[n]
        // Manual ground heights replace the end samples of the profile.
        val ground = profile.groundM.copyOf()
        if (state.a.groundAltManual) state.a.groundAltM?.let { ground[0] = it }
        if (state.b.groundAltManual) state.b.groundAltM?.let { ground[n] = it }
        // Optional real obstacles (OpenStreetMap). A failure falls back to the preset, like the weather does.
        var clutterStatus: PlannerClutterStatus = PlannerClutterStatus.Idle
        if (state.preciseTerrain) {
            try {
                val map = clutter.forLink(pa, pb)
                val heights = state.clutterHeights
                val totalM = profile.stepM * n
                val clearM = PlannerClutter.CLEAR_AROUND_ANTENNA_M
                val withObstacles = withContext(computeDispatcher) {
                    PlannerClutter.withClutter(
                        ground = ground,
                        stepM = profile.stepM,
                        clutterAt = { i ->
                            val p = Geodesy.interpolate(pa, pb, if (totalM > 0.0) i * profile.stepM / totalM else 0.0)
                            map.heightAt(p.lat, p.lon, heights)
                        },
                        clearStartM = clearM,
                        clearEndM = clearM,
                    )
                }
                withObstacles.copyInto(ground)
                clutterStatus = PlannerClutterStatus.Ready(map.stats)
            } catch (e: CancellationException) {
                throw e
            } catch (e: PlannerClutterException) {
                clutterStatus = PlannerClutterStatus.Failed(e.failure, e.detail)
            } catch (e: Exception) {
                clutterStatus = PlannerClutterStatus.Failed(
                    org.meshtastic.feature.map.planner.data.PlannerClutterFailure.BAD_RESPONSE,
                )
            }
        }
        val used = PathProfile(profile.stepM, ground)
        val extraDb = state.copy(clutter = clutterStatus).effectiveExtraLossDb

        val distKm = used.distanceM / 1000.0
        val atmo = atmosphericLossDb(state.frequencyMHz, conditions, distKm)

        val input = LinkInput(
            a = linkEnd(state.a, pa, ground[0]),
            b = linkEnd(state.b, pb, ground[n]),
            frequencyMHz = state.frequencyMHz,
            bandwidthKhz = state.bandwidthKhz,
            spreadingFactor = state.spreadingFactor,
            noiseFigureDb = state.noiseFigureDb,
            kFactor = k,
            surfaceRefractivity = n0,
            extraLossDb = extraDb + atmo,
        )
        return try {
            val (link, series) = withContext(computeDispatcher) {
                val l = LinkBudget.analyze(input, used)
                l to LinkBudget.series(input, used)
            }
            PlannerResults(
                link = link,
                series = series,
                groundAltA = autoA,
                groundAltB = autoB,
                weather = status,
                kFactor = k,
                surfaceRefractivity = n0,
                atmosphericLossDb = atmo,
                comparison = compareMeasured(state, link, nodes),
                error = null,
                clutter = clutterStatus,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            empty(PlannerError.COMPUTE_FAILED, status, k, n0)
        }
    }

    /**
     * Coverage around the end [PlannerUiState.coverageSide] using the state's radio, environment (k and N as of the
     * last [compute]) and coverage settings. Throws [PlannerElevationException] when terrain is unavailable.
     */
    suspend fun computeCoverage(
        state: PlannerUiState,
        onClutterFailure: (PlannerClutterFailure, String?) -> Unit = { _, _ -> },
        onProgress: (Float) -> Unit = {},
    ): CoverageResult {
        val end = state.end(state.coverageSide)
        val center = end.point ?: throw IllegalArgumentException("coverage side has no point")
        val sampler = elevation.prepareArea(center, state.coverageMaxRangeKm)
        var clutterMap: ClutterMap? = null
        if (state.preciseTerrain) {
            // Only within the radius OpenStreetMap data is requested for; farther out the preset loss still applies.
            clutterMap = try {
                clutter.forArea(center, min(state.coverageMaxRangeKm, state.clutterRadiusKm))
            } catch (e: CancellationException) {
                throw e
            } catch (e: PlannerClutterException) {
                onClutterFailure(e.failure, e.detail)
                null
            } catch (e: Exception) {
                onClutterFailure(PlannerClutterFailure.BAD_RESPONSE, null)
                null
            }
        }
        val heights = state.clutterHeights
        val clutterAt: ((Double, Double) -> Double)? = clutterMap?.let { m -> { lat, lon -> m.heightAt(lat, lon, heights) } }
        val manualGround = end.groundAltM
        val ground = if (end.groundAltManual && manualGround != null) manualGround else sampler(center.lat, center.lon)
        val input = CoverageInput(
            center = center,
            groundAltM = ground,
            antennaHeightM = end.antennaHeightM,
            txPowerDbm = end.txPowerDbm,
            antennaGainDbi = end.antennaGainDbi,
            feederLossDb = end.feederLossDb,
            rxAntennaHeightM = state.coverageRxHeightM,
            rxGainDbi = state.coverageRxGainDbi,
            frequencyMHz = state.frequencyMHz,
            bandwidthKhz = state.bandwidthKhz,
            spreadingFactor = state.spreadingFactor,
            noiseFigureDb = state.noiseFigureDb,
            kFactor = state.kFactor,
            surfaceRefractivity = state.surfaceRefractivity,
            extraLossDb = if (clutterMap != null) state.effectiveExtraLossDb else state.extraLossDb,
            maxRangeKm = state.coverageMaxRangeKm,
            radials = state.coverageRadials,
            rangeSteps = (state.coverageMaxRangeKm * 2.0).toInt().coerceIn(60, 150),
        )
        return withContext(computeDispatcher) { Coverage.compute(input, sampler, clutterAt = clutterAt, onProgress = onProgress) }
    }

    // ---- helpers ----

    private suspend fun incomplete(pa: GeoPoint?, pb: GeoPoint?): PlannerResults {
        // Best effort ground heights for the ends that exist; failures are ignored here.
        val ga = if (pa != null) altitudeOrNull(pa) else null
        val gb = if (pb != null) altitudeOrNull(pb) else null
        return PlannerResults(
            null, null, ga, gb, PlannerWeatherStatus.Idle, PlannerUiState.DEFAULT_K, PlannerUiState.DEFAULT_N0,
            0.0, null, null,
        )
    }

    private suspend fun altitudeOrNull(p: GeoPoint): Double? = try {
        elevation.altitudeAt(p.lat, p.lon)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun empty(
        error: PlannerError,
        status: PlannerWeatherStatus = PlannerWeatherStatus.Idle,
        k: Double = PlannerUiState.DEFAULT_K,
        n0: Double = PlannerUiState.DEFAULT_N0,
    ): PlannerResults = PlannerResults(null, null, null, null, status, k, n0, 0.0, null, error)

    private fun mapElevation(e: PlannerElevationException): PlannerError = when (e.failure) {
        PlannerElevationFailure.NETWORK -> PlannerError.ELEVATION_OFFLINE
        PlannerElevationFailure.DECODE -> PlannerError.ELEVATION_DECODE
    }

    private fun linkEnd(end: PlannerEnd, point: GeoPoint, groundM: Double): LinkEnd = LinkEnd(
        point = point,
        groundAltM = groundM,
        antennaHeightM = end.antennaHeightM,
        txPowerDbm = end.txPowerDbm,
        antennaGainDbi = end.antennaGainDbi,
        feederLossDb = end.feederLossDb,
    )

    private suspend fun fetchWeather(p: GeoPoint): PlannerWeatherResult {
        val key = (round2(p.lat)).toString() + "," + round2(p.lon).toString()
        val now = clockMs()
        val c = cachedResult
        if (c != null && cachedKey == key && now - cachedAtMs < WEATHER_CACHE_MS) return c
        val r = weather.fetch(p.lat, p.lon)
        if (r is PlannerWeatherResult.Success) {
            cachedKey = key
            cachedAtMs = now
            cachedResult = r
        }
        return r
    }

    /** Drops the cached weather (used by "refresh weather"). */
    fun invalidateWeather() {
        cachedResult = null
        cachedKey = null
    }

    private fun round2(v: Double): Long = kotlin.math.round(v * 100.0).toLong()

    private fun atmosphericLossDb(fMHz: Double, c: PropagationConditions?, distKm: Double): Double {
        if (fMHz < 1000.0) return 0.0
        val t = c?.tempC ?: STD_TEMP_C
        val p = c?.pressureHpa ?: STD_PRESSURE_HPA
        val rh = c?.rhPct ?: STD_RH_PCT
        val rain = c?.rainMmH ?: 0.0
        val perKm = Atmosphere.gasAttenuationDbPerKm(fMHz, t, p, rh) + Atmosphere.rainAttenuationDbPerKm(fMHz, rain)
        return max(0.0, perKm * distKm)
    }

    companion object {
        /**
         * Prediction versus the last measurement: applies when one end is a mesh node (not ours) with a measurement and
         * the other end is our station. Pure, so the ViewModel can refresh it cheaply when node data changes.
         */
        fun compareMeasured(state: PlannerUiState, link: LinkResult, nodes: List<PlannerNodeOption>): MeasuredComparison? {
            val ourNum = nodes.firstOrNull { it.isOurs }?.num
            fun isOurs(end: PlannerEnd): Boolean {
                val s = end.pointSource
                return s is PlannerPointSource.Station || (s is PlannerPointSource.Node && s.num == ourNum)
            }
            for (side in listOf(PlannerSide.A, PlannerSide.B)) {
                val tx = state.end(side)
                val rx = state.end(side.other)
                val src = tx.pointSource
                if (src !is PlannerPointSource.Node || src.num == ourNum || !isOurs(rx)) continue
                val node = nodes.firstOrNull { it.num == src.num } ?: continue
                val dir = if (side == PlannerSide.A) link.aToB else link.bToA
                val floor = state.noiseFloorDbm
                val predSnr = dir.rxPowerDbm - floor
                val rssi = node.rssiDbm
                val snr = node.snrDb
                return MeasuredComparison(
                    nodeNum = node.num,
                    nodeName = node.displayName,
                    nodeSide = side,
                    direct = node.hopsAway == 0,
                    predictedRssiDbm = dir.rxPowerDbm,
                    predictedSnrDb = predSnr,
                    noiseFloorDbm = floor,
                    measuredRssiDbm = rssi,
                    measuredSnrDb = snr,
                    rssiDeltaDb = if (rssi != null) rssi - dir.rxPowerDbm else null,
                    snrDeltaDb = if (snr != null) snr - predSnr else null,
                )
            }
            return null
        }

        const val MIN_DISTANCE_M = 10.0
        private const val WEATHER_CACHE_MS = 15 * 60 * 1000L
        private const val STD_TEMP_C = 15.0
        private const val STD_PRESSURE_HPA = 1013.0
        private const val STD_RH_PCT = 60.0
    }
}
