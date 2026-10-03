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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.math.PI
import kotlin.math.cos

/**
 * Turns an Overpass API answer (`[out:json]` with `out tags geom`) into a [ClutterMap].
 *
 * Understands closed ways and multipolygon relations (a forest is very often a relation whose outline is split over
 * several ways, which are joined here). Data © OpenStreetMap contributors, ODbL.
 */
object OsmClutterParser {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Throws when [text] is not an Overpass answer, or when the server says it gave up (`remark`).
     *
     * @param seen keys (`id * 2 + 1` for a relation, `id * 2` for a way) of the elements taken from earlier pieces of a
     *   large area: an element crossing the border of two pieces is answered by both and must be counted once. The
     *   keys of this answer are added only when the whole answer was read.
     * @param thinM when above zero, outline points closer than this (m) to the previously kept one are dropped: a
     *   forest outline of a large area does not need a point every few metres, and the points are what fills memory.
     */
    fun parse(text: String, seen: MutableSet<Long>? = null, thinM: Double = 0.0): ClutterMap {
        val root = json.parseToJsonElement(text).jsonObject
        val remark = (root["remark"] as? JsonPrimitive)?.content
        val elements = root["elements"] as? JsonArray ?: throw IllegalArgumentException("no elements")
        if (remark != null && elements.isEmpty() && looksLikeFailure(remark)) {
            throw IllegalStateException("overpass: $remark")
        }
        val out = ArrayList<ClutterPolygon>(elements.size)
        val fresh = if (seen != null) ArrayList<Long>() else null
        for (e in elements) {
            val o = e as? JsonObject ?: continue
            val type = (o["type"] as? JsonPrimitive)?.content
            if (seen != null && (type == "way" || type == "relation")) {
                val id = (o["id"] as? JsonPrimitive)?.content?.toLongOrNull()
                if (id != null) {
                    val key = id * 2 + (if (type == "relation") 1L else 0L)
                    if (key in seen) continue
                    fresh?.add(key)
                }
            }
            when (type) {
                "way" -> wayPolygon(o, thinM)?.let { if (!isNegligible(it, thinM)) out.add(it) }
                "relation" -> relationPolygons(o, thinM).forEach { if (!isNegligible(it, thinM)) out.add(it) }
            }
        }
        if (seen != null && fresh != null) seen.addAll(fresh)
        return ClutterMap(out)
    }

    private fun looksLikeFailure(remark: String): Boolean {
        val r = remark.lowercase()
        return r.contains("error") || r.contains("timed out") || r.contains("out of memory")
    }

    // ---- classification ----

    internal fun classify(tags: Map<String, String>): ClutterKind? {
        val building = tags["building"]
        if (building != null && building != "no") return ClutterKind.BUILDING
        if (tags["natural"] == "wood" || tags["landuse"] == "forest") return ClutterKind.FOREST
        return when (tags["landuse"]) {
            "residential" -> ClutterKind.RESIDENTIAL
            "commercial", "industrial", "retail" -> ClutterKind.COMMERCIAL
            else -> null
        }
    }

    /** `height` ("12", "12.5", "12 m"), else `building:levels` x 3 m + 1 m of roof; null when neither is usable. */
    internal fun buildingHeight(tags: Map<String, String>): Double? {
        parseNumber(tags["height"])?.let { if (it > 0.0) return it.coerceAtMost(ClutterHeights.MAX_HEIGHT_M) }
        parseNumber(tags["building:levels"])?.let {
            if (it > 0.0) return (it * LEVEL_M + ROOF_M).coerceAtMost(ClutterHeights.MAX_HEIGHT_M)
        }
        return null
    }

    /** Leading decimal number of [s] ("12,5 m" -> 12.5), or null. */
    internal fun parseNumber(s: String?): Double? {
        if (s == null) return null
        val t = s.trim().replace(',', '.')
        var end = 0
        while (end < t.length && (t[end].isDigit() || t[end] == '.')) end++
        if (end == 0) return null
        return t.substring(0, end).toDoubleOrNull()
    }

    // ---- elements ----

    private fun tagsOf(o: JsonObject): Map<String, String> {
        val tags = o["tags"] as? JsonObject ?: return emptyMap()
        val m = HashMap<String, String>(tags.size)
        for ((k, v) in tags) (v as? JsonPrimitive)?.let { m[k] = it.content }
        return m
    }

    private fun geometryOf(el: JsonElement?): DoubleArray? {
        val arr = el as? JsonArray ?: return null
        val out = DoubleArray(arr.size * 2)
        var n = 0
        for (p in arr) {
            val o = p as? JsonObject ?: continue
            val lat = (o["lat"] as? JsonPrimitive)?.doubleOrNull ?: continue
            val lon = (o["lon"] as? JsonPrimitive)?.doubleOrNull ?: continue
            out[2 * n] = lat
            out[2 * n + 1] = lon
            n++
        }
        return if (n == arr.size) out else out.copyOf(2 * n)
    }

    private fun wayPolygon(o: JsonObject, thinM: Double): ClutterPolygon? {
        val tags = tagsOf(o)
        val kind = classify(tags) ?: return null
        val ring = geometryOf(o["geometry"]) ?: return null
        if (!isClosed(ring)) return null
        val explicit = if (kind == ClutterKind.BUILDING) buildingHeight(tags) else null
        return ClutterPolygon(kind, explicit, thinRing(ring, thinM))
    }

    private fun relationPolygons(o: JsonObject, thinM: Double): List<ClutterPolygon> {
        val tags = tagsOf(o)
        val kind = classify(tags) ?: return emptyList()
        val members = o["members"] as? JsonArray ?: return emptyList()
        val outers = ArrayList<DoubleArray>()
        val inners = ArrayList<DoubleArray>()
        for (m in members) {
            val mo = m as? JsonObject ?: continue
            if ((mo["type"] as? JsonPrimitive)?.content != "way") continue
            val geom = geometryOf(mo["geometry"]) ?: continue
            when ((mo["role"] as? JsonPrimitive)?.content) {
                "outer", "" -> outers.add(geom)
                "inner" -> inners.add(geom)
            }
        }
        val outerRings = assembleRings(outers).map { thinRing(it, thinM) }
        val innerRings = assembleRings(inners).map { thinRing(it, thinM) }
        if (outerRings.isEmpty()) return emptyList()
        val holesFor = Array(outerRings.size) { ArrayList<DoubleArray>() }
        for (h in innerRings) {
            val idx = outerRings.indexOfFirst { ClutterPolygon.ringContains(it, h[0], h[1]) }
            if (idx >= 0) holesFor[idx].add(h)
        }
        val explicit = if (kind == ClutterKind.BUILDING) buildingHeight(tags) else null
        return outerRings.mapIndexed { i, ring -> ClutterPolygon(kind, explicit, ring, holesFor[i]) }
    }

    // ---- thinning ----

    /** True when thinning is on and the whole outline fits in a square of [thinM] metres: invisible at that scale. */
    private fun isNegligible(p: ClutterPolygon, thinM: Double): Boolean {
        if (thinM <= 0.0) return false
        val heightM = (p.maxLat - p.minLat) * METERS_PER_DEG
        val widthM = (p.maxLon - p.minLon) * METERS_PER_DEG * cos(p.minLat * PI / 180.0)
        return heightM < thinM && widthM < thinM
    }

    /**
     * [ring] without the points that lie closer than [tolM] metres to the previously kept one; the first point (which is
     * also the last of a closed ring) always stays. A ring that would be left with fewer than four points (a small
     * building, say) is returned as it was.
     */
    internal fun thinRing(ring: DoubleArray, tolM: Double): DoubleArray {
        val n = ring.size / 2
        if (tolM <= 0.0 || n <= MIN_RING_POINTS) return ring
        val tolDeg = tolM / METERS_PER_DEG
        val tol2 = tolDeg * tolDeg
        val cosLat = cos(ring[0] * PI / 180.0)
        val out = DoubleArray(ring.size)
        out[0] = ring[0]
        out[1] = ring[1]
        var kept = 1
        for (i in 1 until n - 1) {
            val dy = ring[2 * i] - out[2 * (kept - 1)]
            val dx = (ring[2 * i + 1] - out[2 * (kept - 1) + 1]) * cosLat
            if (dx * dx + dy * dy >= tol2) {
                out[2 * kept] = ring[2 * i]
                out[2 * kept + 1] = ring[2 * i + 1]
                kept++
            }
        }
        out[2 * kept] = ring[2 * (n - 1)]
        out[2 * kept + 1] = ring[2 * (n - 1) + 1]
        kept++
        return if (kept < MIN_RING_POINTS) ring else out.copyOf(2 * kept)
    }

    // ---- ring assembly ----

    private fun isClosed(ring: DoubleArray): Boolean {
        val n = ring.size / 2
        return n >= MIN_RING_POINTS && ring[0] == ring[2 * n - 2] && ring[1] == ring[2 * n - 1]
    }

    /**
     * Joins way pieces that share end points into closed rings (the pieces of a multipolygon outline come in any
     * order and direction). Pieces that never close are dropped.
     */
    internal fun assembleRings(pieces: List<DoubleArray>): List<DoubleArray> {
        val open = ArrayList<DoubleArray>(pieces.filter { it.size >= 4 })
        val rings = ArrayList<DoubleArray>()
        while (open.isNotEmpty()) {
            var cur = open.removeAt(open.size - 1)
            var stuck = false
            while (!isClosed(cur) && !stuck) {
                val endLat = cur[cur.size - 2]
                val endLon = cur[cur.size - 1]
                var found = -1
                var reversed = false
                for (k in open.indices) {
                    val p = open[k]
                    if (p[0] == endLat && p[1] == endLon) {
                        found = k
                        break
                    }
                    if (p[p.size - 2] == endLat && p[p.size - 1] == endLon) {
                        found = k
                        reversed = true
                        break
                    }
                }
                if (found < 0) {
                    stuck = true
                } else {
                    val p = open.removeAt(found)
                    cur = append(cur, if (reversed) reverse(p) else p)
                }
            }
            if (isClosed(cur)) rings.add(cur)
        }
        return rings
    }

    /** [b] starts where [a] ends; its first point is not repeated. */
    private fun append(a: DoubleArray, b: DoubleArray): DoubleArray {
        val out = DoubleArray(a.size + b.size - 2)
        a.copyInto(out)
        b.copyInto(out, destinationOffset = a.size, startIndex = 2)
        return out
    }

    private fun reverse(p: DoubleArray): DoubleArray {
        val n = p.size / 2
        val out = DoubleArray(p.size)
        for (i in 0 until n) {
            out[2 * i] = p[2 * (n - 1 - i)]
            out[2 * i + 1] = p[2 * (n - 1 - i) + 1]
        }
        return out
    }

    private const val LEVEL_M = 3.0
    private const val ROOF_M = 1.0
    private const val MIN_RING_POINTS = 4
    private const val METERS_PER_DEG = 111_320.0
}
