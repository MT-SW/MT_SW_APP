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
@file:Suppress("CyclomaticComplexMethod", "LongMethod")

package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_close
import org.meshtastic.core.resources.planner_credits_line_cables
import org.meshtastic.core.resources.planner_credits_line_itm
import org.meshtastic.core.resources.planner_credits_line_mapterhorn
import org.meshtastic.core.resources.planner_credits_line_meshmap
import org.meshtastic.core.resources.planner_credits_line_openmeteo
import org.meshtastic.core.resources.planner_credits_line_osm
import org.meshtastic.core.resources.planner_credits_line_siteplanner_original
import org.meshtastic.core.resources.planner_credits_line_splat
import org.meshtastic.core.resources.planner_info_antenna_gain_body
import org.meshtastic.core.resources.planner_info_antenna_gain_title
import org.meshtastic.core.resources.planner_info_antenna_height_body
import org.meshtastic.core.resources.planner_info_antenna_height_title
import org.meshtastic.core.resources.planner_info_bearing_body
import org.meshtastic.core.resources.planner_info_bearing_title
import org.meshtastic.core.resources.planner_info_button
import org.meshtastic.core.resources.planner_info_clutter_body
import org.meshtastic.core.resources.planner_info_clutter_title
import org.meshtastic.core.resources.planner_info_coverage_body
import org.meshtastic.core.resources.planner_info_coverage_title
import org.meshtastic.core.resources.planner_info_credits_body
import org.meshtastic.core.resources.planner_info_credits_title
import org.meshtastic.core.resources.planner_info_export_body
import org.meshtastic.core.resources.planner_info_export_title
import org.meshtastic.core.resources.planner_info_feeder_body
import org.meshtastic.core.resources.planner_info_feeder_precise_body
import org.meshtastic.core.resources.planner_info_feeder_precise_title
import org.meshtastic.core.resources.planner_info_feeder_title
import org.meshtastic.core.resources.planner_info_frequency_body
import org.meshtastic.core.resources.planner_info_frequency_title
import org.meshtastic.core.resources.planner_info_fresnel_body
import org.meshtastic.core.resources.planner_info_fresnel_title
import org.meshtastic.core.resources.planner_info_general_body
import org.meshtastic.core.resources.planner_info_general_title
import org.meshtastic.core.resources.planner_info_ground_alt_body
import org.meshtastic.core.resources.planner_info_ground_alt_title
import org.meshtastic.core.resources.planner_info_k_factor_body
import org.meshtastic.core.resources.planner_info_k_factor_title
import org.meshtastic.core.resources.planner_info_link_result_body
import org.meshtastic.core.resources.planner_info_link_result_title
import org.meshtastic.core.resources.planner_info_margin_body
import org.meshtastic.core.resources.planner_info_margin_title
import org.meshtastic.core.resources.planner_info_measured_body
import org.meshtastic.core.resources.planner_info_measured_title
import org.meshtastic.core.resources.planner_info_modem_body
import org.meshtastic.core.resources.planner_info_modem_title
import org.meshtastic.core.resources.planner_info_points_body
import org.meshtastic.core.resources.planner_info_points_title
import org.meshtastic.core.resources.planner_info_profile_body
import org.meshtastic.core.resources.planner_info_profile_title
import org.meshtastic.core.resources.planner_info_sensitivity_body
import org.meshtastic.core.resources.planner_info_sensitivity_title
import org.meshtastic.core.resources.planner_info_tx_power_body
import org.meshtastic.core.resources.planner_info_tx_power_title
import org.meshtastic.core.resources.planner_info_weather_body
import org.meshtastic.core.resources.planner_info_weather_title
import org.meshtastic.core.resources.planner_open_tropo_reference
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons

/** Topics of the "i" buttons. Texts: `planner_info_<topic lowercase>_title` / `_body`. */
enum class PlannerInfoTopic {
    GENERAL,
    POINTS,
    GROUND_ALT,
    ANTENNA_HEIGHT,
    TX_POWER,
    ANTENNA_GAIN,
    FEEDER,
    FEEDER_PRECISE,
    FREQUENCY,
    MODEM,
    SENSITIVITY,
    K_FACTOR,
    WEATHER,
    CLUTTER,
    PROFILE,
    FRESNEL,
    LINK_RESULT,
    MARGIN,
    BEARING,
    MEASURED,
    COVERAGE,
    EXPORT,
    CREDITS,
}

private const val TROPO_URL = "https://www.dxinfocentre.com/tropo_eur.html"

private fun PlannerInfoTopic.titleRes(): StringResource = when (this) {
    PlannerInfoTopic.GENERAL -> Res.string.planner_info_general_title
    PlannerInfoTopic.POINTS -> Res.string.planner_info_points_title
    PlannerInfoTopic.GROUND_ALT -> Res.string.planner_info_ground_alt_title
    PlannerInfoTopic.ANTENNA_HEIGHT -> Res.string.planner_info_antenna_height_title
    PlannerInfoTopic.TX_POWER -> Res.string.planner_info_tx_power_title
    PlannerInfoTopic.ANTENNA_GAIN -> Res.string.planner_info_antenna_gain_title
    PlannerInfoTopic.FEEDER -> Res.string.planner_info_feeder_title
    PlannerInfoTopic.FEEDER_PRECISE -> Res.string.planner_info_feeder_precise_title
    PlannerInfoTopic.FREQUENCY -> Res.string.planner_info_frequency_title
    PlannerInfoTopic.MODEM -> Res.string.planner_info_modem_title
    PlannerInfoTopic.SENSITIVITY -> Res.string.planner_info_sensitivity_title
    PlannerInfoTopic.K_FACTOR -> Res.string.planner_info_k_factor_title
    PlannerInfoTopic.WEATHER -> Res.string.planner_info_weather_title
    PlannerInfoTopic.CLUTTER -> Res.string.planner_info_clutter_title
    PlannerInfoTopic.PROFILE -> Res.string.planner_info_profile_title
    PlannerInfoTopic.FRESNEL -> Res.string.planner_info_fresnel_title
    PlannerInfoTopic.LINK_RESULT -> Res.string.planner_info_link_result_title
    PlannerInfoTopic.MARGIN -> Res.string.planner_info_margin_title
    PlannerInfoTopic.BEARING -> Res.string.planner_info_bearing_title
    PlannerInfoTopic.MEASURED -> Res.string.planner_info_measured_title
    PlannerInfoTopic.COVERAGE -> Res.string.planner_info_coverage_title
    PlannerInfoTopic.EXPORT -> Res.string.planner_info_export_title
    PlannerInfoTopic.CREDITS -> Res.string.planner_info_credits_title
}

private fun PlannerInfoTopic.bodyRes(): StringResource = when (this) {
    PlannerInfoTopic.GENERAL -> Res.string.planner_info_general_body
    PlannerInfoTopic.POINTS -> Res.string.planner_info_points_body
    PlannerInfoTopic.GROUND_ALT -> Res.string.planner_info_ground_alt_body
    PlannerInfoTopic.ANTENNA_HEIGHT -> Res.string.planner_info_antenna_height_body
    PlannerInfoTopic.TX_POWER -> Res.string.planner_info_tx_power_body
    PlannerInfoTopic.ANTENNA_GAIN -> Res.string.planner_info_antenna_gain_body
    PlannerInfoTopic.FEEDER -> Res.string.planner_info_feeder_body
    PlannerInfoTopic.FEEDER_PRECISE -> Res.string.planner_info_feeder_precise_body
    PlannerInfoTopic.FREQUENCY -> Res.string.planner_info_frequency_body
    PlannerInfoTopic.MODEM -> Res.string.planner_info_modem_body
    PlannerInfoTopic.SENSITIVITY -> Res.string.planner_info_sensitivity_body
    PlannerInfoTopic.K_FACTOR -> Res.string.planner_info_k_factor_body
    PlannerInfoTopic.WEATHER -> Res.string.planner_info_weather_body
    PlannerInfoTopic.CLUTTER -> Res.string.planner_info_clutter_body
    PlannerInfoTopic.PROFILE -> Res.string.planner_info_profile_body
    PlannerInfoTopic.FRESNEL -> Res.string.planner_info_fresnel_body
    PlannerInfoTopic.LINK_RESULT -> Res.string.planner_info_link_result_body
    PlannerInfoTopic.MARGIN -> Res.string.planner_info_margin_body
    PlannerInfoTopic.BEARING -> Res.string.planner_info_bearing_body
    PlannerInfoTopic.MEASURED -> Res.string.planner_info_measured_body
    PlannerInfoTopic.COVERAGE -> Res.string.planner_info_coverage_body
    PlannerInfoTopic.EXPORT -> Res.string.planner_info_export_body
    PlannerInfoTopic.CREDITS -> Res.string.planner_info_credits_body
}

/** Small "i" icon button that opens the [PlannerInfoDialog] of [topic]. */
@Composable
fun PlannerInfoButton(topic: PlannerInfoTopic, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier.size(36.dp)) {
        Icon(
            imageVector = MeshtasticIcons.Info,
            contentDescription = stringResource(Res.string.planner_info_button),
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    if (open) PlannerInfoDialog(topic = topic, onDismiss = { open = false })
}

/** Scrollable dialog with the long explanation of [topic]. */
@Composable
fun PlannerInfoDialog(topic: PlannerInfoTopic, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(topic.titleRes())) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = stringResource(topic.bodyRes()), style = MaterialTheme.typography.bodyMedium)
                if (topic == PlannerInfoTopic.WEATHER) {
                    TextButton(onClick = { uriHandler.openUri(TROPO_URL) }) {
                        Text(stringResource(Res.string.planner_open_tropo_reference))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.planner_close)) } },
    )
}

/** Attribution lines of the exported reports (the on-screen text is the CREDITS info topic). */
@Composable
internal fun PlannerCreditLines(): List<String> = listOf(
    stringResource(Res.string.planner_credits_line_meshmap),
    stringResource(Res.string.planner_credits_line_siteplanner_original),
    stringResource(Res.string.planner_credits_line_splat),
    stringResource(Res.string.planner_credits_line_itm),
    stringResource(Res.string.planner_credits_line_mapterhorn),
    stringResource(Res.string.planner_credits_line_openmeteo),
    stringResource(Res.string.planner_credits_line_osm),
    stringResource(Res.string.planner_credits_line_cables),
)
