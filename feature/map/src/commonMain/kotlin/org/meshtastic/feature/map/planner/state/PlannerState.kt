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

import org.meshtastic.feature.map.planner.CoverageResult
import org.meshtastic.feature.map.planner.FeederBuilder
import org.meshtastic.feature.map.planner.FeederConfig
import org.meshtastic.feature.map.planner.FeederResult
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.LinkResult
import org.meshtastic.feature.map.planner.PlannerBands
import org.meshtastic.feature.map.planner.ProfileSeries
import org.meshtastic.feature.map.planner.data.PropagationConditions
import org.meshtastic.feature.map.planner.data.PlannerWeatherError
import org.meshtastic.proto.Config.LoRaConfig.ModemPreset
import kotlin.math.log10
import kotlin.math.pow

/** The two link ends. */
enum class PlannerSide {
    A,
    B,
    ;

    val other: PlannerSide get() = if (this == A) B else A
}

/** Where the coordinates of a link end came from. */
sealed interface PlannerPointSource {
    /** No point chosen yet. */
    data object None : PlannerPointSource

    /** A mesh node with a known position (its number is [num]). */
    data class Node(val num: Int) : PlannerPointSource

    /** Picked on the map. */
    data object Map : PlannerPointSource

    /** Typed in by the user. */
    data object Manual : PlannerPointSource

    /** The connected node ("my station"). */
    data object Station : PlannerPointSource
}

/** Typical additional loss (clutter) presets; [extraDb] is applied as extra loss. [CUSTOM] means a typed value. */
enum class PlannerClutterPreset(val extraDb: Double) {
    NONE(0.0),
    RURAL_OPEN(0.0),
    FOREST_LIGHT(3.0),
    FOREST_DENSE(10.0),
    SUBURBAN(6.0),
    URBAN(12.0),
    CUSTOM(0.0),
}

/** Missing data / attention markers for one side (the UI shows a localized text for each). */
enum class PlannerHint {
    /** No coordinates for this end. */
    POINT_MISSING,

    /** "Use station" was requested but the connected node has no position (or none is connected). */
    STATION_NO_POSITION,
}

/** Typed error reasons (never prose). */
enum class PlannerError {
    /** Terrain tiles could not be fetched (offline). */
    ELEVATION_OFFLINE,

    /** Terrain tiles could not be decoded. */
    ELEVATION_DECODE,

    /** The two points are (almost) identical. */
    POINTS_TOO_CLOSE,

    /** Unexpected failure of the radio calculation. */
    COMPUTE_FAILED,

    /** Coverage requested without a position for the selected side. */
    COVERAGE_NEEDS_POINT,
}

/** State of the optional weather input. */
sealed interface PlannerWeatherStatus {
    /** Weather not requested (or no point yet). */
    data object Idle : PlannerWeatherStatus

    data object Loading : PlannerWeatherStatus

    data class Ready(val conditions: PropagationConditions) : PlannerWeatherStatus

    /** The planner falls back to k = 4/3 and N0 = 301. */
    data class Failed(val error: PlannerWeatherError) : PlannerWeatherStatus
}

/** One link end. All values are plain data; derived values are computed properties. */
data class PlannerEnd(
    val pointSource: PlannerPointSource = PlannerPointSource.None,
    val lat: Double? = null,
    val lon: Double? = null,
    val name: String = "",
    /** Ground height (m ASL): from the terrain model unless [groundAltManual]. Null until known. */
    val groundAltM: Double? = null,
    val groundAltManual: Boolean = false,
    val antennaHeightM: Double = DEFAULT_HEIGHT_M,
    val txPowerDbm: Double = DEFAULT_TX_DBM,
    val antennaGainDbi: Double = DEFAULT_GAIN_DBI,
    /** Feeder loss typed by the user (used while [feederPrecise] is false). */
    val feederManualDb: Double = 0.0,
    /** When true the loss is computed from [feederConfig] instead of [feederManualDb]. */
    val feederPrecise: Boolean = false,
    val feederConfig: FeederConfig = DEFAULT_FEEDER,
    /** [feederConfig] evaluated at the current frequency (kept up to date by the ViewModel). */
    val feederResult: FeederResult = EMPTY_FEEDER_RESULT,
    /** True when "use station" failed for this end (drives [PlannerHint.STATION_NO_POSITION]). */
    val stationUnavailable: Boolean = false,
) {
    val isComplete: Boolean get() = lat != null && lon != null

    val point: GeoPoint? get() = if (lat != null && lon != null) GeoPoint(lat, lon) else null

    /** Feeder loss that enters the link budget. */
    val feederLossDb: Double get() = if (feederPrecise) feederResult.totalDb else feederManualDb

    /** Transmit power in watts. */
    val txPowerW: Double get() = dbmToWatts(txPowerDbm)

    /** Id of the matching [FeederBuilder.presets] entry, or null for a custom configuration. */
    val feederPresetId: String? get() = FeederBuilder.presets.firstOrNull { it.config == feederConfig }?.id

    companion object {
        const val DEFAULT_HEIGHT_M = 2.0
        const val DEFAULT_TX_DBM = 20.0
        const val DEFAULT_GAIN_DBI = 2.15
        val DEFAULT_FEEDER: FeederConfig = FeederBuilder.presets.first { it.id == "direct" }.config
        val EMPTY_FEEDER_RESULT = FeederResult(0.0, emptyList(), false, emptyList())

        fun dbmToWatts(dbm: Double): Double = 10.0.pow(dbm / 10.0) / 1000.0

        fun wattsToDbm(watts: Double): Double = if (watts <= 0.0) -100.0 else 10.0 * log10(watts * 1000.0)
    }
}

/** Link-level prediction versus the last measurement of a mesh node (LNA-corrected RSSI). */
data class MeasuredComparison(
    /** The node whose packets were measured (it transmits, our station receives). */
    val nodeNum: Int,
    val nodeName: String,
    /** Side of the planner the node sits on. */
    val nodeSide: PlannerSide,
    /** False when the node is more than 0 hops away: the measurement then does not describe this radio hop. */
    val direct: Boolean,
    val predictedRssiDbm: Double,
    val predictedSnrDb: Double,
    val noiseFloorDbm: Double,
    val measuredRssiDbm: Int?,
    val measuredSnrDb: Float?,
    /** measured - predicted, null when the measurement is missing. */
    val rssiDeltaDb: Double?,
    val snrDeltaDb: Double?,
)

/** Everything the planner UI shows. Immutable. */
data class PlannerUiState(
    val a: PlannerEnd = PlannerEnd(),
    val b: PlannerEnd = PlannerEnd(),
    val selectedSide: PlannerSide = PlannerSide.A,
    // ---- radio ----
    val frequencyMHz: Double = 869.525,
    val bandId: String = PlannerBands.DEFAULT_ID,
    val bandwidthKhz: Double = 250.0,
    val spreadingFactor: Int = 11,
    /** True when BW/SF were edited by hand (then [modemPreset] is null). */
    val radioOverride: Boolean = false,
    val modemPreset: ModemPreset? = ModemPreset.LONG_FAST,
    val noiseFigureDb: Double = 6.0,
    // ---- environment ----
    val useWeather: Boolean = false,
    val weather: PlannerWeatherStatus = PlannerWeatherStatus.Idle,
    /** Effective earth radius factor used by the last calculation (4/3 without weather). */
    val kFactor: Double = DEFAULT_K,
    /** Sea-level surface refractivity (N-units) used by the last calculation (301 without weather). */
    val surfaceRefractivity: Double = DEFAULT_N0,
    /** Manual clutter / extra loss in dB (gas and rain are added automatically when weather is on). */
    val extraLossDb: Double = 0.0,
    val clutterPreset: PlannerClutterPreset = PlannerClutterPreset.NONE,
    /** Gas + rain loss over the path that was added by the last calculation (0 below 1 GHz). */
    val atmosphericLossDb: Double = 0.0,
    // ---- results ----
    val computing: Boolean = false,
    val link: LinkResult? = null,
    val series: ProfileSeries? = null,
    val comparison: MeasuredComparison? = null,
    val error: PlannerError? = null,
    val hintsBySide: Map<PlannerSide, List<PlannerHint>> = emptyMap(),
    // ---- coverage ----
    val coverageSide: PlannerSide = PlannerSide.A,
    val coverageMaxRangeKm: Double = 50.0,
    val coverageRadials: Int = 180,
    val coverageRxHeightM: Double = 2.0,
    val coverageRxGainDbi: Double = 0.0,
    val coverageComputing: Boolean = false,
    /** 0..1 while [coverageComputing]. */
    val coverageProgress: Float = 0f,
    val coverage: CoverageResult? = null,
    val coverageError: PlannerError? = null,
) {
    fun end(side: PlannerSide): PlannerEnd = if (side == PlannerSide.A) a else b

    internal fun withEnd(side: PlannerSide, end: PlannerEnd): PlannerUiState =
        if (side == PlannerSide.A) copy(a = end) else copy(b = end)

    /** Both ends have coordinates, so a link can be calculated. */
    val isLinkReady: Boolean get() = a.isComplete && b.isComplete

    /** Noise floor (dBm) = -174 + 10 log10(BW) + NF. */
    val noiseFloorDbm: Double get() = -174.0 + 10.0 * log10(bandwidthKhz * 1000.0) + noiseFigureDb

    companion object {
        const val DEFAULT_K = 4.0 / 3.0
        const val DEFAULT_N0 = 301.0
    }
}

/** A mesh node that can be used as a link end. [rssiDbm] is already LNA corrected. */
data class PlannerNodeOption(
    val num: Int,
    val longName: String,
    val shortName: String,
    val lat: Double,
    val lon: Double,
    val altitudeM: Double?,
    val snrDb: Float?,
    val rssiDbm: Int?,
    val hopsAway: Int,
    val isOurs: Boolean,
) {
    /** Best display name. */
    val displayName: String get() = longName.ifBlank { shortName.ifBlank { "!" + num.toUInt().toString(16) } }
}
