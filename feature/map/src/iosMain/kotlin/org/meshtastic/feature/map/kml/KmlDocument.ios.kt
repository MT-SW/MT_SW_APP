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
package org.meshtastic.feature.map.kml

import co.touchlab.kermit.Logger
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.feature.map.layers.MAX_KMZ_INFLATED_BYTES
import org.meshtastic.feature.map.layers.isKmzArchive
import platform.Foundation.NSTemporaryDirectory

private val log = Logger.withTag("KmlDocument")

/** The KML inside a KMZ (the first `.kml` entry), or the bytes themselves when it is a bare KML. */
actual fun readKmlDocument(bytes: ByteArray): String? = if (bytes.isKmzArchive()) {
    withZip(bytes) { zip ->
        val entry = zip.listRecursively("/".toPath()).firstOrNull { it.name.endsWith(".kml", ignoreCase = true) }
        entry?.let { zip.readWithin(it, MAX_KMZ_INFLATED_BYTES)?.decodeToString() }
    }
} else {
    bytes.decodeToString()
}

/** The images [hrefs] points at, taken out of the KMZ; empty for a bare KML. */
actual fun readKmlArchiveImages(bytes: ByteArray, hrefs: Set<String>): Map<String, ByteArray> {
    if (hrefs.isEmpty() || !bytes.isKmzArchive()) return emptyMap()
    return withZip(bytes) { zip ->
        val images = mutableMapOf<String, ByteArray>()
        var budget = MAX_KMZ_INFLATED_BYTES
        for (entry in zip.listRecursively("/".toPath())) {
            val name = entry.toString().trimStart('/')
            if (name !in hrefs || zip.metadata(entry).isDirectory) continue
            // Budget blown mid-archive: keep the images already taken, the overlays whose files went unread are
            // skipped.
            val data = zip.readWithin(entry, budget) ?: break
            budget -= data.size
            images[name] = data
        }
        images
    } ?: emptyMap()
}

/** The archive is written to a temporary file first, because the zip reader works on files. */
private fun <T> withZip(bytes: ByteArray, block: (FileSystem) -> T): T? {
    val fileSystem = FileSystem.SYSTEM
    val file: Path = (NSTemporaryDirectory().trimEnd('/') + "/kmz_$nowMillis.zip").toPath()
    return try {
        fileSystem.write(file) { write(bytes) }
        block(fileSystem.openZip(file))
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        log.w(e) { "Failed to read the KMZ archive" }
        null
    } finally {
        runCatching { fileSystem.delete(file) }
    }
}

/**
 * This entry inflated, or null once it would take the archive past [remaining] bytes (the header size is not trusted).
 */
private fun FileSystem.readWithin(path: Path, remaining: Long): ByteArray? {
    val out = Buffer()
    val source = source(path).buffer()
    try {
        while (true) {
            val read = source.read(out, READ_CHUNK)
            if (read == -1L) break
            if (out.size > remaining) {
                log.w { "Refusing a KMZ entry past the ${MAX_KMZ_INFLATED_BYTES}B inflate cap" }
                return null
            }
        }
    } finally {
        source.close()
    }
    return out.readByteArray()
}

private const val READ_CHUNK = 8192L
