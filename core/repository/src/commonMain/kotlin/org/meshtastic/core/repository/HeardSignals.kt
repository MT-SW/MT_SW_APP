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
package org.meshtastic.core.repository

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * One packet that really reached the radio over the air (not over MQTT): who sent it, the last byte of the node that
 * passed it on (0 when not known), whether it came straight from the sender, and the signal it arrived with.
 */
data class HeardSignal(
    val from: Int,
    val relayByte: Int,
    val direct: Boolean,
    val rssi: Int,
    val snr: Float,
    val timeMillis: Long,
)

/** MT_SW: the signals of the packets heard, for the range recording of a hike. Nothing is kept here. */
object HeardSignals {
    private val flow =
        MutableSharedFlow<HeardSignal>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val signals: SharedFlow<HeardSignal>
        get() = flow

    fun emit(signal: HeardSignal) {
        flow.tryEmit(signal)
    }
}
