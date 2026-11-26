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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * MT_SW: how a hike travels between devices. The files go to a public image host (the same ones the chat uses for
 * pictures) and only a link goes over the mesh. The map module shares and imports hikes; the messaging module owns the
 * connections to the hosts, so the map reaches them through this seam.
 */
interface HikeTransport {
    /** Uploads a file and returns its link. [onProgress] gets the share sent so far (0..1). Throws on failure. */
    suspend fun upload(bytes: ByteArray, mimeType: String, fileName: String, onProgress: (Float) -> Unit = {}): String

    /** Downloads a file of at most [maxBytes]. Throws on failure or when the file is bigger. */
    suspend fun download(url: String, maxBytes: Long): ByteArray
}

object HikeLinks {
    /** The boot, which opens the message that carries a hike. */
    const val MARK = "\uD83E\uDD7E"

    private val urlRegex = Regex("https://[^\\s]+\\.json", RegexOption.IGNORE_CASE)
    private val allowedHosts = listOf("catbox.moe", "uguu.se")

    /** The text of the message that carries the hike at [url]. */
    fun message(url: String): String = "$MARK MT_SW: $url"

    /** The link of a hike in a received message, or null when the message carries none. */
    fun hikeUrl(text: String): String? =
        if (text.contains(MARK)) urlRegex.find(text)?.value?.takeIf(::isAllowedHost) else null

    /** Only the image hosts the app itself uploads to are trusted. */
    fun isAllowedHost(url: String): Boolean {
        if (!url.startsWith("https://")) return false
        val host = url.removePrefix("https://").substringBefore('/').substringBefore(':').lowercase()
        return allowedHosts.any { host == it || host.endsWith(".$it") }
    }
}

/** Reads a shared hike (by its link) into the hikes of this device; implemented by the map module. */
interface HikeImporter {
    /**
     * Returns the id of the new hike. [senderNum] is the node that sent the link (0 when not known), so the hike can be
     * found again from that node's details. [onStep] gets the number of files done and of all files. Throws on failure.
     */
    suspend fun import(url: String, senderNum: Int = 0, onStep: (Int, Int) -> Unit): String

    /** The newest hike received from each node: node number → hike id. Empty until a node has sent one. */
    val receivedFrom: StateFlow<Map<Int, String>>
}

/** The hike just received from a message: the map replays it the next time it is shown. */
object HikeImportRequest {
    val pendingReplay = MutableStateFlow<String?>(null)
}

/** A text for the message field of the next conversation that opens (the link of a shared hike). */
object ChatDraftRequest {
    val text = MutableStateFlow<String?>(null)
}
