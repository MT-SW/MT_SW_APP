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
@file:Suppress("MagicNumber", "TooManyFunctions", "LongMethod", "CyclomaticComplexMethod")

package org.meshtastic.feature.settings.antenna

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.antenna.AntennaDesign
import org.meshtastic.core.model.antenna.AntennaInput
import org.meshtastic.core.model.antenna.AntennaMath
import org.meshtastic.core.model.antenna.AntennaPrefill
import org.meshtastic.core.model.antenna.AntennaType
import org.meshtastic.core.model.antenna.Cable
import org.meshtastic.core.model.antenna.Connector
import org.meshtastic.core.model.antenna.Dim
import org.meshtastic.core.model.antenna.DimKind
import org.meshtastic.core.model.antenna.Insulation
import org.meshtastic.core.model.antenna.Note
import org.meshtastic.core.model.antenna.Polarization
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.antenna_accuracy
import org.meshtastic.core.resources.antenna_boom
import org.meshtastic.core.resources.antenna_cable
import org.meshtastic.core.resources.antenna_cable_custom
import org.meshtastic.core.resources.antenna_cable_for
import org.meshtastic.core.resources.antenna_cable_length
import org.meshtastic.core.resources.antenna_cable_loss_custom
import org.meshtastic.core.resources.antenna_cable_none
import org.meshtastic.core.resources.antenna_calc_title
import org.meshtastic.core.resources.antenna_connectors
import org.meshtastic.core.resources.antenna_decimal
import org.meshtastic.core.resources.antenna_diameter
import org.meshtastic.core.resources.antenna_dim_arm
import org.meshtastic.core.resources.antenna_dim_boom
import org.meshtastic.core.resources.antenna_dim_depth
import org.meshtastic.core.resources.antenna_dim_diameter
import org.meshtastic.core.resources.antenna_dim_director
import org.meshtastic.core.resources.antenna_dim_driven
import org.meshtastic.core.resources.antenna_dim_feed
import org.meshtastic.core.resources.antenna_dim_feed_width
import org.meshtastic.core.resources.antenna_dim_gap
import org.meshtastic.core.resources.antenna_dim_ground
import org.meshtastic.core.resources.antenna_dim_half
import org.meshtastic.core.resources.antenna_dim_inset
import org.meshtastic.core.resources.antenna_dim_length
import org.meshtastic.core.resources.antenna_dim_patch_l
import org.meshtastic.core.resources.antenna_dim_patch_w
import org.meshtastic.core.resources.antenna_dim_pitch
import org.meshtastic.core.resources.antenna_dim_position
import org.meshtastic.core.resources.antenna_dim_quarter
import org.meshtastic.core.resources.antenna_dim_radial
import org.meshtastic.core.resources.antenna_dim_radiator
import org.meshtastic.core.resources.antenna_dim_reflector
import org.meshtastic.core.resources.antenna_dim_reflector_plate
import org.meshtastic.core.resources.antenna_dim_section
import org.meshtastic.core.resources.antenna_dim_side
import org.meshtastic.core.resources.antenna_dim_spacing
import org.meshtastic.core.resources.antenna_dim_stub
import org.meshtastic.core.resources.antenna_dim_tail_driver
import org.meshtastic.core.resources.antenna_dim_tail_reflector
import org.meshtastic.core.resources.antenna_dim_total
import org.meshtastic.core.resources.antenna_dim_turn_wire
import org.meshtastic.core.resources.antenna_dim_wavelength
import org.meshtastic.core.resources.antenna_dim_whip
import org.meshtastic.core.resources.antenna_dim_width
import org.meshtastic.core.resources.antenna_dimensions
import org.meshtastic.core.resources.antenna_drawing_note
import org.meshtastic.core.resources.antenna_elements
import org.meshtastic.core.resources.antenna_eps
import org.meshtastic.core.resources.antenna_feed_title
import org.meshtastic.core.resources.antenna_freq_ours
import org.meshtastic.core.resources.antenna_frequency
import org.meshtastic.core.resources.antenna_gain
import org.meshtastic.core.resources.antenna_gain_override
import org.meshtastic.core.resources.antenna_impedance
import org.meshtastic.core.resources.antenna_insulation
import org.meshtastic.core.resources.antenna_insulation_none
import org.meshtastic.core.resources.antenna_insulation_thick
import org.meshtastic.core.resources.antenna_insulation_thin
import org.meshtastic.core.resources.antenna_metal_boom
import org.meshtastic.core.resources.antenna_note_balun
import org.meshtastic.core.resources.antenna_note_choke
import org.meshtastic.core.resources.antenna_note_coax_vf
import org.meshtastic.core.resources.antenna_note_coil
import org.meshtastic.core.resources.antenna_note_feed_tap
import org.meshtastic.core.resources.antenna_note_gain
import org.meshtastic.core.resources.antenna_note_half_balun
import org.meshtastic.core.resources.antenna_note_helix_gain
import org.meshtastic.core.resources.antenna_note_helix_match
import org.meshtastic.core.resources.antenna_note_moxon
import org.meshtastic.core.resources.antenna_note_patch
import org.meshtastic.core.resources.antenna_note_radials
import org.meshtastic.core.resources.antenna_note_tune_swr
import org.meshtastic.core.resources.antenna_note_yagi_boom
import org.meshtastic.core.resources.antenna_note_yagi_driven
import org.meshtastic.core.resources.antenna_pol_circular
import org.meshtastic.core.resources.antenna_pol_either
import org.meshtastic.core.resources.antenna_pol_linear
import org.meshtastic.core.resources.antenna_pol_vertical
import org.meshtastic.core.resources.antenna_properties
import org.meshtastic.core.resources.antenna_radial_angle
import org.meshtastic.core.resources.antenna_res_at_antenna
import org.meshtastic.core.resources.antenna_res_band_none
import org.meshtastic.core.resources.antenna_res_band_ok
import org.meshtastic.core.resources.antenna_res_band_over
import org.meshtastic.core.resources.antenna_res_cable
import org.meshtastic.core.resources.antenna_res_connectors
import org.meshtastic.core.resources.antenna_res_duty
import org.meshtastic.core.resources.antenna_res_eirp
import org.meshtastic.core.resources.antenna_res_mismatch
import org.meshtastic.core.resources.antenna_res_range
import org.meshtastic.core.resources.antenna_res_total
import org.meshtastic.core.resources.antenna_sections
import org.meshtastic.core.resources.antenna_shortening
import org.meshtastic.core.resources.antenna_spacing_mm
import org.meshtastic.core.resources.antenna_substrate
import org.meshtastic.core.resources.antenna_swr
import org.meshtastic.core.resources.antenna_turns
import org.meshtastic.core.resources.antenna_tx_ours
import org.meshtastic.core.resources.antenna_tx_power
import org.meshtastic.core.resources.antenna_tx_radio
import org.meshtastic.core.resources.antenna_tx_radio_region
import org.koin.compose.koinInject
import org.meshtastic.core.repository.RadioConfigRepository
import androidx.compose.material3.FilterChip
import org.meshtastic.core.resources.antenna_type
import org.meshtastic.core.resources.antenna_type_biquad
import org.meshtastic.core.resources.antenna_type_coax_stub
import org.meshtastic.core.resources.antenna_type_collinear
import org.meshtastic.core.resources.antenna_type_dipole
import org.meshtastic.core.resources.antenna_type_five_eighths
import org.meshtastic.core.resources.antenna_type_folded_dipole
import org.meshtastic.core.resources.antenna_type_ground_plane
import org.meshtastic.core.resources.antenna_type_helix
import org.meshtastic.core.resources.antenna_type_j_pole
import org.meshtastic.core.resources.antenna_type_moxon
import org.meshtastic.core.resources.antenna_type_patch
import org.meshtastic.core.resources.antenna_type_slim_jim
import org.meshtastic.core.resources.antenna_type_yagi
import org.meshtastic.core.resources.antenna_wavelength
import org.meshtastic.core.resources.antenna_yagi_row
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.feature.settings.component.ExpressiveSection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin

private val FREQ_CHIPS = listOf(AntennaMath.OUR_MHZ, 868.0, 433.0, 915.0, 2440.0)

/** Antenna calculator: sizes of many antenna types with a drawing, and the losses of the feed line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AntennaCalculatorScreen(onBack: () -> Unit) {
    val prefill = remember { AntennaPrefill.consume() }
    val prefillMhz = prefill.first
    val prefillType = prefill.second
    var freqText by remember { mutableStateOf(numberText(prefillMhz ?: AntennaMath.OUR_MHZ)) }
    var type by remember { mutableStateOf(prefillType ?: AntennaType.GROUND_PLANE) }
    var diameterText by remember { mutableStateOf("1.5") }
    var insulation by remember { mutableStateOf(Insulation.NONE) }
    var angleText by remember { mutableStateOf("45") }
    var elementsText by remember { mutableStateOf("5") }
    var boomText by remember { mutableStateOf("20") }
    var metalBoom by remember { mutableStateOf(true) }
    var antennaCable by remember { mutableStateOf(Cable.RG58) }
    var sectionsText by remember { mutableStateOf("4") }
    var turnsText by remember { mutableStateOf("8") }
    var epsText by remember { mutableStateOf("4.4") }
    var substrateText by remember { mutableStateOf("1.6") }
    var foldedText by remember { mutableStateOf("0") }

    val mhz = parse(freqText)?.takeIf { it in 1.0..100_000.0 } ?: AntennaMath.OUR_MHZ
    val input =
        AntennaInput(
            type = type,
            mhz = mhz,
            diameterMm = parse(diameterText)?.takeIf { it > 0 } ?: 1.5,
            insulation = insulation,
            radialAngleDeg = parse(angleText) ?: 45.0,
            elements = parse(elementsText)?.toInt() ?: 5,
            boomMm = parse(boomText) ?: 20.0,
            metalBoom = metalBoom,
            cable = antennaCable,
            sections = parse(sectionsText)?.toInt() ?: 4,
            turns = parse(turnsText)?.toInt() ?: 8,
            epsR = parse(epsText) ?: 4.4,
            substrateMm = parse(substrateText) ?: 1.6,
            foldedSpacingMm = parse(foldedText) ?: 0.0,
        )
    val design = AntennaMath.design(input)
    val sep = stringResource(Res.string.antenna_decimal)
    val lambda = AntennaMath.wavelength(mhz)

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.antenna_calc_title),
                canNavigateUp = true,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = {},
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExpressiveSection(title = stringResource(Res.string.antenna_type)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(stringResource(Res.string.antenna_frequency), freqText) { freqText = it }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FREQ_CHIPS.forEach { f ->
                            FilterChip(
                                selected = abs(mhz - f) < 0.001,
                                onClick = { freqText = numberText(f) },
                                label = {
                                    Text(if (f == AntennaMath.OUR_MHZ) stringResource(Res.string.antenna_freq_ours) else fmt(f, 0, sep) + " MHz")
                                },
                            )
                        }
                    }
                }
                DropDownPreference(
                    title = stringResource(Res.string.antenna_type),
                    enabled = true,
                    items = AntennaType.entries.map { it to stringResource(typeName(it)) },
                    selectedItem = type,
                    onItemSelected = { type = it },
                )
            }

            ExpressiveSection(title = stringResource(typeName(type))) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val wire = type !in setOf(AntennaType.HELIX, AntennaType.PATCH, AntennaType.COAX_STUB)
                    if (wire) NumberField(stringResource(Res.string.antenna_diameter), diameterText) { diameterText = it }
                    when (type) {
                        AntennaType.GROUND_PLANE, AntennaType.FIVE_EIGHTHS ->
                            NumberField(stringResource(Res.string.antenna_radial_angle), angleText) { angleText = it }
                        AntennaType.FOLDED_DIPOLE -> NumberField(stringResource(Res.string.antenna_spacing_mm), foldedText) { foldedText = it }
                        AntennaType.YAGI -> {
                            NumberField(stringResource(Res.string.antenna_elements), elementsText, decimal = false) { elementsText = it }
                            NumberField(stringResource(Res.string.antenna_boom), boomText) { boomText = it }
                        }
                        AntennaType.COLLINEAR -> NumberField(stringResource(Res.string.antenna_sections), sectionsText, decimal = false) { sectionsText = it }
                        AntennaType.HELIX -> NumberField(stringResource(Res.string.antenna_turns), turnsText, decimal = false) { turnsText = it }
                        AntennaType.PATCH -> {
                            NumberField(stringResource(Res.string.antenna_eps), epsText) { epsText = it }
                            NumberField(stringResource(Res.string.antenna_substrate), substrateText) { substrateText = it }
                        }
                        else -> {}
                    }
                }
                if (type == AntennaType.YAGI) {
                    SwitchPreference(
                        title = stringResource(Res.string.antenna_metal_boom),
                        checked = metalBoom,
                        enabled = true,
                        onCheckedChange = { metalBoom = it },
                    )
                }
                if (type !in setOf(AntennaType.HELIX, AntennaType.PATCH, AntennaType.COAX_STUB, AntennaType.BIQUAD)) {
                    DropDownPreference(
                        title = stringResource(Res.string.antenna_insulation),
                        enabled = true,
                        items =
                            listOf(
                                Insulation.NONE to stringResource(Res.string.antenna_insulation_none),
                                Insulation.THIN to stringResource(Res.string.antenna_insulation_thin),
                                Insulation.THICK to stringResource(Res.string.antenna_insulation_thick),
                            ),
                        selectedItem = insulation,
                        onItemSelected = { insulation = it },
                    )
                }
                if (type in setOf(AntennaType.COLLINEAR, AntennaType.COAX_STUB, AntennaType.FOLDED_DIPOLE)) {
                    DropDownPreference(
                        title = stringResource(Res.string.antenna_cable_for),
                        enabled = true,
                        items = Cable.entries.map { it to "${it.label} (VF ${fmt(it.vf, 2, sep)})" },
                        selectedItem = antennaCable,
                        onItemSelected = { antennaCable = it },
                    )
                }
            }

            AntennaDrawing(type, design, input, sep)
            Text(
                text = stringResource(Res.string.antenna_drawing_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ExpressiveSection(title = stringResource(Res.string.antenna_dimensions)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DimensionRows(type, design, sep)
                }
            }

            ExpressiveSection(title = stringResource(Res.string.antenna_properties)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Line(stringResource(Res.string.antenna_wavelength, length(lambda, sep)))
                    if (design.shortening != 1.0) Line(stringResource(Res.string.antenna_shortening, fmt(design.shortening, 3, sep)))
                    design.impedanceOhm?.let { Line(stringResource(Res.string.antenna_impedance, fmt(it, 0, sep))) }
                    if (type != AntennaType.COAX_STUB) {
                        Line(stringResource(Res.string.antenna_gain, fmt(design.gainDbi, 1, sep)))
                        Line(stringResource(polarizationName(design.polarization)))
                    }
                    design.notes.forEach { note ->
                        Text("• " + stringResource(noteText(note)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (type != AntennaType.COAX_STUB) FeedSection(mhz, design, sep, prefill.third)

            Text(
                text = stringResource(Res.string.antenna_accuracy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ------------------------------------------------------------------ feed line

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedSection(mhz: Double, design: AntennaDesign, sep: String, txDbm: Double?) {
    val radioConfig: RadioConfigRepository = koinInject()
    val lora = radioConfig.localConfigFlow.collectAsState(null).value?.lora
    // Radio output power; 0 means "the region's maximum", which is known here only for the EU 868 regions.
    val radioDbm: Double? =
        when {
            lora == null -> null
            lora.tx_power > 0 -> lora.tx_power.toDouble()
            lora.region.name.startsWith("EU_") -> 27.0
            else -> null
        }
    var powerText by remember { mutableStateOf(numberText(txDbm ?: radioDbm ?: 22.0)) }
    var powerTouched by remember { mutableStateOf(txDbm != null) }
    LaunchedEffect(radioDbm) {
        if (!powerTouched && radioDbm != null) powerText = numberText(radioDbm)
    }
    var cableChoice by remember { mutableStateOf<Int>(Cable.H155.ordinal) }
    var lengthText by remember { mutableStateOf("5") }
    var customLossText by remember { mutableStateOf("20") }
    var swrText by remember { mutableStateOf("1.5") }
    var gainText by remember { mutableStateOf("") }
    val counts = remember { mutableStateMapOf(Connector.SMA to 2) }

    val none = -1
    val custom = -2
    val cable = Cable.entries.getOrNull(cableChoice)
    val gain = parse(gainText) ?: design.gainDbi
    val tx = parse(powerText) ?: 22.0
    val lengthM = if (cableChoice == none) 0.0 else parse(lengthText)?.coerceAtLeast(0.0) ?: 0.0
    val res =
        AntennaMath.feed(
            mhz = mhz,
            txDbm = tx,
            cable = cable,
            cableM = lengthM,
            connectors = counts.toMap(),
            swr = parse(swrText)?.coerceAtLeast(1.0) ?: 1.0,
            gainDbi = gain,
            customLossPer100m = if (cableChoice == custom) parse(customLossText) else null,
        )
    val per100 = if (cableChoice == custom) parse(customLossText) ?: 0.0 else cable?.lossPer100m(mhz) ?: 0.0

    ExpressiveSection(title = stringResource(Res.string.antenna_feed_title)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(stringResource(Res.string.antenna_tx_power), powerText, signed = true) {
                powerText = it
                powerTouched = true
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (radioDbm != null) {
                    val label =
                        if (lora != null && lora.tx_power > 0) {
                            stringResource(Res.string.antenna_tx_radio, numberText(radioDbm))
                        } else {
                            stringResource(Res.string.antenna_tx_radio_region, numberText(radioDbm))
                        }
                    FilterChip(
                        selected = parse(powerText) == radioDbm,
                        onClick = {
                            powerText = numberText(radioDbm)
                            powerTouched = true
                        },
                        label = { Text(label) },
                    )
                }
                FilterChip(
                    selected = parse(powerText) == 27.0,
                    onClick = {
                        powerText = numberText(27.0)
                        powerTouched = true
                    },
                    label = { Text(stringResource(Res.string.antenna_tx_ours, numberText(27.0))) },
                )
            }
            Text(power(tx, sep), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropDownPreference(
            title = stringResource(Res.string.antenna_cable),
            enabled = true,
            items =
                listOf(none to stringResource(Res.string.antenna_cable_none)) +
                    Cable.entries.map { it.ordinal to "${it.label} — ${fmt(it.lossPer100m(mhz), 1, sep)} dB/100 m" } +
                    listOf(custom to stringResource(Res.string.antenna_cable_custom)),
            selectedItem = cableChoice,
            onItemSelected = { cableChoice = it },
        )
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cableChoice != none) NumberField(stringResource(Res.string.antenna_cable_length), lengthText) { lengthText = it }
            if (cableChoice == custom) NumberField(stringResource(Res.string.antenna_cable_loss_custom), customLossText) { customLossText = it }
            Text(stringResource(Res.string.antenna_connectors), style = MaterialTheme.typography.titleSmall)
            Connector.entries.forEach { c ->
                val n = counts[c] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("${c.label} (${fmt(c.lossDb, 2, sep)} dB)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { counts[c] = max(0, n - 1) }) { Text("−") }
                    Text(n.toString(), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { counts[c] = min(20, n + 1) }) { Text("+") }
                }
            }
            NumberField(stringResource(Res.string.antenna_swr), swrText) { swrText = it }
            NumberField(stringResource(Res.string.antenna_gain_override), gainText, signed = true) { gainText = it }

            if (cableChoice != none) Line(stringResource(Res.string.antenna_res_cable, fmt(per100, 1, sep), fmt(res.cableDb, 2, sep)))
            Line(stringResource(Res.string.antenna_res_connectors, fmt(res.connectorsDb, 2, sep)))
            Line(stringResource(Res.string.antenna_res_mismatch, fmt(res.mismatchDb, 2, sep)))
            Line(stringResource(Res.string.antenna_res_total, fmt(res.totalDb, 2, sep), fmt((1 - 10.0.pow(-res.totalDb / 10)) * 100, 0, sep)))
            Line(stringResource(Res.string.antenna_res_at_antenna, fmt(res.atAntennaDbm, 1, sep), milli(res.atAntennaDbm, sep)))
            Line(stringResource(Res.string.antenna_res_eirp, fmt(res.eirpDbm, 1, sep), milli(res.eirpDbm, sep), fmt(res.erpDbm, 1, sep), milli(res.erpDbm, sep)))
            val band = res.band
            if (band == null) {
                Line(stringResource(Res.string.antenna_res_band_none))
            } else {
                val limit = band.erpDbm?.let { "${fmt(it, 0, sep)} dBm ERP (${milli(it, sep)})" } ?: band.eirpDbm?.let { "${fmt(it, 0, sep)} dBm EIRP (${milli(it, sep)})" }.orEmpty()
                Line(stringResource(if (res.overLimit == true) Res.string.antenna_res_band_over else Res.string.antenna_res_band_ok, band.name, limit))
                band.dutyPercent?.let { Line(stringResource(Res.string.antenna_res_duty, fmt(it, 1, sep))) }
            }
            Line(stringResource(Res.string.antenna_res_range, fmt(res.rangeFactor * 100, 0, sep)))
        }
    }
}

// ------------------------------------------------------------------ table

@Composable
private fun DimensionRows(type: AntennaType, design: AntennaDesign, sep: String) {
    if (type == AntennaType.YAGI) {
        val positions = design.dims.filter { it.kind == DimKind.POSITION }.associateBy { it.index }
        design.dims.filter { it.kind != DimKind.POSITION }.forEach { d ->
            val pos =
                when (d.kind) {
                    DimKind.REFLECTOR -> 0.0
                    DimKind.DRIVEN -> positions[1]?.meters
                    DimKind.DIRECTOR -> positions[d.index + 1]?.meters
                    else -> null
                }
            val what = "${d.letter} — ${dimName(d)}"
            if (pos != null) Line(stringResource(Res.string.antenna_yagi_row, what, length(d.meters, sep), length(pos, sep)))
            else Line("$what: ${length(d.meters, sep)}")
        }
        return
    }
    design.dims.forEach { d -> Line("${d.letter} — ${dimName(d)}: ${length(d.meters, sep)}") }
}

@Composable
private fun dimName(d: Dim): String = when (d.kind) {
    DimKind.DIRECTOR -> stringResource(Res.string.antenna_dim_director, d.index.toString())
    else -> stringResource(dimRes(d.kind))
}

private fun dimRes(k: DimKind): StringResource = when (k) {
    DimKind.TOTAL -> Res.string.antenna_dim_total
    DimKind.RADIATOR -> Res.string.antenna_dim_radiator
    DimKind.RADIAL -> Res.string.antenna_dim_radial
    DimKind.ARM -> Res.string.antenna_dim_arm
    DimKind.GAP -> Res.string.antenna_dim_gap
    DimKind.SPACING -> Res.string.antenna_dim_spacing
    DimKind.STUB -> Res.string.antenna_dim_stub
    DimKind.FEED -> Res.string.antenna_dim_feed
    DimKind.SECTION -> Res.string.antenna_dim_section
    DimKind.WHIP -> Res.string.antenna_dim_whip
    DimKind.REFLECTOR -> Res.string.antenna_dim_reflector
    DimKind.DRIVEN -> Res.string.antenna_dim_driven
    DimKind.DIRECTOR -> Res.string.antenna_dim_director
    DimKind.BOOM -> Res.string.antenna_dim_boom
    DimKind.POSITION -> Res.string.antenna_dim_position
    DimKind.WIDTH -> Res.string.antenna_dim_width
    DimKind.DEPTH -> Res.string.antenna_dim_depth
    DimKind.TAIL_DRIVER -> Res.string.antenna_dim_tail_driver
    DimKind.TAIL_REFLECTOR -> Res.string.antenna_dim_tail_reflector
    DimKind.SIDE -> Res.string.antenna_dim_side
    DimKind.REFLECTOR_PLATE -> Res.string.antenna_dim_reflector_plate
    DimKind.DIAMETER -> Res.string.antenna_dim_diameter
    DimKind.PITCH -> Res.string.antenna_dim_pitch
    DimKind.TURN_WIRE -> Res.string.antenna_dim_turn_wire
    DimKind.LENGTH -> Res.string.antenna_dim_length
    DimKind.GROUND -> Res.string.antenna_dim_ground
    DimKind.PATCH_W -> Res.string.antenna_dim_patch_w
    DimKind.PATCH_L -> Res.string.antenna_dim_patch_l
    DimKind.INSET -> Res.string.antenna_dim_inset
    DimKind.FEED_WIDTH -> Res.string.antenna_dim_feed_width
    DimKind.QUARTER -> Res.string.antenna_dim_quarter
    DimKind.HALF -> Res.string.antenna_dim_half
    DimKind.WAVELENGTH -> Res.string.antenna_dim_wavelength
}

private fun typeName(t: AntennaType): StringResource = when (t) {
    AntennaType.GROUND_PLANE -> Res.string.antenna_type_ground_plane
    AntennaType.DIPOLE -> Res.string.antenna_type_dipole
    AntennaType.FOLDED_DIPOLE -> Res.string.antenna_type_folded_dipole
    AntennaType.FIVE_EIGHTHS -> Res.string.antenna_type_five_eighths
    AntennaType.J_POLE -> Res.string.antenna_type_j_pole
    AntennaType.SLIM_JIM -> Res.string.antenna_type_slim_jim
    AntennaType.COLLINEAR -> Res.string.antenna_type_collinear
    AntennaType.YAGI -> Res.string.antenna_type_yagi
    AntennaType.MOXON -> Res.string.antenna_type_moxon
    AntennaType.BIQUAD -> Res.string.antenna_type_biquad
    AntennaType.HELIX -> Res.string.antenna_type_helix
    AntennaType.PATCH -> Res.string.antenna_type_patch
    AntennaType.COAX_STUB -> Res.string.antenna_type_coax_stub
}

private fun polarizationName(p: Polarization): StringResource = when (p) {
    Polarization.VERTICAL -> Res.string.antenna_pol_vertical
    Polarization.HORIZONTAL_OR_VERTICAL -> Res.string.antenna_pol_either
    Polarization.CIRCULAR -> Res.string.antenna_pol_circular
    Polarization.LINEAR -> Res.string.antenna_pol_linear
}

private fun noteText(n: Note): StringResource = when (n) {
    Note.TUNE_SWR -> Res.string.antenna_note_tune_swr
    Note.RADIALS_ANGLE -> Res.string.antenna_note_radials
    Note.BALUN_4_1 -> Res.string.antenna_note_balun
    Note.HALF_WAVE_BALUN -> Res.string.antenna_note_half_balun
    Note.COIL_MATCH -> Res.string.antenna_note_coil
    Note.CHOKE -> Res.string.antenna_note_choke
    Note.FEED_TAP -> Res.string.antenna_note_feed_tap
    Note.YAGI_DRIVEN -> Res.string.antenna_note_yagi_driven
    Note.YAGI_BOOM -> Res.string.antenna_note_yagi_boom
    Note.MOXON_RANGE -> Res.string.antenna_note_moxon
    Note.HELIX_MATCH -> Res.string.antenna_note_helix_match
    Note.HELIX_GAIN -> Res.string.antenna_note_helix_gain
    Note.PATCH_FR4 -> Res.string.antenna_note_patch
    Note.GAIN_APPROX -> Res.string.antenna_note_gain
    Note.COAX_VF -> Res.string.antenna_note_coax_vf
}

// ------------------------------------------------------------------ small parts

@Composable
private fun Line(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium)

@Composable
private fun NumberField(label: String, value: String, decimal: Boolean = true, signed: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' || (signed && it == '-') }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = when {
                    signed -> KeyboardType.Text
                    decimal -> KeyboardType.Decimal
                    else -> KeyboardType.Number
                },
            ),
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    )
}

private fun parse(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

private fun numberText(v: Double): String = fmt(v, 5, ".")

/** [v] with up to [decimals] digits after the separator [sep], trailing zeros removed. */
private fun fmt(v: Double, decimals: Int, sep: String): String {
    val p = 10.0.pow(decimals)
    val scaled = (abs(v) * p).roundToLong()
    val whole = scaled / p.toLong()
    val frac = (scaled % p.toLong()).toString().padStart(decimals, '0').trimEnd('0')
    val sign = if (v < 0 && scaled != 0L) "-" else ""
    return sign + whole + if (frac.isEmpty() || decimals == 0) "" else sep + frac
}

/** A length in the handiest unit: metres from 1 m, millimetres under 2 cm, otherwise centimetres. */
private fun length(m: Double, sep: String): String = when {
    m >= 1.0 -> fmt(m, 3, sep) + " m"
    m < 0.02 -> fmt(m * 1000, 1, sep) + " mm"
    else -> fmt(m * 100, 1, sep) + " cm"
}

private fun milli(dbm: Double, sep: String): String {
    val mw = AntennaMath.mw(dbm)
    return if (mw >= 1000) fmt(mw / 1000, 2, sep) + " W" else fmt(mw, if (mw < 10) 2 else 0, sep) + " mW"
}

private fun power(dbm: Double, sep: String) = "${fmt(dbm, 1, sep)} dBm = ${milli(dbm, sep)}"

// ------------------------------------------------------------------ drawing

@Composable
private fun AntennaDrawing(type: AntennaType, design: AntennaDesign, input: AntennaInput, sep: String) {
    val measurer = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onSurface
    val dimInk = MaterialTheme.colorScheme.onSurfaceVariant
    val style = MaterialTheme.typography.labelMedium.copy(color = ink, fontSize = 12.sp)
    val labels = design.dims.associate { it.letter to "${it.letter} = ${length(it.meters, sep)}" }
    val directorLabels = design.dims.filter { it.kind == DimKind.DIRECTOR }.map { it.letter }
    Canvas(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        val p = Pen(this, measurer, style, ink, dimInk)
        when (type) {
            AntennaType.GROUND_PLANE -> p.vertical(labels, radiatorShare = 0.45f, angle = input.radialAngleDeg, coil = false)
            AntennaType.FIVE_EIGHTHS -> p.vertical(labels, radiatorShare = 0.6f, angle = input.radialAngleDeg, coil = true)
            AntennaType.DIPOLE -> p.dipole(labels)
            AntennaType.FOLDED_DIPOLE -> p.folded(labels)
            AntennaType.J_POLE -> p.jPole(labels, slim = false)
            AntennaType.SLIM_JIM -> p.jPole(labels, slim = true)
            AntennaType.COLLINEAR -> p.collinear(labels, input.sections.coerceIn(2, 8))
            AntennaType.YAGI -> p.yagi(design, directorLabels, labels)
            AntennaType.MOXON -> p.moxon(labels)
            AntennaType.BIQUAD -> p.biquad(labels)
            AntennaType.HELIX -> p.helix(labels, input.turns.coerceIn(3, 12))
            AntennaType.PATCH -> p.patch(labels)
            AntennaType.COAX_STUB -> p.stub(labels)
        }
    }
}

/** Drawing helpers: the antenna in [ink], dimension lines and their labels in [dimInk]. */
private class Pen(val s: DrawScope, val tm: TextMeasurer, val style: TextStyle, val ink: Color, val dimInk: Color) {
    val w = s.size.width
    val h = s.size.height
    val thick = 4f
    val thin = 1.5f

    fun line(a: Offset, b: Offset, color: Color = ink, width: Float = thick) = s.drawLine(color, a, b, strokeWidth = width)

    fun dot(c: Offset) = s.drawCircle(ink, radius = 6f, center = c)

    fun text(t: String?, at: Offset, center: Boolean = true) {
        if (t.isNullOrEmpty()) return
        val r = tm.measure(t, style)
        val x = if (center) at.x - r.size.width / 2f else at.x
        val y = at.y - r.size.height / 2f
        s.drawText(r, topLeft = Offset(x.coerceIn(0f, max(0f, w - r.size.width)), y.coerceIn(0f, max(0f, h - r.size.height))))
    }

    /** A dimension line from [a] to [b], moved sideways by [off] pixels, with end ticks and [label] in the middle. */
    fun dim(a: Offset, b: Offset, label: String?, off: Float, labelOff: Float = 0f) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val nx = -dy / len
        val ny = dx / len
        val a2 = Offset(a.x + nx * off, a.y + ny * off)
        val b2 = Offset(b.x + nx * off, b.y + ny * off)
        line(a2, b2, dimInk, thin)
        line(Offset(a2.x - nx * 6, a2.y - ny * 6), Offset(a2.x + nx * 6, a2.y + ny * 6), dimInk, thin)
        line(Offset(b2.x - nx * 6, b2.y - ny * 6), Offset(b2.x + nx * 6, b2.y + ny * 6), dimInk, thin)
        val lo = if (labelOff != 0f) labelOff else 14f * kotlin.math.sign(off)
        text(label, Offset((a2.x + b2.x) / 2 + nx * lo, (a2.y + b2.y) / 2 + ny * lo))
    }

    fun coax(from: Offset, to: Offset) {
        line(from, to, dimInk, 6f)
        line(from, to, ink, 2f)
    }

    fun vertical(l: Map<String, String>, radiatorShare: Float, angle: Double, coil: Boolean) {
        val cx = w / 2
        val top = h * 0.06f
        val base = h * (0.06f + radiatorShare + 0.1f)
        val radialLen = h * 0.3f
        line(Offset(cx, top), Offset(cx, base - if (coil) 24f else 0f))
        if (coil) zigzag(Offset(cx, base - 24f), Offset(cx, base), 4)
        val a = angle * PI / 180
        val dx = (radialLen * cos(a)).toFloat()
        val dy = (radialLen * sin(a)).toFloat()
        line(Offset(cx, base), Offset(cx - dx, base + dy))
        line(Offset(cx, base), Offset(cx + dx, base + dy))
        dot(Offset(cx, base))
        coax(Offset(cx, base), Offset(cx, h - 4f))
        dim(Offset(cx, top), Offset(cx, base), l["A"], off = -60f)
        dim(Offset(cx, base), Offset(cx + dx, base + dy), l["B"], off = 30f)
    }

    fun zigzag(a: Offset, b: Offset, n: Int) {
        val path = Path()
        path.moveTo(a.x, a.y)
        for (i in 1..n * 2) {
            val t = i / (n * 2f)
            val x = a.x + (b.x - a.x) * t + if (i % 2 == 0) -8f else 8f
            path.lineTo(x, a.y + (b.y - a.y) * t)
        }
        path.lineTo(b.x, b.y)
        s.drawPath(path, ink, style = Stroke(width = 3f))
    }

    fun dipole(l: Map<String, String>) {
        val y = h * 0.4f
        val left = w * 0.08f
        val right = w * 0.92f
        val gap = 14f
        val cx = w / 2
        line(Offset(left, y), Offset(cx - gap, y))
        line(Offset(cx + gap, y), Offset(right, y))
        dot(Offset(cx - gap, y))
        dot(Offset(cx + gap, y))
        coax(Offset(cx, y + 4f), Offset(cx, h - 4f))
        dim(Offset(left, y), Offset(right, y), l["A"], off = -40f)
        dim(Offset(left, y), Offset(cx - gap, y), l["B"], off = 40f)
        text(l["C"], Offset(cx + 70f, y + 80f), center = false)
    }

    fun folded(l: Map<String, String>) {
        val y = h * 0.32f
        val sp = 40f
        val left = w * 0.1f + sp / 2
        val right = w * 0.9f - sp / 2
        val cx = w / 2
        line(Offset(left, y), Offset(right, y))
        line(Offset(left, y + sp), Offset(cx - 10f, y + sp))
        line(Offset(cx + 10f, y + sp), Offset(right, y + sp))
        s.drawArc(ink, 90f, 180f, false, topLeft = Offset(left - sp / 2, y), size = Size(sp, sp), style = Stroke(width = thick))
        s.drawArc(ink, -90f, 180f, false, topLeft = Offset(right - sp / 2, y), size = Size(sp, sp), style = Stroke(width = thick))
        dot(Offset(cx - 10f, y + sp))
        dot(Offset(cx + 10f, y + sp))
        dim(Offset(left - sp / 2, y), Offset(right + sp / 2, y), l["A"], off = -36f)
        dim(Offset(right + sp / 2, y), Offset(right + sp / 2, y + sp), l["B"], off = -30f)
        // λ/2 coax loop (balun) under the feed point.
        val loopY = y + sp + 90f
        val path = Path()
        path.moveTo(cx - 10f, y + sp)
        path.cubicTo(cx - 120f, loopY, cx + 120f, loopY, cx + 10f, y + sp)
        s.drawPath(path, dimInk, style = Stroke(width = 5f))
        text(l["D"], Offset(cx, loopY + 6f))
        coax(Offset(cx - 10f, y + sp), Offset(cx - 10f, h - 4f))
        text(l["C"], Offset(w * 0.1f, h - 20f), center = false)
    }

    fun jPole(l: Map<String, String>, slim: Boolean) {
        val bottom = h * 0.95f
        val top = h * 0.05f
        val total = bottom - top
        val xLong = w * 0.45f
        val xShort = w * 0.55f
        val stubTop = bottom - total / 3f
        line(Offset(xLong, top), Offset(xLong, bottom))
        line(Offset(xLong, bottom), Offset(xShort, bottom))
        if (slim) {
            val gapTop = stubTop - 10f
            line(Offset(xShort, bottom), Offset(xShort, gapTop - 2f))
            line(Offset(xShort, gapTop - 14f), Offset(xShort, top))
            line(Offset(xLong, top), Offset(xShort, top))
            text(l["E"], Offset(xShort + 12f, gapTop - 8f), center = false)
            dim(Offset(xLong, top), Offset(xShort, top), l["F"], off = 20f, labelOff = -24f)
        } else {
            line(Offset(xShort, bottom), Offset(xShort, stubTop))
            text(l["E"], Offset(xShort + 12f, bottom - 6f), center = false)
        }
        val feedY = bottom - total / 30f
        dot(Offset(xLong, feedY))
        dot(Offset(xShort, feedY))
        line(Offset(xShort, feedY), Offset(w * 0.8f, feedY), dimInk, 5f)
        dim(Offset(xLong, top), Offset(xLong, bottom), l["A"], off = 70f)
        dim(Offset(xShort, top), Offset(xShort, stubTop), l["B"], off = -90f)
        dim(Offset(xShort, stubTop), Offset(xShort, bottom), l["C"], off = -90f)
        text(l["D"], Offset(w * 0.8f + 8f, feedY - 18f), center = false)
    }

    fun collinear(l: Map<String, String>, n: Int) {
        val cx = w / 2
        val bottom = h * 0.95f
        val top = h * 0.04f
        val whip = (bottom - top) * 0.16f
        val seg = (bottom - top - whip) / n
        for (i in 0 until n) {
            val y0 = bottom - i * seg
            val y1 = y0 - seg + 6f
            line(Offset(cx, y0), Offset(cx, y1), ink, 9f)
            line(Offset(cx, y0), Offset(cx, y1), dimInk, 3f)
            if (i < n - 1) {
                line(Offset(cx - 8f, y1 - 1f), Offset(cx + 8f, y1 - 5f), ink, 2f)
            }
        }
        line(Offset(cx, top + whip), Offset(cx, top))
        dim(Offset(cx, bottom), Offset(cx, bottom - seg + 6f), l["A"], off = 40f)
        dim(Offset(cx, top + whip), Offset(cx, top), l["B"], off = 40f)
        dim(Offset(cx, bottom), Offset(cx, top), l["C"], off = -60f)
    }

    fun yagi(design: AntennaDesign, directors: List<String>, l: Map<String, String>) {
        val elements = design.dims.filter { it.kind == DimKind.REFLECTOR || it.kind == DimKind.DRIVEN || it.kind == DimKind.DIRECTOR }
        val positions = design.dims.filter { it.kind == DimKind.POSITION }.associate { it.index to it.meters }
        val boomLen = design.dims.firstOrNull { it.kind == DimKind.BOOM }?.meters ?: 1.0
        val maxEl = elements.maxOf { it.meters }
        val left = w * 0.08f
        val right = w * 0.92f
        val cy = h * 0.45f
        val half = h * 0.32f
        line(Offset(left, cy), Offset(right, cy), dimInk, 6f)
        elements.forEach { e ->
            val pos = when (e.kind) {
                DimKind.REFLECTOR -> 0.0
                DimKind.DRIVEN -> positions[1] ?: 0.0
                else -> positions[e.index + 1] ?: 0.0
            }
            val x = left + ((pos / boomLen) * (right - left)).toFloat()
            val hh = half * (e.meters / maxEl).toFloat()
            line(Offset(x, cy - hh), Offset(x, cy + hh), ink, if (e.kind == DimKind.DRIVEN) 5f else 3f)
            if (e.kind == DimKind.DRIVEN) dot(Offset(x, cy))
            text(e.letter, Offset(x, cy - hh - 12f))
        }
        dim(Offset(left, cy + half), Offset(right, cy + half), l["L"], off = 24f)
    }

    fun moxon(l: Map<String, String>) {
        val left = w * 0.12f
        val right = w * 0.88f
        val top = h * 0.18f
        val bottom = h * 0.82f
        val depth = bottom - top
        val tailB = depth * 0.33f
        val gap = depth * 0.15f
        // Driver (top) with tails down, reflector (bottom) with tails up.
        line(Offset(left, top), Offset(right, top))
        line(Offset(left, top), Offset(left, top + tailB))
        line(Offset(right, top), Offset(right, top + tailB))
        line(Offset(left, bottom), Offset(right, bottom))
        line(Offset(left, bottom), Offset(left, top + tailB + gap))
        line(Offset(right, bottom), Offset(right, top + tailB + gap))
        dot(Offset(w / 2, top))
        dim(Offset(left, top), Offset(right, top), l["A"], off = -30f)
        dim(Offset(right, top), Offset(right, top + tailB), l["B"], off = -40f)
        dim(Offset(right, top + tailB), Offset(right, top + tailB + gap), l["C"], off = -40f)
        dim(Offset(right, top + tailB + gap), Offset(right, bottom), l["D"], off = -40f)
        dim(Offset(left, top), Offset(left, bottom), l["E"], off = 40f)
    }

    fun biquad(l: Map<String, String>) {
        val cx = w / 2
        val cy = h * 0.5f
        val plate = min(w * 0.8f, h * 0.9f)
        s.drawRect(dimInk.copy(alpha = 0.15f), topLeft = Offset(cx - plate / 2, cy - plate / 2), size = Size(plate, plate))
        val side = plate * 0.3f
        val d = side * 0.7071f
        val leftC = Offset(cx - d, cy)
        val rightC = Offset(cx + d, cy)
        listOf(leftC, rightC).forEach { c ->
            val path = Path()
            path.moveTo(c.x - d, c.y)
            path.lineTo(c.x, c.y - d)
            path.lineTo(c.x + d, c.y)
            path.lineTo(c.x, c.y + d)
            path.close()
            s.drawPath(path, ink, style = Stroke(width = thick))
        }
        dot(Offset(cx, cy))
        dim(Offset(leftC.x - d, cy), Offset(leftC.x, cy - d), l["A"], off = -20f)
        dim(Offset(cx - plate / 2, cy + plate / 2), Offset(cx + plate / 2, cy + plate / 2), l["C"], off = -18f)
        text(l["B"], Offset(cx, cy + d + 30f))
    }

    fun helix(l: Map<String, String>, turns: Int) {
        val plateX = w * 0.1f
        val cy = h * 0.5f
        val radius = h * 0.18f
        val right = w * 0.9f
        line(Offset(plateX, cy - h * 0.4f), Offset(plateX, cy + h * 0.4f), ink, 6f)
        val path = Path()
        val steps = turns * 24
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = plateX + 20f + (right - plateX - 20f) * t
            val y = cy + radius * sin(2 * PI * turns * t).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        s.drawPath(path, ink, style = Stroke(width = 3f))
        val pitch = (right - plateX - 20f) / turns
        dim(Offset(right, cy - radius), Offset(right, cy + radius), l["A"], off = -24f)
        dim(Offset(plateX + 20f, cy + radius), Offset(plateX + 20f + pitch, cy + radius), l["B"], off = -30f)
        dim(Offset(plateX + 20f, cy - radius), Offset(right, cy - radius), l["C"], off = 30f)
        dim(Offset(plateX, cy - h * 0.4f), Offset(plateX, cy + h * 0.4f), l["D"], off = 14f, labelOff = 40f)
    }

    fun patch(l: Map<String, String>) {
        val gw = w * 0.7f
        val gh = h * 0.8f
        val gx = (w - gw) / 2
        val gy = (h - gh) / 2
        s.drawRect(dimInk.copy(alpha = 0.15f), topLeft = Offset(gx, gy), size = Size(gw, gh))
        val pw = gw * 0.62f
        val ph = gh * 0.5f
        val px = (w - pw) / 2
        val py = gy + gh * 0.15f
        s.drawRect(ink, topLeft = Offset(px, py), size = Size(pw, ph), style = Stroke(width = thick))
        val cx = w / 2
        val inset = ph * 0.3f
        val fw = 14f
        line(Offset(cx, py + ph - inset), Offset(cx, gy + gh), ink, fw)
        dim(Offset(px, py), Offset(px + pw, py), l["A"], off = -20f)
        dim(Offset(px, py), Offset(px, py + ph), l["B"], off = 30f)
        text(l["C"], Offset(cx + 16f, py + ph - inset / 2), center = false)
        text(l["D"], Offset(cx + 16f, gy + gh - 20f), center = false)
        dim(Offset(gx, gy + gh), Offset(gx + gw, gy + gh), l["E"], off = -16f)
        dim(Offset(gx + gw, gy), Offset(gx + gw, gy + gh), l["F"], off = -16f, labelOff = -30f)
    }

    fun stub(l: Map<String, String>) {
        val left = w * 0.1f
        val right = w * 0.9f
        val y1 = h * 0.3f
        val y2 = h * 0.7f
        val mid = left + (right - left) / 2
        coax(Offset(left, y1), Offset(mid, y1))
        coax(Offset(left, y2), Offset(right, y2))
        dim(Offset(left, y1), Offset(mid, y1), l["A"], off = -24f)
        dim(Offset(left, y2), Offset(right, y2), l["B"], off = -24f)
    }
}
