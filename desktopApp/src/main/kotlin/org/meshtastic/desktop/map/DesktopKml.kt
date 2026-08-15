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
package org.meshtastic.desktop.map

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private const val KML_COLOR_HEX_LENGTH = 8
private const val KML_COLOR_RADIX = 16
private const val BYTE_MASK = 0xFFL
private const val SHIFT_24 = 24
private const val SHIFT_16 = 16
private const val SHIFT_8 = 8

/**
 * Parses a local `.kml` file into [ParsedFeature]s, sharing [ParsedGeometry]/[FeatureStyle] with the GeoJSON parser.
 * Supports Point/LineString/Polygon/MultiGeometry placemarks with inline or `styleUrl`-referenced `<Style>`
 * (LineStyle + PolyStyle). KMZ (zipped KML) is not supported in v1 — plain `.kml` only.
 */
fun parseKmlFile(file: File): List<ParsedFeature> {
    val factory =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            // Harden against XXE for untrusted files: disable DTDs. Best-effort — some JAXP providers don't support
            // this feature name, in which case we still proceed with the (safer-by-default on modern JDKs) parser.
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            isExpandEntityReferences = false
        }
    val doc = factory.newDocumentBuilder().parse(file)
    doc.documentElement.normalize()

    val styles = collectStyles(doc.documentElement)
    val placemarks = doc.getElementsByTagName("Placemark")
    val features = mutableListOf<ParsedFeature>()
    for (i in 0 until placemarks.length) {
        val placemark = placemarks.item(i) as? Element ?: continue
        val style = resolveStyle(placemark, styles)
        parsePlacemarkGeometries(placemark).forEach { geometry -> features.add(ParsedFeature(geometry, style)) }
    }
    return features
}

private fun collectStyles(root: Element): Map<String, FeatureStyle> {
    val styleNodes = root.getElementsByTagName("Style")
    val styles = mutableMapOf<String, FeatureStyle>()
    for (i in 0 until styleNodes.length) {
        val styleEl = styleNodes.item(i) as? Element ?: continue
        val id = styleEl.getAttribute("id").takeIf { it.isNotBlank() } ?: continue
        styles[id] = parseStyleElement(styleEl)
    }
    return styles
}

private fun resolveStyle(placemark: Element, styles: Map<String, FeatureStyle>): FeatureStyle {
    val inline = placemark.directChild("Style")?.let(::parseStyleElement)
    if (inline != null) return inline
    val styleUrl = placemark.directChild("styleUrl")?.textContent?.trim()?.removePrefix("#")
    return styleUrl?.let { styles[it] } ?: FeatureStyle()
}

private fun parseStyleElement(styleEl: Element): FeatureStyle {
    val lineStyle = styleEl.directChild("LineStyle")
    val polyStyle = styleEl.directChild("PolyStyle")
    val strokeColor = lineStyle?.directChild("color")?.textContent?.let(::parseKmlColorArgb)
    val strokeWidth = lineStyle?.directChild("width")?.textContent?.toFloatOrNull()
    val fillFlag = polyStyle?.directChild("fill")?.textContent?.trim()
    val fillColor =
        if (fillFlag == "0") {
            0x00000000L // explicit "no fill" — fully transparent rather than falling back to a default fill color
        } else {
            polyStyle?.directChild("color")?.textContent?.let(::parseKmlColorArgb)
        }
    return FeatureStyle(strokeColorArgb = strokeColor, fillColorArgb = fillColor, strokeWidth = strokeWidth)
}

private fun parsePlacemarkGeometries(placemark: Element): List<ParsedGeometry> {
    val geometries = mutableListOf<ParsedGeometry>()
    placemark.directChild("Point")?.let { geometries.addAll(parsePointGeometry(it)) }
    placemark.directChild("LineString")?.let { geometries.addAll(parseLineStringGeometry(it)) }
    placemark.directChild("Polygon")?.let { geometries.add(parseKmlPolygon(it)) }
    placemark.directChild("MultiGeometry")?.let { multiEl ->
        multiEl.directChildren("Polygon").forEach { geometries.add(parseKmlPolygon(it)) }
        multiEl.directChildren("LineString").forEach { geometries.addAll(parseLineStringGeometry(it)) }
        multiEl.directChildren("Point").forEach { geometries.addAll(parsePointGeometry(it)) }
    }
    return geometries
}

private fun parsePointGeometry(pointEl: Element): List<ParsedGeometry> =
    pointEl.directChild("coordinates")?.textContent?.let(::parseKmlCoordinate)?.let {
        listOf(ParsedGeometry.Point(it.first, it.second))
    } ?: emptyList()

private fun parseLineStringGeometry(lineEl: Element): List<ParsedGeometry> =
    lineEl.directChild("coordinates")?.textContent?.let(::parseKmlCoordinateList)?.let {
        listOf(ParsedGeometry.LineString(it))
    } ?: emptyList()

private fun parseKmlPolygon(polyEl: Element): ParsedGeometry.Polygon {
    val rings = mutableListOf<List<Pair<Double, Double>>>()
    polyEl.directChild("outerBoundaryIs")?.directChild("LinearRing")?.directChild("coordinates")?.textContent?.let {
        rings.add(parseKmlCoordinateList(it))
    }
    polyEl.directChildren("innerBoundaryIs").forEach { inner ->
        inner.directChild("LinearRing")?.directChild("coordinates")?.textContent?.let {
            rings.add(parseKmlCoordinateList(it))
        }
    }
    return ParsedGeometry.Polygon(rings)
}

/** KML `<coordinates>` is whitespace-separated "lon,lat[,alt]" tuples. */
private fun parseKmlCoordinateList(raw: String): List<Pair<Double, Double>> =
    raw.trim().split(Regex("\\s+")).mapNotNull(::parseKmlCoordinate)

private fun parseKmlCoordinate(raw: String): Pair<Double, Double>? {
    val parts = raw.trim().split(",")
    if (parts.size < 2) return null
    val lon = parts[0].toDoubleOrNull() ?: return null
    val lat = parts[1].toDoubleOrNull() ?: return null
    return lon to lat
}

/**
 * KML colors are 8-hex-char `aabbggrr` (alpha, blue, green, red) — a different channel order than CSS/ARGB — so this
 * reassembles them into the packed 0xAARRGGBB [Long] the rest of the layer-import pipeline uses.
 */
private fun parseKmlColorArgb(raw: String): Long? {
    val hex = raw.trim()
    if (hex.length != KML_COLOR_HEX_LENGTH) return null
    return try {
        val full = hex.toLong(KML_COLOR_RADIX)
        val alpha = (full shr SHIFT_24) and BYTE_MASK
        val blue = (full shr SHIFT_16) and BYTE_MASK
        val green = (full shr SHIFT_8) and BYTE_MASK
        val red = full and BYTE_MASK
        (alpha shl SHIFT_24) or (red shl SHIFT_16) or (green shl SHIFT_8) or blue
    } catch (e: NumberFormatException) {
        null
    }
}

/**
 * First direct child element named [tagName], or null. Unlike [Element.getElementsByTagName], this does not descend
 * into nested elements — needed because e.g. a `Placemark` inside a `Folder` inside a `Document` would otherwise pick
 * up a same-named tag from a sibling Placemark's subtree.
 */
private fun Element.directChild(tagName: String): Element? {
    val children = childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == tagName) return child as Element
    }
    return null
}

private fun Element.directChildren(tagName: String): List<Element> {
    val result = mutableListOf<Element>()
    val children = childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == tagName) result.add(child as Element)
    }
    return result
}
