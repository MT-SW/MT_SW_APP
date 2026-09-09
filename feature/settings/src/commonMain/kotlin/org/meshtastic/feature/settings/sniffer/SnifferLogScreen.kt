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
package org.meshtastic.feature.settings.sniffer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.sniffer_log_empty
import org.meshtastic.core.resources.sniffer_log_title
import org.meshtastic.core.ui.component.CopyIconButton
import org.meshtastic.core.ui.component.MainAppBar

/** Sniffer Log: live view of locally-heard packets not addressed to this node. See [SnifferLogViewModel]. */
@Composable
fun SnifferLogScreen(viewModel: SnifferLogViewModel, onNavigateUp: () -> Unit) {
    val sniffedPackets by viewModel.sniffedPackets.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.sniffer_log_title),
                subtitle = null,
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                actions = {},
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        if (sniffedPackets.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.sniffer_log_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        val listState = rememberLazyListState()
        // Newest packet is always at index 0 (sortedByDescending). Only follow new arrivals while the user is
        // already at (or near) the top — scrolling away to inspect an older entry must not get yanked back.
        val isNearTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
        var autoScroll by remember { mutableStateOf(true) }

        LaunchedEffect(listState) { snapshotFlow { isNearTop }.collect { autoScroll = it } }

        LaunchedEffect(sniffedPackets.size) {
            if (autoScroll && sniffedPackets.isNotEmpty()) listState.animateScrollToItem(0)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sniffedPackets, key = { "${it.receivedAtMillis}-${it.fromId}-${it.toId}" }) { packet ->
                var isExpanded by remember(packet) { mutableStateOf(false) }
                SniffedPacketCard(packet, isExpanded = isExpanded, onClick = { isExpanded = !isExpanded })
            }
        }
    }
}

@Composable
private fun SniffedPacketCard(packet: SniffedPacket, isExpanded: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${packet.fromId} → ${packet.toId}", style = MaterialTheme.typography.titleSmall)
                Text(
                    // formatDateTime lacks a seconds guarantee across platforms (Android's DateUtils.formatDateTime
                    // has no seconds flag at all); a live packet log needs second-level precision, so compose it
                    // from formatDate + formatTimeWithSeconds instead, which both platforms define consistently.
                    text =
                        "${DateFormatter.formatDate(packet.receivedAtMillis)} " +
                                DateFormatter.formatTimeWithSeconds(packet.receivedAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val hopsText = "hops ${packet.hopStart - packet.hopLimit}/${packet.hopStart}"
            val signalText = listOfNotNull(packet.rssi?.let { "RSSI $it" }, "SNR ${packet.snr}").joinToString(" • ")
            Text(
                text = "ch ${packet.channel} • $hopsText • $signalText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val portText = if (packet.isEncrypted) "encrypted" else packet.portNum?.let { "port $it" } ?: "unknown port"
            Text(
                text = portText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isExpanded) {
                Text(
                    text = packet.decodedPayload ?: packet.payloadHex,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
                CopyIconButton(valueToCopy = packet.copyText, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}