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
import org.meshtastic.core.model.RegionInfo
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
            PlannerBand("868", "868 MHz (EU)", 869.44165, 863.0, 870.0),
            PlannerBand("915", "915 MHz", 915.0, 902.0, 922.0),
            PlannerBand("923", "923 MHz", 923.0, 922.0, 930.0),
            PlannerBand("2400", "2400 MHz", 2440.0, 2400.0, 2500.0),
            PlannerBand(FREE_ID, "20-20000 MHz", 868.0, FREE_MIN_MHZ, FREE_MAX_MHZ),
        )

    fun byId(id: String): PlannerBand? = all.firstOrNull { it.id == id }

    /** Id of the first fixed band containing [mHz], otherwise [FREE_ID]. */
    fun idFor(mHz: Double): String =
        all.firstOrNull { it.id != FREE_ID && mHz >= it.minMHz && mHz <= it.maxMHz }?.id ?: FREE_ID

    /**
     * Default operating frequency (MHz) for [bandId] with a channel of [bandwidthKhz] (the effective one, already
     * scaled for 2.4 GHz). In the 868 MHz band the Narrow presets (62.5 kHz) use the MT_SW frequency 869.44165 MHz;
     * every other bandwidth uses the first slot of the EU 868 sub-band (869.4 MHz + half the bandwidth, e.g.
     * 869.525 MHz for 250 kHz). 433, 470 and 2400 MHz use the first slot of their region; other bands their centre.
     */
    fun defaultFrequencyMHz(bandId: String, bandwidthKhz: Double): Double {
        val band = byId(bandId) ?: return 868.0
        val region = regionFor(band.id, bandwidthKhz) ?: return band.centerMHz
        // First slot of the region, exactly as the radio computes it: band start + padding + half the bandwidth.
        // Float -> text -> Double avoids float noise such as 869.4000244 for 869.4f.
        return region.freqStart.toString().toDouble() + region.padding.toString().toDouble() + bandwidthKhz / 2000.0
    }

    /**
     * The app's [RegionInfo] a band stands for (band start, slot padding, wide LoRa), or null when there is no
     * matching region (169 and 923 MHz keep their centre). Narrow presets in 868 MHz use the narrow EU region.
     */
    fun regionFor(bandId: String, bandwidthKhz: Double): RegionInfo? = when (bandId) {
        "433" -> RegionInfo.EU_433
        "470" -> RegionInfo.CN
        "868" -> if (kotlin.math.abs(bandwidthKhz - 62.5) < 1.0) RegionInfo.EU_N_868 else RegionInfo.EU_868
        "915" -> RegionInfo.US
        "2400" -> RegionInfo.LORA_24
        else -> null
    }

    /**
     * Bandwidth multiplier of a band: 2.4 GHz LoRa is "wide LoRa" and the firmware scales the preset bandwidth by
     * 3.25 there (250 kHz becomes 812.5 kHz); the spreading factor stays the same.
     */
    fun bandwidthScale(bandId: String): Double =
        if (regionFor(bandId, 0.0)?.wideLora == true) 3.25 else 1.0

    /** MT_SW default frequency for the Narrow presets in the 868 MHz band. */
    const val NARROW_868_MHZ = 869.44165

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
