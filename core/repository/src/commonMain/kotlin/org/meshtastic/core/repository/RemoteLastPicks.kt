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

/** The three things a node can be picked for in the remote lists window; each keeps its own memory. */
enum class RemotePickKind {
    FAVORITE,
    IGNORE,
    CONTACT,
}

/**
 * MT_SW: the nodes last picked in the remote lists window, kept separately for favorites, ignored nodes and added
 * contacts, so the same choice need not be ticked again. Each memory can be cleared on its own.
 */
interface RemoteLastPicks {
    /** Node numbers last picked, per kind. */
    val picks: StateFlow<Map<RemotePickKind, Set<Int>>>

    /** Remembers [nums] as the last pick for [kind] (replaces what was remembered for it). */
    fun remember(kind: RemotePickKind, nums: Collection<Int>)

    /** Forgets what was remembered for [kind]. */
    fun clear(kind: RemotePickKind)
}
