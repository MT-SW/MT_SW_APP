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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import org.meshtastic.core.resources.coverage_info_body
import org.meshtastic.core.resources.coverage_loading
import org.meshtastic.core.resources.coverage_none
import org.meshtastic.core.resources.coverage_refresh
import org.meshtastic.core.resources.coverage_summary
import org.meshtastic.core.resources.coverage_title
import org.meshtastic.core.ui.component.InfoButton
import org.meshtastic.feature.map.component.LayerOpacitySlider
import org.meshtastic.feature.map.coverage.CoverageController
import org.meshtastic.feature.map.maplibre.layers.COVERAGE_DEFAULT_OPACITY
import org.meshtastic.feature.map.maplibre.layers.COVERAGE_LAYER_ID

/**
 * The coverage layer, as a section of the layers sheet: on/off with a plain description behind the "i", transparency,
 * the colour key, how many measurements there are, and a button to read them again.
 */
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
                state.points.isEmpty() -> stringResource(Res.string.coverage_none)
                else -> stringResource(Res.string.coverage_summary, state.points.size, state.nodes)
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
