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
@file:OptIn(ExperimentalForeignApi::class)
@file:Suppress("TooGenericExceptionCaught")

package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKQuantity
import platform.HealthKit.HKQuantityType
import platform.HealthKit.HKQuantityTypeIdentifierActiveEnergyBurned
import platform.HealthKit.HKQuantityTypeIdentifierDistanceWalkingRunning
import platform.HealthKit.HKQuantityTypeIdentifierHeartRate
import platform.HealthKit.HKQuantityTypeIdentifierStepCount
import platform.HealthKit.HKQuantitySample
import platform.HealthKit.HKQuery
import platform.HealthKit.HKQueryOptionNone
import platform.HealthKit.HKSeriesType
import platform.HealthKit.HKStatisticsOptionCumulativeSum
import platform.HealthKit.HKStatisticsOptionDiscreteAverage
import platform.HealthKit.HKStatisticsOptionDiscreteMax
import platform.HealthKit.HKStatisticsQuery
import platform.HealthKit.HKUnit
import platform.HealthKit.HKWorkout
import platform.HealthKit.HKWorkoutActivityTypeHiking
import platform.HealthKit.HKWorkoutBuilder
import platform.HealthKit.HKWorkoutConfiguration
import platform.HealthKit.HKWorkoutRouteBuilder
import platform.HealthKit.HKWorkoutType
import platform.HealthKit.countUnit
import platform.HealthKit.kilocalorieUnit
import platform.HealthKit.meterUnit
import platform.HealthKit.minuteUnit
import platform.HealthKit.predicateForSamplesWithStartDate
import platform.HealthKit.unitDividedByUnit
import kotlin.coroutines.resume
import kotlin.math.roundToInt

private const val MS_PER_SECOND = 1000.0
private const val ROUTE_CHUNK = 500
private const val ACCURACY_METERS = 5.0

@Composable
actual fun rememberHealthSync(): HealthSync? = remember {
    if (HKHealthStore.isHealthDataAvailable()) {
        IosHealthSync()
    } else {
        Logger.i { "Zdrowie: Zdrowie (HealthKit) niedostępne na tym urządzeniu" }
        null
    }
}

private fun date(millis: Long): NSDate = NSDate.dateWithTimeIntervalSince1970(millis / MS_PER_SECOND)

private fun quantity(id: String?): HKQuantityType? = HKQuantityType.quantityTypeForIdentifier(id)

private class IosHealthSync : HealthSync {
    private val store = HKHealthStore()

    private suspend fun authorize(share: Set<Any>, read: Set<Any>): Boolean =
        suspendCancellableCoroutine { c ->
            store.requestAuthorizationToShareTypes(share, readTypes = read) { ok, error ->
                if (!ok) Logger.w { "Zdrowie: pytanie o zgody nie udało się: ${error?.localizedDescription}" }
                c.resume(ok)
            }
        }

    /** One number from the store over the time range; null when there is none or no right to read it. */
    private suspend fun number(
        type: HKQuantityType,
        from: Long,
        to: Long,
        options: ULong,
        read: (platform.HealthKit.HKStatistics) -> Double?,
    ): Double? =
        suspendCancellableCoroutine { c ->
            val predicate =
                HKQuery.predicateForSamplesWithStartDate(date(from), endDate = date(to), options = HKQueryOptionNone)
            val query =
                HKStatisticsQuery(
                    quantityType = type,
                    quantitySamplePredicate = predicate,
                    options = options,
                    completionHandler = { _, stats, error ->
                        if (error != null) Logger.i { "Zdrowie: zapytanie bez wyniku: ${error.localizedDescription}" }
                        c.resume(stats?.let(read))
                    },
                )
            store.executeQuery(query)
        }

    override suspend fun read(fromMillis: Long, toMillis: Long): HealthData? =
        try {
            val steps = quantity(HKQuantityTypeIdentifierStepCount)
            val distance = quantity(HKQuantityTypeIdentifierDistanceWalkingRunning)
            val heart = quantity(HKQuantityTypeIdentifierHeartRate)
            val energy = quantity(HKQuantityTypeIdentifierActiveEnergyBurned)
            val types = listOfNotNull(steps, distance, heart, energy).toSet<Any>()
            if (!authorize(emptySet(), types)) {
                null
            } else {
                val bpm = HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit())
                val data =
                    HealthData(
                        steps =
                        steps?.let {
                            number(it, fromMillis, toMillis, HKStatisticsOptionCumulativeSum) { s ->
                                s.sumQuantity()?.doubleValueForUnit(HKUnit.countUnit())
                            }
                        }?.toLong(),
                        distanceMeters =
                        distance?.let {
                            number(it, fromMillis, toMillis, HKStatisticsOptionCumulativeSum) { s ->
                                s.sumQuantity()?.doubleValueForUnit(HKUnit.meterUnit())
                            }
                        },
                        avgHeartRate =
                        heart?.let {
                            number(it, fromMillis, toMillis, HKStatisticsOptionDiscreteAverage) { s ->
                                s.averageQuantity()?.doubleValueForUnit(bpm)
                            }
                        }?.roundToInt(),
                        maxHeartRate =
                        heart?.let {
                            number(it, fromMillis, toMillis, HKStatisticsOptionDiscreteMax) { s ->
                                s.maximumQuantity()?.doubleValueForUnit(bpm)
                            }
                        }?.roundToInt(),
                        calories =
                        energy?.let {
                            number(it, fromMillis, toMillis, HKStatisticsOptionCumulativeSum) { s ->
                                s.sumQuantity()?.doubleValueForUnit(HKUnit.kilocalorieUnit())
                            }
                        }?.roundToInt(),
                    )
                Logger.i { "Zdrowie: odczyt zakończony, dane ${if (data.isEmpty) "puste" else "są"}" }
                data
            }
        } catch (e: Exception) {
            Logger.w { "Zdrowie: odczyt nie udał się: ${e.message}" }
            null
        }

    private suspend fun step(label: String, run: ((Boolean, platform.Foundation.NSError?) -> Unit) -> Unit): Boolean =
        suspendCancellableCoroutine { c ->
            run { ok, error ->
                if (!ok) Logger.w { "Zdrowie: $label nie udało się: ${error?.localizedDescription}" }
                c.resume(ok)
            }
        }

    override suspend fun export(
        title: String,
        fromMillis: Long,
        toMillis: Long,
        distanceMeters: Double,
        route: List<HealthRoutePoint>,
    ): Boolean =
        try {
            val distanceType = quantity(HKQuantityTypeIdentifierDistanceWalkingRunning)
            val share =
                listOfNotNull(HKWorkoutType.workoutType(), HKSeriesType.workoutRouteType(), distanceType).toSet<Any>()
            if (!authorize(share, emptySet())) {
                false
            } else {
                val config = HKWorkoutConfiguration().apply { activityType = HKWorkoutActivityTypeHiking }
                val builder = HKWorkoutBuilder(healthStore = store, configuration = config, device = null)
                val start = date(fromMillis)
                val end = date(toMillis.coerceAtLeast(fromMillis + 1000))
                var ok =
                    step("rozpoczęcie treningu") { done ->
                        builder.beginCollectionWithStartDate(start, completion = done)
                    }
                if (ok && distanceType != null && distanceMeters > 0) {
                    val sample =
                        HKQuantitySample.quantitySampleWithType(
                            distanceType,
                            quantity = HKQuantity.quantityWithUnit(HKUnit.meterUnit(), doubleValue = distanceMeters),
                            startDate = start,
                            endDate = end,
                        )
                    ok = step("dodanie dystansu") { done -> builder.addSamples(listOf(sample), completion = done) }
                }
                if (ok) {
                    ok =
                        step("zakończenie treningu") { done ->
                            builder.endCollectionWithEndDate(end, completion = done)
                        }
                }
                val workout: HKWorkout? =
                    if (!ok) {
                        null
                    } else {
                        suspendCancellableCoroutine { c ->
                            builder.finishWorkoutWithCompletion { w, error ->
                                if (w == null) {
                                    Logger.w { "Zdrowie: zapis treningu nie udał się: ${error?.localizedDescription}" }
                                }
                                c.resume(w)
                            }
                        }
                    }
                if (workout == null) {
                    false
                } else {
                    saveRoute(workout, route.filter { it.t in fromMillis..toMillis })
                    Logger.i { "Zdrowie: trening zapisany" }
                    true
                }
            }
        } catch (e: Exception) {
            Logger.w { "Zdrowie: zapis nie udał się: ${e.message}" }
            false
        }

    /** The route is a bonus: when it fails the workout stays saved without it. */
    private suspend fun saveRoute(workout: HKWorkout, points: List<HealthRoutePoint>) {
        if (points.isEmpty()) return
        val routeBuilder = HKWorkoutRouteBuilder(healthStore = store, device = null)
        val locations =
            points.map {
                CLLocation(
                    coordinate = CLLocationCoordinate2DMake(it.lat, it.lon),
                    altitude = it.ele ?: 0.0,
                    horizontalAccuracy = ACCURACY_METERS,
                    verticalAccuracy = if (it.ele != null) ACCURACY_METERS else -1.0,
                    timestamp = date(it.t),
                )
            }
        var ok = true
        locations.chunked(ROUTE_CHUNK).forEach { chunk ->
            if (ok) ok = step("dodanie trasy") { done -> routeBuilder.insertRouteData(chunk, completion = done) }
        }
        if (!ok) return
        suspendCancellableCoroutine<Unit> { c ->
            routeBuilder.finishRouteWithWorkout(workout, metadata = null) { route, error ->
                if (route == null) Logger.w { "Zdrowie: zapis trasy nie udał się: ${error?.localizedDescription}" }
                c.resume(Unit)
            }
        }
    }
}
