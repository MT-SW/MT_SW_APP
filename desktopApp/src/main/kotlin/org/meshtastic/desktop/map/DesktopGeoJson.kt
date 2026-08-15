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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A parsed, renderer-agnostic geometry — (lon, lat) pairs throughout, matching GeoJSON's coordinate order. */
sealed interface ParsedGeometry {
    data class Point(val lon: Double, val lat: Double) : ParsedGeometry

    data class LineString(val points: List<Pair<Double, Double>>) : ParsedGeometry

    /** [rings] first entry is the outer boundary; any further entries are holes (not yet rendered as holes in v1). */
    data class Polygon(val rings: List<List<Pair<Double, Double>>>) : ParsedGeometry

    data class MultiPolygon(val polygons: List<Polygon>) : ParsedGeometry
}

/** Resolved mapbox-simplestyle properties. ARGB colors packed as [Long] (0xAARRGGBB) so callers can build Compose
 * `Color` directly via `Color(argb.toInt())`. Null fields mean "renderer picks its own default". */
data class FeatureStyle(val strokeColorArgb: Long? = null, val fillColorArgb: Long? = null, val strokeWidth: Float? = null)

data class ParsedFeature(val geometry: ParsedGeometry, val style: FeatureStyle)

private val geoJsonParser = Json { ignoreUnknownKeys = true }

private const val MIN_POINT_COORDS = 2

/** Parses a GeoJSON document — a bare geometry, a single `Feature`, or a `FeatureCollection` — into [ParsedFeature]s. */
fun parseGeoJson(text: String): List<ParsedFeature> {
    val root = geoJsonParser.parseToJsonElement(text).jsonObject
    return when (root["type"]?.jsonPrimitive?.contentOrNull) {
        "FeatureCollection" -> root["features"]?.jsonArray.orEmpty().mapNotNull { parseFeature(it.jsonObject) }
        "Feature" -> listOfNotNull(parseFeature(root))
        else -> parseGeometry(root)?.let { listOf(ParsedFeature(it, FeatureStyle())) }.orEmpty()
    }
}

private fun parseFeature(obj: JsonObject): ParsedFeature? {
    val geometry = obj["geometry"]?.jsonObject?.let(::parseGeometry) ?: return null
    return ParsedFeature(geometry, parseStyle(obj["properties"]?.jsonObject))
}

private fun parseGeometry(obj: JsonObject): ParsedGeometry? {
    val coords = obj["coordinates"]?.jsonArray ?: return null
    return when (obj["type"]?.jsonPrimitive?.contentOrNull) {
        "Point" -> coords.toLonLat()?.let { ParsedGeometry.Point(it.first, it.second) }
        "LineString" -> ParsedGeometry.LineString(coords.toLonLatList())
        "Polygon" -> ParsedGeometry.Polygon(coords.toRings())
        "MultiPolygon" -> ParsedGeometry.MultiPolygon(coords.map { ParsedGeometry.Polygon(it.jsonArray.toRings()) })
        // MultiPoint, MultiLineString, GeometryCollection: not supported in v1.
        else -> null
    }
}

private fun JsonArray.toLonLat(): Pair<Double, Double>? {
    if (size < MIN_POINT_COORDS) return null
    val lon = this[0].jsonPrimitive.doubleOrNull ?: return null
    val lat = this[1].jsonPrimitive.doubleOrNull ?: return null
    return lon to lat
}

private fun JsonArray.toLonLatList(): List<Pair<Double, Double>> = mapNotNull { it.jsonArray.toLonLat() }

private fun JsonArray.toRings(): List<List<Pair<Double, Double>>> = map { it.jsonArray.toLonLatList() }

private fun parseStyle(properties: JsonObject?): FeatureStyle {
    properties ?: return FeatureStyle()
    val strokeRaw = properties["stroke"]?.jsonPrimitive?.contentOrNull ?: properties["color"]?.jsonPrimitive?.contentOrNull
    val fillRaw = properties["fill"]?.jsonPrimitive?.contentOrNull ?: properties["color"]?.jsonPrimitive?.contentOrNull
    val fillOpacity = properties["fill-opacity"]?.jsonPrimitive?.floatOrNull
    val strokeWidth = properties["stroke-width"]?.jsonPrimitive?.floatOrNull
    val strokeColor = strokeRaw?.let(::parseCssColorArgb)
    val fillColor = fillRaw?.let(::parseCssColorArgb)?.let { color -> fillOpacity?.let { color.withAlpha(it) } ?: color }
    return FeatureStyle(strokeColorArgb = strokeColor, fillColorArgb = fillColor, strokeWidth = strokeWidth)
}

private const val OPAQUE_ALPHA = 255L
private const val HEX_RADIX = 16
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val RGB_MASK = 0x00FFFFFFL
private const val HEX_RGB_LENGTH = 6
private const val HEX_ARGB_LENGTH = 8
private const val MIN_RGB_COMPONENTS = 3
private const val RGBA_COMPONENT_COUNT = 4

/** Overrides this ARGB color's alpha channel with [opacity] (0f–1f), keeping its RGB. */
private fun Long.withAlpha(opacity: Float): Long {
    val alpha = (opacity.coerceIn(0f, 1f) * OPAQUE_ALPHA).toLong() shl ALPHA_SHIFT
    return alpha or (this and RGB_MASK)
}

/** Parses a hex (`#RRGGBB`/`#AARRGGBB`) or `rgb()`/`rgba()` CSS color into a packed 0xAARRGGBB [Long]; null if
 * unparseable or an unsupported form (named colors like `"red"` aren't handled in v1). */
private fun parseCssColorArgb(raw: String): Long? {
    val value = raw.trim()
    return try {
        when {
            value.startsWith("#") -> {
                val hex = value.removePrefix("#")
                when (hex.length) {
                    HEX_RGB_LENGTH -> (OPAQUE_ALPHA shl ALPHA_SHIFT) or hex.toLong(HEX_RADIX)
                    HEX_ARGB_LENGTH -> hex.toLong(HEX_RADIX)
                    else -> null
                }
            }

            value.startsWith("rgb", ignoreCase = true) -> {
                val parts = value.substringAfter('(').substringBefore(')').split(',').map { it.trim() }
                if (parts.size < MIN_RGB_COMPONENTS) return null
                val r = parts[0].toLong()
                val g = parts[1].toLong()
                val b = parts[2].toLong()
                val a = if (parts.size >= RGBA_COMPONENT_COUNT) (parts[3].toFloat() * OPAQUE_ALPHA).toLong() else OPAQUE_ALPHA
                (a shl ALPHA_SHIFT) or (r shl RED_SHIFT) or (g shl GREEN_SHIFT) or b
            }

            else -> null
        }
    } catch (e: NumberFormatException) {
        null
    }
}
