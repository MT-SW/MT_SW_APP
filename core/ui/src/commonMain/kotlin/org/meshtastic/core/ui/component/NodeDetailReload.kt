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
package org.meshtastic.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import org.koin.compose.koinInject
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.navigation.NodeDetailRoute
import org.meshtastic.core.navigation.NodesRoute
import org.meshtastic.core.navigation.replaceAll
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioInterfaceService

/** A device change settles for this long before the node card is reloaded (a quick off-and-on is no change). */
private const val DEVICE_SETTLE_MS = 300L

/** The nodes of the new device are read from its own database a moment after the switch. */
private const val NODE_LIST_SETTLE_MS = 700L

/**
 * An open node card shows the node of the device that was in use when it was opened. When another device is selected,
 * the card is closed and, if the same node is in the node list of the new device, opened again so that it shows the
 * data of that device; a node the new device does not know leaves the list (all platforms, every column layout).
 */
@OptIn(FlowPreview::class)
@Composable
fun ReloadNodeDetailsOnDeviceChange(multiBackstack: MultiBackstack) {
    val radio: RadioInterfaceService = koinInject()
    val nodes: NodeRepository = koinInject()
    LaunchedEffect(multiBackstack) {
        var shown = radio.currentDeviceAddressFlow.value
        radio.currentDeviceAddressFlow.filterNotNull().debounce(DEVICE_SETTLE_MS).collect { address ->
            val previous = shown
            shown = address
            if (previous == null || previous == address) return@collect
            delay(NODE_LIST_SETTLE_MS)
            reloadNodeDetails(multiBackstack.backStacks.values, nodes.nodeDBbyNum.value.keys)
        }
    }
}

private suspend fun reloadNodeDetails(stacks: Collection<NavBackStack<NavKey>>, known: Set<Int>) {
    val reopen = mutableListOf<Pair<NavBackStack<NavKey>, NodesRoute.NodeDetail>>()
    stacks.forEach { stack ->
        val open = stack.filterIsInstance<NodesRoute.NodeDetail>().lastOrNull() ?: return@forEach
        stack.replaceAll(stack.filter { it !is NodesRoute.NodeDetail && it !is NodeDetailRoute })
        val num = open.destNum
        if (num != null && num in known) reopen += stack to NodesRoute.NodeDetail(destNum = num)
    }
    if (reopen.isEmpty()) return
    // Two frames: the closed screens (and their data) are cleared first, so the reopened card starts afresh.
    withFrameNanos {}
    withFrameNanos {}
    reopen.forEach { (stack, route) -> stack.add(route) }
}
