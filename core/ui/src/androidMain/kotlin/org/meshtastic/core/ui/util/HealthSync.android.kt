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
@file:Suppress("MagicNumber", "ReturnCount", "ThrowsCount", "TooGenericExceptionCaught")

package org.meshtastic.core.ui.util

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt

private val READ_PERMISSIONS =
    setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class),
    )

private val WRITE_PERMISSIONS =
    setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE,
    )

@Composable
actual fun rememberHealthSync(): HealthSync? {
    val context = LocalContext.current
    val status = remember { HealthConnectClient.getSdkStatus(context) }
    if (status != HealthConnectClient.SDK_AVAILABLE) {
        Logger.i { "Zdrowie: Health Connect niedostępny w tym telefonie (stan $status)" }
        return null
    }
    val pending = rememberUpdatedState(arrayOfNulls<CompletableDeferred<Set<String>>>(1))
    val launcher =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            Logger.i { "Zdrowie: odpowiedź na pytanie o zgody, przyznano ${granted.size}" }
            pending.value[0]?.complete(granted)
        }
    return remember(context) {
        AndroidHealthSync(context) { missing ->
            val wait = CompletableDeferred<Set<String>>()
            pending.value[0] = wait
            launcher.launch(missing)
            wait.await()
        }
    }
}

private class AndroidHealthSync(context: Context, private val ask: suspend (Set<String>) -> Set<String>) : HealthSync {
    private val client = HealthConnectClient.getOrCreate(context)

    private suspend fun ensure(wanted: Set<String>): Set<String> {
        val have = client.permissionController.getGrantedPermissions()
        val missing = wanted - have
        if (missing.isEmpty()) return have
        Logger.i { "Zdrowie: brakuje zgód (${missing.size}), pytam" }
        return ask(missing) + have
    }

    override suspend fun read(fromMillis: Long, toMillis: Long): HealthData? = try {
        val granted = ensure(READ_PERMISSIONS)
        val metrics = buildSet {
            if (HealthPermission.getReadPermission(StepsRecord::class) in granted) add(StepsRecord.COUNT_TOTAL)
            if (HealthPermission.getReadPermission(DistanceRecord::class) in granted) {
                add(DistanceRecord.DISTANCE_TOTAL)
            }
            if (HealthPermission.getReadPermission(HeartRateRecord::class) in granted) {
                add(HeartRateRecord.BPM_AVG)
                add(HeartRateRecord.BPM_MAX)
            }
            if (HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted) {
                add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            }
            if (HealthPermission.getReadPermission(ElevationGainedRecord::class) in granted) {
                add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
            }
        }
        if (metrics.isEmpty()) {
            Logger.w { "Zdrowie: brak zgody na odczyt, nic nie pobrano" }
            null
        } else {
            val result =
                client.aggregate(
                    AggregateRequest(
                        metrics = metrics,
                        timeRangeFilter =
                        TimeRangeFilter.between(
                            Instant.ofEpochMilli(fromMillis),
                            Instant.ofEpochMilli(toMillis),
                        ),
                    ),
                )
            val data =
                HealthData(
                    steps = result[StepsRecord.COUNT_TOTAL],
                    distanceMeters = result[DistanceRecord.DISTANCE_TOTAL]?.inMeters,
                    avgHeartRate = result[HeartRateRecord.BPM_AVG]?.toInt(),
                    maxHeartRate = result[HeartRateRecord.BPM_MAX]?.toInt(),
                    calories =
                    result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.roundToInt(),
                    ascentMeters = result[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters?.roundToInt(),
                )
            Logger.i { "Zdrowie: odczyt zakończony, dane ${if (data.isEmpty) "puste" else "są"}" }
            data
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w { "Zdrowie: odczyt nie udał się: ${e.message}" }
        null
    }

    override suspend fun export(
        title: String,
        fromMillis: Long,
        toMillis: Long,
        distanceMeters: Double,
        route: List<HealthRoutePoint>,
    ): Boolean {
        val granted =
            try {
                ensure(WRITE_PERMISSIONS)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w { "Zdrowie: pytanie o zgody na zapis nie udało się: ${e.message}" }
                return false
            }
        if (HealthPermission.getWritePermission(ExerciseSessionRecord::class) !in granted) {
            Logger.w { "Zdrowie: brak zgody na zapis treningu" }
            return false
        }
        val start = Instant.ofEpochMilli(fromMillis)
        val end = Instant.ofEpochMilli(toMillis.coerceAtLeast(fromMillis + 1000))
        val locations =
            route
                .filter { it.t in fromMillis..toMillis }
                .map {
                    ExerciseRoute.Location(
                        time = Instant.ofEpochMilli(it.t),
                        latitude = it.lat,
                        longitude = it.lon,
                        altitude = it.ele?.let { e -> Length.meters(e) },
                    )
                }
        suspend fun insert(withRoute: Boolean) {
            val session =
                ExerciseSessionRecord(
                    startTime = start,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = end,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
                    title = title,
                    exerciseRoute = if (withRoute && locations.isNotEmpty()) ExerciseRoute(locations) else null,
                )
            val records =
                buildList<androidx.health.connect.client.records.Record> {
                    add(session)
                    if (distanceMeters > 0) {
                        add(
                            DistanceRecord(
                                startTime = start,
                                startZoneOffset = ZoneOffset.UTC,
                                endTime = end,
                                endZoneOffset = ZoneOffset.UTC,
                                distance = Length.meters(distanceMeters),
                                metadata = Metadata.manualEntry(),
                            ),
                        )
                    }
                }
            client.insertRecords(records)
        }
        return try {
            insert(withRoute = HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE in granted)
            Logger.i { "Zdrowie: trening zapisany, punktów trasy ${locations.size}" }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w { "Zdrowie: zapis z trasą nie udał się (${e.message}), próbuję bez trasy" }
            try {
                insert(withRoute = false)
                Logger.i { "Zdrowie: trening zapisany bez trasy" }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e2: Exception) {
                Logger.w { "Zdrowie: zapis treningu nie udał się: ${e2.message}" }
                false
            }
        }
    }
}
