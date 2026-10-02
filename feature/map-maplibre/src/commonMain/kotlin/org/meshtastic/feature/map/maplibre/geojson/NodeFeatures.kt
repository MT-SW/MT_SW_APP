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

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.util.precisionRadiusMetersOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

/** Formats an `@ColorInt` ARGB value as the `#rrggbb` string MapLibre expressions parse. */
@Suppress("MagicNumber")
internal fun Int.toCssHex(): String {
    val hex = (this and 0xFFFFFF).toString(16).padStart(6, '0')
    return "#$hex"
}

/**
 * Projects nodes into a GeoJSON [FeatureCollection] for the clustered node source.
 *
 * Nodes without a usable fix are dropped rather than emitted at (0, 0) — that is what produced the "flying through the
 * ocean" jump on the OSMdroid map.
 *
 * Nodes sharing an effectively identical position (e.g. several fixed-position devices at one address) are nudged apart
 * by a few metres in a small circle around that shared point, via [spreadCoincidentPositions] — invisible at any normal
 * zoom, but enough that they stop landing on the exact same pixel once zoomed in close, and enough that a cluster of
 * them is no longer mathematically unbreakable by zooming further.
 */
fun nodesToFeatureCollection(nodes: List<Node>, myNodeNum: Int? = null): FeatureCollection<Point, JsonObject?> {
    val validNodes = nodes.filter { it.validPosition != null }
    val positions = spreadCoincidentPositions(validNodes)

    return FeatureCollection(
        validNodes.map { node ->
            val (foreground, background) = node.colors
            Feature(
                geometry = Point(positions.getValue(node.num)),
                properties =
                buildJsonObject {
                    put(NodeFeatureKeys.NODE_NUM, node.num)
                    put(NodeFeatureKeys.SHORT_NAME, node.user.short_name)
                    put(NodeFeatureKeys.LONG_NAME, node.user.long_name)
                    put(NodeFeatureKeys.IS_FAVORITE, node.isFavorite)
                    put(NodeFeatureKeys.IS_SELF, myNodeNum != null && node.num == myNodeNum)
                    put(NodeFeatureKeys.FOREGROUND, foreground.toCssHex())
                    put(NodeFeatureKeys.BACKGROUND, background.toCssHex())
                    // Omitted rather than written as 0.0 when the node reports no precision: 0 is a real
                    // radius, and a reader cannot tell the difference. GeoCircle drops such nodes instead.
                    precisionRadiusMetersOrNull(node.position.precision_bits)?.let {
                        put(NodeFeatureKeys.PRECISION_METERS, it)
                    }
                    put(NodeFeatureKeys.CHIP, node.toNodeChip().featureValue())
                },
            )
        },
    )
}

/**
 * Maps each node's number to a [Position], nudged a few metres off its real fix when it shares one with other nodes in
 * the list.
 *
 * Grouped by rounding to roughly 11cm at the equator — tight enough to only catch nodes reporting an effectively
 * identical fixed position, not ordinary GPS jitter between two nearby real readings (which is usually several metres).
 * A group of one is returned unchanged; real position data should never be altered for a node that has no collision.
 */
private fun spreadCoincidentPositions(nodes: List<Node>): Map<Int, Position> {
    val groups = nodes.groupBy { coincidenceKey(it) }
    return buildMap {
        for (group in groups.values) {
            group.forEachIndexed { index, node ->
                put(node.num, spreadPosition(node.latitude, node.longitude, index, group.size))
            }
        }
    }
}

/** Rounds to [COINCIDENCE_PRECISION] decimal degrees, so nodes reporting the same fixed position group together. */
private fun coincidenceKey(node: Node): Pair<Long, Long> =
    (node.latitude * COINCIDENCE_PRECISION).roundToLong() to (node.longitude * COINCIDENCE_PRECISION).roundToLong()

/**
 * One node of [count] sharing [baseLat]/[baseLon], placed at its share of a small circle around that point.
 *
 * A single node ([count] 1) is returned exactly where it was reported — there is no collision to resolve, so nothing
 * here should ever move a node with a fix of its own.
 */
private fun spreadPosition(baseLat: Double, baseLon: Double, index: Int, count: Int): Position {
    if (count <= 1) return Position(longitude = baseLon, latitude = baseLat)

    val angle = 2 * PI * index / count
    val metersPerDegreeLongitude =
        (METERS_PER_DEGREE_LATITUDE * cos(Math.toRadians(baseLat))).coerceAtLeast(MIN_METERS_PER_DEGREE_LONGITUDE)
    val deltaLat = (SPREAD_RADIUS_METERS * cos(angle)) / METERS_PER_DEGREE_LATITUDE
    val deltaLon = (SPREAD_RADIUS_METERS * sin(angle)) / metersPerDegreeLongitude
    return Position(longitude = baseLon + deltaLon, latitude = baseLat + deltaLat)
}

/** ~11cm at the equator: catches an identical reported fix, not real GPS jitter between two nearby readings. */
private const val COINCIDENCE_PRECISION = 1_000_000.0

/** How far apart, in real metres, coincident nodes are spread — invisible until zoomed in close. */
private const val SPREAD_RADIUS_METERS = 8.0

/** Standard equatorial approximation; fine at this scale, no need for a proper geodesic library here. */
private const val METERS_PER_DEGREE_LATITUDE = 111_320.0

/**
 * Floor for the latitude-adjusted metres-per-degree-of-longitude, so a node exactly at a pole cannot divide by (near)
 * zero. Never actually reached by this app's mesh, but cheap to guard against.
 */
private const val MIN_METERS_PER_DEGREE_LONGITUDE = 1.0

/**
 * One distinct chip appearance. Two markers that look identical only need drawing once.
 *
 * @param outlined draws a white border, as the discovery map's chips have. Node chips have none, matching
 *   [org.meshtastic.core.ui.component.NodeChip].
 * @param glyph drawn instead of [label], for the discovery map's sensor and social markers. The Google discovery map
 *   substitutes an icon for the name the same way.
 */
internal data class MapChipKey(
    val label: String,
    val background: Int,
    val foreground: Int,
    val struckThrough: Boolean = false,
    val outlined: Boolean = false,
    val glyph: MapChipGlyph? = null,
)

/** The icons a chip can carry in place of its text. */
internal enum class MapChipGlyph {
    /** A node whose traffic is mostly environment telemetry. */
    SENSOR,

    /** A node whose traffic is mostly messages. */
    SOCIAL,
}

/**
 * The value a feature carries so one layer can pick that marker's chip image.
 *
 * Every field that changes the pixels is in the key: a short name is not unique, and neither is a colour.
 */
internal fun MapChipKey.featureValue(): String =
    "$label ${background.toString(HEX_RADIX)} ${foreground.toString(HEX_RADIX)} $struckThrough $outlined $glyph"

internal fun Node.toNodeChip(): MapChipKey {
    val (foreground, background) = colors
    return MapChipKey(
        // Matches NodeChip, which shows "???" rather than an empty badge for a node that has not sent a name yet.
        label = user.short_name.ifEmpty { UNNAMED_LABEL },
        background = background,
        foreground = foreground,
        struckThrough = isIgnored,
    )
}

private const val UNNAMED_LABEL = "???"
private const val HEX_RADIX = 16
