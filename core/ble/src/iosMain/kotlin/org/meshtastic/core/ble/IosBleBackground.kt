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
package org.meshtastic.core.ble

import co.touchlab.kermit.Logger
import com.juul.kable.CentralManager

/**
 * Turns on Core Bluetooth state restoration for the whole app (iPhone and iPad). With it, iPadOS/iOS keeps the radio
 * connection request alive even after the system has closed the app in the background, and starts the app again when the
 * radio is back in range. It has to be called once, before anything else uses Bluetooth (scanning, connecting); a second
 * call or a call after the first use is harmless and only logged.
 */
fun configureIosBleBackground() {
    try {
        CentralManager.configure { stateRestoration = true }
        Logger.i { "iOS Bluetooth: state restoration on" }
    } catch (e: IllegalStateException) {
        Logger.w(e) { "iOS Bluetooth: state restoration could not be set (Bluetooth already in use)" }
    }
}
