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

package org.meshtastic.feature.map.planner.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_chart_label_a
import org.meshtastic.core.resources.planner_chart_label_b
import org.meshtastic.core.resources.planner_ducting_elevated
import org.meshtastic.core.resources.planner_ducting_normal
import org.meshtastic.core.resources.planner_ducting_possible
import org.meshtastic.core.resources.planner_no
import org.meshtastic.core.resources.planner_report_chart_x_axis
import org.meshtastic.core.resources.planner_report_chart_y_axis
import org.meshtastic.core.resources.planner_report_coverage_title
import org.meshtastic.core.resources.planner_report_disclaimer
import org.meshtastic.core.resources.planner_report_footer
import org.meshtastic.core.resources.planner_report_generated
import org.meshtastic.core.resources.planner_report_lbl_antenna_gain
import org.meshtastic.core.resources.planner_report_lbl_antenna_height
import org.meshtastic.core.resources.planner_report_lbl_azimuth_ab
import org.meshtastic.core.resources.planner_report_lbl_azimuth_ba
import org.meshtastic.core.resources.planner_report_lbl_bandwidth
import org.meshtastic.core.resources.planner_report_lbl_delta_rssi
import org.meshtastic.core.resources.planner_report_lbl_delta_snr
import org.meshtastic.core.resources.planner_report_lbl_distance
import org.meshtastic.core.resources.planner_report_lbl_ducting
import org.meshtastic.core.resources.planner_report_lbl_elevation_a
import org.meshtastic.core.resources.planner_report_lbl_elevation_b
import org.meshtastic.core.resources.planner_report_lbl_extra_loss
import org.meshtastic.core.resources.planner_report_lbl_terrain_data
import org.meshtastic.core.resources.planner_report_val_terrain_osm
import org.meshtastic.core.resources.planner_report_lbl_feeder_approx
import org.meshtastic.core.resources.planner_report_lbl_feeder_loss
import org.meshtastic.core.resources.planner_report_lbl_frequency
import org.meshtastic.core.resources.planner_report_lbl_fresnel
import org.meshtastic.core.resources.planner_report_lbl_fspl
import org.meshtastic.core.resources.planner_report_lbl_ground_alt
import org.meshtastic.core.resources.planner_report_lbl_humidity
import org.meshtastic.core.resources.planner_report_lbl_itm_loss
import org.meshtastic.core.resources.planner_report_lbl_k_factor
import org.meshtastic.core.resources.planner_report_lbl_latitude
import org.meshtastic.core.resources.planner_report_lbl_line_of_sight
import org.meshtastic.core.resources.planner_report_lbl_longitude
import org.meshtastic.core.resources.planner_report_lbl_margin_ab
import org.meshtastic.core.resources.planner_report_lbl_margin_ba
import org.meshtastic.core.resources.planner_report_lbl_meas_rssi
import org.meshtastic.core.resources.planner_report_lbl_meas_snr
import org.meshtastic.core.resources.planner_report_lbl_measured_node
import org.meshtastic.core.resources.planner_report_lbl_name
import org.meshtastic.core.resources.planner_report_lbl_noise_figure
import org.meshtastic.core.resources.planner_report_lbl_noise_floor
import org.meshtastic.core.resources.planner_report_lbl_pred_rssi
import org.meshtastic.core.resources.planner_report_lbl_pred_snr
import org.meshtastic.core.resources.planner_report_lbl_pressure
import org.meshtastic.core.resources.planner_report_lbl_rain
import org.meshtastic.core.resources.planner_report_lbl_refractivity
import org.meshtastic.core.resources.planner_report_lbl_rx_power_ab
import org.meshtastic.core.resources.planner_report_lbl_rx_power_ba
import org.meshtastic.core.resources.planner_report_lbl_sensitivity
import org.meshtastic.core.resources.planner_report_lbl_spreading_factor
import org.meshtastic.core.resources.planner_report_lbl_temperature
import org.meshtastic.core.resources.planner_report_lbl_total_loss
import org.meshtastic.core.resources.planner_report_lbl_tx_power
import org.meshtastic.core.resources.planner_report_lbl_verdict_ab
import org.meshtastic.core.resources.planner_report_lbl_verdict_ba
import org.meshtastic.core.resources.planner_report_lbl_weather_source
import org.meshtastic.core.resources.planner_report_lbl_weather_time
import org.meshtastic.core.resources.planner_report_legend_excellent
import org.meshtastic.core.resources.planner_report_legend_good
import org.meshtastic.core.resources.planner_report_legend_marginal
import org.meshtastic.core.resources.planner_report_legend_none
import org.meshtastic.core.resources.planner_report_legend_weak
import org.meshtastic.core.resources.planner_report_note_not_direct
import org.meshtastic.core.resources.planner_report_note_weather_failed
import org.meshtastic.core.resources.planner_report_note_weather_not_used
import org.meshtastic.core.resources.planner_report_note_weather_used
import org.meshtastic.core.resources.planner_report_section_link
import org.meshtastic.core.resources.planner_report_section_measured
import org.meshtastic.core.resources.planner_report_section_radio
import org.meshtastic.core.resources.planner_report_section_results
import org.meshtastic.core.resources.planner_report_section_station_a
import org.meshtastic.core.resources.planner_report_section_station_b
import org.meshtastic.core.resources.planner_report_section_weather
import org.meshtastic.core.resources.planner_report_title
import org.meshtastic.core.resources.planner_verdict_excellent
import org.meshtastic.core.resources.planner_verdict_good
import org.meshtastic.core.resources.planner_verdict_marginal
import org.meshtastic.core.resources.planner_verdict_no_link
import org.meshtastic.core.resources.planner_verdict_weak
import org.meshtastic.core.resources.planner_yes
import org.meshtastic.feature.map.planner.state.PlannerReportStrings

/** "Generated: yyyy-MM-dd HH:mm UTC" for the report header. */
internal fun plannerGeneratedAt(prefix: String): String = prefix + " " + PlannerClock.formatUtcMinute(nowMillis) + " UTC"

/**
 * All texts of the exported reports (PDF, CSV, KML, GeoJSON, PNG) in the current app language. The time stamp
 * ([PlannerReportStrings.generatedAt]) is taken when this is called; export code refreshes it with
 * [plannerGeneratedAt] right before writing.
 */
@Composable
@Suppress("LongMethod")
fun rememberPlannerReportStrings(): PlannerReportStrings {
    val generatedPrefix = stringResource(Res.string.planner_report_generated)
    return PlannerReportStrings(
        generatedAt = plannerGeneratedAt(generatedPrefix),
        credits = PlannerCreditLines(),
        title = stringResource(Res.string.planner_report_title),
        footer = stringResource(Res.string.planner_report_footer),
        sectionStationA = stringResource(Res.string.planner_report_section_station_a),
        sectionStationB = stringResource(Res.string.planner_report_section_station_b),
        sectionRadio = stringResource(Res.string.planner_report_section_radio),
        sectionLink = stringResource(Res.string.planner_report_section_link),
        sectionResults = stringResource(Res.string.planner_report_section_results),
        sectionWeather = stringResource(Res.string.planner_report_section_weather),
        sectionMeasured = stringResource(Res.string.planner_report_section_measured),
        defaultNameA = stringResource(Res.string.planner_report_section_station_a),
        defaultNameB = stringResource(Res.string.planner_report_section_station_b),
        lblName = stringResource(Res.string.planner_report_lbl_name),
        lblLatitude = stringResource(Res.string.planner_report_lbl_latitude),
        lblLongitude = stringResource(Res.string.planner_report_lbl_longitude),
        lblGroundAlt = stringResource(Res.string.planner_report_lbl_ground_alt),
        lblAntennaHeight = stringResource(Res.string.planner_report_lbl_antenna_height),
        lblTxPower = stringResource(Res.string.planner_report_lbl_tx_power),
        lblAntennaGain = stringResource(Res.string.planner_report_lbl_antenna_gain),
        lblFeederLoss = stringResource(Res.string.planner_report_lbl_feeder_loss),
        lblFeederApprox = stringResource(Res.string.planner_report_lbl_feeder_approx),
        lblFrequency = stringResource(Res.string.planner_report_lbl_frequency),
        lblBandwidth = stringResource(Res.string.planner_report_lbl_bandwidth),
        lblSpreadingFactor = stringResource(Res.string.planner_report_lbl_spreading_factor),
        lblNoiseFigure = stringResource(Res.string.planner_report_lbl_noise_figure),
        lblNoiseFloor = stringResource(Res.string.planner_report_lbl_noise_floor),
        lblSensitivity = stringResource(Res.string.planner_report_lbl_sensitivity),
        lblDistance = stringResource(Res.string.planner_report_lbl_distance),
        lblAzimuthAB = stringResource(Res.string.planner_report_lbl_azimuth_ab),
        lblAzimuthBA = stringResource(Res.string.planner_report_lbl_azimuth_ba),
        lblElevationAngleA = stringResource(Res.string.planner_report_lbl_elevation_a),
        lblElevationAngleB = stringResource(Res.string.planner_report_lbl_elevation_b),
        lblFspl = stringResource(Res.string.planner_report_lbl_fspl),
        lblItmLoss = stringResource(Res.string.planner_report_lbl_itm_loss),
        lblExtraLoss = stringResource(Res.string.planner_report_lbl_extra_loss),
        lblTerrainData = stringResource(Res.string.planner_report_lbl_terrain_data),
        valTerrainOsm = stringResource(Res.string.planner_report_val_terrain_osm),
        lblTotalLoss = stringResource(Res.string.planner_report_lbl_total_loss),
        lblLineOfSight = stringResource(Res.string.planner_report_lbl_line_of_sight),
        lblFresnel = stringResource(Res.string.planner_report_lbl_fresnel),
        lblKFactor = stringResource(Res.string.planner_report_lbl_k_factor),
        lblRefractivity = stringResource(Res.string.planner_report_lbl_refractivity),
        lblRxPowerAB = stringResource(Res.string.planner_report_lbl_rx_power_ab),
        lblMarginAB = stringResource(Res.string.planner_report_lbl_margin_ab),
        lblVerdictAB = stringResource(Res.string.planner_report_lbl_verdict_ab),
        lblRxPowerBA = stringResource(Res.string.planner_report_lbl_rx_power_ba),
        lblMarginBA = stringResource(Res.string.planner_report_lbl_margin_ba),
        lblVerdictBA = stringResource(Res.string.planner_report_lbl_verdict_ba),
        verdictExcellent = stringResource(Res.string.planner_verdict_excellent),
        verdictGood = stringResource(Res.string.planner_verdict_good),
        verdictMarginal = stringResource(Res.string.planner_verdict_marginal),
        verdictWeak = stringResource(Res.string.planner_verdict_weak),
        verdictNoLink = stringResource(Res.string.planner_verdict_no_link),
        yes = stringResource(Res.string.planner_yes),
        no = stringResource(Res.string.planner_no),
        lblWeatherTime = stringResource(Res.string.planner_report_lbl_weather_time),
        lblWeatherSource = stringResource(Res.string.planner_report_lbl_weather_source),
        lblTemperature = stringResource(Res.string.planner_report_lbl_temperature),
        lblPressure = stringResource(Res.string.planner_report_lbl_pressure),
        lblHumidity = stringResource(Res.string.planner_report_lbl_humidity),
        lblRain = stringResource(Res.string.planner_report_lbl_rain),
        lblDucting = stringResource(Res.string.planner_report_lbl_ducting),
        ductingNormal = stringResource(Res.string.planner_ducting_normal),
        ductingElevated = stringResource(Res.string.planner_ducting_elevated),
        ductingPossible = stringResource(Res.string.planner_ducting_possible),
        lblMeasuredNode = stringResource(Res.string.planner_report_lbl_measured_node),
        lblPredictedRssi = stringResource(Res.string.planner_report_lbl_pred_rssi),
        lblMeasuredRssi = stringResource(Res.string.planner_report_lbl_meas_rssi),
        lblRssiDelta = stringResource(Res.string.planner_report_lbl_delta_rssi),
        lblPredictedSnr = stringResource(Res.string.planner_report_lbl_pred_snr),
        lblMeasuredSnr = stringResource(Res.string.planner_report_lbl_meas_snr),
        lblSnrDelta = stringResource(Res.string.planner_report_lbl_delta_snr),
        noteNotDirect = stringResource(Res.string.planner_report_note_not_direct),
        chartLabelA = stringResource(Res.string.planner_chart_label_a),
        chartLabelB = stringResource(Res.string.planner_chart_label_b),
        chartXAxis = stringResource(Res.string.planner_report_chart_x_axis),
        chartYAxis = stringResource(Res.string.planner_report_chart_y_axis),
        coverageTitle = stringResource(Res.string.planner_report_coverage_title),
        legendExcellent = stringResource(Res.string.planner_report_legend_excellent),
        legendGood = stringResource(Res.string.planner_report_legend_good),
        legendMarginal = stringResource(Res.string.planner_report_legend_marginal),
        legendWeak = stringResource(Res.string.planner_report_legend_weak),
        legendNone = stringResource(Res.string.planner_report_legend_none),
        disclaimer = stringResource(Res.string.planner_report_disclaimer),
        noteWeatherUsed = stringResource(Res.string.planner_report_note_weather_used),
        noteWeatherNotUsed = stringResource(Res.string.planner_report_note_weather_not_used),
        noteWeatherFailed = stringResource(Res.string.planner_report_note_weather_failed),
        unitM = "m",
        unitKm = "km",
        unitDb = "dB",
        unitDbm = "dBm",
        unitMHz = "MHz",
        unitKHz = "kHz",
        unitW = "W",
        unitDeg = "\u00B0",
        unitHpa = "hPa",
        unitCelsius = "\u00B0C",
        unitPercent = "%",
        unitMmH = "mm/h",
        unitDbi = "dBi",
    )
}

/** Prefix of [PlannerReportStrings.generatedAt] ("Generated:"). */
@Composable
internal fun plannerGeneratedPrefix(): String = stringResource(Res.string.planner_report_generated)
