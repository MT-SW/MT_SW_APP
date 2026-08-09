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
package org.meshtastic.feature.node.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.NumberFormatter

@Composable
fun NetworkSummaryScreen(viewModel: NetworkSummaryViewModel, onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Podsumowanie") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.attentionItems.isNotEmpty()) {
                item {
                    SummaryCard(title = "⚠ Wymaga uwagi") {
                        uiState.attentionItems.forEach { item ->
                            Text(text = "${item.nodeName}: ${item.reason}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                SummaryCard(title = "Ogólne") {
                    Text(
                        "Online: ${uiState.onlineCount} / ${uiState.totalCount}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (uiState.quietestNodes.isNotEmpty()) {
                        Text(text = "Najdłużej milczą:", style = MaterialTheme.typography.bodyMedium)
                        uiState.quietestNodes.forEachIndexed { index, node ->
                            Text(
                                text = "${index + 1}. ${node.nodeName} — ${node.hoursSilent}h",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            item {
                SummaryCard(title = "Zasilanie") {
                    Text(
                        text = "Śr. bateria: ${uiState.averageBatteryLevel?.let { "$it%" } ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Słaba bateria (<20%): ${uiState.lowBatteryCount}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Zasilanie sieciowe: ${uiState.mainsPoweredCount}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                SummaryCard(title = "Sygnał") {
                    if (uiState.topSignalNodes.isNotEmpty()) {
                        Text(text = "Najlepszy sygnał:", style = MaterialTheme.typography.bodyMedium)
                        uiState.topSignalNodes.forEachIndexed { index, leader ->
                            Text(
                                text = "${index + 1}. ${leader.nodeName} — ${MetricFormatter.snr(leader.snr)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Text("Słaby sygnał: ${uiState.weakSignalCount} węzłów", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Śr. noise floor: ${uiState.averageNoiseFloor?.let { "$it dB" } ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                SummaryCard(title = "Eter") {
                    Text(
                        text =
                        "Śr. zajęcie kanału: ${uiState.averageChannelUtilization?.let {
                            MetricFormatter.percent(it)
                        } ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text =
                        "Max zajęcie kanału: ${uiState.maxChannelUtilization?.let {
                            MetricFormatter.percent(it)
                        } ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text =
                        "Śr. air util TX: ${uiState.averageAirUtilTx?.let {
                            MetricFormatter.percent(it)
                        } ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                SummaryCard(title = "Ruch") {
                    Text("Pakiety TX: ${uiState.totalPacketsTx}", style = MaterialTheme.typography.bodyMedium)
                    Text("Pakiety RX: ${uiState.totalPacketsRx}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Duplikaty: ${uiState.totalDupes}, Uszkodzone: ${uiState.totalBad}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (uiState.topRelayNodeName != null) {
                        Text(
                            text = "Najwięcej przekazał: ${uiState.topRelayNodeName} (${uiState.topRelayCount})",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                SummaryCard(title = "Sąsiedzi") {
                    Text(
                        "Zgłasza sąsiadów: ${uiState.neighborReportingCount} węzłów",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text =
                        "Śr. liczba sąsiadów: ${uiState.averageNeighborCount?.let {
                            NumberFormatter.format(it, 1)
                        } ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (uiState.uptimeLeaders.isNotEmpty()) {
                item {
                    SummaryCard(title = "Najdłużej działające węzły") {
                        uiState.uptimeLeaders.forEachIndexed { index, leader ->
                            Text(
                                text = "${index + 1}. ${leader.nodeName} — ${formatUptime(leader.uptimeSeconds)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            if (uiState.topPositionSenders.isNotEmpty()) {
                item {
                    SummaryCard(title = "Najwięcej pozycji") {
                        uiState.topPositionSenders.forEachIndexed { index, sender ->
                            Text(
                                text = "${index + 1}. ${sender.nodeName} — ${sender.positionCount}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            if (uiState.closestNodes.isNotEmpty()) {
                item {
                    SummaryCard(title = "Najbliżej") {
                        uiState.closestNodes.forEachIndexed { index, node ->
                            val distanceText =
                                if (node.distanceMeters >= METERS_PER_KM) {
                                    "${NumberFormatter.format(node.distanceMeters / METERS_PER_KM.toFloat(), 1)} km"
                                } else {
                                    "${node.distanceMeters} m"
                                }
                            Text(
                                text = "${index + 1}. ${node.nodeName} — $distanceText",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            if (uiState.topTelemetryVolumeSenders.isNotEmpty()) {
                item {
                    SummaryCard(title = "Najwięcej danych (telemetria)") {
                        uiState.topTelemetryVolumeSenders.forEachIndexed { index, leader ->
                            Text(
                                text = "${index + 1}. ${leader.nodeName} — ${leader.packetCount}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            if (uiState.topAllPacketVolumeSenders.isNotEmpty()) {
                item {
                    SummaryCard(title = "Najwięcej danych (wszystkie pakiety)") {
                        uiState.topAllPacketVolumeSenders.forEachIndexed { index, leader ->
                            Text(
                                text = "${index + 1}. ${leader.nodeName} — ${leader.packetCount}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            item {
                SummaryCard(title = "Wiadomości") {
                    Text(
                        text = "Tydzień: ${uiState.totalMessagesWeek}, dziś: ${uiState.totalMessagesToday}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (uiState.channelMessageCounts.isNotEmpty()) {
                        uiState.channelMessageCounts.forEach { channel ->
                            Text(
                                text =
                                "Kanał ${channel.channelIndex}: tydzień ${channel.weekCount}, " +
                                    "dziś ${channel.todayCount}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            if (uiState.topMessageSendersWeek.isNotEmpty()) {
                item {
                    SummaryCard(title = "Najaktywniejsi (wiadomości, tydzień)") {
                        uiState.topMessageSendersWeek.forEachIndexed { index, sender ->
                            Text(
                                text = "${index + 1}. ${sender.nodeName} — ${sender.count}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            if (uiState.topMessageSendersToday.isNotEmpty()) {
                item {
                    SummaryCard(title = "Najaktywniejsi (wiadomości, dziś)") {
                        uiState.topMessageSendersToday.forEachIndexed { index, sender ->
                            Text(
                                text = "${index + 1}. ${sender.nodeName} — ${sender.count}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private fun formatUptime(seconds: Int): String {
    val hours = seconds / SECONDS_PER_HOUR
    val days = hours / HOURS_PER_DAY
    return if (days > 0) "${days}d ${hours % HOURS_PER_DAY}h" else "${hours}h"
}

private const val SECONDS_PER_HOUR = 3600
private const val HOURS_PER_DAY = 24
private const val METERS_PER_KM = 1000
