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
package org.meshtastic.core.ui.image

import coil3.ImageLoader
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer
import okio.FileSystem

/**
 * Makes SVG pictures that colour their shapes with CSS classes (`.cls-1{fill:#fff}`, as the device pictures from the
 * flasher site do) draw correctly on Desktop and iPhone. Their SVG renderer ignores the `<style>` block, so shapes that
 * should have no fill or only an outline came out as solid black. The CSS values are copied onto each shape as plain
 * attributes before the picture goes to [delegate]. Other pictures pass through unchanged.
 */
class CssInliningDecoderFactory(private val delegate: Decoder.Factory) : Decoder.Factory {
    override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
        val looksLikeSvg =
            result.mimeType?.contains("svg", ignoreCase = true) == true ||
                (result.mimeType == null && result.source.source().peek().readUtf8(SNIFF_BYTES).contains("<svg"))
        if (!looksLikeSvg) return delegate.create(result, options, imageLoader)
        val bytes = result.source.source().readByteArray()
        val fixed = inlineSvgCss(bytes.decodeToString()).encodeToByteArray()
        val newResult =
            SourceFetchResult(
                source = ImageSource(source = Buffer().write(fixed), fileSystem = FileSystem.SYSTEM),
                mimeType = result.mimeType,
                dataSource = result.dataSource,
            )
        val inner = delegate.create(newResult, options, imageLoader) ?: return null
        return Decoder { inner.decode() }
    }
}

private const val SNIFF_BYTES = 512L

private val STYLE_BLOCK = Regex("<style[^>]*>(.*?)</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
private val CSS_RULE = Regex("([^{}]+)\\{([^{}]*)\\}")
private val SHAPE_TAG = Regex("<(path|rect|circle|ellipse|line|polyline|polygon|g|text|use)\\b([^>]*)>")
private val CLASS_ATTR = Regex("\\sclass\\s*=\\s*\"([^\"]*)\"")

/** Copies simple `.class{prop:value}` rules from the `<style>` block onto the shapes that use the class. */
internal fun inlineSvgCss(svg: String): String {
    val css = STYLE_BLOCK.findAll(svg).joinToString("\n") { it.groupValues[1] }
    if (css.isBlank()) return svg
    val byClass = HashMap<String, MutableMap<String, String>>()
    for (rule in CSS_RULE.findAll(css)) {
        val props =
            rule.groupValues[2]
                .split(';')
                .mapNotNull { decl ->
                    val i = decl.indexOf(':')
                    if (i <= 0) null else decl.substring(0, i).trim() to decl.substring(i + 1).trim()
                }
                .filter { it.first.isNotEmpty() && it.second.isNotEmpty() }
        for (selector in rule.groupValues[1].split(',')) {
            val name = selector.trim()
            if (name.startsWith('.') && name.drop(1).all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
                byClass.getOrPut(name.drop(1)) { LinkedHashMap() }.putAll(props)
            }
        }
    }
    if (byClass.isEmpty()) return svg
    return SHAPE_TAG.replace(svg) { m ->
        val tag = m.groupValues[1]
        var attrs = m.groupValues[2]
        val classes = CLASS_ATTR.find(attrs)?.groupValues?.get(1)?.split(' ')?.filter { it.isNotEmpty() }.orEmpty()
        val merged = LinkedHashMap<String, String>()
        classes.forEach { c -> byClass[c]?.let(merged::putAll) }
        if (merged.isEmpty()) {
            m.value
        } else {
            // A CSS rule wins over a plain attribute of the same name, so the old attribute is replaced.
            merged.keys.forEach { prop ->
                attrs = attrs.replace(Regex("\\s$prop\\s*=\\s*\"[^\"]*\""), "")
            }
            val added = merged.entries.joinToString("") { (k, v) -> " $k=\"$v\"" }
            "<$tag$added$attrs>"
        }
    }
}
