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

package org.meshtastic.ios.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.meshtastic.core.repository.Location
import org.meshtastic.core.repository.LocationRepository
import org.meshtastic.core.repository.PhoneLocationProvider

/** iPhone and iPad: the phone's location (CoreLocation) for everything that asks for it, like on Android. */
class IosLocationRepository(private val provider: PhoneLocationProvider) : LocationRepository {
    private val receiving = MutableStateFlow(false)

    override val receivingLocationUpdates: StateFlow<Boolean> = receiving

    override fun getLocations(): Flow<Location> = provider
        .locationUpdates()
        .onStart { receiving.value = true }
        .onCompletion { receiving.value = false }
        .mapNotNull { if (it.permissionGranted) it.location else null }
        .map {
            Location(
                latitude = it.latitude,
                longitude = it.longitude,
                altitudeMeters = it.altitude,
                mslAltitudeMeters = it.mslAltitude,
                accuracyMeters = it.accuracyM,
                speedMetersPerSecond = it.speedMps,
                timeMillis = it.timeMillis,
            )
        }
}
