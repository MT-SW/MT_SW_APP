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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.coverage_all_nodes
import org.meshtastic.core.resources.coverage_info_body
import org.meshtastic.core.resources.coverage_loading
import org.meshtastic.core.resources.coverage_none
import org.meshtastic.core.resources.coverage_none_filtered
import org.meshtastic.core.resources.coverage_period_all
import org.meshtastic.core.resources.coverage_period_day
import org.meshtastic.core.resources.coverage_period_month
import org.meshtastic.core.resources.coverage_period_week
import org.meshtastic.core.resources.coverage_refresh
import org.meshtastic.core.resources.coverage_summary
import org.meshtastic.core.resources.coverage_summary_filtered
import org.meshtastic.core.resources.coverage_title
import org.meshtastic.core.ui.component.InfoButton
import org.meshtastic.feature.map.component.LayerOpacitySlider
import org.meshtastic.feature.map.coverage.CoverageController
import org.meshtastic.feature.map.maplibre.layers.COVERAGE_DEFAULT_OPACITY
import org.meshtastic.feature.map.maplibre.layers.COVERAGE_LAYER_ID

/**
 * The coverage layer, as a section of the layers sheet: on/off with a plain description behind the "i", transparency,
 * the colour key, a filter by age and by node, how many measurements there are, and a button to read them again.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CoverageSection(opacity: Map<String, Float>, onOpacityChange: (String, Float) -> Unit) {
    val controller: CoverageController = koinInject()
    val state by controller.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.coverage_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            InfoButton(Res.string.coverage_title, Res.string.coverage_info_body)
            Switch(checked = state.enabled, onCheckedChange = controller::setEnabled)
        }
        if (!state.enabled) return@Column

        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PERIODS.forEach { (days, label) ->
                FilterChip(
                    selected = state.days == days,
                    onClick = { controller.setDays(days) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        if (state.sources.isNotEmpty()) {
            var menuOpen by remember { mutableStateOf(false) }
            val chosen = state.sources.firstOrNull { it.nodeNum == state.nodeFilter }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                OutlinedButton(onClick = { menuOpen = true }) {
                    Text(chosen?.name ?: stringResource(Res.string.coverage_all_nodes))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.coverage_all_nodes)) },
                        onClick = {
                            controller.setNode(null)
                            menuOpen = false
                        },
                    )
                    state.sources.forEach { source ->
                        DropdownMenuItem(
                            text = { Text("${source.name} (${source.count})") },
                            onClick = {
                                controller.setNode(source.nodeNum)
                                menuOpen = false
                            },
                        )
                    }
                }
            }
        }

        LayerOpacitySlider(
            layerKey = COVERAGE_LAYER_ID,
            opacity = opacity[COVERAGE_LAYER_ID] ?: COVERAGE_DEFAULT_OPACITY,
            onOpacityChange = onOpacityChange,
        )
        TrackLegend(
            showSnr = true,
            mainName = "",
            mainColor = Color.Unspecified,
            compareName = null,
            compareColor = Color.Unspecified,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Text(
            text =
            when {
                state.loading -> stringResource(Res.string.coverage_loading)
                state.total == 0 -> stringResource(Res.string.coverage_none)

                state.points.isEmpty() -> stringResource(Res.string.coverage_none_filtered)

                state.days == 0 && state.nodeFilter == null ->
                    stringResource(Res.string.coverage_summary, state.points.size, state.nodes)

                else ->
                    stringResource(Res.string.coverage_summary_filtered, state.points.size, state.total, state.nodes)
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        OutlinedButton(
            onClick = { scope.launch { controller.refresh() } },
            enabled = !state.loading,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(stringResource(Res.string.coverage_refresh))
        }
    }
}

private const val WEEK_DAYS = 7
private const val MONTH_DAYS = 30

/** The ages the filter offers, in days (0: all of them), with their names. */
private val PERIODS =
    listOf(
        0 to Res.string.coverage_period_all,
        1 to Res.string.coverage_period_day,
        WEEK_DAYS to Res.string.coverage_period_week,
        MONTH_DAYS to Res.string.coverage_period_month,
    )
