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

import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.feature.map.layers.mapLayerFileSystem
import org.meshtastic.feature.map.layers.mapLayersDirectory
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.Geodesy
import kotlin.math.roundToInt

/**
 * Buildings (outlines and heights of OpenStreetMap, © OpenStreetMap contributors, ODbL) saved on the device, so the
 * terrain panorama raises its skyline by them without asking the internet each time it opens. One file per downloaded
 * area, named after its centre and range; [load] merges every area that covers the place looked from.
 */
@Suppress("TooManyFunctions")
class BuildingStore(
    private val fileSystem: FileSystem = mapLayerFileSystem(),
    private val directory: Path = (mapLayersDirectory().parent ?: mapLayersDirectory()) / "buildings",
    private val ageDays: suspend () -> Int = { DEFAULT_MAX_AGE_DAYS },
) : ClutterPieceCache {
    private val piecesDirectory: Path = directory / "pieces"

    /** Saved pieces older than this many days are downloaded again. */
    @Volatile var maxAgeDays: Int = DEFAULT_MAX_AGE_DAYS

    private class Area(val lat: Double, val lon: Double, val radiusKm: Int, val path: Path)

    private fun areas(): List<Area> = try {
        if (!fileSystem.exists(directory)) {
            emptyList()
        } else {
            fileSystem.list(directory).mapNotNull { path ->
                val parts = path.name.removeSuffix(EXTENSION).split('_')
                if (parts.size != NAME_PARTS) return@mapNotNull null
                val lat = parts[0].toIntOrNull() ?: return@mapNotNull null
                val lon = parts[1].toIntOrNull() ?: return@mapNotNull null
                val km = parts[2].toIntOrNull() ?: return@mapNotNull null
                Area(lat / DEGREE_SCALE, lon / DEGREE_SCALE, km, path)
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun covers(area: Area, at: GeoPoint): Boolean =
        Geodesy.distanceM(GeoPoint(area.lat, area.lon), at) <= area.radiusKm * METRES_PER_KM

    /** Whether a downloaded area covers [at]. */
    fun covers(at: GeoPoint): Boolean = areas().any { covers(it, at) }

    /** The buildings of every downloaded area that covers [at]; empty when there is none. */
    fun load(at: GeoPoint): ClutterMap {
        val polygons = ArrayList<ClutterPolygon>()
        for (area in areas().filter { covers(it, at) }) {
            try {
                polygons += decode(fileSystem.read(area.path) { readByteArray() })
            } catch (_: Exception) {
                // A damaged file is skipped; the rest still counts.
            }
        }
        return ClutterMap(polygons)
    }

    /** Saves the buildings of the area around [centre]; false when the disk refused. */
    fun write(centre: GeoPoint, radiusKm: Int, buildings: List<ClutterPolygon>): Boolean = try {
        fileSystem.createDirectories(directory)
        val name =
            "${(centre.lat * DEGREE_SCALE).roundToInt()}_${(centre.lon * DEGREE_SCALE).roundToInt()}_$radiusKm$EXTENSION"
        fileSystem.write(directory / name.toPath()) { write(encode(buildings)) }
        true
    } catch (_: Exception) {
        false
    }

    override suspend fun refreshAge() {
        maxAgeDays = ageDays()
    }

    override fun savedBuildings(a: GeoPoint, b: GeoPoint): List<ClutterPolygon>? {
        val fresh =
            areas().filter { area ->
                val savedAt = fileSystem.metadataOrNull(area.path)?.lastModifiedAtMillis
                covers(area, a) && covers(area, b) && savedAt != null && nowMillis - savedAt < maxAgeDays * MS_PER_DAY
            }
        if (fresh.isEmpty()) return null
        return try {
            fresh.flatMap { decode(fileSystem.read(it.path) { readByteArray() }) }
        } catch (_: Exception) {
            null
        }
    }

    override fun readPiece(key: String): List<ClutterPolygon>? = try {
        val path = piecesDirectory / "$key$PIECE_EXTENSION".toPath()
        val savedAt = fileSystem.metadataOrNull(path)?.lastModifiedAtMillis
        // A piece older than a month is asked for again: that is the update of an area that is already saved.
        if (savedAt != null && nowMillis - savedAt < maxAgeDays * MS_PER_DAY) {
            decode(fileSystem.read(path) { readByteArray() })
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    override fun writePiece(key: String, polygons: List<ClutterPolygon>) {
        try {
            fileSystem.createDirectories(piecesDirectory)
            fileSystem.write(piecesDirectory / "$key$PIECE_EXTENSION".toPath()) { write(encode(polygons)) }
        } catch (_: Exception) {
            // A piece not saved is only downloaded again next time.
        }
    }

    /** The space the saved buildings take, in bytes. */
    fun sizeBytes(): Long = try {
        areas().sumOf { fileSystem.metadataOrNull(it.path)?.size ?: 0L } +
            (if (fileSystem.exists(piecesDirectory)) fileSystem.list(piecesDirectory) else emptyList()).sumOf {
                fileSystem.metadataOrNull(it)?.size ?: 0L
            }
    } catch (_: Exception) {
        0L
    }

    /** Deletes all saved buildings. */
    fun clear() {
        try {
            if (fileSystem.exists(directory)) fileSystem.deleteRecursively(directory)
        } catch (_: Exception) {
            // best effort
        }
    }

    private fun encode(buildings: List<ClutterPolygon>): ByteArray {
        val out = Buffer()
        out.writeInt(buildings.size)
        for (b in buildings) {
            out.writeByte(if (b.explicitHeightM != null) 1 else 0)
            out.writeLong((b.explicitHeightM ?: 0.0).toRawBits())
            writeRing(out, b.outer)
            out.writeInt(b.holes.size)
            b.holes.forEach { writeRing(out, it) }
        }
        return out.readByteArray()
    }

    private fun writeRing(out: Buffer, ring: DoubleArray) {
        out.writeInt(ring.size)
        ring.forEach { out.writeLong(it.toRawBits()) }
    }

    private fun decode(bytes: ByteArray): List<ClutterPolygon> {
        val input = Buffer().write(bytes)
        val count = input.readInt()
        return List(count) {
            val hasHeight = input.readByte().toInt() == 1
            val height = Double.fromBits(input.readLong())
            val outer = readRing(input)
            val holes = List(input.readInt()) { readRing(input) }
            ClutterPolygon(ClutterKind.BUILDING, height.takeIf { hasHeight }, outer, holes)
        }
    }

    private fun readRing(input: Buffer): DoubleArray =
        DoubleArray(input.readInt()) { Double.fromBits(input.readLong()) }

    private companion object {
        const val EXTENSION = ".bld"
        const val PIECE_EXTENSION = ".piece"
        const val DEFAULT_MAX_AGE_DAYS = 30
        const val MS_PER_DAY = 24L * 60 * 60 * 1000
        const val NAME_PARTS = 3
        const val DEGREE_SCALE = 100_000.0
        const val METRES_PER_KM = 1_000.0
    }
}
