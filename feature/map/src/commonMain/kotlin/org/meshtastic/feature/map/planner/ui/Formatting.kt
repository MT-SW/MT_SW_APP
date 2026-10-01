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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_clutter_custom
import org.meshtastic.core.resources.planner_clutter_forest_dense
import org.meshtastic.core.resources.planner_clutter_forest_light
import org.meshtastic.core.resources.planner_clutter_none
import org.meshtastic.core.resources.planner_clutter_rural_open
import org.meshtastic.core.resources.planner_clutter_suburban
import org.meshtastic.core.resources.planner_clutter_urban
import org.meshtastic.core.resources.planner_ducting_elevated
import org.meshtastic.core.resources.planner_ducting_normal
import org.meshtastic.core.resources.planner_ducting_possible
import org.meshtastic.core.resources.planner_error_compute_failed
import org.meshtastic.core.resources.planner_error_coverage_needs_point
import org.meshtastic.core.resources.planner_error_elevation_decode
import org.meshtastic.core.resources.planner_error_elevation_offline
import org.meshtastic.core.resources.planner_error_points_too_close
import org.meshtastic.core.resources.planner_hint_point_missing
import org.meshtastic.core.resources.planner_hint_station_no_position
import org.meshtastic.core.resources.planner_itm_mode_diffraction
import org.meshtastic.core.resources.planner_itm_mode_los
import org.meshtastic.core.resources.planner_itm_mode_troposcatter
import org.meshtastic.core.resources.planner_itm_mode_unknown
import org.meshtastic.core.resources.planner_point_a
import org.meshtastic.core.resources.planner_point_b
import org.meshtastic.core.resources.planner_verdict_excellent
import org.meshtastic.core.resources.planner_verdict_good
import org.meshtastic.core.resources.planner_verdict_marginal
import org.meshtastic.core.resources.planner_verdict_no_link
import org.meshtastic.core.resources.planner_verdict_weak
import org.meshtastic.core.resources.planner_weather_error_bad_response
import org.meshtastic.core.resources.planner_weather_error_http
import org.meshtastic.core.resources.planner_weather_error_offline
import org.meshtastic.core.ui.theme.StatusColors.StatusGreen
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline
import org.meshtastic.core.ui.theme.StatusColors.StatusOrange
import org.meshtastic.core.ui.theme.StatusColors.StatusRed
import org.meshtastic.core.ui.theme.StatusColors.StatusYellow
import org.meshtastic.feature.map.planner.DuctingLevel
import org.meshtastic.feature.map.planner.ItmMode
import org.meshtastic.feature.map.planner.LinkVerdict
import org.meshtastic.feature.map.planner.data.PlannerWeatherError
import org.meshtastic.feature.map.planner.state.PlannerClutterPreset
import org.meshtastic.feature.map.planner.state.PlannerError
import org.meshtastic.feature.map.planner.state.PlannerHint
import org.meshtastic.feature.map.planner.state.PlannerSide

/** Localized number (locale decimal mark). */
internal fun fmt(value: Double, decimals: Int): String = NumberFormatter.format(value, decimals)

/** Localized number, optionally with an explicit plus sign for positive values. */
internal fun fmtSigned(value: Double, decimals: Int): String =
    (if (value > 0.0) "+" else "") + NumberFormatter.format(value, decimals)

/** "950 m" / "12.34 km". */
internal fun fmtDistance(meters: Double, unitM: String, unitKm: String): String =
    if (meters < 1000.0) "${fmt(meters, 0)} $unitM" else "${fmt(meters / 1000.0, 2)} $unitKm"

@Composable
internal fun verdictColor(verdict: LinkVerdict): Color = when (verdict) {
    LinkVerdict.EXCELLENT -> MaterialTheme.colorScheme.StatusGreen
    LinkVerdict.GOOD -> MaterialTheme.colorScheme.StatusOnline
    LinkVerdict.MARGINAL -> MaterialTheme.colorScheme.StatusYellow
    LinkVerdict.WEAK -> MaterialTheme.colorScheme.StatusOrange
    LinkVerdict.NO_LINK -> MaterialTheme.colorScheme.StatusRed
}

@Composable
internal fun verdictText(verdict: LinkVerdict): String = stringResource(
    when (verdict) {
        LinkVerdict.EXCELLENT -> Res.string.planner_verdict_excellent
        LinkVerdict.GOOD -> Res.string.planner_verdict_good
        LinkVerdict.MARGINAL -> Res.string.planner_verdict_marginal
        LinkVerdict.WEAK -> Res.string.planner_verdict_weak
        LinkVerdict.NO_LINK -> Res.string.planner_verdict_no_link
    },
)

@Composable
internal fun sideName(side: PlannerSide): String = stringResource(
    if (side == PlannerSide.A) Res.string.planner_point_a else Res.string.planner_point_b,
)

@Composable
internal fun errorText(error: PlannerError): String = stringResource(
    when (error) {
        PlannerError.ELEVATION_OFFLINE -> Res.string.planner_error_elevation_offline
        PlannerError.ELEVATION_DECODE -> Res.string.planner_error_elevation_decode
        PlannerError.POINTS_TOO_CLOSE -> Res.string.planner_error_points_too_close
        PlannerError.COMPUTE_FAILED -> Res.string.planner_error_compute_failed
        PlannerError.COVERAGE_NEEDS_POINT -> Res.string.planner_error_coverage_needs_point
    },
)

@Composable
internal fun hintText(side: PlannerSide, hint: PlannerHint): String {
    val name = sideName(side)
    return when (hint) {
        PlannerHint.POINT_MISSING -> stringResource(Res.string.planner_hint_point_missing, name)
        PlannerHint.STATION_NO_POSITION -> stringResource(Res.string.planner_hint_station_no_position, name)
    }
}

@Composable
internal fun weatherErrorText(error: PlannerWeatherError): String = stringResource(
    when (error) {
        PlannerWeatherError.OFFLINE -> Res.string.planner_weather_error_offline
        PlannerWeatherError.HTTP_ERROR -> Res.string.planner_weather_error_http
        PlannerWeatherError.BAD_RESPONSE -> Res.string.planner_weather_error_bad_response
    },
)

@Composable
internal fun ductingText(level: DuctingLevel): String = stringResource(
    when (level) {
        DuctingLevel.NORMAL -> Res.string.planner_ducting_normal
        DuctingLevel.ELEVATED -> Res.string.planner_ducting_elevated
        DuctingLevel.POSSIBLE_DUCT -> Res.string.planner_ducting_possible
    },
)

@Composable
internal fun clutterText(preset: PlannerClutterPreset): String = stringResource(
    when (preset) {
        PlannerClutterPreset.NONE -> Res.string.planner_clutter_none
        PlannerClutterPreset.RURAL_OPEN -> Res.string.planner_clutter_rural_open
        PlannerClutterPreset.FOREST_LIGHT -> Res.string.planner_clutter_forest_light
        PlannerClutterPreset.FOREST_DENSE -> Res.string.planner_clutter_forest_dense
        PlannerClutterPreset.SUBURBAN -> Res.string.planner_clutter_suburban
        PlannerClutterPreset.URBAN -> Res.string.planner_clutter_urban
        PlannerClutterPreset.CUSTOM -> Res.string.planner_clutter_custom
    },
)

@Composable
internal fun itmModeText(mode: ItmMode): String = stringResource(
    when (mode) {
        ItmMode.NOT_SET -> Res.string.planner_itm_mode_unknown
        ItmMode.LINE_OF_SIGHT -> Res.string.planner_itm_mode_los
        ItmMode.DIFFRACTION -> Res.string.planner_itm_mode_diffraction
        ItmMode.TROPOSCATTER -> Res.string.planner_itm_mode_troposcatter
    },
)
