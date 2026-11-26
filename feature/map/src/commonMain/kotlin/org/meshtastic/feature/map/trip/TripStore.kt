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
@file:Suppress("TooGenericExceptionCaught")

package org.meshtastic.feature.map.trip

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Single
import org.meshtastic.feature.map.layers.mapLayerFileSystem
import org.meshtastic.feature.map.layers.mapLayersDirectory

/**
 * MT_SW: where the hikes are kept: one folder for each, with `trip.json` (route and picture list) and the pictures.
 * Everything stays on the device.
 */
@Single
class TripStore {
    private val fs: FileSystem = mapLayerFileSystem()
    private val root: Path = (mapLayersDirectory().parent ?: mapLayersDirectory()) / "trips"
    private val json = Json { ignoreUnknownKeys = true }

    private val mutableTrips = MutableStateFlow<List<Trip>>(emptyList())

    /** The saved hikes, newest first. */
    val trips: StateFlow<List<Trip>>
        get() = mutableTrips

    /** The ids of the hikes drawn on the map. */
    val visibleIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        refresh()
    }

    fun refresh() {
        val loaded =
            try {
                fs.listOrNull(root).orEmpty().mapNotNull { dir ->
                    val file = dir / TRIP_FILE
                    try {
                        if (fs.exists(file)) json.decodeFromString<Trip>(fs.read(file) { readUtf8() }) else null
                    } catch (e: Exception) {
                        Logger.w { "Wędrówki: nie udało się odczytać zapisu ${dir.name}: ${e.message}" }
                        null
                    }
                }
            } catch (e: Exception) {
                Logger.w { "Wędrówki: nie udało się odczytać listy: ${e.message}" }
                emptyList()
            }
        mutableTrips.value = loaded.sortedByDescending { it.startedMillis }
        Logger.i { "Wędrówki: zapisanych ${loaded.size}" }
    }

    /** Writes a picture into the folder of hike [tripId] and returns its file name, or null when it failed. */
    fun writePhoto(tripId: String, bytes: ByteArray, timeMillis: Long): String? =
        try {
            val dir = root / tripId
            fs.createDirectories(dir)
            val name = "p$timeMillis.jpg"
            fs.write(dir / name) { write(bytes) }
            Logger.i { "Wędrówki: zdjęcie zapisane, ${bytes.size} bajtów" }
            name
        } catch (e: Exception) {
            Logger.w { "Wędrówki: zdjęcia nie udało się zapisać: ${e.message}" }
            null
        }

    fun photoBytes(tripId: String, file: String): ByteArray? =
        try {
            fs.read(root / tripId / file) { readByteArray() }
        } catch (e: Exception) {
            Logger.w { "Wędrówki: zdjęcia nie udało się odczytać: ${e.message}" }
            null
        }

    fun save(trip: Trip) {
        try {
            val dir = root / trip.id
            fs.createDirectories(dir)
            fs.write(dir / TRIP_FILE) { writeUtf8(json.encodeToString(Trip.serializer(), trip)) }
            Logger.i { "Wędrówki: zapisana, punktów ${trip.points.size}, zdjęć ${trip.photos.size}" }
        } catch (e: Exception) {
            Logger.w { "Wędrówki: zapis nie udał się: ${e.message}" }
        }
        refresh()
    }

    fun delete(tripId: String) {
        try {
            fs.deleteRecursively(root / tripId)
            Logger.i { "Wędrówki: usunięta" }
        } catch (e: Exception) {
            Logger.w { "Wędrówki: usunięcie nie udało się: ${e.message}" }
        }
        visibleIds.update { it - tripId }
        refresh()
    }

    fun setVisible(tripId: String, visible: Boolean) {
        visibleIds.update { if (visible) it + tripId else it - tripId }
        Logger.i { "Wędrówki: ${if (visible) "pokazana" else "ukryta"} na mapie" }
    }

    private companion object {
        const val TRIP_FILE = "trip.json"
    }
}
