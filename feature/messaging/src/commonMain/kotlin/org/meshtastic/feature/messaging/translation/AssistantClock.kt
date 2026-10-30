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
package org.meshtastic.feature.messaging.translation

import kotlinx.datetime.offsetAt
import org.meshtastic.core.common.util.systemTimeZone

/** Times written next to the messages of a conversation and of a test. */
object AssistantClock {
    private const val MS_PER_SECOND = 1000L
    private const val SECONDS_PER_DAY = 86_400L
    private const val SECONDS_PER_HOUR = 3_600L
    private const val SECONDS_PER_MINUTE = 60L

    /** The local clock time (HH:mm:ss) of an instant in milliseconds, or empty when it is not known (0). */
    fun clock(ms: Long): String {
        if (ms <= 0L) return ""
        val offset = systemTimeZone.offsetAt(kotlin.time.Instant.fromEpochMilliseconds(ms)).totalSeconds
        val day = ((ms / MS_PER_SECOND + offset) % SECONDS_PER_DAY + SECONDS_PER_DAY) % SECONDS_PER_DAY
        fun two(v: Long) = v.toString().padStart(2, '0')
        return "${two(day / SECONDS_PER_HOUR)}:${two(day % SECONDS_PER_HOUR / SECONDS_PER_MINUTE)}:${two(day % SECONDS_PER_MINUTE)}"
    }

    /** A duration in seconds with one decimal place ("12.3"). */
    fun seconds(ms: Long): String = (kotlin.math.round(ms / 100.0) / 10).toString()
}
