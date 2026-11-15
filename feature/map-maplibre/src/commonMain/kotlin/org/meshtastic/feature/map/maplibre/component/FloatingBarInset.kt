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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import org.maplibre.compose.overlay.LocalViewportInsets
import org.meshtastic.core.ui.component.LocalFloatingBarInset

/**
 * Pushes the map's own ornaments (scale bar, logo, attribution) above the floating bar, which the map runs under: the
 * attribution has to stay readable, so it must not sit behind the bar.
 */
@Composable
internal fun WithFloatingBarInset(content: @Composable () -> Unit) {
    val current = LocalViewportInsets.current
    val direction = LocalLayoutDirection.current
    val merged =
        PaddingValues(
            start = current.calculateStartPadding(direction),
            top = current.calculateTopPadding(),
            end = current.calculateEndPadding(direction),
            bottom = current.calculateBottomPadding() + LocalFloatingBarInset.current,
        )
    CompositionLocalProvider(LocalViewportInsets provides merged, content = content)
}
