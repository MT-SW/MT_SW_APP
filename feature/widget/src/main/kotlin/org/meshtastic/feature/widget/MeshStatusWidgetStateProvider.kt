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
package org.meshtastic.feature.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Single
import org.meshtastic.core.service.stats.MeshStatsFeed
import org.meshtastic.core.service.stats.MeshStatsSnapshot

private const val MESH_STATUS_SAMPLE_MS = 30_000L

/** The lines of the "Statystyki MT_SW" widget: the same as the permanent notification, refreshed every 30 s. */
@Single
class MeshStatusWidgetStateProvider(feed: MeshStatsFeed) {
    @Suppress("InjectDispatcher") // feature:widget does not depend on core:di, where CoroutineDispatchers lives
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @OptIn(FlowPreview::class)
    val state: StateFlow<MeshStatsSnapshot?> =
        feed
            .snapshots(ownChoice = true, withNodes = true)
            .sample(MESH_STATUS_SAMPLE_MS)
            .stateIn(scope, SharingStarted.Eagerly, null)
}
