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
package org.meshtastic.feature.map.planner.ui

import org.meshtastic.feature.map.planner.export.Num
import kotlin.math.absoluteValue
import kotlin.math.max

/**
 * Pure, locale independent parsing and formatting helpers of the planner forms (no Compose, unit tested).
 *
 * Parsing accepts what people really type: a comma or a dot as the decimal mark, spaces, hemisphere letters
 * (N/S/E/W) before or after the number and degrees-minutes-seconds. Formatting always uses a dot.
 */
object PlannerInput {
    private const val LAT_LIMIT = 90.0
    private const val LON_LIMIT = 180.0
    private const val SIXTY = 60.0
    private const val SECONDS_PER_HOUR = 3600.0
    private const val NUMBER_CHARS = "+-.eE"

    /**
     * Parses a decimal number. `12,5` and `12.5` give 12.5; when both marks are present the last one is the decimal
     * mark (`1.234,5` is 1234.5). Returns null for empty, malformed, NaN and infinite input.
     */
    fun parseNumber(text: String): Double? {
        val s0 = text.filterNot { it.isWhitespace() || it == ' ' || it == ' ' }
        if (s0.isEmpty()) return null
        val lastDot = s0.lastIndexOf('.')
        val lastComma = s0.lastIndexOf(',')
        val s =
            when {
                lastDot >= 0 && lastComma >= 0 -> {
                    val dec = max(lastDot, lastComma)
                    s0.substring(0, dec).filterNot { it == '.' || it == ',' } + "." + s0.substring(dec + 1)
                }
                lastComma >= 0 -> s0.replace(',', '.')
                else -> s0
            }
        if (s.count { it == '.' } > 1) return null
        if (s.any { !(it in '0'..'9' || it in NUMBER_CHARS) }) return null
        val v = s.toDoubleOrNull() ?: return null
        return if (v.isNaN() || v.isInfinite()) null else v
    }

    /**
     * Parses a latitude ([isLatitude] true, +-90) or longitude (+-180). Accepts a plain number, a number with a
     * hemisphere letter (`52.1N`, `S 12,5`, `19.2 E`) and degrees-minutes-seconds (`52°12'30"N`). A hemisphere
     * letter of the wrong axis (E for a latitude) is rejected, and so is a letter combined with a negative number.
     */
    fun parseCoordinate(text: String, isLatitude: Boolean): Double? {
        var s = text.trim()
        if (s.isEmpty()) return null
        var hemisphere: Char? = null
        if (isHemisphere(s.first())) {
            hemisphere = s.first().uppercaseChar()
            s = s.substring(1).trim()
        } else if (isHemisphere(s.last())) {
            hemisphere = s.last().uppercaseChar()
            s = s.dropLast(1).trim()
        }
        var sign = 1.0
        if (hemisphere != null) {
            val latitudeLetter = hemisphere == 'N' || hemisphere == 'S'
            if (latitudeLetter != isLatitude) return null
            if (hemisphere == 'S' || hemisphere == 'W') sign = -1.0
        }
        val raw = (if (s.contains('°') || s.contains('º') || s.contains('˚')) parseDms(s) else parseNumber(s)) ?: return null
        if (hemisphere != null && raw < 0.0) return null
        val v = sign * raw
        val limit = if (isLatitude) LAT_LIMIT else LON_LIMIT
        return if (v.absoluteValue > limit) null else v
    }

    /**
     * Parses a pasted "latitude, longitude" pair: separators `;`, `, ` or white space, or a single comma between two
     * dot-decimal numbers (`52.1,19.2`). If the first value cannot be a latitude the order is swapped (lon, lat).
     */
    fun parseLatLonPair(text: String): Pair<Double, Double>? {
        val t = text.trim()
        if (t.isEmpty()) return null
        val parts: List<String> =
            when {
                t.contains(';') -> t.split(';')
                else -> {
                    val bySpace = t.split(Regex("""\s*,\s+|\s+"""))
                    if (bySpace.size == 2) bySpace else if (t.count { it == ',' } == 1) t.split(',') else bySpace
                }
            }.map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size != 2) return null
        val lat = parseCoordinate(parts[0], true)
        val lon = parseCoordinate(parts[1], false)
        if (lat != null && lon != null) return lat to lon
        val lon2 = parseCoordinate(parts[0], false)
        val lat2 = parseCoordinate(parts[1], true)
        return if (lat2 != null && lon2 != null) lat2 to lon2 else null
    }

    /** "52.12345, 19.12345" (dot decimals, [decimals] digits). */
    fun formatCoordinates(lat: Double, lon: Double, decimals: Int = 5): String =
        Num.fmt(lat, decimals) + ", " + Num.fmt(lon, decimals)

    /** A plain number for a text field: at most [decimals] digits, no trailing zeros, dot decimal mark. */
    fun formatField(value: Double, decimals: Int): String = Num.fmtTrim(value, decimals)

    /** Watts with a sensible number of digits (2 for >= 1 W, otherwise 4). */
    fun formatWatts(watts: Double): String = Num.fmtTrim(watts, if (watts >= 1.0) 2 else 4)

    /** True when [a] and [b] are equal for the purpose of "has the text field changed the model value". */
    fun sameValue(a: Double, b: Double): Boolean = (a - b).absoluteValue <= 1.0e-6 * max(1.0, max(a.absoluteValue, b.absoluteValue))

    private fun isHemisphere(c: Char): Boolean = c.uppercaseChar() in "NSEW"

    private fun parseDms(input: String): Double? {
        val s = input.replace('′', '\'').replace('’', '\'').replace('″', '"').replace('”', '"')
            .replace('º', '°').replace('˚', '°').trim()
        val degEnd = s.indexOf('°')
        if (degEnd <= 0) return null
        val negative = s.startsWith("-")
        val deg = parseNumber(s.substring(0, degEnd))?.absoluteValue ?: return null
        var rest = s.substring(degEnd + 1).trim()
        var minutes = 0.0
        var seconds = 0.0
        if (rest.isNotEmpty()) {
            val mEnd = rest.indexOf('\'')
            if (mEnd < 0) return null
            minutes = parseNumber(rest.substring(0, mEnd)) ?: return null
            rest = rest.substring(mEnd + 1).trim().removeSuffix("\"").trim()
            if (rest.isNotEmpty()) seconds = parseNumber(rest) ?: return null
        }
        if (minutes < 0.0 || minutes >= SIXTY || seconds < 0.0 || seconds >= SIXTY) return null
        val v = deg + minutes / SIXTY + seconds / SECONDS_PER_HOUR
        return if (negative) -v else v
    }
}
