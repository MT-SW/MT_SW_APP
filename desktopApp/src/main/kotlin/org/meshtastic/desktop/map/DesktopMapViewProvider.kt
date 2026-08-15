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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.geofence.toGeofence
import org.meshtastic.core.model.isLocked
import org.meshtastic.core.model.isModifiableBy
import org.meshtastic.core.model.util.toCodePointString
import org.meshtastic.core.model.util.waypointIconOrDefault
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.delete_for_everyone
import org.meshtastic.core.resources.delete_for_me
import org.meshtastic.core.resources.expires
import org.meshtastic.core.resources.locked
import org.meshtastic.core.resources.waypoint_new
import org.meshtastic.core.ui.component.precisionBitsToMeters
import org.meshtastic.core.ui.icon.CellTower
import org.meshtastic.core.ui.icon.Download
import org.meshtastic.core.ui.icon.LocationOn
import org.meshtastic.core.ui.icon.Lock
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.MapViewProvider
import org.meshtastic.desktop.siteplanner.DesktopSitePlannerHost
import org.meshtastic.desktop.siteplanner.toSitePlannerParams
import org.meshtastic.proto.Waypoint
import java.io.File

private const val PIN_SIZE_DP = 28
private const val WAYPOINT_MARKER_SIZE_DP = 20
private const val LOCK_BADGE_SIZE_DP = 12
private const val PRECISION_CIRCLE_ALPHA = 0.15f
private val GEOFENCE_STROKE_COLOR = Color(0xFFFF9800)
private val GEOFENCE_FILL_COLOR = Color(0xFFFF9800).copy(alpha = 0.12f)
private const val GEOFENCE_STROKE_WIDTH_DP = 2

// Mirrors MapViewportState's own MIN_ZOOM/MAX_ZOOM clamp range (those constants are private to that file).
private const val DOWNLOAD_MIN_ZOOM = 2
private const val DOWNLOAD_MAX_ZOOM = 18
private const val DEFAULT_DOWNLOAD_ZOOM_SPAN = 3
private const val BYTES_PER_MB = 1024 * 1024

/**
 * Desktop implementation of [MapViewProvider] backed by [OsmCanvasMap]. Renders node markers (with optional precision
 * circles), waypoint markers with geofence overlays, imported GeoJSON/KML layers, offline tile-region downloads, and
 * a Site Planner coverage-estimate trigger (via JCEF, see the `siteplanner` package). Node markers use pin-shaped
 * `MeshtasticIcons.LocationOn` icons with a short-name label; clicking one navigates via [navigateToNodeDetails] —
 * on desktop this is wired (in DesktopNavigation.kt) to open the Nodes tab and push detail there, not jump directly.
 */
class DesktopMapViewProvider : MapViewProvider {
    @Composable
    @Suppress("LongMethod")
    override fun MapView(
        modifier: Modifier,
        navigateToNodeDetails: (Int) -> Unit,
        waypointId: Int?,
        sitePlannerNodeNum: Int?,
    ) {
        val viewModel: DesktopMapViewModel = koinViewModel()
        val nodesWithPosition by viewModel.nodesWithPosition.collectAsState()
        val waypoints by viewModel.waypoints.collectAsState()
        val mapFilterState by viewModel.mapFilterStateFlow.collectAsState()
        val ourNode by viewModel.ourNodeInfo.collectAsState()
        val isConnected by viewModel.isConnected.collectAsState()
        val viewport = rememberMapViewportState()
        val hasCentered = remember { mutableStateOf(false) }
        var selectedWaypointId by remember { mutableStateOf<Int?>(null) }

        val layerManager = koinInject<DesktopMapLayerManager>()
        val mapLayers by layerManager.layers.collectAsState()
        var parsedLayerFeatures by remember { mutableStateOf<Map<String, List<ParsedFeature>>>(emptyMap()) }

        var lastViewportSize by remember { mutableStateOf(Offset.Zero) }
        var showDownloadDialog by remember { mutableStateOf(false) }
        var showSitePlanner by remember { mutableStateOf(false) }
        val channelSet by viewModel.channelSet.collectAsState()
        val httpClient = koinInject<HttpClient>()
        val coroutineScope = rememberCoroutineScope()

        // NOTE (recovery script — needs verification): `sitePlannerNodeNum` is a MapViewProvider interface
        // parameter that did not exist in the originally-recovered session — it must have arrived via a later
        // upstream sync (an official/shared "open Site Planner for this specific node" entry point). Best-effort
        // interpretation: auto-open the Site Planner dialog, pre-seeded from that node's params if known
        // (falling back to the connected radio's own node otherwise). Verify this matches what Android's own
        // MapViewProvider implementation actually does with this parameter, and adjust if not.
        val sitePlannerTargetNode = remember(sitePlannerNodeNum, nodesWithPosition, ourNode) {
            sitePlannerNodeNum?.let { num -> nodesWithPosition.find { it.num == num } ?: ourNode }
        }
        LaunchedEffect(sitePlannerNodeNum) { if (sitePlannerNodeNum != null) showSitePlanner = true }

        // Parses newly-added layer files off the main thread and caches results by layer id (not content), so
        // toggling visibility off/on doesn't re-read/re-parse the file; drops cached entries for removed layers.
        LaunchedEffect(mapLayers) {
            val toParse = mapLayers.filterNot { parsedLayerFeatures.containsKey(it.id) }
            val newlyParsed =
                if (toParse.isEmpty()) {
                    emptyMap()
                } else {
                    withContext(Dispatchers.IO) {
                        toParse.associate { layer ->
                            layer.id to
                                runCatching {
                                    when (layer.layerType) {
                                        DesktopLayerType.GEOJSON -> parseGeoJson(File(layer.filePath).readText())
                                        DesktopLayerType.KML -> parseKmlFile(File(layer.filePath))
                                    }
                                }
                                    .getOrElse { emptyList() }
                        }
                    }
                }
            val validIds = mapLayers.map { it.id }.toSet()
            parsedLayerFeatures = (parsedLayerFeatures + newlyParsed).filterKeys { it in validIds }
        }

        val visibleLayerFeatures =
            remember(mapLayers, parsedLayerFeatures) {
                mapLayers.filter { it.isVisible }.flatMap { parsedLayerFeatures[it.id].orEmpty() }
            }

        // Mirrors Android fork's onNodesChanged filter: favorites-only and last-heard cutoff never hide our own node.
        val visibleNodes =
            remember(nodesWithPosition, mapFilterState, ourNode) {
                nodesWithPosition.filter { node ->
                    val isOurNode = node.num == ourNode?.num
                    when {
                        mapFilterState.onlyFavorites && !node.isFavorite && !isOurNode -> false
                        mapFilterState.lastHeardFilter.seconds != 0L &&
                            (nowSeconds - node.lastHeard) > mapFilterState.lastHeardFilter.seconds &&
                            !isOurNode -> false
                        else -> true
                    }
                }
            }

        Box(modifier = modifier) {
            OsmCanvasMap(viewport = viewport, modifier = Modifier.fillMaxSize()) { toScreenOffset, viewportSize ->
                LaunchedEffect(viewportSize) { lastViewportSize = viewportSize }
                LaunchedEffect(nodesWithPosition, viewportSize) {
                    if (
                        !hasCentered.value &&
                        nodesWithPosition.isNotEmpty() &&
                        viewportSize.x > 0f &&
                        viewportSize.y > 0f
                    ) {
                        val lats = nodesWithPosition.map { it.latitude }
                        val lons = nodesWithPosition.map { it.longitude }
                        viewport.fitToBounds(lons.min(), lats.min(), lons.max(), lats.max(), viewportSize)
                        hasCentered.value = true
                    }
                }
                if (visibleLayerFeatures.isNotEmpty()) {
                    MapLayersOverlay(
                        features = visibleLayerFeatures,
                        toScreenOffset = toScreenOffset,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (mapFilterState.showPrecisionCircle) {
                    visibleNodes.forEach { node ->
                        val position = node.validPosition ?: return@forEach
                        val precisionBits = position.precision_bits ?: 0
                        if (precisionBits <= 0) return@forEach
                        val lat = (position.latitude_i ?: 0) * 1e-7
                        val lon = (position.longitude_i ?: 0) * 1e-7
                        val screenOffset = toScreenOffset(lon, lat)
                        val radiusMeters = precisionBitsToMeters(precisionBits)
                        val radiusScreenUnits = (radiusMeters / metersPerScreenUnit(lat, viewport.zoom)).toFloat()
                        PrecisionCircle(
                            radius = radiusScreenUnits,
                            color = Color(node.colors.second),
                            modifier = Modifier.offset { IntOffset(screenOffset.x.toInt(), screenOffset.y.toInt()) },
                        )
                    }
                }
                visibleNodes.forEach { node ->
                    val position = node.validPosition ?: return@forEach
                    val lat = (position.latitude_i ?: 0) * 1e-7
                    val lon = (position.longitude_i ?: 0) * 1e-7
                    val screenOffset = toScreenOffset(lon, lat)
                    NodeMarker(
                        node = node,
                        modifier = Modifier.offset { IntOffset(screenOffset.x.toInt(), screenOffset.y.toInt()) },
                        onClick = { navigateToNodeDetails(node.num) },
                    )
                }
                if (mapFilterState.showWaypoints) {
                    waypoints.values.forEach { packet ->
                        val pt = packet.waypoint ?: return@forEach
                        val geofence = pt.toGeofence() ?: return@forEach
                        geofence.circle?.let { circle ->
                            val screenOffset = toScreenOffset(circle.centerLon, circle.centerLat)
                            val radiusScreenUnits =
                                (circle.radiusMeters / metersPerScreenUnit(circle.centerLat, viewport.zoom)).toFloat()
                            GeofenceCircleOverlay(
                                radius = radiusScreenUnits,
                                modifier =
                                Modifier.offset { IntOffset(screenOffset.x.toInt(), screenOffset.y.toInt()) },
                            )
                        }
                        geofence.box?.let { box ->
                            val topLeft = toScreenOffset(box.west, box.north)
                            val bottomRight = toScreenOffset(box.east, box.south)
                            GeofenceBoxOverlay(
                                width = bottomRight.x - topLeft.x,
                                height = bottomRight.y - topLeft.y,
                                modifier = Modifier.offset { IntOffset(topLeft.x.toInt(), topLeft.y.toInt()) },
                            )
                        }
                    }
                    waypoints.values.forEach { packet ->
                        val pt = packet.waypoint ?: return@forEach
                        val lat = (pt.latitude_i ?: 0) * 1e-7
                        val lon = (pt.longitude_i ?: 0) * 1e-7
                        val screenOffset = toScreenOffset(lon, lat)
                        WaypointMarker(
                            emoji = pt.icon.waypointIconOrDefault().toCodePointString(),
                            isLocked = pt.isLocked,
                            modifier = Modifier.offset { IntOffset(screenOffset.x.toInt(), screenOffset.y.toInt()) },
                            onClick = { selectedWaypointId = pt.id },
                        )
                    }
                }

                selectedWaypointId?.let { id ->
                    waypoints[id]?.waypoint?.let { pt ->
                        WaypointDetailsDialog(
                            waypoint = pt,
                            canDeleteForEveryone = pt.isModifiableBy(viewModel.myNodeNum) && isConnected,
                            onDeleteForMe = {
                                viewModel.deleteWaypoint(pt.id)
                                selectedWaypointId = null
                            },
                            onDeleteForEveryone = {
                                viewModel.sendWaypoint(pt.copy(expire = 1))
                                viewModel.deleteWaypoint(pt.id)
                                selectedWaypointId = null
                            },
                            onDismiss = { selectedWaypointId = null },
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { showDownloadDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(imageVector = MeshtasticIcons.Download, contentDescription = "Download offline region")
            }

            FloatingActionButton(
                onClick = { showSitePlanner = true },
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            ) {
                Icon(imageVector = MeshtasticIcons.CellTower, contentDescription = "Site Planner coverage estimate")
            }

            if (showDownloadDialog) {
                DownloadRegionDialog(
                    getCurrentBounds = { viewport.visibleBounds(lastViewportSize) },
                    onStartDownload = { tiles, onProgress -> downloadTiles(httpClient, tiles, onProgress) },
                    coroutineScope = coroutineScope,
                    onDismiss = { showDownloadDialog = false },
                )
            }

            if (showSitePlanner) {
                DesktopSitePlannerHost(
                    initialParams = sitePlannerTargetNode.toSitePlannerParams(channelSet),
                    onDismiss = { showSitePlanner = false },
                    onImport = { name, geoJson, _, _ -> layerManager.addGeoJsonLayer(name, geoJson) },
                    onUseNodeLocation =
                    ourNode?.takeIf { it.validPosition != null }?.let { node -> { node.latitude to node.longitude } },
                )
            }
        }
    }
}

/**
 * Offline-region download dialog. The region is always the *current* map viewport (re-read from [getCurrentBounds] on
 * each recomposition while open) rather than a separately drawn rectangle — mirrors the Android fork's
 * `generateBoxOverlay()`, which also just reads the live viewport bounds instead of offering a distinct
 * drag-a-rectangle tool. [onStartDownload] downloads sequentially and reports (completed, total) progress; a failed
 * tile is skipped rather than aborting the batch, so a partial download still leaves a usable region.
 *
 * NOTE (recovery script): includes the "Manage Cache" button and nested CacheManagementDialog (tile count + size in
 * MB, "Clear Cache" calling TileCache.clear()) — fully recovered verbatim below, not a gap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadRegionDialog(
    getCurrentBounds: () -> GeoBounds,
    onStartDownload: suspend (tiles: List<TileCoord>, onProgress: (Int, Int) -> Unit) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit,
) {
    var zoomRange by remember { mutableStateOf(0f..DEFAULT_DOWNLOAD_ZOOM_SPAN.toFloat()) }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var showCacheManagement by remember { mutableStateOf(false) }

    val bounds = getCurrentBounds()
    val minZoom = zoomRange.start.toInt().coerceIn(DOWNLOAD_MIN_ZOOM, DOWNLOAD_MAX_ZOOM)
    val maxZoom = zoomRange.endInclusive.toInt().coerceIn(minZoom, DOWNLOAD_MAX_ZOOM)
    val tileCount =
        remember(bounds, minZoom, maxZoom) {
            tilesInBounds(bounds.minLon, bounds.minLat, bounds.maxLon, bounds.maxLat, minZoom..maxZoom).size
        }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text("Download Offline Region") },
        text = {
            Column {
                Text("Downloads the currently visible map area for zoom levels $minZoom–$maxZoom.")
                Text("Estimated tiles: $tileCount")
                RangeSlider(
                    value = zoomRange,
                    onValueChange = { if (!isDownloading) zoomRange = it },
                    valueRange = DOWNLOAD_MIN_ZOOM.toFloat()..DOWNLOAD_MAX_ZOOM.toFloat(),
                    steps = DOWNLOAD_MAX_ZOOM - DOWNLOAD_MIN_ZOOM - 1,
                )
                progress?.let { (completed, total) ->
                    LinearProgressIndicator(
                        progress = { if (total > 0) completed.toFloat() / total else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("$completed / $total tiles")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isDownloading,
                onClick = {
                    isDownloading = true
                    val tiles =
                        tilesInBounds(bounds.minLon, bounds.minLat, bounds.maxLon, bounds.maxLat, minZoom..maxZoom)
                    coroutineScope.launch {
                        onStartDownload(tiles) { completed, total -> progress = completed to total }
                        isDownloading = false
                    }
                },
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showCacheManagement = true }) { Text("Manage Cache") }
                TextButton(enabled = !isDownloading, onClick = onDismiss) { Text(stringResource(Res.string.close)) }
            }
        },
    )

    if (showCacheManagement) {
        CacheManagementDialog(onDismiss = { showCacheManagement = false })
    }
}

/** Shows the offline tile cache's total size/tile count (computed off the main thread) with a clear/purge action. */
@Composable
private fun CacheManagementDialog(onDismiss: () -> Unit) {
    var stats by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var cleared by remember { mutableStateOf(false) }

    LaunchedEffect(cleared) {
        stats = withContext(Dispatchers.IO) { TileCache.totalSizeBytes() to TileCache.tileCount() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offline Tile Cache") },
        text = {
            val (sizeBytes, tileCount) = stats ?: return@AlertDialog Text("Calculating…")
            val sizeMb = sizeBytes / (BYTES_PER_MB)
            Text("$tileCount tiles, ${sizeMb}MB on disk")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    TileCache.clear()
                    cleared = !cleared
                },
            ) {
                Text("Clear Cache")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } },
    )
}

@Composable
private fun NodeMarker(node: Node, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier =
        modifier
            // Anchors the pin's bottom tip (not its center) on the lat/lon point — matches how the LocationOn
            // glyph's visual pin shape touches down at the bottom-center of its viewBox.
            .offset(x = -(PIN_SIZE_DP / 2).dp, y = -PIN_SIZE_DP.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = MeshtasticIcons.LocationOn,
            contentDescription = null,
            tint = Color(node.colors.second),
            modifier = Modifier.size(PIN_SIZE_DP.dp),
        )
        Text(
            text = node.user.short_name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            modifier =
            Modifier.background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                .padding(horizontal = 2.dp),
        )
    }
}

/** Orange circular geofence overlay, matching the Android fork's `GEOFENCE_OVERLAY_COLOR` styling. */
@Composable
private fun GeofenceCircleOverlay(radius: Float, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .offset(x = -radius.dp, y = -radius.dp)
            .size((radius * 2).dp)
            .background(GEOFENCE_FILL_COLOR, CircleShape)
            .border(GEOFENCE_STROKE_WIDTH_DP.dp, GEOFENCE_STROKE_COLOR, CircleShape),
    )
}

/** Orange rectangular geofence overlay for a [GeofenceBox], positioned at its top-left (west/north) corner. */
@Composable
private fun GeofenceBoxOverlay(width: Float, height: Float, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .size(width.dp, height.dp)
            .background(GEOFENCE_FILL_COLOR)
            .border(GEOFENCE_STROKE_WIDTH_DP.dp, GEOFENCE_STROKE_COLOR),
    )
}

/** A translucent circle showing GPS precision uncertainty radius, centered on the node's reported position. */
@Composable
private fun PrecisionCircle(radius: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .offset(x = -radius.dp, y = -radius.dp)
            .size((radius * 2).dp)
            .background(color.copy(alpha = PRECISION_CIRCLE_ALPHA), CircleShape),
    )
}

/** A tappable emoji-glyph waypoint marker, centered on its lat/lon point, with a small lock badge when locked. */
@Composable
private fun WaypointMarker(emoji: String, isLocked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier =
        modifier
            .offset(x = -(WAYPOINT_MARKER_SIZE_DP / 2).dp, y = -(WAYPOINT_MARKER_SIZE_DP / 2).dp)
            .clickable(onClick = onClick),
    ) {
        Text(text = emoji, fontSize = TextUnit.Unspecified)
        if (isLocked) {
            Icon(
                imageVector = MeshtasticIcons.Lock,
                contentDescription = stringResource(Res.string.locked),
                modifier =
                Modifier.offset(x = (WAYPOINT_MARKER_SIZE_DP - LOCK_BADGE_SIZE_DP).dp, y = 0.dp)
                    .size(LOCK_BADGE_SIZE_DP.dp),
            )
        }
    }
}

/**
 * Waypoint tap dialog — trimmed desktop equivalent of the Android fork's `WaypointInfoDialog`/`DeleteWaypointDialog`.
 * Full editing (position, icon, name) is left out of scope for now; this covers viewing details and deleting.
 */
@Composable
private fun WaypointDetailsDialog(
    waypoint: Waypoint,
    canDeleteForEveryone: Boolean,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val expireText =
        when {
            waypoint.expire == 0 || waypoint.expire == Int.MAX_VALUE -> "Never"
            waypoint.expire * 1000L <= nowMillis -> "Expired"
            else -> DateFormatter.formatRelativeTime(waypoint.expire * 1000L)
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(waypoint.name.ifBlank { stringResource(Res.string.waypoint_new) }) },
        text = {
            Column {
                if (waypoint.description.isNotBlank()) Text(waypoint.description)
                Text("${stringResource(Res.string.expires)}: $expireText")
                if (waypoint.isLocked) Text(stringResource(Res.string.locked))
            }
        },
        confirmButton = { TextButton(onClick = onDeleteForMe) { Text(stringResource(Res.string.delete_for_me)) } },
        dismissButton = {
            Row {
                if (canDeleteForEveryone) {
                    TextButton(onClick = onDeleteForEveryone) {
                        Text(stringResource(Res.string.delete_for_everyone))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
            }
        },
    )
}
