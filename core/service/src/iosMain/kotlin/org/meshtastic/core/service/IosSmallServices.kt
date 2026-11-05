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
package org.meshtastic.core.service

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import org.meshtastic.core.repository.GeocodedPlace
import org.meshtastic.core.repository.Location
import org.meshtastic.core.repository.LocationService
import org.meshtastic.core.repository.PhoneLocationProvider
import org.meshtastic.core.repository.ReverseGeocoder
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLPlacemark
import kotlin.coroutines.resume

/** iPhone: the system asks about the local network by itself, so the app treats it as allowed. */
@Single
class IosLocalNetworkAccess : LocalNetworkAccess {
    override fun isGranted(): Boolean = true
}

@Single
class IosDeviceAddressChangeHook : DeviceAddressChangeHook {
    override fun onDeviceAddressChanged() = Unit
}

/** Place names for coordinates, from the system geocoder. */
@OptIn(ExperimentalForeignApi::class)
@Single
class IosReverseGeocoder : ReverseGeocoder {
    override suspend fun place(latitude: Double, longitude: Double): GeocodedPlace? =
        withTimeoutOrNull(GEOCODE_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                CLGeocoder().reverseGeocodeLocation(CLLocation(latitude = latitude, longitude = longitude)) {
                        marks,
                        _,
                    ->
                    val mark = marks?.firstOrNull() as? CLPlacemark
                    cont.resume(mark?.let { GeocodedPlace(it.ISOcountryCode, it.administrativeArea, it.locality) })
                }
            }
        }

    private companion object {
        const val GEOCODE_TIMEOUT_MS = 10_000L
    }
}

/** The phone's own location, taken from the system's first fix (waits a few seconds at most). */
@Single
class IosLocationService(private val provider: PhoneLocationProvider) : LocationService {
    override suspend fun getCurrentLocation(): Location? =
        withTimeoutOrNull(FIX_TIMEOUT_MS) { provider.locationUpdates().first { it.location != null }.location }
            ?.let {
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

    private companion object {
        const val FIX_TIMEOUT_MS = 10_000L
    }
}
