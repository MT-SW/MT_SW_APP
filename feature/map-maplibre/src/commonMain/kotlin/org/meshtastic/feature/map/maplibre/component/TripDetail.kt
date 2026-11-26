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
@file:Suppress("ModifierMissing", "LongMethod", "TooManyFunctions", "TooGenericExceptionCaught")

package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.trips_add_gallery
import org.meshtastic.core.resources.trips_add_stop
import org.meshtastic.core.resources.trips_caption
import org.meshtastic.core.resources.trips_chosen
import org.meshtastic.core.resources.trips_move_to
import org.meshtastic.core.resources.trips_no_time
import org.meshtastic.core.resources.trips_photo_taken
import org.meshtastic.core.resources.trips_rename
import org.meshtastic.core.resources.trips_send_this
import org.meshtastic.core.resources.trips_share
import org.meshtastic.core.resources.trips_share_chat
import org.meshtastic.core.resources.trips_share_chat_hint
import org.meshtastic.core.resources.trips_share_copy
import org.meshtastic.core.resources.trips_share_from
import org.meshtastic.core.resources.trips_share_go
import org.meshtastic.core.resources.trips_share_progress
import org.meshtastic.core.resources.trips_share_range
import org.meshtastic.core.resources.trips_share_to
import org.meshtastic.core.resources.trips_share_warning
import org.meshtastic.core.resources.trips_stats
import org.meshtastic.core.resources.trips_stats_ascent
import org.meshtastic.core.resources.trips_stats_counts
import org.meshtastic.core.resources.trips_stop
import org.meshtastic.core.resources.trips_stop_place
import org.meshtastic.core.resources.trips_stop_time
import org.meshtastic.core.resources.trips_stop_title
import org.meshtastic.core.resources.trips_unplaced
import org.meshtastic.core.resources.trips_unplaced_note
import org.meshtastic.core.ui.component.StyledBottomSheet
import org.meshtastic.core.ui.icon.Add
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PhotoCamera
import org.meshtastic.core.ui.util.ChatDraftRequest
import org.meshtastic.core.ui.util.HikeImportRequest
import org.meshtastic.core.ui.util.HikeLinks
import org.meshtastic.core.ui.util.createClipEntry
import org.meshtastic.core.ui.util.readExif
import org.meshtastic.core.ui.util.rememberCameraLauncher
import org.meshtastic.core.ui.util.rememberMultiImagePicker
import org.meshtastic.feature.map.playback.RoutePlayback
import org.meshtastic.feature.map.trip.Trip
import org.meshtastic.feature.map.trip.TripShare
import org.meshtastic.feature.map.trip.TripStop
import org.meshtastic.feature.map.trip.TripStore
import org.meshtastic.feature.map.trip.stats
import org.meshtastic.feature.map.trip.TripViewRequest
import kotlin.math.abs

/** Shows the details of a hike while a stop is asked for from the map (a tap on a drop). */
@Composable
internal fun TripViewHost() {
    val store: TripStore = koinInject()
    val playback: RoutePlayback = koinInject()
    val received by HikeImportRequest.pendingReplay.collectAsStateWithLifecycle()
    // A hike received from a message is replayed as soon as the map is shown.
    LaunchedEffect(received) {
        received?.let { id ->
            Logger.i { "Wędrówki: odtwarzanie odebranej wędrówki" }
            store.setVisible(id, true)
            playback.playTrip(id)
            HikeImportRequest.pendingReplay.value = null
        }
    }
    val request by TripViewRequest.stop.collectAsStateWithLifecycle()
    request?.let { (tripId, stopId) ->
        TripDetailWindow(tripId = tripId, focusStopId = stopId, onDismiss = { TripViewRequest.stop.value = null })
    }
}

/**
 * MT_SW: the details of one hike: its points with stacks of pictures. Pictures come from the camera or the gallery,
 * to a chosen point or placed by themselves by their time and position, and every placement can be changed by hand.
 */
@Composable
internal fun TripDetailWindow(tripId: String, focusStopId: String? = null, onDismiss: () -> Unit) {
    val store: TripStore = koinInject()
    val trips by store.trips.collectAsStateWithLifecycle()
    val trip = trips.firstOrNull { it.id == tripId }
    val scope = rememberCoroutineScope()
    val unplacedTitle = stringResource(Res.string.trips_unplaced)
    var unplaced by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Pair<String, String>?>(null) }
    val pickAll =
        rememberMultiImagePicker { picked ->
            scope.launch {
                val missing =
                    withContext(Dispatchers.Default) {
                        store.addPhotosAuto(tripId, picked.map { it.bytes to readExif(it.bytes) }, unplacedTitle)
                    }
                unplaced = missing
            }
        }
    StyledBottomSheet(onDismissRequest = onDismiss) {
        if (trip == null) {
            Text(text = "", modifier = Modifier.padding(16.dp))
            return@StyledBottomSheet
        }
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NameField(trip, store)
            val stats = remember(trip) { trip.stats() }
            Text(
                text =
                stringResource(Res.string.trips_stats, stats.distanceText, stats.durationText, stats.averageText),
                style = MaterialTheme.typography.bodyMedium,
            )
            stats.ascentMeters?.let {
                Text(stringResource(Res.string.trips_stats_ascent, it), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = stringResource(Res.string.trips_stats_counts, trip.points.size, trip.photoCount),
                style = MaterialTheme.typography.bodySmall,
            )
            TripCharts(trip)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pickAll?.let { pick ->
                    TextButton(onClick = pick) {
                        Icon(imageVector = MeshtasticIcons.Add, contentDescription = null)
                        Text(stringResource(Res.string.trips_add_gallery), modifier = Modifier.padding(start = 4.dp))
                    }
                }
                TextButton(
                    onClick = { store.addStop(tripId, trip.points.lastIndex) },
                    enabled = trip.points.isNotEmpty(),
                ) {
                    Text(stringResource(Res.string.trips_add_stop))
                }
            }
            if (unplaced > 0) {
                Text(
                    text = stringResource(Res.string.trips_unplaced_note, unplaced),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            HorizontalDivider()
            trip.stops.sortedBy { it.t }.forEachIndexed { index, stop ->
                StopCard(
                    trip = trip,
                    stop = stop,
                    number = index + 1,
                    focused = stop.id == focusStopId,
                    onOpenPhoto = { file -> editing = stop.id to file },
                )
            }
            HorizontalDivider()
            ShareSection(trip = trip, onClose = onDismiss)
        }
    }
    editing?.let { (stopId, file) ->
        PhotoDialog(trip = trip, stopId = stopId, file = file, onDismiss = { editing = null })
    }
}

@Composable
private fun NameField(trip: Trip, store: TripStore) {
    var name by remember(trip.id) { mutableStateOf(trip.name) }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(Res.string.trips_rename)) },
        singleLine = true,
        modifier =
        Modifier.fillMaxWidth().onFocusChanged {
            if (!it.isFocused && name != trip.name && name.isNotBlank()) store.rename(trip.id, name)
        },
    )
}

@Composable
private fun StopCard(trip: Trip, stop: TripStop, number: Int, focused: Boolean, onOpenPhoto: (String) -> Unit) {
    val store: TripStore = koinInject()
    val scope = rememberCoroutineScope()
    val camera =
        rememberCameraLauncher { bytes, _ ->
            store.archive(bytes)
            store.addPhotos(trip.id, stop.id, listOf(bytes to nowTaken()))
        }
    val gallery =
        rememberMultiImagePicker { picked ->
            scope.launch {
                withContext(Dispatchers.Default) {
                    store.addPhotos(trip.id, stop.id, picked.map { it.bytes to (readExif(it.bytes).takenMillis ?: 0L) })
                }
            }
        }
    Card(
        colors =
        CardDefaults.cardColors(
            containerColor =
            if (focused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(Res.string.trips_stop, number), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(Res.string.trips_stop_time, DateFormatter.formatTime(stop.t)),
                style = MaterialTheme.typography.bodySmall,
            )
            var title by remember(stop.id, stop.title) { mutableStateOf(stop.title) }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(Res.string.trips_stop_title)) },
                singleLine = true,
                modifier =
                Modifier.fillMaxWidth().onFocusChanged {
                    if (!it.isFocused && title != stop.title) store.setStopTitle(trip.id, stop.id, title)
                },
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                stop.photos.forEachIndexed { i, photo ->
                    TripThumb(
                        tripId = trip.id,
                        file = photo.file,
                        size = THUMB_DP.dp,
                        chosen = i == stop.shareIndex,
                        onClick = { onOpenPhoto(photo.file) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                camera?.let {
                    IconButton(onClick = it) {
                        Icon(imageVector = MeshtasticIcons.PhotoCamera, contentDescription = null)
                    }
                }
                gallery?.let {
                    IconButton(onClick = it) { Icon(imageVector = MeshtasticIcons.Add, contentDescription = null) }
                }
                IconButton(onClick = { store.removeStop(trip.id, stop.id) }) {
                    Icon(imageVector = MeshtasticIcons.Delete, contentDescription = stringResource(Res.string.delete))
                }
            }
            if (trip.points.size > 1) {
                val current = nearestIndex(trip, stop)
                var position by remember(stop.id, stop.t) { mutableFloatStateOf(current.toFloat()) }
                Text(stringResource(Res.string.trips_stop_place), style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = position,
                    onValueChange = { position = it },
                    onValueChangeFinished = { store.placeStop(trip.id, stop.id, position.toInt()) },
                    valueRange = 0f..trip.points.lastIndex.toFloat(),
                )
            }
        }
    }
}

private fun nearestIndex(trip: Trip, stop: TripStop): Int =
    trip.points.indices.minByOrNull { abs(trip.points[it].t - stop.t) } ?: 0

private fun nowTaken(): Long = org.meshtastic.core.common.util.nowMillis

@Composable
private fun PhotoDialog(trip: Trip, stopId: String, file: String, onDismiss: () -> Unit) {
    val store: TripStore = koinInject()
    val stop = trip.stops.firstOrNull { it.id == stopId }
    val photo = stop?.photos?.firstOrNull { it.file == file }
    if (stop == null || photo == null) {
        onDismiss()
        return
    }
    val big by produceState<ImageBitmap?>(null, trip.id, file) {
        value =
            withContext(Dispatchers.Default) {
                store.photoBytes(trip.id, file)?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
            }
    }
    var caption by remember(file) { mutableStateOf(photo.caption) }
    var moving by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = {
            if (caption != photo.caption) store.setCaption(trip.id, stopId, file, caption)
            onDismiss()
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                big?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text =
                    if (photo.takenMillis > 0L) {
                        stringResource(Res.string.trips_photo_taken, DateFormatter.formatDateTime(photo.takenMillis))
                    } else {
                        stringResource(Res.string.trips_no_time)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text(stringResource(Res.string.trips_caption)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                val index = stop.photos.indexOfFirst { it.file == file }
                if (index == stop.shareIndex) {
                    Text(stringResource(Res.string.trips_chosen), style = MaterialTheme.typography.labelMedium)
                } else {
                    TextButton(onClick = { store.setShareIndex(trip.id, stopId, index) }) {
                        Text(stringResource(Res.string.trips_send_this))
                    }
                }
                if (trip.stops.size > 1) {
                    TextButton(onClick = { moving = !moving }) { Text(stringResource(Res.string.trips_move_to)) }
                    if (moving) {
                        trip.stops.sortedBy { it.t }.forEachIndexed { i, other ->
                            if (other.id != stopId) {
                                TextButton(
                                    onClick = {
                                        store.movePhoto(trip.id, stopId, file, other.id)
                                        onDismiss()
                                    },
                                ) {
                                    Text(
                                        stringResource(Res.string.trips_stop, i + 1) +
                                            if (other.title.isNotBlank()) " – ${other.title}" else "",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (caption != photo.caption) store.setCaption(trip.id, stopId, file, caption)
                    onDismiss()
                },
            ) {
                Text(stringResource(Res.string.close))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    store.removePhoto(trip.id, stopId, file)
                    onDismiss()
                },
            ) {
                Text(stringResource(Res.string.delete))
            }
        },
    )
    Logger.d { "Wędrówki: okno zdjęcia otwarte" }
}

/** The small copy of a picture of a hike, round-cornered; the one chosen to be sent has a frame. */
@Composable
internal fun TripThumb(tripId: String, file: String, size: Dp, chosen: Boolean, onClick: () -> Unit) {
    val store: TripStore = koinInject()
    val bitmap by produceState<ImageBitmap?>(null, tripId, file) {
        value =
            withContext(Dispatchers.Default) {
                store.photoBytes(tripId, file, small = true)?.let {
                    runCatching { it.decodeToImageBitmap() }.getOrNull()
                }
            }
    }
    val shape = RoundedCornerShape(THUMB_CORNER_DP.dp)
    val frame = if (chosen) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier
    Box(modifier = Modifier.size(size).clip(shape).then(frame).clickable(onClick = onClick)) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } ?: Box(modifier = Modifier.size(size).clip(shape).border(1.dp, Color.Gray, shape))
    }
}

private const val THUMB_DP = 72
private const val THUMB_CORNER_DP = 8

/** A picture of a hike shown large (a slide of the replay). */
@Composable
internal fun TripSlideImage(tripId: String, file: String, modifier: Modifier = Modifier) {
    val store: TripStore = koinInject()
    val bitmap by produceState<ImageBitmap?>(null, tripId, file) {
        value =
            withContext(Dispatchers.Default) {
                store.photoBytes(tripId, file)?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
            }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.heightIn(max = SLIDE_MAX_HEIGHT_DP.dp),
        )
    }
}

private const val SLIDE_MAX_HEIGHT_DP = 220

/** The sharing of a hike: the part of the route to send, the warning, and the link at the end. */
@Composable
private fun ShareSection(trip: Trip, onClose: () -> Unit) {
    val share: TripShare = koinInject()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    var cutStart by remember(trip.id) { mutableFloatStateOf(0f) }
    var cutEnd by remember(trip.id) { mutableFloatStateOf(trip.points.lastIndex.coerceAtLeast(0).toFloat()) }
    var progress by remember(trip.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var link by remember(trip.id) { mutableStateOf<String?>(null) }
    if (trip.points.size < 2) return
    val last = trip.points.lastIndex.toFloat()
    Text(stringResource(Res.string.trips_share), style = MaterialTheme.typography.titleSmall)
    Text(
        text = stringResource(Res.string.trips_share_warning),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
    val from = trip.points[cutStart.toInt().coerceIn(0, trip.points.lastIndex)].t
    val to = trip.points[cutEnd.toInt().coerceIn(0, trip.points.lastIndex)].t
    Text(
        text =
        stringResource(Res.string.trips_share_range, DateFormatter.formatTime(from), DateFormatter.formatTime(to)),
        style = MaterialTheme.typography.bodySmall,
    )
    Text(stringResource(Res.string.trips_share_from), style = MaterialTheme.typography.bodySmall)
    Slider(value = cutStart, onValueChange = { cutStart = it.coerceAtMost(cutEnd) }, valueRange = 0f..last)
    Text(stringResource(Res.string.trips_share_to), style = MaterialTheme.typography.bodySmall)
    Slider(value = cutEnd, onValueChange = { cutEnd = it.coerceAtLeast(cutStart) }, valueRange = 0f..last)
    val running = progress
    if (running != null) {
        Text(
            text = stringResource(Res.string.trips_share_progress, running.first, running.second),
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        TextButton(
            onClick = {
                progress = 0 to 1
                scope.launch {
                    try {
                        link =
                            share.share(trip.id, cutStart.toInt(), cutEnd.toInt()) { done, all ->
                                progress = done to all
                            }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.w { "Wędrówki: udostępnianie nie udało się: ${e.message}" }
                    }
                    progress = null
                }
            },
        ) {
            Text(stringResource(Res.string.trips_share_go))
        }
    }
    link?.let { url ->
        Text(text = url, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = { scope.launch { clipboard.setClipEntry(createClipEntry(url, "hike")) } },
            ) {
                Text(stringResource(Res.string.trips_share_copy))
            }
            TextButton(
                onClick = {
                    ChatDraftRequest.text.value = HikeLinks.message(url)
                    Logger.i { "Wędrówki: link wstawiony do następnej rozmowy" }
                    onClose()
                },
            ) {
                Text(stringResource(Res.string.trips_share_chat))
            }
        }
        Text(stringResource(Res.string.trips_share_chat_hint), style = MaterialTheme.typography.bodySmall)
    }
}
