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
package org.meshtastic.feature.messaging.ui.translator

import kotlin.math.roundToLong
import org.meshtastic.core.ui.util.JsonText
import org.meshtastic.core.ui.util.PdfFile
import org.meshtastic.core.ui.util.PdfText
import org.meshtastic.feature.messaging.translation.AssistantActions
import org.meshtastic.feature.messaging.translation.AssistantClock
import org.meshtastic.feature.messaging.translation.AssistantLine

/** One bubble of the conversation, prepared for saving. */
data class ExportEntry(
    val fromUser: Boolean,
    val text: String,
    val note: String?,
    val buttons: List<String>,
    val at: Long = 0L,
    val tookMs: Long = 0L,
)

/** The file kinds the conversation can be saved as. */
enum class ExportFormat(val label: String, val extension: String, val mime: String) {
    PDF("PDF", ".pdf", "application/pdf"),
    MARKDOWN("Markdown", ".md", "text/markdown"),
    TEXT("TXT", ".txt", "text/plain"),
    HTML("HTML", ".html", "text/html"),
    JSON("JSON", ".json", "application/json"),
}

/** Turns the conversation into plain text, Markdown, HTML or a PDF that looks like the chat. */
object AssistantExport {
    fun entries(lines: List<AssistantLine>): List<ExportEntry> = lines.filter { it.text.isNotBlank() }.map { line ->
        val (shown, actions) = AssistantActions.parse(line.text)
        val web = AssistantActions.web(line.text)
        ExportEntry(
            fromUser = line.fromUser,
            text = shown,
            note = web?.let { it.note.ifBlank { it.queries } }?.takeIf { it.isNotBlank() },
            buttons = actions.map { it.caption.ifBlank { it.label } },
            at = line.at,
            tookMs = line.tookMs,
        )
    }

    fun text(entries: List<ExportEntry>, you: String, assistant: String): String = entries.joinToString("\n\n") { e ->
        buildString {
            append(if (e.fromUser) you else assistant).append(stamp(e)).append(": ").append(e.text)
            e.note?.let { append("\n(").append(it).append(")") }
            e.buttons.forEach { append("\n[").append(it).append("]") }
        }
    }

    /**
     * The conversation as structured data, the easiest form for a program (or a reviewer) to read: who said what, in
     * order, with the web note and the buttons of each answer, and the whole text exactly as written.
     */
    fun json(entries: List<ExportEntry>, title: String, stamp: String): String =
        buildString {
            append("{\n  \"title\": ").append(q(title)).append(",\n  \"saved\": ").append(q(stamp))
            append(",\n  \"messages\": [")
            entries.forEachIndexed { i, e ->
                append(if (i == 0) "\n" else ",\n")
                append("    {\"n\": ").append(i + 1).append(", \"role\": ").append(q(if (e.fromUser) "user" else "assistant"))
                if (e.at > 0L) append(", \"").append(if (e.fromUser) "sent" else "received").append("\": ").append(q(AssistantClock.clock(e.at)))
                if (!e.fromUser && e.tookMs > 0L) append(", \"answer_seconds\": ").append(AssistantClock.seconds(e.tookMs))
                append(", \"text\": ").append(q(e.text))
                e.note?.let { append(", \"web\": ").append(q(it)) }
                if (e.buttons.isNotEmpty()) append(", \"buttons\": [").append(e.buttons.joinToString(", ") { q(it) }).append("]")
                append("}")
            }
            append(if (entries.isEmpty()) "]\n}\n" else "\n  ]\n}\n")
        }

    private fun q(s: String): String = JsonText.quote(s)

    /** " (12:03:17)" for a sent question, " (12:03:29, 12.1 s)" for a received answer; empty when the time is not known. */
    private fun stamp(e: ExportEntry): String =
        if (e.at <= 0L) "" else " (" + AssistantClock.clock(e.at) + (if (!e.fromUser && e.tookMs > 0L) ", " + AssistantClock.seconds(e.tookMs) + " s" else "") + ")"

    fun markdown(entries: List<ExportEntry>, title: String, stamp: String, you: String, assistant: String): String =
        buildString {
            append("# ").append(title).append("\n\n_").append(stamp).append("_\n\n")
            entries.forEach { e ->
                append("**").append(if (e.fromUser) you else assistant).append(stamp(e)).append(":**\n\n")
                append(if (e.fromUser) e.text.lines().joinToString("\n") { "> $it" } else e.text).append("\n\n")
                e.note?.let { append("_").append(it).append("_\n\n") }
                e.buttons.forEach { append("- [").append(it).append("]\n") }
                if (e.buttons.isNotEmpty()) append('\n')
            }
        }

    fun html(entries: List<ExportEntry>, title: String, stamp: String, you: String, assistant: String): String =
        buildString {
            append("<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
            append("<title>").append(esc(title)).append("</title><style>")
            append("body{font-family:sans-serif;max-width:760px;margin:16px auto;padding:0 16px;color:#222}")
            append(".b{border-radius:14px;padding:10px 14px;margin:10px 0;white-space:pre-wrap}")
            append(".u{background:#e3e8ee;margin-left:12%}.a{background:#f1f1f1;margin-right:4%}")
            append(".l{font-size:12px;font-weight:bold;color:#555}.n{font-size:12px;color:#555;margin-top:6px}")
            append(".k{font-size:13px;color:#1a5fb4;margin-top:6px}</style></head><body>")
            append("<h2>").append(esc(title)).append("</h2><p>").append(esc(stamp)).append("</p>")
            entries.forEach { e ->
                append("<div class=\"b ").append(if (e.fromUser) "u" else "a").append("\"><div class=\"l\">")
                append(esc(if (e.fromUser) you else assistant)).append("</div>").append(esc(e.text))
                e.note?.let { append("<div class=\"n\">").append(esc(it)).append("</div>") }
                e.buttons.forEach { append("<div class=\"k\">[").append(esc(it)).append("]</div>") }
                append("</div>")
            }
            append("</body></html>")
        }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    // ---- PDF ----

    private const val PW = 595.0
    private const val PH = 842.0
    private const val MARGIN = 40.0
    private const val BOTTOM = 44.0
    private const val TOP = PH - 44.0
    private const val PAD = 8.0
    private const val GAP = 8.0

    private class Piece(val text: String, val size: Double, val bold: Boolean, val color: Int) {
        val height get() = size * 1.35
    }

    private fun f(v: Double) = ((v * 100).roundToLong() / 100.0).toString()

    private fun rgb(c: Int) =
        "${f(((c shr 16) and 255) / 255.0)} ${f(((c shr 8) and 255) / 255.0)} ${f((c and 255) / 255.0)}"

    fun pdf(entries: List<ExportEntry>, title: String, stamp: String, you: String, assistant: String): ByteArray {
        val pages = ArrayList<StringBuilder>()
        var sb = StringBuilder().also { pages.add(it) }
        var y = TOP

        fun newPage() {
            sb = StringBuilder().also { pages.add(it) }
            y = TOP
        }

        fun text(x: Double, baseline: Double, p: Piece) {
            sb.append(rgb(p.color)).append(" rg BT /F").append(if (p.bold) 2 else 1).append(' ').append(f(p.size))
                .append(" Tf ").append(f(x)).append(' ').append(f(baseline)).append(" Td ")
                .append(PdfText.literal(p.text)).append(" Tj ET\n")
        }

        text(MARGIN, y - 14, Piece(title, 14.0, true, 0x222222))
        y -= 20.0
        text(MARGIN, y - 9, Piece(stamp, 9.0, false, 0x666666))
        y -= 22.0

        for (e in entries) {
            val width = if (e.fromUser) 400.0 else 470.0
            val inner = width - 2 * PAD
            val pieces = ArrayList<Piece>()
            pieces.add(Piece(if (e.fromUser) you else assistant, 8.0, true, 0x666666))
            PdfText.wrap(e.text, false, 10.0, inner).forEach { pieces.add(Piece(it, 10.0, false, 0x222222)) }
            e.note?.let { n -> PdfText.wrap(n, false, 8.0, inner).forEach { pieces.add(Piece(it, 8.0, false, 0x666666)) } }
            e.buttons.forEach { b ->
                PdfText.wrap("[ $b ]", false, 9.0, inner).forEach { pieces.add(Piece(it, 9.0, false, 0x1A5FB4)) }
            }
            val x = if (e.fromUser) PW - MARGIN - width else MARGIN
            var i = 0
            while (i < pieces.size) {
                var avail = y - BOTTOM - 2 * PAD
                if (avail < pieces[i].height) {
                    newPage()
                    avail = y - BOTTOM - 2 * PAD
                }
                var h = 0.0
                val start = i
                while (i < pieces.size && (i == start || h + pieces[i].height <= avail)) {
                    h += pieces[i].height
                    i++
                }
                val boxH = h + 2 * PAD
                sb.append(rgb(if (e.fromUser) 0xE3E8EE else 0xF1F1F1)).append(" rg ").append(f(x)).append(' ')
                    .append(f(y - boxH)).append(' ').append(f(width)).append(' ').append(f(boxH)).append(" re f\n")
                var base = y - PAD
                for (k in start until i) {
                    base -= pieces[k].size * 1.05
                    text(x + PAD, base, pieces[k])
                    base -= pieces[k].height - pieces[k].size * 1.05
                }
                y -= boxH + GAP
            }
        }
        return PdfFile.assemble(pages.map { it.toString() }, title, "MT_SW")
    }
}
