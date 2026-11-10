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
 * The computer app's "start when I log in" switch. Only the desktop app installs one; Android starts on its own after a
 * reboot (boot receiver) and an iPhone or iPad cannot start an app by itself, so there [AutostartControls.current]
 * stays null and the switch is not shown.
 */
interface AutostartControl {
    /** True when the app is registered to start at login. */
    val enabled: StateFlow<Boolean>

    /** Registers or removes the start at login. */
    fun setEnabled(on: Boolean)
}

/** Holder of the platform's [AutostartControl]; null until the platform code installs it. */
object AutostartControls {
    var current: AutostartControl? = null
}
