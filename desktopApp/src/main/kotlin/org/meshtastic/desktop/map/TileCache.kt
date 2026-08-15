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
package org.meshtastic.desktop.map

import co.touchlab.kermit.Logger
import org.meshtastic.core.database.desktopDataDir
import java.io.File

private const val TILE_CACHE_DIR = "tile_cache"
private const val TAG = "TileCache"

/**
 * File-based on-disk cache for explicitly-downloaded OSM tiles, kept separate from Coil's shared image cache (which is
 * LRU-evicted and capped at 32 MiB alongside avatars/hardware images — unsuitable for a region the user deliberately
 * saved for offline use). Tiles here persist until [clear] is called.
 */
object TileCache {
    private val cacheDir = File(desktopDataDir(), TILE_CACHE_DIR)

    private fun tileFile(zoom: Int, x: Int, y: Int): File = File(cacheDir, "$zoom/$x/$y.png")

    /** The cached tile file for (zoom, x, y), or null if not downloaded. */
    fun localTileFile(zoom: Int, x: Int, y: Int): File? = tileFile(zoom, x, y).takeIf { it.exists() }

    fun hasTile(zoom: Int, x: Int, y: Int): Boolean = tileFile(zoom, x, y).exists()

    /** Writes [bytes] as the cached tile for (zoom, x, y), creating parent directories as needed. */
    fun saveTile(zoom: Int, x: Int, y: Int, bytes: ByteArray) {
        val file = tileFile(zoom, x, y)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    /** Total bytes used by the tile cache on disk. */
    fun totalSizeBytes(): Long = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun tileCount(): Long = cacheDir.walkTopDown().count { it.isFile }.toLong()

    /** Deletes the entire tile cache. */
    fun clear() {
        runCatching { cacheDir.deleteRecursively() }
            .onFailure { Logger.withTag(TAG).e(it) { "Failed to clear tile cache" } }
    }
}
