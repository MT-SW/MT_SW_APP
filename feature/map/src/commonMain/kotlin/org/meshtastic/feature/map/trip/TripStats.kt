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
    val descentMeters: Int? = null,
    val lowestMeters: Int? = null,
    val highestMeters: Int? = null,
    val maxKmh: Double? = null,
    /** The time the route was really moving (not standing), as the share of the whole time. */
    val movingMillis: Long? = null,
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

private const val MOVING_MIN_MPS = 0.3
private const val MAX_PLAUSIBLE_MPS = 50.0
private const val MPS_TO_KMH = 3.6

/** The numbers of a saved hike. */
fun Trip.stats(): TripStats {
    val heights = points.mapNotNull { it.ele }
    val steps = heights.zipWithNext { a, b -> b - a }
    val ascent = if (heights.size < 2) null else steps.filter { it > 0 }.sum().toInt()
    val descent = if (heights.size < 2) null else -steps.filter { it < 0 }.sum().toInt()
    var maxMps = 0.0
    var moving = 0L
    points.zipWithNext { a, b ->
        val dt = (b.t - a.t) / MS_PER_SECOND_D
        if (dt > 0) {
            val v = TripStore.meters(a.lat, a.lon, b.lat, b.lon) / dt
            if (v in MOVING_MIN_MPS..MAX_PLAUSIBLE_MPS) {
                moving += b.t - a.t
                if (v > maxMps) maxMps = v
            }
        }
    }
    return TripStats(
        distanceMeters = distanceMeters,
        durationMillis = (endedMillis - startedMillis).coerceAtLeast(0L),
        ascentMeters = ascent,
        descentMeters = descent,
        lowestMeters = heights.minOrNull()?.toInt(),
        highestMeters = heights.maxOrNull()?.toInt(),
        maxKmh = if (maxMps > 0) maxMps * MPS_TO_KMH else null,
        movingMillis = if (moving > 0) moving else null,
    )
}

private const val MS_PER_SECOND_D = 1000.0
