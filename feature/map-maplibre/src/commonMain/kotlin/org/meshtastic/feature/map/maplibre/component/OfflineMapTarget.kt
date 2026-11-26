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
@file:Suppress("LongMethod")

package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.offline.DownloadProgress
import org.maplibre.compose.offline.DownloadStatus
import org.maplibre.compose.offline.OfflinePack
import org.maplibre.compose.offline.OfflinePackDefinition
import org.maplibre.compose.offline.OfflineStorage
import org.maplibre.compose.offline.OfflineStorageState
import org.maplibre.compose.offline.offlineStorage
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.common.util.formatByteSize
import org.meshtastic.core.common.util.ioDispatcher
import org.meshtastic.core.common.util.safeCatching
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.map_cache_manager
import org.meshtastic.core.resources.map_cache_tiles
import org.meshtastic.core.resources.map_download_status_complete
import org.meshtastic.core.resources.map_download_status_downloading
import org.meshtastic.core.resources.map_download_status_paused
import org.meshtastic.core.resources.map_offline_create_failed
import org.meshtastic.core.resources.map_offline_unavailable
import org.meshtastic.core.resources.map_select_download_region
import org.meshtastic.core.resources.map_start_download
import org.meshtastic.core.resources.map_tile_download_estimate
import org.meshtastic.core.resources.map_tile_limit_reached
import org.meshtastic.core.resources.map_zoom_levels
import org.meshtastic.core.resources.offline_area_hint
import org.meshtastic.core.resources.offline_area_radius_km
import org.meshtastic.core.resources.offline_area_title
import org.meshtastic.core.resources.offline_area_visible
import org.meshtastic.core.resources.offline_detail_title
import org.meshtastic.core.resources.offline_estimate_size
import org.meshtastic.core.resources.offline_maps_empty
import org.meshtastic.core.resources.offline_size_warning
import org.meshtastic.core.ui.component.Button
import org.meshtastic.core.ui.component.IndeterminateProgressLine
import org.meshtastic.core.ui.component.ProgressLine
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PlayArrow
import org.meshtastic.feature.map.layers.mapLayerFileSystem
import org.meshtastic.feature.map.layers.mapLayersDirectory
import org.meshtastic.feature.map.maplibre.style.Basemap
import org.meshtastic.feature.map.maplibre.style.offlineStyleJson
import org.meshtastic.feature.map.maplibre.tileCount
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.roundToInt

/** What the map must tell the offline sheet: which style to pack, and which region is on screen. */
internal class OfflineMapTarget(
    val styleUrl: String?,
    /** The raster basemap on screen, when there is no [styleUrl]: packed through a style written for it. */
    val rasterBasemap: Basemap.Raster? = null,
    val bounds: () -> BoundingBox?,
    val zoom: () -> Double,
    /** Moves the map onto a downloaded region, so a pack in the list can actually be gone to. */
    val showRegion: (BoundingBox) -> Unit,
)

/**
 * Offline downloads, as a section of the layers sheet.
 *
 * A pack is defined against a style document. A vector basemap has one; a raster basemap (such as the default
 * OpenStreetMap) draws over an empty style, so a small style for it is written out and packed instead.
 */
@Composable
internal fun OfflineMapsSection(target: OfflineMapTarget, onShowRegion: (BoundingBox) -> Unit) {
    // Offline storage hangs off the runtime. The default runtime is the one every map here uses, so its packs are
    // the ones the user sees on the map.
    val manager = DefaultMapRuntime.instance.offlineStorage
    val scope = rememberCoroutineScope()
    val managerState = manager.state.collectAsStateWithLifecycle().value
    val packs = (managerState as? OfflineStorageState.Ready)?.packs.orEmpty()
    var createFailed by remember { mutableStateOf(false) }
    // A pack definition now carries the pixel ratio it was downloaded at, so the tiles match this display.
    val pixelRatio = LocalDensity.current.density

    // MT_SW: besides the area on screen, the person can pick a radius around the centre of the map (up to 1000 km) and
    // how many zoom levels deeper to go; the estimate below follows the choice.
    var radiusKm by remember { mutableStateOf(0) }
    var detail by remember { mutableStateOf(DEFAULT_DETAIL) }
    val area = target.areaFor(radiusKm, detail)
    val range = area?.range ?: target.zoomRange()
    val estimate = area?.bounds?.tileCount(range.first, range.last) ?: 0L
    val usage = rememberCacheUsage(packs)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = stringResource(Res.string.map_cache_manager), style = MaterialTheme.typography.titleMedium)

        CacheUsageLine(storedBytes = usage.bytes, storedTiles = usage.tiles)
        OfflineAreaChoice(
            radiusKm = radiusKm,
            onRadius = {
                radiusKm = it
                Logger.i { "Mapa offline: wybrany zasięg ${if (it == 0) "widoczny obszar" else "$it km"}" }
            },
            detail = detail,
            onDetail = {
                detail = it
                Logger.i { "Mapa offline: szczegółowość +$it" }
            },
        )
        DownloadEstimateLines(estimate = estimate, range = range)
        if (estimate > 0L) {
            val bytes = estimate * ESTIMATED_TILE_BYTES
            Text(
                text = stringResource(Res.string.offline_estimate_size, formatByteSize(bytes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (bytes > BIG_DOWNLOAD_BYTES) {
                Text(
                    text = stringResource(Res.string.offline_size_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (estimate > MAX_PACK_TILES) {
                Text(
                    text = stringResource(Res.string.map_tile_limit_reached, MAX_PACK_TILES.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Button(
            onClick = {
                createFailed = false
                scope.launch {
                    Logger.i {
                        "Mapa offline: pobieranie, zasięg ${if (radiusKm == 0) "widoczny obszar" else "$radiusKm km"}, " +
                            "poziomy ${range.first}–${range.last}, kafelków $estimate"
                    }
                    createFailed = area == null || !manager.downloadArea(target, area, pixelRatio)
                }
            },
            enabled =
            canDownloadOfflinePack(
                managerState is OfflineStorageState.Ready,
                target.styleUrl ?: target.rasterBasemap?.id,
                estimate,
            ) && estimate <= MAX_PACK_TILES,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Text(text = stringResource(Res.string.map_start_download))
        }

        if (createFailed) {
            Text(
                text = stringResource(Res.string.map_offline_create_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        when {
            managerState is OfflineStorageState.Loading ->
                IndeterminateProgressLine(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

            managerState is OfflineStorageState.Failed -> {
                Text(
                    text = stringResource(Res.string.map_offline_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                // Upstream's message, which is not ours to localise.
                managerState.cause.message?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            packs.isEmpty() ->
                Text(
                    text = stringResource(Res.string.offline_maps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

            else ->
                packs.forEach { pack ->
                    OfflinePackRow(
                        pack = pack,
                        onShow = { bounds -> onShowRegion(bounds) },
                        // Hygiene, not an ANR fix: `resume` only posts onto the offline runtime's owner thread,
                        // taking its lock just long enough to enqueue — this dispatch merely keeps that acquisition
                        // off the frame path.
                        onToggle = { scope.launch(ioDispatcher) { manager.resume(pack) } },
                        onDelete = { scope.launch { manager.delete(pack) } },
                    )
                }
        }
    }
}

/**
 * What the next download will cost, before committing to it.
 *
 * The OSMdroid map showed this, and it matters more than it looks: the same two extra zoom levels are a couple of
 * hundred tiles over a neighbourhood and tens of thousands over a state.
 */
@Composable
private fun DownloadEstimateLines(estimate: Long, range: IntRange) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = stringResource(Res.string.map_select_download_region), style = MaterialTheme.typography.bodyMedium)
        Text(
            text =
            stringResource(Res.string.map_tile_download_estimate) +
                " " +
                stringResource(Res.string.map_cache_tiles, estimate.toInt()) +
                "  " +
                stringResource(Res.string.map_zoom_levels, range.first, range.last),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OfflinePackRow(
    pack: OfflinePack,
    onShow: (BoundingBox) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val progress = pack.downloadProgress.collectAsStateWithLifecycle().value
    val bounds = (pack.definition as? OfflinePackDefinition.TilePyramid)?.bounds

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier =
            Modifier.fillMaxWidth(PACK_ROW_TEXT_FRACTION)
                .then(if (bounds != null) Modifier.clickable { onShow(bounds) } else Modifier),
        ) {
            Text(text = pack.label(), style = MaterialTheme.typography.bodyLarge)
            if (progress is DownloadProgress.Healthy && progress.status != DownloadStatus.Complete) {
                ProgressLine(
                    progress = progress.fraction(),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            } else {
                Text(
                    text = progress.summary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val status = (progress as? DownloadProgress.Healthy)?.status
            if (status == DownloadStatus.Paused) {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = MeshtasticIcons.PlayArrow,
                        contentDescription = stringResource(Res.string.map_start_download),
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = MeshtasticIcons.Delete, contentDescription = stringResource(Res.string.delete))
            }
        }
    }
}

/**
 * Packs the region currently on screen, from the current zoom a couple of levels deeper.
 *
 * What bounds the download is the *width* of the zoom range, not an absolute ceiling: a pack covers every tile in its
 * range, but the visible box shrinks as you zoom in, so two extra levels costs roughly the same work at any zoom. An
 * absolute cap is worse than useless here — it silently inverts the range once you are past it, which MapLibre rejects
 * outright with "offline region zoom range is invalid".
 *
 * Errors are caught rather than thrown: `create` reports a native failure by throwing, and this runs in a UI-scoped
 * coroutine, so letting it escape takes the window's event thread down with it.
 *
 * Creates the pack only. A created pack is paused, and stays that way until the user presses play on its row —
 * [OfflineStorage.resume] is deliberately not called here, so a download never starts itself.
 */
private suspend fun OfflineStorage.downloadArea(
    target: OfflineMapTarget,
    area: OfflineArea,
    pixelRatio: Float,
): Boolean {
    val bounds = area.bounds
    val range = area.range

    return safeCatching {
        val styleUrl =
            target.styleUrl ?: target.rasterBasemap?.let { writeOfflineStyle(it) } ?: return@safeCatching null
        create(
            OfflinePackDefinition.TilePyramid(
                styleUrl = styleUrl,
                bounds = bounds,
                pixelRatio = pixelRatio,
                minZoom = range.first.toDouble(),
                maxZoom = range.last.toDouble(),
            ),
        )
    }
        .onFailure { error -> Logger.w(error) { "Could not create an offline pack" } }
        .map { it != null }
        .getOrDefault(false)
}

/**
 * Writes the style a raster basemap is packed through and returns its `file://` URL.
 *
 * Kept in the persistent layers directory rather than a cache: a pack remembers this URL, and a cache the system may
 * empty would leave a paused pack that cannot be resumed.
 */
private suspend fun writeOfflineStyle(basemap: Basemap.Raster): String = withContext(ioDispatcher) {
    val fs = mapLayerFileSystem()
    val dir = mapLayersDirectory() / "offline-styles"
    fs.createDirectories(dir)
    val file = dir / "${basemap.id}.json"
    fs.write(file) { writeUtf8(basemap.offlineStyleJson()) }
    val path = file.toString().replace('\\', '/')
    if (path.startsWith("/")) "file://$path" else "file:///$path"
}

private fun OfflinePack.label(): String {
    val definition = definition
    val bounds = (definition as? OfflinePackDefinition.TilePyramid)?.bounds
    val centre = bounds?.let { "${it.south.round()}, ${it.west.round()}" } ?: EM_DASH
    val minZoom = definition.minZoom.toInt()
    val maxZoom = definition.maxZoom?.toInt() ?: minZoom
    return "$centre  z$minZoom–$maxZoom"
}

private fun DownloadProgress.fraction(): Float = when (this) {
    is DownloadProgress.Healthy ->
        if (requiredResourceCount > 0L) {
            (completedResourceCount.toFloat() / requiredResourceCount.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    else -> 0f
}

/**
 * One line describing a pack's state, assembled from resources rather than written in English.
 *
 * `status.name` went straight into the UI before, so every locale read the library's own enum constants. The tile count
 * and byte size render as the cache figures above render them.
 */
@Composable
private fun DownloadProgress.summary(): String = when (this) {
    is DownloadProgress.Healthy ->
        listOfNotNull(
            when (status) {
                DownloadStatus.Paused -> Res.string.map_download_status_paused
                DownloadStatus.Downloading -> Res.string.map_download_status_downloading
                DownloadStatus.Complete -> Res.string.map_download_status_complete
                else -> null
            }?.let { stringResource(it) },
            stringResource(Res.string.map_cache_tiles, completedTileCount.toInt()),
            formatByteSize(completedResourceBytes),
        )
            .joinToString(SUMMARY_SEPARATOR)

    // Upstream's message, which is not ours to localise, and more useful than a generic failure line.
    is DownloadProgress.Error -> message

    is DownloadProgress.TileLimitExceeded -> stringResource(Res.string.map_tile_limit_reached, limit.toInt())

    else -> EM_DASH
}

private fun Double.round(): String {
    // Rounded, not truncated, so a negative coordinate labels the same way as its positive twin.
    val scaled = (this * COORD_SCALE).roundToInt() / COORD_SCALE
    return scaled.toString()
}

private const val PACK_ROW_TEXT_FRACTION = 0.8f
private const val PACK_EXTRA_ZOOM_LEVELS = 2

/** MaplibreMap's own default zoomRange, which an offline region may not exceed. */
private const val MIN_PACK_ZOOM = 0
private const val MAX_PACK_ZOOM = 20
private const val COORD_SCALE = 100.0

/**
 * The zoom levels a download covers: the current level plus a couple deeper.
 *
 * Deliberately the only place this is worked out. Computing it twice is what produced an inverted range — an absolute
 * ceiling below the current zoom — which MapLibre rejects outright.
 */
/** What a download covers: the box and the zoom levels. */
private class OfflineArea(val bounds: BoundingBox, val range: IntRange)

/**
 * The area for the person's choice: the visible area ([radiusKm] 0) or a box of [radiusKm] around the centre of what is
 * on screen, from the zoom at which the box is about [TILES_ACROSS] tiles wide to [detail] levels deeper. A wider box
 * therefore starts at a lower zoom, which keeps the number of tiles about the same.
 */
private fun OfflineMapTarget.areaFor(radiusKm: Int, detail: Int): OfflineArea? {
    val visible = bounds() ?: return null
    if (radiusKm <= 0) return OfflineArea(visible, zoomRange())
    val latitude = (visible.south + visible.north) / 2
    val longitude = (visible.west + visible.east) / 2
    val dLat = radiusKm / KM_PER_DEGREE
    val dLon = radiusKm / (KM_PER_DEGREE * cos(latitude * PI / HALF_TURN_DEGREES).coerceAtLeast(MIN_COS_LATITUDE))
    val box =
        BoundingBox(
            southwest =
            Position(
                longitude = (longitude - dLon).coerceAtLeast(-MAX_LONGITUDE),
                latitude = (latitude - dLat).coerceAtLeast(-MAX_LATITUDE),
            ),
            northeast =
            Position(
                longitude = (longitude + dLon).coerceAtMost(MAX_LONGITUDE),
                latitude = (latitude + dLat).coerceAtMost(MAX_LATITUDE),
            ),
        )
    val worldKm = EQUATOR_KM * cos(latitude * PI / HALF_TURN_DEGREES).coerceAtLeast(MIN_COS_LATITUDE)
    val fitting = floor(log2(TILES_ACROSS * worldKm / (2.0 * radiusKm))).toInt()
    val first = fitting.coerceIn(MIN_PACK_ZOOM, MAX_PACK_ZOOM)
    val last = (first + detail).coerceAtMost(MAX_PACK_ZOOM)
    return OfflineArea(box, first..last)
}

/** The choice of area (radius around the centre of the map) and of detail (zoom levels deeper), as chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfflineAreaChoice(radiusKm: Int, onRadius: (Int) -> Unit, detail: Int, onDetail: (Int) -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(Res.string.offline_area_title), style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(Res.string.offline_area_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OFFLINE_RADII_KM.forEach { km ->
                FilterChip(
                    selected = radiusKm == km,
                    onClick = { onRadius(km) },
                    label = {
                        Text(
                            if (km == 0) {
                                stringResource(Res.string.offline_area_visible)
                            } else {
                                stringResource(Res.string.offline_area_radius_km, km)
                            },
                        )
                    },
                )
            }
        }
        if (radiusKm > 0) {
            Text(text = stringResource(Res.string.offline_detail_title), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OFFLINE_DETAILS.forEach { level ->
                    FilterChip(selected = detail == level, onClick = { onDetail(level) }, label = { Text("+$level") })
                }
            }
        }
    }
}

private val OFFLINE_RADII_KM = listOf(0, 5, 20, 50, 100, 200, 500, 1000)
private val OFFLINE_DETAILS = listOf(1, 2, 3, 4)
private const val DEFAULT_DETAIL = 2
private const val TILES_ACROSS = 6.0
private const val EQUATOR_KM = 40075.0
private const val KM_PER_DEGREE = 111.32
private const val HALF_TURN_DEGREES = 180.0
private const val MIN_COS_LATITUDE = 0.05
private const val MAX_LATITUDE = 85.0
private const val MAX_LONGITUDE = 180.0

/** About what one raster tile weighs, for the size estimate. */
private const val ESTIMATED_TILE_BYTES = 25_000L
private const val BIG_DOWNLOAD_BYTES = 500_000_000L

/** The most tiles one pack may hold; the map engine stops a pack that asks for more. */
private const val MAX_PACK_TILES = 6000L

private fun OfflineMapTarget.zoomRange(): IntRange {
    val current = zoom().toInt().coerceIn(MIN_PACK_ZOOM, MAX_PACK_ZOOM)
    val max = (current + PACK_EXTRA_ZOOM_LEVELS).coerceAtMost(MAX_PACK_ZOOM)
    return current.coerceAtMost(max)..max
}

/** The separator between the parts of a pack's summary line. */
private const val SUMMARY_SEPARATOR = " \u00b7 "

/** Stands in for a value there is nothing to show for; a symbol, so it needs no translation. */
private const val EM_DASH = "\u2014"
