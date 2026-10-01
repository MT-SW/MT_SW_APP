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

import kotlin.math.log10

/** LoRa receiver sensitivity. */
object Sensitivity {
    /** Demodulator SNR limit (dB) for spreading factors 5..12 (Semtech SX126x/SX127x data sheets). */
    fun snrLimitDb(sf: Int): Double = when (sf) {
        5 -> -2.5
        6 -> -5.0
        7 -> -7.5
        8 -> -10.0
        9 -> -12.5
        10 -> -15.0
        11 -> -17.5
        12 -> -20.0
        else -> if (sf < 5) -2.5 else -20.0
    }

    /** Sensitivity in dBm: -174 + 10 log10(BW Hz) + NF + SNR limit. */
    fun dbm(bwKhz: Double, sf: Int, nfDb: Double): Double =
        -174.0 + 10.0 * log10(bwKhz * 1000.0) + nfDb + snrLimitDb(sf)
}
