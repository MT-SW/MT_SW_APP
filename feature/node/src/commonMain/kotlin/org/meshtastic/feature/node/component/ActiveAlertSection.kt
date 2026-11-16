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
package org.meshtastic.feature.node.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.NotificationPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.node_active_alert
import org.meshtastic.core.resources.node_active_alert_hint
import org.meshtastic.core.resources.node_away_alert
import org.meshtastic.core.ui.component.Switch

/**
 * MT_SW: the "active" alert of one node: a notification when the activity status the node shares changes to active. Off
 * by default; the quiet hours are the ones of the silence alerts.
 */
@Composable
fun ActiveAlertSection(nodeNum: Int, modifier: Modifier = Modifier) {
    val prefs = koinInject<NotificationPrefs>()
    val alerts by prefs.nodeActiveAlerts.collectAsStateWithLifecycle()
    val away by prefs.nodeAwayAlerts.collectAsStateWithLifecycle()
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.node_active_alert),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Switch(checked = nodeNum in alerts, onCheckedChange = { prefs.setNodeActiveAlert(nodeNum, it) })
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.node_active_alert_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.node_away_alert),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = nodeNum in away, onCheckedChange = { prefs.setNodeAwayAlert(nodeNum, it) })
            }
        }
    }
}
