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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.feature.map.planner.CoverageLayer
import org.meshtastic.feature.map.planner.FeederBuilder
import org.meshtastic.feature.map.planner.FeederConfig
import org.meshtastic.feature.map.planner.PlannerBands
import org.meshtastic.feature.map.planner.data.ClutterHeights
import org.meshtastic.feature.map.planner.data.PlannerClutter
import org.meshtastic.feature.map.planner.data.PlannerClutterSource
import org.meshtastic.feature.map.planner.data.PlannerElevationException
import org.meshtastic.feature.map.planner.data.PlannerElevationFailure
import org.meshtastic.feature.map.planner.data.PlannerElevationSource
import org.meshtastic.feature.map.planner.data.PlannerWeatherSource
import org.meshtastic.feature.map.planner.export.PlannerCsv
import org.meshtastic.feature.map.planner.export.PlannerGeoJson
import org.meshtastic.feature.map.planner.export.PlannerKml
import org.meshtastic.feature.map.planner.export.PlannerPdf
import org.meshtastic.feature.map.planner.export.PlannerProfileImage
import org.meshtastic.feature.map.planner.export.PlannerReport
import org.meshtastic.feature.map.planner.plannerRadioFromPreset
import org.meshtastic.proto.Config.LoRaConfig.ModemPreset

/**
 * State holder of the native MT_SW Planner. Register scope: the host screen (Koin viewModel), so the state survives the
 * planner sheet being dismissed while the host stays.
 *
 * Usage: collect [uiState] and [nodesWithPosition]; call the setters; every change that affects the result triggers a
 * debounced recalculation ([DEBOUNCE_MS]). Coverage is computed on demand with [computeCoverage]. Exports are
 * available through [buildReport] and the `export*` functions.
 */
@KoinViewModel
@Suppress("TooManyFunctions")
class PlannerViewModel(
    elevation: PlannerElevationSource,
    weather: PlannerWeatherSource,
    nodeSource: PlannerNodeSource,
    clutter: PlannerClutterSource,
) : ViewModel() {

    /** Replaceable in tests (e.g. to change the compute dispatcher). */
    internal var computer: PlannerComputer = PlannerComputer(elevation, weather, clutter = clutter)

    /** Debounce before a recalculation starts; tests may lower it. */
    internal var debounceMs: Long = DEBOUNCE_MS

    private val _uiState = MutableStateFlow(normalize(PlannerUiState()))

    /** The whole planner state. */
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    /** Mesh nodes that have a position (for the node pickers); [PlannerNodeOption.isOurs] marks the connected node. */
    val nodesWithPosition: StateFlow<List<PlannerNodeOption>> =
        nodeSource.nodes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var computeJob: Job? = null
    private var coverageJob: Job? = null

    init {
        // Keep the measured-vs-predicted comparison fresh when node data (SNR/RSSI) changes, without a full recompute.
        viewModelScope.launch {
            nodesWithPosition.collect { nodes ->
                _uiState.update { cur ->
                    val link = cur.link
                    if (link == null) cur else cur.copy(comparison = PlannerComputer.compareMeasured(cur, link, nodes))
                }
            }
        }
    }

    // ------------------------------------------------------------------ points

    /** Selects which side the UI edits (A/B tab). Does not recalculate. */
    fun selectSide(side: PlannerSide) = mutate(recompute = false) { it.copy(selectedSide = side) }

    /** Takes position and name from mesh node [nodeNum] (ignored when the node has no position). */
    fun setPointFromNode(side: PlannerSide, nodeNum: Int) {
        val node = nodesWithPosition.value.firstOrNull { it.num == nodeNum } ?: return
        mutate { st -> st.withEnd(side, placed(st.end(side), PlannerPointSource.Node(nodeNum), node.lat, node.lon, node.displayName)) }
    }

    /** Sets a point picked on the map. */
    fun setPointFromMap(side: PlannerSide, lat: Double, lon: Double) = setPoint(side, PlannerPointSource.Map, lat, lon)

    /** Sets a point typed by the user. */
    fun setPointManual(side: PlannerSide, lat: Double, lon: Double) = setPoint(side, PlannerPointSource.Manual, lat, lon)

    /** Uses the connected node; marks [PlannerHint.STATION_NO_POSITION] when it has no position or is not connected. */
    fun useStation(side: PlannerSide) {
        val ours = nodesWithPosition.value.firstOrNull { it.isOurs }
        if (ours == null) {
            mutate(recompute = false) { st -> st.withEnd(side, st.end(side).copy(stationUnavailable = true)) }
            return
        }
        mutate { st -> st.withEnd(side, placed(st.end(side), PlannerPointSource.Station, ours.lat, ours.lon, ours.displayName)) }
    }

    /** Removes the point of [side] (radio parameters of the end are kept). */
    fun clearPoint(side: PlannerSide) = mutate { st ->
        st.withEnd(
            side,
            st.end(side).copy(
                pointSource = PlannerPointSource.None,
                lat = null,
                lon = null,
                name = "",
                groundAltM = null,
                groundAltManual = false,
                stationUnavailable = false,
            ),
        )
    }

    fun setName(side: PlannerSide, name: String) = mutate(recompute = false) { st -> st.withEnd(side, st.end(side).copy(name = name)) }

    /** Manual ground height (m ASL); null returns to the terrain model value. */
    fun setGroundAlt(side: PlannerSide, meters: Double?) = mutate { st ->
        val e = st.end(side)
        st.withEnd(
            side,
            if (meters == null || meters.isNaN()) {
                e.copy(groundAltManual = false, groundAltM = null)
            } else {
                e.copy(groundAltManual = true, groundAltM = meters)
            },
        )
    }

    // ------------------------------------------------------------------ end parameters

    fun setAntennaHeight(side: PlannerSide, meters: Double) = editEnd(side, meters) { e, v -> e.copy(antennaHeightM = v.coerceIn(0.0, 2000.0)) }

    fun setTxPowerDbm(side: PlannerSide, dbm: Double) = editEnd(side, dbm) { e, v -> e.copy(txPowerDbm = v.coerceIn(-10.0, 50.0)) }

    /** Transmit power in watts (stored as dBm). */
    fun setTxPowerW(side: PlannerSide, watts: Double) =
        editEnd(side, watts) { e, v -> e.copy(txPowerDbm = PlannerEnd.wattsToDbm(v).coerceIn(-10.0, 50.0)) }

    fun setAntennaGain(side: PlannerSide, dbi: Double) = editEnd(side, dbi) { e, v -> e.copy(antennaGainDbi = v.coerceIn(-10.0, 60.0)) }

    /** Simple feeder loss in dB. Does not change [PlannerEnd.feederPrecise]; the value is kept when switching modes. */
    fun setFeederManualDb(side: PlannerSide, db: Double) = editEnd(side, db) { e, v -> e.copy(feederManualDb = v.coerceIn(0.0, 100.0)) }

    /** Switches between the manual loss (false) and the connector/cable breakdown (true). */
    fun setFeederPrecise(side: PlannerSide, precise: Boolean) =
        mutate { st -> st.withEnd(side, st.end(side).copy(feederPrecise = precise)) }

    /** Sets the detailed feeder and switches the end to precise mode. */
    fun setFeederConfig(side: PlannerSide, config: FeederConfig) =
        mutate { st -> st.withEnd(side, st.end(side).copy(feederConfig = config, feederPrecise = true)) }

    /** Applies a [FeederBuilder.presets] entry by id (unknown ids are ignored) and switches to precise mode. */
    fun applyFeederPreset(side: PlannerSide, presetId: String) {
        val preset = FeederBuilder.presets.firstOrNull { it.id == presetId } ?: return
        setFeederConfig(side, preset.config)
    }

    /** Resizes the feeder to [connectorCount] connectors (see [FeederBuilder.resize]) and switches to precise mode. */
    fun resizeFeederConnectors(side: PlannerSide, connectorCount: Int) =
        mutate { st ->
            val e = st.end(side)
            st.withEnd(side, e.copy(feederConfig = FeederBuilder.resize(e.feederConfig, connectorCount), feederPrecise = true))
        }

    /** Copies power, gain, height and the whole feeder (manual and precise) from A to B. Points are untouched. */
    fun copyAToB() = mutate { st ->
        val a = st.a
        st.copy(
            b = st.b.copy(
                txPowerDbm = a.txPowerDbm,
                antennaGainDbi = a.antennaGainDbi,
                antennaHeightM = a.antennaHeightM,
                feederManualDb = a.feederManualDb,
                feederPrecise = a.feederPrecise,
                feederConfig = a.feederConfig,
            ),
        )
    }

    /** Exchanges the two ends completely. */
    fun swapAB() = mutate { st -> st.copy(a = st.b, b = st.a, coverageSide = st.coverageSide.other, coverage = null) }

    // ------------------------------------------------------------------ radio

    /** Sets the frequency (20..20000 MHz); the band selection follows. */
    fun setFrequency(mHz: Double) {
        if (mHz.isNaN()) return
        mutate { st ->
            val f = PlannerBands.clampMHz(mHz)
            withBandRadio(st.copy(frequencyMHz = f, bandId = PlannerBands.idFor(f)), st.bandId)
        }
    }

    /** Picks a band: the frequency jumps to its centre, except for the free band which keeps the frequency. */
    fun setBand(bandId: String) {
        val band = PlannerBands.byId(bandId) ?: return
        mutate { st ->
            if (band.id == PlannerBands.FREE_ID) {
                st.copy(bandId = band.id)
            } else {
                // The band decides the effective bandwidth (2.4 GHz is wide LoRa), then the default frequency.
                val moved = withBandRadio(st.copy(bandId = band.id), st.bandId)
                moved.copy(frequencyMHz = PlannerBands.defaultFrequencyMHz(band.id, moved.bandwidthKhz))
            }
        }
    }

    /** Takes bandwidth and spreading factor from an app modem preset. */
    fun setRadioFromPreset(preset: ModemPreset?) {
        val base = plannerRadioFromPreset(preset)
        mutate { st ->
            val radio = base.copy(bandwidthKhz = base.bandwidthKhz * PlannerBands.bandwidthScale(st.bandId))
            // A frequency still sitting on the previous preset's default follows the new preset.
            val followsDefault = kotlin.math.abs(
                st.frequencyMHz - PlannerBands.defaultFrequencyMHz(st.bandId, st.bandwidthKhz),
            ) < 1.0e-6
            st.copy(
                bandwidthKhz = radio.bandwidthKhz,
                spreadingFactor = radio.spreadingFactor,
                modemPreset = preset,
                radioOverride = false,
                frequencyMHz = if (followsDefault && st.bandId != PlannerBands.FREE_ID) {
                    PlannerBands.defaultFrequencyMHz(st.bandId, radio.bandwidthKhz)
                } else {
                    st.frequencyMHz
                },
            )
        }
    }

    /**
     * Keeps the preset's bandwidth in step with the band: entering or leaving 2.4 GHz rescales it (wide LoRa x3.25).
     * A hand-typed bandwidth ([PlannerUiState.radioOverride]) is left alone.
     */
    private fun withBandRadio(st: PlannerUiState, previousBandId: String): PlannerUiState {
        val preset = st.modemPreset
        if (st.radioOverride || preset == null || st.bandId == previousBandId) return st
        val base = plannerRadioFromPreset(preset)
        return st.copy(bandwidthKhz = base.bandwidthKhz * PlannerBands.bandwidthScale(st.bandId))
    }

    /** Manual bandwidth in kHz (sets [PlannerUiState.radioOverride]). */
    fun setBandwidthKhz(khz: Double) {
        if (khz.isNaN() || khz <= 0.0) return
        mutate { st -> st.copy(bandwidthKhz = khz.coerceIn(1.0, 2000.0), modemPreset = null, radioOverride = true) }
    }

    /** Manual spreading factor 5..12 (sets [PlannerUiState.radioOverride]). */
    fun setSpreadingFactor(sf: Int) =
        mutate { st -> st.copy(spreadingFactor = sf.coerceIn(5, 12), modemPreset = null, radioOverride = true) }

    fun setNoiseFigure(db: Double) {
        if (db.isNaN()) return
        mutate { st -> st.copy(noiseFigureDb = db.coerceIn(0.0, 30.0)) }
    }

    // ------------------------------------------------------------------ environment

    /** Turns the Open-Meteo input on/off. Off means standard atmosphere (k = 4/3, N0 = 301). */
    fun setUseWeather(on: Boolean) = mutate { st ->
        st.copy(
            useWeather = on,
            weather = if (on) st.weather else PlannerWeatherStatus.Idle,
            kFactor = if (on) st.kFactor else PlannerUiState.DEFAULT_K,
            surfaceRefractivity = if (on) st.surfaceRefractivity else PlannerUiState.DEFAULT_N0,
        )
    }

    /** Forces a new weather download and recalculates immediately. */
    fun refreshWeather() {
        computer.invalidateWeather()
        scheduleRecompute(immediate = true)
    }

    /** Manual extra loss in dB; the clutter preset becomes CUSTOM unless it already has this value. */
    fun setExtraLossDb(db: Double) {
        if (db.isNaN()) return
        mutate { st ->
            val v = db.coerceIn(0.0, 100.0)
            val keep = st.clutterPreset != PlannerClutterPreset.CUSTOM && st.clutterPreset.extraDb == v
            st.copy(extraLossDb = v, clutterPreset = if (keep) st.clutterPreset else PlannerClutterPreset.CUSTOM)
        }
    }

    /** Applies a clutter preset's extra loss; CUSTOM only marks the loss as hand-typed. */
    fun setClutterPreset(preset: PlannerClutterPreset) = mutate { st ->
        if (preset == PlannerClutterPreset.CUSTOM) st.copy(clutterPreset = preset) else st.copy(clutterPreset = preset, extraLossDb = preset.extraDb)
    }

    /** Switches the real buildings / forests (OpenStreetMap) on or off; off keeps the preset behaviour. */
    fun setPreciseTerrain(on: Boolean) = mutate { it.copy(preciseTerrain = on, clutter = PlannerClutterStatus.Idle) }

    fun setClutterRadius(km: Double) {
        if (km.isNaN()) return
        mutate { it.copy(clutterRadiusKm = km.coerceIn(1.0, PlannerClutter.MAX_AREA_RADIUS_KM), clutter = PlannerClutterStatus.Idle) }
    }

    fun setForestHeight(m: Double) {
        if (m.isNaN()) return
        mutate { it.copy(forestHeightM = m.coerceIn(0.0, ClutterHeights.MAX_HEIGHT_M)) }
    }

    fun setBuildingHeight(m: Double) {
        if (m.isNaN()) return
        mutate { it.copy(buildingHeightM = m.coerceIn(0.0, ClutterHeights.MAX_HEIGHT_M)) }
    }

    // ------------------------------------------------------------------ computing

    /** Recalculates now (no debounce). Normally not needed: every setter schedules a debounced recalculation. */
    fun recompute() = scheduleRecompute(immediate = true)

    // ------------------------------------------------------------------ coverage

    fun setCoverageSide(side: PlannerSide) = mutate(recompute = false) { it.copy(coverageSide = side) }

    fun setCoverageMaxRangeKm(km: Double) {
        if (km.isNaN()) return
        mutate(recompute = false) { it.copy(coverageMaxRangeKm = km.coerceIn(1.0, 300.0)) }
    }

    fun setCoverageOpacity(value: Double) = mutate(recompute = false) { it.copy(coverageOpacity = value.coerceIn(0.15, 1.0)) }

    fun setCoverageRadials(count: Int) = mutate(recompute = false) { it.copy(coverageRadials = count.coerceIn(8, 360)) }

    fun setCoverageRxHeightM(meters: Double) {
        if (meters.isNaN()) return
        mutate(recompute = false) { it.copy(coverageRxHeightM = meters.coerceIn(0.0, 100.0)) }
    }

    fun setCoverageRxGainDbi(dbi: Double) {
        if (dbi.isNaN()) return
        mutate(recompute = false) { it.copy(coverageRxGainDbi = dbi.coerceIn(-10.0, 30.0)) }
    }

    /** Starts the coverage calculation for [PlannerUiState.coverageSide] (progress in [PlannerUiState.coverageProgress]). */
    fun computeCoverage() {
        val snapshot = _uiState.value
        if (!snapshot.end(snapshot.coverageSide).isComplete) {
            _uiState.update { it.copy(coverageError = PlannerError.COVERAGE_NEEDS_POINT) }
            return
        }
        coverageJob?.cancel()
        _uiState.update { it.copy(coverageComputing = true, coverageProgress = 0f, coverageError = null) }
        coverageJob = viewModelScope.launch {
            try {
                val result =
                    computer.computeCoverage(
                        snapshot,
                        onClutterFailure = { f -> _uiState.update { it.copy(clutter = PlannerClutterStatus.Failed(f)) } },
                    ) { p -> _uiState.update { it.copy(coverageProgress = p) } }
                _uiState.update { it.copy(coverage = result, coverageComputing = false, coverageProgress = 1f) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: PlannerElevationException) {
                val err = if (e.failure == PlannerElevationFailure.NETWORK) PlannerError.ELEVATION_OFFLINE else PlannerError.ELEVATION_DECODE
                _uiState.update { it.copy(coverageComputing = false, coverageError = err) }
            } catch (e: Exception) {
                _uiState.update { it.copy(coverageComputing = false, coverageError = PlannerError.COMPUTE_FAILED) }
            } catch (e: Throwable) { // out of memory is not an Exception
                _uiState.update { it.copy(coverageComputing = false, coverageError = PlannerError.COMPUTE_FAILED) }
            }
        }
    }

    /** Cancels a running coverage calculation and drops the result. */
    fun clearCoverage() {
        coverageJob?.cancel()
        coverageJob = null
        _uiState.update { it.copy(coverage = null, coverageComputing = false, coverageProgress = 0f, coverageError = null) }
    }

    /** Back to defaults (keeps nothing). */
    fun reset() {
        computeJob?.cancel()
        coverageJob?.cancel()
        _uiState.value = normalize(PlannerUiState())
    }

    // ------------------------------------------------------------------ export

    /** The export model for the current state; [strings] carries all texts. */
    fun buildReport(strings: PlannerReportStrings = PlannerReportStrings()): PlannerReport =
        PlannerReportBuilder.build(_uiState.value, strings)

    suspend fun exportPdf(strings: PlannerReportStrings): ByteArray {
        val report = buildReport(strings)
        return withContext(Dispatchers.Default) { PlannerPdf.render(report) }
    }

    /** `label;value` rows of all report sections. */
    suspend fun exportCsvSummary(strings: PlannerReportStrings): String {
        val report = buildReport(strings)
        return withContext(Dispatchers.Default) { PlannerCsv.summary(report) }
    }

    /** Terrain profile samples (distance, ground, LOS, Fresnel). Header only when there is no profile yet. */
    suspend fun exportCsvProfile(strings: PlannerReportStrings): String {
        val report = buildReport(strings)
        return withContext(Dispatchers.Default) { PlannerCsv.profile(report) }
    }

    suspend fun exportKml(strings: PlannerReportStrings): String {
        val report = buildReport(strings)
        return withContext(Dispatchers.Default) { PlannerKml.render(report) }
    }

    suspend fun exportGeoJson(strings: PlannerReportStrings): String {
        val report = buildReport(strings)
        return withContext(Dispatchers.Default) { PlannerGeoJson.render(report) }
    }

    /** Map-layer GeoJSON (filled sectors, heat-map look) of the calculated coverage, or null when there is none. */
    suspend fun coverageLayerGeoJson(name: String): String? {
        val coverage = uiState.value.coverage ?: return null
        return withContext(Dispatchers.Default) { CoverageLayer.render(coverage, name, uiState.value.coverageOpacity) }
    }

    /** PNG of the terrain profile chart, or null when no profile has been calculated. */
    suspend fun exportPng(strings: PlannerReportStrings): ByteArray? {
        val report = buildReport(strings)
        val profile = report.profile ?: return null
        return withContext(Dispatchers.Default) { PlannerProfileImage.render(profile) }
    }

    // ------------------------------------------------------------------ internals

    private fun setPoint(side: PlannerSide, source: PlannerPointSource, lat: Double, lon: Double) {
        if (lat.isNaN() || lon.isNaN() || lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return
        mutate { st ->
            val e = st.end(side)
            val keepName = e.pointSource is PlannerPointSource.Map || e.pointSource is PlannerPointSource.Manual ||
                e.pointSource is PlannerPointSource.None
            st.withEnd(side, placed(e, source, lat, lon, if (keepName) e.name else ""))
        }
    }

    private fun placed(e: PlannerEnd, source: PlannerPointSource, lat: Double, lon: Double, name: String): PlannerEnd = e.copy(
        pointSource = source,
        lat = lat,
        lon = lon,
        name = name,
        groundAltM = null,
        groundAltManual = false,
        stationUnavailable = false,
    )

    private fun editEnd(side: PlannerSide, value: Double, f: (PlannerEnd, Double) -> PlannerEnd) {
        if (value.isNaN()) return
        mutate { st -> st.withEnd(side, f(st.end(side), value)) }
    }

    private fun mutate(recompute: Boolean = true, f: (PlannerUiState) -> PlannerUiState) {
        _uiState.update { normalize(f(it)) }
        if (recompute) scheduleRecompute()
    }

    private fun scheduleRecompute(immediate: Boolean = false) {
        computeJob?.cancel()
        computeJob = viewModelScope.launch {
            if (!immediate && debounceMs > 0) delay(debounceMs)
            runCompute()
        }
    }

    private suspend fun runCompute() {
        val snapshot = _uiState.value
        if (snapshot.isLinkReady) {
            _uiState.update {
                it.copy(
                    computing = true,
                    weather = if (it.useWeather) PlannerWeatherStatus.Loading else PlannerWeatherStatus.Idle,
                    clutter = if (it.preciseTerrain) PlannerClutterStatus.Loading else PlannerClutterStatus.Idle,
                )
            }
        } else {
            _uiState.update { it.copy(computing = false) }
        }
        val res = computer.compute(snapshot, nodesWithPosition.value)
        _uiState.update { cur ->
            normalize(
                cur.copy(
                    a = applyGround(cur.a, snapshot.a, res.groundAltA),
                    b = applyGround(cur.b, snapshot.b, res.groundAltB),
                    link = res.link,
                    series = res.series,
                    comparison = res.comparison,
                    error = res.error,
                    weather = if (cur.useWeather) res.weather else PlannerWeatherStatus.Idle,
                    clutter = if (cur.preciseTerrain) res.clutter else PlannerClutterStatus.Idle,
                    kFactor = res.kFactor,
                    surfaceRefractivity = res.surfaceRefractivity,
                    atmosphericLossDb = res.atmosphericLossDb,
                    computing = false,
                ),
            )
        }
    }

    /** Stores the terrain-model ground height unless the user typed one or the point changed meanwhile. */
    private fun applyGround(current: PlannerEnd, computedFor: PlannerEnd, auto: Double?): PlannerEnd {
        if (current.groundAltManual || auto == null) return current
        if (current.lat != computedFor.lat || current.lon != computedFor.lon) return current
        return current.copy(groundAltM = auto)
    }

    companion object {
        const val DEBOUNCE_MS = 300L

        /** Derived fields: feeder loss at the current frequency, per side hints. */
        internal fun normalize(s: PlannerUiState): PlannerUiState {
            val a = s.a.copy(feederResult = FeederBuilder.compute(s.a.feederConfig, s.frequencyMHz))
            val b = s.b.copy(feederResult = FeederBuilder.compute(s.b.feederConfig, s.frequencyMHz))
            return s.copy(
                a = a,
                b = b,
                hintsBySide = mapOf(PlannerSide.A to hintsOf(a), PlannerSide.B to hintsOf(b)),
            )
        }

        private fun hintsOf(e: PlannerEnd): List<PlannerHint> {
            val out = ArrayList<PlannerHint>()
            if (!e.isComplete) out.add(PlannerHint.POINT_MISSING)
            if (e.stationUnavailable) out.add(PlannerHint.STATION_NO_POSITION)
            return out
        }
    }
}
