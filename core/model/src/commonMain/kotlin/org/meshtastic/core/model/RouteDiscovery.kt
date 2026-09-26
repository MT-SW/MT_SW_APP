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
package org.meshtastic.core.model

import co.touchlab.kermit.Logger
import org.meshtastic.core.model.util.decodeOrNull
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.RouteDiscovery

val MeshPacket.fullRouteDiscovery: RouteDiscovery?
    get() {
        val d = decoded
        if (d != null && !d.want_response && d.portnum == PortNum.TRACEROUTE_APP) {
            val originalRd = RouteDiscovery.ADAPTER.decodeOrNull(d.payload, Logger) ?: return null

            val destinationId = if (d.dest != 0) d.dest else this.to
            val sourceId = if (d.source != 0) d.source else this.from

            // Note: Wire lists are immutable
            val fullRoute = listOf(destinationId) + originalRd.route + sourceId
            val fullRouteBack = listOf(sourceId) + originalRd.route_back + destinationId

            // hop_start/bitfield used to gate this (as a proxy for "firmware new enough to have set
            // hop_start"), but that proxy fires on ANY nonzero bitfield bit -- unrelated ones included -- so a
            // sniffed packet (especially via MQTT, relayed through other nodes) could get route_back wrapped with
            // endpoints even though snr_back didn't have the matching count, which is what formatTraceroutePath
            // needs below; that mismatch made it fall back to showing "?" for every hop, and separately made the
            // route look one hop longer than it really was.
            //
            // A genuine per-hop snr_back always has exactly one more entry than there are intermediate route_back
            // nodes (the extra entry is the final hop back to us), so checking that directly -- instead of
            // guessing from hop_start/bitfield -- only wraps route_back when doing so is actually consistent with
            // the SNR data the packet carries.
            val snrBackMatchesFullPath = originalRd.snr_back.size == originalRd.route_back.size + 1

            return originalRd
                .newBuilder()
                .also { wb ->
                    wb.route = fullRoute
                    wb.route_back = if (snrBackMatchesFullPath) fullRouteBack else originalRd.route_back
                }
                .build()
        }
        return null
    }

@Suppress("MagicNumber")
private fun formatTraceroutePath(nodesList: List<String>, snrList: List<Int>): String {
    // nodesList should include both origin and destination nodes
    // origin will not have an SNR value, but destination should
    val snrStr =
        if (snrList.size == nodesList.size - 1) {
            snrList
        } else {
            // use unknown SNR for entire route if snrList has invalid size
            List(nodesList.size - 1) { -128 }
        }
            .map { snr ->
                val str = if (snr == -128) "?" else "${snr / 4f}"
                "⇊ $str dB"
            }

    return nodesList
        .map { userName -> "■ $userName" }
        .flatMapIndexed { i, nodeStr -> if (i == 0) listOf(nodeStr) else listOf(snrStr[i - 1], nodeStr) }
        .joinToString("\n")
}

fun RouteDiscovery.getTracerouteResponse(
    getUser: (nodeNum: Int) -> String,
    headerTowards: String = "Route traced toward destination:\n\n",
    headerBack: String = "Route traced back to us:\n\n",
): String = buildString {
    if (route.isNotEmpty()) {
        append(headerTowards)
        append(formatTraceroutePath(route.map(getUser), snr_towards))
    }
    if (route_back.isNotEmpty()) {
        append("\n\n")
        append(headerBack)
        append(formatTraceroutePath(route_back.map(getUser), snr_back))
    }
}

fun MeshPacket.getTracerouteResponse(
    getUser: (nodeNum: Int) -> String,
    headerTowards: String = "Route traced toward destination:\n\n",
    headerBack: String = "Route traced back to us:\n\n",
): String? = fullRouteDiscovery?.getTracerouteResponse(getUser, headerTowards, headerBack)

enum class TracerouteMapAvailability {
    Ok,
    MissingEndpoints,
    NoMappableNodes,
}

fun evaluateTracerouteMapAvailability(
    forwardRoute: List<Int>,
    returnRoute: List<Int>,
    positionedNodeNums: Set<Int>,
): TracerouteMapAvailability {
    val endpoints =
        listOfNotNull(
            forwardRoute.firstOrNull(),
            forwardRoute.lastOrNull(),
            returnRoute.firstOrNull(),
            returnRoute.lastOrNull(),
        )
            .distinct()
    val missingEndpoint = endpoints.any { !positionedNodeNums.contains(it) }
    if (missingEndpoint) return TracerouteMapAvailability.MissingEndpoints
    val relatedNodeNums = (forwardRoute + returnRoute).toSet()
    val hasAnyMappable = relatedNodeNums.any { positionedNodeNums.contains(it) }
    return if (hasAnyMappable) TracerouteMapAvailability.Ok else TracerouteMapAvailability.NoMappableNodes
}
