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
 * The computer app's "allow a second copy" switch. By default only one copy of the program runs: starting it again
 * brings the window of the first copy forward, so two copies never fight over the same radio. Only the desktop app
 * installs one; on Android and iPhone/iPad [SingleInstanceControls.current] stays null and the switch is not shown.
 */
interface SingleInstanceControl {
    /** True when a second copy of the program may run next to the first one. */
    val allowSeveral: StateFlow<Boolean>

    /** Allows or forbids a second copy; it takes effect at the next start of the program. */
    fun setAllowSeveral(on: Boolean)
}

/** Holder of the platform's [SingleInstanceControl]; null until the platform code installs it. */
object SingleInstanceControls {
    var current: SingleInstanceControl? = null
}
