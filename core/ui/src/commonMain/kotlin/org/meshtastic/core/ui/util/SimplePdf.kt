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

import kotlin.math.roundToLong

/** A plain A4 document made of a title and titled sections of text lines (no pictures), written without a library. */
object SimplePdf {
    private const val PW = 595.0
    private const val PH = 842.0
    private const val MARGIN = 40.0
    private const val BOTTOM = 48.0
    private const val TOP = PH - 44.0

    private fun f(v: Double) = ((v * 100).roundToLong() / 100.0).toString()

    private fun rgb(c: Int) =
        "${f(((c shr 16) and 0xFF) / 255.0)} ${f(((c shr 8) and 0xFF) / 255.0)} ${f((c and 0xFF) / 255.0)}"

    /** [sections] are (heading, lines); a long line is wrapped, a line starting with "• " keeps a hanging indent. */
    fun render(
        title: String,
        subtitle: String,
        sections: List<Pair<String, List<String>>>,
        footer: String,
        creator: String,
    ): ByteArray {
        val pages = ArrayList<StringBuilder>()
        var sb = StringBuilder().also { pages.add(it) }
        var y = TOP

        fun text(x: Double, baseline: Double, s: String, bold: Boolean, size: Double, color: Int) {
            sb.append(rgb(color)).append(" rg BT /F").append(if (bold) 2 else 1).append(' ').append(f(size))
                .append(" Tf ").append(f(x)).append(' ').append(f(baseline)).append(" Td ")
                .append(PdfText.literal(s)).append(" Tj ET\n")
        }

        fun need(height: Double) {
            if (y - height < BOTTOM) {
                sb = StringBuilder().also { pages.add(it) }
                y = TOP
            }
        }

        text(MARGIN, y - 16, title, true, 16.0, 0x222222)
        y -= 24.0
        text(MARGIN, y - 9, subtitle, false, 9.0, 0x666666)
        y -= 26.0

        for ((heading, lines) in sections) {
            need(40.0)
            text(MARGIN, y - 11, heading, true, 11.0, 0x1A5FB4)
            y -= 18.0
            for (line in lines) {
                val bullet = line.startsWith("• ")
                val indent = if (bullet) 10.0 else 0.0
                val wrapped = PdfText.wrap(if (bullet) line.removePrefix("• ") else line, false, 10.0, PW - 2 * MARGIN - indent)
                wrapped.forEachIndexed { i, part ->
                    need(14.0)
                    if (bullet && i == 0) text(MARGIN, y - 10, "•", false, 10.0, 0x222222)
                    text(MARGIN + indent, y - 10, part, false, 10.0, 0x222222)
                    y -= 13.5
                }
            }
            y -= 10.0
        }
        if (footer.isNotBlank()) {
            PdfText.wrap(footer, false, 8.0, PW - 2 * MARGIN).forEach {
                need(12.0)
                text(MARGIN, y - 8, it, false, 8.0, 0x666666)
                y -= 11.0
            }
        }
        return PdfFile.assemble(pages.map { it.toString() }, title, creator)
    }
}
