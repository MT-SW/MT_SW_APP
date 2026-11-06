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
@file:OptIn(ExperimentalForeignApi::class)

package org.meshtastic.ios

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.common.background.AppBackgroundControl
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLLocationAccuracyThreeKilometers
import platform.Foundation.NSError
import platform.Foundation.NSUserDefaults
import platform.darwin.NSObject

/**
 * Keeps the app running in the background on iPhone and iPad, so the Wi-Fi/TCP (also over Tailscale) link to the radio
 * and the collecting of data are not cut when iOS would suspend the app. iOS has no background mode for plain network
 * sockets; the allowed way, used by the original Meshtastic app for Apple too, is continuous location in the background
 * (UIBackgroundModes "location", permission "Always"). The position is read rarely and coarsely, only to keep the app
 * alive. Off by default; the choice is saved and restored after the app was started again in the background.
 */
internal object IosBackgroundKeepAlive : AppBackgroundControl {
    private const val KEY = "mtsw.background.keepalive"
    private const val DISTANCE_FILTER_M = 3000.0

    private val defaults = NSUserDefaults.standardUserDefaults
    private val manager = CLLocationManager()
    private val enabledState = MutableStateFlow(defaults.boolForKey(KEY))
    private val permissionState = MutableStateFlow(false)

    private val delegate =
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                refreshPermission()
                if (enabledState.value) apply()
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                Logger.w { "iOS background: location error ${didFailWithError.localizedDescription}" }
            }
        }

    override val enabled: StateFlow<Boolean> = enabledState
    override val permissionGranted: StateFlow<Boolean> = permissionState

    /** Called once at start; brings the saved choice back (also after iOS started the app only in the background). */
    fun restore() {
        manager.delegate = delegate
        refreshPermission()
        if (enabledState.value) apply()
    }

    override fun setEnabled(on: Boolean) {
        defaults.setBool(on, KEY)
        enabledState.value = on
        if (on) apply() else stop()
    }

    private fun refreshPermission() {
        permissionState.value = manager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways
    }

    private fun apply() {
        if (!permissionState.value) {
            // The system shows its own question; the answer comes back through the delegate.
            manager.requestAlwaysAuthorization()
            return
        }
        manager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
        manager.distanceFilter = DISTANCE_FILTER_M
        manager.pausesLocationUpdatesAutomatically = false
        manager.allowsBackgroundLocationUpdates = true
        manager.startUpdatingLocation()
        Logger.i { "iOS background: keeping the app running (location in the background) on" }
    }

    private fun stop() {
        manager.stopUpdatingLocation()
        manager.allowsBackgroundLocationUpdates = false
        Logger.i { "iOS background: keeping the app running off" }
    }
}
