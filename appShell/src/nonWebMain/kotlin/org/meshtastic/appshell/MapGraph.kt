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
package org.meshtastic.appshell

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.meshtastic.core.navigation.MapRoute
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.navigation.NodesRoute
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.ui.util.LocalMapMainScreenProvider
import org.meshtastic.core.ui.util.isDesktopPlatform
import org.meshtastic.feature.map.navigation.meshHudEntry
import org.meshtastic.feature.map.navigation.mapGraph as featureMapGraph

internal actual fun EntryProviderScope<NavKey>.mapGraph(
    backStack: NavBackStack<NavKey>,
    multiBackstack: MultiBackstack,
) {
    if (!isDesktopPlatform) {
        featureMapGraph(backStack)
        return
    }
    // MT_SW, desktop only: clicking a node on the map switches to the Nodes tab and opens its detail there (back
    // returns to the node list), instead of pushing NodeDetail onto the Map tab's own backstack.
    entry<MapRoute.Map> { args ->
        val mapScreen = LocalMapMainScreenProvider.current
        val openNodeInNodesTab: (Int) -> Unit = { id -> openNodeDetailInNodesTab(multiBackstack, id) }
        mapScreen(openNodeInNodesTab, openNodeInNodesTab, args.waypointId, args.plannerNodeNum, args.focusNodeNum) {
            backStack.add(MapRoute.MeshHud())
        }
    }
    // The mesh view (terrain panorama first): a node tapped in it opens in the Nodes tab, as from the map.
    meshHudEntry(backStack) { id -> openNodeDetailInNodesTab(multiBackstack, id) }
}

/**
 * Opens a node's details in the Nodes tab, from the map or the mesh view. What was open there before is cleared down to
 * the node list first, the way a tap on the list does it: pushing on top of it instead left the same node's details in
 * the stack twice after a second tap, and the screen then failed ("key used multiple times").
 */
private fun openNodeDetailInNodesTab(multiBackstack: MultiBackstack, id: Int) {
    multiBackstack.navigateTopLevel(TopLevelDestination.Nodes.route)
    val stack = multiBackstack.backStacks[TopLevelDestination.Nodes.route] ?: return
    while (stack.lastOrNull() != NodesRoute.Nodes && stack.size > 1) stack.removeLastOrNull()
    stack.add(NodesRoute.NodeDetail(id))
}
