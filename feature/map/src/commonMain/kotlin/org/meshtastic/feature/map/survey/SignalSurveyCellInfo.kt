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
package org.meshtastic.feature.map.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.dbm_value
import org.meshtastic.core.resources.mesh_hud_snr
import org.meshtastic.core.resources.rssi
import org.meshtastic.core.resources.signal_survey_all_readings
import org.meshtastic.core.resources.signal_survey_heard_directly
import org.meshtastic.core.resources.signal_survey_last_reading
import org.meshtastic.core.resources.signal_survey_reading_direct
import org.meshtastic.core.resources.signal_survey_reading_relayed
import org.meshtastic.core.resources.signal_survey_readings
import org.meshtastic.core.resources.signal_survey_senders
import org.meshtastic.core.resources.signal_survey_unplaced
import org.meshtastic.core.resources.snr
import org.meshtastic.core.ui.component.Card
import org.meshtastic.core.ui.component.verticalScrollWithBar
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.ExpandLess
import org.meshtastic.core.ui.icon.ExpandMore
import org.meshtastic.core.ui.icon.MeshtasticIcons

internal const val SIGNAL_SURVEY_CELL_INFO_TAG = "signal_survey_cell_info"
private const val CARD_MAX_WIDTH_DP = 480
private const val MAP_CONTROLS_INSET_DP = 88
private const val LINKS_MAX_HEIGHT_DP = 120

/**
 * The tapped square's details, as a card a map mounts at its bottom edge. Not a dialog: the lines to the nodes heard
 * there are drawn on the map behind it, and a scrim would hide them.
 */
@Composable
fun SignalSurveyCellInfo(modifier: Modifier = Modifier, viewModel: SignalSurveyViewModel = koinViewModel()) {
    val selected by viewModel.selectedCell.collectAsStateWithLifecycle()
    val links by viewModel.selectedLinks.collectAsStateWithLifecycle()
    val nodes by koinInject<NodeRepository>().nodeDBbyNum.collectAsStateWithLifecycle()
    val names =
        remember(nodes) { nodes.mapValues { (_, node) -> node.user.short_name.ifBlank { node.user.long_name } } }
    selected?.let {
        SignalSurveyCellCard(it, links, onDismiss = { viewModel.selectCell(null) }, modifier = modifier, names = names)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SignalSurveyCellCard(
    cell: SurveyCellUi,
    links: SurveyLinks,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    names: Map<Int, String> = emptyMap(),
) {
    val reading = cell.cell
    var showReadings by remember(cell.id) { mutableStateOf(false) }
    Card(
        modifier =
        modifier
            // The wide end margin keeps the map's zoom buttons, at the bottom end on both maps, reachable.
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = MAP_CONTROLS_INSET_DP.dp)
            .widthIn(max = CARD_MAX_WIDTH_DP.dp)
            .fillMaxWidth()
            .testTag(SIGNAL_SURVEY_CELL_INFO_TAG),
    ) {
        Column(Modifier.padding(start = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).background(cell.quality.color(), RoundedCornerShape(3.dp)))
                Text(
                    stringResource(cell.quality.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(MeshtasticIcons.Close, contentDescription = stringResource(Res.string.close))
                }
            }
            reading.bestSnr?.let { snr ->
                val value = stringResource(Res.string.mesh_hud_snr, NumberFormatter.format(snr, 1))
                Text("${stringResource(Res.string.snr)} $value")
            }
            Text("${stringResource(Res.string.rssi)} ${stringResource(Res.string.dbm_value, reading.rssi)}")
            Text(pluralStringResource(Res.plurals.signal_survey_readings, reading.samples, reading.samples))
            Text(pluralStringResource(Res.plurals.signal_survey_senders, reading.senders, reading.senders))
            Text(
                stringResource(
                    Res.string.signal_survey_last_reading,
                    DateFormatter.formatDateTimeShort(reading.lastHeardMillis),
                ),
            )
            if (reading.readings.size > 1) {
                ReadingsToggle(count = reading.readings.size, open = showReadings) {
                    showReadings = !showReadings
                    Logger.d { "Badanie sygnału: lista odczytów kwadratu, rozwinięta: $showReadings" }
                }
                if (showReadings) ReadingsList(reading.readings, names)
            }
            if (links.links.isNotEmpty()) {
                Text(
                    stringResource(Res.string.signal_survey_heard_directly),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                // A square heard by many nodes would otherwise push names past the map's edge.
                val scroll = rememberScrollState()
                val bounded =
                    Modifier.padding(end = 16.dp).heightIn(max = LINKS_MAX_HEIGHT_DP.dp).verticalScrollWithBar(scroll)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = bounded) {
                    links.links.forEach { link ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(link.quality.color(), CircleShape))
                            Text(link.name, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
            if (links.unplaced > 0) {
                Text(
                    pluralStringResource(Res.plurals.signal_survey_unplaced, links.unplaced, links.unplaced),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** The row that opens and closes the list of every reading in the square (the square itself shows the clearest). */
@Composable
private fun ReadingsToggle(count: Int, open: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(top = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.signal_survey_all_readings, count),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (open) MeshtasticIcons.ExpandLess else MeshtasticIcons.ExpandMore,
            contentDescription = null,
        )
    }
}

/** Every reading in the square, the clearest first: who was heard, RSSI, SNR, the time and how it arrived. */
@Composable
private fun ReadingsList(readings: List<SurveyReading>, names: Map<Int, String>) {
    val scroll = rememberScrollState()
    Column(
        modifier =
            Modifier
                .padding(end = 16.dp)
                .heightIn(max = READINGS_MAX_HEIGHT_DP.dp)
                .verticalScrollWithBar(scroll),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        readings.forEachIndexed { index, item ->
            val name = names[item.fromNum] ?: NodeAddress.numToDefaultId(item.fromNum)
            val snrLabel = stringResource(Res.string.snr)
            val snr = item.snr?.let { " · $snrLabel ${NumberFormatter.format(it, 1)}" }.orEmpty()
            val howRes =
                if (item.direct) Res.string.signal_survey_reading_direct else Res.string.signal_survey_reading_relayed
            val how = stringResource(howRes)
            Text(
                text =
                "${if (index == 0) "★ " else ""}$name · ${item.rssi} dBm$snr · " +
                    "${DateFormatter.formatTime(item.timeMillis)} · $how",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private const val READINGS_MAX_HEIGHT_DP = 160
