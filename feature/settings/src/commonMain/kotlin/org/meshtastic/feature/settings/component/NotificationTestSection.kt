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
package org.meshtastic.feature.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.Notification
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.notification_test_alert_button
import org.meshtastic.core.resources.notification_test_alert_text
import org.meshtastic.core.resources.notification_test_hint
import org.meshtastic.core.resources.notification_test_radio_button
import org.meshtastic.core.resources.notification_test_radio_text
import org.meshtastic.core.resources.notification_test_sender
import org.meshtastic.core.resources.notification_test_title
import org.meshtastic.proto.ClientNotification

/** The first channel, where the tap on a test alarm leads (the alarm itself is not sent to anyone). */
private const val TEST_CONTACT_KEY = "0^all"

/**
 * Buttons that show a notification of one kind at once (an alarm such as an SOS, and a notice from the radio), so the
 * sound, the switch or the system setting of that kind can be tried without waiting for a real event. Nothing is sent
 * over the radio.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationTestSection() {
    val manager = koinInject<MeshNotificationManager>()
    val scope = rememberCoroutineScope()
    val sender = stringResource(Res.string.notification_test_sender)
    val alertText = stringResource(Res.string.notification_test_alert_text)
    val radioText = stringResource(Res.string.notification_test_radio_text)

    ExpressiveSection(title = stringResource(Res.string.notification_test_title)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(Res.string.notification_test_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedButton(
                    onClick = { scope.launch { manager.showAlertNotification(TEST_CONTACT_KEY, sender, alertText) } },
                ) {
                    Text(stringResource(Res.string.notification_test_alert_button))
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            manager.showClientNotification(
                                ClientNotification(message = radioText),
                                sender,
                                Notification.Type.Info,
                            )
                        }
                    },
                ) {
                    Text(stringResource(Res.string.notification_test_radio_button))
                }
            }
        }
    }
}
