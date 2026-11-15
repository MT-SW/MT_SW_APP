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

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import co.touchlab.kermit.Logger

private const val BUZZ_ON_MS = 60L
private const val BUZZ_OFF_MS = 15L

@Composable
actual fun rememberHoldVibration(): HoldVibration {
    val context = LocalContext.current
    return remember(context) { AndroidHoldVibration(context.applicationContext) }
}

/** Repeats a short strong pulse with a tiny pause, which feels like one continuous buzz, until stopped. */
private class AndroidHoldVibration(context: Context) : HoldVibration {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    override fun start() {
        val motor = vibrator?.takeIf { it.hasVibrator() } ?: return
        val timings = longArrayOf(0L, BUZZ_ON_MS, BUZZ_OFF_MS)
        // A phone that does not allow the vibration (or has no working motor) must never take the app down with it.
        @Suppress("TooGenericExceptionCaught")
        try {
            motor.vibrate(VibrationEffect.createWaveform(timings, 0))
        } catch (e: RuntimeException) {
            Logger.w(e) { "Hold vibration could not start" }
        }
    }

    override fun stop() {
        @Suppress("TooGenericExceptionCaught")
        try {
            vibrator?.cancel()
        } catch (e: RuntimeException) {
            Logger.w(e) { "Hold vibration could not stop" }
        }
    }
}

@Composable
actual fun rememberTapHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }
}
