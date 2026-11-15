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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions

/** How long the button must be held before it starts to buzz, so a normal tap stays quiet. */
private const val HOLD_START_MS = 350L

/** The pause between two ticks of the buzz: short enough to feel like one continuous, strong vibration. */
private const val TICK_MS = 28L

/**
 * MT_SW: makes the element buzz strongly for as long as a finger is held on it (after a short hold). The buzz stops
 * when the finger is lifted, and also when [active] turns false (for example the moment a message was sent and the
 * button is no longer usable). It does not take over the touch, so the button's own click works as before. It follows
 * the "buzz on hold" option; where the device cannot vibrate (a computer) it does nothing.
 */
@Composable
fun Modifier.holdBuzz(active: Boolean = true): Modifier {
    val haptic = LocalHapticFeedback.current
    val allowed = rememberUiOptions().flag(UiOptions.HOLD_BUZZ)
    if (!active || !allowed) return this
    return pointerInput(haptic) {
        coroutineScope {
            val scope: CoroutineScope = this
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val buzz =
                    scope.launch {
                        delay(HOLD_START_MS)
                        while (isActive) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            delay(TICK_MS)
                        }
                    }
                do {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                } while (event.changes.any { it.pressed })
                buzz.cancel()
            }
        }
    }
}
