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

/**
 * Which sizes of the statistics widget are on the home screen. When [known] is false the platform cannot tell, and
 * every option is offered.
 */
data class WidgetSizesInUse(
    val known: Boolean = false,
    val medium: Boolean = false,
    val large: Boolean = false,
    val extraLarge: Boolean = false,
)

/** The sizes of the widget the user has placed, kept current while the screen is shown. */
@Composable expect fun rememberWidgetSizes(): WidgetSizesInUse
