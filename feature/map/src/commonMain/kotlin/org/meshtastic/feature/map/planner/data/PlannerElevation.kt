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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.meshtastic.core.common.util.ioDispatcher
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.PathProfile
import org.meshtastic.feature.map.terrain.ElevationTile
import org.meshtastic.feature.map.terrain.GeoBounds
import org.meshtastic.feature.map.terrain.MapterhornEndpoints
import org.meshtastic.feature.map.terrain.TerrainTileFetcher
import org.meshtastic.feature.map.terrain.TerrainTileMath
import org.meshtastic.feature.map.terrain.TileIndex
import org.meshtastic.feature.map.terrain.decodeTerrariumTile
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/** Why elevation data could not be loaded. The UI maps this to a localized message. */
enum class PlannerElevationFailure {
    /** The tile archive could not be reached or read (no network, server error). */
    NETWORK,

    /** A tile was received but could not be decoded. */
    DECODE,
}

/** Thrown by [PlannerElevationSource] when terrain data is unavailable. */
class PlannerElevationException(val failure: PlannerElevationFailure, cause: Throwable? = null) :
    Exception("Planner elevation unavailable: $failure", cause)

/**
 * Terrain access for the planner. Heights are metres above sea level; sea and missing data are 0.
 * Implementations throw [PlannerElevationException] when data cannot be loaded.
 */
interface PlannerElevationSource {
    /** Uniformly sampled terrain profile between [a] and [b] (see [PathProfile.fromSampler]). */
    suspend fun profile(a: GeoPoint, b: GeoPoint): PathProfile

    /**
     * Loads the terrain around [center] within [radiusKm] and returns a fast synchronous sampler
     * `(lat, lon) -> metres` for [org.meshtastic.feature.map.planner.Coverage]. Outside the loaded area it returns 0.
     */
    suspend fun prepareArea(center: GeoPoint, radiusKm: Double): (Double, Double) -> Double

    /** Terrain height at one point, or null when there is no data tile for it. */
    suspend fun altitudeAt(lat: Double, lon: Double): Double?
}

/**
 * [PlannerElevationSource] backed by Mapterhorn Terrarium tiles (global archive, zoom 12; regional archives for
 * [preferredZoom] above 12 when the area fits one regional archive), with an in-memory LRU of decoded tiles.
 *
 * Bilinear sampling inside a tile (neighbour pixels are clamped at tile edges). Values below -50 m are treated as sea
 * (0 m) so that bathymetry never reaches the radio model.
 *
 * @param preferredZoom 12 (default) .. 18; higher values are used only where a regional archive exists and the
 *   requested area needs at most [MAX_TILES_HIGH_ZOOM] tiles, otherwise the global zoom is used.
 * @param fetcherFactory opens a PMTiles archive by URL (injectable for tests).
 * @param tileDecoder decodes a tile (injectable for tests).
 */
class PlannerElevation(
    private val preferredZoom: Int = MapterhornEndpoints.GLOBAL_MAX_ZOOM,
    private val fetcherFactory: (String) -> TerrainTileFetcher = { url -> TerrainTileFetcher(url) },
    private val tileDecoder: (ByteArray) -> ElevationTile = { bytes -> decodeTerrariumTile(bytes) },
    private val ioContext: CoroutineDispatcher = ioDispatcher,
    private val cacheCapacity: Int = DEFAULT_CACHE_TILES,
) : PlannerElevationSource, AutoCloseable {

    private val lock = Mutex()
    private val fetcherLock = Mutex()
    private val fetchers = HashMap<String, TerrainTileFetcher>()

    /** LRU: insertion order, most recently used last. A null value means "no tile in the archive" (sea/no data). */
    private val cache = LinkedHashMap<TileIndex, ElevationTile?>()

    override suspend fun profile(a: GeoPoint, b: GeoPoint): PathProfile {
        val bounds = boundsOf(listOf(a, b), 0.01)
        val zoom = zoomForBounds(bounds, MAX_TILES_HIGH_ZOOM)
        // First pass records which tiles the sampler touches, then they are loaded, then the real pass samples.
        val needed = LinkedHashSet<TileIndex>()
        PathProfile.fromSampler(a, b) { lat, lon ->
            needed.add(TerrainTileMath.tileAt(zoom, lat, lon))
            0.0
        }
        val tiles = loadTiles(needed, bounds, zoom)
        return PathProfile.fromSampler(a, b) { lat, lon -> sampleBilinear(zoom, lat, lon, tiles) }
    }

    override suspend fun prepareArea(center: GeoPoint, radiusKm: Double): (Double, Double) -> Double {
        val bounds = areaBounds(center, radiusKm)
        // Coverage steps are >= 100 m, so the zoom is lowered until the area fits a modest number of tiles.
        var zoom = MapterhornEndpoints.GLOBAL_MAX_ZOOM
        while (zoom > MIN_AREA_ZOOM && TerrainTileMath.tileCountAt(zoom, bounds) > MAX_AREA_TILES) zoom--
        val needed = LinkedHashSet<TileIndex>(TerrainTileMath.tilesAt(zoom, bounds))
        val tiles = loadTiles(needed, bounds, zoom)
        val z = zoom
        return { lat, lon -> sampleBilinear(z, lat, lon, tiles) }
    }

    override suspend fun altitudeAt(lat: Double, lon: Double): Double? {
        val zoom = MapterhornEndpoints.GLOBAL_MAX_ZOOM
        val idx = TerrainTileMath.tileAt(zoom, lat, lon)
        val tiles = loadTiles(setOf(idx), GeoBounds(lat, lon, lat, lon), zoom)
        if (tiles[idx] == null) return null
        return sampleBilinear(zoom, lat, lon, tiles)
    }

    override fun close() {
        for (f in fetchers.values) {
            try {
                f.close()
            } catch (_: Exception) {
                // closing is best effort
            }
        }
        fetchers.clear()
        cache.clear()
    }

    private fun zoomForBounds(bounds: GeoBounds, maxTiles: Int): Int {
        var zoom = preferredZoom.coerceIn(MapterhornEndpoints.GLOBAL_MAX_ZOOM, MapterhornEndpoints.REGIONAL_MAX_ZOOM)
        if (zoom <= MapterhornEndpoints.GLOBAL_MAX_ZOOM) return MapterhornEndpoints.GLOBAL_MAX_ZOOM
        if (MapterhornEndpoints.regionalUrlFor(bounds) == null) return MapterhornEndpoints.GLOBAL_MAX_ZOOM
        while (zoom > MapterhornEndpoints.GLOBAL_MAX_ZOOM && TerrainTileMath.tileCountAt(zoom, bounds) > maxTiles) zoom--
        return zoom
    }

    /** Loads [needed] tiles (cache first). Missing tiles map to null. Throws [PlannerElevationException]. */
    private suspend fun loadTiles(
        needed: Set<TileIndex>,
        bounds: GeoBounds,
        zoom: Int,
    ): Map<TileIndex, ElevationTile?> {
        val result = HashMap<TileIndex, ElevationTile?>()
        val missing = ArrayList<TileIndex>()
        lock.withLock {
            for (t in needed) {
                if (cache.containsKey(t)) {
                    val v = cache.remove(t)
                    cache[t] = v
                    result[t] = v
                } else {
                    missing.add(t)
                }
            }
        }
        if (missing.isEmpty()) return result
        val url = if (zoom > MapterhornEndpoints.GLOBAL_MAX_ZOOM) {
            MapterhornEndpoints.regionalUrlFor(bounds) ?: MapterhornEndpoints.GLOBAL_PMTILES_URL
        } else {
            MapterhornEndpoints.GLOBAL_PMTILES_URL
        }
        val fetched = withContext(ioContext) {
            fetcherLock.withLock {
                val out = ArrayList<Pair<TileIndex, ElevationTile?>>()
                val fetcher = fetcherFor(url)
                for (t in missing) {
                    val bytes = try {
                        fetcher.fetchTile(t.zoom, t.x, t.y)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        dropFetcher(url)
                        throw PlannerElevationException(PlannerElevationFailure.NETWORK, e)
                    }
                    val tile = if (bytes == null) {
                        null
                    } else {
                        try {
                            tileDecoder(bytes)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            throw PlannerElevationException(PlannerElevationFailure.DECODE, e)
                        }
                    }
                    out.add(t to tile)
                }
                out
            }
        }
        lock.withLock {
            for ((t, tile) in fetched) {
                cache.remove(t)
                cache[t] = tile
                result[t] = tile
            }
            while (cache.size > max(cacheCapacity, MIN_CACHE_TILES)) {
                val oldest = cache.keys.first()
                cache.remove(oldest)
            }
        }
        return result
    }

    /** Must be called with [fetcherLock] held. */
    private fun fetcherFor(url: String): TerrainTileFetcher {
        val existing = fetchers[url]
        if (existing != null) return existing
        val created = try {
            fetcherFactory(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw PlannerElevationException(PlannerElevationFailure.NETWORK, e)
        }
        fetchers[url] = created
        return created
    }

    private fun dropFetcher(url: String) {
        val f = fetchers.remove(url) ?: return
        try {
            f.close()
        } catch (_: Exception) {
            // best effort
        }
    }

    companion object {
        const val DEFAULT_CACHE_TILES = 32
        private const val MIN_CACHE_TILES = 8
        const val MAX_TILES_HIGH_ZOOM = 40
        private const val MAX_AREA_TILES = 36L
        private const val MIN_AREA_ZOOM = 7
        private const val SEA_THRESHOLD_M = -50.0

        internal fun boundsOf(points: List<GeoPoint>, padDeg: Double): GeoBounds {
            var south = Double.MAX_VALUE
            var north = -Double.MAX_VALUE
            var west = Double.MAX_VALUE
            var east = -Double.MAX_VALUE
            for (p in points) {
                south = min(south, p.lat)
                north = max(north, p.lat)
                west = min(west, p.lon)
                east = max(east, p.lon)
            }
            return GeoBounds(south - padDeg, west - padDeg, north + padDeg, east + padDeg)
        }

        internal fun areaBounds(center: GeoPoint, radiusKm: Double): GeoBounds {
            val dLat = radiusKm / 110.574
            val cosLat = max(0.05, cos(center.lat * PI / 180.0))
            val dLon = radiusKm / (111.320 * cosLat)
            return GeoBounds(
                center.lat - dLat,
                (center.lon - dLon).coerceAtLeast(-180.0),
                center.lat + dLat,
                (center.lon + dLon).coerceAtMost(180.0),
            )
        }

        /**
         * Bilinear elevation at [lat]/[lon] from [tiles] (keyed by tile index at [zoom]). A tile that is absent
         * from the map or null gives 0 m.
         */
        internal fun sampleBilinear(
            zoom: Int,
            lat: Double,
            lon: Double,
            tiles: Map<TileIndex, ElevationTile?>,
        ): Double {
            val n = (1L shl zoom).toDouble()
            val latC = lat.coerceIn(-85.05112878, 85.05112878)
            val fx = (lon + 180.0) / 360.0 * n
            val fy = (1.0 - asinh(tan(latC * PI / 180.0)) / PI) / 2.0 * n
            val maxIdx = (1L shl zoom).toInt() - 1
            val tx = floor(fx).toInt().coerceIn(0, maxIdx)
            val ty = floor(fy).toInt().coerceIn(0, maxIdx)
            val tile = tiles[TileIndex(zoom, tx, ty)] ?: return 0.0
            val px = (fx - tx) * tile.width - 0.5
            val py = (fy - ty) * tile.height - 0.5
            val x0 = floor(px).toInt()
            val y0 = floor(py).toInt()
            val wx = px - x0
            val wy = py - y0
            val e00 = clean(tile.elevationAt(x0, y0))
            val e10 = clean(tile.elevationAt(x0 + 1, y0))
            val e01 = clean(tile.elevationAt(x0, y0 + 1))
            val e11 = clean(tile.elevationAt(x0 + 1, y0 + 1))
            val top = e00 * (1.0 - wx) + e10 * wx
            val bottom = e01 * (1.0 - wx) + e11 * wx
            return top * (1.0 - wy) + bottom * wy
        }

        private fun clean(e: Float): Double {
            val d = e.toDouble()
            return if (d.isNaN() || d < SEA_THRESHOLD_M) 0.0 else d
        }
    }
}
