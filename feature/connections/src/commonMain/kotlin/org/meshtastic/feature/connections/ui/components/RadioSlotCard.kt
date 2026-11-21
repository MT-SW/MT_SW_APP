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
package org.meshtastic.feature.connections.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.radio_chooser_connect
import org.meshtastic.core.resources.radio_chooser_disconnect
import org.meshtastic.core.resources.radio_chooser_remove
import org.meshtastic.core.resources.radio_slot_background
import org.meshtastic.core.resources.radio_slot_empty
import org.meshtastic.core.resources.radio_slot_position
import org.meshtastic.core.resources.radio_slot_use
import org.meshtastic.core.resources.radio_status_connecting
import org.meshtastic.core.resources.radio_status_disconnected
import org.meshtastic.core.ui.component.Button
import org.meshtastic.core.ui.component.OutlinedButton
import org.meshtastic.core.ui.component.RadioPlate
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.MeshRadio
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.viewmodel.RadioChoice
import org.meshtastic.core.ui.viewmodel.RadioStatus

/**
 * The page of one saved-radio place of the connection card that is not the radio in use: an empty place (the next
 * device picked from the list is saved here), or a saved radio that is connected in the background or disconnected.
 * Connecting a radio here makes it the one in use; the radio that was in use stays connected in the background.
 */
@Composable
fun RadioSlotCard(
    slotId: Int,
    slotCount: Int,
    radio: RadioChoice?,
    onUse: () -> Unit,
    onDisconnect: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 100.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.radio_slot_position, slotId, slotCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Icon(
            imageVector = MeshtasticIcons.MeshRadio,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
        if (radio == null) {
            Text(
                text = stringResource(Res.string.radio_slot_empty),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
            )
            return@Column
        }
        RadioPlate(radio)
        Text(
            text =
            radio.progress ?: when (radio.status) {
                RadioStatus.Connected -> stringResource(Res.string.radio_slot_background)
                RadioStatus.Connecting -> stringResource(Res.string.radio_status_connecting)
                RadioStatus.Disconnected -> stringResource(Res.string.radio_status_disconnected)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The buttons share the width of the card (never wider than it), whatever the size of the window.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onUse, modifier = Modifier.weight(1f)) {
                Text(
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text =
                    stringResource(
                        if (radio.status == RadioStatus.Connected) {
                            Res.string.radio_slot_use
                        } else {
                            Res.string.radio_chooser_connect
                        },
                    ),
                )
            }
            if (radio.status != RadioStatus.Disconnected) {
                OutlinedButton(onClick = onDisconnect, modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.radio_chooser_disconnect),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        TextButton(onClick = onRemove) { Text(stringResource(Res.string.radio_chooser_remove)) }
    }
}
