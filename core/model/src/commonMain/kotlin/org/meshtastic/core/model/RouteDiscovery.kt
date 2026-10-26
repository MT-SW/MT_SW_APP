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
import org.meshtastic.proto.HardwareModel
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.RouteDiscovery
import org.meshtastic.proto.User

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

            // route_back only ever carries the *intermediate* relay hops, same as route does for the forward
            // path above -- the actual endpoints are never part of either list, they come from the packet's own
            // header (source/destinationId), so both directions get wrapped unconditionally and symmetrically.
            //
            // An earlier version of this gated the route_back wrap on snr_back having exactly one more entry than
            // there are intermediate hops (route_back.size + 1), on the theory that a sniffed-mid-transit copy
            // (heard by a relay or MQTT gateway that isn't the packet's actual destination) wouldn't yet carry
            // that final hop's own SNR measurement -- true, but the wrong conclusion: that only means the LAST
            // edge's SNR is unknown for that copy, not that the destination node itself is unknown or shouldn't be
            // shown. Gating the wrap on it meant a sniffed copy silently lost BOTH endpoints from the displayed
            // route whenever it hadn't reached its true destination yet -- exactly the packets the Sniffer mostly
            // sees. formatTraceroutePath already degrades a length-mismatched snr_back to "?" per edge while still
            // showing every node name, so there's no need to withhold the nodes themselves over missing SNR.
            return originalRd
                .newBuilder()
                .also { wb ->
                    wb.route = fullRoute
                    wb.route_back = fullRouteBack
                }
                .build()
        }
        return null
    }

/**
 * The inbound path of a traceroute request that reached us, from its originator to us, with `route_back` empty. The
 * relays appended themselves on the way and our radio added only its own SNR, so the endpoints are filled in here. Null
 * unless this packet is a traceroute request.
 */
val MeshPacket.inboundRouteDiscovery: RouteDiscovery?
    get() {
        val d = decoded ?: return null
        if (!d.want_response || d.portnum != PortNum.TRACEROUTE_APP) return null
        val originalRd = RouteDiscovery.ADAPTER.decodeOrNull(d.payload, Logger) ?: return null
        val sourceId = if (d.source != 0) d.source else this.from
        val destinationId = if (d.dest != 0) d.dest else this.to
        return originalRd
            .newBuilder()
            .also { wb ->
                wb.route = listOf(sourceId) + originalRd.route + destinationId
                wb.route_back = emptyList()
                wb.snr_back = emptyList()
            }
            .build()
    }

/**
 * Display name for a traceroute hop. A node missing from the node DB comes back from the repository as a placeholder
 * user with no hardware model and a generated "Meshtastic xxxx" name; that case is named by its full node id. Firmware
 * fills a relay that did not add itself with the broadcast address, which is not a node, so it gets [unknownHopLabel].
 * A non-blank local [displayName] replaces the broadcast long name.
 */
fun tracerouteHopName(nodeNum: Int, user: User, unknownHopLabel: String, displayName: String = ""): String {
    if (nodeNum == NodeAddress.NODENUM_BROADCAST) return unknownHopLabel
    val id = NodeAddress.numToDefaultId(nodeNum)
    val placeholder =
        displayName.isBlank() &&
            user.hw_model == HardwareModel.UNSET &&
            (user.long_name.isBlank() || user.long_name == "Meshtastic ${id.takeLast(PLACEHOLDER_SUFFIX_LENGTH)}")
    return if (placeholder) id else "${displayName.ifBlank { user.long_name }} (${user.short_name})"
}

private const val PLACEHOLDER_SUFFIX_LENGTH = 4

/** A route for display, and the known node that stands in for its last relay when that hop was unknown. */
class LikelyRelay(val route: RouteDiscovery, val node: Node?)

/**
 * The firmware leaves the broadcast address where a relay could not add itself (typically one on another channel). The
 * last relay before us is the one exception that can still be identified: the response carries its low address byte.
 * The returned route has that one hop replaced by the single known node that matches; it is unchanged, with a null
 * node, when the byte is missing, nothing or several nodes match, or the node is already on the route.
 */
fun RouteDiscovery.withLikelyLastRelay(relayNode: Int, nodes: Collection<Node>, ourNodeNum: Int?): LikelyRelay {
    val back = route_back
    val lastRelayIndex = back.size - 2
    if (relayNode == 0 || back.size < MIN_BACK_ROUTE || back[lastRelayIndex] != NodeAddress.NODENUM_BROADCAST) {
        return LikelyRelay(this, null)
    }
    val node =
        nodes
            .filter {
                it.num != ourNodeNum &&
                    it.lastHeard != 0 &&
                    (it.num and RELAY_BYTE_MASK) == (relayNode and RELAY_BYTE_MASK) &&
                    it.num !in back &&
                    it.num !in route
            }
            .singleOrNull() ?: return LikelyRelay(this, null)
    val shown =
        newBuilder()
            .also { wb -> wb.route_back = back.mapIndexed { i, num -> if (i == lastRelayIndex) node.num else num } }
            .build()
    return LikelyRelay(shown, node)
}

private const val RELAY_BYTE_MASK = 0xFF
private const val MIN_BACK_ROUTE = 3

@Suppress("MagicNumber")
internal fun formatTraceroutePath(nodesList: List<String>, snrList: List<Int>): String {
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

    /** Start and destination are known, but at least one relay on the way has no position. */
    MissingRelays,
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
    if (!hasAnyMappable) return TracerouteMapAvailability.NoMappableNodes
    // The map draws every hop with its signal strength, so a relay without a position would leave a gap.
    return if (relatedNodeNums.all { positionedNodeNums.contains(it) }) {
        TracerouteMapAvailability.Ok
    } else {
        TracerouteMapAvailability.MissingRelays
    }
}
