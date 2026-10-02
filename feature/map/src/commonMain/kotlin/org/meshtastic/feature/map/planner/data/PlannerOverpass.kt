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
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
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

    private suspend fun fetch(query: String): ClutterMap {
        val body: String? = try {
            withTimeoutOrNull(timeoutMs) {
                val response = httpClient.get(baseUrl) { parameter("data", query) }
                if (response.status.isSuccess()) response.bodyAsText() else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw PlannerClutterException(PlannerClutterFailure.NETWORK, e)
        }
        if (body == null) throw PlannerClutterException(PlannerClutterFailure.NETWORK)
        return try {
            withContext(Dispatchers.Default) { OsmClutterParser.parse(body) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw PlannerClutterException(PlannerClutterFailure.BAD_RESPONSE, e)
        }
    }

    /** Clears the in-memory answers (used by "refresh"). */
    suspend fun invalidate() = lock.withLock { cache.clear() }

    companion object {
        const val BASE_URL = "https://overpass-api.de/api/interpreter"
        const val TIMEOUT_MS = 60_000L
        const val CACHE_MS = 30 * 60 * 1000L
        private const val CACHE_ENTRIES = 6

        /** Rounded to about 1 m so that the same pick always hits the cache. */
        private fun pointKey(p: GeoPoint): String = Num.fmt(p.lat, 5) + "," + Num.fmt(p.lon, 5)
    }
}
