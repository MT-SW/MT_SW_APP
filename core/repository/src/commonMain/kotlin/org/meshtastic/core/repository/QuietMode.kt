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

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * MT_SW: the quiet mode (for a hike, for instance): notifications are held back so nothing disturbs. Alarms (the SOS)
 * and alerts always get through. In [Mode.Priority] the notices that come from the priority devices (the favourite
 * nodes) get through as well: their direct messages and the notices about the node itself.
 */
object QuietMode {
    enum class Mode {
        Off,
        Silent,
        Priority,
    }

    val mode = MutableStateFlow(Mode.Off)

    private var priorityNums: Set<Int> = emptySet()
    private var priorityIds: Set<Int> = emptySet()
    private var priorityNames: Set<String> = emptySet()

    /** The priority devices (node numbers). */
    fun setPriority(nums: Set<Int>) {
        priorityNums = nums
        val names = nums.map { "!" + it.toUInt().toString(HEX_RADIX).padStart(HEX_WIDTH, '0') }.toSet()
        priorityNames = names
        // A direct message is filed under the channel number and the node id; the notice is keyed by its hash.
        priorityIds = nums.toSet() + names.flatMap { name -> (0 until CHANNELS).map { "$it$name".hashCode() } }
    }

    fun set(newMode: Mode) {
        if (mode.value != newMode) Logger.i { "Tryb ciszy: $newMode" }
        mode.value = newMode
    }

    /** Whether a notice keyed by [id] (a node number or the hash of a conversation) is held back now. */
    fun blocksId(id: Int): Boolean =
        when (mode.value) {
            Mode.Off -> false
            Mode.Silent -> true
            Mode.Priority -> id !in priorityIds
        }

    fun blocks(notification: Notification): Boolean {
        if (mode.value == Mode.Off || notification.alarm || notification.category == Notification.Category.Alert) {
            return false
        }
        if (mode.value == Mode.Silent) return true
        val link = notification.deepLink.orEmpty()
        val fromPriority =
            notification.id?.let { it in priorityIds } == true || priorityNames.any { link.contains(it) }
        return !fromPriority
    }

    private const val HEX_RADIX = 16
    private const val HEX_WIDTH = 8
    private const val CHANNELS = 8
}
