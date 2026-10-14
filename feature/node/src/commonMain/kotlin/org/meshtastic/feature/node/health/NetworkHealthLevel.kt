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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.network_health_info_ether
import org.meshtastic.core.resources.network_health_info_neighbors
import org.meshtastic.core.resources.network_health_info_none
import org.meshtastic.core.resources.network_health_info_power
import org.meshtastic.core.resources.network_health_info_signal
import org.meshtastic.core.resources.network_health_level_bad
import org.meshtastic.core.resources.network_health_level_critical
import org.meshtastic.core.resources.network_health_level_good
import org.meshtastic.core.resources.network_health_level_medium
import org.meshtastic.core.resources.network_health_level_unknown
import org.meshtastic.core.ui.theme.StatusColors.StatusConnecting
import org.meshtastic.core.ui.theme.StatusColors.StatusDisconnected
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline
import org.meshtastic.core.ui.theme.StatusColors.StatusPurple

/**
 * How one node is doing in one metric tab. The order is from best to worst; [UNKNOWN] comes last and means "no verdict"
 * (no data, or a metric without a single good/bad limit).
 */
enum class HealthLevel {
    GOOD,
    MEDIUM,
    BAD,
    CRITICAL,
    UNKNOWN,
    ;

    /** True for the two worst verdicts: what the "problems only" filter keeps. */
    val isProblem: Boolean
        get() = this == BAD || this == CRITICAL
}

/** The worst verdict in [levels], ignoring [HealthLevel.UNKNOWN]; [HealthLevel.UNKNOWN] when nothing has a verdict. */
internal fun worstLevel(levels: Iterable<HealthLevel>): HealthLevel =
    levels.filter { it != HealthLevel.UNKNOWN }.maxByOrNull { it.ordinal } ?: HealthLevel.UNKNOWN

// Battery, in percent. Anything above 100 is how the firmware reports external power.
private const val BATTERY_GOOD = 50
private const val BATTERY_MEDIUM = 20
private const val BATTERY_BAD = 5

// SNR of the last direct reception, in dB.
private const val SNR_GOOD = 0f
private const val SNR_MEDIUM = -7.5f
private const val SNR_BAD = -15f

// Channel utilization, in percent.
private const val ETHER_GOOD = 15f
private const val ETHER_MEDIUM = 25f
private const val ETHER_BAD = 40f

// Number of neighbors a node reports hearing.
private const val NEIGHBORS_GOOD = 3
private const val NEIGHBORS_MEDIUM = 2

/** This node's verdict in this metric tab; [HealthLevel.UNKNOWN] for tabs without a good/bad limit. */
internal fun NetworkHealthMetric.levelFor(node: NodeHealthInfo): HealthLevel = when (this) {
    NetworkHealthMetric.POWER -> powerLevel(node)
    NetworkHealthMetric.SIGNAL -> node.snr?.let(::snrLevel) ?: HealthLevel.UNKNOWN
    NetworkHealthMetric.ETHER -> node.channelUtilization?.let(::etherLevel) ?: HealthLevel.UNKNOWN
    NetworkHealthMetric.NEIGHBORS -> node.neighborCount?.let(::neighborLevel) ?: HealthLevel.UNKNOWN
    NetworkHealthMetric.ENVIRONMENT,
    NetworkHealthMetric.RESOURCES,
    NetworkHealthMetric.TRAFFIC,
    -> HealthLevel.UNKNOWN
}

private fun powerLevel(node: NodeHealthInfo): HealthLevel {
    if (node.isPowered) return HealthLevel.GOOD
    val battery = node.batteryLevel ?: return HealthLevel.UNKNOWN
    return when {
        battery > 100 || battery >= BATTERY_GOOD -> HealthLevel.GOOD
        battery >= BATTERY_MEDIUM -> HealthLevel.MEDIUM
        battery >= BATTERY_BAD -> HealthLevel.BAD
        else -> HealthLevel.CRITICAL
    }
}

internal fun snrLevel(snr: Float): HealthLevel = when {
    snr >= SNR_GOOD -> HealthLevel.GOOD
    snr >= SNR_MEDIUM -> HealthLevel.MEDIUM
    snr >= SNR_BAD -> HealthLevel.BAD
    else -> HealthLevel.CRITICAL
}

internal fun etherLevel(utilization: Float): HealthLevel = when {
    utilization < ETHER_GOOD -> HealthLevel.GOOD
    utilization < ETHER_MEDIUM -> HealthLevel.MEDIUM
    utilization < ETHER_BAD -> HealthLevel.BAD
    else -> HealthLevel.CRITICAL
}

private fun neighborLevel(count: Int): HealthLevel = when {
    count >= NEIGHBORS_GOOD -> HealthLevel.GOOD
    count == NEIGHBORS_MEDIUM -> HealthLevel.MEDIUM
    count == 1 -> HealthLevel.BAD
    else -> HealthLevel.CRITICAL
}

/**
 * Fill colour of this level: the palette the signal-quality indicator already uses — gold, red, purple, and white on a
 * dark theme / black on a light one (where white would be invisible).
 */
@Composable
internal fun HealthLevel.color(): Color = when (this) {
    HealthLevel.GOOD -> MaterialTheme.colorScheme.StatusOnline
    HealthLevel.MEDIUM -> MaterialTheme.colorScheme.StatusDisconnected
    HealthLevel.BAD -> MaterialTheme.colorScheme.StatusPurple
    HealthLevel.CRITICAL -> MaterialTheme.colorScheme.StatusConnecting
    HealthLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** The word shown next to the colour, so colour is never the only signal. */
internal val HealthLevel.labelRes: StringResource
    get() = when (this) {
        HealthLevel.GOOD -> Res.string.network_health_level_good
        HealthLevel.MEDIUM -> Res.string.network_health_level_medium
        HealthLevel.BAD -> Res.string.network_health_level_bad
        HealthLevel.CRITICAL -> Res.string.network_health_level_critical
        HealthLevel.UNKNOWN -> Res.string.network_health_level_unknown
    }

/** The explanation of this tab's colour limits, shown in the info dialog. */
internal val NetworkHealthMetric.infoRes: StringResource
    get() = when (this) {
        NetworkHealthMetric.POWER -> Res.string.network_health_info_power
        NetworkHealthMetric.SIGNAL -> Res.string.network_health_info_signal
        NetworkHealthMetric.ETHER -> Res.string.network_health_info_ether
        NetworkHealthMetric.NEIGHBORS -> Res.string.network_health_info_neighbors
        NetworkHealthMetric.ENVIRONMENT,
        NetworkHealthMetric.RESOURCES,
        NetworkHealthMetric.TRAFFIC,
        -> Res.string.network_health_info_none
    }
