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
package org.meshtastic.feature.map.planner.data

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.meshtastic.feature.map.layers.mapLayerFileSystem
import org.meshtastic.feature.map.layers.mapLayersDirectory
import org.meshtastic.feature.map.terrain.TileIndex

/**
 * Terrain tiles saved on the device, so the planner and the terrain panorama work without the internet where the
 * terrain was saved. One file per tile; an empty file records a tile the archive does not have (sea).
 */
class TerrainStore(
    private val fileSystem: FileSystem = mapLayerFileSystem(),
    private val directory: Path = (mapLayersDirectory().parent ?: mapLayersDirectory()) / "terrain",
) {
    private fun pathOf(tile: TileIndex): Path = directory / "${tile.zoom}-${tile.x}-${tile.y}.tile".toPath()

    fun has(tile: TileIndex): Boolean = fileSystem.exists(pathOf(tile))

    /** The saved bytes (empty: no tile there), or null when this tile was never saved. */
    fun read(tile: TileIndex): ByteArray? {
        val path = pathOf(tile)
        return try {
            if (fileSystem.exists(path)) fileSystem.read(path) { readByteArray() } else null
        } catch (_: Exception) {
            null
        }
    }

    /** Saves a tile; false when the disk refused (full, no access). */
    fun write(tile: TileIndex, bytes: ByteArray?): Boolean = try {
        fileSystem.createDirectories(directory)
        fileSystem.write(pathOf(tile)) { write(bytes ?: ByteArray(0)) }
        true
    } catch (_: Exception) {
        false
    }

    /** The space the saved terrain takes, in bytes. */
    fun sizeBytes(): Long = try {
        if (fileSystem.exists(directory)) {
            fileSystem.list(directory).sumOf { fileSystem.metadataOrNull(it)?.size ?: 0L }
        } else {
            0L
        }
    } catch (_: Exception) {
        0L
    }

    /** Deletes all saved terrain. */
    fun clear() {
        try {
            if (fileSystem.exists(directory)) fileSystem.deleteRecursively(directory)
        } catch (_: Exception) {
            // best effort
        }
    }
}
