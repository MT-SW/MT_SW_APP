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

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

private const val WIDGET_RECEIVER = "org.meshtastic.feature.widget.MeshStatusWidgetReceiver"
private const val REREAD_MS = 2_000L

// The same heights the widget itself switches its areas at (second area, third area).
private const val SECOND_AREA_FROM_DP = 200
private const val THIRD_AREA_FROM_DP = 330

/** Reads the size of every statistics widget placed on the home screen, and keeps it current while the screen shows. */
@Composable
actual fun rememberWidgetSizes(): WidgetSizesInUse {
    val context = LocalContext.current
    var sizes by remember { mutableStateOf(readSizes(context)) }
    LaunchedEffect(context) {
        while (true) {
            sizes = readSizes(context)
            delay(REREAD_MS)
        }
    }
    return sizes
}

private fun readSizes(context: Context): WidgetSizesInUse = runCatching {
    val manager = AppWidgetManager.getInstance(context)
    val heights =
        manager.getAppWidgetIds(ComponentName(context.packageName, WIDGET_RECEIVER)).map { id ->
            val options = manager.getAppWidgetOptions(id)
            maxOf(
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
            )
        }
    WidgetSizesInUse(
        known = true,
        medium = heights.any { it < SECOND_AREA_FROM_DP },
        large = heights.any { it in SECOND_AREA_FROM_DP until THIRD_AREA_FROM_DP },
        extraLarge = heights.any { it >= THIRD_AREA_FROM_DP },
    )
}
    .getOrDefault(WidgetSizesInUse())
