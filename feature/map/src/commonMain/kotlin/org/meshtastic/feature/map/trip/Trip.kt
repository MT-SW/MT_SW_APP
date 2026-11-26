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

import kotlinx.serialization.Serializable

/** One point of a hike's route. */
@Serializable data class TripPoint(val lat: Double, val lon: Double, val ele: Double? = null, val t: Long)

/** A picture of a stop. [file] is the name inside the hike's folder; the small copy is kept as `th_` + [file]. */
@Serializable
data class TripPhoto(
    val file: String,
    /** When the picture was taken, when known (epoch milliseconds); 0 when not. */
    val takenMillis: Long = 0L,
    val caption: String = "",
    /** The link the picture was sent to over the internet, once the hike is shared; empty before. */
    val url: String = "",
)

/**
 * A place on the route where pictures were taken: a stack of [photos]. When the hike is shared only one picture of
 * the stop goes out: the one at [shareIndex] (the first by default).
 */
@Serializable
data class TripStop(
    val id: String,
    val lat: Double,
    val lon: Double,
    /** The time at this place on the route (epoch milliseconds). */
    val t: Long,
    val title: String = "",
    val photos: List<TripPhoto> = emptyList(),
    val shareIndex: Int = 0,
)

/** The first form of a saved picture (one per place), read only to bring old hikes over to [TripStop]. */
@Serializable
data class LegacyPhoto(
    val lat: Double,
    val lon: Double,
    val t: Long,
    val file: String,
    val caption: String = "",
    val url: String = "",
)

/** One signal heard on the route: where it was heard, from which node and how strong ([relayed]: it was passed on). */
@Serializable
data class TripSignal(
    val t: Long,
    val lat: Double,
    val lon: Double,
    val node: Int,
    val rssi: Int,
    val snr: Float,
    val relayed: Boolean = false,
)

/** What the health app of the phone had recorded for the time of the hike (read on request, kept with the hike). */
@Serializable
data class TripHealth(
    val steps: Long? = null,
    val distanceMeters: Double? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val calories: Int? = null,
    val ascentMeters: Int? = null,
    /** When it was read (epoch milliseconds). */
    val readMillis: Long = 0L,
)

/** MT_SW: a saved hike: the route and the stops with their pictures. */
@Serializable
data class Trip(
    val id: String,
    val name: String,
    val startedMillis: Long,
    val endedMillis: Long,
    val distanceMeters: Double,
    val points: List<TripPoint>,
    val stops: List<TripStop> = emptyList(),
    val photos: List<LegacyPhoto> = emptyList(),
    val health: TripHealth? = null,
    /** The node that sent this hike to us (0 for a hike recorded here). */
    val sharedBy: Int = 0,
    /** The signals of the nodes heard along the route (range recording). */
    val signals: List<TripSignal> = emptyList(),
) {
    val photoCount: Int
        get() = stops.sumOf { it.photos.size }
}
