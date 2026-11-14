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
package org.meshtastic.feature.map.maplibre.layers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.meshtastic.core.model.Node
import org.meshtastic.core.repository.SosAck
import org.meshtastic.core.repository.SosAcks
import org.meshtastic.core.repository.latestByNode
import org.meshtastic.feature.map.maplibre.geojson.NodeFeatureKeys
import org.meshtastic.feature.map.maplibre.geojson.lenientInt
import org.meshtastic.feature.map.maplibre.geojson.nodesToFeatureCollection
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.feature.map.maplibre.style.MapColors

/**
 * A ring round each node that answered the SOS we sent last: green when it is coming, red when it is calling for help,
 * grey when it only saw the SOS. The rings differ in size too, so they stay apart where the theme has no colours. The
 * answers are those of the list "Who confirmed the SOS"; a new SOS clears them.
 *
 * Unclustered nodes only, like the pulse: a ring under a cluster bubble would not say which member answered.
 */
@Composable
internal fun SosAnswerLayer(unclustered: List<Node>, onNodeClick: (Int) -> Unit) {
    val answers by koinInject<SosAcks>().entries.collectAsStateWithLifecycle()
    val latest = remember(answers) { answers.latestByNode() }
    for (ack in SosAck.entries) {
        val nodes = remember(unclustered, latest) { unclustered.filter { latest[it.num] == ack } }
        val source = rememberFeatureSource(nodes) { nodesToFeatureCollection(nodes) }
        CircleLayer(
            id = "sos-answer-${ack.name.lowercase()}",
            source = source,
            color = const(Color.Transparent),
            radius = const(ringRadius(ack).dp),
            strokeColor = const(ringColor(ack)),
            strokeWidth = const(RING_WIDTH_DP.dp),
            // A tap on the ring opens the node like a tap on its chip does.
            onClick = { features ->
                val num = features.firstNotNullOfOrNull { it.properties?.get(NodeFeatureKeys.NODE_NUM).lenientInt() }
                if (num == null) {
                    ClickResult.Pass
                } else {
                    onNodeClick(num)
                    ClickResult.Consume
                }
            },
        )
    }
}

private fun ringRadius(ack: SosAck): Int = when (ack) {
    SosAck.SEEN -> RING_SEEN_DP
    SosAck.COMING -> RING_COMING_DP
    SosAck.CALLING_HELP -> RING_HELP_DP
}

private fun ringColor(ack: SosAck): Color = when (ack) {
    SosAck.SEEN -> MapColors.SosSeen
    SosAck.COMING -> MapColors.SosComing
    SosAck.CALLING_HELP -> MapColors.SosHelp
}

/** Clear of the node chip (64x28dp, a half-diagonal of about 35dp), so the ring is not hidden behind it. */
private const val RING_SEEN_DP = 38
private const val RING_COMING_DP = 42
private const val RING_HELP_DP = 46
private const val RING_WIDTH_DP = 4
