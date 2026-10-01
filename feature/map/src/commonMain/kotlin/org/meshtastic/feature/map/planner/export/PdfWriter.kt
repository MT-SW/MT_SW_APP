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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** PDF text helpers: Helvetica with WinAnsi + a /Differences table for the Polish letters missing from WinAnsi. */
object PdfText {
    /** Polish letters absent from WinAnsi; they live on codes 1..16 via /Differences. */
    internal const val POLISH = "ąćęłńśźżĄĆĘŁŃŚŹŻ"
    internal val GLYPHS = listOf(
        "aogonek", "cacute", "eogonek", "lslash", "nacute", "sacute", "zacute", "zdotaccent",
        "Aogonek", "Cacute", "Eogonek", "Lslash", "Nacute", "Sacute", "Zacute", "Zdotaccent",
    )

    /** Code (1..16) used in the content stream for a Polish letter, or -1. */
    fun polishCode(c: Char): Int = POLISH.indexOf(c).let { if (it < 0) -1 else it + 1 }

    /** /Differences array body, e.g. `1 /aogonek /cacute ...`. */
    fun differences(): String = "1 " + GLYPHS.joinToString(" ") { "/$it" }

    private fun winAnsi(c: Char): Int {
        val v = c.code
        return when {
            v in 32..126 -> v
            v == 9 || v == 0xA0 -> 32
            v == 0xAD || v == 0x2212 -> 45
            v in 0xA1..0xFF -> v
            v == 0x20AC -> 0x80
            v == 0x201A -> 0x82
            v == 0x201E -> 0x84
            v == 0x2026 -> 0x85
            v == 0x2020 -> 0x86
            v == 0x2018 -> 0x91
            v == 0x2019 -> 0x92
            v == 0x201C -> 0x93
            v == 0x201D -> 0x94
            v == 0x2022 -> 0x95
            v == 0x2013 -> 0x96
            v == 0x2014 -> 0x97
            v == 0x2122 -> 0x99
            else -> -1
        }
    }

    /** Replaces a few common symbols that WinAnsi lacks by ASCII look-alikes. */
    private fun expand(s: String): String {
        if (s.none { it == '\u2265' || it == '\u2264' || it == '\u2248' || it == '\u2192' || it == '\u2260' }) return s
        val sb = StringBuilder(s.length + 4)
        for (c in s) when (c) {
            '\u2265' -> sb.append(">=")
            '\u2264' -> sb.append("<=")
            '\u2248' -> sb.append('~')
            '\u2192' -> sb.append("->")
            '\u2260' -> sb.append("!=")
            else -> sb.append(c)
        }
        return sb.toString()
    }

    /** Maps a Unicode string to the single-byte codes of the font encoding (as chars 0..255). Unsupported -> '?'. */
    fun encode(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in expand(s)) {
            val p = polishCode(c)
            if (p > 0) { sb.append(p.toChar()); continue }
            val w = winAnsi(c)
            sb.append(if (w < 0) '?' else w.toChar())
        }
        return sb.toString()
    }

    /** Escapes an already encoded string for use inside a PDF literal string (without the parentheses). */
    fun escape(encoded: String): String {
        val sb = StringBuilder(encoded.length + 8)
        for (c in encoded) {
            val v = c.code
            when {
                c == '(' || c == ')' || c == '\\' -> { sb.append('\\'); sb.append(c) }
                v < 32 || v > 126 -> {
                    sb.append('\\')
                    sb.append(('0' + ((v shr 6) and 7))).append(('0' + ((v shr 3) and 7))).append(('0' + (v and 7)))
                }
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** `(...)` literal for a Unicode string. */
    fun literal(s: String): String = "(" + escape(encode(s)) + ")"

    private const val REG = "278,278,355,556,556,889,667,191,333,333,389,584,278,333,278,278,556,556,556,556,556,556,556,556,556,556,278,278,584,584,584,556," +
        "1015,667,667,722,722,667,611,778,722,278,500,667,556,833,722,778,667,778,722,667,611,722,667,944,667,667,611,278,278,278,469,556,333," +
        "556,556,500,556,556,278,556,556,222,222,500,222,833,556,556,556,556,333,500,278,556,500,722,500,500,500,334,260,334,584"
    private const val BLD = "278,333,474,556,556,889,722,238,333,333,389,584,278,333,278,278,556,556,556,556,556,556,556,556,556,556,333,333,584,584,584,611," +
        "975,722,722,722,722,667,611,778,722,278,556,722,611,833,722,778,667,778,722,667,611,722,667,944,667,667,611,333,278,333,584,556,333," +
        "556,611,556,611,556,333,611,611,278,278,556,278,889,611,611,611,611,389,556,333,611,556,778,556,556,500,389,280,389,584"
    private val W_REG = REG.split(',').map { it.toInt() }.toIntArray()
    private val W_BLD = BLD.split(',').map { it.toInt() }.toIntArray()
    private const val LAT_U = "AAAAAAACEEEEIIIIDNOOOOO*OUUUUYPs"
    private const val LAT_L = "aaaaaaaceeeeiiiidnooooo/ouuuuypy"

    /** `/FirstChar /LastChar /Widths` entries for the font dictionary (codes 1..255). */
    internal fun widthsEntry(bold: Boolean): String {
        val sb = StringBuilder("/FirstChar 1 /LastChar 255 /Widths [")
        for (code in 1..255) {
            if (code > 1) sb.append(' ')
            val defined = code in 1..16 || code in 32..126 || code in 0xA0..0xFF || code in intArrayOf(0x80, 0x82, 0x84, 0x85, 0x86, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x99)
            sb.append(if (defined) codeWidth(code, bold) else 0)
        }
        return sb.append(']').toString()
    }

    /** Advance width (1000 units/em) of an encoded byte code. */
    private fun codeWidth(code: Int, bold: Boolean): Int {
        val t = if (bold) W_BLD else W_REG
        fun ascii(ch: Char) = t[ch.code - 32]
        return when {
            code in 32..126 -> t[code - 32]
            code in 1..16 -> {
                val base = "acelnszz"[(code - 1) % 8]
                if (code - 1 >= 8) {
                    if (base == 'l') (if (bold) 611 else 556) else ascii("ACELNSZZ"[(code - 1) % 8])
                } else if (base == 'l') (if (bold) 278 else 222) else ascii(base)
            }
            code == 0x80 -> 556
            code == 0x85 || code == 0x97 || code == 0x99 -> 1000
            code == 0x91 || code == 0x92 || code == 0x82 -> if (bold) 278 else 222
            code == 0x84 -> if (bold) 500 else 333
            code == 0x86 -> 556
            code == 0x93 || code == 0x94 -> if (bold) 500 else 333
            code == 0x95 -> 350
            code == 0x96 -> 556
            code == 0xB0 -> 400
            code == 0xB1 || code == 0xD7 || code == 0xF7 -> 584
            code in 0xC0..0xDF -> if (LAT_U[code - 0xC0] == '*') 584 else ascii(LAT_U[code - 0xC0])
            code in 0xE0..0xFF -> if (LAT_L[code - 0xE0] == '/') 584 else ascii(LAT_L[code - 0xE0])
            code in 0xA0..0xBF -> if (code == 0xA0) 278 else 556
            else -> 556
        }
    }

    /** Width of a Unicode string in points. */
    fun width(s: String, bold: Boolean, size: Double): Double {
        var w = 0
        for (c in encode(s)) w += codeWidth(c.code, bold)
        return w * size / 1000.0
    }

    /** Greedy word wrap; over-long words are broken character-wise. */
    fun wrap(text: String, bold: Boolean, size: Double, maxW: Double): List<String> {
        val out = ArrayList<String>()
        for (para in text.replace("\r", "").split('\n')) {
            var line = ""
            for (word0 in para.split(' ')) {
                var word = word0
                if (word.isEmpty() && line.isNotEmpty()) continue
                while (width(word, bold, size) > maxW && word.length > 1) {
                    if (line.isNotEmpty()) { out.add(line); line = "" }
                    var k = 1
                    while (k < word.length && width(word.substring(0, k + 1), bold, size) <= maxW) k++
                    out.add(word.substring(0, k))
                    word = word.substring(k)
                }
                val cand = if (line.isEmpty()) word else "$line $word"
                if (line.isNotEmpty() && width(cand, bold, size) > maxW) { out.add(line); line = word } else line = cand
            }
            out.add(line)
        }
        return out
    }
}

/** Minimal PDF 1.4 writer (A4 portrait, Helvetica, vector charts). */
object PlannerPdf {
    private const val PW = 595.0
    private const val PH = 842.0
    private const val ML = 40.0
    private const val CW = PW - 2 * ML
    private const val BOTTOM = 52.0
    private const val TOP = PH - 40.0

    private class Doc {
        val pages = ArrayList<StringBuilder>()
        var sb = StringBuilder()
        var y = TOP

        init { pages.add(sb) }

        fun newPage() { sb = StringBuilder(); pages.add(sb); y = TOP }
        fun ensure(h: Double): Boolean { if (y - h < BOTTOM) { newPage(); return true }; return false }

        fun f(v: Double) = Num.fmt(v, 2)
        fun fill(c: Int) { sb.append(col((c shr 16) and 255)).append(' ').append(col((c shr 8) and 255)).append(' ').append(col(c and 255)).append(" rg\n") }
        fun stroke(c: Int) { sb.append(col((c shr 16) and 255)).append(' ').append(col((c shr 8) and 255)).append(' ').append(col(c and 255)).append(" RG\n") }
        private fun col(v: Int) = Num.fmt(v / 255.0, 3)
        fun width(w: Double) { sb.append(f(w)).append(" w\n") }
        fun rect(x: Double, y: Double, w: Double, h: Double, color: Int) { fill(color); sb.append(f(x)).append(' ').append(f(y)).append(' ').append(f(w)).append(' ').append(f(h)).append(" re f\n") }
        fun strokeRect(x: Double, y: Double, w: Double, h: Double, color: Int, lw: Double) { stroke(color); width(lw); sb.append(f(x)).append(' ').append(f(y)).append(' ').append(f(w)).append(' ').append(f(h)).append(" re S\n") }
        fun line(x0: Double, y0: Double, x1: Double, y1: Double, color: Int, lw: Double) { stroke(color); width(lw); sb.append(f(x0)).append(' ').append(f(y0)).append(" m ").append(f(x1)).append(' ').append(f(y1)).append(" l S\n") }
        fun polygon(xs: DoubleArray, ys: DoubleArray, color: Int) {
            if (xs.size < 3) return
            fill(color)
            for (i in xs.indices) sb.append(f(xs[i])).append(' ').append(f(ys[i])).append(if (i == 0) " m\n" else " l\n")
            sb.append("h f\n")
        }
        fun polyline(xs: DoubleArray, ys: DoubleArray, color: Int, lw: Double) {
            if (xs.size < 2) return
            stroke(color); width(lw)
            for (i in xs.indices) sb.append(f(xs[i])).append(' ').append(f(ys[i])).append(if (i == 0) " m\n" else " l\n")
            sb.append("S\n")
        }
        fun text(x: Double, y: Double, size: Double, bold: Boolean, s: String, color: Int = 0x222222) {
            if (s.isEmpty()) return
            fill(color)
            sb.append("BT /").append(if (bold) "F2 " else "F1 ").append(f(size)).append(" Tf ").append(f(x)).append(' ').append(f(y)).append(" Td ").append(PdfText.literal(s)).append(" Tj ET\n")
        }
        fun textRight(xr: Double, y: Double, size: Double, bold: Boolean, s: String, color: Int = 0x222222) =
            text(xr - PdfText.width(s, bold, size), y, size, bold, s, color)
        fun textCenter(xc: Double, y: Double, size: Double, bold: Boolean, s: String, color: Int = 0x222222) =
            text(xc - PdfText.width(s, bold, size) / 2, y, size, bold, s, color)
        fun textUp(x: Double, y: Double, size: Double, bold: Boolean, s: String, color: Int = 0x222222) {
            if (s.isEmpty()) return
            fill(color)
            sb.append("BT /").append(if (bold) "F2 " else "F1 ").append(f(size)).append(" Tf 0 1 -1 0 ").append(f(x)).append(' ').append(f(y)).append(" Tm ").append(PdfText.literal(s)).append(" Tj ET\n")
        }
        fun clipRect(x: Double, y: Double, w: Double, h: Double) { sb.append("q ").append(f(x)).append(' ').append(f(y)).append(' ').append(f(w)).append(' ').append(f(h)).append(" re W n\n") }
        fun restore() { sb.append("Q\n") }
    }

    fun render(report: PlannerReport): ByteArray {
        val d = Doc()
        // title block
        for (l in PdfText.wrap(report.title, true, 20.0, CW)) { d.y -= 22.0; d.text(ML, d.y, 20.0, true, l, 0x111111) }
        if (report.subtitle.isNotEmpty()) for (l in PdfText.wrap(report.subtitle, false, 11.0, CW)) { d.y -= 14.0; d.text(ML, d.y, 11.0, false, l, 0x444444) }
        if (report.generatedAt.isNotEmpty()) { d.y -= 13.0; d.text(ML, d.y, 9.0, false, report.generatedAt, 0x777777) }
        d.y -= 8.0
        d.line(ML, d.y, ML + CW, d.y, 0x1565C0, 1.5)
        d.y -= 14.0

        for (s in report.sections) section(d, s)
        report.profile?.let { profileBlock(d, it) }
        report.coverage?.let { coverageBlock(d, it) }
        if (report.points.isNotEmpty()) pointsBlock(d, report.points)
        if (report.notes.isNotEmpty()) notesBlock(d, report.notes)

        val total = d.pages.size
        for ((i, pg) in d.pages.withIndex()) {
            d.sb = pg
            d.line(ML, 40.0, ML + CW, 40.0, 0xBBBBBB, 0.5)
            val num = "${i + 1} / $total"
            val nw = PdfText.width(num, false, 8.0)
            val ft = PdfText.wrap(report.footer, false, 8.0, CW - nw - 16).firstOrNull() ?: ""
            d.text(ML, 28.0, 8.0, false, ft, 0x777777)
            d.textRight(ML + CW, 28.0, 8.0, false, num, 0x777777)
        }
        return assemble(d.pages.map { it.toString() }, report.title)
    }

    // ---- blocks ----

    private fun section(d: Doc, s: ReportSection) {
        val labelW = 170.0
        val pad = 3.0
        val lh = 11.0
        fun heading() {
            d.y -= 16.0
            d.rect(ML, d.y - 4.0, CW, 18.0, 0xE3EEF9)
            d.text(ML + 6, d.y + 1.0, 11.0, true, s.title, 0x0D47A1)
            d.y -= 6.0
        }
        val firstH = if (s.rows.isEmpty()) 0.0 else rowH(s.rows[0], labelW, lh, pad)
        d.ensure(30.0 + firstH)
        heading()
        var shade = false
        for (r in s.rows) {
            val lab = PdfText.wrap(r.label, true, 9.0, labelW - 2 * pad - 2)
            val vals = PdfText.wrap(r.value, false, 9.0, CW - labelW - 2 * pad - 2)
            val h = max(lab.size, vals.size) * lh + 2 * pad
            if (d.ensure(h)) { heading(); shade = false }
            if (shade) d.rect(ML, d.y - h, CW, h, 0xF5F5F5)
            shade = !shade
            var ty = d.y - pad - 8.0
            for (l in lab) { d.text(ML + pad + 1, ty, 9.0, true, l, 0x333333); ty -= lh }
            ty = d.y - pad - 8.0
            for (l in vals) { d.text(ML + labelW + pad, ty, 9.0, false, l, 0x111111); ty -= lh }
            d.y -= h
        }
        d.line(ML, d.y, ML + CW, d.y, 0xCCCCCC, 0.5)
        d.y -= 6.0
    }

    private fun rowH(r: ReportKeyValue, labelW: Double, lh: Double, pad: Double): Double {
        val a = PdfText.wrap(r.label, true, 9.0, labelW - 2 * pad - 2).size
        val b = PdfText.wrap(r.value, false, 9.0, CW - labelW - 2 * pad - 2).size
        return max(a, b) * lh + 2 * pad
    }

    private fun profileBlock(d: Doc, p: ReportProfile) {
        val h = 250.0
        d.ensure(h + 12)
        d.y -= 8.0
        val ox = ML
        val oyTop = d.y
        val left = 52.0; val right = 10.0; val top = 22.0; val bottom = 34.0
        val px0 = ox + left
        val pw = CW - left - right
        val ph = h - top - bottom
        val pyTop = oyTop - top
        val pyBot = pyTop - ph
        val c = ChartData(p)
        fun sx(v: Double) = px0 + (v - c.xMin) / (c.xMax - c.xMin) * pw
        fun sy(v: Double) = pyBot + (v - c.yMin) / (c.yMax - c.yMin) * ph
        val xt = c.xTicks(); val yt = c.yTicks()
        val xd = ChartData.decimalsFor(xt); val yd = ChartData.decimalsFor(yt)
        d.rect(px0, pyBot, pw, ph, 0xFFFFFF)
        for (t in yt) d.line(px0, sy(t), px0 + pw, sy(t), 0xE0E0E0, 0.4)
        for (t in xt) d.line(sx(t), pyBot, sx(t), pyTop, 0xE0E0E0, 0.4)
        if (c.valid) {
            val n = c.n
            d.clipRect(px0, pyBot, pw, ph)
            val fx = DoubleArray(2 * n); val fy = DoubleArray(2 * n)
            for (i in 0 until n) { fx[i] = sx(c.x[i]); fy[i] = sy(c.up[i]) }
            for (i in 0 until n) { fx[n + i] = sx(c.x[n - 1 - i]); fy[n + i] = sy(c.lo[n - 1 - i]) }
            d.polygon(fx, fy, 0xBFD9F2)
            val gx = DoubleArray(n + 2); val gy = DoubleArray(n + 2)
            for (i in 0 until n) { gx[i] = sx(c.x[i]); gy[i] = sy(c.ground[i]) }
            gx[n] = gx[n - 1]; gy[n] = pyBot; gx[n + 1] = gx[0]; gy[n + 1] = pyBot
            d.polygon(gx, gy, 0xB89F78)
            d.polyline(gx.copyOf(n), gy.copyOf(n), 0x6B5636, 0.8)
            val lx = DoubleArray(n) { sx(c.x[it]) }; val ly = DoubleArray(n) { sy(c.los[it]) }
            d.polyline(lx, ly, 0xD32F2F, 1.4)
            d.restore()
        }
        d.line(px0, pyBot, px0, pyTop, 0x222222, 0.8)
        d.line(px0, pyBot, px0 + pw, pyBot, 0x222222, 0.8)
        for (t in xt) {
            d.line(sx(t), pyBot, sx(t), pyBot - 3, 0x222222, 0.6)
            d.textCenter(sx(t), pyBot - 12, 7.0, false, Num.fmt(t, xd), 0x333333)
        }
        for (t in yt) {
            d.line(px0 - 3, sy(t), px0, sy(t), 0x222222, 0.6)
            d.textRight(px0 - 5, sy(t) - 2.5, 7.0, false, Num.fmt(t, yd), 0x333333)
        }
        d.textCenter(px0 + pw / 2, pyBot - 25, 8.0, false, p.xAxisLabel, 0x222222)
        d.textUp(ox + 9, pyBot + ph / 2 - PdfText.width(p.yAxisLabel, false, 8.0) / 2, 8.0, false, p.yAxisLabel, 0x222222)
        d.text(px0, pyTop + 6, 11.0, true, p.labelA, 0x111111)
        d.textRight(px0 + pw, pyTop + 6, 11.0, true, p.labelB, 0x111111)
        d.y = oyTop - h - 6
    }

    private fun coverageBlock(d: Doc, cov: ReportCoverage) {
        val size = 340.0
        d.ensure(size + 40)
        d.y -= 6.0
        d.text(ML, d.y - 10, 11.0, true, cov.title, 0x0D47A1)
        d.y -= 18.0
        val x0 = ML
        val yTop = d.y
        val yBot = yTop - size
        d.rect(x0, yBot, size, size, 0xF2F2F2)

        val xs = DoubleArray(cov.samples.size) { CoverageBins.xKm(cov, cov.samples[it].lon) }
        val ys = DoubleArray(cov.samples.size) { CoverageBins.yKm(cov, cov.samples[it].lat) }
        var half = cov.maxRangeKm
        if (!(half > 0)) {
            half = 0.0
            for (i in xs.indices) half = max(half, max(abs(xs[i]), abs(ys[i])))
            if (half <= 0) half = 1.0
        }
        half *= 1.05
        val scale = size / (2 * half)
        val cellKm = CoverageBins.cellKm(xs, ys)
        val side = max(1.2, cellKm * scale * 1.02)
        val cx = x0 + size / 2; val cy = yBot + size / 2

        d.clipRect(x0, yBot, size, size)
        val order = cov.samples.indices.filter { !cov.samples[it].marginDb.isNaN() }.groupBy { CoverageBins.index(cov, cov.samples[it].marginDb) }
        for (idx in order.keys.sorted().reversed()) {
            d.fill(CoverageBins.color(cov, idx))
            for (i in order[idx]!!) {
                d.sb.append(d.f(cx + xs[i] * scale - side / 2)).append(' ').append(d.f(cy + ys[i] * scale - side / 2)).append(' ')
                    .append(d.f(side)).append(' ').append(d.f(side)).append(" re f\n")
            }
        }
        // range ring
        if (cov.maxRangeKm > 0) {
            val r = cov.maxRangeKm * scale
            val k = 0.5523 * r
            d.stroke(0x555555); d.width(0.6)
            d.sb.append("[3 3] 0 d ").append(d.f(cx + r)).append(' ').append(d.f(cy)).append(" m ")
            d.sb.append(d.f(cx + r)).append(' ').append(d.f(cy + k)).append(' ').append(d.f(cx + k)).append(' ').append(d.f(cy + r)).append(' ').append(d.f(cx)).append(' ').append(d.f(cy + r)).append(" c ")
            d.sb.append(d.f(cx - k)).append(' ').append(d.f(cy + r)).append(' ').append(d.f(cx - r)).append(' ').append(d.f(cy + k)).append(' ').append(d.f(cx - r)).append(' ').append(d.f(cy)).append(" c ")
            d.sb.append(d.f(cx - r)).append(' ').append(d.f(cy - k)).append(' ').append(d.f(cx - k)).append(' ').append(d.f(cy - r)).append(' ').append(d.f(cx)).append(' ').append(d.f(cy - r)).append(" c ")
            d.sb.append(d.f(cx + k)).append(' ').append(d.f(cy - r)).append(' ').append(d.f(cx + r)).append(' ').append(d.f(cy - k)).append(' ').append(d.f(cx + r)).append(' ').append(d.f(cy)).append(" c S [] 0 d\n")
        }
        // centre marker
        d.rect(cx - 4, cy - 4, 8.0, 8.0, 0xFFFFFF)
        d.line(cx - 6, cy, cx + 6, cy, 0x000000, 1.2)
        d.line(cx, cy - 6, cx, cy + 6, 0x000000, 1.2)
        d.restore()
        d.strokeRect(x0, yBot, size, size, 0x444444, 0.8)

        // scale bar (about a fifth of the width)
        val target = 2 * half / 5
        val mag = ChartData.pow10(kotlin.math.floor(kotlin.math.log10(target)).toInt())
        val f = target / mag
        val barKm = (if (f < 1.5) 1.0 else if (f < 3.5) 2.0 else if (f < 7.5) 5.0 else 10.0) * mag
        val barPt = barKm * scale
        val by = yBot + 12
        d.rect(x0 + 8, by - 3, barPt + 6, 20.0, 0xFFFFFF)
        d.line(x0 + 11, by + 2, x0 + 11 + barPt, by + 2, 0x000000, 1.5)
        d.line(x0 + 11, by - 1, x0 + 11, by + 5, 0x000000, 1.0)
        d.line(x0 + 11 + barPt, by - 1, x0 + 11 + barPt, by + 5, 0x000000, 1.0)
        d.text(x0 + 11, by + 8, 7.0, false, Num.fmtTrim(barKm, 3) + " km", 0x000000)

        // legend
        val lx = x0 + size + 16
        var ly = yTop - 12
        for ((i, e) in cov.legend.withIndex()) {
            val lines = PdfText.wrap(e.first, false, 9.0, ML + CW - lx - 20)
            d.rect(lx, ly - 2, 12.0, 12.0, CoverageBins.color(cov, i))
            d.strokeRect(lx, ly - 2, 12.0, 12.0, 0x666666, 0.4)
            var ty = ly
            for (l in lines) { d.text(lx + 18, ty, 9.0, false, l, 0x222222); ty -= 11 }
            ly -= max(18.0, lines.size * 11.0 + 6)
        }
        d.y = yBot - 12
    }

    private fun pointsBlock(d: Doc, pts: List<ReportPoint>) {
        val w1 = 150.0; val w2 = 115.0
        val lh = 11.0; val pad = 3.0
        d.ensure(30.0)
        d.y -= 6.0
        var shade = false
        for (p in pts) {
            val n = PdfText.wrap(p.name, true, 9.0, w1 - 2 * pad)
            val coord = Num.fmt(p.lat, 5) + ", " + Num.fmt(p.lon, 5)
            val ds = PdfText.wrap(p.description, false, 9.0, CW - w1 - w2 - 2 * pad)
            val h = max(n.size, ds.size) * lh + 2 * pad
            if (d.ensure(h)) shade = false
            if (shade) d.rect(ML, d.y - h, CW, h, 0xF5F5F5)
            shade = !shade
            var ty = d.y - pad - 8
            for (l in n) { d.text(ML + pad, ty, 9.0, true, l, 0x111111); ty -= lh }
            d.text(ML + w1, d.y - pad - 8, 9.0, false, coord, 0x333333)
            ty = d.y - pad - 8
            for (l in ds) { d.text(ML + w1 + w2, ty, 9.0, false, l, 0x333333); ty -= lh }
            d.y -= h
        }
        d.line(ML, d.y, ML + CW, d.y, 0xCCCCCC, 0.5)
        d.y -= 8
    }

    private fun notesBlock(d: Doc, notes: List<String>) {
        d.ensure(30.0)
        d.y -= 6.0
        for (n in notes) {
            val lines = PdfText.wrap(n, false, 8.0, CW - 12)
            val h = lines.size * 10.0 + 3
            d.ensure(h)
            d.text(ML, d.y - 8, 8.0, false, "•", 0x555555)
            var ty = d.y - 8
            for (l in lines) { d.text(ML + 10, ty, 8.0, false, l, 0x555555); ty -= 10 }
            d.y -= h
        }
    }

    // ---- file structure ----

    private fun assemble(contents: List<String>, title: String): ByteArray {
        val np = contents.size
        // objects: 1 catalog, 2 pages, 3 F1, 4 F2, 5 encoding, 6 info, then (page, content) pairs
        val objs = ArrayList<String>()
        objs.add("<< /Type /Catalog /Pages 2 0 R >>")
        val kids = (0 until np).joinToString(" ") { "${7 + 2 * it} 0 R" }
        objs.add("<< /Type /Pages /Kids [$kids] /Count $np >>")
        objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica ${PdfText.widthsEntry(false)} /Encoding 5 0 R >>")
        objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold ${PdfText.widthsEntry(true)} /Encoding 5 0 R >>")
        objs.add("<< /Type /Encoding /BaseEncoding /WinAnsiEncoding /Differences [${PdfText.differences()}] >>")
        objs.add("<< /Title ${utf16Hex(title)} /Creator (Planer MT_SW) >>")
        for (i in 0 until np) {
            objs.add(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents ${8 + 2 * i} 0 R " +
                    "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> /ProcSet [/PDF /Text] >> >>",
            )
            val c = contents[i]
            objs.add("<< /Length ${c.length} >>\nstream\n$c\nendstream")
        }
        val out = StringBuilder()
        out.append("%PDF-1.4\n%").append(0xE2.toChar()).append(0xE3.toChar()).append(0xCF.toChar()).append(0xD3.toChar()).append('\n')
        val offs = IntArray(objs.size)
        for ((i, o) in objs.withIndex()) {
            offs[i] = out.length
            out.append(i + 1).append(" 0 obj\n").append(o).append("\nendobj\n")
        }
        val xref = out.length
        out.append("xref\n0 ").append(objs.size + 1).append('\n')
        out.append("0000000000 65535 f \n")
        for (o in offs) out.append(o.toString().padStart(10, '0')).append(" 00000 n \n")
        out.append("trailer\n<< /Size ").append(objs.size + 1).append(" /Root 1 0 R /Info 6 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n")
        val bytes = ByteArray(out.length)
        for (i in 0 until out.length) bytes[i] = out[i].code.toByte()
        return bytes
    }

    private fun utf16Hex(s: String): String {
        val h = "0123456789ABCDEF"
        val sb = StringBuilder("<FEFF")
        for (c in s) { val v = c.code; sb.append(h[(v shr 12) and 15]).append(h[(v shr 8) and 15]).append(h[(v shr 4) and 15]).append(h[v and 15]) }
        return sb.append('>').toString()
    }
}
