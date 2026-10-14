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

package org.meshtastic.feature.node.health

import kotlin.math.roundToInt

// A small PDF writer with no extra libraries. It uses the built-in Helvetica fonts; Polish letters are mapped to
// glyph names so they print correctly. Only text, lines and filled rectangles are needed for the summary.

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40
private const val TOP_Y = PAGE_HEIGHT - MARGIN
private const val BOTTOM_Y = MARGIN + 10
private const val BODY_SIZE = 10
private const val LINE_HEIGHT = 15
private const val LABEL_X = MARGIN + 8
private const val BAR_X = 230
private const val BAR_MAX_WIDTH = 220
private const val BAR_HEIGHT = 8
private const val VALUE_X = BAR_X + BAR_MAX_WIDTH + 8
private const val MAX_LABEL_CHARS = 34
private const val FIRST_CUSTOM_CODE = 128

private val POLISH_GLYPHS =
    listOf(
        'ą' to "aogonek",
        'ć' to "cacute",
        'ę' to "eogonek",
        'ł' to "lslash",
        'ń' to "nacute",
        'ś' to "sacute",
        'ź' to "zacute",
        'ż' to "zdotaccent",
        'Ą' to "Aogonek",
        'Ć' to "Cacute",
        'Ę' to "Eogonek",
        'Ł' to "Lslash",
        'Ń' to "Nacute",
        'Ś' to "Sacute",
        'Ź' to "Zacute",
        'Ż' to "Zdotaccent",
    )

private val POLISH_CODES: Map<Char, Char> =
    POLISH_GLYPHS.mapIndexed { index, pair -> pair.first to (FIRST_CUSTOM_CODE + index).toChar() }.toMap()

/** Converts text to the one-byte-per-character form used inside the PDF, escaping the PDF special characters. */
private fun pdfText(text: String): String = buildString {
    text.forEach { ch ->
        val mapped =
            POLISH_CODES[ch]
                ?: when {
                    ch.code in 32..126 || ch.code in 160..255 -> ch
                    ch == '—' || ch == '–' || ch == '−' -> '-'
                    ch == '█' -> '#'
                    else -> '?'
                }
        if (mapped == '(' || mapped == ')' || mapped == '\\') append('\\')
        append(mapped)
    }
}

/** Draws one page at a time; [y] counts down from the top of the page. */
private class PdfPages {
    val pages = mutableListOf<StringBuilder>()
    var y = TOP_Y
    private var page = StringBuilder()

    init {
        newPage()
    }

    fun newPage() {
        page = StringBuilder()
        pages += page
        y = TOP_Y
    }

    fun ensureSpace(height: Int) {
        if (y - height < BOTTOM_Y) newPage()
    }

    fun text(x: Int, size: Int, bold: Boolean, value: String, gray: Float = 0.1f) {
        page.append("$gray g BT /${if (bold) "F2" else "F1"} $size Tf $x $y Td (${pdfText(value)}) Tj ET\n")
    }

    fun rect(x: Int, bottom: Int, width: Int, height: Int, r: Float, g: Float, b: Float) {
        page.append("$r $g $b rg $x $bottom $width $height re f\n")
    }
}

private fun wrapLine(text: String, limit: Int): List<String> {
    if (text.length <= limit) return listOf(text)
    val result = mutableListOf<String>()
    var rest = text
    while (rest.length > limit) {
        val cut = rest.lastIndexOf(' ', limit).takeIf { it > 0 } ?: limit
        result += rest.substring(0, cut)
        rest = rest.substring(cut).trimStart()
    }
    return result + rest
}

private const val LINE_CHARS = 95

/** Renders the report as a multi-page A4 PDF. */
internal fun NetworkReport.toPdf(): ByteArray {
    val doc = PdfPages()
    doc.text(MARGIN, 20, true, title)
    doc.y -= LINE_HEIGHT + 4
    doc.text(MARGIN, BODY_SIZE, false, generated, gray = 0.45f)
    doc.y -= LINE_HEIGHT + 6

    sections.forEach { section ->
        val needed = LINE_HEIGHT * (section.lines.size + section.bars.size + 2)
        doc.ensureSpace(minOf(needed, PAGE_HEIGHT / 3))
        doc.rect(MARGIN, doc.y - 3, 4, 16, 0.85f, 0.65f, 0f)
        doc.text(MARGIN + 10, 13, true, section.title)
        doc.y -= LINE_HEIGHT + 4

        section.lines.forEach { line ->
            wrapLine("${line.label}: ${line.value}", LINE_CHARS).forEach { part ->
                doc.ensureSpace(LINE_HEIGHT)
                doc.text(LABEL_X, BODY_SIZE, false, part)
                doc.y -= LINE_HEIGHT
            }
        }

        val max = section.bars.maxOfOrNull { it.value } ?: 0f
        section.bars.forEach { bar ->
            doc.ensureSpace(LINE_HEIGHT)
            doc.text(LABEL_X, BODY_SIZE, false, bar.label.take(MAX_LABEL_CHARS))
            doc.rect(BAR_X, doc.y - 1, BAR_MAX_WIDTH, BAR_HEIGHT, 0.9f, 0.9f, 0.9f)
            val width = if (max > 0f) (bar.value / max * BAR_MAX_WIDTH).roundToInt().coerceIn(0, BAR_MAX_WIDTH) else 0
            if (width > 0) doc.rect(BAR_X, doc.y - 1, width, BAR_HEIGHT, 0.85f, 0.65f, 0f)
            doc.text(VALUE_X, BODY_SIZE, true, bar.text)
            doc.y -= LINE_HEIGHT
        }
        doc.y -= LINE_HEIGHT / 2
    }
    return assemblePdf(doc.pages.map { it.toString() })
}

/** Object numbers: 1 catalog, 2 page list, 3 encoding, 4 and 5 fonts, then a page and its content for each page. */
private fun assemblePdf(contents: List<String>): ByteArray {
    val out = StringBuilder("%PDF-1.4\n")
    val offsets = mutableListOf<Int>()

    fun obj(body: String) {
        offsets += out.length
        out.append("${offsets.size} 0 obj\n$body\nendobj\n")
    }

    val firstPageObj = 6
    val kids = contents.indices.joinToString(" ") { "${firstPageObj + it * 2} 0 R" }
    val differences = "$FIRST_CUSTOM_CODE " + POLISH_GLYPHS.joinToString(" ") { "/${it.second}" }

    obj("<< /Type /Catalog /Pages 2 0 R >>")
    obj("<< /Type /Pages /Kids [$kids] /Count ${contents.size} >>")
    obj("<< /Type /Encoding /BaseEncoding /WinAnsiEncoding /Differences [$differences] >>")
    obj("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding 3 0 R >>")
    obj("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding 3 0 R >>")
    contents.forEachIndexed { index, content ->
        val contentObj = firstPageObj + index * 2 + 1
        obj(
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_WIDTH $PAGE_HEIGHT] " +
                "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents $contentObj 0 R >>",
        )
        obj("<< /Length ${content.length} >>\nstream\n$content\nendstream")
    }

    val xrefAt = out.length
    out.append("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
    offsets.forEach { out.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
    out.append("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefAt\n%%EOF\n")
    return ByteArray(out.length) { out[it].code.toByte() }
}
