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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import co.touchlab.kermit.Logger

/**
 * MT_SW: every window the system opens on top of the app (dialog, pop-up, menu) is a window of its own, and its
 * composition starts again from the screen's own density, so the in-app scale (text and elements) would be lost there.
 * The windows below take the density of the place that opens them and give it back to their content, so the scale set
 * in "Wygląd" holds in every window, on every platform.
 */
@Composable
fun ProvideAppDensity(density: Density, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDensity provides density, content = content)
}

/** The same as the Compose `Dialog`, but its content keeps the in-app scale. */
@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val appDensity = LocalDensity.current
    LaunchedEffect(Unit) { logWindowScale("okno dialogowe", appDensity) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        ProvideAppDensity(appDensity, content)
    }
}

/** The same as the Compose `Popup` (aligned form), but its content keeps the in-app scale. */
@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    val appDensity = LocalDensity.current
    androidx.compose.ui.window.Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        ProvideAppDensity(appDensity, content)
    }
}

/** Writes once, to the log, which scale a window got (for diagnosing a window that looks too big or too small). */
internal fun logWindowScale(name: String, density: Density) {
    Logger.d { "Okno $name: skala ${density.density} / tekst ${density.fontScale}" }
}

/**
 * The Material tooltip box with the in-app interface scale kept in the tooltip. The tooltip opens in its own window,
 * which would otherwise fall back to the system scale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val appDensity = LocalDensity.current
    androidx.compose.material3.TooltipBox(
        positionProvider = positionProvider,
        tooltip = {
            val scope = this
            ProvideAppDensity(appDensity) { scope.tooltip() }
        },
        state = state,
        modifier = modifier,
        content = content,
    )
}
