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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import org.meshtastic.core.repository.PhoneLocation
import org.meshtastic.core.repository.PhoneLocationProvider
import org.meshtastic.core.repository.PhoneLocationState
import org.meshtastic.feature.map.hud.PhonePressureProvider
import org.meshtastic.feature.node.compass.CompassHeadingProvider
import org.meshtastic.feature.node.compass.HeadingState
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreMotion.CMAltimeter
import platform.CoreMotion.CMAltitudeData
import platform.Foundation.NSError
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObject

private fun CLLocationManager.granted(): Boolean =
    authorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
        authorizationStatus == kCLAuthorizationStatusAuthorizedAlways

/** GPS of the iPhone: asks for permission, then reports every fix. */
@OptIn(ExperimentalForeignApi::class)
class IosPhoneLocationProvider : PhoneLocationProvider {
    override fun locationUpdates(): Flow<PhoneLocationState> = callbackFlow {
        val manager = CLLocationManager()
        var last: PhoneLocation? = null

        fun push() {
            trySend(
                PhoneLocationState(
                    permissionGranted = manager.granted(),
                    providerEnabled = CLLocationManager.locationServicesEnabled(),
                    location = last,
                ),
            )
        }

        val delegate =
            object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                    val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                    val (lat, lon) = loc.coordinate.useContents { latitude to longitude }
                    last =
                        PhoneLocation(
                            latitude = lat,
                            longitude = lon,
                            altitude = loc.ellipsoidalAltitude,
                            timeMillis = (loc.timestamp.timeIntervalSince1970 * 1000).toLong(),
                            mslAltitude = loc.altitude,
                            accuracyM = loc.horizontalAccuracy.takeIf { it >= 0 }?.toFloat(),
                            verticalAccuracyM = loc.verticalAccuracy.takeIf { it >= 0 }?.toFloat(),
                            speedMps = loc.speed.takeIf { it >= 0 }?.toFloat(),
                        )
                    push()
                }

                override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                    if (manager.granted()) manager.startUpdatingLocation()
                    push()
                }

                override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                    push()
                }
            }
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
        if (manager.granted()) manager.startUpdatingLocation() else manager.requestWhenInUseAuthorization()
        push()
        awaitClose {
            manager.stopUpdatingLocation()
            manager.delegate = null
        }
    }.flowOn(Dispatchers.Main)
}

/** Compass of the iPhone. */
class IosCompassHeadingProvider : CompassHeadingProvider {
    override fun headingUpdates(): Flow<HeadingState> {
        if (!CLLocationManager.headingAvailable()) return kotlinx.coroutines.flow.flowOf(HeadingState(hasSensor = false))
        return callbackFlow {
            val manager = CLLocationManager()
            val delegate =
                object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
                        val h = didUpdateHeading.trueHeading.takeIf { it >= 0 } ?: didUpdateHeading.magneticHeading
                        trySend(HeadingState(heading = h.toFloat(), accuracy = didUpdateHeading.headingAccuracy.toInt()))
                    }
                }
            manager.delegate = delegate
            manager.startUpdatingHeading()
            awaitClose {
                manager.stopUpdatingHeading()
                manager.delegate = null
            }
        }.flowOn(Dispatchers.Main)
    }
}

/** Barometer of the iPhone (hPa). */
class IosPhonePressureProvider : PhonePressureProvider {
    override fun pressureUpdates(): Flow<Float> {
        if (!CMAltimeter.isRelativeAltitudeAvailable()) return emptyFlow()
        return callbackFlow {
            val altimeter = CMAltimeter()
            altimeter.startRelativeAltitudeUpdatesToQueue(NSOperationQueue.mainQueue) { data: CMAltitudeData?, _: NSError? ->
                data?.pressure?.let { trySend(it.floatValue * 10f) }
            }
            awaitClose { altimeter.stopRelativeAltitudeUpdates() }
        }
    }
}
