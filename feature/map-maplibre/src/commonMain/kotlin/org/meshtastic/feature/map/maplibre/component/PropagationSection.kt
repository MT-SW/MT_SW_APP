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
@file:Suppress("MagicNumber")

package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.maplibre.spatialk.geojson.BoundingBox
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.propagation_age
import org.meshtastic.core.resources.propagation_auto
import org.meshtastic.core.resources.propagation_download
import org.meshtastic.core.resources.propagation_failed
import org.meshtastic.core.resources.propagation_hour_ahead
import org.meshtastic.core.resources.propagation_hour_now
import org.meshtastic.core.resources.propagation_kind_enhanced
import org.meshtastic.core.resources.propagation_kind_gradient
import org.meshtastic.core.resources.propagation_kind_k
import org.meshtastic.core.resources.propagation_legend
import org.meshtastic.core.resources.propagation_loading
import org.meshtastic.core.resources.propagation_none
import org.meshtastic.core.resources.propagation_source
import org.meshtastic.core.resources.propagation_title
import org.meshtastic.core.resources.propagation_update
import org.meshtastic.feature.map.component.LayerOpacitySlider
import org.meshtastic.feature.map.maplibre.layers.PROPAGATION_DEFAULT_OPACITY
import org.meshtastic.feature.map.maplibre.layers.PROPAGATION_LAYER_ID
import org.meshtastic.feature.map.propagation.GeoBounds
import org.meshtastic.feature.map.propagation.PropagationController
import org.meshtastic.feature.map.propagation.PropagationKind

/**
 * The propagation layer, as a section of the layers sheet: on/off, the kind of picture, transparency, which hour of the
 * 24-hour forecast, and the download for what is on screen.
 */
@Composable
@Suppress("LongMethod")
internal fun PropagationSection(
    target: OfflineMapTarget,
    opacity: Map<String, Float>,
    onOpacityChange: (String, Float) -> Unit,
) {
    val controller: PropagationController = koinInject()
    val state by controller.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.propagation_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.enabled,
                onCheckedChange = { on ->
                    controller.setEnabled(on)
                    if (on && state.grid == null) target.bounds()?.let { controller.download(it.toGeo()) }
                },
            )
        }
        if (!state.enabled) return@Column

        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PropagationKind.entries.forEach { kind ->
                FilterChip(
                    selected = state.kind == kind,
                    onClick = { controller.setKind(kind) },
                    label = { Text(stringResource(kind.label())) },
                )
            }
        }

        LayerOpacitySlider(
            layerKey = PROPAGATION_LAYER_ID,
            opacity = opacity[PROPAGATION_LAYER_ID] ?: PROPAGATION_DEFAULT_OPACITY,
            onOpacityChange = onOpacityChange,
        )

        val grid = state.grid
        if (grid != null) {
            val ahead = state.hoursAhead
            Text(
                text =
                if (ahead == 0) {
                    stringResource(Res.string.propagation_hour_now)
                } else {
                    stringResource(Res.string.propagation_hour_ahead, ahead)
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            val last = (grid.hours - 1 - grid.hourAt(nowMillis)).coerceAtLeast(1)
            Slider(
                value = ahead.toFloat(),
                onValueChange = { controller.setHoursAhead(it.toInt()) },
                valueRange = 0f..last.toFloat(),
                steps = (last - 1).coerceAtLeast(0),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Legend()

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = state.autoRefresh, onCheckedChange = controller::setAutoRefresh)
            Text(stringResource(Res.string.propagation_auto), style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                enabled = !state.loading,
                onClick = { target.bounds()?.let { controller.download(it.toGeo()) } },
            ) {
                val label = if (grid == null) Res.string.propagation_download else Res.string.propagation_update
                Text(stringResource(label))
            }
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(Res.string.propagation_loading), style = MaterialTheme.typography.bodySmall)
            }
        }

        val status =
            when {
                state.failed -> stringResource(Res.string.propagation_failed)

                grid == null -> stringResource(Res.string.propagation_none)

                else ->
                    stringResource(Res.string.propagation_age, ((nowMillis - grid.fetchedAtMillis) / 60_000L).toInt())
            }
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(Res.string.propagation_source),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun Legend() {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(stringResource(Res.string.propagation_legend), style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(0xFF3080E0, 0xFF40C040, 0xFFF0D020, 0xFFF08020, 0xFFE02020, 0xFFB000B0).forEach {
                Box(Modifier.size(14.dp).clip(CircleShape).background(Color(it)))
            }
        }
    }
}

private val PROPAGATION_KIND_LABELS: Map<PropagationKind, StringResource> =
    mapOf(
        PropagationKind.GRADIENT to Res.string.propagation_kind_gradient,
        PropagationKind.ENHANCED to Res.string.propagation_kind_enhanced,
        PropagationKind.K_FACTOR to Res.string.propagation_kind_k,
    )

private fun PropagationKind.label(): StringResource = PROPAGATION_KIND_LABELS.getValue(this)

private fun BoundingBox.toGeo(): GeoBounds = GeoBounds(south = south, west = west, north = north, east = east)
