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
package org.meshtastic.feature.map.coverage

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.formatString
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.model.fixOrNull
import org.meshtastic.core.model.util.GeoConstants
import org.meshtastic.core.model.util.isMeasuredDirect
import org.meshtastic.core.repository.MapPrefs
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.ui.util.toPosition
import org.meshtastic.proto.PortNum

/** One measurement: where a node was when the radio heard its position directly, and with what SNR (dB). */
data class CoveragePoint(val latitude: Double, val longitude: Double, val snr: Float, val time: Int, val nodeNum: Int)

/** A node that has measurements, for the node filter: its name and how many measurements it has. */
data class CoverageSource(val nodeNum: Int, val name: String, val count: Int)

/** What the coverage layer of the main map shows. */
data class CoverageState(
    val enabled: Boolean = false,
    val loading: Boolean = false,
    val points: List<CoveragePoint> = emptyList(),
    /** How many nodes the shown [points] come from. */
    val nodes: Int = 0,
    /** How many measurements there are before the filter; [points] is the part the filter lets through. */
    val total: Int = 0,
    /** The nodes that have measurements, most measurements first. */
    val sources: List<CoverageSource> = emptyList(),
    /** The filter on the age of a measurement, in days; 0 shows all of them. */
    val days: Int = 0,
    /** The filter on one node; null shows all nodes. */
    val nodeFilter: Int? = null,
    /** When the measurements were last read, in milliseconds; 0 when they were not read yet. */
    val readMillis: Long = 0L,
)

/**
 * The coverage layer of the main map: where, and how strongly, the radio heard the nodes directly.
 *
 * The measurements are read from the stored position packets of every node when the layer is switched on and on
 * request, not continuously, since that is one read per node. A position counts only when it reached the radio
 * directly, without a relay: a relayed one carries the SNR of the last hop, which says nothing about the distance to
 * the node. The newest [MAX_POINTS] are kept, so the map stays light.
 */
@Single
class CoverageController(
    private val nodeRepository: NodeRepository,
    private val meshLogRepository: MeshLogRepository,
    private val mapPrefs: MapPrefs,
    private val dispatchers: CoroutineDispatchers,
) {
    private val mutable = MutableStateFlow(CoverageState())
    val state: StateFlow<CoverageState> = mutable.asStateFlow()
    private var restored = false
    private var all: List<CoveragePoint> = emptyList()

    /** Switches the layer on or off and remembers it, so it is still on after the app is opened again. */
    fun setEnabled(on: Boolean) {
        restored = true
        mutable.update { it.copy(enabled = on) }
        mapPrefs.setCoverageEnabled(on)
    }

    /** Takes over the saved on/off setting, once, when the map is first shown. */
    suspend fun restore() {
        if (restored) return
        restored = true
        val saved = mapPrefs.awaitCoverageEnabled()
        val days = mapPrefs.awaitCoverageDays()
        val node = mapPrefs.awaitCoverageNode()
        mutable.update { it.copy(enabled = saved, days = days, nodeFilter = node) }
    }

    /** Reads the measurements again. A read that is already going on is not started a second time. */
    suspend fun refresh() {
        if (mutable.value.loading) return
        mutable.update { it.copy(loading = true) }
        try {
            val measured = withContext(dispatchers.default) { measure() }
            all = measured.first
            mutable.update { state ->
                // A saved node that has no measurements any more must not leave the map empty.
                val node = state.nodeFilter?.takeIf { saved -> measured.second.any { it.nodeNum == saved } }
                state.copy(sources = measured.second, readMillis = nowMillis, nodeFilter = node)
            }
            applyFilter()
            Logger.i { "Coverage: ${mutable.value.points.size} of ${all.size} points are shown on the map" }
        } finally {
            mutable.update { it.copy(loading = false) }
        }
    }

    /** Shows only the measurements of the last [days] days (0: all of them). */
    fun setDays(days: Int) {
        mutable.update { it.copy(days = days) }
        applyFilter()
        mapPrefs.setCoverageFilter(days, mutable.value.nodeFilter)
    }

    /** Shows only the measurements of [nodeNum] (null: all nodes). */
    fun setNode(nodeNum: Int?) {
        mutable.update { it.copy(nodeFilter = nodeNum) }
        applyFilter()
        mapPrefs.setCoverageFilter(mutable.value.days, nodeNum)
    }

    private fun applyFilter() {
        mutable.update { state ->
            val since = if (state.days > 0) nowMillis / MILLIS_PER_SECOND - state.days * SECONDS_PER_DAY else 0L
            val shown = all.filter { (state.nodeFilter == null || it.nodeNum == state.nodeFilter) && it.time >= since }
            state.copy(points = shown, nodes = shown.map { it.nodeNum }.distinct().size, total = all.size)
        }
    }

    private suspend fun measure(): Pair<List<CoveragePoint>, List<CoverageSource>> {
        val points = mutableListOf<CoveragePoint>()
        val names = mutableMapOf<Int, String>()
        var seen = 0
        var direct = 0
        for (node in nodeRepository.nodeDBbyNum.value.values) {
            if (node.lastHeard == 0) continue
            val packets = meshLogRepository.getMeshPacketsFrom(node.num, PortNum.POSITION_APP.value).first()
            for (packet in packets) {
                seen++
                if (!packet.isMeasuredDirect()) continue
                direct++
                val snr = packet.rx_snr.takeUnless { it.isNaN() } ?: continue
                val position = packet.toPosition() ?: continue
                val (latI, lonI) = position.fixOrNull() ?: continue
                points +=
                    CoveragePoint(
                        latitude = latI * GeoConstants.DEG_D,
                        longitude = lonI * GeoConstants.DEG_D,
                        snr = snr,
                        time = position.time.takeIf { it != 0 } ?: packet.rx_time ?: 0,
                        nodeNum = node.num,
                    )
                names[node.num] = node.effectiveLongName
            }
        }
        Logger.i { "Coverage: $seen position packets read, $direct heard directly, ${points.size} with a fix and SNR" }
        val kept = points.sortedByDescending { it.time }.take(MAX_POINTS)
        val sources =
            kept
                .groupingBy { it.nodeNum }
                .eachCount()
                .map { (num, count) ->
                    CoverageSource(num, names[num].orEmpty().ifBlank { formatString("!%08x", num) }, count)
                }
                .sortedByDescending { it.count }
        return kept to sources
    }

    private companion object {
        const val MAX_POINTS = 20_000
        const val MILLIS_PER_SECOND = 1_000L
        const val SECONDS_PER_DAY = 86_400L
    }
}
