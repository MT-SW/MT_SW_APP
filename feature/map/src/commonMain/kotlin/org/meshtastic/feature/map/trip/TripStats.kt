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
package org.meshtastic.feature.map.trip

import org.meshtastic.core.common.util.NumberFormatter

/** MT_SW: the basic numbers of a hike: distance, time, average speed and the ascent (when heights were recorded). */
data class TripStats(
    val distanceMeters: Double,
    val durationMillis: Long,
    val ascentMeters: Int?,
) {
    /** The average speed over the whole time, in km/h. */
    val averageKmh: Double
        get() = if (durationMillis > 0) distanceMeters / METERS_PER_KM / (durationMillis / MS_PER_HOUR) else 0.0

    val distanceText: String
        get() = NumberFormatter.format(distanceMeters / METERS_PER_KM, 1) + " km"

    val averageText: String
        get() = NumberFormatter.format(averageKmh, 1) + " km/h"

    val durationText: String
        get() = formatDuration(durationMillis)

    private companion object {
        const val MS_PER_HOUR = 3_600_000.0
        const val METERS_PER_KM = 1000.0
    }
}

/** Hours and minutes, e.g. 2:05 h; under an hour minutes and seconds, e.g. 12:30 min. */
fun formatDuration(millis: Long): String {
    val total = (millis / MS_PER_SECOND).coerceAtLeast(0)
    val h = total / SECONDS_PER_HOUR
    val m = total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val s = total % SECONDS_PER_MINUTE
    return if (h > 0) "$h:${m.toString().padStart(2, '0')} h" else "$m:${s.toString().padStart(2, '0')} min"
}

private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_HOUR = 3600L
private const val SECONDS_PER_MINUTE = 60L

/** The numbers of a saved hike. */
fun Trip.stats(): TripStats {
    val heights = points.mapNotNull { it.ele }
    val ascent =
        if (heights.size < 2) {
            null
        } else {
            heights.zipWithNext { a, b -> (b - a).coerceAtLeast(0.0) }.sum().toInt()
        }
    return TripStats(distanceMeters, (endedMillis - startedMillis).coerceAtLeast(0L), ascent)
}
