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

import org.meshtastic.core.ui.util.HealthData

/** What the health app gave, as it is kept with the hike. */
fun HealthData.toTripHealth(readMillis: Long): TripHealth = TripHealth(
    steps = steps,
    distanceMeters = distanceMeters,
    avgHeartRate = avgHeartRate,
    maxHeartRate = maxHeartRate,
    calories = calories,
    ascentMeters = ascentMeters,
    readMillis = readMillis,
)
