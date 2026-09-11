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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.sniffer_auto_scroll
import org.meshtastic.core.resources.sniffer_log_empty
import org.meshtastic.core.resources.sniffer_log_title
import org.meshtastic.core.resources.sniffer_new_packets
import org.meshtastic.core.ui.component.CopyIconButton
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.ArrowCircleUp
import org.meshtastic.core.ui.icon.MeshtasticIcons

/** Sniffer Log: live view of locally-heard packets not addressed to this node. See [SnifferLogViewModel]. */
@Composable
fun SnifferLogScreen(viewModel: SnifferLogViewModel, onNavigateUp: () -> Unit) {
    val sniffedPackets by viewModel.sniffedPackets.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Newest packet is always at index 0 (sortedByDescending). Auto-scroll is a manual toggle via the FAB below.
    var autoScroll by remember { mutableStateOf(true) }
    // Timestamp of the newest packet at the moment auto-scroll was paused. Null while auto-scroll is on. Used to
    // count unseen packets by comparing timestamps rather than list length — robust to MeshLogRepository's
    // DEFAULT_MAX_LOGS cap, where the list's *length* can stop changing even as its *content* keeps changing.
    var pausedAtTimestamp by remember { mutableStateOf<Long?>(null) }
    val unseenCount = pausedAtTimestamp?.let { threshold -> sniffedPackets.count { it.receivedAtMillis > threshold } } ?: 0

    val fabState =
        when {
            autoScroll -> FabDisplayState.Live
            unseenCount > 0 -> FabDisplayState.PausedWithNew(unseenCount)
            else -> FabDisplayState.PausedIdle
        }

    // Keying on `.size` broke once the shared `log` table passed MeshLogRepository's DEFAULT_MAX_LOGS cap (5000
    // rows): getAllLogs() then always returns exactly that many rows — a new packet pushes the oldest one out of
    // the capped result at the same time it adds itself, so the list's *length* stops changing even though its
    // *content* keeps changing. Keying on the newest item's identity instead reacts correctly regardless of the cap.
    val newestPacketKey = sniffedPackets.firstOrNull()?.let { "${it.receivedAtMillis}-${it.fromId}-${it.toId}" }
    LaunchedEffect(newestPacketKey) {
        if (autoScroll && newestPacketKey != null) listState.animateScrollToItem(0)
    }

    fun onFabClick() {
        if (autoScroll) {
            pausedAtTimestamp = sniffedPackets.firstOrNull()?.receivedAtMillis
            autoScroll = false
        } else {
            pausedAtTimestamp = null
            autoScroll = true
            coroutineScope.launch { if (sniffedPackets.isNotEmpty()) listState.animateScrollToItem(0) }
        }
    }

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
        floatingActionButton = {
            if (sniffedPackets.isNotEmpty()) {
                AutoScrollFab(fabState = fabState, onClick = ::onFabClick)
            }
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
                val fromLabel = packet.fromShortName?.let { "${packet.fromId} ($it)" } ?: packet.fromId
                val toLabel = packet.toShortName?.let { "${packet.toId} ($it)" } ?: packet.toId
                Text(text = "$fromLabel → $toLabel", style = MaterialTheme.typography.titleSmall)
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

/** Auto-scroll FAB display state: following live, paused with nothing new, or paused with [PausedWithNew.count]. */
private sealed class FabDisplayState {
    data object Live : FabDisplayState()
    data object PausedIdle : FabDisplayState()
    data class PausedWithNew(val count: Int) : FabDisplayState()
}

@Composable
private fun AutoScrollFab(fabState: FabDisplayState, onClick: () -> Unit) {
    Crossfade(targetState = fabState, label = "auto_scroll_fab") { state ->
        when (state) {
            is FabDisplayState.Live -> LiveAutoScrollFab(onClick = onClick)
            is FabDisplayState.PausedIdle -> PausedAutoScrollFab(onClick = onClick)
            is FabDisplayState.PausedWithNew -> NewPacketsFab(count = state.count, onClick = onClick)
        }
    }
}

@Composable
private fun LiveAutoScrollFab(onClick: () -> Unit) {
    Box {
        FloatingActionButton(onClick = onClick) {
            Icon(
                imageVector = MeshtasticIcons.ArrowCircleUp,
                contentDescription = stringResource(Res.string.sniffer_auto_scroll),
            )
        }
        val infiniteTransition = rememberInfiniteTransition(label = "live_dot")
        val dotAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.4f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "live_dot_alpha",
        )
        Box(
            modifier =
                Modifier.size(10.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = dotAlpha)),
        )
    }
}

@Composable
private fun PausedAutoScrollFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Icon(
            imageVector = MeshtasticIcons.ArrowCircleUp,
            contentDescription = stringResource(Res.string.sniffer_auto_scroll),
        )
    }
}

@Composable
private fun NewPacketsFab(count: Int, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = {
            Icon(
                imageVector = MeshtasticIcons.ArrowCircleUp,
                contentDescription = stringResource(Res.string.sniffer_auto_scroll),
            )
        },
        text = { Text(stringResource(Res.string.sniffer_new_packets, count)) },
    )
}