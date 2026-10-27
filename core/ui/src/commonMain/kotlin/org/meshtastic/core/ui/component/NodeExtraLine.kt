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
@file:Suppress("MagicNumber")

package org.meshtastic.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeExtraMode
import org.meshtastic.core.model.NodeExtraStats
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.AirQuality
import org.meshtastic.core.ui.icon.AirUtilization
import org.meshtastic.core.ui.icon.ChannelUtilization
import org.meshtastic.core.ui.icon.DataUsage
import org.meshtastic.core.ui.icon.DetectionSensor
import org.meshtastic.core.ui.icon.Distance
import org.meshtastic.core.ui.icon.ElectricPower
import org.meshtastic.core.ui.icon.Humidity
import org.meshtastic.core.ui.icon.LightMode
import org.meshtastic.core.ui.icon.Memory
import org.meshtastic.core.ui.icon.PeopleCount
import org.meshtastic.core.ui.icon.Pressure
import org.meshtastic.core.ui.icon.SoilMoisture
import org.meshtastic.core.ui.icon.SoilTemperature
import org.meshtastic.core.ui.icon.Snr
import org.meshtastic.core.ui.icon.Speed
import org.meshtastic.core.ui.icon.Storage
import org.meshtastic.core.ui.icon.Temperature
import org.meshtastic.core.ui.icon.Tsunami
import org.meshtastic.core.ui.icon.Voltage
import org.meshtastic.core.ui.icon.Wifi
import org.meshtastic.core.ui.util.LocalLnaCorrection
import org.meshtastic.core.ui.icon.Weight
import org.meshtastic.core.ui.icon.WindDirection

/**
 * How the extra line of data under each node is shown; provided by the node list. [packets] and [hardware] switch the
 * packet counters and the hardware data on; sensors (power and weather) follow the environment metrics switch.
 */
data class NodeExtraConfig(
    val mode: NodeExtraMode = NodeExtraMode.ALL,
    val iconsOnly: Boolean = false,
    val packets: Boolean = false,
    val hardware: Boolean = false,
    val ether: Boolean = false,
    /** Shows the neighbours icon with the count from the last saved neighbour list. */
    val neighbors: Boolean = true,
) {
    /** True when the statistics shown in the extra line itself (beyond the sensors) are switched on. */
    val anyStats: Boolean
        get() = packets || hardware
}

val LocalNodeExtraConfig = staticCompositionLocalOf { NodeExtraConfig() }

/** Newest traffic / hardware readings per node number; provided by the node list. */
val LocalNodeExtraStats = compositionLocalOf<Map<Int, NodeExtraStats>> { emptyMap() }

/** One shared ticker for the whole list: every few seconds all visible paged rows move to their next page. */
val LocalNodeExtraTick = compositionLocalOf<State<Int>> { mutableIntStateOf(0) }

/** Time between automatic page changes. */
const val NODE_EXTRA_PAGE_MILLIS = 4_000L

/** After a manual swipe the row stays put for this many ticks. */
private const val MANUAL_PAUSE_TICKS = 3

/** One reading: an icon in the app's own grey style, a short label (hidden when icons only) and the value. */
data class ExtraChip(val icon: (@Composable () -> ImageVector)?, val label: String, val value: String)

/** A themed run of readings; packet and hardware groups are pushed to the right edge of the card. */
private data class ExtraGroup(val chips: List<ExtraChip>, val rightAligned: Boolean)

private data class ExtraPage(val chips: List<ExtraChip>, val rightAligned: Boolean)

private fun legend(icon: (@Composable () -> ImageVector)?, text: String) = icon to text

/** Icon -> meaning, for the (i) legend in the settings. */
val nodeExtraLegend: List<Pair<(@Composable () -> ImageVector)?, String>> =
        listOf(
            legend({ MeshtasticIcons.Temperature }, "Temperatura"),
            legend({ MeshtasticIcons.Humidity }, "Wilgotność"),
            legend({ MeshtasticIcons.Pressure }, "Ciśnienie"),
            legend({ MeshtasticIcons.WindDirection }, "Wiatr (prędkość, kierunek)"),
            legend({ MeshtasticIcons.Speed }, "Poryw wiatru"),
            legend({ MeshtasticIcons.Tsunami }, "Deszcz (1 godz. / 24 godz.)"),
            legend({ MeshtasticIcons.LightMode }, "Światło / promieniowanie UV"),
            legend({ MeshtasticIcons.DetectionSensor }, "Promieniowanie"),
            legend({ MeshtasticIcons.Distance }, "Odległość / poziom"),
            legend({ MeshtasticIcons.Weight }, "Waga"),
            legend({ MeshtasticIcons.SoilTemperature }, "Gleba (temperatura)"),
            legend({ MeshtasticIcons.SoilMoisture }, "Gleba (wilgotność)"),
            legend({ MeshtasticIcons.AirQuality }, "Jakość powietrza"),
            legend({ MeshtasticIcons.PeopleCount }, "Licznik osób (BLE / Wi-Fi)"),
            legend({ MeshtasticIcons.Voltage }, "Napięcie"),
            legend({ MeshtasticIcons.ElectricPower }, "Prąd / kanały zasilania"),
            legend({ MeshtasticIcons.ChannelUtilization }, "Zajętość kanału"),
            legend({ MeshtasticIcons.AirUtilization }, "Czas nadawania"),
            legend({ MeshtasticIcons.Snr }, "Szum tła (poziom szumu na kanale)"),
            legend(null, "TX: pakiety wysłane"),
            legend(null, "RX: pakiety odebrane"),
            legend(null, "B: pakiety błędne (uszkodzone)"),
            legend(null, "D: duplikaty"),
            legend(null, "Przek.: pakiety przekazane dalej"),
            legend({ MeshtasticIcons.Wifi }, "Siła sygnału Wi-Fi węzła (z jego pakietu)"),
            legend({ MeshtasticIcons.Memory }, "Procesor (CPU)"),
            legend({ MeshtasticIcons.DataUsage }, "Wolna pamięć"),
            legend({ MeshtasticIcons.Storage }, "Zajęta pamięć flash / PSRAM"),
        )

private fun Float?.ok(): Float? = this?.takeIf { !it.isNaN() && !it.isInfinite() }

private fun fmt(value: Float, decimals: Int = 1): String =
    org.meshtastic.core.common.util.NumberFormatter.format(value, decimals)

private fun weatherChips(node: Node, tempF: Boolean, imperial: Boolean): List<ExtraChip> = buildList {
    val env = node.environmentMetrics
    val aq = node.airQualityMetrics
    val pax = node.paxcounter
    (env.temperature ?: aq.co2_temperature).ok()?.let {
        add(ExtraChip({ MeshtasticIcons.Temperature }, "Temp.", MetricFormatter.temperature(it, tempF)))
    }
    (env.relative_humidity.ok()?.takeIf { it != 0f } ?: aq.co2_humidity.ok()?.takeIf { it != 0f })?.let {
        add(ExtraChip({ MeshtasticIcons.Humidity }, "Wilg.", MetricFormatter.humidity(it)))
    }
    env.barometric_pressure.ok()?.takeIf { it != 0f }?.let {
        add(ExtraChip({ MeshtasticIcons.Pressure }, "Baro", MetricFormatter.pressure(it)))
    }
    env.wind_speed.ok()?.let { speed ->
        val dir = env.wind_direction?.let { " ${it}°" } ?: ""
        add(ExtraChip({ MeshtasticIcons.WindDirection }, "Wiatr", MetricFormatter.windSpeed(speed, imperial) + dir))
    }
    env.wind_gust.ok()?.takeIf { it != 0f }?.let {
        add(ExtraChip({ MeshtasticIcons.Speed }, "Poryw", MetricFormatter.windSpeed(it, imperial)))
    }
    val rain1 = env.rainfall_1h.ok()
    val rain24 = env.rainfall_24h.ok()
    if (rain1 != null || rain24 != null) {
        val text =
            listOfNotNull(
                rain1?.let { MetricFormatter.rainfall(it, imperial) },
                rain24?.let { MetricFormatter.rainfall(it, imperial) },
            )
                .joinToString(" / ")
        add(ExtraChip({ MeshtasticIcons.Tsunami }, "Deszcz", text))
    }
    env.lux.ok()?.let { add(ExtraChip({ MeshtasticIcons.LightMode }, "Światło", "${fmt(it, 0)} lx")) }
    env.uv_lux.ok()?.let { add(ExtraChip({ MeshtasticIcons.LightMode }, "UV", fmt(it, 1))) }
    env.radiation.ok()?.let { add(ExtraChip({ MeshtasticIcons.DetectionSensor }, "Promien.", "${fmt(it, 1)} µR/h")) }
    env.distance.ok()?.let { add(ExtraChip({ MeshtasticIcons.Distance }, "Odl.", "${fmt(it, 0)} mm")) }
    env.weight.ok()?.let { add(ExtraChip({ MeshtasticIcons.Weight }, "Waga", MetricFormatter.weight(it, imperial))) }
    env.soil_temperature.ok()?.let {
        add(ExtraChip({ MeshtasticIcons.SoilTemperature }, "Gleba", MetricFormatter.temperature(it, tempF)))
    }
    env.soil_moisture?.takeIf { it in 0..100 }?.let {
        add(ExtraChip({ MeshtasticIcons.SoilMoisture }, "Wilg. gl.", MetricFormatter.percent(it)))
    }
    env.iaq?.takeIf { it != 0 }?.let { add(ExtraChip({ MeshtasticIcons.AirQuality }, "IAQ", "$it")) }
    if (pax.ble != 0 || pax.wifi != 0) {
        add(ExtraChip({ MeshtasticIcons.PeopleCount }, "Osoby", "B:${pax.ble} W:${pax.wifi}"))
    }
}

// The card already shows battery / "powered", its voltage, uptime and (for our own node) channel and air
// utilization, so none of those are repeated here.
private fun powerChips(node: Node): List<ExtraChip> = buildList {
    val env = node.environmentMetrics
    val power = node.powerMetrics
    // Only the voltage of an external sensor (environment or power telemetry) is shown here. The basic device
    // voltage belongs to the battery percentage and is not repeated.
    env.voltage.ok()?.takeIf { it != 0f }?.let {
        add(ExtraChip({ MeshtasticIcons.Voltage }, "Nap.", MetricFormatter.voltage(it)))
    }
    env.current.ok()?.let { add(ExtraChip({ MeshtasticIcons.ElectricPower }, "Prąd", MetricFormatter.current(it))) }
    listOf(
        Triple(1, power.ch1_voltage.ok(), power.ch1_current.ok()),
        Triple(2, power.ch2_voltage.ok(), power.ch2_current.ok()),
        Triple(3, power.ch3_voltage.ok(), power.ch3_current.ok()),
    )
        .forEach { (ch, v, c) ->
            if (v != null || c != null) {
                val text =
                    listOfNotNull(v?.let { MetricFormatter.voltage(it) }, c?.let { MetricFormatter.current(it) })
                        .joinToString(" ")
                add(ExtraChip({ MeshtasticIcons.ElectricPower }, "Kan. $ch", text))
            }
        }
}

private fun etherChips(node: Node, noiseFloor: Int?, showUtilization: Boolean): List<ExtraChip> = buildList {
    // The full card of our own node already shows ChUtil and AirUtil in its signal row; the compact card does not.
    if (showUtilization) {
        val dm = node.deviceMetrics
        dm.channel_utilization.ok()?.let {
            add(ExtraChip({ MeshtasticIcons.ChannelUtilization }, "ChUtil", MetricFormatter.percent(it)))
        }
        dm.air_util_tx.ok()?.let {
            add(ExtraChip({ MeshtasticIcons.AirUtilization }, "AirUtil", MetricFormatter.percent(it)))
        }
    }
    noiseFloor?.let { add(ExtraChip({ MeshtasticIcons.Snr }, "Szum", "$it dBm")) }
}

private fun trafficChips(stats: NodeExtraStats?): List<ExtraChip> = buildList {
    stats?.packetsTx?.let { add(ExtraChip(null, "TX", "$it")) }
    stats?.packetsRx?.let { add(ExtraChip(null, "RX", "$it")) }
    stats?.packetsRxBad?.let { add(ExtraChip(null, "B", "$it")) }
    stats?.rxDupe?.let { add(ExtraChip(null, "D", "$it")) }
    stats?.txRelay?.let { add(ExtraChip(null, "Przek.", "$it")) }
}

private fun resourceChips(stats: NodeExtraStats?): List<ExtraChip> = buildList {
    stats?.cpuPercent?.let { add(ExtraChip({ MeshtasticIcons.Memory }, "CPU", "$it%")) }
    stats?.heapUsedPercent?.ok()?.let {
        add(ExtraChip({ MeshtasticIcons.DataUsage }, "RAM", MetricFormatter.percent(it, 0)))
    }
    stats?.flashUsedPercent?.ok()?.let {
        add(ExtraChip({ MeshtasticIcons.Storage }, "Flash", MetricFormatter.percent(it, 0)))
    }
    stats?.psramUsedPercent?.ok()?.let {
        add(ExtraChip({ MeshtasticIcons.Storage }, "PSRAM", MetricFormatter.percent(it, 0)))
    }
    // Shown exactly as the node sent it in its last packet.
    stats?.wifiRssi?.let { add(ExtraChip({ MeshtasticIcons.Wifi }, "Wi-Fi", "$it dBm")) }
}

/**
 * The groups that have anything to show, top to bottom: sensors (power first, then weather), packets, hardware. The
 * sensors follow the environment metrics switch, the other two have their own switches. The ether readings are not
 * here: they go into the hops row of the card ([etherInlineItems]).
 */
private fun buildGroups(
    node: Node,
    stats: NodeExtraStats?,
    config: NodeExtraConfig,
    showEnvironment: Boolean,
    tempF: Boolean,
    imperial: Boolean,
    isThisNode: Boolean,
): List<ExtraGroup> = buildList {
    if (showEnvironment) add(ExtraGroup(powerChips(node) + weatherChips(node, tempF, imperial), rightAligned = false))
    if (config.packets) add(ExtraGroup(trafficChips(stats), rightAligned = true))
    if (config.hardware) add(ExtraGroup(resourceChips(stats), rightAligned = true))
}
    .filter { it.chips.isNotEmpty() }

/**
 * The extra line of data under a node: weather sensors, power, traffic and resources. Only the newest readings are
 * shown. In paged mode a page is one theme (e.g. weather) or a part of it; pages change by themselves on the shared
 * ticker and by swiping left / right (a manual swipe pauses the automatic change for a moment). A theme with no data is
 * skipped, so a node with no sensors still shows its traffic and resources.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NodeExtraLine(
    node: Node,
    tempInFahrenheit: Boolean,
    distanceUnits: MeasurementSystem,
    isThisNode: Boolean,
    showEnvironment: Boolean,
    modifier: Modifier = Modifier,
) {
    val config = LocalNodeExtraConfig.current
    val stats = LocalNodeExtraStats.current[node.num]
    val imperial = distanceUnits != MeasurementSystem.METRIC
    val groups =
        remember(node, stats, config, showEnvironment, tempInFahrenheit, imperial, isThisNode) {
            buildGroups(node, stats, config, showEnvironment, tempInFahrenheit, imperial, isThisNode)
        }
    if (groups.isEmpty()) return

    if (config.mode == NodeExtraMode.ALL) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.forEach { g ->
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                    if (g.rightAligned) Arrangement.spacedBy(10.dp, Alignment.End) else Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    g.chips.forEach { ExtraChipView(it, config.iconsOnly) }
                }
            }
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val perPage = pageCapacity(maxWidth, config.iconsOnly)
        val pages =
            remember(groups, perPage) {
                groups.flatMap { g -> g.chips.chunked(perPage).map { ExtraPage(it, g.rightAligned) } }
            }
        PagedRow(pages = pages, iconsOnly = config.iconsOnly)
    }
}

private fun pageCapacity(width: Dp, iconsOnly: Boolean): Int {
    val perChip = if (iconsOnly) 78.dp else 112.dp
    return (width.value / perChip.value).toInt().coerceIn(1, 8)
}

@Composable
private fun PagedRow(pages: List<ExtraPage>, iconsOnly: Boolean) {
    val tickState = LocalNodeExtraTick.current
    val tick by tickState
    val pagerState = rememberPagerState { pages.size }
    var autoScrolling by remember { mutableStateOf(false) }
    var pausedUntil by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick, pages.size) {
        if (pages.size > 1 && tick >= pausedUntil && !pagerState.isScrollInProgress) {
            autoScrolling = true
            try {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % pages.size)
            } finally {
                autoScrolling = false
            }
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .collect { scrolling -> if (scrolling && !autoScrolling) pausedUntil = tickState.value + MANUAL_PAUSE_TICKS }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = pages.getOrNull(index)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                if (page?.rightAligned == true) Arrangement.spacedBy(12.dp, Alignment.End) else Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                page?.chips?.forEach { ExtraChipView(it, iconsOnly) }
            }
        }
        if (pages.size > 1) {
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                pages.indices.forEach { i ->
                    Box(
                        Modifier.size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == pagerState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtraChipView(chip: ExtraChip, iconsOnly: Boolean) {
    if (chip.icon != null) {
        // The same grey icon + small label + value as the rest of the card.
        IconInfo(
            icon = chip.icon.invoke(),
            contentDescription = chip.label,
            label = if (iconsOnly) null else chip.label,
            text = chip.value,
        )
    } else {
        // Counters have no icon: a short "RX:" in front of the number, in the same style.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "${chip.label}:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = chip.value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun chipItem(chip: ExtraChip, iconsOnly: Boolean): @Composable () -> Unit = { ExtraChipView(chip, iconsOnly) }

/**
 * Ether readings (channel utilization, air time, noise floor) for a node card. They carry a short label, unless the
 * "same icons" option is on or [forceIconsOnly] is set (when they share the hops line). Empty when the "Ether" option is
 * off. On the full card our own node already shows channel utilization and air time in its signal row, so it gets only
 * the noise floor there ([compactCard] has no such row and gets all three).
 */
@Composable
fun etherInlineItems(
    node: Node,
    isThisNode: Boolean,
    forceIconsOnly: Boolean,
    compactCard: Boolean = false,
): List<@Composable () -> Unit> {
    if (!LocalNodeExtraConfig.current.ether) return emptyList()
    val stats = LocalNodeExtraStats.current[node.num]
    // The noise floor is shown with the node's LNA correction, like everywhere else in the app.
    val lna = LocalLnaCorrection.current
    val noise = stats?.noiseFloor?.let { lna.noiseFloor(node.num, it) }
    val iconsOnly = forceIconsOnly || LocalNodeExtraConfig.current.iconsOnly
    return etherChips(node, noise, showUtilization = !isThisNode || compactCard).map { chipItem(it, iconsOnly) }
}
