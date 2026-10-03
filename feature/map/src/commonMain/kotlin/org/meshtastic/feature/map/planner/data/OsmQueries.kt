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
import org.meshtastic.feature.map.planner.Geodesy
import org.meshtastic.feature.map.planner.export.Num

/** Overpass QL text for the obstacles the planner wants. Data © OpenStreetMap contributors (ODbL). */
object OsmQueries {
    /** Buildings and forests are looked for this far (m) either side of the path; the profile samples lie on it. */
    const val CORRIDOR_M = 30

    /**
     * Server side time limit (s) and memory cap (bytes) written into every query.
     *
     * [SERVER_MAX_BYTES] is the RAM the Overpass SERVER may use to answer (its own default is 512 MiB), not memory of
     * this device. Set too low (it was 16 MiB once) the server answers "out of memory" even for a tiny area. What the
     * device downloads is bounded separately by the answer size limit in PlannerOverpass.
     */
    const val SERVER_TIMEOUT_S = 40

    /** A link query follows a path of tens of kilometres and may take longer than a piece of a coverage area. */
    const val LINK_SERVER_TIMEOUT_S = 60
    const val SERVER_MAX_BYTES = 268_435_456

    private const val POLYLINE_SPACING_M = 1500.0
    private val AREA_LANDUSE = listOf("forest", "residential", "commercial", "industrial", "retail")
    private const val MAX_POLYLINE_POINTS = 40

    private fun header(timeoutS: Int = SERVER_TIMEOUT_S) = "[out:json][timeout:$timeoutS][maxsize:$SERVER_MAX_BYTES];"

    private fun forestStatements(whole: String): String = buildString {
        append("way[\"natural\"=\"wood\"](around:$CORRIDOR_M,$whole);")
        append("way[\"landuse\"=\"forest\"](around:$CORRIDOR_M,$whole);")
        append("relation[\"natural\"=\"wood\"][\"type\"=\"multipolygon\"](around:$CORRIDOR_M,$whole);")
        append("relation[\"landuse\"=\"forest\"][\"type\"=\"multipolygon\"](around:$CORRIDOR_M,$whole);")
    }

    private fun buildingStatements(line: String): String = buildString {
        append("way[\"building\"](around:$CORRIDOR_M,$line);")
        append("relation[\"building\"][\"type\"=\"multipolygon\"](around:$CORRIDOR_M,$line);")
    }

    /** Fractions of the path where buildings are looked for: all of it, or only near the two ends on a long link. */
    private fun buildingStretches(a: GeoPoint, b: GeoPoint): List<Pair<Double, Double>> {
        val d = Geodesy.distanceM(a, b)
        return if (d > PlannerClutter.LONG_LINK_M) {
            val f = PlannerClutter.BUILDINGS_NEAR_END_M / d
            listOf(0.0 to f, (1.0 - f) to 1.0)
        } else {
            listOf(0.0 to 1.0)
        }
    }

    /**
     * Forests along the whole path, buildings along it too. On a link longer than [PlannerClutter.LONG_LINK_M]
     * buildings are only requested near the two ends. All in one request.
     */
    fun link(a: GeoPoint, b: GeoPoint): String {
        val sb = StringBuilder(header()).append('(')
        sb.append(forestStatements(polyline(a, b, 0.0, 1.0)))
        for ((f0, f1) in buildingStretches(a, b)) sb.append(buildingStatements(polyline(a, b, f0, f1)))
        return sb.append(");out tags geom;").toString()
    }

    /**
     * The same obstacles as [link], as several lighter requests: the forests of the whole path, then the buildings of
     * each stretch on its own. One heavy request used to run into the server's time limit on a long link; the parts
     * are answered one by one and each gets the longer [LINK_SERVER_TIMEOUT_S].
     */
    fun linkParts(a: GeoPoint, b: GeoPoint): List<String> {
        val parts = ArrayList<String>()
        parts.add(header(LINK_SERVER_TIMEOUT_S) + "(" + forestStatements(polyline(a, b, 0.0, 1.0)) + ");out tags geom;")
        for ((f0, f1) in buildingStretches(a, b)) {
            parts.add(header(LINK_SERVER_TIMEOUT_S) + "(" + buildingStatements(polyline(a, b, f0, f1)) + ");out tags geom;")
        }
        return parts
    }

    /** Forests and built-up areas in the box around [center] ([radiusKm] is capped at the planner's limit). */
    fun area(center: GeoPoint, radiusKm: Double): String {
        val r = radiusKm.coerceIn(0.1, PlannerClutter.MAX_AREA_RADIUS_KM)
        return box(ClutterBox.around(center.lat, center.lon, r))
    }

    /** Forests and built-up areas inside [box]: the piece of a large coverage area that is asked for on its own. */
    fun box(box: ClutterBox): String {
        val b = box.asQueryBox()
        val sb = StringBuilder(header()).append('(')
        // Exact key=value matches, not a regular expression on the value: the server answers those from its index and
        // looks only at woods and built-up land, while a pattern makes it read every landuse (all the fields and meadows).
        sb.append("way[\"natural\"=\"wood\"]($b);")
        for (v in AREA_LANDUSE) sb.append("way[\"landuse\"=\"$v\"]($b);")
        sb.append("relation[\"natural\"=\"wood\"][\"type\"=\"multipolygon\"]($b);")
        for (v in AREA_LANDUSE) sb.append("relation[\"landuse\"=\"$v\"][\"type\"=\"multipolygon\"]($b);")
        return sb.append(");out tags geom;").toString()
    }

    /** `lat,lon,lat,lon,...` of points along the path between fractions [f0] and [f1] (both ends included). */
    internal fun polyline(a: GeoPoint, b: GeoPoint, f0: Double, f1: Double): String {
        val lengthM = Geodesy.distanceM(a, b) * (f1 - f0)
        val count = PlannerClutter.sampleCount(lengthM, POLYLINE_SPACING_M, MAX_POLYLINE_POINTS)
        val sb = StringBuilder()
        for (i in 0 until count) {
            val f = f0 + (f1 - f0) * i / (count - 1)
            val p = Geodesy.interpolate(a, b, f)
            if (i > 0) sb.append(',')
            sb.append(Num.fmt(p.lat, 5)).append(',').append(Num.fmt(p.lon, 5))
        }
        return sb.toString()
    }
}
