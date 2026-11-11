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
@file:Suppress("MagicNumber", "TooGenericExceptionCaught", "SwallowedException")

package org.meshtastic.feature.map.propagation

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.MapPrefs
import org.meshtastic.feature.map.layers.mapLayerFileSystem
import org.meshtastic.feature.map.layers.mapLayersDirectory

/** What the propagation layer shows and how it is going. */
data class PropagationState(
    val enabled: Boolean = false,
    val kind: PropagationKind = PropagationKind.GRADIENT,
    val autoRefresh: Boolean = true,
    /** How many hours ahead of now the picture is for (0 = now). */
    val hoursAhead: Int = 0,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val grid: TropoGrid? = null,
)

/**
 * The propagation layer of the map: the saved forecast, the settings and the downloads.
 *
 * The forecast covers the visible map for the next 24 hours and is kept on the device, so the layer still draws (for
 * the hours that are left) without the internet. While the layer is on and automatic refresh is allowed, it is
 * downloaded again once it is older than [REFRESH_AFTER_MS] or when the map has moved outside it.
 */
@Single
class PropagationController(
    httpClient: HttpClient,
    private val mapPrefs: MapPrefs,
    dispatchers: CoroutineDispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val source = TropoForecastSource(httpClient)
    private val fileSystem: FileSystem = mapLayerFileSystem()
    private val file: Path = (mapLayersDirectory().parent ?: mapLayersDirectory()) / "propagation" / "forecast.txt"
    private val mutable = MutableStateFlow(PropagationState())
    val state: StateFlow<PropagationState> = mutable.asStateFlow()
    private var lastTry = 0L

    /**
     * The widest area the forecast was asked for (already padded): the user zooms out, downloads, then zooms in again,
     * and a later refresh must keep fetching this whole area instead of shrinking to the screen. Only the "download
     * for this view" button sets it afresh, and it grows when the view leaves it.
     */
    private var area: GeoBounds? = null

    init {
        scope.launch {
            val prefs = mapPrefs.awaitPropagationPrefs()
            val saved = readSaved()
            mutable.update {
                it.copy(
                    enabled = prefs.enabled,
                    kind = PropagationKind.of(prefs.kind),
                    autoRefresh = prefs.autoRefresh,
                    grid = it.grid ?: saved,
                )
            }
            if (area == null) area = saved?.bounds
        }
    }

    fun setEnabled(on: Boolean) {
        mutable.update { it.copy(enabled = on) }
        mapPrefs.updatePropagationPrefs { it.copy(enabled = on) }
    }

    fun setKind(kind: PropagationKind) {
        mutable.update { it.copy(kind = kind) }
        mapPrefs.updatePropagationPrefs { it.copy(kind = kind.ordinal) }
    }

    fun setAutoRefresh(on: Boolean) {
        mutable.update { it.copy(autoRefresh = on) }
        mapPrefs.updatePropagationPrefs { it.copy(autoRefresh = on) }
    }

    fun setHoursAhead(hours: Int) = mutable.update {
        it.copy(hoursAhead = hours.coerceIn(0, TropoForecastSource.HOURS - 1))
    }

    /** Downloads the forecast for [bounds] now (the "download for this view" button). */
    fun download(bounds: GeoBounds) {
        if (mutable.value.loading) return
        area = padded(bounds)
        fetchArea()
    }

    private fun fetchArea() {
        val wanted = area ?: return
        lastTry = nowMillis
        mutable.update { it.copy(loading = true, failed = false) }
        scope.launch {
            val grid = source.fetch(wanted, nowMillis)
            if (grid != null) save(grid)
            mutable.update { it.copy(loading = false, failed = grid == null, grid = grid ?: it.grid, hoursAhead = 0) }
        }
    }

    /** Called as the map settles: downloads again only when the saved forecast is old or does not cover [bounds]. */
    fun ensureFor(bounds: GeoBounds) {
        val s = mutable.value
        if (!s.enabled || !s.autoRefresh || s.loading) return
        val grid = s.grid
        val stale = grid == null || nowMillis - grid.fetchedAtMillis > REFRESH_AFTER_MS
        val uncovered = grid != null && !grid.bounds.contains(bounds)
        val retryOk = nowMillis - lastTry > RETRY_AFTER_MS
        if ((stale || uncovered) && retryOk) {
            val kept = area
            // Refreshes the whole remembered area; only a view outside it widens it.
            area =
                when {
                    kept == null -> padded(bounds)
                    kept.contains(bounds) -> kept
                    else -> union(kept, padded(bounds))
                }
            fetchArea()
        }
    }


    /** A little wider than the screen, so a small pan does not leave the layer's edge showing. */
    private fun padded(b: GeoBounds): GeoBounds {
        val dLat = b.latSpan * PAD
        val dLon = b.lonSpan * PAD
        return GeoBounds(
            (b.south - dLat).coerceAtLeast(-MAX_LAT),
            b.west - dLon,
            (b.north + dLat).coerceAtMost(MAX_LAT),
            b.east + dLon,
        )
    }

    private fun readSaved(): TropoGrid? = try {
        if (fileSystem.exists(file)) TropoGrid.decode(fileSystem.read(file) { readUtf8() }) else null
    } catch (e: Exception) {
        null
    }

    private fun save(grid: TropoGrid) {
        try {
            file.parent?.let { fileSystem.createDirectories(it) }
            fileSystem.write(file) { writeUtf8(grid.encode()) }
        } catch (e: Exception) {
            // Not being able to keep it only costs a download next time.
        }
    }

    companion object {
        const val REFRESH_AFTER_MS = 3 * 3_600_000L
        const val RETRY_AFTER_MS = 2 * 60_000L
        private const val PAD = 0.15
        private const val MAX_LAT = 80.0
    }
}

/** The smallest area holding both [a] and [b]. */
private fun union(a: GeoBounds, b: GeoBounds) = GeoBounds(
    south = minOf(a.south, b.south),
    west = minOf(a.west, b.west),
    north = maxOf(a.north, b.north),
    east = maxOf(a.east, b.east),
)
