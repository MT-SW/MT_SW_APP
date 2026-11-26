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

/** A picture taken during a hike, tied to the place where it was taken. [file] is the name inside the hike's folder. */
@Serializable
data class TripPhoto(
    val lat: Double,
    val lon: Double,
    val t: Long,
    val file: String,
    val caption: String = "",
    /** The link the picture was sent to over the internet, once the hike is shared; empty before. */
    val url: String = "",
)

/** MT_SW: a saved hike: the route and the pictures taken along it. */
@Serializable
data class Trip(
    val id: String,
    val name: String,
    val startedMillis: Long,
    val endedMillis: Long,
    val distanceMeters: Double,
    val points: List<TripPoint>,
    val photos: List<TripPhoto> = emptyList(),
)
