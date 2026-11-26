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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.ui.util.HealthSync
import org.meshtastic.core.ui.util.rememberHealthSync
import org.meshtastic.feature.map.track.TrackRecorder
import org.meshtastic.feature.map.trip.TripStore
import org.meshtastic.feature.map.trip.toTripHealth

private const val LIVE_EVERY_MS = 2 * 60_000L
private const val SAVE_WAIT_MS = 500L
private const val SAVE_TRIES = 10
private const val RECENT_MS = 30 * 60_000L

/**
 * MT_SW: while a hike is recorded the steps, distance, heart rate and energy are taken from the health app by
 * themselves (when the option is on, which it is by default): the permission is asked for when the recording starts,
 * the numbers are refreshed while it runs and saved with the hike when it ends. A hike that ended while this was not on
 * screen is filled in the next time the map is shown. Errors only go to the log.
 */
@Composable
internal fun HikeHealthAuto() {
    val recorder: TrackRecorder = koinInject()
    val store: TripStore = koinInject()
    val prefs: UiPrefs = koinInject()
    val options by prefs.uiOptions.collectAsStateWithLifecycle()
    val auto = UiOptions(options)[UiOptions.HIKE_HEALTH_AUTO] == 1
    val health = rememberHealthSync()
    val state by recorder.state.collectAsStateWithLifecycle()
    var running by remember { mutableStateOf<Triple<String, Long, Boolean>?>(null) }

    LaunchedEffect(state.recording, auto, health) {
        if (health == null || !auto) return@LaunchedEffect
        if (state.recording) {
            val id = state.tripId
            val from = state.startedMillis
            running = Triple(id, from, true)
            Logger.i { "Zdrowie (auto): nagrywanie ruszyło, pobieranie danych z aplikacji zdrowia" }
            while (true) {
                val data = health.read(from, nowMillis)
                if (data == null) {
                    Logger.i { "Zdrowie (auto): brak zgody lub odczytu, przerywam na tę wędrówkę" }
                    break
                }
                recorder.setLiveHealth(data)
                delay(LIVE_EVERY_MS)
            }
        } else {
            running?.let { (id, from, _) ->
                running = null
                finish(health, store, recorder, id, from, nowMillis)
            }
        }
    }

    // A hike that ended while the map was not on screen.
    LaunchedEffect(health, auto) {
        if (health == null || !auto || state.recording) return@LaunchedEffect
        val latest = store.trips.value.maxByOrNull { it.endedMillis } ?: return@LaunchedEffect
        if (latest.health == null && nowMillis - latest.endedMillis < RECENT_MS) {
            Logger.i { "Zdrowie (auto): uzupełniam dane niedawnej wędrówki" }
            finish(health, store, recorder, latest.id, latest.startedMillis, latest.endedMillis)
        }
    }
}

private suspend fun finish(
    health: HealthSync,
    store: TripStore,
    recorder: TrackRecorder,
    tripId: String,
    from: Long,
    to: Long,
) {
    val data = health.read(from, to)
    recorder.setLiveHealth(null)
    if (data == null || data.isEmpty) {
        Logger.i { "Zdrowie (auto): na koniec brak danych do zapisania" }
        return
    }
    // The hike is saved a moment after the recording stops.
    repeat(SAVE_TRIES) {
        if (store.find(tripId) != null) {
            store.setHealth(tripId, data.toTripHealth(nowMillis))
            Logger.i { "Zdrowie (auto): dane zapisane przy wędrówce" }
            return
        }
        delay(SAVE_WAIT_MS)
    }
    Logger.w { "Zdrowie (auto): wędrówka nie została zapisana, dane pominięte" }
}
