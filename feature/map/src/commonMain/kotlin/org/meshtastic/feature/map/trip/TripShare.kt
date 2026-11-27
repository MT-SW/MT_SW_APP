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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import org.meshtastic.core.ui.util.HikeImporter
import org.meshtastic.core.ui.util.HikeLinks
import org.meshtastic.core.ui.util.HikeTransport

/** The file a shared hike is sent as: a small route and one picture link for each place. */
@Serializable
data class SharedTrip(
    val v: Int = 1,
    val name: String,
    /** Each point as latitude, longitude, seconds. */
    val points: List<List<Double>>,
    val stops: List<SharedStop>,
)

@Serializable
data class SharedStop(
    val lat: Double,
    val lon: Double,
    val t: Long,
    val title: String = "",
    val photo: String,
    val caption: String = "",
)

/**
 * MT_SW: sharing a hike. The route (cut to the part chosen) and one picture of each place (the chosen one, else the
 * first) go to a public host as files; the link to the route file is what goes over the mesh. The same class reads such
 * a link back into a hike of the own device.
 */
@Single(binds = [TripShare::class, HikeImporter::class])
class TripShare(private val store: TripStore, private val transport: HikeTransport) : HikeImporter {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Uploads the hike [tripId] cut to the route points [from]..[to] and returns the link to its route file. [onStep]
     * gets the number of files done and the number of all files.
     */
    suspend fun share(tripId: String, from: Int, to: Int, onStep: (Int, Int) -> Unit): String {
        val trip = checkNotNull(store.find(tripId)) { "Brak wędrówki" }
        val first = from.coerceIn(0, trip.points.lastIndex)
        val last = to.coerceIn(first, trip.points.lastIndex)
        val points = trip.points.subList(first, last + 1)
        val start = points.first().t
        val end = points.last().t
        val stops = trip.stops.filter { it.photos.isNotEmpty() && it.t in start..end }.sortedBy { it.t }.take(MAX_STOPS)
        val total = stops.size + 1
        Logger.i { "Wędrówki: udostępnianie, punktów trasy ${points.size}, miejsc ${stops.size}" }
        val shared = mutableListOf<SharedStop>()
        stops.forEachIndexed { i, stop ->
            onStep(i, total)
            val photo = stop.photos.getOrNull(stop.shareIndex) ?: stop.photos.first()
            val url =
                photo.url.takeIf { HikeLinks.isAllowedHost(it) }
                    ?: run {
                        val bytes = checkNotNull(store.photoBytes(tripId, photo.file)) { "Brak zdjęcia" }
                        transport.upload(bytes, "image/jpeg", "photo.jpg").also {
                            store.setPhotoUrl(tripId, stop.id, photo.file, it)
                        }
                    }
            shared += SharedStop(stop.lat, stop.lon, stop.t, stop.title, url, photo.caption)
        }
        onStep(stops.size, total)
        val file =
            SharedTrip(
                name = trip.name,
                points = points.map { listOf(it.lat, it.lon, (it.t / MILLIS).toDouble()) },
                stops = shared.map { it.copy(t = it.t / MILLIS) },
            )
        val link =
            transport.upload(
                json.encodeToString(SharedTrip.serializer(), file).encodeToByteArray(),
                "application/json",
                "hike.json",
            )
        onStep(total, total)
        Logger.i { "Wędrówki: udostępniona, plików $total" }
        return link
    }

    /**
     * The whole hike as a file in the same form that is shared (route, places, the chosen picture link of each place
     * where it was uploaded), for saving on the device. Nothing is uploaded. Null when the hike is not found.
     */
    suspend fun exportJson(tripId: String): String? {
        val trip = store.find(tripId) ?: return null
        val stops =
            trip.stops
                .sortedBy { it.t }
                .map { stop ->
                    val photo = stop.photos.getOrNull(stop.shareIndex) ?: stop.photos.firstOrNull()
                    SharedStop(
                        lat = stop.lat,
                        lon = stop.lon,
                        t = stop.t / MILLIS,
                        title = stop.title,
                        photo = photo?.url.orEmpty(),
                        caption = photo?.caption.orEmpty(),
                    )
                }
        val file =
            SharedTrip(
                name = trip.name,
                points = trip.points.map { listOf(it.lat, it.lon, (it.t / MILLIS).toDouble()) },
                stops = stops,
            )
        Logger.i { "Wędrówki: zapis do pliku, punktów trasy ${trip.points.size}, miejsc ${stops.size}" }
        return json.encodeToString(SharedTrip.serializer(), file)
    }

    /**
     * The whole hike with its chosen pictures in one ZIP archive for saving on the device: the route file
     * ([ARCHIVE_ROUTE]) and the pictures next to it, so nothing needs the internet to be read back. Null when the hike
     * is not found.
     */
    suspend fun exportArchive(tripId: String): ByteArray? {
        val trip = store.find(tripId) ?: return null
        val files = mutableListOf<Pair<String, ByteArray>>()
        val stops =
            trip.stops
                .sortedBy { it.t }
                .map { stop ->
                    val photo = stop.photos.getOrNull(stop.shareIndex) ?: stop.photos.firstOrNull()
                    val bytes = photo?.let { store.photoBytes(tripId, it.file) }
                    val name = bytes?.let { "$ARCHIVE_PHOTO_DIR/${files.size + 1}.jpg".also { n -> files += n to it } }
                    SharedStop(
                        lat = stop.lat,
                        lon = stop.lon,
                        t = stop.t / MILLIS,
                        title = stop.title,
                        photo = name.orEmpty(),
                        caption = photo?.caption.orEmpty(),
                    )
                }
        val route =
            SharedTrip(
                name = trip.name,
                points = trip.points.map { listOf(it.lat, it.lon, (it.t / MILLIS).toDouble()) },
                stops = stops,
            )
        files.add(0, ARCHIVE_ROUTE to json.encodeToString(SharedTrip.serializer(), route).encodeToByteArray())
        Logger.i { "Wędrówki: zapis do archiwum, punktów trasy ${trip.points.size}, zdjęć ${files.size - 1}" }
        return TripArchive.write(files)
    }

    /** Reads a hike saved by [exportArchive] into the own hikes and returns its id. Throws when it cannot be read. */
    suspend fun importArchive(bytes: ByteArray): String {
        check(bytes.size <= MAX_ARCHIVE_BYTES) { "Archiwum jest za duże" }
        Logger.i { "Wędrówki: wczytywanie z archiwum, ${bytes.size} B" }
        val entries = checkNotNull(TripArchive.read(bytes, MAX_PHOTO_BYTES.toInt())) { "To nie jest archiwum wędrówki" }
        val route = checkNotNull(entries[ARCHIVE_ROUTE]) { "W archiwum brak pliku trasy" }
        check(route.size <= MAX_FILE_BYTES) { "Plik trasy jest za duży" }
        return build(
            json.decodeFromString<SharedTrip>(route.decodeToString()),
            senderNum = 0,
            onStep = { _, _ -> },
            allowed = { it.startsWith("$ARCHIVE_PHOTO_DIR/") && entries.containsKey(it) },
            fetch = { name -> checkNotNull(entries[name]) { "Brak zdjęcia" } },
            keepUrl = false,
        )
    }

    /** Reads the hike behind [url] into the own hikes and returns its id. Throws when it cannot be read. */
    override val receivedFrom: StateFlow<Map<Int, String>>
        get() = store.receivedFrom

    override suspend fun import(url: String, senderNum: Int, onStep: (Int, Int) -> Unit): String {
        check(HikeLinks.isAllowedHost(url)) { "Nieznany serwer" }
        return build(
            json.decodeFromString<SharedTrip>(transport.download(url, MAX_FILE_BYTES).decodeToString()),
            senderNum,
            onStep,
        )
    }

    /** Reads a hike saved to a file on the device ([exportJson]) into the own hikes and returns its id. */
    suspend fun importFile(text: String): String {
        check(text.length <= MAX_FILE_BYTES) { "Plik jest za duży" }
        Logger.i { "Wędrówki: wczytywanie z pliku, ${text.length} znaków" }
        return build(json.decodeFromString<SharedTrip>(text), senderNum = 0, onStep = { _, _ -> })
    }

    private suspend fun build(
        shared: SharedTrip,
        senderNum: Int,
        onStep: (Int, Int) -> Unit,
        allowed: (String) -> Boolean = { HikeLinks.isAllowedHost(it) },
        fetch: suspend (String) -> ByteArray = { transport.download(it, MAX_PHOTO_BYTES) },
        keepUrl: Boolean = true,
    ): String {
        check(shared.points.size in 2..MAX_POINTS) { "Zła liczba punktów trasy" }
        check(shared.points.all { it.size == POINT_FIELDS && it[0] in LAT_RANGE && it[1] in LON_RANGE }) {
            "Zły punkt trasy"
        }
        val id = store.newId()
        val points = shared.points.map { TripPoint(it[0], it[1], null, it[2].toLong() * MILLIS) }
        val stops = shared.stops.filter { allowed(it.photo) }.take(MAX_STOPS)
        val total = stops.size
        val built = mutableListOf<TripStop>()
        stops.forEachIndexed { i, s ->
            onStep(i, total)
            val photo =
                try {
                    store
                        .writePhoto(id, fetch(s.photo), s.t * MILLIS)
                        ?.copy(caption = s.caption.take(MAX_TEXT), url = if (keepUrl) s.photo else "")
                } catch (e: Exception) {
                    Logger.w { "Wędrówki: zdjęcia z udostępnionej wędrówki nie udało się pobrać: ${e.message}" }
                    null
                }
            built +=
                TripStop(
                    id = store.newId(),
                    lat = s.lat,
                    lon = s.lon,
                    t = s.t * MILLIS,
                    title = s.title.take(MAX_TEXT),
                    photos = listOfNotNull(photo),
                )
        }
        onStep(total, total)
        val trip =
            Trip(
                id = id,
                name = shared.name.take(MAX_TEXT).ifBlank { "Wędrówka" },
                startedMillis = points.first().t,
                endedMillis = points.last().t,
                distanceMeters = routeLength(points),
                points = points,
                stops = built,
                sharedBy = senderNum,
            )
        store.save(trip)
        Logger.i { "Wędrówki: odebrana, punktów trasy ${points.size}, miejsc ${built.size}" }
        return id
    }

    private fun routeLength(points: List<TripPoint>): Double =
        points.zipWithNext().sumOf { (a, b) -> TripStore.meters(a.lat, a.lon, b.lat, b.lon) }

    private companion object {
        const val MILLIS = 1000L
        const val MAX_STOPS = 40
        const val MAX_POINTS = 20_000
        const val POINT_FIELDS = 3
        const val MAX_FILE_BYTES = 4L * 1024 * 1024
        const val MAX_ARCHIVE_BYTES = 120 * 1024 * 1024
        const val ARCHIVE_ROUTE = "wedrowka.json"
        const val ARCHIVE_PHOTO_DIR = "zdjecia"
        const val MAX_PHOTO_BYTES = 8L * 1024 * 1024
        const val MAX_TEXT = 200
        val LAT_RANGE = -90.0..90.0
        val LON_RANGE = -180.0..180.0
    }
}
