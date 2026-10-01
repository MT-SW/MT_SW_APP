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
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ChannelOption
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_atmospheric_loss
import org.meshtastic.core.resources.planner_band_free
import org.meshtastic.core.resources.planner_bandwidth
import org.meshtastic.core.resources.planner_clutter
import org.meshtastic.core.resources.planner_clutter_title
import org.meshtastic.core.resources.planner_ducting_warning
import org.meshtastic.core.resources.planner_extra_loss
import org.meshtastic.core.resources.planner_frequency
import org.meshtastic.core.resources.planner_frequency_title
import org.meshtastic.core.resources.planner_k_factor
import org.meshtastic.core.resources.planner_k_factor_title
import org.meshtastic.core.resources.planner_modem_custom
import org.meshtastic.core.resources.planner_modem_override_note
import org.meshtastic.core.resources.planner_modem_preset
import org.meshtastic.core.resources.planner_modem_title
import org.meshtastic.core.resources.planner_noise_figure
import org.meshtastic.core.resources.planner_noise_floor
import org.meshtastic.core.resources.planner_refractivity
import org.meshtastic.core.resources.planner_section_environment
import org.meshtastic.core.resources.planner_section_radio
import org.meshtastic.core.resources.planner_sensitivity
import org.meshtastic.core.resources.planner_sensitivity_title
import org.meshtastic.core.resources.planner_spreading_factor
import org.meshtastic.core.resources.planner_unit_db
import org.meshtastic.core.resources.planner_unit_dbm
import org.meshtastic.core.resources.planner_unit_khz
import org.meshtastic.core.resources.planner_unit_mhz
import org.meshtastic.core.resources.planner_use_weather
import org.meshtastic.core.resources.planner_weather_failed
import org.meshtastic.core.resources.planner_weather_idle
import org.meshtastic.core.resources.planner_weather_loading
import org.meshtastic.core.resources.planner_weather_ready
import org.meshtastic.core.resources.planner_weather_refresh
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Refresh
import org.meshtastic.feature.map.planner.DuctingLevel
import org.meshtastic.feature.map.planner.PlannerBands
import org.meshtastic.feature.map.planner.Sensitivity
import org.meshtastic.feature.map.planner.export.Num
import org.meshtastic.feature.map.planner.state.PlannerClutterPreset
import org.meshtastic.feature.map.planner.state.PlannerUiState
import org.meshtastic.feature.map.planner.state.PlannerViewModel
import org.meshtastic.feature.map.planner.state.PlannerWeatherStatus

private const val MAX_BW_KHZ = 2000.0
private const val MAX_NF_DB = 30.0
private const val MAX_EXTRA_LOSS_DB = 100.0
private const val SF_MIN = 5
private const val SF_MAX = 12

/** Global radio parameters and the environment (weather, clutter): two cards. */
@Composable
internal fun RadioSection(state: PlannerUiState, vm: PlannerViewModel) {
    RadioCard(state = state, vm = vm)
    EnvironmentCard(state = state, vm = vm)
}

private fun presetLabel(option: ChannelOption): String =
    option.modemPreset.name

@Composable
private fun RadioCard(state: PlannerUiState, vm: PlannerViewModel) {
    PlannerCard(title = stringResource(Res.string.planner_section_radio)) {
        PlannerSubheading(text = stringResource(Res.string.planner_frequency_title), info = PlannerInfoTopic.FREQUENCY)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlannerBands.all.forEach { band ->
                FilterChip(
                    selected = state.bandId == band.id,
                    onClick = { vm.setBand(band.id) },
                    label = {
                        Text(if (band.id == PlannerBands.FREE_ID) stringResource(Res.string.planner_band_free) else band.name)
                    },
                )
            }
        }
        PlannerNumberField(
            label = stringResource(Res.string.planner_frequency),
            value = state.frequencyMHz,
            onValue = { vm.setFrequency(it) },
            decimals = 5,
            suffix = stringResource(Res.string.planner_unit_mhz),
            min = PlannerBands.FREE_MIN_MHZ,
            max = PlannerBands.FREE_MAX_MHZ,
        )

        PlannerSubheading(text = stringResource(Res.string.planner_modem_title), info = PlannerInfoTopic.MODEM)
        PlannerDropdown(
            label = stringResource(Res.string.planner_modem_preset),
            selectedLabel =
            state.modemPreset?.let { p -> ChannelOption.from(p)?.let { presetLabel(it) } ?: p.name }
                ?: stringResource(Res.string.planner_modem_custom),
            options = ChannelOption.entries.map { it.modemPreset to presetLabel(it) },
            onSelect = { vm.setRadioFromPreset(it) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            PlannerNumberField(
                label = stringResource(Res.string.planner_bandwidth),
                value = state.bandwidthKhz,
                onValue = { vm.setBandwidthKhz(it) },
                modifier = Modifier.weight(1f),
                decimals = 2,
                suffix = stringResource(Res.string.planner_unit_khz),
                min = 1.0,
                max = MAX_BW_KHZ,
            )
            PlannerDropdown(
                label = stringResource(Res.string.planner_spreading_factor),
                selectedLabel = "SF" + state.spreadingFactor,
                options = (SF_MIN..SF_MAX).map { it to "SF$it" },
                onSelect = { vm.setSpreadingFactor(it) },
                modifier = Modifier.weight(1f),
            )
        }
        if (state.radioOverride) {
            Text(
                text = stringResource(Res.string.planner_modem_override_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        PlannerSubheading(text = stringResource(Res.string.planner_sensitivity_title), info = PlannerInfoTopic.SENSITIVITY)
        PlannerNumberField(
            label = stringResource(Res.string.planner_noise_figure),
            value = state.noiseFigureDb,
            onValue = { vm.setNoiseFigure(it) },
            decimals = 1,
            suffix = stringResource(Res.string.planner_unit_db),
            min = 0.0,
            max = MAX_NF_DB,
        )
        PlannerValueRow(
            label = stringResource(Res.string.planner_noise_floor),
            value = fmt(state.noiseFloorDbm, 1) + " " + stringResource(Res.string.planner_unit_dbm),
        )
        PlannerValueRow(
            label = stringResource(Res.string.planner_sensitivity),
            value =
            fmt(Sensitivity.dbm(state.bandwidthKhz, state.spreadingFactor, state.noiseFigureDb), 1) +
                " " + stringResource(Res.string.planner_unit_dbm),
            bold = true,
        )
    }
}

@Composable
private fun clutterLabel(preset: PlannerClutterPreset): String {
    val base = clutterText(preset)
    return if (preset != PlannerClutterPreset.CUSTOM && preset.extraDb > 0.0) {
        base + " (" + fmt(preset.extraDb, 0) + " " + stringResource(Res.string.planner_unit_db) + ")"
    } else {
        base
    }
}

@Composable
private fun EnvironmentCard(state: PlannerUiState, vm: PlannerViewModel) {
    PlannerCard(title = stringResource(Res.string.planner_section_environment)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { vm.setUseWeather(!state.useWeather) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = state.useWeather, onCheckedChange = null)
            Text(
                text = stringResource(Res.string.planner_use_weather),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            PlannerInfoButton(PlannerInfoTopic.WEATHER)
        }
        if (state.useWeather) WeatherStatusBlock(state = state, vm = vm)

        PlannerSubheading(text = stringResource(Res.string.planner_k_factor_title), info = PlannerInfoTopic.K_FACTOR)
        PlannerValueRow(label = stringResource(Res.string.planner_k_factor), value = fmt(state.kFactor, 3))
        PlannerValueRow(label = stringResource(Res.string.planner_refractivity), value = fmt(state.surfaceRefractivity, 1))
        if (state.atmosphericLossDb > 0.0) {
            PlannerValueRow(
                label = stringResource(Res.string.planner_atmospheric_loss),
                value = fmt(state.atmosphericLossDb, 2) + " " + stringResource(Res.string.planner_unit_db),
            )
        }

        PlannerSubheading(text = stringResource(Res.string.planner_clutter_title), info = PlannerInfoTopic.CLUTTER)
        PlannerDropdown(
            label = stringResource(Res.string.planner_clutter),
            selectedLabel = clutterLabel(state.clutterPreset),
            options = PlannerClutterPreset.entries.map { it to clutterLabel(it) },
            onSelect = { vm.setClutterPreset(it) },
        )
        PlannerNumberField(
            label = stringResource(Res.string.planner_extra_loss),
            value = state.extraLossDb,
            onValue = { vm.setExtraLossDb(it) },
            decimals = 1,
            suffix = stringResource(Res.string.planner_unit_db),
            min = 0.0,
            max = MAX_EXTRA_LOSS_DB,
        )
    }
}

@Composable
private fun WeatherStatusBlock(state: PlannerUiState, vm: PlannerViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val status = state.weather
        when (status) {
            PlannerWeatherStatus.Idle ->
                Text(
                    text = stringResource(Res.string.planner_weather_idle),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            PlannerWeatherStatus.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(Res.string.planner_weather_loading),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
            is PlannerWeatherStatus.Ready -> {
                val c = status.conditions
                Text(
                    text =
                    stringResource(
                        Res.string.planner_weather_ready,
                        ductingText(c.level),
                        fmt(c.analysis.kFactor, 3),
                        c.fetchedAtIso,
                        c.source,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
            is PlannerWeatherStatus.Failed ->
                Text(
                    text = stringResource(Res.string.planner_weather_failed, weatherErrorText(status.error)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
        }
        IconButton(onClick = { vm.refreshWeather() }) {
            Icon(MeshtasticIcons.Refresh, contentDescription = stringResource(Res.string.planner_weather_refresh))
        }
    }
    val ready = state.weather as? PlannerWeatherStatus.Ready
    if (ready != null && ready.conditions.level == DuctingLevel.POSSIBLE_DUCT) {
        PlannerNotice(text = stringResource(Res.string.planner_ducting_warning), isError = true)
    }
}
