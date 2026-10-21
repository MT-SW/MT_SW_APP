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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.model.LinkSignal
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeExtraMode
import org.meshtastic.core.model.NodeExtraStats
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline
import org.meshtastic.core.ui.theme.StatusColors.StatusOrange
import org.meshtastic.core.ui.theme.StatusColors.StatusYellow

/** How the extra line of data under each node is shown; provided by the node list. */
data class NodeExtraConfig(val mode: NodeExtraMode = NodeExtraMode.OFF, val iconsOnly: Boolean = false)

val LocalNodeExtraConfig = staticCompositionLocalOf { NodeExtraConfig() }

/** Newest traffic / resource readings per node number; provided by the node list. */
val LocalNodeExtraStats = compositionLocalOf<Map<Int, NodeExtraStats>> { emptyMap() }

/** Strength of the link to the connected radio (Bluetooth or the device's Wi-Fi); null = nothing to show. */
val LocalLinkSignal = compositionLocalOf<LinkSignal?> { null }

/** One shared ticker for the whole list: every few seconds all visible paged rows move to their next page. */
val LocalNodeExtraTick = compositionLocalOf<State<Int>> { mutableIntStateOf(0) }

/** Time between automatic page changes. */
const val NODE_EXTRA_PAGE_MILLIS = 4_000L

/** After a manual swipe the row stays put for this many ticks. */
private const val MANUAL_PAUSE_TICKS = 3

/** One reading: a symbol, a short label (shown unless icons only) and the value. */
data class ExtraChip(val glyph: String, val label: String, val value: String)

private data class ExtraGroup(val chips: List<ExtraChip>)

/** Symbol -> meaning, for the (i) legend in the settings. */
val nodeExtraLegend: List<Pair<String, String>> =
    listOf(
        "🌡" to "Temperatura",
        "💧" to "Wilgotność",
        "🧭" to "Ciśnienie",
        "🌬" to "Wiatr (prędkość, kierunek)",
        "💨" to "Poryw wiatru",
        "🌧" to "Deszcz (1 godz. / 24 godz.)",
        "☀" to "Światło",
        "😎" to "Promieniowanie UV",
        "☢" to "Promieniowanie",
        "📏" to "Odległość / poziom",
        "⚖" to "Waga",
        "🌱" to "Gleba (temperatura, wilgotność)",
        "🫧" to "Jakość powietrza",
        "👥" to "Licznik osób (BLE / Wi-Fi)",
        "🔋" to "Bateria",
        "🔌" to "Napięcie",
        "⚡" to "Prąd / kanały zasilania",
        "📶" to "Zajętość kanału",
        "📡" to "Czas nadawania",
        "⬆" to "Pakiety wysłane",
        "⬇" to "Pakiety odebrane",
        "⚠" to "Pakiety uszkodzone",
        "♻" to "Duplikaty",
        "🔁" to "Przekazane dalej",
        "🧠" to "Procesor (CPU)",
        "🧮" to "Wolna pamięć",
        "💾" to "Zajęta pamięć flash",
        "🗄" to "Wolna pamięć PSRAM",
        "⏱" to "Czas pracy",
    )

private fun Float?.ok(): Float? = this?.takeIf { !it.isNaN() && !it.isInfinite() }

private fun fmt(value: Float, decimals: Int = 1): String = org.meshtastic.core.common.util.NumberFormatter.format(value, decimals)

private fun weatherChips(node: Node, tempF: Boolean, imperial: Boolean): List<ExtraChip> = buildList {
    val env = node.environmentMetrics
    val aq = node.airQualityMetrics
    val pax = node.paxcounter
    (env.temperature ?: aq.co2_temperature).ok()?.let {
        add(ExtraChip("🌡", "Temperatura", MetricFormatter.temperature(it, tempF)))
    }
    (env.relative_humidity.ok()?.takeIf { it != 0f } ?: aq.co2_humidity.ok()?.takeIf { it != 0f })?.let {
        add(ExtraChip("💧", "Wilgotność", MetricFormatter.humidity(it)))
    }
    env.barometric_pressure.ok()?.takeIf { it != 0f }?.let {
        add(ExtraChip("🧭", "Ciśnienie", MetricFormatter.pressure(it)))
    }
    env.wind_speed.ok()?.let { speed ->
        val dir = env.wind_direction?.let { " ${it}°" } ?: ""
        add(ExtraChip("🌬", "Wiatr", MetricFormatter.windSpeed(speed, imperial) + dir))
    }
    env.wind_gust.ok()?.takeIf { it != 0f }?.let {
        add(ExtraChip("💨", "Poryw", MetricFormatter.windSpeed(it, imperial)))
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
        add(ExtraChip("🌧", "Deszcz", text))
    }
    env.lux.ok()?.let { add(ExtraChip("☀", "Światło", "${fmt(it, 0)} lx")) }
    env.uv_lux.ok()?.let { add(ExtraChip("😎", "UV", fmt(it, 1))) }
    env.radiation.ok()?.let { add(ExtraChip("☢", "Promieniowanie", "${fmt(it, 1)} µR/h")) }
    env.distance.ok()?.let { add(ExtraChip("📏", "Odległość", "${fmt(it, 0)} mm")) }
    env.weight.ok()?.let { add(ExtraChip("⚖", "Waga", MetricFormatter.weight(it, imperial))) }
    env.soil_temperature.ok()?.let {
        add(ExtraChip("🌱", "Gleba", MetricFormatter.temperature(it, tempF)))
    }
    env.soil_moisture?.takeIf { it in 0..100 }?.let { add(ExtraChip("🌱", "Wilg. gleby", MetricFormatter.percent(it))) }
    env.iaq?.takeIf { it != 0 }?.let { add(ExtraChip("🫧", "Jakość powietrza", "$it")) }
    if (pax.ble != 0 || pax.wifi != 0) add(ExtraChip("👥", "Osoby", "B:${pax.ble} W:${pax.wifi}"))
}

private fun powerChips(node: Node): List<ExtraChip> = buildList {
    val env = node.environmentMetrics
    val power = node.powerMetrics
    node.batteryLevel?.takeIf { it > 0 }?.let { level ->
        add(ExtraChip("🔋", "Bateria", if (level > 100) "zasilany" else "$level%"))
    }
    (node.voltage ?: env.voltage).ok()?.takeIf { it != 0f }?.let {
        add(ExtraChip("🔌", "Napięcie", MetricFormatter.voltage(it)))
    }
    env.current.ok()?.let { add(ExtraChip("⚡", "Prąd", MetricFormatter.current(it))) }
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
                add(ExtraChip("⚡", "Kanał $ch", "$ch: $text"))
            }
        }
}

private fun trafficChips(node: Node, stats: NodeExtraStats?): List<ExtraChip> = buildList {
    val dm = node.deviceMetrics
    dm.channel_utilization.ok()?.let { add(ExtraChip("📶", "Zajętość kanału", MetricFormatter.percent(it))) }
    dm.air_util_tx.ok()?.let { add(ExtraChip("📡", "Czas nadawania", MetricFormatter.percent(it))) }
    stats?.packetsTx?.let { add(ExtraChip("⬆", "Wysłane", "$it")) }
    stats?.packetsRx?.let { add(ExtraChip("⬇", "Odebrane", "$it")) }
    stats?.packetsRxBad?.let { add(ExtraChip("⚠", "Uszkodzone", "$it")) }
    stats?.rxDupe?.let { add(ExtraChip("♻", "Duplikaty", "$it")) }
    stats?.txRelay?.let { add(ExtraChip("🔁", "Przekazane", "$it")) }
}

private fun resourceChips(node: Node, stats: NodeExtraStats?): List<ExtraChip> = buildList {
    stats?.cpuPercent?.let { add(ExtraChip("🧠", "CPU", "$it%")) }
    stats?.heapFreePercent?.ok()?.let { add(ExtraChip("🧮", "Wolna pamięć", MetricFormatter.percent(it, 0))) }
    stats?.flashUsedPercent?.ok()?.let { add(ExtraChip("💾", "Flash", MetricFormatter.percent(it, 0))) }
    stats?.psramFreePercent?.ok()?.let { add(ExtraChip("🗄", "PSRAM", MetricFormatter.percent(it, 0))) }
    node.deviceMetrics.uptime_seconds?.takeIf { it > 0 }?.let {
        add(ExtraChip("⏱", "Czas pracy", org.meshtastic.core.resources.formatDuration(it.toLong())))
    }
}

/** The groups that have anything to show, in the order weather, power, traffic, resources. */
private fun buildGroups(node: Node, stats: NodeExtraStats?, tempF: Boolean, imperial: Boolean): List<ExtraGroup> =
    listOf(
        weatherChips(node, tempF, imperial),
        powerChips(node),
        trafficChips(node, stats),
        resourceChips(node, stats),
    )
        .filter { it.isNotEmpty() }
        .map { ExtraGroup(it) }

/**
 * The extra line of data under a node: weather sensors, power, traffic and resources. Only the newest readings are
 * shown. In paged mode a page is one theme (e.g. weather) or a part of it; pages change by themselves on the shared
 * ticker and by swiping left / right (a manual swipe pauses the automatic change for a moment). A theme with no data is
 * skipped, so a node with no sensors still shows its traffic and resources.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NodeExtraLine(node: Node, tempInFahrenheit: Boolean, distanceUnits: MeasurementSystem, modifier: Modifier = Modifier) {
    val config = LocalNodeExtraConfig.current
    if (config.mode == NodeExtraMode.OFF) return
    val stats = LocalNodeExtraStats.current[node.num]
    val imperial = distanceUnits != MeasurementSystem.METRIC
    val groups =
        remember(node, stats, tempInFahrenheit, imperial) { buildGroups(node, stats, tempInFahrenheit, imperial) }
    if (groups.isEmpty()) return

    if (config.mode == NodeExtraMode.ALL) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            groups.forEach { g -> g.chips.forEach { ExtraChipView(it, config.iconsOnly) } }
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val perPage = pageCapacity(maxWidth, config.iconsOnly)
        val pages = remember(groups, perPage) { groups.flatMap { it.chips.chunked(perPage) } }
        PagedRow(pages = pages, iconsOnly = config.iconsOnly)
    }
}

private fun pageCapacity(width: Dp, iconsOnly: Boolean): Int {
    val perChip = if (iconsOnly) 78.dp else 140.dp
    return (width.value / perChip.value).toInt().coerceIn(1, 8)
}

@Composable
private fun PagedRow(pages: List<List<ExtraChip>>, iconsOnly: Boolean) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                pages.getOrNull(index)?.forEach { ExtraChipView(it, iconsOnly) }
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = chip.glyph, style = MaterialTheme.typography.labelMedium)
        Text(
            text = chip.value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!iconsOnly) {
            Text(
                text = chip.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The link strength in dBm, in a colour that follows how good it is; shows nothing when there is no reading. */
@Composable
fun LinkSignalLabel(modifier: Modifier = Modifier) {
    val signal = LocalLinkSignal.current ?: return
    val color =
        when {
            signal.dbm >= -70 -> MaterialTheme.colorScheme.StatusOnline
            signal.dbm >= -85 -> MaterialTheme.colorScheme.StatusYellow
            else -> MaterialTheme.colorScheme.StatusOrange
        }
    Text(
        text = "${signal.dbm} dBm",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        modifier = modifier,
    )
}
