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
package org.meshtastic.core.ui.util

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
    fun widthsEntry(bold: Boolean): String {
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

/** Writes the PDF 1.4 file structure around finished page content streams (A4 portrait, Helvetica). */
object PdfFile {
    fun assemble(contents: List<String>, title: String, creator: String): ByteArray {
        val np = contents.size
        // objects: 1 catalog, 2 pages, 3 F1, 4 F2, 5 encoding, 6 info, then (page, content) pairs
        val objs = ArrayList<String>()
        objs.add("<< /Type /Catalog /Pages 2 0 R >>")
        val kids = (0 until np).joinToString(" ") { "${7 + 2 * it} 0 R" }
        objs.add("<< /Type /Pages /Kids [$kids] /Count $np >>")
        objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica ${PdfText.widthsEntry(false)} /Encoding 5 0 R >>")
        objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold ${PdfText.widthsEntry(true)} /Encoding 5 0 R >>")
        objs.add("<< /Type /Encoding /BaseEncoding /WinAnsiEncoding /Differences [${PdfText.differences()}] >>")
        objs.add("<< /Title ${utf16Hex(title)} /Creator ${utf16Hex(creator)} >>")
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
