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
package org.meshtastic.core.repository

/** Switches the device shown in the app (MT_SW), e.g. after a tap on a notification that came from another device. */
interface DeviceSwitcher {
    /**
     * Makes the saved device with [address] the one the app shows. The connection of the other devices is not touched.
     * Returns false when [address] is not one of the saved devices.
     */
    fun switchTo(address: String): Boolean
}
