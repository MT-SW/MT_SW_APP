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
@file:Suppress("CompositionLocalAllowlist", "MatchingDeclarationName")

package org.meshtastic.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.meshtastic.core.model.UiOptions

/** The current "look of the elements" options; kept in step with the saved settings by the main view model. */
object AppUiOptions {
    val state = MutableStateFlow(UiOptions())
}

/**
 * The options handed down once from the top of the app ([AppTheme]), so the hundreds of icons, cards and buttons on a
 * screen read one value instead of each listening to the flow on its own (which made scrolling heavy).
 */
val LocalUiOptions = staticCompositionLocalOf<UiOptions?> { null }

/** The current options as Compose state, so the elements redraw when a choice changes. */
@Composable
fun rememberUiOptions(): UiOptions {
    LocalUiOptions.current?.let {
        return it
    }
    val options by AppUiOptions.state.collectAsState()
    return options
}
