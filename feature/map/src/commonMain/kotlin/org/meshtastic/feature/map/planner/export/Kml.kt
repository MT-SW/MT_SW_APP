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
 * KML 2.2 export: a Placemark per point, a LineString between the first two points and, when coverage exists, a
 * folder with one small Point placemark per sample styled by its legend class (robust for any grid shape).
 */
object PlannerKml {
    private fun kmlColor(argb: Int): String =
        Num.hex2(argb ushr 24) + Num.hex2(argb and 255) + Num.hex2((argb shr 8) and 255) + Num.hex2((argb shr 16) and 255)

    private fun coord(lat: Double, lon: Double) = Num.fmt(lon, 6) + "," + Num.fmt(lat, 6) + ",0"

    fun render(report: PlannerReport): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n<Document>\n")
        sb.append("<name>").append(Xml.escape(report.title)).append("</name>\n")
        val desc = listOf(report.subtitle, report.generatedAt).filter { it.isNotEmpty() }.joinToString("\n")
        if (desc.isNotEmpty()) sb.append("<description>").append(Xml.escape(desc)).append("</description>\n")
        sb.append("<Style id=\"pt\"><IconStyle><scale>1.1</scale><Icon><href>http://maps.google.com/mapfiles/kml/paddle/red-circle.png</href></Icon></IconStyle></Style>\n")
        sb.append("<Style id=\"link\"><LineStyle><color>ff0000ff</color><width>3</width></LineStyle></Style>\n")
        val cov = report.coverage
        if (cov != null) {
            for (i in cov.legend.indices) {
                sb.append("<Style id=\"cov").append(i).append("\"><IconStyle><color>").append(kmlColor(CoverageBins.color(cov, i)))
                    .append("</color><scale>0.4</scale><Icon><href>http://maps.google.com/mapfiles/kml/shapes/placemark_square.png</href></Icon></IconStyle><LabelStyle><scale>0</scale></LabelStyle></Style>\n")
            }
            sb.append("<Style id=\"cov-1\"><IconStyle><color>ff8b7d60</color><scale>0.4</scale><Icon><href>http://maps.google.com/mapfiles/kml/shapes/placemark_square.png</href></Icon></IconStyle><LabelStyle><scale>0</scale></LabelStyle></Style>\n")
        }
        for (p in report.points) {
            sb.append("<Placemark><name>").append(Xml.escape(p.name)).append("</name>")
            if (p.description.isNotEmpty()) sb.append("<description>").append(Xml.escape(p.description)).append("</description>")
            sb.append("<styleUrl>#pt</styleUrl><Point><coordinates>").append(coord(p.lat, p.lon)).append("</coordinates></Point></Placemark>\n")
        }
        if (report.points.size >= 2) {
            val a = report.points[0]
            val b = report.points[1]
            sb.append("<Placemark><name>").append(Xml.escape(a.name + " - " + b.name)).append("</name><styleUrl>#link</styleUrl>")
                .append("<LineString><tessellate>1</tessellate><coordinates>").append(coord(a.lat, a.lon)).append(' ')
                .append(coord(b.lat, b.lon)).append("</coordinates></LineString></Placemark>\n")
        }
        if (cov != null) {
            sb.append("<Folder><name>").append(Xml.escape(cov.title)).append("</name>\n")
            sb.append("<Placemark><name>").append(Xml.escape(cov.title)).append("</name><styleUrl>#pt</styleUrl><Point><coordinates>")
                .append(coord(cov.centerLat, cov.centerLon)).append("</coordinates></Point></Placemark>\n")
            for (s in cov.samples) {
                if (s.marginDb.isNaN()) continue
                val idx = CoverageBins.index(cov, s.marginDb)
                sb.append("<Placemark><styleUrl>#cov").append(idx).append("</styleUrl><ExtendedData><Data name=\"margin_db\"><value>")
                    .append(Num.fmt(s.marginDb, 1)).append("</value></Data></ExtendedData><Point><coordinates>")
                    .append(coord(s.lat, s.lon)).append("</coordinates></Point></Placemark>\n")
            }
            sb.append("</Folder>\n")
        }
        sb.append("</Document>\n</kml>\n")
        return sb.toString()
    }
}
