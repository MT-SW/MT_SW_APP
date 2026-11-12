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

package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen

/** Keeps the screen on while [enabled] (firmware update, scanning, HUD), like the window flag on Android. */
@Composable
actual fun KeepScreenOn(enabled: Boolean) {
    DisposableEffect(enabled) {
        if (enabled) UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose { if (enabled) UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}

/** Sets the screen brightness while the composable is shown (QR codes) and puts the old value back afterwards. */
@Composable
actual fun SetScreenBrightness(brightness: Float) {
    DisposableEffect(brightness) {
        val screen = UIScreen.mainScreen
        val previous = screen.brightness
        screen.brightness = brightness.toDouble().coerceIn(0.0, 1.0)
        onDispose { screen.brightness = previous }
    }
}

private fun locationStatus(manager: CLLocationManager): PermissionStatus = when (manager.authorizationStatus) {
    kCLAuthorizationStatusAuthorizedWhenInUse,
    kCLAuthorizationStatusAuthorizedAlways,
    -> PermissionStatus.GRANTED

    kCLAuthorizationStatusNotDetermined -> PermissionStatus.NOT_REQUESTED

    // iOS asks only once; after a refusal the only way back is the app settings.
    else -> PermissionStatus.PERMANENTLY_DENIED
}

/** The real location permission of iOS (while using the app), refreshed every time the app comes back to the front. */
@Composable
actual fun rememberLocationPermissionState(): PermissionUiState {
    val manager = remember { CLLocationManager() }
    var status by remember { mutableStateOf(locationStatus(manager)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = locationStatus(manager) }
    return PermissionUiState(
        status = status,
        request = { manager.requestWhenInUseAuthorization() },
        openAppSettings = { openAppSettings() },
    )
}

// iOS has no coarse/precise split that an app has to ask for separately, so the same state serves both.
@Composable actual fun rememberPreciseLocationPermissionState(): PermissionUiState = rememberLocationPermissionState()
