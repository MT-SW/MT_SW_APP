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
@file:Suppress("MagicNumber")

package org.meshtastic.feature.map.planner

import org.meshtastic.feature.map.planner.data.ClutterHeights
import org.meshtastic.feature.map.planner.data.ClutterKind
import org.meshtastic.feature.map.planner.data.ClutterPolygon
import org.meshtastic.feature.map.planner.export.Num
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Map-layer GeoJSON of the buildings of a plan.
 *
 * For a link: the buildings along the route, those that rise into the line of sight in their own colour. For a
 * coverage: the buildings within the range as a quiet backdrop. Simplestyle properties, drawn by both map flavours.
 */
object BuildingsLayer {
    private const val NEAR_M = 60.0
    private const val MAX_FEATURES = 6000
    private const val EARTH_RADIUS_M = 6_371_000.0
    private const val K_FACTOR = 4.0 / 3.0
    private const val ROUTE_COLOR = "#8d8d8d"
    private const val BLOCKING_COLOR = "#e53935"
    private const val BACKDROP_COLOR = "#7a7a7a"

    /** A building and whether it stands up into the line of sight. */
    internal class Classified(val polygon: ClutterPolygon, val blocking: Boolean)

    /**
     * The buildings within [NEAR_M] of the straight line A to B, each marked blocking when its roof is above the line
     * of sight. [zA] and [zB] are the antenna altitudes (m above sea level); ground comes from [profile].
     */
    @Suppress("LoopWithTooManyJumpStatements")
    internal fun classify(
        polygons: List<ClutterPolygon>,
        a: GeoPoint,
        b: GeoPoint,
        zA: Double,
        zB: Double,
        profile: PathProfile,
        heights: ClutterHeights,
    ): List<Classified> {
        val cosLat = cos(a.lat * PI / 180.0)
        val bx = (b.lon - a.lon) * cosLat * METERS_PER_DEG
        val by = (b.lat - a.lat) * METERS_PER_DEG
        val length2 = bx * bx + by * by
        if (length2 <= 0.0) return emptyList()
        val out = ArrayList<Classified>()
        for (p in polygons) {
            if (p.kind != ClutterKind.BUILDING) continue
            val px = ((p.minLon + p.maxLon) / 2.0 - a.lon) * cosLat * METERS_PER_DEG
            val py = ((p.minLat + p.maxLat) / 2.0 - a.lat) * METERS_PER_DEG
            val t = ((px * bx + py * by) / length2).coerceIn(0.0, 1.0)
            val dx = px - bx * t
            val dy = py - by * t
            val radius = max(p.maxLat - p.minLat, p.maxLon - p.minLon) * METERS_PER_DEG / 2.0
            if (dx * dx + dy * dy > (NEAR_M + radius) * (NEAR_M + radius)) continue
            val distance = t * profile.distanceM
            val ground = groundAt(profile, distance)
            val los = zA + (zB - zA) * t
            val bulge = distance * (profile.distanceM - distance) / (2.0 * K_FACTOR * EARTH_RADIUS_M)
            out += Classified(p, ground + p.heightM(heights) + bulge > los)
        }
        return out
    }

    private fun groundAt(profile: PathProfile, distanceM: Double): Double {
        val last = profile.groundM.size - 1
        val f = (distanceM / profile.stepM).coerceIn(0.0, last.toDouble())
        val i = min(f.toInt(), last - 1).coerceAtLeast(0)
        val frac = f - i
        return profile.groundM[i] * (1 - frac) + profile.groundM[min(i + 1, last)] * frac
    }

    /** GeoJSON of a link's buildings (see [classify]). */
    internal fun renderLink(buildings: List<Classified>): String = collection(
        buildings.take(MAX_FEATURES).map {
            feature(it.polygon, if (it.blocking) BLOCKING_COLOR else ROUTE_COLOR, 0.65)
        },
    )

    /** GeoJSON of the buildings around a coverage centre, as a backdrop. */
    fun renderBackdrop(polygons: List<ClutterPolygon>): String = collection(
        polygons
            .asSequence()
            .filter { it.kind == ClutterKind.BUILDING }
            .take(MAX_FEATURES)
            .map { feature(it, BACKDROP_COLOR, 0.35) }
            .toList(),
    )

    private fun collection(features: List<String>): String =
        "{\"type\":\"FeatureCollection\",\"features\":[${features.joinToString(",")}]}"

    private fun feature(p: ClutterPolygon, colour: String, opacity: Double): String {
        val ring = StringBuilder("[")
        val n = p.outer.size / 2
        for (i in 0..n) {
            val k = i % n
            if (i > 0) ring.append(',')
            ring
                .append('[')
                .append(Num.fmt(p.outer[2 * k + 1], 6))
                .append(',')
                .append(Num.fmt(p.outer[2 * k], 6))
                .append(']')
        }
        ring.append(']')
        return "{\"type\":\"Feature\",\"properties\":{\"fill\":\"$colour\",\"fill-opacity\":$opacity," +
            "\"stroke\":\"$colour\",\"stroke-width\":1},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[$ring]}}"
    }

    private const val METERS_PER_DEG = 111_320.0
}
