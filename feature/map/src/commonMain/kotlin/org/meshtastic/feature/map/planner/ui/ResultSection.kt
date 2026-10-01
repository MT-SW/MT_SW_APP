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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_bearing_ab
import org.meshtastic.core.resources.planner_bearing_ba
import org.meshtastic.core.resources.planner_bearing_note
import org.meshtastic.core.resources.planner_bearing_title
import org.meshtastic.core.resources.planner_direction_ab
import org.meshtastic.core.resources.planner_direction_ba
import org.meshtastic.core.resources.planner_distance
import org.meshtastic.core.resources.planner_elevation_a
import org.meshtastic.core.resources.planner_elevation_b
import org.meshtastic.core.resources.planner_extra_loss_result
import org.meshtastic.core.resources.planner_fresnel
import org.meshtastic.core.resources.planner_fresnel_note
import org.meshtastic.core.resources.planner_fspl
import org.meshtastic.core.resources.planner_itm_failed
import org.meshtastic.core.resources.planner_itm_loss
import org.meshtastic.core.resources.planner_itm_mode
import org.meshtastic.core.resources.planner_itm_warnings
import org.meshtastic.core.resources.planner_k_factor
import org.meshtastic.core.resources.planner_los
import org.meshtastic.core.resources.planner_los_title
import org.meshtastic.core.resources.planner_losses_title
import org.meshtastic.core.resources.planner_margin
import org.meshtastic.core.resources.planner_margin_title
import org.meshtastic.core.resources.planner_measured_delta_rssi
import org.meshtastic.core.resources.planner_measured_delta_snr
import org.meshtastic.core.resources.planner_measured_meas_rssi
import org.meshtastic.core.resources.planner_measured_meas_snr
import org.meshtastic.core.resources.planner_measured_missing
import org.meshtastic.core.resources.planner_measured_node
import org.meshtastic.core.resources.planner_measured_not_direct
import org.meshtastic.core.resources.planner_measured_note
import org.meshtastic.core.resources.planner_measured_pred_rssi
import org.meshtastic.core.resources.planner_measured_pred_snr
import org.meshtastic.core.resources.planner_no
import org.meshtastic.core.resources.planner_result_need_points
import org.meshtastic.core.resources.planner_rx_power
import org.meshtastic.core.resources.planner_section_measured
import org.meshtastic.core.resources.planner_section_result
import org.meshtastic.core.resources.planner_sensitivity
import org.meshtastic.core.resources.planner_total_loss
import org.meshtastic.core.resources.planner_unit_db
import org.meshtastic.core.resources.planner_unit_dbm
import org.meshtastic.core.resources.planner_unit_km
import org.meshtastic.core.resources.planner_unit_m
import org.meshtastic.core.resources.planner_yes
import org.meshtastic.core.ui.theme.StatusColors.StatusGreen
import org.meshtastic.core.ui.theme.StatusColors.StatusOrange
import org.meshtastic.feature.map.planner.DirectionResult
import org.meshtastic.feature.map.planner.LinkBudget
import org.meshtastic.feature.map.planner.LinkResult
import org.meshtastic.feature.map.planner.state.MeasuredComparison
import org.meshtastic.feature.map.planner.state.PlannerSide
import org.meshtastic.feature.map.planner.state.PlannerUiState

private const val RECOMMENDED_FRESNEL_RATIO = 0.6

/** Link result: distance, bearings, losses, margins per direction, line of sight / Fresnel, measured comparison. */
@Composable
internal fun ResultSection(state: PlannerUiState) {
    PlannerCard(title = stringResource(Res.string.planner_section_result), info = PlannerInfoTopic.LINK_RESULT) {
        state.error?.let { PlannerNotice(text = errorText(it), isError = true) }
        if (state.computing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (!state.isLinkReady) {
            PlannerNotice(text = stringResource(Res.string.planner_result_need_points))
            PlannerSide.entries.forEach { side ->
                state.hintsBySide[side].orEmpty().forEach { hint -> PlannerNotice(text = hintText(side, hint)) }
            }
        }
        val link = state.link
        if (link != null && state.isLinkReady) LinkDetails(state = state, link = link)
    }
    state.comparison?.let { MeasuredCard(it) }
}

@Composable
private fun LinkDetails(state: PlannerUiState, link: LinkResult) {
    val unitDb = stringResource(Res.string.planner_unit_db)
    val unitDbm = stringResource(Res.string.planner_unit_dbm)
    val unitM = stringResource(Res.string.planner_unit_m)
    val unitKm = stringResource(Res.string.planner_unit_km)
    val degree = "°"
    PlannerValueRow(
        label = stringResource(Res.string.planner_distance),
        value = fmtDistance(link.distanceM, unitM, unitKm),
        bold = true,
    )

    PlannerSubheading(text = stringResource(Res.string.planner_bearing_title), info = PlannerInfoTopic.BEARING)
    PlannerValueRow(stringResource(Res.string.planner_bearing_ab), fmt(link.bearingAToBDeg, 1) + degree)
    PlannerValueRow(stringResource(Res.string.planner_bearing_ba), fmt(link.bearingBToADeg, 1) + degree)
    PlannerValueRow(stringResource(Res.string.planner_elevation_a), fmt(link.elevationAAngleDeg, 2) + degree)
    PlannerValueRow(stringResource(Res.string.planner_elevation_b), fmt(link.elevationBAngleDeg, 2) + degree)
    Text(
        text = stringResource(Res.string.planner_bearing_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    HorizontalDivider()
    PlannerSubheading(text = stringResource(Res.string.planner_losses_title))
    PlannerValueRow(stringResource(Res.string.planner_fspl), fmt(link.freeSpaceLossDb, 1) + " " + unitDb)
    val itmFailed = link.itmLossDb >= LinkBudget.ITM_ERROR_LOSS_DB
    if (itmFailed) {
        PlannerNotice(text = stringResource(Res.string.planner_itm_failed), isError = true)
    } else {
        PlannerValueRow(stringResource(Res.string.planner_itm_loss), fmt(link.itmLossDb, 1) + " " + unitDb)
        PlannerValueRow(
            stringResource(Res.string.planner_extra_loss_result),
            fmt(link.totalPathLossDb - link.itmLossDb, 1) + " " + unitDb,
        )
        PlannerValueRow(
            stringResource(Res.string.planner_total_loss),
            fmt(link.totalPathLossDb, 1) + " " + unitDb,
            bold = true,
        )
    }
    PlannerValueRow(stringResource(Res.string.planner_sensitivity), fmt(link.sensitivityDbm, 1) + " " + unitDbm)
    PlannerValueRow(stringResource(Res.string.planner_itm_mode), itmModeText(link.itmMode))
    if (link.itmWarnings != 0) {
        PlannerNotice(text = stringResource(Res.string.planner_itm_warnings, link.itmWarnings.toString()))
    }

    HorizontalDivider()
    PlannerSubheading(text = stringResource(Res.string.planner_margin_title), info = PlannerInfoTopic.MARGIN)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DirectionCard(
            title = stringResource(Res.string.planner_direction_ab),
            dir = link.aToB,
            modifier = Modifier.weight(1f),
        )
        DirectionCard(
            title = stringResource(Res.string.planner_direction_ba),
            dir = link.bToA,
            modifier = Modifier.weight(1f),
        )
    }

    HorizontalDivider()
    PlannerSubheading(text = stringResource(Res.string.planner_los_title), info = PlannerInfoTopic.FRESNEL)
    PlannerValueRow(
        label = stringResource(Res.string.planner_los),
        value = stringResource(if (link.lineOfSightClear) Res.string.planner_yes else Res.string.planner_no),
        valueColor = if (link.lineOfSightClear) MaterialTheme.colorScheme.StatusGreen else MaterialTheme.colorScheme.StatusOrange,
    )
    PlannerValueRow(
        label = stringResource(Res.string.planner_fresnel),
        value = fmt(link.worstFresnelClearanceM, 1) + " " + unitM + " (" + fmt(link.worstFresnelRatio * 100.0, 0) + " %)",
        valueColor =
        if (link.worstFresnelRatio >= RECOMMENDED_FRESNEL_RATIO) {
            MaterialTheme.colorScheme.StatusGreen
        } else {
            MaterialTheme.colorScheme.StatusOrange
        },
    )
    if (link.worstFresnelRatio < RECOMMENDED_FRESNEL_RATIO) {
        Text(
            text = stringResource(Res.string.planner_fresnel_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // The k factor / weather status is shown with the environment inputs; repeat the value that was used.
    PlannerValueRow(stringResource(Res.string.planner_k_factor), fmt(state.kFactor, 3))
}

@Composable
private fun DirectionCard(title: String, dir: DirectionResult, modifier: Modifier = Modifier) {
    val color: Color = verdictColor(dir.verdict)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, color),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(Res.string.planner_rx_power) + ": " + fmt(dir.rxPowerDbm, 1) + " " +
                    stringResource(Res.string.planner_unit_dbm),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.planner_margin) + ": " + fmtSigned(dir.marginDb, 1) + " " +
                    stringResource(Res.string.planner_unit_db),
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
            Text(
                text = verdictText(dir.verdict),
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MeasuredCard(cmp: MeasuredComparison) {
    val unitDb = stringResource(Res.string.planner_unit_db)
    val unitDbm = stringResource(Res.string.planner_unit_dbm)
    val missing = stringResource(Res.string.planner_measured_missing)
    PlannerCard(title = stringResource(Res.string.planner_section_measured), info = PlannerInfoTopic.MEASURED) {
        PlannerValueRow(
            label = stringResource(Res.string.planner_measured_node),
            value = cmp.nodeName.ifBlank { "!" + cmp.nodeNum.toUInt().toString(16) } + " (" + sideName(cmp.nodeSide) + ")",
        )
        if (!cmp.direct) PlannerNotice(text = stringResource(Res.string.planner_measured_not_direct))
        PlannerValueRow(
            stringResource(Res.string.planner_measured_pred_rssi),
            fmt(cmp.predictedRssiDbm, 1) + " " + unitDbm,
        )
        PlannerValueRow(
            stringResource(Res.string.planner_measured_meas_rssi),
            cmp.measuredRssiDbm?.let { it.toString() + " " + unitDbm } ?: missing,
        )
        cmp.rssiDeltaDb?.let { PlannerValueRow(stringResource(Res.string.planner_measured_delta_rssi), fmtSigned(it, 1) + " " + unitDb, bold = true) }
        PlannerValueRow(
            stringResource(Res.string.planner_measured_pred_snr),
            fmt(cmp.predictedSnrDb, 1) + " " + unitDb,
        )
        PlannerValueRow(
            stringResource(Res.string.planner_measured_meas_snr),
            cmp.measuredSnrDb?.let { fmt(it.toDouble(), 2) + " " + unitDb } ?: missing,
        )
        cmp.snrDeltaDb?.let { PlannerValueRow(stringResource(Res.string.planner_measured_delta_snr), fmtSigned(it, 1) + " " + unitDb, bold = true) }
        Text(
            text = stringResource(Res.string.planner_measured_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
