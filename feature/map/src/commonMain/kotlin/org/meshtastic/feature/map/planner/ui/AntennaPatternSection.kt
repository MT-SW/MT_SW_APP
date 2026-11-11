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
@file:OptIn(ExperimentalLayoutApi::class)

package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.antenna.AntennaPatterns
import org.meshtastic.core.model.antenna.PatternKind
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_pattern_aim_other
import org.meshtastic.core.resources.planner_pattern_azimuth
import org.meshtastic.core.resources.planner_pattern_beamwidth
import org.meshtastic.core.resources.planner_pattern_elements
import org.meshtastic.core.resources.planner_pattern_front_to_back
import org.meshtastic.core.resources.planner_pattern_kind_biquad
import org.meshtastic.core.resources.planner_pattern_kind_dish
import org.meshtastic.core.resources.planner_pattern_kind_helix
import org.meshtastic.core.resources.planner_pattern_kind_moxon
import org.meshtastic.core.resources.planner_pattern_kind_omni
import org.meshtastic.core.resources.planner_pattern_kind_panel
import org.meshtastic.core.resources.planner_pattern_kind_sector
import org.meshtastic.core.resources.planner_pattern_kind_yagi
import org.meshtastic.core.resources.planner_pattern_reset
import org.meshtastic.core.resources.planner_pattern_tilt
import org.meshtastic.core.resources.planner_pattern_title
import org.meshtastic.core.ui.component.AntennaPatternPlot
import org.meshtastic.feature.map.planner.state.PlannerEnd
import org.meshtastic.feature.map.planner.state.PlannerSide
import org.meshtastic.feature.map.planner.state.PlannerViewModel

private fun kindName(kind: PatternKind): StringResource = when (kind) {
    PatternKind.OMNI -> Res.string.planner_pattern_kind_omni
    PatternKind.YAGI -> Res.string.planner_pattern_kind_yagi
    PatternKind.MOXON -> Res.string.planner_pattern_kind_moxon
    PatternKind.PANEL -> Res.string.planner_pattern_kind_panel
    PatternKind.BIQUAD -> Res.string.planner_pattern_kind_biquad
    PatternKind.HELIX -> Res.string.planner_pattern_kind_helix
    PatternKind.DISH -> Res.string.planner_pattern_kind_dish
    PatternKind.SECTOR -> Res.string.planner_pattern_kind_sector
}

/**
 * The kind of antenna and how it radiates around the horizon: a directional antenna is turned towards the other end (or
 * a typed bearing), and the link and the coverage area follow from it.
 */
@Composable
@Suppress("MultipleEmitters")
internal fun AntennaPatternSection(side: PlannerSide, end: PlannerEnd, vm: PlannerViewModel) {
    PlannerSubheading(text = stringResource(Res.string.planner_pattern_title))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PatternKind.entries.forEach { kind ->
            FilterChip(
                selected = end.patternKind == kind,
                onClick = { vm.setPatternKind(side, kind) },
                label = { Text(stringResource(kindName(kind))) },
            )
        }
    }
    if (AntennaPatterns.hasElements(end.patternKind)) {
        PlannerNumberField(
            label = stringResource(Res.string.planner_pattern_elements),
            value = end.patternElements.toDouble(),
            onValue = { vm.setPatternElements(side, it.toInt()) },
            decimals = 0,
            min = AntennaPatterns.MIN_ELEMENTS.toDouble(),
            max = AntennaPatterns.MAX_ELEMENTS.toDouble(),
        )
    }
    if (end.patternKind.directional) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(checked = end.aimAtOther, onCheckedChange = { vm.setAimAtOther(side, it) })
            Text(stringResource(Res.string.planner_pattern_aim_other), style = MaterialTheme.typography.bodyMedium)
        }
        AimFields(side, end, vm)
        PlannerNumberField(
            label = stringResource(Res.string.planner_pattern_beamwidth),
            value = end.beamwidthDeg,
            onValue = { vm.setBeamwidth(side, it) },
            decimals = 0,
            suffix = "°",
            min = 5.0,
            max = 360.0,
        )
        PlannerNumberField(
            label = stringResource(Res.string.planner_pattern_front_to_back),
            value = end.frontToBackDb,
            onValue = { vm.setFrontToBack(side, it) },
            decimals = 0,
            suffix = "dB",
            min = 0.0,
            max = 60.0,
        )
        TextButton(onClick = { vm.resetPattern(side) }) { Text(stringResource(Res.string.planner_pattern_reset)) }
    }
    AntennaPatternPlot(
        beamwidthDeg = end.beamwidthDeg,
        frontToBackDb = end.frontToBackDb,
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(PLOT_WIDTH_SHARE),
    )
}

/** The direction (azimuth) and the tilt of a directional antenna. */
@Composable
@Suppress("MultipleEmitters")
private fun AimFields(side: PlannerSide, end: PlannerEnd, vm: PlannerViewModel) {
    PlannerNumberField(
        label = stringResource(Res.string.planner_pattern_azimuth),
        value = end.azimuthDeg,
        onValue = { vm.setAzimuth(side, it) },
        decimals = 0,
        suffix = "°",
        min = 0.0,
        max = 359.0,
    )
    PlannerNumberField(
        label = stringResource(Res.string.planner_pattern_tilt),
        value = end.tiltDeg,
        onValue = { vm.setTilt(side, it) },
        decimals = 1,
        suffix = "°",
        min = -60.0,
        max = 60.0,
    )
}

private const val PLOT_WIDTH_SHARE = 0.6f
