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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.ui.component.FullScreenLoading
import org.meshtastic.core.ui.icon.ArrowBack
import org.meshtastic.core.ui.icon.BatteryEmpty
import org.meshtastic.core.ui.icon.ChannelUtilization
import org.meshtastic.core.ui.icon.DataUsage
import org.meshtastic.core.ui.icon.Distance
import org.meshtastic.core.ui.icon.Download
import org.meshtastic.core.ui.icon.MeshHub
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Message
import org.meshtastic.core.ui.icon.Nodes
import org.meshtastic.core.ui.icon.Share
import org.meshtastic.core.ui.icon.SignalCellular4Bar
import org.meshtastic.core.ui.icon.Speed
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline
import org.meshtastic.core.ui.util.rememberFileExporter
import org.meshtastic.core.ui.util.rememberShareText
import kotlin.math.round
import kotlin.time.Instant

private const val BATTERY_GOOD = 50
private const val BATTERY_MEDIUM = 20
private const val BATTERY_BAD = 5
private const val ETHER_GOOD = 15f
private const val ETHER_MEDIUM = 25f
private const val ETHER_BAD = 40f
private const val SNR_BAR_OFFSET = 30f
private const val PERCENT = 100f
private const val TWO_DIGITS = 2
private const val OFFLINE_ALPHA = 0.25f

private fun batteryLevelOf(percent: Int): HealthLevel = when {
    percent >= BATTERY_GOOD -> HealthLevel.GOOD
    percent >= BATTERY_MEDIUM -> HealthLevel.MEDIUM
    percent >= BATTERY_BAD -> HealthLevel.BAD
    else -> HealthLevel.CRITICAL
}

private fun etherLevelOf(percent: Float): HealthLevel = when {
    percent < ETHER_GOOD -> HealthLevel.GOOD
    percent < ETHER_MEDIUM -> HealthLevel.MEDIUM
    percent < ETHER_BAD -> HealthLevel.BAD
    else -> HealthLevel.CRITICAL
}

internal fun generatedStamp(): String {
    val dt = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val time = "${dt.hour.toString().padStart(TWO_DIGITS, '0')}:${dt.minute.toString().padStart(TWO_DIGITS, '0')}"
    return "Wygenerowano: ${dt.date} $time"
}

private const val MIME_PDF = "application/pdf"
private const val MIME_HTML = "text/html"
private const val MIME_MARKDOWN = "text/markdown"
private const val MIME_TEXT = "text/plain"
private const val MIME_CSV = "text/csv"

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod")
@Composable
fun NetworkSummaryScreen(viewModel: NetworkSummaryViewModel, onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val share = rememberShareText()
    var menuOpen by remember { mutableStateOf(false) }

    val exportPdf = rememberFileExporter(content = { uiState.toReport(generatedStamp()).toPdf() })
    val exportHtml = rememberFileExporter(content = { uiState.toReport(generatedStamp()).toHtml().encodeToByteArray() })
    val exportMarkdown =
        rememberFileExporter(content = { uiState.toReport(generatedStamp()).toMarkdown().encodeToByteArray() })
    val exportText =
        rememberFileExporter(content = { uiState.toReport(generatedStamp()).toPlainText().encodeToByteArray() })
    val exportCsv = rememberFileExporter(content = { uiState.toReport(generatedStamp()).toCsv().encodeToByteArray() })

    val gold = MaterialTheme.colorScheme.StatusOnline
    val purple = HealthLevel.BAD.color()
    val red = HealthLevel.MEDIUM.color()
    val iconMessage = MeshtasticIcons.Message
    val iconSpeed = MeshtasticIcons.Speed
    val iconDistance = MeshtasticIcons.Distance
    val iconDataUsage = MeshtasticIcons.DataUsage
    val iconNodes = MeshtasticIcons.Nodes

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Podsumowanie") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(imageVector = MeshtasticIcons.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val report = uiState.toReport(generatedStamp())
                        share(report.toPlainText(), report.title)
                    }) {
                        Icon(MeshtasticIcons.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            MeshtasticIcons.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Zapisz jako PDF") },
                            onClick = {
                                menuOpen = false
                                exportPdf("podsumowanie_sieci.pdf", MIME_PDF)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Zapisz jako strona HTML") },
                            onClick = {
                                menuOpen = false
                                exportHtml("podsumowanie_sieci.html", MIME_HTML)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Zapisz jako Markdown") },
                            onClick = {
                                menuOpen = false
                                exportMarkdown("podsumowanie_sieci.md", MIME_MARKDOWN)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Zapisz jako tekst") },
                            onClick = {
                                menuOpen = false
                                exportText("podsumowanie_sieci.txt", MIME_TEXT)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Zapisz jako CSV (arkusz)") },
                            onClick = {
                                menuOpen = false
                                exportCsv("podsumowanie_sieci.csv", MIME_CSV)
                            },
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        if (!uiState.isLoaded) {
            FullScreenLoading(modifier = Modifier.padding(contentPadding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { SummaryHero(uiState) }

            if (uiState.attentionItems.isNotEmpty()) {
                item {
                    SummarySection(title = "Wymaga uwagi", icon = MeshtasticIcons.Warning, accent = purple) {
                        uiState.attentionItems.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                HealthDot(color = purple)
                                Text(
                                    text = "${item.nodeName}: ${item.reason}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val battery = uiState.averageBatteryLevel
                        StatTile(
                            label = "Średnia bateria",
                            value = battery?.let { "$it%" } ?: "—",
                            icon = MeshtasticIcons.BatteryEmpty,
                            color = battery?.let { batteryLevelOf(it).color() } ?: gold,
                            modifier = Modifier.weight(1f),
                        )
                        val ether = uiState.averageChannelUtilization
                        StatTile(
                            label = "Zajęcie kanału",
                            value = ether?.let { MetricFormatter.percent(it, decimalPlaces = 1) } ?: "—",
                            icon = MeshtasticIcons.ChannelUtilization,
                            color = ether?.let { etherLevelOf(it).color() } ?: gold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            label = "Szum (średnio)",
                            value = uiState.averageNoiseFloor?.let { "$it dB" } ?: "—",
                            icon = MeshtasticIcons.SignalCellular4Bar,
                            // Same limits and colours as the signal tab (the average is already after LNA).
                            color = uiState.averageNoiseFloor?.let { noiseFloorLevel(it.toFloat()).color() } ?: gold,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Wiadomości dziś",
                            value = "${uiState.totalMessagesToday}",
                            icon = MeshtasticIcons.Message,
                            color = gold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                SummarySection(title = "Zasilanie", icon = MeshtasticIcons.BatteryEmpty) {
                    DistributionBar(counts = uiState.batteryDistribution)
                    Text(
                        text =
                        "Słaba bateria: ${uiState.lowBatteryCount}, zasilanie zewnętrzne: ${uiState.mainsPoweredCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SummarySection(title = "Sygnał", icon = MeshtasticIcons.SignalCellular4Bar) {
                    DistributionBar(counts = uiState.snrDistribution)
                    if (uiState.topSignalNodes.isNotEmpty()) {
                        Text("Najlepszy sygnał", style = MaterialTheme.typography.labelLarge)
                        BarList(
                            bars =
                            uiState.topSignalNodes.map {
                                ReportBar(it.nodeName, it.snr + SNR_BAR_OFFSET, MetricFormatter.snr(it.snr))
                            },
                            color = gold,
                        )
                    }
                    Text(
                        text = "Węzły ze słabym sygnałem: ${uiState.weakSignalCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SummarySection(title = "Eter", icon = MeshtasticIcons.ChannelUtilization) {
                    EtherGauges(uiState)
                }
            }

            item {
                SummarySection(title = "Ruch", icon = MeshtasticIcons.DataUsage) {
                    TrafficBlock(uiState)
                }
            }

            item {
                SummarySection(title = "Sąsiedzi", icon = MeshtasticIcons.MeshHub) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        BigNumber("${uiState.neighborReportingCount}", "węzłów z raportem")
                        BigNumber(
                            uiState.averageNeighborCount?.let { oneDecimal(it) } ?: "—",
                            "średnio sąsiadów",
                        )
                    }
                }
            }

            item {
                SummarySection(title = "Wiadomości", icon = MeshtasticIcons.Message) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        BigNumber("${uiState.totalMessagesWeek}", "w tygodniu")
                        BigNumber("${uiState.totalMessagesToday}", "dziś")
                    }
                    val days = uiState.messagesPerDay
                    ColumnChart(
                        values = days,
                        labels = days.indices.map { if (it == days.lastIndex) "dziś" else "-${days.lastIndex - it}d" },
                        color = gold,
                    )
                    if (uiState.channelMessageCounts.isNotEmpty()) {
                        Text("Według kanałów (tydzień)", style = MaterialTheme.typography.labelLarge)
                        BarList(
                            bars =
                            uiState.channelMessageCounts.map {
                                ReportBar("Kanał ${it.channelIndex}", it.weekCount.toFloat(), "${it.weekCount}")
                            },
                            color = red,
                        )
                    }
                }
            }

            rankingItem(
                "Najaktywniejsi (tydzień)",
                iconMessage,
                uiState.topMessageSendersWeek.map { ReportBar(it.nodeName, it.count.toFloat(), "${it.count}") },
                gold,
            )
            rankingItem(
                "Najaktywniejsi (dziś)",
                iconMessage,
                uiState.topMessageSendersToday.map { ReportBar(it.nodeName, it.count.toFloat(), "${it.count}") },
                gold,
            )
            rankingItem(
                "Najdłużej działające węzły",
                iconSpeed,
                uiState.uptimeLeaders.map {
                    ReportBar(it.nodeName, it.uptimeSeconds.toFloat(), uptimeText(it.uptimeSeconds))
                },
                gold,
            )
            rankingItem(
                "Najwięcej pozycji",
                iconDistance,
                uiState.topPositionSenders.map {
                    ReportBar(it.nodeName, it.positionCount.toFloat(), "${it.positionCount}")
                },
                gold,
            )
            rankingItem(
                "Najbliżej",
                iconDistance,
                uiState.closestNodes.map {
                    ReportBar(it.nodeName, it.distanceMeters.toFloat(), distanceText(it.distanceMeters))
                },
                purple,
            )
            rankingItem(
                "Najwięcej danych (telemetria)",
                iconDataUsage,
                uiState.topTelemetryVolumeSenders.map {
                    ReportBar(it.nodeName, it.packetCount.toFloat(), "${it.packetCount}")
                },
                red,
            )
            rankingItem(
                "Najwięcej danych (wszystkie pakiety)",
                iconDataUsage,
                uiState.topAllPacketVolumeSenders.map {
                    ReportBar(it.nodeName, it.packetCount.toFloat(), "${it.packetCount}")
                },
                red,
            )
            rankingItem(
                "Najdłużej milczą",
                iconNodes,
                uiState.quietestNodes.map { ReportBar(it.nodeName, it.hoursSilent.toFloat(), "${it.hoursSilent} h") },
                purple,
            )
        }
    }
}

private fun LazyListScope.rankingItem(
    title: String,
    icon: ImageVector,
    bars: List<ReportBar>,
    color: Color,
) {
    if (bars.isEmpty()) return
    item { SummarySection(title = title, icon = icon, accent = color) { BarList(bars = bars, color = color) } }
}

private fun oneDecimal(value: Double): String = (round(value * 10) / 10).toString()

/** The top card: a donut of online / offline nodes and the main counts next to it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryHero(uiState: NetworkSummaryUiState) {
    val gold = MaterialTheme.colorScheme.StatusOnline
    val brush =
        Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), MaterialTheme.colorScheme.surfaceContainer),
        )
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(brush).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DonutChart(
                segments =
                listOf(
                    gold to uiState.onlineCount,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = OFFLINE_ALPHA) to
                        (uiState.totalCount - uiState.onlineCount).coerceAtLeast(0),
                ),
                centerText = "${uiState.onlineCount}/${uiState.totalCount}",
                sizeDp = HERO_DONUT_DP,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sieć w liczbach", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "Online: ${uiState.onlineCount} z ${uiState.totalCount} węzłów",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LegendCount(HealthLevel.BAD.color(), "${uiState.attentionItems.size} do uwagi")
                    LegendCount(HealthLevel.MEDIUM.color(), "${uiState.lowBatteryCount} słaba bateria")
                    LegendCount(gold, "${uiState.mainsPoweredCount} z zasilania")
                }
            }
        }
    }
}

private const val HERO_DONUT_DP = 110

@Composable
private fun LegendCount(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        HealthDot(color = color)
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BigNumber(value: String, caption: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.StatusOnline,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EtherGauges(uiState: NetworkSummaryUiState) {
    val values =
        listOf(
            "Średnie zajęcie kanału" to uiState.averageChannelUtilization,
            "Największe zajęcie kanału" to uiState.maxChannelUtilization,
            "Średni czas nadawania" to uiState.averageAirUtilTx,
        )
    if (values.all { it.second == null }) Text("Brak danych", style = MaterialTheme.typography.bodyMedium)
    values.forEach { (label, value) ->
        if (value != null) {
            GaugeRow(
                label = label,
                percentText = MetricFormatter.percent(value, decimalPlaces = 1),
                fraction = value / PERCENT,
                color = etherLevelOf(value).color(),
            )
        }
    }
}

@Composable
private fun TrafficBlock(uiState: NetworkSummaryUiState) {
    val received = uiState.totalPacketsRx
    val good = (received - uiState.totalDupes - uiState.totalBad).coerceAtLeast(0)
    val gold = MaterialTheme.colorScheme.StatusOnline
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DonutChart(
            segments =
            listOf(
                gold to good,
                HealthLevel.MEDIUM.color() to uiState.totalDupes,
                HealthLevel.BAD.color() to uiState.totalBad,
            ),
            centerText = "$received",
            sizeDp = TRAFFIC_DONUT_DP,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LegendCount(gold, "$good poprawne")
            LegendCount(HealthLevel.MEDIUM.color(), "${uiState.totalDupes} duplikaty")
            LegendCount(HealthLevel.BAD.color(), "${uiState.totalBad} uszkodzone")
            Text(
                text = "Wysłane: ${uiState.totalPacketsTx}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    uiState.topRelayNodeName?.let {
        Text(
            text = "Najwięcej przekazał: $it (${uiState.topRelayCount})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val TRAFFIC_DONUT_DP = 96
