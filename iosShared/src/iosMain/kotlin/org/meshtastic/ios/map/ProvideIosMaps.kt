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
package org.meshtastic.ios.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.ui.util.LocalDiscoveryMapProvider
import org.meshtastic.core.ui.util.LocalHorizonPickMapProvider
import org.meshtastic.core.ui.util.LocalInlineMapProvider
import org.meshtastic.core.ui.util.LocalMapMainScreenProvider
import org.meshtastic.core.ui.util.LocalMapViewProvider
import org.meshtastic.core.ui.util.LocalNodeTrackMapProvider
import org.meshtastic.core.ui.util.LocalPlannerAvailable
import org.meshtastic.core.ui.util.LocalTracerouteMapOverlayInsetsProvider
import org.meshtastic.core.ui.util.LocalTracerouteMapProvider
import org.meshtastic.core.ui.util.TracerouteMapOverlayInsets
import org.meshtastic.feature.map.MapScreen
import org.meshtastic.feature.map.SharedMapViewModel
import org.meshtastic.feature.map.maplibre.MapLibreDiscoveryMap
import org.meshtastic.feature.map.maplibre.MapLibreHorizonPickMap
import org.meshtastic.feature.map.maplibre.MapLibreInlineMap
import org.meshtastic.feature.map.maplibre.MapLibreNodeTrackMap

/** Hands every map place of the app (Map tab, node card, tracks, planner, discovery, traceroute) the shared MapLibre maps. */
@Composable
fun ProvideIosMaps(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMapViewProvider provides iosMapViewProvider(),
        LocalPlannerAvailable provides true,
        LocalMapMainScreenProvider provides
            { onClickNodeChip, navigateToNodeDetails, waypointId, plannerNodeNum, focusNodeNum, onOpenMeshHud ->
                MapScreen(
                    viewModel = koinViewModel<SharedMapViewModel>(),
                    onClickNodeChip = onClickNodeChip,
                    navigateToNodeDetails = navigateToNodeDetails,
                    waypointId = waypointId,
                    plannerNodeNum = plannerNodeNum,
                    focusNodeNum = focusNodeNum,
                    onOpenMeshHud = onOpenMeshHud,
                )
            },
        LocalInlineMapProvider provides { node, modifier -> MapLibreInlineMap(node, modifier) },
        LocalNodeTrackMapProvider provides
            { destNum, positions, modifier, selectedPositionTime, onPositionSelect, showAttribution ->
                MapLibreNodeTrackMap(
                    destNum = destNum,
                    positions = positions,
                    modifier = modifier,
                    selectedPositionTime = selectedPositionTime,
                    onPositionSelect = onPositionSelect,
                    showAttribution = showAttribution,
                )
            },
        LocalHorizonPickMapProvider provides
            { lat, lon, radiusKm, nodes, onCenterChanged, modifier ->
                MapLibreHorizonPickMap(lat, lon, radiusKm, nodes, onCenterChanged, modifier)
            },
        LocalDiscoveryMapProvider provides
            { userLatitude, userLongitude, nodes, modifier ->
                MapLibreDiscoveryMap(userLatitude, userLongitude, nodes, modifier)
            },
        LocalTracerouteMapProvider provides
            { overlay, nodePositions, onMappableCountChanged, modifier ->
                IosTracerouteMap(overlay, nodePositions, onMappableCountChanged, modifier)
            },
        LocalTracerouteMapOverlayInsetsProvider provides
            TracerouteMapOverlayInsets(overlayPadding = PaddingValues(bottom = 48.dp)),
        content = content,
    )
}
