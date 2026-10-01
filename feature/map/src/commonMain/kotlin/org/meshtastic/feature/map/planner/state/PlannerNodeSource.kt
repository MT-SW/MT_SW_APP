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
package org.meshtastic.feature.map.planner.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.meshtastic.core.model.Node
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.ui.util.LnaCorrection

/** Source of the mesh nodes the planner can use as link ends. */
interface PlannerNodeSource {
    /** Nodes with a valid position (our own node included, flagged by [PlannerNodeOption.isOurs]). */
    val nodes: Flow<List<PlannerNodeOption>>
}

/**
 * Production [PlannerNodeSource] on top of the node database. RSSI is corrected with the app's LNA gain of the
 * connected device (the device that received the packets), exactly as the node list shows it.
 */
class RepositoryPlannerNodeSource(private val nodeRepository: NodeRepository, private val uiPrefs: UiPrefs) :
    PlannerNodeSource {

    override val nodes: Flow<List<PlannerNodeOption>> =
        combine(nodeRepository.getNodes(), nodeRepository.ourNodeInfo, uiPrefs.lnaGains) { all, ours, gains ->
            val ourNum = ours?.num
            val lna = LnaCorrection(ourNum, gains)
            all.filter { node -> !node.isIgnored && node.validPosition != null }
                .map { node -> node.toOption(node.num == ourNum, lna) }
        }

    private fun Node.toOption(isOurs: Boolean, lna: LnaCorrection): PlannerNodeOption {
        val rawAltitude: Int? = position.altitude
        val rawRssi = rssiOrNull
        return PlannerNodeOption(
            num = num,
            longName = user.long_name,
            shortName = user.short_name,
            lat = latitude,
            lon = longitude,
            altitudeM = rawAltitude?.toDouble(),
            snrDb = snrOrNull,
            rssiDbm = if (isOurs || rawRssi == null) null else lna.rssi(rawRssi),
            hopsAway = hopsAway,
            isOurs = isOurs,
        )
    }
}
