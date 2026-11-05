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
@file:Suppress("ReturnCount")

package org.meshtastic.feature.messaging.translation

import kotlinx.coroutines.delay
import java.io.File

/**
 * Reads the free memory of the device (Android and Linux) and waits until memory of a stopped program is given back.
 */
internal object MemoryProbe {
    private const val SAMPLE_MS = 300L
    private const val SETTLED_MB = 30
    private const val MAX_WAIT_MS = 4_000L
    private const val BACK_WAIT_MS = 20_000L
    private const val BACK_TOLERANCE_MB = 150
    private const val BYTES_PER_KB_TO_MB = 1024

    /** Memory the system can give right now (MB), or null where it cannot be read. */
    fun availableMb(): Int? = runCatching {
        File("/proc/meminfo")
            .readLines()
            .firstOrNull { it.startsWith("MemAvailable:") }
            ?.split(Regex("\\s+"))
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?.let { (it / BYTES_PER_KB_TO_MB).toInt() }
    }
        .getOrNull()

    /**
     * Right after a program ends (or fails to start) the system still takes back its memory, so the number is too small
     * for a moment; this waits until two readings in a row are about the same (at most a few seconds).
     */
    suspend fun settle(before: Int? = null) {
        if (before != null) {
            // The earlier try took memory: wait (longer) until it is back, so the next way starts from a clear place.
            var waited = 0L
            while (waited < BACK_WAIT_MS) {
                val now = availableMb() ?: return
                if (now >= before - BACK_TOLERANCE_MB) return
                delay(SAMPLE_MS)
                waited += SAMPLE_MS
            }
            return
        }
        var last = availableMb() ?: return
        var waited = 0L
        while (waited < MAX_WAIT_MS) {
            delay(SAMPLE_MS)
            waited += SAMPLE_MS
            val now = availableMb() ?: return
            if (kotlin.math.abs(now - last) < SETTLED_MB) return
            last = now
        }
    }
}
