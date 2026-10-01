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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_feeder_approximate
import org.meshtastic.core.resources.planner_feeder_breakdown
import org.meshtastic.core.resources.planner_feeder_cable_n
import org.meshtastic.core.resources.planner_feeder_connector_count
import org.meshtastic.core.resources.planner_feeder_connector_n
import org.meshtastic.core.resources.planner_feeder_custom_cable
import org.meshtastic.core.resources.planner_feeder_custom_loss
import org.meshtastic.core.resources.planner_feeder_fewer
import org.meshtastic.core.resources.planner_feeder_length
import org.meshtastic.core.resources.planner_feeder_manual
import org.meshtastic.core.resources.planner_feeder_more
import org.meshtastic.core.resources.planner_feeder_precise
import org.meshtastic.core.resources.planner_feeder_no_cable
import org.meshtastic.core.resources.planner_feeder_preset
import org.meshtastic.core.resources.planner_feeder_preset_adapter_n_n
import org.meshtastic.core.resources.planner_feeder_preset_adapter_sma_n
import org.meshtastic.core.resources.planner_feeder_preset_custom
import org.meshtastic.core.resources.planner_feeder_preset_direct
import org.meshtastic.core.resources.planner_feeder_preset_ecoflex_roof
import org.meshtastic.core.resources.planner_feeder_preset_heliax_mast
import org.meshtastic.core.resources.planner_feeder_preset_long_lmr400
import org.meshtastic.core.resources.planner_feeder_preset_pigtail
import org.meshtastic.core.resources.planner_feeder_preset_pigtail_cable
import org.meshtastic.core.resources.planner_feeder_title
import org.meshtastic.core.resources.planner_feeder_total
import org.meshtastic.core.resources.planner_feeder_warn_custom_loss
import org.meshtastic.core.resources.planner_feeder_warn_frequency
import org.meshtastic.core.resources.planner_feeder_warn_length
import org.meshtastic.core.resources.planner_feeder_warn_other
import org.meshtastic.core.resources.planner_feeder_warn_sections
import org.meshtastic.core.resources.planner_feeder_warn_too_many
import org.meshtastic.core.resources.planner_feeder_warn_unknown
import org.meshtastic.core.resources.planner_unit_db
import org.meshtastic.core.resources.planner_unit_db_per_m
import org.meshtastic.core.resources.planner_unit_m
import org.meshtastic.core.ui.icon.Add
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Remove
import org.meshtastic.feature.map.planner.CableDb
import org.meshtastic.feature.map.planner.FeederBuilder
import org.meshtastic.feature.map.planner.state.PlannerEnd
import org.meshtastic.feature.map.planner.state.PlannerSide
import org.meshtastic.feature.map.planner.state.PlannerViewModel
import org.meshtastic.feature.map.planner.FeederSection as FeederSectionData

private const val MAX_FEEDER_DB = 100.0
private const val MAX_SECTION_LENGTH_M = 1000.0
private const val MAX_CUSTOM_DB_PER_M = 10.0
private const val DEFAULT_CUSTOM_DB_PER_M = 0.5
private const val DEFAULT_CABLE_ID = "rg316"
private const val DEFAULT_SECTION_LENGTH_M = 0.2

/** Feeder loss of one side: a single manual value, or the connector / cable builder ("Dokladny wybor kabla"). */
@Composable
internal fun FeederSection(side: PlannerSide, end: PlannerEnd, vm: PlannerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlannerSubheading(text = stringResource(Res.string.planner_feeder_title), info = PlannerInfoTopic.FEEDER)
        if (!end.feederPrecise) {
            PlannerNumberField(
                label = stringResource(Res.string.planner_feeder_manual),
                value = end.feederManualDb,
                onValue = { vm.setFeederManualDb(side, it) },
                decimals = 2,
                suffix = stringResource(Res.string.planner_unit_db),
                min = 0.0,
                max = MAX_FEEDER_DB,
            )
        }
        Row(
            modifier =
            Modifier.fillMaxWidth().clickable { vm.setFeederPrecise(side, !end.feederPrecise) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = end.feederPrecise, onCheckedChange = null)
            Text(
                text = stringResource(Res.string.planner_feeder_precise),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            PlannerInfoButton(PlannerInfoTopic.FEEDER_PRECISE)
        }
        if (end.feederPrecise) FeederBuilderUi(side = side, end = end, vm = vm)
    }
}

@Composable
private fun presetName(id: String): String = stringResource(
    when (id) {
        "direct" -> Res.string.planner_feeder_preset_direct
        "adapter_sma_n" -> Res.string.planner_feeder_preset_adapter_sma_n
        "adapter_n_n" -> Res.string.planner_feeder_preset_adapter_n_n
        "pigtail" -> Res.string.planner_feeder_preset_pigtail
        "pigtail_cable" -> Res.string.planner_feeder_preset_pigtail_cable
        "long_lmr400" -> Res.string.planner_feeder_preset_long_lmr400
        "ecoflex_roof" -> Res.string.planner_feeder_preset_ecoflex_roof
        "heliax_mast" -> Res.string.planner_feeder_preset_heliax_mast
        else -> Res.string.planner_feeder_preset_custom
    },
)

@Composable
private fun FeederBuilderUi(side: PlannerSide, end: PlannerEnd, vm: PlannerViewModel) {
    val cfg = end.feederConfig
    val count = cfg.connectorIds.size
    val customLabel = stringResource(Res.string.planner_feeder_custom_cable)
    val approxMark = " ~"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlannerDropdown(
            label = stringResource(Res.string.planner_feeder_preset),
            selectedLabel = end.feederPresetId?.let { presetName(it) } ?: stringResource(Res.string.planner_feeder_preset_custom),
            options = FeederBuilder.presets.map { it.id to presetName(it.id) },
            onSelect = { vm.applyFeederPreset(side, it) },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.planner_feeder_connector_count),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { vm.resizeFeederConnectors(side, count - 1) },
                enabled = count > 0,
            ) {
                Icon(MeshtasticIcons.Remove, contentDescription = stringResource(Res.string.planner_feeder_fewer))
            }
            Text(text = count.toString(), style = MaterialTheme.typography.titleMedium)
            IconButton(
                onClick = { vm.resizeFeederConnectors(side, count + 1) },
                enabled = count < FeederBuilder.MAX_CONNECTORS,
            ) {
                Icon(MeshtasticIcons.Add, contentDescription = stringResource(Res.string.planner_feeder_more))
            }
        }
        for (i in 0 until count) {
            val connectorId = cfg.connectorIds[i]
            PlannerDropdown(
                label = stringResource(Res.string.planner_feeder_connector_n, (i + 1).toString()),
                selectedLabel = CableDb.connector(connectorId)?.let { connectorLabel(it.name, it.approximate, approxMark) } ?: connectorId,
                options = CableDb.connectors.map { it.id to connectorLabel(it.name, it.approximate, approxMark) },
                onSelect = { id ->
                    val ids = cfg.connectorIds.toMutableList()
                    ids[i] = id
                    vm.setFeederConfig(side, cfg.copy(connectorIds = ids))
                },
            )
            if (i < count - 1) {
                SectionEditor(
                    index = i,
                    section = cfg.sections.getOrNull(i) ?: FeederSectionData(DEFAULT_CABLE_ID, DEFAULT_SECTION_LENGTH_M),
                    customLabel = customLabel,
                    approxMark = approxMark,
                    onChange = { updated ->
                        val list = cfg.sections.toMutableList()
                        while (list.size <= i) list.add(FeederSectionData(DEFAULT_CABLE_ID, DEFAULT_SECTION_LENGTH_M))
                        list[i] = updated
                        vm.setFeederConfig(side, cfg.copy(sections = list))
                    },
                )
            }
        }
        FeederBreakdown(end = end, customLabel = customLabel)
    }
}

private fun connectorLabel(name: String, approximate: Boolean, mark: String): String =
    if (approximate) name + mark else name

@Composable
private fun SectionEditor(
    index: Int,
    section: FeederSectionData,
    customLabel: String,
    approxMark: String,
    onChange: (FeederSectionData) -> Unit,
) {
    val isCustom = section.cableId == FeederBuilder.CUSTOM_ID
    val isNone = section.cableId == FeederBuilder.NONE_ID
    val noneLabel = stringResource(Res.string.planner_feeder_no_cable)
    val cable = CableDb.cable(section.cableId)
    val cableOptions =
        listOf(FeederBuilder.NONE_ID to noneLabel) +
            CableDb.cables.map { it.id to (if (it.approximate) it.name + approxMark else it.name) } +
            (FeederBuilder.CUSTOM_ID to customLabel)
    Column(
        modifier = Modifier.padding(start = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlannerDropdown(
            label = stringResource(Res.string.planner_feeder_cable_n, (index + 1).toString()),
            selectedLabel =
            when {
                isNone -> noneLabel
                isCustom -> customLabel
                cable != null -> cable.name + if (cable.approximate) approxMark else ""
                else -> section.cableId
            },
            options = cableOptions,
            onSelect = { id ->
                onChange(
                    section.copy(
                        cableId = id,
                        customDbPerM =
                        if (id == FeederBuilder.CUSTOM_ID) section.customDbPerM ?: DEFAULT_CUSTOM_DB_PER_M else section.customDbPerM,
                    ),
                )
            },
        )
        if (!isNone) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            PlannerNumberField(
                label = stringResource(Res.string.planner_feeder_length),
                value = section.lengthM,
                onValue = { onChange(section.copy(lengthM = it)) },
                modifier = Modifier.weight(1f),
                decimals = 2,
                suffix = stringResource(Res.string.planner_unit_m),
                min = 0.0,
                max = MAX_SECTION_LENGTH_M,
            )
            if (isCustom) {
                PlannerNumberField(
                    label = stringResource(Res.string.planner_feeder_custom_loss),
                    value = section.customDbPerM ?: DEFAULT_CUSTOM_DB_PER_M,
                    onValue = { onChange(section.copy(customDbPerM = it)) },
                    modifier = Modifier.weight(1f),
                    decimals = 3,
                    suffix = stringResource(Res.string.planner_unit_db_per_m),
                    min = 0.0,
                    max = MAX_CUSTOM_DB_PER_M,
                )
            }
        }
    }
}

@Composable
private fun FeederBreakdown(end: PlannerEnd, customLabel: String) {
    val result = end.feederResult
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PlannerSubheading(text = stringResource(Res.string.planner_feeder_breakdown))
        result.lines.forEach { line ->
            PlannerValueRow(
                label = localizeFeederLabel(line.label, customLabel),
                value = fmt(line.lossDb, 2) + " " + stringResource(Res.string.planner_unit_db),
            )
        }
        HorizontalDivider()
        PlannerValueRow(
            label = stringResource(Res.string.planner_feeder_total),
            value = fmt(result.totalDb, 2) + " " + stringResource(Res.string.planner_unit_db),
            bold = true,
        )
        if (result.approximate) {
            Text(
                text = stringResource(Res.string.planner_feeder_approximate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        result.warnings.distinct().forEach { code -> PlannerNotice(text = feederWarningText(code)) }
    }
}

/** Engine labels carry display names of the database; only the neutral word "custom" is localized. */
internal fun localizeFeederLabel(label: String, customLabel: String): String =
    if (label.startsWith(FeederBuilder.CUSTOM_ID + " ")) customLabel + label.substring(FeederBuilder.CUSTOM_ID.length) else label

@Composable
private fun feederWarningText(code: String): String = stringResource(
    when {
        code == "SECTION_COUNT_MISMATCH" -> Res.string.planner_feeder_warn_sections
        code == "CONNECTOR_COUNT_EXCEEDS_MAX" -> Res.string.planner_feeder_warn_too_many
        code.startsWith("UNKNOWN_") -> Res.string.planner_feeder_warn_unknown
        code == "CUSTOM_LOSS_MISSING" -> Res.string.planner_feeder_warn_custom_loss
        code == "INVALID_LENGTH" -> Res.string.planner_feeder_warn_length
        code == "FREQUENCY_CLAMPED" -> Res.string.planner_feeder_warn_frequency
        else -> Res.string.planner_feeder_warn_other
    },
)
