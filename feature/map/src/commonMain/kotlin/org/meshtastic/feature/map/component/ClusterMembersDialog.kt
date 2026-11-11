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
package org.meshtastic.feature.map.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.nodes_at_this_location
import org.meshtastic.core.ui.component.NodeChip

/**
 * Lists the nodes sharing one spot, when zooming in further will not separate them.
 *
 * Renders nothing when [members] is empty, so callers can hand it state directly without a guard.
 */
@Composable
fun ClusterMembersDialog(
    members: List<ClusterMemberEntry>,
    onMemberClick: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (members.isEmpty()) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { Text(text = stringResource(Res.string.nodes_at_this_location)) },
        text = {
            LazyColumn(contentPadding = PaddingValues(vertical = 0.dp)) {
                // Deduped by node number: it is the key and must be unique, and a node can reach the map from two
                // sources, or arrive as a `num = 0` placeholder.
                items(items = members.distinctBy { it.nodeNum }, key = { it.nodeNum }) { member ->
                    ClusterMemberRow(member = member, onClick = { onMemberClick(member.nodeNum) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.close)) } },
    )
}

@Composable
private fun ClusterMemberRow(member: ClusterMemberEntry, onClick: () -> Unit) {
    Column {
        Row(
            modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = MIN_TOUCH_TARGET.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            member.node?.let { NodeChip(node = it) }
            Spacer(Modifier.width(12.dp))
            // One line: the short name is already on the chip, so a subtitle that only repeats it is dropped.
            val extra = member.subtitle.takeIf { it.isNotBlank() && it != member.node?.user?.short_name }
            Text(
                text = if (extra != null) "${member.title} · $extra" else member.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

/** Material's minimum touch target, matching the 48dp the shared nav display uses. */
private const val MIN_TOUCH_TARGET = 44
