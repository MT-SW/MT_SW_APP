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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL

private const val WIDGET_APP_GROUP = "group.pl.sw.mt"
private const val SIZES_FILE = "widget-sizes.txt"
private const val REREAD_MS = 2_000L

/** The app writes the sizes in use into the shared folder every time it comes to the front; this reads them. */
@Composable
actual fun rememberWidgetSizes(): WidgetSizesInUse {
    var sizes by remember { mutableStateOf(readSizes()) }
    LaunchedEffect(Unit) {
        while (true) {
            // The file or the system is asked off the main thread; the screen only gets the answer.
            sizes = withContext(Dispatchers.Default) { readSizes() }
            delay(REREAD_MS)
        }
    }
    return sizes
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun readSizes(): WidgetSizesInUse {
    val folder = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(WIDGET_APP_GROUP)
    val url = folder?.URLByAppendingPathComponent(SIZES_FILE)
    val names =
        url?.let { NSString.stringWithContentsOfURL(it, encoding = NSUTF8StringEncoding, error = null) }
            ?.split(",")
            .orEmpty()
    return if (names.firstOrNull() == "ok") {
        WidgetSizesInUse(
            known = true,
            medium = "medium" in names,
            large = "large" in names,
            extraLarge = "extra" in names,
        )
    } else {
        WidgetSizesInUse()
    }
}
