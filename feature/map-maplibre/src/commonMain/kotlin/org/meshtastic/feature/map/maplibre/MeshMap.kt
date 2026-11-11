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
package org.meshtastic.feature.map.maplibre

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.koin.compose.koinInject
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.interaction.ClickEvent
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.location.BearingUpdate
import org.maplibre.compose.location.LocationState
import org.maplibre.compose.location.LocationTrackingEffect
import org.maplibre.compose.location.updateCamera
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.LocalMapState
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.material3.LocationIndicatorDefaults
import org.maplibre.compose.material3.LocationIndicatorLayer
import org.maplibre.compose.overlay.include
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.Node
import org.meshtastic.core.ui.theme.PauseGrayscaleLayer
import org.meshtastic.core.ui.theme.WithSystemDensity
import org.meshtastic.core.ui.util.MapViewReset
import org.meshtastic.feature.map.BaseMapViewModel
import org.meshtastic.feature.map.MapLink
import org.meshtastic.feature.map.MapNodePolicy
import org.meshtastic.feature.map.component.MapEngineUnavailable
import org.meshtastic.feature.map.component.MeshMapFitPadding
import org.meshtastic.feature.map.maplibre.component.PropagationOverlay
import org.meshtastic.feature.map.maplibre.component.meshMapOrnaments
import org.meshtastic.feature.map.maplibre.geojson.ClusterMember
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.feature.map.maplibre.layers.CustomLayer
import org.meshtastic.feature.map.maplibre.layers.CustomLayers
import org.meshtastic.feature.map.maplibre.layers.MapOverlayLayers
import org.meshtastic.feature.map.maplibre.layers.NodeLayers
import org.meshtastic.feature.map.maplibre.layers.NodeLinkLayers
import org.meshtastic.feature.map.maplibre.layers.PROPAGATION_DEFAULT_OPACITY
import org.meshtastic.feature.map.maplibre.layers.PROPAGATION_LAYER_ID
import org.meshtastic.feature.map.maplibre.layers.PropagationLayer
import org.meshtastic.feature.map.maplibre.layers.RasterBasemapLayer
import org.meshtastic.feature.map.maplibre.layers.TerrainLayers
import org.meshtastic.feature.map.maplibre.layers.WaypointLayers
import org.meshtastic.feature.map.maplibre.layers.nodeTapAt
import org.meshtastic.feature.map.maplibre.style.Basemap
import org.meshtastic.feature.map.maplibre.style.Basemaps
import org.meshtastic.feature.map.maplibre.style.MapColors
import org.meshtastic.feature.map.maplibre.style.MapOverlay
import org.meshtastic.feature.map.maplibre.style.toBaseStyle
import org.meshtastic.feature.map.maplibre.style.zoomRange
import org.meshtastic.feature.map.maplibre.tiles.InstallRadarSmoothing
import org.meshtastic.feature.map.propagation.PropagationController
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import org.maplibre.compose.layers.LocationIndicatorDefaults as CoreLocationIndicatorDefaults

/**
 * Everything the mesh map draws, as one [MapState].
 *
 * Since maplibre-compose 0.16.0 the base style and the sources and layers over it belong to the state rather than to a
 * trailing block on `MaplibreMap`, so this is where the mesh data is read and turned into layers. [MeshMap] presents
 * the result. The state is returned because the caller also needs it: the toolbar reads the bearing off it, the zoom
 * buttons move it, and the Planner asks it where the map is pointed.
 *
 * The camera effects stay out of the style block, even though that is where upstream puts `LocationTrackingEffect`. The
 * library hosts style content in a subcomposition keyed on the loaded style and disposes it on every base-style switch,
 * so a `remember` in there does not survive the user changing basemap.
 *
 * Shared by the F-Droid Android flavor and the desktop app — the two differ only in what they hand in, not in what gets
 * drawn.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
fun rememberMeshMapState(
    viewModel: BaseMapViewModel,
    navigateToNodeDetails: (Int) -> Unit,
    basemap: Basemap = Basemaps.default,
    /** Where the map opens. [rememberRestoredCamera] supplies the position the user left it on. */
    initialCameraPosition: CameraPosition = CameraPosition(),
    overlays: List<MapOverlay> = emptyList(),
    layerOpacity: Map<String, Float> = emptyMap(),
    customLayers: List<CustomLayer> = emptyList(),
    /** Called with the nodes of a tapped cluster that cannot be zoomed apart any further. */
    onClusterMembers: (List<ClusterMember>) -> Unit = {},
    onWaypointClick: (Int) -> Unit = {},
    /** The first corner of a box being authored, drawn so the tap reads as registered. */
    boxCorner: Position? = null,
    /** Location and heading state to draw a puck for and optionally follow. Null disables both. */
    locationState: LocationState? = null,
    /**
     * Whether the user has asked to be followed. Gates both the camera and the puck, so switching tracking off leaves
     * no stale dot behind — and matches the Google flavor, which shows the dot only while tracking.
     */
    followLocation: Boolean = false,
    /** How a location update should affect camera bearing. [BearingUpdate.Ignore] follows position only. */
    bearingUpdate: BearingUpdate = BearingUpdate.Ignore,
    /**
     * Whether to frame the mesh once positions arrive. False when the caller has restored a remembered camera, so the
     * user's own view is not yanked away from them.
     */
    frameOnNodes: Boolean = true,
    /**
     * A node to fly to and draw above every other ("Show on map"). It stays visible whatever the map filters say, and
     * is drawn on its own, outside the clusters, so several nodes on one spot cannot bury it.
     */
    focusNodeNum: Int? = null,
    /** Radio links between nodes, drawn under the node chips while the "links" filter is on. */
    links: List<MapLink> = emptyList(),
): MapState {
    val nodes by viewModel.nodesWithPosition.collectAsStateWithLifecycle()
    val waypoints by viewModel.waypoints.collectAsStateWithLifecycle()
    val filterState by viewModel.mapFilterStateFlow.collectAsStateWithLifecycle()
    val myNodeInfo by viewModel.myNodeInfo.collectAsStateWithLifecycle()
    val ourNode by viewModel.ourNodeInfo.collectAsStateWithLifecycle()
    val displayUnits by viewModel.displayUnits.collectAsStateWithLifecycle()

    val propagationController: PropagationController = koinInject()
    val propagation by propagationController.state.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val focusedNode = remember(nodes, focusNodeNum) { focusNodeNum?.let { num -> nodes.firstOrNull { it.num == num } } }
    // The mesh-wide filter is computed once per data change, not per camera frame.
    // `nowSeconds` is deliberately not a key: a packet arriving already changes the node list.
    val visibleNodes =
        remember(nodes, filterState, myNodeInfo, ourNode) {
            MapNodePolicy.visibleNodes(nodes, filterState, nowSeconds, myNodeInfo?.myNodeNum, ourNode)
        }

    InstallRadarSmoothing()
    val mapState =
        rememberMapState(baseStyle = basemap.toBaseStyle(), initialCameraPosition = initialCameraPosition) {
            val state = checkNotNull(LocalMapState.current)
            // Whole zoom levels only: the contour tiles follow the level, not every frame of a pinch.
            val zoomLevel by remember(state) { derivedStateOf { state.cameraPosition.zoom.toInt() } }
            // The viewport local, rather than the state's own property: it is what the library scopes viewport
            // recomposition to, and these reads are the whole reason this block runs per frame.
            // Held still while the camera stays inside it: reading the live viewport here made this whole block (every
            // layer) recompose on each frame of a pan, which is what cost the iPhone its smoothness.
            val viewportBounds by remember(state) {
                val held = StickyBounds()
                derivedStateOf { held.update(state.viewport?.visibleBounds?.toBoundingBox()) }
            }

            if (basemap is Basemap.Raster) {
                RasterBasemapLayer(basemap)
            }
            MapOverlayLayers(overlays, layerOpacity)
            PropagationLayer(propagation, layerOpacity[PROPAGATION_LAYER_ID] ?: PROPAGATION_DEFAULT_OPACITY)
            TerrainLayers(
                viewportBounds = viewportBounds,
                zoom = zoomLevel.toDouble(),
                displayUnits = displayUnits,
            )
            CustomLayers(customLayers, layerOpacity)

            if (filterState.showWaypoints) {
                WaypointLayers(waypoints = waypoints.values, onWaypointClick = onWaypointClick)
            }

            if (filterState.showLinks) {
                NodeLinkLayers(links = links, nodes = nodes)
            }

            MeshMapNodeLayers(
                visibleNodes = visibleNodes,
                mapState = state,
                viewportBounds = viewportBounds,
                filterState = filterState,
                myNodeNum = myNodeInfo?.myNodeNum,
                focusedNode = focusedNode,
                navigateToNodeDetails = navigateToNodeDetails,
                onClusterMembers = onClusterMembers,
                scope = scope,
            )

            // Declared last so the user's own position and an in-progress box corner draw above the mesh.
            UserLocationPuck(locationState = locationState, visible = followLocation)
            BoxCornerMarker(boxCorner)
        }

    // Both effects stay out here, in this composition rather than the style block's. The library hosts the style
    // content in a subcomposition keyed on the loaded style and disposes it on every base-style switch, so a
    // `remember` in there is lost whenever the user changes basemap — which would reset FrameOnce's latch and
    // re-frame the mesh over wherever they had panned to.
    // The propagation regions are downloaded again together once the oldest forecast is old.
    LaunchedEffect(propagation.enabled, propagation.autoRefresh) {
        if (propagation.enabled && propagation.autoRefresh) {
            while (true) {
                propagationController.ensureFor()
                delay(PROPAGATION_CHECK_MS)
            }
        }
    }
    FrameOnce(enabled = frameOnNodes, nodes = visibleNodes, mapState = mapState)
    FlyToFocusedNode(focusNodeNum = focusNodeNum, focusedNode = focusedNode, mapState = mapState)

    // Follows position whenever tracking is on; touches bearing only when the caller asks for it, so a user who
    // has rotated the map is not straightened out behind their back.
    FollowUserLocation(
        locationState = locationState,
        mapState = mapState,
        followLocation = followLocation,
        bearingUpdate = bearingUpdate,
    )

    return mapState
}

/**
 * The mesh map, rendered by MapLibre.
 *
 * Presents [mapState] and nothing else; what gets drawn is declared by [rememberMeshMapState].
 *
 * [basemap] must be the same one that state was built with: it supplies the zoom range the camera is held to, and a
 * different value here would clamp the camera to a range the loaded style cannot serve.
 */
@Composable
fun MeshMap(
    mapState: MapState,
    modifier: Modifier = Modifier,
    basemap: Basemap = Basemaps.default,
    /** HTML credits for drawn data no style source carries; see [meshMapOrnaments]. */
    extraCredits: List<String> = emptyList(),
    /** Called with the pressed position on a long press, which is how a new waypoint gets placed. */
    onMapLongClick: (Position) -> Unit = {},
    /** Called with the tapped position. Used to collect the two corners of a waypoint's geofence bounding box. */
    onMapClick: (Position) -> Unit = {},
    /** Called with a node whose name bubble was tapped. */
    onNodeClick: ((Int) -> Unit)? = null,
    /** Called with the nodes standing on one spot when a tap lands on that stack. */
    onNodeStack: ((List<ClusterMember>) -> Unit)? = null,
) {
    // No engine on this device means presenting a map is the UnsatisfiedLinkError crash in #7001. Covers the view
    // only: `rememberMapState` builds the native runtime itself, so a runtime that cannot come up at all has already
    // thrown by the time this is reached.
    if (!LocalMapLibreRuntimeProbe.current()) return MapEngineUnavailable(modifier)

    // The store-screenshot capture waits for this tag instead of a fixed delay.
    LaunchedEffect(mapState) {
        // Idle can arrive before the first render session; only an idle after a drawn frame means tiles are on screen.
        mapState.events.dropWhile { it !is MapEvent.FrameRendered }.filterIsInstance<MapEvent.Idle>().first()
        Logger.withTag("MapDrawn").d { "tiles drawn" }
        ReturnView.setHome(mapState)
    }

    val scope = rememberCoroutineScope()
    DisposableEffect(mapState) { onDispose { ReturnView.forget(mapState) } }
    // Pressing the Map tab while the map is open goes back to the view the map had on entering.
    LaunchedEffect(mapState) {
        MapViewReset.events.collect { _ ->
            ReturnView.take(mapState)?.let { first -> mapState.animateCamera(first.toCameraUpdate()) }
        }
    }
    MapFrameStats(mapState)
    PauseGrayscaleLayer()
    val zoomRange = basemap.zoomRange()
    val credits = extraCredits.distinct()
    val ornaments = remember(credits) { meshMapOrnaments(credits) }
    // The size the map really takes on screen, which is what the finger is measured against. The map reports its own
    // size separately; if the two ever differ, taps are scaled back by the ratio.
    var shown by remember { mutableStateOf(DpSize.Zero) }
    Box(modifier) {
        WithSystemDensity {
            // Measured in the map's own dp, which is the unit its taps and projection use.
            val mapDensity = LocalDensity.current
            MaplibreMap(
                modifier =
                Modifier.fillMaxSize().onSizeChanged {
                    shown = with(mapDensity) { DpSize(it.width.toDp(), it.height.toDp()) }
                },
                state = mapState,
                // Honour what the source can actually serve, as the OSMdroid map did.
                cameraConstraints =
                CameraConstraints {
                    minZoom = zoomRange.start.toDouble()
                    maxZoom = zoomRange.endInclusive.toDouble()
                },
                interactions =
                MapInteractions {
                    callbacks {
                        longClick {
                            onEvent { event ->
                                event.correctedPosition(mapState, shown)?.let(onMapLongClick)
                                ClickResult.Consume
                            }
                        }
                        // Pass, not Consume: a tap has to keep reaching the layers underneath or nothing is selectable.
                        click {
                            onEvent { event ->
                                val scale = mapState.displayScale(shown)
                                val finger = event.screenOffset.dividedBy(scale)
                                val tap = event.correctedPosition(mapState, shown)
                                Logger.i {
                                    "Mapa: dotknięcie ekranu w ${event.position}, po korekcie $tap, " +
                                        "widok ${shown.text()}, mapa ${mapState.viewport?.size?.text()}, " +
                                        "skala wyświetlania ${scale.toFloat()}"
                                }
                                // A tap on a node's name bubble belongs to the node, whatever the zoom or rotation; the
                                // check needs the map's answer, so it runs beside the event and the rest of the tap
                                // (waypoints, planner) is only given the taps that hit no node.
                                scope.launch {
                                    val onNode =
                                        tap != null &&
                                            onNodeClick != null &&
                                            nodeTapAt(mapState, tap, finger, onNodeClick, onNodeStack ?: {})
                                    if (!onNode) tap?.let(onMapClick)
                                }
                                ClickResult.Pass
                            }
                        }
                    }
                },
                overlay = { include(ornaments) },
            )
        }
        // As wide as the room left of the zoom buttons, with a small gap to them, and low so it covers little map.
        PropagationOverlay(
            Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 8.dp, end = PROPAGATION_END_CLEARANCE_DP.dp, bottom = 56.dp),
        )
    }
}

/** A size as "width x height" in whole dp, for the log. */
private fun DpSize.text() = "${width.value.toInt()}x${height.value.toInt()}"

private const val DISPLAY_SCALE_MIN = 0.5f
private const val DISPLAY_SCALE_MAX = 2f
private const val DISPLAY_SCALE_TOLERANCE = 0.03f

/**
 * How much larger the map is drawn on screen than the size it was rendered and projected at: [shown] (the room the map
 * takes) over the map's own reported size. 1 whenever the two agree, which is every platform but the iPhone so far,
 * and also when either is not known yet or the ratio is implausible.
 */
internal fun MapState.displayScale(shown: DpSize): Float {
    val rendered = viewport?.size
    val ratio = if (rendered != null && rendered.width.value > 0f) shown.width.value / rendered.width.value else 1f
    val usable = ratio in DISPLAY_SCALE_MIN..DISPLAY_SCALE_MAX && kotlin.math.abs(ratio - 1f) > DISPLAY_SCALE_TOLERANCE
    return if (usable) ratio else 1f
}

private fun DpOffset.dividedBy(scale: Float) = DpOffset(x / scale, y / scale)

/** The tapped position with the map's drawn-larger-than-projected scale taken out of the finger's place. */
private fun ClickEvent.correctedPosition(state: MapState, shown: DpSize): Position? {
    val scale = state.displayScale(shown)
    return if (scale == 1f) position else state.positionFromScreenLocation(screenOffset.dividedBy(scale))
}

/** Remembers the last padded box between recompositions; see [rememberStickyBounds]. */
private class StickyBounds {
    var box: BoundingBox? = null

    /** The held box, replaced by a padded copy of [viewport] only once the viewport leaves it or gets far smaller. */
    fun update(viewport: BoundingBox?): BoundingBox? {
        if (viewport == null) return box
        val current = box
        val inside =
            current != null &&
                viewport.west >= current.west &&
                viewport.east <= current.east &&
                viewport.south >= current.south &&
                viewport.north <= current.north &&
                (viewport.east - viewport.west) * STICKY_MAX_RATIO >= (current.east - current.west)
        if (!inside) box = viewport.padded(CHIP_VIEW_PADDING)
        return box
    }
}

/**
 * The padded viewport, held still while the camera stays well inside it.
 *
 * The viewport changes on every frame of a pan, and everything keyed on it — which chips and precision circles are in
 * view — was recomputed, and handed to the map as a new list, once per frame. This keeps the same box until the real
 * viewport leaves it (or zooming in leaves it far too large), so a drag costs nothing until it has actually travelled.
 */
@Composable
private fun rememberStickyBounds(viewport: BoundingBox?): BoundingBox? = remember { StickyBounds() }.update(viewport)

/** How many times larger than the viewport the held box may get before zooming in rebuilds it. */
private const val STICKY_MAX_RATIO = 4

/** The node chips, clusters and precision circles — split out of [MeshMap] itself only to keep that function short. */
@Composable
@MaplibreComposable
private fun MeshMapNodeLayers(
    visibleNodes: List<Node>,
    mapState: MapState,
    viewportBounds: BoundingBox?,
    filterState: BaseMapViewModel.MapFilterState,
    myNodeNum: Int?,
    focusedNode: Node?,
    navigateToNodeDetails: (Int) -> Unit,
    onClusterMembers: (List<ClusterMember>) -> Unit,
    scope: CoroutineScope,
) {
    val zoomLevel by remember(mapState) { derivedStateOf { floor(mapState.cameraPosition.zoom).toInt() } }
    NodeLayers(
        nodes = visibleNodes,
        focusedNode = focusedNode,
        // Padded so a modest pan keeps the same nodes in view, and the chips are not redrawn for every frame of a
        // drag. Without a viewport at all — the first composition — every node is a candidate, as before.
        visibleBounds = rememberStickyBounds(viewportBounds),
        // Floored: clustering indexes per whole zoom level, so the set only changes when the level does — and a
        // pinch does not recompute it for every fractional step in between.
        zoom = zoomLevel,
        myNodeNum = myNodeNum,
        showPrecisionCircles = filterState.showPrecisionCircle,
        onNodeClick = navigateToNodeDetails,
        onClusterMembers = onClusterMembers,
        onClusterZoom = { centre, expansionZoom ->
            scope.launch {
                val current = mapState.cameraPosition
                // A cluster that cannot report an expansion zoom answers with a sentinel (0 on
                // Android and desktop, -1 on iOS), so clamp — never zoom out on a tap.
                ReturnView.remember(mapState)
                mapState.animateCamera(
                    CameraUpdate(center = centre, zoom = maxOf(expansionZoom, current.zoom)),
                    CameraAnimation.Ease.Standard,
                )
            }
        },
    )
}

/**
 * Frames the mesh once, the first time positions arrive.
 *
 * Once only: re-fitting on every node update would yank the camera away from wherever the user had panned to, and on a
 * mesh that is still filling in that would be continuous.
 */
@Composable
private fun FrameOnce(enabled: Boolean, nodes: List<Node>, mapState: MapState) {
    if (!enabled) return

    var hasFramed by remember { mutableStateOf(false) }
    val hasViewport = mapState.viewport != null
    // The node list is read through a snapshot rather than keyed on, so an arriving packet cannot cancel this.
    // Keying on it meant the effect restarted mid-fit: `frameBounds` suspends, and whether the latch was set
    // before the call (fit lost, latch kept, mesh never framed) or after it (latch lost to a user pan, so a later
    // packet re-frames over them) one of the two failure modes was always reachable. Nothing here restarts on
    // node changes now, so the latch and the fit cannot come apart.
    val currentNodes by rememberUpdatedState(nodes)
    val fitPadding = MeshMapFitPadding.toDpPadding(LocalLayoutDirection.current)
    // An effect, not composition-body work: a launch from composition fires even if the composition is
    // abandoned, while its state write is rolled back — a camera jump with no framing recorded. Fitting before
    // the map reports a viewport silently lands on a default, hence the gate.
    LaunchedEffect(hasViewport) {
        if (hasFramed || !hasViewport) return@LaunchedEffect
        // Waits for the first node set that has anything to frame; a mesh still filling in reports none.
        val box = snapshotFlow { nodesBoundingBox(currentNodes) }.filterNotNull().first()
        hasFramed = true
        mapState.frameBounds(box, padding = fitPadding)
    }
}

/**
 * Flies to the node "Show on map" asked for, once per request.
 *
 * Waits for the map to report a viewport and for the node to be in the list (a node list still filling in has none
 * yet). The node is read through a snapshot, so an arriving packet cannot restart the flight half way; once the camera
 * has moved, the user is free to pan away and nothing pulls them back.
 */
@Composable
private fun FlyToFocusedNode(focusNodeNum: Int?, focusedNode: Node?, mapState: MapState) {
    if (focusNodeNum == null) return

    val hasViewport = mapState.viewport != null
    val currentNode by rememberUpdatedState(focusedNode)
    LaunchedEffect(focusNodeNum, hasViewport) {
        if (!hasViewport) return@LaunchedEffect
        val node = snapshotFlow { currentNode }.filterNotNull().first()
        ReturnView.remember(mapState)
        mapState.animateCamera(
            CameraUpdate(center = Position(longitude = node.longitude, latitude = node.latitude), zoom = FOCUS_ZOOM),
            CameraAnimation.Ease.Standard,
        )
    }
}

/** Close enough to tell apart nodes standing on the same site; the basemap's own maximum still caps it. */
private const val FOCUS_ZOOM = 16.0

/** Keeps the camera on the user while tracking is on. No-op without a location source. */
@Composable
private fun FollowUserLocation(
    locationState: LocationState?,
    mapState: MapState,
    followLocation: Boolean,
    bearingUpdate: BearingUpdate,
) {
    if (locationState == null) return

    LocationTrackingEffect(
        locationState = locationState,
        enabled = followLocation,
        trackBearing = bearingUpdate != BearingUpdate.Ignore,
    ) {
        updateCamera(mapState = mapState, updateBearing = bearingUpdate)
    }
}

/** The user's position, accuracy and heading. Drawn only while tracking, so switching it off leaves no stale dot. */
@Composable
@MaplibreComposable
private fun UserLocationPuck(locationState: LocationState?, visible: Boolean) {
    if (locationState == null) return

    // The indicator has no notion of age, so a fix that stops arriving is greyed and loses its accuracy ring here,
    // or an old position reads as a current one.
    val mark = locationState.lastLocationMeasurementMark
    var stale by remember(mark) { mutableStateOf(mark != null && mark.elapsedNow() > STALE_LOCATION_AFTER) }
    LaunchedEffect(mark) {
        if (mark == null || stale) return@LaunchedEffect
        delay(STALE_LOCATION_AFTER - mark.elapsedNow())
        stale = true
    }
    val colors = MaterialTheme.colorScheme

    // The state overload, which resolves the latest measurement and its most accurate bearing itself. Hidden rather
    // than unmounted when tracking stops, since layer additions are queued and a quick toggle could lose the re-add.
    LocationIndicatorLayer(
        id = "user-location",
        locationState = locationState,
        visible = visible,
        accuracyRadiusColor = if (stale) Color.Transparent else colors.primary.copy(alpha = ACCURACY_FILL_ALPHA),
        accuracyRadiusBorderColor = if (stale) Color.Transparent else colors.primary,
        topImage =
        if (stale) {
            CoreLocationIndicatorDefaults.topImage(colors.surfaceDim, colors.onPrimary)
        } else {
            LocationIndicatorDefaults.topImage()
        },
    )
}

/** How long a location fix counts as current, the threshold the library's own puck used before 0.18. */
private val STALE_LOCATION_AFTER = 30.seconds

private const val ACCURACY_FILL_ALPHA = 0.15f

/**
 * The first corner tapped while authoring a geofence box.
 *
 * The flow commits on the second tap, so there is never a both-corners-uncommitted state to preview a rectangle from —
 * the Google flavor marks the single corner for the same reason.
 */
@Composable
@MaplibreComposable
private fun BoxCornerMarker(corner: Position?) {
    if (corner == null) return

    val source =
        rememberFeatureSource(corner) {
            FeatureCollection(listOf(Feature<Point, JsonObject?>(geometry = Point(corner), properties = null)))
        }
    CircleLayer(
        id = "box-corner",
        source = source,
        color = const(MapColors.Highlight),
        radius = const(8.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(2.dp),
    )
}

/**
 * How far past the screen edge a node still counts as "in view" for chip drawing, as a fraction of the visible span.
 *
 * Half a screen in each direction: enough that a flick does not immediately invalidate the chip set, small enough that
 * a dense mesh does not blow the image budget on nodes well off screen.
 */
private const val CHIP_VIEW_PADDING = 0.5

private const val PROPAGATION_CHECK_MS = 60_000L

/** Room kept free on the right for the zoom buttons (their width and margin) plus a small gap. */
private const val PROPAGATION_END_CLEARANCE_DP = 88

/** Diagnostics for the logs: how smooth the map is while it moves (only a window with slow frames is written). */
@Composable
private fun MapFrameStats(mapState: MapState) {
    LaunchedEffect(mapState) {
        var mark = TimeSource.Monotonic.markNow()
        var frames = 0
        var slow = 0
        var longest = 0L
        snapshotFlow { mapState.cameraPosition }
            .collect {
                val gap = mark.elapsedNow().inWholeMilliseconds
                mark = TimeSource.Monotonic.markNow()
                if (gap < FRAME_IDLE_GAP_MS) {
                    frames++
                    if (gap > SLOW_FRAME_MS) slow++
                    longest = maxOf(longest, gap)
                    if (frames >= FRAME_WINDOW) {
                        if (slow > 0) Logger.i { "Mapa: klatek $frames, wolnych $slow, najdłuższa przerwa $longest ms" }
                        frames = 0
                        slow = 0
                        longest = 0L
                    }
                }
            }
    }
}

private const val FRAME_WINDOW = 60
private const val SLOW_FRAME_MS = 32
private const val FRAME_IDLE_GAP_MS = 250
