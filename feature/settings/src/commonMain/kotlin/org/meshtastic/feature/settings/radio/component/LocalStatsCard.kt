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
package org.meshtastic.feature.settings.radio.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.local_stats_basic_enabled
import org.meshtastic.core.resources.local_stats_basic_enabled_desc
import org.meshtastic.core.resources.local_stats_basic_interval
import org.meshtastic.core.resources.local_stats_ext_enabled
import org.meshtastic.core.resources.local_stats_ext_enabled_desc
import org.meshtastic.core.resources.local_stats_ext_interval
import org.meshtastic.core.resources.local_stats_needs_v3
import org.meshtastic.core.resources.local_stats_reading
import org.meshtastic.core.resources.local_stats_title
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.component.TitledCard
import org.meshtastic.feature.settings.util.IntervalConfiguration
import org.meshtastic.feature.settings.util.MIN_DEVICE_METRICS_INTERVAL_SECS
import org.meshtastic.feature.settings.util.intervals
import org.meshtastic.feature.settings.util.toDisplayString
import org.meshtastic.proto.LocalStatsBroadcastConfig

/**
 * Extra and extended extra telemetry (the node's own statistics), for MT_SW_FIRMWARE v3 and newer. They are not part of
 * the module config: the radio keeps both options together and gets all four values at once. The card only edits
 * [form]; saving and discarding go through the screen's usual save bar, together with the other telemetry settings.
 *
 * Intervals shorter than [MIN_DEVICE_METRICS_INTERVAL_SECS] (2 hours) are offered only after the hidden options are
 * unlocked ([unlocked]). On older firmware ([supported] false) the card stays visible and looks normal, but nothing in
 * it reacts to touch, and it says from which version it works.
 */
@Composable
internal fun LocalStatsCard(
    config: LocalStatsBroadcastConfig?,
    form: LocalStatsBroadcastConfig,
    supported: Boolean,
    loading: Boolean,
    connected: Boolean,
    unlocked: Boolean,
    onFormChange: (LocalStatsBroadcastConfig) -> Unit,
) {
    val editable = supported && connected && config != null && !loading
    // On older firmware the rows keep their normal look (a dimmed card looks broken) and a transparent layer on top
    // swallows every touch instead.
    val rowsEnabled = editable || !supported

    TitledCard(title = stringResource(Res.string.local_stats_title)) {
        if (!supported) {
            Text(
                text = stringResource(Res.string.local_stats_needs_v3),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        } else if (config == null) {
            Text(
                text = stringResource(Res.string.local_stats_reading),
                modifier = Modifier.padding(16.dp),
            )
        }
        Box {
            Column {
                SwitchPreference(
                    title = stringResource(Res.string.local_stats_basic_enabled),
                    summary = stringResource(Res.string.local_stats_basic_enabled_desc),
                    checked = form.local_stats_enabled,
                    enabled = rowsEnabled,
                    onCheckedChange = { onFormChange(form.copy(local_stats_enabled = it)) },
                    containerColor = CardDefaults.cardColors().containerColor,
                )
                HorizontalDivider()
                val basicItems =
                    IntervalConfiguration.BROADCAST_SHORT.intervals(
                        unlocked,
                        MIN_DEVICE_METRICS_INTERVAL_SECS,
                        form.local_stats_interval_secs.toLong(),
                    )
                DropDownPreference(
                    title = stringResource(Res.string.local_stats_basic_interval),
                    selectedItem = form.local_stats_interval_secs.toLong(),
                    enabled = rowsEnabled,
                    items = basicItems.map { it.value to it.toDisplayString() },
                    onItemSelected = { onFormChange(form.copy(local_stats_interval_secs = it.toInt())) },
                )
                HorizontalDivider()
                SwitchPreference(
                    title = stringResource(Res.string.local_stats_ext_enabled),
                    summary = stringResource(Res.string.local_stats_ext_enabled_desc),
                    checked = form.local_stats_extended_enabled,
                    enabled = rowsEnabled,
                    onCheckedChange = { onFormChange(form.copy(local_stats_extended_enabled = it)) },
                    containerColor = CardDefaults.cardColors().containerColor,
                )
                HorizontalDivider()
                val extItems =
                    IntervalConfiguration.BROADCAST_SHORT.intervals(
                        unlocked,
                        MIN_DEVICE_METRICS_INTERVAL_SECS,
                        form.local_stats_extended_interval_secs.toLong(),
                    )
                DropDownPreference(
                    title = stringResource(Res.string.local_stats_ext_interval),
                    selectedItem = form.local_stats_extended_interval_secs.toLong(),
                    enabled = rowsEnabled,
                    items = extItems.map { it.value to it.toDisplayString() },
                    onItemSelected = { onFormChange(form.copy(local_stats_extended_interval_secs = it.toInt())) },
                )
            }
            if (!supported) {
                Box(
                    Modifier.matchParentSize().pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    },
                )
            }
        }
    }
}
