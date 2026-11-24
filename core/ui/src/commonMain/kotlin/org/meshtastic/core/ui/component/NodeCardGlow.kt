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

package org.meshtastic.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.rememberUiOptions

/**
 * MT_SW: the blinking shadow of a node-list card. Off by default (the card shadow stays still); with the option
 * "blinking shadow" on, the card of our own node blinks together with the connection icon (blue when sending, green
 * or the theme colour when receiving), and the cards of other nodes blink in the colour of the theme when a packet
 * from them arrives ([lastHeard] changes).
 */
@Composable
fun Modifier.nodeCardGlow(lastHeard: Int, isThisNode: Boolean): Modifier {
    val options = rememberUiOptions()
    if (options[UiOptions.NODE_CHIP_BLINK] != 1) return this
    val cardShape = CardDefaults.shape
    if (isThisNode) return this.activityBlink(cardShape)
    val color = MaterialTheme.colorScheme.primary
    val alpha = remember { Animatable(0f) }
    val previous = remember { mutableIntStateOf(lastHeard) }
    LaunchedEffect(lastHeard) {
        if (lastHeard > 0 && lastHeard != previous.intValue) {
            alpha.snapTo(1f)
            alpha.animateTo(0f, tween(durationMillis = NODE_BLINK_MILLIS, easing = LinearEasing))
        }
        previous.intValue = lastHeard
    }
    return this.drawBehind { drawBlinkGlow(cardShape, color, alpha.value) }
}
