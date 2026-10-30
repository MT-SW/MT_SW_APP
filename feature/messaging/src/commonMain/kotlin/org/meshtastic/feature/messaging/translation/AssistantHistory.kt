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
package org.meshtastic.feature.messaging.translation

import kotlinx.serialization.Serializable

/** One saved message of an assistant conversation. */
@Serializable data class StoredLine(val fromUser: Boolean, val text: String, val at: Long = 0L, val tookMs: Long = 0L, val info: String = "")

/** A saved assistant conversation. It lives only on this device. */
@Serializable
data class StoredChat(val id: String, val title: String, val updatedAt: Long, val lines: List<StoredLine>)

/** Keeps the assistant conversations on the device so they can be opened and continued later. */
interface AssistantHistoryStore {
    /** All saved conversations, newest first. */
    suspend fun list(): List<StoredChat>

    suspend fun save(chat: StoredChat)

    suspend fun delete(id: String)
}
