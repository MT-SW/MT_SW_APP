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
@file:Suppress("MagicNumber")

package org.meshtastic.core.service

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.sqrt

/** Readings per second of the accelerometer while the phone's GPS rests. */
private const val SAMPLE_SECONDS = 0.5

/** How far (in g) the total acceleration has to differ from rest to count as a shake of movement. */
private const val MOTION_THRESHOLD_G = 0.12

/** Shakes needed inside one window of [WINDOW_SAMPLES] readings, so one bump of the table does not wake the GPS. */
private const val MOTION_HITS = 4

private const val WINDOW_SAMPLES = 12

/**
 * MT_SW: iPhone counterpart of Android's significant-motion sensor. Suspends until the accelerometer shows steady
 * movement (walking, driving) and returns true; returns false at once when the phone has no accelerometer, since
 * nothing would then wake a sleeping GPS. Needs no permission.
 */
@OptIn(ExperimentalForeignApi::class)
internal suspend fun awaitSignificantMotion(): Boolean {
    val manager = CMMotionManager()
    if (!manager.accelerometerAvailable) return false
    return suspendCancellableCoroutine { continuation ->
        var hits = 0
        var samples = 0
        manager.accelerometerUpdateInterval = SAMPLE_SECONDS
        manager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
            val reading = data ?: return@startAccelerometerUpdatesToQueue
            val total = reading.acceleration.useContents { sqrt(x * x + y * y + z * z) }
            if (abs(total - 1.0) > MOTION_THRESHOLD_G) hits++
            samples++
            if (hits >= MOTION_HITS) {
                manager.stopAccelerometerUpdates()
                if (continuation.isActive) continuation.resume(true)
            } else if (samples >= WINDOW_SAMPLES) {
                hits = 0
                samples = 0
            }
        }
        continuation.invokeOnCancellation { manager.stopAccelerometerUpdates() }
    }
}
