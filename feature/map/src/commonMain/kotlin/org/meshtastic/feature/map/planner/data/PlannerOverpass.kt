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

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.export.Num
import kotlin.math.max

/**
 * [PlannerClutterSource] that asks an Overpass API server (default: the public overpass-api.de instance) for the
 * buildings and forests of OpenStreetMap. Data © OpenStreetMap contributors, ODbL (https://www.openstreetmap.org/copyright).
 *
 * Answers are kept in memory for [CACHE_MS], so changing power, height or any other parameter of the same link does not
 * download the same data again. One request at a time: the public server limits parallel use.
 */
class PlannerOverpass(
    private val httpClient: HttpClient,
    private val baseUrl: String = BASE_URL,
    private val timeoutMs: Long = TIMEOUT_MS,
    private val clockMs: () -> Long = { nowMillis },
    private val fallbackUrls: List<String> = FALLBACK_URLS,
) : PlannerClutterSource {

    /** Guards the answers kept in memory; held only for a moment. */
    private val cacheLock = Mutex()

    /** One request at a time (the public server limits parallel use); held for one request, never for a whole area. */
    private val requestLock = Mutex()

    private val _progress = MutableStateFlow<ClutterProgress?>(null)
    override val progress: StateFlow<ClutterProgress?> = _progress
    private val cache = LinkedHashMap<String, Pair<Long, ClutterMap>>()

    override suspend fun forLink(a: GeoPoint, b: GeoPoint): ClutterMap =
        cached("L:" + pointKey(a) + ":" + pointKey(b)) { fetch(OsmQueries.link(a, b)) }

    override suspend fun forArea(center: GeoPoint, radiusKm: Double): ClutterMap {
        val r = radiusKm.coerceIn(0.1, PlannerClutter.MAX_AREA_RADIUS_KM)
        val key = "A:" + pointKey(center) + ":" + Num.fmt(r, 1)
        // A small area is one request. A large one (50 or 100 km is a box of 100 to 200 km on a side) would be one
        // answer of tens of megabytes: it is fetched piece by piece instead.
        return if (r <= SINGLE_REQUEST_KM) {
            cached(key) { fetch(OsmQueries.area(center, r)) }
        } else {
            cached(key) { fetchTiled(ClutterBox.around(center.lat, center.lon, r)) }
        }
    }

    private suspend fun cached(key: String, load: suspend () -> ClutterMap): ClutterMap {
        cacheLock.withLock {
            val hit = cache[key]
            if (hit != null && clockMs() - hit.first < CACHE_MS) return hit.second
        }
        val map = load()
        cacheLock.withLock {
            cache.remove(key)
            cache[key] = clockMs() to map
            while (cache.size > CACHE_ENTRIES) cache.remove(cache.keys.first())
        }
        return map
    }

    /** The server that answered last is asked first: a dead main server must not cost a timeout for every piece. */
    private var lastGoodUrl: String? = null

    /**
     * Asks the main server and, when it is busy or does not answer, the mirrors one after another. An answer that is
     * too big, or a refusal because of the query itself ("out of memory", "timed out"), stops at once: another server
     * would say the same, and the caller can split the area instead.
     */
    private suspend fun fetch(query: String, seen: MutableSet<Long>? = null, thinM: Double = 0.0): ClutterMap {
        val urls = (listOf(baseUrl) + fallbackUrls).distinct()
        return requestLock.withLock {
            val ordered = lastGoodUrl?.let { good -> listOf(good) + urls.filter { it != good } } ?: urls
            var last: PlannerClutterException? = null
            for (url in ordered) {
                try {
                    val map = fetchFrom(url, query, seen, thinM)
                    lastGoodUrl = url
                    return@withLock map
                } catch (e: PlannerClutterException) {
                    if (!worthAskingAnotherServer(e)) throw e
                    last = e
                }
            }
            throw last ?: PlannerClutterException(PlannerClutterFailure.NETWORK)
        }
    }

    private fun worthAskingAnotherServer(e: PlannerClutterException): Boolean = when (e.failure) {
        PlannerClutterFailure.NETWORK, PlannerClutterFailure.BAD_RESPONSE -> true
        PlannerClutterFailure.SERVER_LIMIT -> e.detail?.startsWith("HTTP") == true
        PlannerClutterFailure.TOO_LARGE -> false
    }

    private class TileWork(val box: ClutterBox, val tries: Int = 0)

    /**
     * A large area as many requests, one at a time (the public server limits parallel use): the box is cut into tiles
     * of about [TILE_START_KM]; a tile the server finds too heavy (or whose answer is too big) is cut into four and
     * asked again, down to [MIN_TILE_KM], so dense places end up in small pieces and empty land in big ones. An element
     * on the border of two tiles is kept once, outlines are thinned to fit in memory, and a server that says "too many
     * requests" is waited for. Everything or an error: half a map would look like open ground where it is not.
     */
    private suspend fun fetchTiled(whole: ClutterBox): ClutterMap =
        try {
            fetchTiledPieces(whole)
        } finally {
            _progress.value = null
        }

    private suspend fun fetchTiledPieces(whole: ClutterBox): ClutterMap {
        val deadline = clockMs() + TILED_BUDGET_MS
        val pending = ArrayDeque<TileWork>()
        ClutterTiles.grid(whole, TILE_START_KM).forEach { pending.addLast(TileWork(it)) }
        var done = 0
        _progress.value = ClutterProgress(0, pending.size)
        val seen = HashSet<Long>()
        val polygons = ArrayList<ClutterPolygon>()
        var points = 0L
        // The farther the area reaches, the coarser the picture needs to be: the coverage raster of 100 km has cells of
        // hundreds of metres, so outlines are thinned (and woods smaller than that dropped) more the bigger the area is.
        val thinM = max(THIN_M, whole.maxSideKm / 2.0 * THIN_M_PER_KM)
        while (pending.isNotEmpty()) {
            if (clockMs() > deadline) throw PlannerClutterException(PlannerClutterFailure.SERVER_LIMIT, detail = "time limit")
            val work = pending.removeFirst()
            val part = try {
                fetch(OsmQueries.box(work.box), seen, thinM)
            } catch (e: PlannerClutterException) {
                val busy = e.detail == "HTTP 429"
                val splittable = (e.failure == PlannerClutterFailure.TOO_LARGE || e.failure == PlannerClutterFailure.SERVER_LIMIT) &&
                    !busy && work.box.maxSideKm > MIN_TILE_KM
                when {
                    busy && work.tries < BUSY_RETRIES -> {
                        delay(BUSY_WAIT_MS)
                        pending.addFirst(TileWork(work.box, work.tries + 1))
                    }
                    splittable -> work.box.quarters().asReversed().forEach { pending.addFirst(TileWork(it)) }
                    else -> throw e
                }
                continue
            }
            done++
            _progress.value = ClutterProgress(done, done + pending.size)
            for (polygon in part.polygons) {
                polygons.add(polygon)
                points += polygon.outer.size / 2
                for (hole in polygon.holes) points += hole.size / 2
            }
            if (points > MAX_POINTS) throw PlannerClutterException(PlannerClutterFailure.TOO_LARGE, detail = "too many points")
            delay(PAUSE_BETWEEN_TILES_MS)
        }
        return ClutterMap(polygons)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchFrom(url: String, query: String, seen: MutableSet<Long>?, thinM: Double): ClutterMap {
        val body: String? = try {
            withTimeoutOrNull(timeoutMs) { download(url, query) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: PlannerClutterException) {
            throw e
        } catch (e: Exception) {
            throw PlannerClutterException(PlannerClutterFailure.NETWORK, e, shortDetail(e.message))
        } catch (e: Throwable) { // OutOfMemoryError is not an Exception: this one really is the device
            throw PlannerClutterException(PlannerClutterFailure.TOO_LARGE, e)
        }
        if (body == null) throw PlannerClutterException(PlannerClutterFailure.NETWORK, detail = "timeout")
        return try {
            withContext(Dispatchers.Default) { OsmClutterParser.parse(body, seen, thinM) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The server's own "out of memory" / "timed out" remark is a refusal of THE SERVER, not a lack of memory
            // of this device, so it must not be reported as one.
            val msg = e.message.orEmpty().lowercase()
            val serverRefused = msg.startsWith("overpass:") &&
                (msg.contains("memory") || msg.contains("maxsize") || msg.contains("timed out") || msg.contains("rate"))
            throw PlannerClutterException(
                if (serverRefused) PlannerClutterFailure.SERVER_LIMIT else PlannerClutterFailure.BAD_RESPONSE,
                e,
                shortDetail(e.message),
            )
        } catch (e: Throwable) { // OutOfMemoryError is not an Exception: this one really is the device
            throw PlannerClutterException(PlannerClutterFailure.TOO_LARGE, e)
        }
    }

    /**
     * Reads the answer in chunks and gives up as soon as it passes [MAX_BODY_BYTES]: a dense area can answer with
     * tens of megabytes, which a phone with a small heap cannot hold (and the parsed tree is several times larger).
     * A status other than success is reported with its code: 429/502/503/504 mean the server is busy or gave up.
     */
    private suspend fun download(url: String, query: String): String? =
        httpClient.prepareGet(url) { parameter("data", query) }.execute { response ->
            if (!response.status.isSuccess()) {
                val code = response.status.value
                val busy = code == 429 || code == 502 || code == 503 || code == 504
                throw PlannerClutterException(
                    if (busy) PlannerClutterFailure.SERVER_LIMIT else PlannerClutterFailure.NETWORK,
                    detail = "HTTP $code",
                )
            }
            val channel = response.bodyAsChannel()
            val chunk = ByteArray(CHUNK_BYTES)
            var buffer = ByteArray(INITIAL_BYTES)
            var size = 0
            while (true) {
                val n = channel.readAvailable(chunk, 0, chunk.size)
                if (n < 0) break
                if (size + n > MAX_BODY_BYTES) throw PlannerClutterException(PlannerClutterFailure.TOO_LARGE)
                if (size + n > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + n))
                chunk.copyInto(buffer, size, 0, n)
                size += n
            }
            buffer.decodeToString(0, size)
        }

    /** Clears the in-memory answers (used by "refresh"). */
    suspend fun invalidate() = cacheLock.withLock { cache.clear() }

    companion object {
        const val BASE_URL = "https://overpass-api.de/api/interpreter"
        const val TIMEOUT_MS = 60_000L
        const val CACHE_MS = 30 * 60 * 1000L
        private const val MAX_BODY_BYTES = 12_000_000
        private const val CHUNK_BYTES = 16 * 1024
        private const val INITIAL_BYTES = 256 * 1024
        private const val CACHE_ENTRIES = 3

        /** Up to this radius an area is a single request. */
        private const val SINGLE_REQUEST_KM = 10.0

        /** Size of the first tiles of a large area, and the smallest tile a heavy one is cut down to (km on a side). */
        private const val TILE_START_KM = 30.0
        private const val MIN_TILE_KM = 4.0

        /** Outline points closer than this (m) are dropped in tiled areas, and more so for a larger radius. */
        private const val THIN_M = 25.0
        private const val THIN_M_PER_KM = 0.8

        /** Outline points kept in memory for a large area (each is two doubles) before it is called too large. */
        private const val MAX_POINTS = 6_000_000L

        /** The whole of a large area has to arrive within this time. */
        private const val TILED_BUDGET_MS = 12 * 60 * 1000L
        private const val PAUSE_BETWEEN_TILES_MS = 250L
        private const val BUSY_WAIT_MS = 5_000L
        private const val BUSY_RETRIES = 4
        private const val DETAIL_MAX_CHARS = 140

        /** Public mirrors tried when the main server refuses, is busy or does not answer. */
        val FALLBACK_URLS = listOf(
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
        )

        /** One line, trimmed: it is shown to the user under the failure message. */
        internal fun shortDetail(message: String?): String? {
            val text = message?.replace('\n', ' ')?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
            return if (text.isEmpty()) null else text.take(DETAIL_MAX_CHARS)
        }

        /** Rounded to about 1 m so that the same pick always hits the cache. */
        private fun pointKey(p: GeoPoint): String = Num.fmt(p.lat, 5) + "," + Num.fmt(p.lon, 5)
    }
}
