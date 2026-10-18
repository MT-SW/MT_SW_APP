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
package org.meshtastic.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Ready-made in-app scales in percent; 0 means "follow the phone's own size settings". */
val UI_SCALE_OPTIONS = listOf(0, 50, 60, 70, 80, 90, 100, 110, 125, 150, 175, 200)

/**
 * The density that applies the in-app scale (text and elements together), independent of the phone's font and display
 * size.
 *
 * With [percent] = 0 the current density is returned unchanged. Otherwise 100% is the phone's default size (when
 * [baseDensity] is known, i.e. the density before the user enlarged "display size") and every other value scales from
 * it.
 */
@Composable
fun rememberScaledDensity(percent: Int, baseDensity: Float? = null): Density {
    val current = LocalDensity.current
    return remember(percent, baseDensity, current) {
        if (percent <= 0) {
            current
        } else {
            Density(density = (baseDensity ?: current.density) * percent / PERCENT, fontScale = 1f)
        }
    }
}

private const val PERCENT = 100f
