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

class PdfTest {
    private fun pdf(r: PlannerReport = SampleData.report()) = bytesToLatin1(PlannerPdf.render(r))

    @Test
    fun headerAndTrailer() {
        val s = pdf()
        assertTrue(s.startsWith("%PDF-1.4\n"))
        assertTrue(s.trimEnd().endsWith("%%EOF"))
    }

    @Test
    fun xrefOffsetsPointToObjects() {
        val s = pdf()
        val sx = s.lastIndexOf("startxref\n")
        val xrefPos = s.substring(sx + 10).substringBefore('\n').toInt()
        assertTrue(s.startsWith("xref\n", xrefPos))
        val lines = s.substring(xrefPos).lines()
        val (first, count) = lines[1].split(' ').map { it.toInt() }
        assertEquals(0, first)
        assertEquals("0000000000 65535 f ", lines[2])
        for (i in 1 until count) {
            val e = lines[2 + i]
            assertEquals(19, e.length, "xref entry length")
            val off = e.substring(0, 10).toInt()
            assertTrue(e.endsWith(" 00000 n "))
            assertTrue(s.startsWith("$i 0 obj\n", off), "object $i at $off")
        }
        assertTrue(s.contains("/Size $count"))
    }

    @Test
    fun streamLengthsMatch() {
        val s = pdf()
        var from = 0
        var n = 0
        while (true) {
            val i = s.indexOf("/Length ", from)
            if (i < 0) break
            val len = s.substring(i + 8).substringBefore(' ').toInt()
            val st = s.indexOf("stream\n", i) + 7
            assertEquals("\nendstream", s.substring(st + len, st + len + 10))
            from = st + len
            n++
        }
        assertTrue(n >= 1)
    }

    @Test
    fun pageCountAndNumbers() {
        val small = pdf(SampleData.report(rows = 2, withProfile = false, withCoverage = false))
        assertTrue(Regex("/Type /Page /").findAll(small).count() >= 1)
        val big = pdf(SampleData.report(rows = 90))
        val pages = Regex("/Type /Page /").findAll(big).count()
        assertTrue(pages >= 3, "pages=$pages")
        assertTrue(big.contains("/Count $pages"))
        assertTrue(big.contains("(1 / $pages)"))
        assertTrue(big.contains("($pages / $pages)"))
    }

    @Test
    fun polishEncoding() {
        val s = pdf()
        assertTrue(s.contains("/Differences [1 /aogonek /cacute /eogonek /lslash /nacute /sacute /zacute /zdotaccent /Aogonek /Cacute /Eogonek /Lslash /Nacute /Sacute /Zacute /Zdotaccent]"))
        assertTrue(s.contains("/BaseEncoding /WinAnsiEncoding"))
        assertEquals(4, PdfText.polishCode('ł'))
        assertEquals(12, PdfText.polishCode('Ł'))
        assertEquals("\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008", PdfText.encode("ąćęłńśźż"))
        assertEquals("óÓ", PdfText.encode("óÓ"))
        // "Łysa Góra" -> Ł=\014, ó=\363
        assertTrue(s.contains("(\\014ysa G\\363ra"))
        assertEquals("a?b", PdfText.encode("a漢b"))
        assertEquals("\u0096", PdfText.encode("–"))
    }

    @Test
    fun escaping() {
        assertEquals("\\(a\\)\\\\", PdfText.escape("(a)\\"))
        assertEquals("\\004", PdfText.escape(PdfText.encode("ł")))
        assertEquals("(x\\(y\\))", PdfText.literal("x(y)"))
        val s = pdf()
        assertFalse(s.contains("<b>\n"))
    }

    @Test
    fun wrapRespectsWidth() {
        val lines = PdfText.wrap("Lorem ipsum dolor sit amet " + "x".repeat(200), false, 9.0, 120.0)
        assertTrue(lines.size > 3)
        for (l in lines) assertTrue(PdfText.width(l, false, 9.0) <= 120.0 + 0.01, l)
        assertEquals(667 * 10 / 1000.0, PdfText.width("A", false, 10.0), 1e-9)
    }

    @Test
    fun deterministic() {
        assertEquals(pdf(), pdf())
    }

    @Test
    fun emptyReportStillValid() {
        val s = bytesToLatin1(PlannerPdf.render(PlannerReport("", "", "", emptyList(), null, null, emptyList(), emptyList(), "")))
        assertTrue(s.startsWith("%PDF-1.4") && s.trimEnd().endsWith("%%EOF"))
    }

    @Test
    fun degenerateProfileAndCoverage() {
        val p = ReportProfile(doubleArrayOf(0.0), doubleArrayOf(1.0), doubleArrayOf(2.0), doubleArrayOf(3.0), doubleArrayOf(0.0), "A", "B", "x", "y")
        val c = ReportCoverage(50.0, 20.0, emptyList(), 0.0, emptyList(), "t")
        val r = SampleData.report().copy(profile = p, coverage = c)
        assertTrue(pdf(r).trimEnd().endsWith("%%EOF"))
    }
}
