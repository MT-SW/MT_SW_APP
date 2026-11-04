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

import org.koin.core.annotation.Single
import org.meshtastic.core.repository.GeocodedPlace
import org.meshtastic.core.repository.Location
import org.meshtastic.core.repository.LocationService
import org.meshtastic.core.repository.ReverseGeocoder

/** iPhone: the system asks about the local network by itself, so the app treats it as allowed. */
@Single
class IosLocalNetworkAccess : LocalNetworkAccess {
    override fun isGranted(): Boolean = true
}

@Single
class IosDeviceAddressChangeHook : DeviceAddressChangeHook {
    override fun onDeviceAddressChanged() = Unit
}

/** Place names for coordinates are not available on iPhone yet. */
@Single
class IosReverseGeocoder : ReverseGeocoder {
    override suspend fun place(latitude: Double, longitude: Double): GeocodedPlace? = null
}

/** The phone's own location is not used on iPhone yet. */
@Single
class IosLocationService : LocationService {
    override suspend fun getCurrentLocation(): Location? = null
}
