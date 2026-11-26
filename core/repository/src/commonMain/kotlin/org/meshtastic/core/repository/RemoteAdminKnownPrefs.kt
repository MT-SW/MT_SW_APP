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

import kotlinx.coroutines.flow.StateFlow

/**
 * MT_SW: the devices that were once confirmed as the user's own through remote management (a remote-admin session came
 * up). Kept for good, so the "Remote lists" entry stays visible for them without asking the device again.
 */
interface RemoteAdminKnownPrefs {
    /** Node numbers confirmed once. */
    val known: StateFlow<Set<Int>>

    /** Remembers that [nodeNum] was confirmed as the user's own. Does nothing when it is already remembered. */
    fun markKnown(nodeNum: Int)
}
