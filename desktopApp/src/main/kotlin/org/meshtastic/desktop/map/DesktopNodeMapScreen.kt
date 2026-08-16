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
package org.meshtastic.desktop.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.feature.map.node.NodeMapViewModel

/**
 * Desktop implementation of [org.meshtastic.core.ui.util.LocalNodeMapScreenProvider] — a full-screen single-node map
 * with a back-navigable app bar, mirroring the fdroid/google flavors' `NodeMapScreen`. Renders via [DesktopTrackMap],
 * the same renderer the embedded Position Log map uses.
 */
@Composable
fun DesktopNodeMapScreen(destNum: Int, onNavigateUp: () -> Unit) {
    val vm: NodeMapViewModel = koinViewModel()
    LaunchedEffect(destNum) { vm.setDestNum(destNum) }
    val node by vm.node.collectAsStateWithLifecycle()
    val positions by vm.positionLogs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            MainAppBar(
                title = node?.user?.long_name ?: "",
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                actions = {},
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        DesktopTrackMap(positions = positions, modifier = Modifier.fillMaxSize().padding(paddingValues))
    }
}