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
package org.meshtastic.feature.map.maplibre.geojson

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Reads a map feature property as a whole number. MapLibre on iPhone hands numbers over as decimals (a node number as
 * `-1.23E9`), which a strict `int` refuses, so a tap on a chip did nothing there.
 */
internal fun JsonElement?.lenientInt(): Int? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.intOrNull ?: primitive.doubleOrNull?.toLong()?.toInt()
}

/** Reads a map feature property as a flag; a number counts as one too (non-zero is true), as MapLibre on iPhone sends. */
internal fun JsonElement?.lenientBoolean(): Boolean? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.booleanOrNull ?: primitive.doubleOrNull?.let { it != 0.0 }
}
