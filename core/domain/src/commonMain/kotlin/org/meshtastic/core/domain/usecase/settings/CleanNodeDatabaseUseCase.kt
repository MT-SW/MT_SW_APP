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
package org.meshtastic.core.domain.usecase.settings

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioController
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/** Use case for cleaning up nodes from the database. */
@Single
open class CleanNodeDatabaseUseCase
constructor(
    private val nodeRepository: NodeRepository,
    private val radioController: RadioController,
) {
    /** Identifies nodes that match the cleanup criteria. */
    open suspend fun getNodesToClean(
        olderThanDays: Float,
        onlyUnknownNodes: Boolean,
        currentTimeSeconds: Long,
    ): List<Node> {
        val sevenDaysAgoSeconds = currentTimeSeconds - 7.days.inWholeSeconds
        val olderThanTimestamp = currentTimeSeconds - olderThanDays.toInt().days.inWholeSeconds

        val nodesToConsider =
            if (onlyUnknownNodes) {
                val olderNodes = nodeRepository.getNodesOlderThan(olderThanTimestamp.toInt())
                val unknownNodes = nodeRepository.getUnknownNodes()
                olderNodes.filter { itNode -> unknownNodes.any { it.num == itNode.num } }
            } else {
                nodeRepository.getNodesOlderThan(olderThanTimestamp.toInt())
            }

        return nodesToConsider.filterNot { node ->
            (node.hasPKC && node.lastHeard >= sevenDaysAgoSeconds) || node.isIgnored || node.isFavorite
        }
    }

    /**
     * Every unknown node -- one that never introduced itself (no node info) and that we hold no public key for --
     * regardless of when it was last heard. Favorites and ignored nodes are always kept.
     */
    open suspend fun getAllUnknownNodesToClean(): List<Node> =
        nodeRepository.getUnknownNodes().filterNot { node -> node.hasPKC || node.isIgnored || node.isFavorite }

    /** Performs the cleanup of specified nodes. */
    open suspend fun cleanNodes(nodeNums: List<Int>) {
        if (nodeNums.isEmpty()) return

        nodeRepository.deleteNodes(nodeNums)
        // The radio has to be told too, otherwise every node comes back from its own database on the next connect.
        // Run to the end even if the screen is closed meanwhile, and pace the commands: a Bluetooth link only accepts
        // a few queued writes at a time, so a burst of hundreds of removals would otherwise be mostly dropped.
        if (radioController.connectionState.value != ConnectionState.Connected) return
        withContext(NonCancellable) {
            for (nodeNum in nodeNums) {
                val packetId = radioController.generatePacketId()
                radioController.removeByNodenum(packetId, nodeNum)
                delay(REMOVE_PACING)
            }
        }
    }

    private companion object {
        val REMOVE_PACING = 40.milliseconds
    }
}
