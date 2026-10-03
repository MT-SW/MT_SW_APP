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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.export.Num

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

    private val lock = Mutex()
    private val cache = LinkedHashMap<String, Pair<Long, ClutterMap>>()

    override suspend fun forLink(a: GeoPoint, b: GeoPoint): ClutterMap =
        cached("L:" + pointKey(a) + ":" + pointKey(b)) { OsmQueries.link(a, b) }

    override suspend fun forArea(center: GeoPoint, radiusKm: Double): ClutterMap {
        val r = radiusKm.coerceIn(0.1, PlannerClutter.MAX_AREA_RADIUS_KM)
        return cached("A:" + pointKey(center) + ":" + Num.fmt(r, 1)) { OsmQueries.area(center, r) }
    }

    private suspend fun cached(key: String, query: () -> String): ClutterMap = lock.withLock {
        val now = clockMs()
        val hit = cache[key]
        if (hit != null && now - hit.first < CACHE_MS) return@withLock hit.second
        val map = fetch(query())
        cache.remove(key)
        cache[key] = now to map
        while (cache.size > CACHE_ENTRIES) cache.remove(cache.keys.first())
        map
    }

    /**
     * Asks the main server and, when it refuses, is busy or does not answer, the mirrors one after another. Data that
     * is genuinely too big for this device ([PlannerClutterFailure.TOO_LARGE]) stops at once: another server would
     * send the same amount.
     */
    private suspend fun fetch(query: String): ClutterMap {
        var last: PlannerClutterException? = null
        for (url in listOf(baseUrl) + fallbackUrls.filter { it != baseUrl }) {
            try {
                return fetchFrom(url, query)
            } catch (e: PlannerClutterException) {
                if (e.failure == PlannerClutterFailure.TOO_LARGE) throw e
                last = e
            }
        }
        throw last ?: PlannerClutterException(PlannerClutterFailure.NETWORK)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchFrom(url: String, query: String): ClutterMap {
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
            withContext(Dispatchers.Default) { OsmClutterParser.parse(body) }
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
    suspend fun invalidate() = lock.withLock { cache.clear() }

    companion object {
        const val BASE_URL = "https://overpass-api.de/api/interpreter"
        const val TIMEOUT_MS = 60_000L
        const val CACHE_MS = 30 * 60 * 1000L
        private const val MAX_BODY_BYTES = 12_000_000
        private const val CHUNK_BYTES = 16 * 1024
        private const val INITIAL_BYTES = 256 * 1024
        private const val CACHE_ENTRIES = 6
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
