/*
 * Copyright (c) 2025 MT_SW contributors
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

import org.meshtastic.feature.map.planner.GeoPoint
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** What kind of obstacle a map polygon is. Data © OpenStreetMap contributors (ODbL). */
enum class ClutterKind {
    /** One building. */
    BUILDING,

    /** A wood or forest (`natural=wood`, `landuse=forest`). */
    FOREST,

    /** A residential area (`landuse=residential`): houses and gardens, no single buildings. */
    RESIDENTIAL,

    /** A commercial, industrial or retail area (`landuse=commercial|industrial|retail`). */
    COMMERCIAL,
}

/**
 * Obstacle heights (m above ground) assumed for each [ClutterKind]. A building that carries its own `height` or
 * `building:levels` tag keeps that value; [buildingM] is only the fallback for buildings that have neither.
 */
data class ClutterHeights(
    val forestM: Double = DEFAULT_FOREST_M,
    val buildingM: Double = DEFAULT_BUILDING_M,
    val residentialM: Double = DEFAULT_RESIDENTIAL_M,
    val commercialM: Double = DEFAULT_COMMERCIAL_M,
) {
    companion object {
        const val DEFAULT_FOREST_M = 15.0
        const val DEFAULT_BUILDING_M = 8.0
        const val DEFAULT_RESIDENTIAL_M = 8.0
        const val DEFAULT_COMMERCIAL_M = 12.0
        const val MAX_HEIGHT_M = 200.0
    }
}

/**
 * One closed outline. [outer] and every ring in [holes] are flat `lat0, lon0, lat1, lon1, ...` arrays (not repeating
 * the first vertex at the end is fine, repeating it is fine too).
 *
 * @param explicitHeightM height from the source data (buildings only), or null to use [ClutterHeights].
 */
@Suppress("ArrayInDataClass")
class ClutterPolygon(
    val kind: ClutterKind,
    val explicitHeightM: Double?,
    val outer: DoubleArray,
    val holes: List<DoubleArray> = emptyList(),
) {
    val minLat: Double
    val maxLat: Double
    val minLon: Double
    val maxLon: Double

    init {
        var a = Double.MAX_VALUE
        var b = -Double.MAX_VALUE
        var c = Double.MAX_VALUE
        var d = -Double.MAX_VALUE
        var i = 0
        while (i + 1 < outer.size) {
            a = min(a, outer[i])
            b = max(b, outer[i])
            c = min(c, outer[i + 1])
            d = max(d, outer[i + 1])
            i += 2
        }
        minLat = a
        maxLat = b
        minLon = c
        maxLon = d
    }

    /** True when the point is inside [outer] and outside every hole. */
    fun contains(lat: Double, lon: Double): Boolean {
        if (lat < minLat || lat > maxLat || lon < minLon || lon > maxLon) return false
        if (!ringContains(outer, lat, lon)) return false
        for (h in holes) if (ringContains(h, lat, lon)) return false
        return true
    }

    fun heightM(heights: ClutterHeights): Double {
        val h = when (kind) {
            ClutterKind.BUILDING -> explicitHeightM ?: heights.buildingM
            ClutterKind.FOREST -> heights.forestM
            ClutterKind.RESIDENTIAL -> heights.residentialM
            ClutterKind.COMMERCIAL -> heights.commercialM
        }
        return h.coerceIn(0.0, ClutterHeights.MAX_HEIGHT_M)
    }

    companion object {
        /** Even-odd ray casting; x is longitude, y is latitude (fine for outlines far smaller than the Earth). */
        fun ringContains(ring: DoubleArray, lat: Double, lon: Double): Boolean {
            val n = ring.size / 2
            if (n < 3) return false
            var inside = false
            var j = n - 1
            for (i in 0 until n) {
                val yi = ring[2 * i]
                val xi = ring[2 * i + 1]
                val yj = ring[2 * j]
                val xj = ring[2 * j + 1]
                if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) inside = !inside
                j = i
            }
            return inside
        }
    }
}

/** How many obstacles of each kind a [ClutterMap] holds. */
data class ClutterStats(val buildings: Int, val forests: Int, val areas: Int) {
    val total: Int get() = buildings + forests + areas
}

/**
 * Obstacles around a path or an area, indexed on a grid for fast point lookups.
 *
 * [heightAt] answers "how tall is whatever stands on this spot", the tallest one when several overlap. Open ground is 0.
 */
class ClutterMap(val polygons: List<ClutterPolygon>) {
    private val cells = HashMap<Long, MutableList<Int>>()

    /** Polygons spanning too many cells to index; checked for every lookup. */
    private val large = ArrayList<Int>()

    val stats: ClutterStats

    init {
        var buildings = 0
        var forests = 0
        var areas = 0
        for ((i, p) in polygons.withIndex()) {
            when (p.kind) {
                ClutterKind.BUILDING -> buildings++
                ClutterKind.FOREST -> forests++
                ClutterKind.RESIDENTIAL, ClutterKind.COMMERCIAL -> areas++
            }
            val ix0 = floor(p.minLon / CELL_DEG).toInt()
            val ix1 = floor(p.maxLon / CELL_DEG).toInt()
            val iy0 = floor(p.minLat / CELL_DEG).toInt()
            val iy1 = floor(p.maxLat / CELL_DEG).toInt()
            val count = (ix1 - ix0 + 1).toLong() * (iy1 - iy0 + 1).toLong()
            if (count > MAX_CELLS_PER_POLYGON) {
                large.add(i)
            } else {
                for (ix in ix0..ix1) for (iy in iy0..iy1) cells.getOrPut(key(ix, iy)) { ArrayList(2) }.add(i)
            }
        }
        stats = ClutterStats(buildings, forests, areas)
    }

    fun isEmpty(): Boolean = polygons.isEmpty()

    /** Height (m above ground) of the tallest obstacle at the point, 0 on open ground. */
    fun heightAt(lat: Double, lon: Double, heights: ClutterHeights = ClutterHeights()): Double {
        var best = 0.0
        val list = cells[key(floor(lon / CELL_DEG).toInt(), floor(lat / CELL_DEG).toInt())]
        if (list != null) {
            for (i in list) {
                val p = polygons[i]
                if (p.contains(lat, lon)) best = max(best, p.heightM(heights))
            }
        }
        for (i in large) {
            val p = polygons[i]
            if (p.contains(lat, lon)) best = max(best, p.heightM(heights))
        }
        return best
    }

    companion object {
        /** About 1.1 km of latitude. */
        const val CELL_DEG = 0.01
        private const val MAX_CELLS_PER_POLYGON = 2500L

        val EMPTY = ClutterMap(emptyList())

        private fun key(ix: Int, iy: Int): Long = (ix.toLong() shl 32) xor (iy.toLong() and 0xFFFFFFFFL)
    }
}

/** Why obstacle data could not be loaded. The UI maps this to a localized message. */
enum class PlannerClutterFailure {
    /** The server could not be reached, answered with an error, or did not answer in time. */
    NETWORK,

    /** The answer could not be understood, or the server reported that the request was too large. */
    BAD_RESPONSE,
}

/** Thrown by [PlannerClutterSource] when obstacle data is unavailable. */
class PlannerClutterException(val failure: PlannerClutterFailure, cause: Throwable? = null) :
    Exception("Planner clutter unavailable: $failure", cause)

/** Obstacle data (buildings, forests) for the planner. Throws [PlannerClutterException] when it cannot be loaded. */
interface PlannerClutterSource {
    /** Obstacles along the straight path between [a] and [b]. */
    suspend fun forLink(a: GeoPoint, b: GeoPoint): ClutterMap

    /** Obstacles (forests and built-up areas) within [radiusKm] of [center]. */
    suspend fun forArea(center: GeoPoint, radiusKm: Double): ClutterMap
}

/** A source that knows no obstacles; the default where none is wired in (and in tests). */
object NoClutterSource : PlannerClutterSource {
    override suspend fun forLink(a: GeoPoint, b: GeoPoint): ClutterMap = ClutterMap.EMPTY

    override suspend fun forArea(center: GeoPoint, radiusKm: Double): ClutterMap = ClutterMap.EMPTY
}

/** Shared numbers for putting obstacles into a terrain profile. */
object PlannerClutter {
    /** Largest radius (km) around the coverage centre for which obstacle data is requested. */
    const val MAX_AREA_RADIUS_KM = 30.0

    /** Obstacles this close to an antenna are ignored: the antenna height is measured from the ground it stands on. */
    const val CLEAR_AROUND_ANTENNA_M = 100.0

    /** Links longer than this only load buildings near their ends; the middle of a long path is open to the sky. */
    const val LONG_LINK_M = 20_000.0

    /** How far from each end buildings are loaded on a long link. */
    const val BUILDINGS_NEAR_END_M = 8_000.0

    /**
     * Terrain heights with obstacle heights added on top. [clutterAt] gives the obstacle height of sample `index`.
     * Samples closer than [clearStartM] to the first and [clearEndM] to the last sample, and those two samples
     * themselves, keep the bare ground.
     */
    fun withClutter(
        ground: DoubleArray,
        stepM: Double,
        clutterAt: (index: Int) -> Double,
        clearStartM: Double = CLEAR_AROUND_ANTENNA_M,
        clearEndM: Double = CLEAR_AROUND_ANTENNA_M,
    ): DoubleArray {
        val last = ground.size - 1
        val out = ground.copyOf()
        val totalM = stepM * last
        for (i in 1 until last) {
            val d = stepM * i
            if (d < clearStartM || totalM - d < clearEndM) continue
            out[i] = ground[i] + clutterAt(i)
        }
        return out
    }

    /** Number of whole samples (at least 2) to place on a stretch of [lengthM] with one sample per [spacingM]. */
    internal fun sampleCount(lengthM: Double, spacingM: Double, maxCount: Int): Int =
        ceil(lengthM / spacingM).toInt().coerceIn(2, maxCount)
}
