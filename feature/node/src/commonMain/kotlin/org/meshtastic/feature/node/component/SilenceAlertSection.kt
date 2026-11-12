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

package org.meshtastic.feature.node.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.NotificationPrefs
import org.meshtastic.core.repository.QuietHours
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.formatDuration
import org.meshtastic.core.resources.node_silence_alert
import org.meshtastic.core.resources.node_silence_alert_custom
import org.meshtastic.core.resources.node_silence_count_relayed
import org.meshtastic.core.resources.node_silence_count_relayed_hint
import org.meshtastic.core.resources.node_silence_quiet_from
import org.meshtastic.core.resources.node_silence_quiet_hint
import org.meshtastic.core.resources.node_silence_quiet_title
import org.meshtastic.core.resources.node_silence_quiet_to
import org.meshtastic.core.resources.node_silence_alert_hint
import org.meshtastic.core.resources.node_silence_alert_off
import org.meshtastic.core.resources.save
import org.meshtastic.core.ui.icon.Add
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Remove
import org.meshtastic.core.ui.icon.Save
import kotlin.time.DurationUnit

/** Ready choices, in minutes: from an hour up to a month. Any other time can be typed in hours. */
private val SILENCE_CHOICES_MINUTES =
    listOf(60, 120, 180, 360, 720, 1_440, 2_880, 4_320, 7_200, 10_080, 20_160, 43_200)

private const val MINUTES_PER_HOUR = 60
private const val MAX_CUSTOM_HOURS = 24 * 90

/**
 * The node watchdog (silence alert) of one node: after how long without a radio signal from it the app raises a
 * notification. Set per node, from an hour up to 90 days; off by default. Optionally also counts packets that only
 * passed through the node.
 */
@Composable
fun SilenceAlertSection(nodeNum: Int, modifier: Modifier = Modifier) {
    val prefs = koinInject<NotificationPrefs>()
    val alerts by prefs.nodeSilenceAlerts.collectAsStateWithLifecycle()
    val current = alerts[nodeNum]
    val relayed by prefs.nodeSilenceCountRelayed.collectAsStateWithLifecycle()
    val quiet by prefs.nodeSilenceQuietHours.collectAsStateWithLifecycle()
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(Res.string.node_silence_alert),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.node_silence_alert_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = current == null,
                    onClick = { prefs.setNodeSilenceAlert(nodeNum, null) },
                    label = { Text(stringResource(Res.string.node_silence_alert_off)) },
                )
                for (minutes in SILENCE_CHOICES_MINUTES) {
                    FilterChip(
                        selected = current == minutes,
                        onClick = { prefs.setNodeSilenceAlert(nodeNum, minutes) },
                        label = { Text(formatDuration(minutes * 60L, DurationUnit.MINUTES)) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            var hours by remember(nodeNum, current) { mutableStateOf(current?.let { (it / MINUTES_PER_HOUR).toString() }.orEmpty()) }
            val hoursValue = hours.toIntOrNull()?.takeIf { it in 1..MAX_CUSTOM_HOURS }
            val keyboardController = LocalSoftwareKeyboardController.current
            val save = {
                hoursValue?.let { prefs.setNodeSilenceAlert(nodeNum, it * MINUTES_PER_HOUR) }
                keyboardController?.hide()
            }
            OutlinedTextField(
                value = hours,
                onValueChange = { value -> hours = value.filter { it.isDigit() }.take(MAX_CUSTOM_HOURS.toString().length) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(Res.string.node_silence_alert_custom)) },
                shape = MaterialTheme.shapes.large,
                trailingIcon = {
                    IconButton(onClick = { save() }, enabled = hoursValue != null) {
                        Icon(imageVector = MeshtasticIcons.Save, contentDescription = stringResource(Res.string.save))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.node_silence_count_relayed),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(Res.string.node_silence_count_relayed_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = nodeNum in relayed,
                    onCheckedChange = { prefs.setNodeSilenceCountRelayed(nodeNum, it) },
                )
            }

            // Quiet hours are one setting for every node's watchdog, kept here beside them.
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.node_silence_quiet_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(Res.string.node_silence_quiet_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = quiet != null,
                    onCheckedChange = { prefs.setNodeSilenceQuietHours(if (it) QuietHours(DEFAULT_QUIET_FROM, DEFAULT_QUIET_TO) else null) },
                )
            }
            quiet?.let { hours ->
                HourStepper(
                    label = stringResource(Res.string.node_silence_quiet_from),
                    hour = hours.startHour,
                    onChange = { prefs.setNodeSilenceQuietHours(hours.copy(startHour = it)) },
                )
                HourStepper(
                    label = stringResource(Res.string.node_silence_quiet_to),
                    hour = hours.endHour,
                    onChange = { prefs.setNodeSilenceQuietHours(hours.copy(endHour = it)) },
                )
            }
        }
    }
}

private const val DEFAULT_QUIET_FROM = 22
private const val DEFAULT_QUIET_TO = 7
private const val HOURS_PER_DAY = 24

/** One whole hour of the day with a button each side to move it, wrapping round midnight. */
@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = { onChange((hour + HOURS_PER_DAY - 1) % HOURS_PER_DAY) }) {
            Icon(imageVector = MeshtasticIcons.Remove, contentDescription = null)
        }
        Text(text = hour.toString().padStart(2, '0') + ":00", style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { onChange((hour + 1) % HOURS_PER_DAY) }) {
            Icon(imageVector = MeshtasticIcons.Add, contentDescription = null)
        }
    }
}
