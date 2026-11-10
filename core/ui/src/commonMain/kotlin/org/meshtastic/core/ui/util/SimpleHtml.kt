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
package org.meshtastic.core.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

/**
 * Small HTML to [AnnotatedString] converter for the computer and iPhone/iPad apps (Android has its own). Understands
 * the tags the app's texts use: `b`, `strong`, `i`, `em`, `u`, `br`, `p` and `a href`, plus the common entities. Any
 * other tag is dropped and its text kept.
 */
fun simpleAnnotatedStringFromHtml(html: String, linkStyles: TextLinkStyles?): AnnotatedString = buildAnnotatedString {
    val tag = Regex("<(/?)([a-zA-Z0-9]+)([^>]*)>")
    val styles = ArrayList<SpanStyle>()
    var position = 0
    var linkUrl: String? = null

    fun appendText(raw: String) {
        if (raw.isEmpty()) return
        val text = decodeEntities(raw)
        val url = linkUrl
        val style = styles.fold(SpanStyle()) { acc, s -> acc.merge(s) }
        withStyle(style) {
            if (url != null) withLink(LinkAnnotation.Url(url, linkStyles)) { append(text) } else append(text)
        }
    }

    for (match in tag.findAll(html)) {
        appendText(html.substring(position, match.range.first))
        position = match.range.last + 1
        val closing = match.groupValues[1] == "/"
        val name = match.groupValues[2].lowercase()
        when (name) {
            "b", "strong" -> if (closing) styles.removeLastOrNull() else styles += SpanStyle(fontWeight = FontWeight.Bold)
            "i", "em" -> if (closing) styles.removeLastOrNull() else styles += SpanStyle(fontStyle = FontStyle.Italic)
            "u" -> if (closing) styles.removeLastOrNull() else styles += SpanStyle(textDecoration = TextDecoration.Underline)
            "br" -> append("\n")
            "p" -> if (closing) append("\n\n")
            "a" ->
                linkUrl =
                    if (closing) null else Regex("href\\s*=\\s*[\"']([^\"']*)[\"']").find(match.groupValues[3])?.groupValues?.get(1)
        }
    }
    appendText(html.substring(position))
}

private fun decodeEntities(text: String): String = text
    .replace("&nbsp;", " ")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&apos;", "'")
    .replace("&amp;", "&")
