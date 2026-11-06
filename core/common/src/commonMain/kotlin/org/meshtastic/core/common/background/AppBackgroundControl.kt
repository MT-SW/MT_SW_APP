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
package org.meshtastic.core.common.background

import kotlinx.coroutines.flow.StateFlow

/**
 * What the settings screen needs from the platform's "keep the app running in the background" switch. Only the iPhone
 * and iPad install one (iOS suspends a background app together with its Wi-Fi/TCP link); on Android and on the computer
 * the app is not suspended like that, so [AppBackgroundControls.current] stays null and the switch is not shown.
 */
interface AppBackgroundControl {
    /** True when the switch is on (saved; the app restores it on its own after a start in the background). */
    val enabled: StateFlow<Boolean>

    /** True while the system permission "Always" for location is granted, which the switch needs. */
    val permissionGranted: StateFlow<Boolean>

    /** Turns the background work on or off; on the first turn-on the system asks for the "Always" permission. */
    fun setEnabled(on: Boolean)
}

/** Holder of the platform's [AppBackgroundControl]; null until the platform code installs it. */
object AppBackgroundControls {
    var current: AppBackgroundControl? = null
}
