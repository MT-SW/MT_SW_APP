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

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo

/** Last good value, kept outside of composition state so a recomposition with a broken window size can reuse it. */
private class AdaptiveInfoHolder {
    var last: WindowAdaptiveInfo? = null
}

/**
 * Same as [currentWindowAdaptiveInfoV2], but safe while the window has no valid size (minimised, hidden or being
 * restored on desktop). In that moment the library throws `NoSuchElementException` (a zero or negative size matches no
 * size class) and the whole app crashes; here the last valid value is reused until the window has a size again.
 */
@Composable
fun currentWindowAdaptiveInfoSafe(): WindowAdaptiveInfo {
    val holder = remember { AdaptiveInfoHolder() }
    val size = LocalWindowInfo.current.containerSize
    val last = holder.last
    if ((size.width <= 0 || size.height <= 0) && last != null) return last
    return currentWindowAdaptiveInfoV2().also { holder.last = it }
}
