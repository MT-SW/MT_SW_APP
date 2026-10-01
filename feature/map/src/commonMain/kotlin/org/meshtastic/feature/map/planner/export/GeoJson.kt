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
package org.meshtastic.feature.map.planner.export

/**
 * GeoJSON (RFC 7946) FeatureCollection: Point features (name, description), a LineString between the first two
 * points and the coverage samples as Point features with `margin_db` and `color` (`#rrggbb`). Coordinates are
 * `[lon, lat]` with 6 decimals.
 */
object PlannerGeoJson {
    private fun pos(lat: Double, lon: Double) = "[" + Num.fmt(lon, 6) + "," + Num.fmt(lat, 6) + "]"

    fun render(report: PlannerReport): String {
        val feats = ArrayList<String>()
        for (p in report.points) {
            feats.add(
                "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":" + pos(p.lat, p.lon) +
                    "},\"properties\":{\"name\":" + Json.str(p.name) + ",\"description\":" + Json.str(p.description) + "}}",
            )
        }
        if (report.points.size >= 2) {
            val a = report.points[0]
            val b = report.points[1]
            feats.add(
                "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[" + pos(a.lat, a.lon) + "," +
                    pos(b.lat, b.lon) + "]},\"properties\":{\"name\":" + Json.str(a.name + " - " + b.name) + "}}",
            )
        }
        val cov = report.coverage
        if (cov != null) {
            feats.add(
                "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":" + pos(cov.centerLat, cov.centerLon) +
                    "},\"properties\":{\"name\":" + Json.str(cov.title) + ",\"kind\":\"coverage_center\"}}",
            )
            for (s in cov.samples) {
                if (s.marginDb.isNaN() || s.marginDb.isInfinite()) continue
                val col = CoverageBins.color(cov, CoverageBins.index(cov, s.marginDb))
                feats.add(
                    "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":" + pos(s.lat, s.lon) +
                        "},\"properties\":{\"margin_db\":" + Num.fmt(s.marginDb, 1) + ",\"color\":\"#" +
                        Num.hex2((col shr 16) and 255) + Num.hex2((col shr 8) and 255) + Num.hex2(col and 255) + "\"}}",
                )
            }
        }
        val sb = StringBuilder()
        sb.append("{\"type\":\"FeatureCollection\",\"name\":").append(Json.str(report.title)).append(",\"features\":[\n")
        sb.append(feats.joinToString(",\n"))
        sb.append("\n]}\n")
        return sb.toString()
    }
}
