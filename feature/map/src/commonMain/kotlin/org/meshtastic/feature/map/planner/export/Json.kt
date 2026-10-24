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

import kotlin.math.min
import org.meshtastic.core.ui.util.JsonText

/**
 * JSON export of everything the report holds, as data and not as a picture: the sections as label/value pairs, the
 * terrain profile as columns of numbers (metres), the points and the coverage samples. Easy to read for a program.
 */
object PlannerJson {
    fun render(report: PlannerReport): String =
        buildString {
            append("{\n")
            append("  \"title\": ").append(q(report.title)).append(",\n")
            append("  \"subtitle\": ").append(q(report.subtitle)).append(",\n")
            append("  \"generated\": ").append(q(report.generatedAt)).append(",\n")
            append("  \"sections\": [")
            report.sections.forEachIndexed { i, s ->
                append(if (i == 0) "\n" else ",\n")
                append("    {\"title\": ").append(q(s.title)).append(", \"rows\": [")
                append(s.rows.joinToString(", ") { "{\"label\": " + q(it.label) + ", \"value\": " + q(it.value) + "}" })
                append("]}")
            }
            append(if (report.sections.isEmpty()) "],\n" else "\n  ],\n")
            append("  \"points\": [")
            append(
                report.points.joinToString(", ") {
                    "{\"name\": ${q(it.name)}, \"lat\": ${Num.fmt(it.lat, 6)}, \"lon\": ${Num.fmt(it.lon, 6)}, \"description\": ${q(it.description)}}"
                },
            )
            append("],\n")
            append("  \"profile\": ")
            val p = report.profile
            if (p == null) {
                append("null")
            } else {
                val n = minOf(p.distancesM.size, p.groundM.size, p.losM.size, p.fresnelUpperM.size, p.fresnelLowerM.size)
                append("{\"labelA\": ").append(q(p.labelA)).append(", \"labelB\": ").append(q(p.labelB)).append(", \"unit\": \"m\"")
                append(", \"samples\": ").append(n)
                append(",\n    \"distance\": ").append(column(p.distancesM, n, 1))
                append(",\n    \"ground\": ").append(column(p.groundM, n, 1))
                append(",\n    \"los\": ").append(column(p.losM, n, 2))
                append(",\n    \"fresnelUpper\": ").append(column(p.fresnelUpperM, n, 2))
                append(",\n    \"fresnelLower\": ").append(column(p.fresnelLowerM, n, 2)).append("}")
            }
            append(",\n  \"coverage\": ")
            val c = report.coverage
            if (c == null) {
                append("null")
            } else {
                append("{\"title\": ").append(q(c.title))
                append(", \"centerLat\": ").append(Num.fmt(c.centerLat, 6)).append(", \"centerLon\": ").append(Num.fmt(c.centerLon, 6))
                append(", \"maxRangeKm\": ").append(Num.fmt(c.maxRangeKm, 2))
                append(", \"legend\": [").append(c.legend.joinToString(", ") { q(it.first) }).append("]")
                append(", \"samples\": ").append(c.samples.size)
                append(", \"cells\": [")
                append(c.samples.joinToString(",") { "[" + Num.fmt(it.lat, 5) + "," + Num.fmt(it.lon, 5) + "," + Num.fmt(it.marginDb.toDouble(), 1) + "]" })
                append("]}")
            }
            append(",\n  \"notes\": [").append(report.notes.joinToString(", ") { q(it) }).append("],\n")
            append("  \"footer\": ").append(q(report.footer)).append("\n}\n")
        }

    private fun column(values: DoubleArray, n: Int, decimals: Int): String =
        "[" + (0 until min(n, values.size)).joinToString(",") { Num.fmt(values[it], decimals) } + "]"

    private fun q(s: String): String = JsonText.quote(s)
}
