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
 * CSV export. Separator is a semicolon (`;`), decimal separator is a point (`.`), line break is CRLF (RFC 4180),
 * fields containing `;`, `"`, CR/LF or leading/trailing blanks are quoted with `"` (quotes doubled).
 */
object PlannerCsv {
    private const val SEP = ';'
    private const val EOL = "\r\n"

    /** Header `distance_m;ground_m;los_m;fresnel_upper_m;fresnel_lower_m` followed by one row per profile sample. */
    fun profile(report: PlannerReport): String {
        val sb = StringBuilder()
        sb.append("distance_m;ground_m;los_m;fresnel_upper_m;fresnel_lower_m").append(EOL)
        val p = report.profile ?: return sb.toString()
        val c = ChartData(p)
        for (i in 0 until c.n) {
            sb.append(Num.fmt(c.x[i], 1)).append(SEP)
                .append(Num.fmt(c.ground[i], 1)).append(SEP)
                .append(Num.fmt(c.los[i], 2)).append(SEP)
                .append(Num.fmt(c.up[i], 2)).append(SEP)
                .append(Num.fmt(c.lo[i], 2)).append(EOL)
        }
        return sb.toString()
    }

    /** One `label;value` row per section row; a section with a title and no rows contributes a title row. */
    fun summary(report: PlannerReport): String {
        val sb = StringBuilder()
        for (s in report.sections) {
            if (s.rows.isEmpty()) sb.append(quote(s.title)).append(SEP).append(EOL)
            for (r in s.rows) sb.append(quote(r.label)).append(SEP).append(quote(r.value)).append(EOL)
        }
        return sb.toString()
    }

    internal fun quote(s: String): String {
        val need = s.any { it == SEP || it == '"' || it == '\n' || it == '\r' } || s.startsWith(" ") || s.endsWith(" ")
        return if (need) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }
}
