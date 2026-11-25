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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.remote_pick_from_list
import org.meshtastic.core.resources.remote_pick_search
import org.meshtastic.core.ui.component.ModalBottomSheet
import org.meshtastic.core.ui.component.NodeChip

/**
 * MT_SW: a window with our own node list, from which the nodes to manage on the remote device are ticked (many at
 * once, with a search). Everything ticked stays selected after the window is closed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemoteNodePickerSheet(
    nodes: List<Node>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val queryState = rememberTextFieldState("")
    val query = queryState.text.toString().trim()
    val shown =
        remember(nodes, query) {
            nodes
                .filter { node ->
                    query.isEmpty() ||
                        node.effectiveLongName.contains(query, ignoreCase = true) ||
                        node.user.short_name.contains(query, ignoreCase = true) ||
                        NodeAddress.numToDefaultId(node.num).contains(query, ignoreCase = true)
                }
                .sortedBy { it.effectiveLongName.lowercase() }
        }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(Res.string.remote_pick_from_list),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        OutlinedTextField(
            state = queryState,
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.remote_pick_search)) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(shown, key = { it.num }) { node ->
                Row(
                    modifier =
                    Modifier.fillMaxWidth()
                        .clickable { onToggle(node.num) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Checkbox(checked = node.num in selected, onCheckedChange = null)
                    NodeChip(node = node)
                    Text(
                        text = node.effectiveLongName,
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = NodeAddress.numToDefaultId(node.num),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
