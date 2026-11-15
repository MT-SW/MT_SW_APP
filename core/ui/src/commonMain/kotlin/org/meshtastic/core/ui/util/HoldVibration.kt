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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable

/**
 * MT_SW: a strong, continuous vibration of the device that lasts until it is stopped (used while the send button is
 * held). Each platform uses its own vibration motor; a computer has none, so there it does nothing.
 */
interface HoldVibration {
    /** Starts vibrating; calling it again while it already vibrates changes nothing. */
    fun start()

    /** Stops vibrating at once. */
    fun stop()
}

/** The vibration of this platform, remembered for as long as the screen exists. */
@Composable expect fun rememberHoldVibration(): HoldVibration

/**
 * A single short tap of the vibration motor (for example when a tab of the bottom bar is changed). On the iPhone it is
 * the Taptic Engine; the standard Compose haptics do nothing there.
 */
@Composable expect fun rememberTapHaptic(): () -> Unit
