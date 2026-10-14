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
package org.meshtastic.feature.node.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.model.NodeMetricsHistoryEntry
import org.meshtastic.core.model.swapBatteryPolarity
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.battery_invert
import org.meshtastic.core.resources.battery_invert_description
import org.meshtastic.core.resources.current_invert
import org.meshtastic.core.resources.current_invert_description
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.proto.Telemetry

private fun Float?.flipped(): Float? = this?.let { if (it == 0f) it else -it }

/**
 * Returns this telemetry with the sign of every current reading flipped when [invert] is true. Used for sensors wired
 * backwards, so the charge current shows as positive. Display only: voltage, battery level and every other field are
 * left exactly as received, so charging detection is unaffected.
 */
internal fun Telemetry.withCurrentSign(invert: Boolean): Telemetry {
    if (!invert) return this
    val env = environment_metrics
    val power = power_metrics
    return newBuilder()
        .also { wb ->
            if (env?.current != null) {
                wb.environment_metrics = env.newBuilder().also { it.current = env.current.flipped() }.build()
            }
            if (power != null) {
                wb.power_metrics =
                    power
                        .newBuilder()
                        .also {
                            it.ch1_current = power.ch1_current.flipped()
                            it.ch2_current = power.ch2_current.flipped()
                            it.ch3_current = power.ch3_current.flipped()
                            it.ch4_current = power.ch4_current.flipped()
                            it.ch5_current = power.ch5_current.flipped()
                            it.ch6_current = power.ch6_current.flipped()
                            it.ch7_current = power.ch7_current.flipped()
                            it.ch8_current = power.ch8_current.flipped()
                        }
                        .build()
            }
        }
        .build()
}

internal fun List<Telemetry>.withCurrentSign(invert: Boolean): List<Telemetry> =
    if (invert) map { it.withCurrentSign(true) } else this

/** Same flip for a stored history entry (Network Health and its detail charts). */
internal fun NodeMetricsHistoryEntry.withCurrentSign(invert: Boolean): NodeMetricsHistoryEntry =
    if (invert && current != null) copy(current = current.flipped()) else this

/** Per-node switch that flips the sign of the displayed current (sensor wired backwards). Display only. */
@Composable
internal fun CurrentPolaritySwitch(nodeNum: Int, modifier: Modifier = Modifier) {
    val uiPrefs = koinInject<UiPrefs>()
    val inverted by uiPrefs.currentInverted.collectAsStateWithLifecycle()
    SwitchPreference(
        modifier = modifier,
        title = stringResource(Res.string.current_invert),
        summary = stringResource(Res.string.current_invert_description),
        checked = nodeNum in inverted,
        enabled = true,
        onCheckedChange = { uiPrefs.setCurrentInverted(nodeNum, it) },
    )
}

/** Same swap of the powered/battery indication for a history entry (see [swapBatteryPolarity]). */
internal fun NodeMetricsHistoryEntry.withBatterySwap(swap: Boolean): NodeMetricsHistoryEntry =
    if (swap) copy(batteryLevel = swapBatteryPolarity(batteryLevel, deviceVoltage)) else this

/** Telemetry with the powered/battery indication swapped (see [swapBatteryPolarity]); display only. */
internal fun Telemetry.withBatterySwap(swap: Boolean): Telemetry {
    val device = device_metrics
    if (!swap || device == null) return this
    return newBuilder()
        .also { wb ->
            wb.device_metrics =
                device
                    .newBuilder()
                    .also { it.battery_level = swapBatteryPolarity(device.battery_level, device.voltage) }
                    .build()
        }
        .build()
}

internal fun List<Telemetry>.withBatterySwap(swap: Boolean): List<Telemetry> =
    if (swap) map { it.withBatterySwap(true) } else this

/** Per-node switch that swaps "powered" and "battery percent" (power monitor wired backwards). Display only. */
@Composable
internal fun BatteryPolaritySwitch(nodeNum: Int, modifier: Modifier = Modifier) {
    val uiPrefs = koinInject<UiPrefs>()
    val inverted by uiPrefs.batteryInverted.collectAsStateWithLifecycle()
    SwitchPreference(
        modifier = modifier,
        title = stringResource(Res.string.battery_invert),
        summary = stringResource(Res.string.battery_invert_description),
        checked = nodeNum in inverted,
        enabled = true,
        onCheckedChange = { uiPrefs.setBatteryInverted(nodeNum, it) },
    )
}
