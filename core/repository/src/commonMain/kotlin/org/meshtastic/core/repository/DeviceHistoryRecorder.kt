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

/**
 * MT_SW: keeps readings of the saved devices for the history of the device comparison. The collector hands a reading
 * over every few minutes while the service runs, so the history grows without the comparison window being open.
 */
interface DeviceHistoryRecorder {
    /** Remembers the reading of the device at [address] (readings too close to the last one are dropped). */
    fun store(address: String, snapshot: DeviceSnapshot, nowMillis: Long)
}
