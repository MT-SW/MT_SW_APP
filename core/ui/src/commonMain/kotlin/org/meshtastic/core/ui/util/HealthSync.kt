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

import androidx.compose.runtime.Composable

/** What the health app of the phone has recorded for a stretch of time. A value is null when it has none. */
data class HealthData(
    val steps: Long? = null,
    val distanceMeters: Double? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val calories: Int? = null,
    val ascentMeters: Int? = null,
) {
    val isEmpty: Boolean
        get() =
            steps == null && distanceMeters == null && avgHeartRate == null && maxHeartRate == null &&
                calories == null && ascentMeters == null
}

/** One point of a route handed to the health app. */
data class HealthRoutePoint(val lat: Double, val lon: Double, val ele: Double?, val t: Long)

/**
 * MT_SW: the link to the health app of the phone through the system's own store (Health Connect on Android, Health on
 * iPhone): it reads what the phone and its watch recorded and writes a hike there as a workout with its route. The
 * apps that feed that store (Huawei Zdrowie, Zdrowie Google, Samsung Health and others) are reached this way; none
 * of their own kits is used. Errors go only to the log.
 */
interface HealthSync {
    /** Reads the data recorded between [fromMillis] and [toMillis], asking for the permission when it is missing. */
    suspend fun read(fromMillis: Long, toMillis: Long): HealthData?

    /** Writes the hike as a workout with its route; false when it could not be done. */
    suspend fun export(
        title: String,
        fromMillis: Long,
        toMillis: Long,
        distanceMeters: Double,
        route: List<HealthRoutePoint>,
    ): Boolean
}

/** The link to the health store, or null where there is none (computer, browser) or it is not installed. */
@Composable expect fun rememberHealthSync(): HealthSync?
