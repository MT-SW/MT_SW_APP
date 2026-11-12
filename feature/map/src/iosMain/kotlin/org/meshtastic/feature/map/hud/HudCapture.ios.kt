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
package org.meshtastic.feature.map.hud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.device_model_capture_failed
import org.meshtastic.core.resources.device_model_capture_saved
import org.meshtastic.core.ui.util.rememberShowToast

/** Saves the camera picture with the overlay to the Photos app, like the capture button on Android. */
@Composable
actual fun rememberHudCapture(still: HudStill?, overlay: GraphicsLayer): (() -> Unit)? {
    if (still == null) return null
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val showToast = rememberShowToast()
    val saved = stringResource(Res.string.device_model_capture_saved)
    val failed = stringResource(Res.string.device_model_capture_failed)
    // One still at a time: the overlay effect draws a single recorded overlay.
    var capturing by remember { mutableStateOf(false) }
    return {
        if (!capturing) {
            capturing = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                @Suppress("TooGenericExceptionCaught")
                val ok =
                    try {
                        still.take(overlay.toImageBitmap())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.w(e) { "Mesh view overlay could not be recorded" }
                        false
                    } finally {
                        capturing = false
                    }
                showToast(if (ok) saved else failed)
            }
        }
    }
}
