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
@file:OptIn(ExperimentalMaterial3Api::class)

package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_antenna_gain
import org.meshtastic.core.resources.planner_antenna_gain_title
import org.meshtastic.core.resources.planner_eirp
import org.meshtastic.core.resources.planner_section_station
import org.meshtastic.core.resources.planner_tx_power_dbm
import org.meshtastic.core.resources.planner_tx_power_title
import org.meshtastic.core.resources.planner_tx_power_w
import org.meshtastic.core.resources.planner_unit_dbi
import org.meshtastic.feature.map.planner.state.PlannerEnd
import org.meshtastic.feature.map.planner.state.PlannerUiState
import org.meshtastic.feature.map.planner.state.PlannerViewModel

private const val MIN_DBM = -10.0
private const val MAX_DBM = 50.0
private const val MIN_WATTS = 0.0001
private const val MAX_WATTS = 100.0
private const val MIN_GAIN = -10.0
private const val MAX_GAIN = 60.0
private const val WATT_DECIMALS = 4

/** Transmit power, antenna gain and feeder of the selected side. */
@Composable
internal fun StationSection(state: PlannerUiState, vm: PlannerViewModel) {
    val side = state.selectedSide
    val end = state.end(side)
    PlannerCard(title = stringResource(Res.string.planner_section_station, sideName(side))) {
        key(side) {
            TxPowerFields(side = side, end = end, vm = vm)
            PlannerSubheading(
                text = stringResource(Res.string.planner_antenna_gain_title),
                info = PlannerInfoTopic.ANTENNA_GAIN,
            )
            PlannerNumberField(
                label = stringResource(Res.string.planner_antenna_gain),
                value = end.antennaGainDbi,
                onValue = { vm.setAntennaGain(side, it) },
                decimals = 2,
                suffix = stringResource(Res.string.planner_unit_dbi),
                min = MIN_GAIN,
                max = MAX_GAIN,
            )
            FeederSection(side = side, end = end, vm = vm)
            val eirp = end.txPowerDbm + end.antennaGainDbi - end.feederLossDb
            Text(
                text =
                stringResource(
                    Res.string.planner_eirp,
                    fmt(eirp, 1),
                    wattsText(PlannerEnd.dbmToWatts(eirp)),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun wattsText(watts: Double): String = fmt(watts, if (watts >= 1.0) 2 else WATT_DECIMALS)

@Composable
private fun TxPowerFields(side: org.meshtastic.feature.map.planner.state.PlannerSide, end: PlannerEnd, vm: PlannerViewModel) {
    var inWatts by remember { mutableStateOf(false) }
    PlannerSubheading(text = stringResource(Res.string.planner_tx_power_title), info = PlannerInfoTopic.TX_POWER)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        key(inWatts) {
            PlannerNumberField(
                label =
                stringResource(
                    if (inWatts) Res.string.planner_tx_power_w else Res.string.planner_tx_power_dbm,
                ),
                value = if (inWatts) end.txPowerW else end.txPowerDbm,
                onValue = { if (inWatts) vm.setTxPowerW(side, it) else vm.setTxPowerDbm(side, it) },
                modifier = Modifier.weight(1f),
                decimals = if (inWatts) WATT_DECIMALS else 1,
                min = if (inWatts) MIN_WATTS else MIN_DBM,
                max = if (inWatts) MAX_WATTS else MAX_DBM,
                supporting =
                if (inWatts) {
                    "= " + fmt(end.txPowerDbm, 1) + " dBm"
                } else {
                    "= " + wattsText(end.txPowerW) + " W"
                },
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(top = 8.dp)) {
            listOf(false to "dBm", true to "W").forEachIndexed { index, (watts, label) ->
                SegmentedButton(
                    selected = inWatts == watts,
                    onClick = { inWatts = watts },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                ) {
                    Text(label)
                }
            }
        }
    }
}
