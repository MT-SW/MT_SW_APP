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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal fun assertJson(s: String) {
    var i = 0
    fun ws() { while (i < s.length && s[i] in " \n\r\t") i++ }
    fun str() {
        assertEquals('"', s[i]); i++
        while (s[i] != '"') { assertTrue(s[i].code >= 0x20, "raw control char"); if (s[i] == '\\') i++; i++ }
        i++
    }
    fun value() {
        ws()
        when (val c = s[i]) {
            '{' -> {
                i++; ws()
                if (s[i] == '}') { i++; return }
                while (true) {
                    ws(); assertEquals('"', s[i]); str(); ws(); assertEquals(':', s[i]); i++; value(); ws()
                    if (s[i] == ',') { i++; continue }
                    assertEquals('}', s[i]); i++; return
                }
            }
            '[' -> {
                i++; ws()
                if (s[i] == ']') { i++; return }
                while (true) {
                    value(); ws()
                    if (s[i] == ',') { i++; continue }
                    assertEquals(']', s[i]); i++; return
                }
            }
            '"' -> str()
            else -> {
                val st = i
                while (i < s.length && s[i] !in ",]} \n\r\t") i++
                val t = s.substring(st, i)
                assertTrue(t == "true" || t == "false" || t == "null" || Regex("-?\\d+(\\.\\d+)?").matches(t), "bad token '$t' ($c)")
            }
        }
    }
    value(); ws()
    assertEquals(s.length, i, "trailing data")
}

class ExportFormatsTest {
    private val r = SampleData.report()

    @Test
    fun numFormatter() {
        assertEquals("0.00", Num.fmt(0.0, 2))
        assertEquals("1234567.89", Num.fmt(1234567.891, 2))
        assertEquals("-0.5", Num.fmt(-0.5, 1))
        assertEquals("0.0", Num.fmt(-0.00001, 1))
        assertEquals("0.000012", Num.fmt(0.000012, 6))
        assertTrue(!Num.fmt(1.0e-7, 9).contains('E'))
        assertEquals("3", Num.fmt(2.5, 0))
        assertEquals("1.000000", Num.fmt(0.9999999, 6))
        assertEquals("2.5", Num.fmtTrim(2.50, 3))
        assertEquals("2", Num.fmtTrim(2.0, 3))
        assertEquals("0", Num.fmt(Double.NaN, 0))
    }

    @Test
    fun csvProfile() {
        val csv = PlannerCsv.profile(r)
        val rows = csv.split("\r\n").filter { it.isNotEmpty() }
        assertEquals("distance_m;ground_m;los_m;fresnel_upper_m;fresnel_lower_m", rows[0])
        assertEquals(1 + r.profile!!.distancesM.size, rows.size)
        for (row in rows.drop(1)) assertEquals(5, row.split(';').size)
        assertTrue(rows[1].startsWith("0.0;"))
        assertFalse(csv.contains(','))
        assertEquals(1, PlannerCsv.profile(r.copy(profile = null)).split("\r\n").filter { it.isNotEmpty() }.size)
    }

    @Test
    fun csvSummaryQuoting() {
        val s = PlannerCsv.summary(r)
        val total = r.sections.sumOf { it.rows.size }
        // quoted multi-field lines contain no raw newlines here, so line count == rows
        assertEquals(total, s.split("\r\n").filter { it.isNotEmpty() }.size)
        assertTrue(s.contains("\"Strata \"\"FSPL\"\"\";\"130.2; dB\""), s)
        assertEquals("\"a;b\"", PlannerCsv.quote("a;b"))
        assertEquals("\"he said \"\"x\"\"\"", PlannerCsv.quote("he said \"x\""))
        assertEquals("\"l1\nl2\"", PlannerCsv.quote("l1\nl2"))
        assertEquals("plain", PlannerCsv.quote("plain"))
    }

    // minimal XML well-formedness checker (tags balanced, entities valid)
    private fun checkXml(s: String) {
        val stack = ArrayList<String>()
        var i = s.indexOf("?>") + 2
        while (i < s.length) {
            val lt = s.indexOf('<', i)
            if (lt < 0) break
            val text = s.substring(i, lt)
            assertFalse(text.contains('>') || text.contains('<'), "raw bracket in text")
            for (m in Regex("&[^;\\s]*;?").findAll(text)) assertTrue(m.value in listOf("&amp;", "&lt;", "&gt;", "&quot;", "&apos;"), "entity ${m.value}")
            val gt = s.indexOf('>', lt)
            val tag = s.substring(lt + 1, gt)
            assertFalse(tag.contains('<'))
            if (tag.startsWith("/")) {
                assertEquals(stack.removeAt(stack.size - 1), tag.substring(1))
            } else if (!tag.endsWith("/")) {
                stack.add(tag.split(' ')[0])
            }
            // attributes: even number of quotes
            assertEquals(0, tag.count { it == '"' } % 2)
            i = gt + 1
        }
        assertTrue(stack.isEmpty(), "unclosed $stack")
    }

    @Test
    fun kmlWellFormed() {
        val k = PlannerKml.render(r)
        checkXml(k)
        assertTrue(k.startsWith("<?xml"))
        assertTrue(k.contains("xmlns=\"http://www.opengis.net/kml/2.2\""))
        assertEquals(1, Regex("<LineString>").findAll(k).count())
        assertEquals(r.points.size + 1 + r.coverage!!.samples.size, Regex("<Placemark>").findAll(k).count() - 1 + 0)
        assertTrue(k.contains("Święty Krzyż &lt;B&gt;"))
        assertTrue(k.contains("Węzeł &quot;B&quot; &amp; co"))
        assertFalse(k.contains("<B>"))
        assertTrue(k.contains("<coordinates>20.628600,50.866100,0</coordinates>"))
        assertTrue(k.contains("<color>ff1b5e20</color>") || k.contains("<color>ff205e1b</color>"))
        checkXml(PlannerKml.render(r.copy(coverage = null, points = emptyList())))
    }

    @Test
    fun xmlEscape() {
        assertEquals("&amp;&lt;&gt;&quot;&apos;x", Xml.escape("&<>\"'x\u0001"))
    }

    @Test
    fun geoJsonWellFormed() {
        val g = PlannerGeoJson.render(r)
        assertJson(g)
        assertTrue(g.contains("\"type\":\"FeatureCollection\""))
        assertEquals(1, Regex("\"LineString\"").findAll(g).count())
        assertEquals(r.coverage!!.samples.size, Regex("\"margin_db\"").findAll(g).count())
        assertTrue(g.contains("\"name\":\"Święty Krzyż <B>\""))
        assertTrue(g.contains("\"description\":\"Węzeł \\\"B\\\" & co\""))
        assertTrue(g.contains("[20.628600,50.866100]"))
        assertFalse(Regex("[:\\[,]-?\\d+(\\.\\d+)?[eE]").containsMatchIn(g))
        assertJson(PlannerGeoJson.render(r.copy(coverage = null, points = emptyList())))
    }

    @Test
    fun jsonEscape() {
        assertEquals("\"a\\\"b\\\\c\\n\\t\\u0001\\u2028\"", Json.str("a\"b\\c\n\t\u0001 "))
    }

    @Test
    fun coverageBins() {
        val c = r.coverage!!
        assertEquals(0, CoverageBins.index(c, 25f))
        assertEquals(1, CoverageBins.index(c, 15f))
        assertEquals(2, CoverageBins.index(c, 5f))
        assertEquals(3, CoverageBins.index(c, -5f))
        assertEquals(4, CoverageBins.index(c, -50f))
        val c2 = c.copy(thresholdsDb = listOf(3f, 2f, 1f, 0f))
        assertEquals(0, CoverageBins.index(c2, 3.5f))
        assertEquals(4, CoverageBins.index(c2, -1f))
        val xs = DoubleArray(10) { it * 2.0 }
        assertEquals(2.0, CoverageBins.cellKm(xs, DoubleArray(10)), 1e-9)
    }
}
