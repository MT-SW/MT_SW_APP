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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import kotlin.math.abs

/** One node inside a tapped cluster. */
data class ClusterMember(val nodeNum: Int, val longName: String, val shortName: String)

/**
 * Reads the members of a cluster out of its leaf features.
 *
 * The names travel on the features themselves — see [nodesToFeatureCollection] — so listing a cluster needs no second
 * lookup against the node database.
 */
internal fun FeatureCollection<*, *>.toClusterMembers(): List<ClusterMember> = features.mapNotNull { feature ->
    (feature.properties as? JsonObject)?.toClusterMember()
}

/**
 * The nodes drawn on the same spot as [top] under one tap, when there is more than one.
 *
 * Coincident nodes are nudged a few metres apart (see `spreadCoincidentPositions`), which only separates their chips
 * once the map is zoomed in close. Until then the chips overlap and a tap reaches only the top one, so the others
 * cannot be opened. Listing them lets the user pick. Chips that merely touch at their edges, with their fixes further
 * apart than [SAME_SPOT_DEGREES], are not a stack and are not listed. Fewer than two nodes returns an empty list.
 */
internal fun stackedMembers(features: List<Feature<*, *>>, top: Feature<*, *>): List<ClusterMember> {
    val topPoint = (top.geometry as? Point)?.coordinates ?: return emptyList()
    val members =
        features
            .filter { feature ->
                val point = (feature.geometry as? Point)?.coordinates
                point != null &&
                    abs(point.latitude - topPoint.latitude) <= SAME_SPOT_DEGREES &&
                    abs(point.longitude - topPoint.longitude) <= SAME_SPOT_DEGREES
            }
            .mapNotNull { (it.properties as? JsonObject)?.toClusterMember() }
            .distinctBy { it.nodeNum }
    return if (members.size > 1) members else emptyList()
}

/**
 * About 30 m of latitude: the spread radius for coincident nodes (8 m) with room to spare, nothing like a neighbour.
 */
private const val SAME_SPOT_DEGREES = 0.00027

private fun JsonObject.toClusterMember(): ClusterMember? {
    val nodeNum = this[NodeFeatureKeys.NODE_NUM].asInt() ?: return null
    return ClusterMember(
        nodeNum = nodeNum,
        longName = this[NodeFeatureKeys.LONG_NAME].asText(),
        shortName = this[NodeFeatureKeys.SHORT_NAME].asText(),
    )
}

private fun JsonElement?.asInt(): Int? = this?.jsonPrimitive?.intOrNull

private fun JsonElement?.asText(): String = this?.jsonPrimitive?.contentOrNull.orEmpty()
