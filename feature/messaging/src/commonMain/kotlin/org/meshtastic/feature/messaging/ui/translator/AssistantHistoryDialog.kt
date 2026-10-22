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
package org.meshtastic.feature.messaging.ui.translator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.assistant_close
import org.meshtastic.core.resources.assistant_delete
import org.meshtastic.core.resources.assistant_history
import org.meshtastic.core.resources.assistant_history_empty
import org.meshtastic.core.resources.assistant_new
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.feature.messaging.translation.StoredChat

/** The list of saved assistant conversations: tap one to continue it, or delete it. */
@Composable
fun AssistantHistoryDialog(
    chats: List<StoredChat>,
    onOpen: (StoredChat) -> Unit,
    onDelete: (StoredChat) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.assistant_history)) },
        text = {
            if (chats.isEmpty()) {
                Text(stringResource(Res.string.assistant_history_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(chats, key = { it.id }) { chat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(chat) }.padding(vertical = 4.dp),
                        ) {
                            Text(
                                text = chat.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onDelete(chat) }) {
                                Icon(MeshtasticIcons.Delete, contentDescription = stringResource(Res.string.assistant_delete))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onNew) { Text(stringResource(Res.string.assistant_new)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.assistant_close)) } },
    )
}
