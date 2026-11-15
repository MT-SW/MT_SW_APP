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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AudioToolbox.AudioServicesPlaySystemSound

/** The system sound id of the iPhone's standard vibration. */
private const val SYSTEM_VIBRATE_ID = 4095u

/** One system vibration lasts about this long, so the next one is started right after it. */
private const val VIBRATE_REPEAT_MS = 380L

@Composable
actual fun rememberHoldVibration(): HoldVibration = remember { IosHoldVibration() }

/** Plays the iPhone's own vibration again and again, until stopped. */
private class IosHoldVibration : HoldVibration {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun start() {
        if (job?.isActive == true) return
        job =
            scope.launch {
                while (isActive) {
                    AudioServicesPlaySystemSound(SYSTEM_VIBRATE_ID)
                    delay(VIBRATE_REPEAT_MS)
                }
            }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }
}
