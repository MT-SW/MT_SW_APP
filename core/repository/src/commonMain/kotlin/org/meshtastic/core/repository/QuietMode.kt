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
 * MT_SW: the quiet mode (for a hike, for instance): while it is on, notifications from all devices are held back so
 * nothing disturbs. Alarms (the SOS) and alerts always get through. Exceptions can be made: the favourite nodes
 * ([exceptFavorites]) and nodes chosen by hand ([exceptNodes]); their direct messages and the notices about the node
 * itself get through.
 */
object QuietMode {
    enum class Mode {
        Off,
        Silent,
    }

    val mode = MutableStateFlow(Mode.Off)

    /** Whether the favourite nodes are an exception. */
    val exceptFavorites = MutableStateFlow(false)

    /** The nodes chosen by hand as an exception (node numbers). */
    val exceptNodes = MutableStateFlow<Set<Int>>(emptySet())

    private var favoriteNums: Set<Int> = emptySet()
    private var exemptNums: Set<Int> = emptySet()
    private var exemptNames: Set<String> = emptySet()
    private var exemptIds: Set<Int> = emptySet()

    /** The favourite nodes the app knows now (node numbers); they count only while [exceptFavorites] is on. */
    fun setFavorites(nums: Set<Int>) {
        favoriteNums = nums
        refresh()
    }

    fun setExceptFavorites(on: Boolean) {
        exceptFavorites.value = on
        refresh()
    }

    fun setExceptNodes(nums: Set<Int>) {
        exceptNodes.value = nums
        refresh()
    }

    private fun refresh() {
        val nums = exceptNodes.value + if (exceptFavorites.value) favoriteNums else emptySet()
        exemptNums = nums
        val names = nums.map { "!" + it.toUInt().toString(HEX_RADIX).padStart(HEX_WIDTH, '0') }.toSet()
        exemptNames = names
        // A direct message is filed under the channel number and the node id; the notice is keyed by its hash.
        exemptIds = nums + names.flatMap { name -> (0 until CHANNELS).map { "$it$name".hashCode() } }
    }

    fun set(newMode: Mode) {
        if (mode.value != newMode) Logger.i { "Tryb ciszy: $newMode" }
        mode.value = newMode
    }

    /** Whether a notice keyed by [id] (a node number or the hash of a conversation) is held back now. */
    fun blocksId(id: Int): Boolean =
        when (mode.value) {
            Mode.Off -> false
            Mode.Silent -> id !in exemptIds
        }

    fun blocks(notification: Notification): Boolean {
        if (mode.value == Mode.Off || notification.alarm || notification.category == Notification.Category.Alert) {
            return false
        }
        val link = notification.deepLink.orEmpty()
        val exempt = notification.id?.let { it in exemptIds } == true || exemptNames.any { link.contains(it) }
        return !exempt
    }

    private const val HEX_RADIX = 16
    private const val HEX_WIDTH = 8
    private const val CHANNELS = 8
}
