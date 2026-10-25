/*
 * Copyright (c) 2026 MT_SW contributors
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
package org.meshtastic.core.model.antenna

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One antenna with its feed line, saved by the user under a [name] ("My antenna"): the calculator takes everything
 * from it, the link planner takes the gain, the feed line and the power.
 */
@Serializable
data class AntennaSetup(
    val id: String,
    val name: String,
    val input: AntennaInput,
    /** Transmit power at the radio, in dBm. */
    val txDbm: Double,
    val swr: Double = 1.0,
    /** Gain typed by the user instead of the calculated one, in dBi. */
    val gainOverrideDbi: Double? = null,
    val feeder: FeederConfig,
) {
    /** The gain used for the link: typed by the user or calculated for this antenna. */
    val gainDbi: Double get() = gainOverrideDbi ?: AntennaMath.design(input).gainDbi

    /** Loss of the feed line at the antenna's own frequency, in dB (connectors and cables, without the SWR mismatch). */
    val feederLossDb: Double get() = FeederBuilder.compute(feeder, input.mhz).totalDb
}

/** Reading and writing the list of saved antennas as text. */
object AntennaSetups {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** The saved antennas in [text]; anything unreadable gives an empty list. */
    fun decode(text: String): List<AntennaSetup> =
        if (text.isBlank()) emptyList() else runCatching { json.decodeFromString<List<AntennaSetup>>(text) }.getOrDefault(emptyList())

    fun encode(list: List<AntennaSetup>): String = json.encodeToString(list)

    /** [list] with [setup] added, or replacing the one with the same id. */
    fun upsert(list: List<AntennaSetup>, setup: AntennaSetup): List<AntennaSetup> =
        if (list.any { it.id == setup.id }) list.map { if (it.id == setup.id) setup else it } else list + setup

    fun remove(list: List<AntennaSetup>, id: String): List<AntennaSetup> = list.filterNot { it.id == id }
}

/**
 * An antenna the user chose in the calculator to use in the link planner ("Use in the planner"). It is taken once, when
 * the planner opens, and applied to the chosen end ([sideIsB] false for A, true for B).
 */
object PlannerHandoff {
    private var setup: AntennaSetup? = null
    private var sideIsB: Boolean = false

    /** True while an antenna waits for the planner. */
    val pending: Boolean get() = setup != null

    fun offer(value: AntennaSetup, toB: Boolean) {
        setup = value
        sideIsB = toB
    }

    /** The waiting antenna and whether it is for end B; it is forgotten once taken. */
    fun consume(): Pair<AntennaSetup, Boolean>? = setup?.let { it to sideIsB }.also { setup = null }
}
