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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

/** The system sound id of the iPhone's standard vibration. */
private const val SYSTEM_VIBRATE_ID = 4095u

/** One system vibration lasts about this long, so the next one is started right after it. */
private const val VIBRATE_REPEAT_MS = 380L

/** The pause between two strong taps of the Taptic Engine. */
private const val TAP_REPEAT_MS = 55L

@Composable
actual fun rememberHoldVibration(): HoldVibration {
    val scope = rememberCoroutineScope()
    return remember(scope) { IosHoldVibration(scope) }
}

/** Plays the iPhone's own vibration again and again, until stopped. */
private class IosHoldVibration(private val scope: CoroutineScope) : HoldVibration {
    private var job: Job? = null

    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            // The Taptic Engine hammered with strong taps feels like one buzz; the system vibration is played too,
            // for the phones where only that one is switched on in the sound settings.
            val taps = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
            taps.prepare()
            var sinceVibrate = VIBRATE_REPEAT_MS
            while (isActive) {
                taps.impactOccurred()
                if (sinceVibrate >= VIBRATE_REPEAT_MS) {
                    AudioServicesPlaySystemSound(SYSTEM_VIBRATE_ID)
                    sinceVibrate = 0L
                }
                delay(TAP_REPEAT_MS)
                sinceVibrate += TAP_REPEAT_MS
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }
}

@Composable
actual fun rememberTapHaptic(): () -> Unit = remember {
    val generator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    generator.prepare()
    val tap: () -> Unit = { generator.impactOccurred() }
    tap
}
