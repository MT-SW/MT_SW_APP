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
package org.meshtastic.feature.map.track

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.FileService
import org.meshtastic.core.repository.LocationRepository
import org.meshtastic.core.repository.NodeRepository
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Instant

/**
 * MT_SW: records the person's own route. The points come from the phone's position and, where there is none (the
 * computer), from the position of the connected radio; a point is added only when it is [MIN_STEP_METERS] away from the
 * last one, so standing still does not fill the track. The finished route is saved as a GPX file, the same format as
 * the exported node tracks. It is kept while the app runs.
 */
@Single
class TrackRecorder(
    private val locationRepository: LocationRepository,
    private val nodeRepository: NodeRepository,
    private val fileService: FileService,
    private val dispatchers: CoroutineDispatchers,
) {
    data class Point(val latitude: Double, val longitude: Double, val altitudeMeters: Double?, val timeMillis: Long)

    data class State(
        val recording: Boolean = false,
        val points: List<Point> = emptyList(),
        val distanceMeters: Double = 0.0,
        val startedMillis: Long = 0L,
    )

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State>
        get() = mutableState

    private var phoneJob: Job? = null
    private var radioJob: Job? = null

    /** Starts a new route; a route already recorded is dropped. */
    fun start() {
        if (mutableState.value.recording) return
        mutableState.value = State(recording = true, startedMillis = nowMillis)
        Logger.i { "Trasa: nagrywanie rozpoczęte" }
        phoneJob =
            scope.launch {
                locationRepository
                    .getLocations()
                    .catch { Logger.w { "Trasa: pozycja telefonu niedostępna: ${it::class.simpleName}" } }
                    .collect { fix ->
                        val height = fix.mslAltitudeMeters ?: fix.altitudeMeters
                        add(Point(fix.latitude, fix.longitude, height, fix.timeMillis))
                    }
            }
        radioJob =
            scope.launch {
                nodeRepository.ourNodeInfo
                    .map { node -> node?.validPosition?.let { Point(node.latitude, node.longitude, null, nowMillis) } }
                    .distinctUntilChanged { old, new ->
                        old?.latitude == new?.latitude && old?.longitude == new?.longitude
                    }
                    .collect { point -> if (point != null) add(point) }
            }
    }

    /** Stops recording; the route stays until it is saved or a new one starts. */
    fun stop() {
        phoneJob?.cancel()
        radioJob?.cancel()
        phoneJob = null
        radioJob = null
        val s = mutableState.value
        mutableState.update { it.copy(recording = false) }
        Logger.i { "Trasa: nagrywanie zakończone, punktów ${s.points.size}, ${s.distanceMeters.toInt()} m" }
    }

    fun clear() {
        if (mutableState.value.recording) stop()
        mutableState.value = State()
        Logger.i { "Trasa: zapis usunięty" }
    }

    /** Writes the recorded route as a GPX file to [uri]. */
    suspend fun save(uri: CommonUri): Boolean {
        val s = mutableState.value
        val ok = fileService.write(uri) { sink -> sink.writeUtf8(buildTrackGpx(s.points)) }
        Logger.i { "Trasa: zapis do pliku GPX ${if (ok) "udany" else "nieudany"}, punktów ${s.points.size}" }
        return ok
    }

    private fun add(point: Point) {
        mutableState.update { old ->
            if (!old.recording) return@update old
            val last = old.points.lastOrNull()
            val step = last?.let { distanceMeters(it, point) } ?: 0.0
            if (last != null && step < MIN_STEP_METERS) {
                old
            } else {
                old.copy(points = old.points + point, distanceMeters = old.distanceMeters + step)
            }
        }
    }

    companion object {
        const val MIN_STEP_METERS = 5.0
        private const val EARTH_RADIUS_M = 6_371_000.0

        /** The distance along the ground between two points (haversine). */
        internal fun distanceMeters(a: Point, b: Point): Double {
            val dLat = (b.latitude - a.latitude).toRadians()
            val dLon = (b.longitude - a.longitude).toRadians()
            val h =
                sin(dLat / 2).pow(2) + cos(a.latitude.toRadians()) * cos(b.latitude.toRadians()) * sin(dLon / 2).pow(2)
            return 2 * EARTH_RADIUS_M * asin(sqrt(h))
        }

        private fun Double.toRadians(): Double = this * PI / 180.0

        private const val DECIMALS = 7

        /** Coordinates are written with a fixed dot, because another program reads the file. */
        internal fun buildTrackGpx(points: List<Point>): String {
            val trkpts = buildString {
                points.forEach { p ->
                    val lat = NumberFormatter.formatInvariant(p.latitude, DECIMALS)
                    val lon = NumberFormatter.formatInvariant(p.longitude, DECIMALS)
                    append("    <trkpt lat=\"$lat\" lon=\"$lon\">")
                    p.altitudeMeters?.let { append("<ele>${NumberFormatter.formatInvariant(it, 1)}</ele>") }
                    append("<time>${Instant.fromEpochMilliseconds(p.timeMillis)}</time></trkpt>\n")
                }
            }
            return """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="MT_SW" xmlns="http://www.topografix.com/GPX/1/1">
  <trk>
    <name>MT_SW</name>
    <trkseg>
$trkpts    </trkseg>
  </trk>
</gpx>"""
        }
    }
}
