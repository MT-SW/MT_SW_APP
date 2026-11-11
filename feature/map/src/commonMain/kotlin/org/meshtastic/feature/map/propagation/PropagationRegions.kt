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
@file:Suppress("TooGenericExceptionCaught", "SwallowedException")

package org.meshtastic.feature.map.propagation

import okio.FileSystem
import okio.Path
import org.meshtastic.feature.map.layers.mapLayerFileSystem
import org.meshtastic.feature.map.layers.mapLayersDirectory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

private const val NORMAL_POINTS = 6
private const val DETAILED_POINTS = 9
private const val FINE_POINTS = 12

/** How many points per side the forecast grid of a region has: more points, finer picture, bigger download. */
enum class PropagationDetail(val points: Int) {
    NORMAL(NORMAL_POINTS),
    DETAILED(DETAILED_POINTS),
    FINE(FINE_POINTS),
    ;

    companion object {
        /** The detail a saved grid with [points] points per side was downloaded with. */
        fun of(points: Int): PropagationDetail = entries.lastOrNull { it.points <= points } ?: NORMAL
    }
}

/**
 * One downloaded area of the propagation layer. Regions may overlap; where they do, the one with the finer grid is
 * drawn. [id] is the time it was added, and also names its file; [name] is what the user called it (empty: not named).
 */
data class PropagationRegion(val id: Long, val grid: TropoGrid, val name: String = "") {
    val detail: PropagationDetail
        get() = PropagationDetail.of(grid.rows)

    /** The width of the area in kilometres, measured at its middle. */
    val widthKm: Int
        get() =
            (grid.bounds.lonSpan * KM_PER_DEGREE * cos((grid.bounds.south + grid.bounds.north) / 2 * PI / 180))
                .roundToInt()

    val heightKm: Int
        get() = (grid.bounds.latSpan * KM_PER_DEGREE).roundToInt()

    /** The area one grid cell covers: the smaller, the finer the picture. */
    val cellArea: Double
        get() = grid.bounds.latSpan * grid.bounds.lonSpan / ((grid.rows - 1) * (grid.cols - 1))

    private companion object {
        const val KM_PER_DEGREE = 111.32
    }
}

/** Keeps every region on the device as its own file, so the forecast still draws without the internet. */
internal class PropagationStore {
    private val fileSystem: FileSystem = mapLayerFileSystem()
    private val dir: Path = (mapLayersDirectory().parent ?: mapLayersDirectory()) / "propagation"
    private val legacy: Path = dir / "forecast.txt"

    /** All saved regions, oldest first. The single forecast of the older versions becomes the first region. */
    fun readAll(): List<PropagationRegion> {
        migrateLegacy()
        return try {
            if (!fileSystem.exists(dir)) {
                emptyList()
            } else {
                fileSystem
                    .list(dir)
                    .mapNotNull { path ->
                        val id = idOf(path) ?: return@mapNotNull null
                        decode(id, fileSystem.read(path) { readUtf8() })
                    }
                    .sortedBy { it.id }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(region: PropagationRegion) {
        try {
            fileSystem.createDirectories(dir)
            val named = if (region.name.isEmpty()) "" else "$NAME_MARK${region.name}\n"
            fileSystem.write(fileOf(region.id)) { writeUtf8(named + region.grid.encode()) }
        } catch (e: Exception) {
            // Not being able to keep it only costs a download next time.
        }
    }

    fun delete(id: Long) {
        try {
            fileSystem.delete(fileOf(id), mustExist = false)
        } catch (e: Exception) {
            // A file left behind is only read again on the next start.
        }
    }

    private fun migrateLegacy() {
        try {
            if (!fileSystem.exists(legacy)) return
            TropoGrid.decode(fileSystem.read(legacy) { readUtf8() })?.let {
                save(PropagationRegion(it.fetchedAtMillis, it))
            }
            fileSystem.delete(legacy, mustExist = false)
        } catch (e: Exception) {
            // The old file stays and is tried again next time.
        }
    }

    /** A region file: an optional line "n <name>", then the grid. The older single forecast has no name line. */
    private fun decode(id: Long, text: String): PropagationRegion? {
        val named = text.startsWith(NAME_MARK)
        val name = if (named) text.lineSequence().first().removePrefix(NAME_MARK) else ""
        val grid = TropoGrid.decode(if (named) text.substringAfter('\n') else text) ?: return null
        return PropagationRegion(id, grid, name)
    }

    private fun fileOf(id: Long): Path = dir / "$FILE_PREFIX$id$FILE_SUFFIX"

    private fun idOf(path: Path): Long? {
        val name = path.name
        return if (name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX)) {
            name.removePrefix(FILE_PREFIX).removeSuffix(FILE_SUFFIX).toLongOrNull()
        } else {
            null
        }
    }

    private companion object {
        const val FILE_PREFIX = "region-"
        const val FILE_SUFFIX = ".txt"
        const val NAME_MARK = "n "
    }
}
