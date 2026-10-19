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
 * Mutes one sender inside one conversation (in practice: one device on one channel). Messages are still stored and
 * shown; only notifications and sound are suppressed. Entries are persisted and expire on their own.
 */
interface SenderMutePrefs {
    /** Active mutes: key from [muteKey] -> epoch millis until which the sender is muted ([Long.MAX_VALUE] = always). */
    val mutes: StateFlow<Map<String, Long>>

    /** Mutes the sender until [untilMillis]; a value of 0 or less removes the mute. */
    fun setMuteUntil(contactKey: String, senderNum: Int, untilMillis: Long)

    companion object {
        fun muteKey(contactKey: String, senderNum: Int): String = "$contactKey|$senderNum"
    }
}

/** True while the sender is muted at [now]. */
fun Map<String, Long>.isSenderMuted(contactKey: String, senderNum: Int, now: Long): Boolean =
    (this[SenderMutePrefs.muteKey(contactKey, senderNum)] ?: 0L) > now

/** The one list of mute lengths used everywhere (private chats, channels, device on a channel). */
object MuteDurations {
    private const val HOUR_MILLIS = 60L * 60 * 1000
    private const val DAY_MILLIS = 24 * HOUR_MILLIS

    /** Length in millis; [Long.MAX_VALUE] means "always". */
    val millis: List<Long> =
        listOf(
            HOUR_MILLIS,
            DAY_MILLIS,
            2 * DAY_MILLIS,
            7 * DAY_MILLIS,
            14 * DAY_MILLIS,
            30 * DAY_MILLIS,
            Long.MAX_VALUE,
        )
}
