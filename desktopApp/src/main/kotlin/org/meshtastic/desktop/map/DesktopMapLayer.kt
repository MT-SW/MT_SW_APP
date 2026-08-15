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

import kotlin.uuid.Uuid

/**
 * Desktop's own map-layer model — a standalone parallel of the Android fork's `MapLayerItem`/`MapLayersManager`
 * (which are Android-specific: `Context`, `android.net.Uri`), not a reuse of it. `filePath` is an absolute path on the
 * local filesystem rather than a content URI, since desktop has no content-resolver concept.
 */
enum class DesktopLayerType {
    KML,
    GEOJSON,
}

data class DesktopMapLayer(
    val id: String = Uuid.random().toString(),
    val name: String,
    val filePath: String,
    val isVisible: Boolean = true,
    val layerType: DesktopLayerType,
    val createdAt: Long? = null,
)

private val KML_EXTENSIONS = setOf("kml")
private val GEOJSON_EXTENSIONS = setOf("geojson", "json")

/** Resolve a file extension (no leading dot) to a [DesktopLayerType], or null if unsupported. */
fun resolveDesktopLayerType(extension: String?): DesktopLayerType? = when (extension?.lowercase()) {
    in KML_EXTENSIONS -> DesktopLayerType.KML
    in GEOJSON_EXTENSIONS -> DesktopLayerType.GEOJSON
    else -> null
}
