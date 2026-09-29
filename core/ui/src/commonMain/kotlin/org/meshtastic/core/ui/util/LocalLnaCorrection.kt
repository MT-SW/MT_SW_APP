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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.compositionLocalOf

/**
 * LNA gain correction (dB) for displayed noise floor and RSSI. [gains] maps node number to gain; the entry for
 * [myNodeNum] is the connected device's own gain. Displayed = raw - gain. Raw values (database, quality rating) are
 * never changed, only what is shown.
 */
data class LnaCorrection(val myNodeNum: Int? = null, val gains: Map<Int, Int> = emptyMap()) {
    /** Gain of the connected device; it corrects its own noise floor and the RSSI of every packet it receives. */
    val localGain: Int
        get() = myNodeNum?.let { gains[it] } ?: 0

    /** RSSI of a packet received by the connected device. */
    fun rssi(raw: Int): Int = raw - localGain

    /** Noise floor reported by [nodeNum]; the local node uses its own gain, others their per-node gain. */
    fun noiseFloor(nodeNum: Int?, raw: Int): Int = raw - (nodeNum?.let { gains[it] } ?: 0)

    /** Noise floor reported by the connected device. */
    fun localNoiseFloor(raw: Int): Int = raw - localGain
}

@Suppress("CompositionLocalAllowlist")
val LocalLnaCorrection = compositionLocalOf { LnaCorrection() }
