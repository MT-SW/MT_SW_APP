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
 * One saved radio of the connection center (MT_SW): the app can remember up to [RadioSlotsPrefs.MAX_SLOTS] radios and
 * keep them connected at the same time. [id] is stable (1..MAX_SLOTS) and names the radio's own data; [address] is the
 * connection address (the same text as the single saved address used before); [name] is what the user sees.
 */
data class RadioSlot(val id: Int, val address: String, val name: String?)

/**
 * The saved radios and the one the screens currently show. The list always keeps the radio that was saved before the
 * connection center existed as radio 1, so nothing is lost when the app is updated.
 */
interface RadioSlotsPrefs {
    /** The saved radios, ordered by [RadioSlot.id]; at most [MAX_SLOTS]. */
    val slots: StateFlow<List<RadioSlot>>

    /** The id of the radio the screens show, or null when no radio is saved. Always one of [slots]. */
    val selectedId: StateFlow<Int?>

    /**
     * The radios the user switched off ("Rozłącz"): they are not connected in the background either. A radio that is
     * saved and not in this set is kept connected whenever the app can.
     */
    val offIds: StateFlow<Set<Int>>

    /** Switches the radio [id] off (true) or back on (false). */
    suspend fun setOff(id: Int, off: Boolean)

    /** Saves [address] in the radio [id] (a free one when null). Returns the id used, or null when all are taken. */
    suspend fun put(address: String, name: String?, id: Int? = null): Int?

    /** Forgets the radio [id]. The selection moves to another saved radio, if any. */
    suspend fun remove(id: Int)

    /** Makes the radio [id] the one the screens show. Ignored for an id that is not saved. */
    suspend fun select(id: Int)

    companion object {
        const val MAX_SLOTS = 3
    }
}
