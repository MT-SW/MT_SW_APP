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
package org.meshtastic.feature.map.playback

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.SignalSurveyRepository
import org.meshtastic.feature.map.trip.TripStore
import kotlin.math.abs

/**
 * MT_SW: the virtual walk. A recorded signal survey is replayed on the map: a dot moves along the route in the order
 * the readings were taken, and the panel shows which node was heard there and how strongly.
 */
@Single
class RoutePlayback(
    private val repository: SignalSurveyRepository,
    private val nodeRepository: NodeRepository,
    private val tripStore: TripStore,
    dispatchers: CoroutineDispatchers,
) {
    /** One reading of the replayed route. [name] is the short name of the node that was heard, when it is known. */
    data class Step(
        val latitude: Double,
        val longitude: Double,
        val timeMillis: Long,
        val name: String,
        val rssi: Int,
        val snr: Float?,
        val direct: Boolean,
        /** The pictures of the place (a hike's stop) shown as slides when the walk gets here. */
        val slides: List<Slide> = emptyList(),
    )

    /** One slide of a hike's stop. */
    data class Slide(val file: String, val caption: String)

    data class State(
        val active: Boolean = false,
        val steps: List<Step> = emptyList(),
        val index: Int = 0,
        val playing: Boolean = false,
        val speed: Int = 1,
        /** The hike being replayed, or null when a signal survey is. */
        val tripId: String? = null,
        /** Which slide of the current place is shown. */
        val slide: Int = 0,
    ) {
        val current: Step?
            get() = steps.getOrNull(index)
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State>
        get() = mutableState

    private var job: Job? = null

    /** Loads the survey [sessionId] and starts replaying it from its first reading. */
    fun play(sessionId: Long) {
        job?.cancel()
        job =
            scope.launch {
                val names = nodeRepository.nodeDBbyNum.value
                val steps =
                    repository
                        .samplesOnce(sessionId)
                        .sortedBy { it.timeMillis }
                        .map { s ->
                            val name = names[s.fromNum]?.user?.short_name?.takeIf { it.isNotBlank() }
                            Step(
                                s.latitude,
                                s.longitude,
                                s.timeMillis,
                                name ?: "!${s.fromNum.toUInt().toString(HEX_RADIX)}",
                                s.rssi,
                                s.snr,
                                s.direct,
                            )
                        }
                Logger.i { "Spacer: sesja $sessionId wczytana, odczytów ${steps.size}" }
                if (steps.size < 2) {
                    Logger.i { "Spacer: za mało odczytów, żeby odtworzyć trasę" }
                    return@launch
                }
                val speed = mutableState.value.speed
                mutableState.value = State(active = true, steps = steps, playing = true, speed = speed)
                run()
            }
    }

    /** Replays the hike [tripId]: the dot walks the route and stops at each place to show its pictures as slides. */
    fun playTrip(tripId: String) {
        job?.cancel()
        job =
            scope.launch {
                val trip = tripStore.find(tripId) ?: return@launch
                if (trip.points.size < 2) {
                    Logger.i { "Spacer: wędrówka ma za mało punktów" }
                    return@launch
                }
                val slidesAt = HashMap<Int, MutableList<Slide>>()
                val titleAt = HashMap<Int, String>()
                trip.stops
                    .filter { it.photos.isNotEmpty() }
                    .forEach { stop ->
                        val at = trip.points.indices.minBy { abs(trip.points[it].t - stop.t) }
                        slidesAt.getOrPut(at) { mutableListOf() }.addAll(stop.photos.map { Slide(it.file, it.caption) })
                        if (stop.title.isNotBlank()) titleAt[at] = stop.title
                    }
                val steps =
                    trip.points.mapIndexed { i, p ->
                        Step(p.lat, p.lon, p.t, titleAt[i] ?: trip.name, 0, null, true, slidesAt[i].orEmpty())
                    }
                Logger.i { "Spacer: wędrówka wczytana, punktów ${steps.size}, miejsc ze zdjęciami ${slidesAt.size}" }
                val speed = mutableState.value.speed
                mutableState.value = State(active = true, steps = steps, playing = true, speed = speed, tripId = tripId)
                run()
            }
    }

    private suspend fun run() {
        while (true) {
            val s = mutableState.value
            if (!s.active || !s.playing) return
            if (s.index >= s.steps.lastIndex) {
                mutableState.update { it.copy(playing = false) }
                Logger.i { "Spacer: koniec trasy" }
                return
            }
            delay(STEP_MILLIS / s.speed)
            mutableState.update {
                if (it.playing) {
                    it.copy(index = (it.index + 1).coerceAtMost(it.steps.lastIndex), slide = 0)
                } else {
                    it
                }
            }
            val slides = mutableState.value.current?.slides.orEmpty()
            for (i in slides.indices) {
                mutableState.update { it.copy(slide = i) }
                delay(SLIDE_MILLIS / mutableState.value.speed)
                if (!mutableState.value.playing || !mutableState.value.active) return
            }
        }
    }

    /** Pauses, or resumes (from the start when the end was reached). */
    fun togglePause() {
        val s = mutableState.value
        if (!s.active) return
        val resume = !s.playing
        mutableState.update {
            it.copy(
                playing = resume,
                index = if (resume && it.index >= it.steps.lastIndex) 0 else it.index,
                slide = 0,
            )
        }
        Logger.i { "Spacer: ${if (resume) "wznowiony" else "wstrzymany"} na odczycie ${mutableState.value.index}" }
        if (resume) {
            job?.cancel()
            job = scope.launch { run() }
        }
    }

    fun setSpeed(speed: Int) {
        mutableState.update { it.copy(speed = speed) }
        Logger.i { "Spacer: prędkość ${speed}x" }
    }

    /** Jumps to the reading [index] (the slider). */
    fun seek(index: Int) {
        mutableState.update { it.copy(index = index.coerceIn(0, (it.steps.size - 1).coerceAtLeast(0)), slide = 0) }
    }

    fun stop() {
        job?.cancel()
        mutableState.value = State(speed = mutableState.value.speed)
        Logger.i { "Spacer: zakończony" }
    }

    companion object {
        val SPEEDS = listOf(1, 2, 4, 8, 16)
        private const val STEP_MILLIS = 600L
        private const val SLIDE_MILLIS = 2500L
        private const val HEX_RADIX = 16
    }
}
