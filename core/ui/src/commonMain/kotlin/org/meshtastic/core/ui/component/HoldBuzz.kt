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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions
import org.meshtastic.core.ui.util.rememberHoldVibration

/** How long the button must be held before it starts to buzz, so a normal tap stays quiet. */
private const val HOLD_START_MS = 350L

/**
 * MT_SW: makes the device vibrate strongly and continuously for as long as a finger is held on the element (after a
 * short hold). The vibration stops when the finger is lifted, and also when [active] turns false (for example the
 * moment a message was sent and the button is no longer usable). It does not take over the touch, so the button's
 * own click works as before. It follows the "buzz on hold" option and uses the vibration motor of the platform (see
 * [rememberHoldVibration]); a computer has none, so there it does nothing.
 */
@Composable
fun Modifier.holdBuzz(active: Boolean = true): Modifier {
    val vibration = rememberHoldVibration()
    val allowed = rememberUiOptions().flag(UiOptions.HOLD_BUZZ)
    if (!active || !allowed) return this
    return pointerInput(vibration) {
        coroutineScope {
            val scope: CoroutineScope = this
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val buzz =
                    scope.launch {
                        delay(HOLD_START_MS)
                        vibration.start()
                    }
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                    } while (event.changes.any { it.pressed })
                } finally {
                    buzz.cancel()
                    vibration.stop()
                }
            }
        }
    }
}
