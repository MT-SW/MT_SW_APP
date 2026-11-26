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
@file:Suppress("TooGenericExceptionCaught", "TooManyFunctions")

package org.meshtastic.feature.map.trip

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Single
import org.meshtastic.core.ui.util.ExifInfo
import org.meshtastic.core.ui.util.saveToHikeAlbum
import org.meshtastic.core.ui.util.scaleJpeg
import org.meshtastic.feature.map.layers.mapLayerFileSystem
import org.meshtastic.feature.map.layers.mapLayersDirectory
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.uuid.Uuid

/**
 * MT_SW: where the hikes are kept: one folder for each, with `trip.json` (route and stops) and the pictures (a picture
 * of at most 1600 px and a small copy of 256 px). Everything stays on the device.
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

    private val mutableReceivedFrom = MutableStateFlow<Map<Int, String>>(emptyMap())

    /** The newest hike received from each node (node number → hike id). */
    val receivedFrom: StateFlow<Map<Int, String>>
        get() = mutableReceivedFrom

    /** The ids of the hikes drawn on the map. */
    val visibleIds = MutableStateFlow<Set<String>>(emptySet())

    private val folderFile: Path = root / "album_folder.txt"

    /** The folder the pictures are copied to on the computer (null: the default one, or the system album). */
    val albumFolder = MutableStateFlow<String?>(null)

    init {
        albumFolder.value =
            try {
                if (fs.exists(folderFile)) fs.read(folderFile) { readUtf8() }.trim().ifBlank { null } else null
            } catch (e: Exception) {
                Logger.w { "Wędrówki: folder zdjęć nie został odczytany: ${e.message}" }
                null
            }
        refresh()
    }

    fun setAlbumFolder(path: String) {
        albumFolder.value = path
        try {
            fs.createDirectories(root)
            fs.write(folderFile) { writeUtf8(path) }
            Logger.i { "Wędrówki: zmieniono folder zdjęć" }
        } catch (e: Exception) {
            Logger.w { "Wędrówki: folder zdjęć nie został zapisany: ${e.message}" }
        }
    }

    /**
     * Keeps a whole copy of a picture taken for a hike in the separate album (the folder on the computer), so deleting
     * a hike in the app never deletes the picture.
     */
    fun archive(bytes: ByteArray) {
        val name = "mtsw_${org.meshtastic.core.common.util.nowMillis}.jpg"
        val saved = saveToHikeAlbum(bytes, name, albumFolder.value)
        Logger.i { "Wędrówki: kopia zdjęcia w albumie: ${if (saved) "zapisana" else "niezapisana"}" }
    }

    fun refresh() {
        val loaded =
            try {
                fs.listOrNull(root).orEmpty().mapNotNull { dir ->
                    val file = dir / TRIP_FILE
                    try {
                        if (fs.exists(file)) {
                            migrate(json.decodeFromString<Trip>(fs.read(file) { readUtf8() }))
                        } else {
                            null
                        }
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
        mutableReceivedFrom.value =
            mutableTrips.value
                .filter { it.sharedBy != 0 }
                .distinctBy { it.sharedBy }
                .associate { it.sharedBy to it.id }
        Logger.i { "Wędrówki: zapisanych ${loaded.size}" }
    }

    /** An old hike kept one picture per place; each of them becomes a stop of its own. */
    private fun migrate(trip: Trip): Trip {
        if (trip.photos.isEmpty()) return trip
        val stops =
            trip.photos.map { p ->
                TripStop(
                    id = newId(),
                    lat = p.lat,
                    lon = p.lon,
                    t = p.t,
                    photos = listOf(TripPhoto(file = p.file, takenMillis = p.t, caption = p.caption, url = p.url)),
                )
            }
        Logger.i { "Wędrówki: stary zapis przeniesiony na punkty, ${stops.size}" }
        return trip.copy(stops = trip.stops + stops, photos = emptyList()).also { persist(it) }
    }

    fun find(tripId: String): Trip? = mutableTrips.value.firstOrNull { it.id == tripId }

    fun newId(): String = Uuid.random().toString()

    /**
     * Writes a picture into the folder of hike [tripId] (shrunk, with a small copy) and returns it; null on failure.
     */
    fun writePhoto(tripId: String, bytes: ByteArray, takenMillis: Long): TripPhoto? =
        try {
            val dir = root / tripId
            fs.createDirectories(dir)
            val name = "p${newId().take(ID_CHARS)}.jpg"
            val main = scaleJpeg(bytes, MAIN_SIDE, MAIN_QUALITY) ?: bytes
            fs.write(dir / name) { write(main) }
            scaleJpeg(bytes, THUMB_SIDE, THUMB_QUALITY)?.let { small -> fs.write(dir / "th_$name") { write(small) } }
            Logger.i { "Wędrówki: zdjęcie zapisane, ${main.size} bajtów" }
            TripPhoto(file = name, takenMillis = takenMillis)
        } catch (e: Exception) {
            Logger.w { "Wędrówki: zdjęcia nie udało się zapisać: ${e.message}" }
            null
        }

    /** The picture, or its small copy when [small] (which falls back to the picture when there is none). */
    fun photoBytes(tripId: String, file: String, small: Boolean = false): ByteArray? =
        try {
            val thumb = root / tripId / "th_$file"
            val path = if (small && fs.exists(thumb)) thumb else root / tripId / file
            fs.read(path) { readByteArray() }
        } catch (e: Exception) {
            Logger.w { "Wędrówki: zdjęcia nie udało się odczytać: ${e.message}" }
            null
        }

    private fun persist(trip: Trip) {
        try {
            val dir = root / trip.id
            fs.createDirectories(dir)
            fs.write(dir / TRIP_FILE) { writeUtf8(json.encodeToString(Trip.serializer(), trip)) }
            Logger.i {
                "Wędrówki: zapisana, punktów ${trip.points.size}, miejsc ${trip.stops.size}, zdjęć ${trip.photoCount}"
            }
        } catch (e: Exception) {
            Logger.w { "Wędrówki: zapis nie udał się: ${e.message}" }
        }
    }

    fun save(trip: Trip) {
        persist(trip)
        refresh()
    }

    private fun mutate(tripId: String, change: (Trip) -> Trip) {
        val trip = find(tripId) ?: return
        persist(change(trip))
        refresh()
    }

    private fun mutateStop(tripId: String, stopId: String, change: (TripStop) -> TripStop) =
        mutate(tripId) { t -> t.copy(stops = t.stops.map { if (it.id == stopId) change(it) else it }) }

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

    fun setHealth(tripId: String, health: TripHealth) {
        mutate(tripId) { it.copy(health = health) }
        Logger.i { "Wędrówki: zapisano dane z aplikacji zdrowia" }
    }

    fun rename(tripId: String, name: String) = mutate(tripId) { it.copy(name = name) }

    /** A new stop at the route point [pointIndex]; returns its id. */
    fun addStop(tripId: String, pointIndex: Int): String? {
        val trip = find(tripId) ?: return null
        val p = trip.points.getOrNull(pointIndex) ?: return null
        val stop = TripStop(id = newId(), lat = p.lat, lon = p.lon, t = p.t)
        persist(trip.copy(stops = trip.stops + stop))
        refresh()
        Logger.i { "Wędrówki: nowy punkt na trasie, miejsce $pointIndex" }
        return stop.id
    }

    /** Puts pictures on the stack of [stopId]; [taken] is the time of each picture when known. */
    fun addPhotos(tripId: String, stopId: String, pictures: List<Pair<ByteArray, Long>>) {
        val written = pictures.mapNotNull { (bytes, taken) -> writePhoto(tripId, bytes, taken) }
        if (written.isEmpty()) return
        mutateStop(tripId, stopId) { it.copy(photos = it.photos + written) }
        Logger.i { "Wędrówki: do punktu dodano zdjęć ${written.size}" }
    }

    /**
     * Puts each picture on the stop it belongs to, by its time or position ([exif]); a picture that fits nowhere
     * goes to a stop at the start of the route. Returns how many could not be placed by what they say about
     * themselves.
     */
    fun addPhotosAuto(tripId: String, pictures: List<Pair<ByteArray, ExifInfo>>, unplacedTitle: String): Int {
        var trip = find(tripId) ?: return 0
        var unplaced = 0
        pictures.forEach { (bytes, exif) ->
            val index = pointFor(trip, exif)
            if (index == null) unplaced += 1
            val p = trip.points[index ?: 0]
            val taken = exif.takenMillis ?: 0L
            val photo = writePhoto(tripId, bytes, taken) ?: return@forEach
            val near =
                trip.stops.firstOrNull { (index != null || it.title == unplacedTitle) && closeTo(it, p) }
            trip =
                if (near != null) {
                    val joined = trip.stops.map { if (it.id == near.id) it.copy(photos = it.photos + photo) else it }
                    trip.copy(stops = joined)
                } else {
                    val title = if (index == null) unplacedTitle else ""
                    trip.copy(stops = trip.stops + TripStop(newId(), p.lat, p.lon, p.t, title, listOf(photo)))
                }
        }
        persist(trip)
        refresh()
        Logger.i { "Wędrówki: automatyczne przypisanie, zdjęć ${pictures.size}, bez dopasowania $unplaced" }
        return unplaced
    }

    private fun closeTo(stop: TripStop, p: TripPoint) = meters(stop.lat, stop.lon, p.lat, p.lon) < STOP_JOIN_METERS

    /** The route point a picture belongs to by its time (inside the hike) or else by its GPS position, or null. */
    private fun pointFor(trip: Trip, exif: ExifInfo): Int? {
        val taken = exif.takenMillis
        if (taken != null && taken in (trip.startedMillis - TIME_SLACK_MS)..(trip.endedMillis + TIME_SLACK_MS)) {
            return trip.points.indices.minBy { kotlin.math.abs(trip.points[it].t - taken) }
        }
        val lat = exif.latitude
        val lon = exif.longitude
        if (lat != null && lon != null) {
            val best = trip.points.indices.minBy { meters(lat, lon, trip.points[it].lat, trip.points[it].lon) }
            if (meters(lat, lon, trip.points[best].lat, trip.points[best].lon) < GPS_MATCH_METERS) return best
        }
        return null
    }

    /** Moves the stop to the route point [pointIndex]; its time follows the point. */
    fun placeStop(tripId: String, stopId: String, pointIndex: Int) {
        val p = find(tripId)?.points?.getOrNull(pointIndex) ?: return
        mutateStop(tripId, stopId) { it.copy(lat = p.lat, lon = p.lon, t = p.t) }
        Logger.i { "Wędrówki: punkt przesunięty na miejsce $pointIndex trasy" }
    }

    /** Moves a picture from one stop to another (the end of the other stack). */
    fun movePhoto(tripId: String, fromStopId: String, file: String, toStopId: String) {
        mutate(tripId) { t ->
            val photo = t.stops.firstOrNull { it.id == fromStopId }?.photos?.firstOrNull { it.file == file }
            if (photo == null || fromStopId == toStopId) {
                t
            } else {
                t.copy(
                    stops =
                    t.stops.map {
                        when (it.id) {
                            fromStopId -> it.copy(photos = it.photos - photo, shareIndex = 0)
                            toStopId -> it.copy(photos = it.photos + photo)
                            else -> it
                        }
                    },
                )
            }
        }
        Logger.i { "Wędrówki: zdjęcie przeniesione do innego punktu" }
    }

    fun setStopTitle(tripId: String, stopId: String, title: String) =
        mutateStop(tripId, stopId) { it.copy(title = title) }

    fun setShareIndex(tripId: String, stopId: String, index: Int) =
        mutateStop(tripId, stopId) { it.copy(shareIndex = index.coerceIn(0, (it.photos.size - 1).coerceAtLeast(0))) }

    fun setCaption(tripId: String, stopId: String, file: String, caption: String) =
        mutateStop(tripId, stopId) { s ->
            s.copy(photos = s.photos.map { if (it.file == file) it.copy(caption = caption) else it })
        }

    fun removePhoto(tripId: String, stopId: String, file: String) {
        runCatching {
            fs.delete(root / tripId / file)
            fs.delete(root / tripId / "th_$file")
        }
        mutateStop(tripId, stopId) { s ->
            val left = s.photos.filter { it.file != file }
            s.copy(photos = left, shareIndex = s.shareIndex.coerceIn(0, (left.size - 1).coerceAtLeast(0)))
        }
        Logger.i { "Wędrówki: zdjęcie usunięte z punktu" }
    }

    fun removeStop(tripId: String, stopId: String) {
        val stop = find(tripId)?.stops?.firstOrNull { it.id == stopId } ?: return
        stop.photos.forEach {
            runCatching {
                fs.delete(root / tripId / it.file)
                fs.delete(root / tripId / "th_${it.file}")
            }
        }
        mutate(tripId) { t -> t.copy(stops = t.stops.filter { it.id != stopId }) }
        Logger.i { "Wędrówki: punkt usunięty" }
    }

    /** Sets the link a picture was uploaded to. */
    fun setPhotoUrl(tripId: String, stopId: String, file: String, url: String) =
        mutateStop(tripId, stopId) { s ->
            s.copy(photos = s.photos.map { if (it.file == file) it.copy(url = url) else it })
        }

    private fun meters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double =
        Companion.meters(lat1, lon1, lat2, lon2)

    companion object {
        private const val TRIP_FILE = "trip.json"
        private const val MAIN_SIDE = 1600
        private const val MAIN_QUALITY = 85
        private const val THUMB_SIDE = 256
        private const val THUMB_QUALITY = 80
        private const val ID_CHARS = 8
        private const val STOP_JOIN_METERS = 40.0
        private const val GPS_MATCH_METERS = 150.0
        private const val TIME_SLACK_MS = 5 * 60_000L
        private const val DEG = 180.0
        private const val EARTH_RADIUS_M = 6_371_000.0

        /** The distance between two places in metres. */
        fun meters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = (lat2 - lat1) * PI / DEG
            val dLon = (lon2 - lon1) * PI / DEG
            val h =
                sin(dLat / 2) * sin(dLat / 2) +
                    cos(lat1 * PI / DEG) * cos(lat2 * PI / DEG) * sin(dLon / 2) * sin(dLon / 2)
            return 2 * EARTH_RADIUS_M * asin(sqrt(h))
        }
    }
}
