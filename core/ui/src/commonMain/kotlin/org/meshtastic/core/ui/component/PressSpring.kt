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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions

private const val PRESSED_SCALE = 0.97f

/**
 * MT_SW: the element gives way a little under a finger and springs back when it is let go, as on iPhone. It does not
 * take over the touch, so the element's own click works as before. It follows the "spring on touch" option and does
 * nothing when that is off.
 */
@Composable
fun Modifier.pressSpring(): Modifier {
    if (!rememberUiOptions().flag(UiOptions.PRESS_SPRING)) return this
    var pressed by remember { mutableStateOf(false) }
    val scale by
        animateFloatAsState(
            targetValue = if (pressed) PRESSED_SCALE else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "pressSpring",
        )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            pressed = true
            do {
                val event = awaitPointerEvent(PointerEventPass.Final)
            } while (event.changes.any { it.pressed })
            pressed = false
        }
    }
}
