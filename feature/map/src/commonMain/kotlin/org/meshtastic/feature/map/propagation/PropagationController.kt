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
@file:Suppress("MagicNumber")

package org.meshtastic.feature.map.propagation

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.MapPrefs

/** What the propagation layer shows and how it is going. */
data class PropagationState(
    val enabled: Boolean = false,
    val kind: PropagationKind = PropagationKind.GRADIENT,
    val autoRefresh: Boolean = true,
    /** How many hours ahead of now the picture is for (0 = now). */
    val hoursAhead: Int = 0,
    val loading: Boolean = false,
    val failed: Boolean = false,
    /** The downloaded areas, oldest first. They may overlap. */
    val regions: List<PropagationRegion> = emptyList(),
) {
    /** Any one of the forecasts (they all cover the same hours), or null while nothing is downloaded. */
    val grid: TropoGrid?
        get() = regions.firstOrNull()?.grid

    /** The latest hour ahead that every region still has a forecast for. */
    val hoursLeft: Int
        get() = regions.minOfOrNull { it.grid.hours - 1 - it.grid.hourAt(nowMillis) } ?: 0
}

/**
 * The propagation layer of the map: the saved regions, the settings and the downloads.
 *
 * Each region is an area (smaller, larger, finer or coarser) with its forecast for the next 24 hours, kept on the
 * device, so the layer still draws (for the hours that are left) without the internet. Regions are added and removed
 * one by one by the user and all downloaded again together: by hand, or, while the layer is on and automatic refresh
 * is allowed, once the oldest is older than [REFRESH_AFTER_MS].
 */
@Single
class PropagationController(
    httpClient: HttpClient,
    private val mapPrefs: MapPrefs,
    dispatchers: CoroutineDispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val source = TropoForecastSource(httpClient)
    private val store = PropagationStore()
    private val mutable = MutableStateFlow(PropagationState())
    val state: StateFlow<PropagationState> = mutable.asStateFlow()
    private var lastTry = 0L

    init {
        scope.launch {
            val prefs = mapPrefs.awaitPropagationPrefs()
            val saved = store.readAll()
            mutable.update {
                it.copy(
                    enabled = prefs.enabled,
                    kind = PropagationKind.of(prefs.kind),
                    autoRefresh = prefs.autoRefresh,
                    regions = (saved + it.regions).distinctBy { region -> region.id }.sortedBy { region -> region.id },
                )
            }
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

    /** Adds a region called [name] over [bounds] (a little wider than the screen), with [detail], and downloads it. */
    fun addRegion(bounds: GeoBounds, detail: PropagationDetail, name: String = "") {
        val s = mutable.value
        if (s.loading || s.regions.size >= MAX_REGIONS) return
        lastTry = nowMillis
        val id = maxOf(nowMillis, (s.regions.maxOfOrNull { it.id } ?: 0L) + 1)
        mutable.update { it.copy(loading = true, failed = false) }
        scope.launch {
            val grid = source.fetch(padded(bounds), nowMillis, detail.points, detail.points)
            val region = grid?.let { PropagationRegion(id, it, name.replace('\n', ' ').trim().take(MAX_NAME)) }
            if (region != null) store.save(region)
            mutable.update {
                it.copy(
                    loading = false,
                    failed = region == null,
                    regions = if (region == null) it.regions else it.regions + region,
                    hoursAhead = 0,
                )
            }
        }
    }

    /** Removes one region, from the map and from the device. The others stay as they are. */
    fun removeRegion(id: Long) {
        mutable.update { it.copy(regions = it.regions.filterNot { region -> region.id == id }) }
        store.delete(id)
    }

    /** Downloads every region again, each over the same area and with the same grid as before. */
    fun refreshAll() {
        val s = mutable.value
        if (s.loading || s.regions.isEmpty()) return
        lastTry = nowMillis
        mutable.update { it.copy(loading = true, failed = false) }
        scope.launch {
            var failed = false
            for (region in s.regions) {
                val grid = source.fetch(region.grid.bounds, nowMillis, region.grid.rows, region.grid.cols)
                if (grid == null) {
                    failed = true
                    continue
                }
                // A region removed while this ran must not come back.
                if (mutable.value.regions.none { it.id == region.id }) continue
                val fresh = region.copy(grid = grid)
                store.save(fresh)
                mutable.update { cur -> cur.copy(regions = cur.regions.map { if (it.id == region.id) fresh else it }) }
            }
            mutable.update { it.copy(loading = false, failed = failed, hoursAhead = 0) }
        }
    }

    /** Called as the map settles: downloads all regions again only when the oldest forecast has got old. */
    fun ensureFor() {
        val s = mutable.value
        if (!s.enabled || !s.autoRefresh || s.loading || nowMillis - lastTry <= RETRY_AFTER_MS) return
        if (s.regions.any { nowMillis - it.grid.fetchedAtMillis > REFRESH_AFTER_MS }) refreshAll()
    }

    companion object {
        const val REFRESH_AFTER_MS = 3 * 3_600_000L
        const val RETRY_AFTER_MS = 2 * 60_000L
        const val MAX_REGIONS = 8
        const val MAX_NAME = 40
    }
}

private const val PAD = 0.15
private const val MAX_LAT = 80.0

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
