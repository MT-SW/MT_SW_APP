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
package org.meshtastic.feature.node.model

import org.meshtastic.core.model.Node
import org.meshtastic.core.navigation.Route
import org.meshtastic.feature.node.component.NodeMenuAction
import org.meshtastic.proto.Config

sealed interface NodeDetailAction {
    data class Navigate(val route: Route) : NodeDetailAction

    data class HandleNodeMenuAction(val action: NodeMenuAction) : NodeDetailAction

    /** Open the remote-administration screen, ensuring a fresh session passkey first. */
    data class OpenRemoteAdmin(val nodeNum: Int) : NodeDetailAction

    /** Force-refresh device metadata (firmware version, edition, role) for the given node. */
    data class RefreshMetadata(val nodeNum: Int) : NodeDetailAction

    /**
     * Sets favorite status for [targetNodeNum] on the remote radio [destNum] over an active admin session — not the
     * locally-connected radio. [targetNodeNum] need not be in the local node DB.
     */
    data class SetRemoteFavorite(val destNum: Int, val targetNodeNum: Int, val favorite: Boolean) : NodeDetailAction

    /**
     * Sets ignore status for [targetNodeNum] on the remote radio [destNum] over an active admin session — not the
     * locally-connected radio. [targetNodeNum] need not be in the local node DB.
     */
    data class SetRemoteIgnored(val destNum: Int, val targetNodeNum: Int, val ignored: Boolean) : NodeDetailAction

    /**
     * Manually adds a contact ([targetNodeNum] with [longName]/[shortName]) to the remote radio [destNum]'s node DB
     * over an active admin session — not the locally-connected radio.
     */
    data class AddRemoteContact(val destNum: Int, val targetNodeNum: Int, val longName: String, val shortName: String) :
        NodeDetailAction

    /** Sets the GPIO pins in [gpioMask] on [destNum] to [gpioValue], via the Remote Hardware module. */
    data class WriteGpio(val destNum: Int, val gpioMask: Long, val gpioValue: Long) : NodeDetailAction

    /** Reads the GPIO pins in [gpioMask] on [destNum], via the Remote Hardware module. */
    data class ReadGpio(val destNum: Int, val gpioMask: Long) : NodeDetailAction

    data object ShareContact : NodeDetailAction

    // Opens the compass sheet scoped to a target node and the user’s preferred units.
    data class OpenCompass(val node: Node, val displayUnits: Config.DisplayConfig.DisplayUnits) : NodeDetailAction
}
