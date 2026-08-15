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
package org.meshtastic.desktop.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.meshtastic.core.navigation.MapRoute
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.navigation.NodesRoute
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.ui.util.LocalMapMainScreenProvider
import org.meshtastic.core.ui.viewmodel.UIViewModel
import org.meshtastic.feature.connections.navigation.connectionsGraph
import org.meshtastic.feature.discovery.navigation.discoveryGraph
import org.meshtastic.feature.docs.navigation.docsEntries
import org.meshtastic.feature.firmware.navigation.firmwareGraph
import org.meshtastic.feature.messaging.navigation.contactsGraph
import org.meshtastic.feature.node.navigation.networkHealthGraph
import org.meshtastic.feature.node.navigation.nodesGraph
import org.meshtastic.feature.settings.navigation.settingsGraph
import org.meshtastic.feature.settings.radio.RadioConfigViewModel
import org.meshtastic.feature.settings.radio.channel.channelsGraph
import org.meshtastic.feature.wifiprovision.navigation.wifiProvisionGraph

/**
 * Registers [NavKey] entry providers for every desktop destination.
 *
 * Each call delegates to the shared navigation graph extension exported by the corresponding feature module, keeping
 * the desktop shell free of screen-level composable knowledge.
 */
fun EntryProviderScope<NavKey>.desktopNavGraph(
    backStack: NavBackStack<NavKey>,
    uiViewModel: UIViewModel,
    multiBackstack: MultiBackstack,
    settingsRadioConfigViewModel: @Composable (SettingsRoute.Settings?) -> RadioConfigViewModel,
) {
    nodesGraph(
        backStack = backStack,
        scrollToTopEvents = uiViewModel.scrollToTopEventFlow,
        onHandleDeepLink = uiViewModel::handleDeepLink,
        onNavigateToConnections = { multiBackstack.navigateTopLevel(TopLevelDestination.Connect.route) },
    )
    networkHealthGraph(backStack)
    contactsGraph(
        backStack = backStack,
        scrollToTopEvents = uiViewModel.scrollToTopEventFlow,
        onHandleDeepLink = uiViewModel::handleDeepLink,
    )
    // Desktop-only override of the shared feature/map `mapGraph`: clicking a node on the map switches to the Nodes
    // tab and opens its detail there (back returns to the node list), instead of the shared implementation's
    // behavior of pushing NodeDetail directly onto the Map tab's own backstack.
    entry<MapRoute.Map> { args ->
        val mapScreen = LocalMapMainScreenProvider.current
        val openNodeInNodesTab: (Int) -> Unit = { id ->
            multiBackstack.navigateTopLevel(TopLevelDestination.Nodes.route)
            multiBackstack.backStacks[TopLevelDestination.Nodes.route]?.add(NodesRoute.NodeDetail(id))
        }
        // ZAŁOŻENIE DO WERYFIKACJI: zakładam że MapRoute.Map ma teraz pole sitePlannerNodeNum
        // analogiczne do waypointId. Jeśli kompilator powie "unresolved reference", wklej mi
        // definicję MapRoute.Map, żeby sprawdzić jak faktycznie się nazywa / czy istnieje.
        mapScreen(openNodeInNodesTab, openNodeInNodesTab, args.waypointId, args.sitePlannerNodeNum)
    }
    firmwareGraph(backStack)
    settingsGraph(backStack, settingsRadioConfigViewModel)
    docsEntries(backStack)
    channelsGraph(backStack)
    connectionsGraph(backStack)
    discoveryGraph(backStack)
    wifiProvisionGraph(backStack)
}
