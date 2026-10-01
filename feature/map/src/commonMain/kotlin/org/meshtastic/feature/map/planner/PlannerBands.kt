/*
 * Copyright (c) 2025 MT_SW contributors
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
package org.meshtastic.feature.map.planner

import org.meshtastic.core.model.ChannelOption
import org.meshtastic.proto.Config.LoRaConfig.ModemPreset

/**
 * A selectable frequency band of the planner. [id] is a stable identifier (also usable as a storage key); the UI
 * shows [name] (already language neutral: MHz figures and ISM/EU tags) except for the [PlannerBands.FREE_ID] band whose
 * label should be localized by the UI.
 */
data class PlannerBand(val id: String, val name: String, val centerMHz: Double, val minMHz: Double, val maxMHz: Double)

/** LoRa modem parameters the link budget needs. */
data class PlannerRadio(val bandwidthKhz: Double, val spreadingFactor: Int)

/** Band list of the planner plus lookup helpers. */
object PlannerBands {
    const val FREE_ID = "free"
    const val FREE_MIN_MHZ = 20.0
    const val FREE_MAX_MHZ = 20000.0
    const val DEFAULT_ID = "868"

    val all: List<PlannerBand> =
        listOf(
            PlannerBand("169", "169 MHz", 169.4, 160.0, 180.0),
            PlannerBand("433", "433 MHz (ISM)", 433.5, 430.0, 440.0),
            PlannerBand("470", "470 MHz", 490.0, 470.0, 510.0),
            PlannerBand("868", "868 MHz (EU)", 869.14465, 863.0, 870.0),
            PlannerBand("915", "915 MHz", 915.0, 902.0, 922.0),
            PlannerBand("923", "923 MHz", 923.0, 922.0, 930.0),
            PlannerBand("2400", "2400 MHz", 2440.0, 2400.0, 2500.0),
            PlannerBand(FREE_ID, "20-20000 MHz", 868.0, FREE_MIN_MHZ, FREE_MAX_MHZ),
        )

    fun byId(id: String): PlannerBand? = all.firstOrNull { it.id == id }

    /** Id of the first fixed band containing [mHz], otherwise [FREE_ID]. */
    fun idFor(mHz: Double): String =
        all.firstOrNull { it.id != FREE_ID && mHz >= it.minMHz && mHz <= it.maxMHz }?.id ?: FREE_ID

    /** Clamps to the supported 20..20000 MHz range (ITM validity). */
    fun clampMHz(mHz: Double): Double = if (mHz.isNaN()) 868.0 else mHz.coerceIn(FREE_MIN_MHZ, FREE_MAX_MHZ)
}

/**
 * Bandwidth (kHz) and spreading factor of an app [ModemPreset] (LITE_*, NARROW_*, TINY_* included; they come from
 * [ChannelOption]). An unknown or null preset maps to the app default (LongFast: 250 kHz / SF11).
 */
fun plannerRadioFromPreset(preset: ModemPreset?): PlannerRadio {
    val option = ChannelOption.from(preset) ?: ChannelOption.DEFAULT
    return PlannerRadio(option.bandwidth.toDouble() * 1000.0, option.spreadingFactor)
}
