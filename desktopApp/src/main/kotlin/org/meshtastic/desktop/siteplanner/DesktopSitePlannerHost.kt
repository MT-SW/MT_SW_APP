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
package org.meshtastic.desktop.siteplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.meshtastic.feature.map.component.SitePlannerParams
import org.meshtastic.feature.map.component.SitePlannerSheet

private const val SITE_PLANNER_BASE_URL = "https://site.meshtastic.org"
private const val SITE_PLANNER_TIMEOUT_MS = 45_000L
private const val TAG = "DesktopSitePlannerHost"

/**
 * Desktop equivalent of the Android fork's `SitePlannerHost`: [SitePlannerSheet] (reused verbatim from commonMain)
 * pre-filled with [initialParams], then a JCEF-backed [SitePlannerCoverageRunner] running fully off-screen — no
 * "transparent WebView" trick needed, since OSR mode is never attached to any visible component in the first place.
 * No GPS-based current-location shortcut on desktop (no `onRequestCurrentLocation` equivalent); map-center shortcut
 * also deferred — only [onUseNodeLocation] is wired.
 *
 * NOTE (recovery script): this file's `toQueryUrl(SITE_PLANNER_BASE_URL)` call assumes `SitePlannerParams` (in
 * commonMain, feature/map) has a `toQueryUrl(baseUrl: String): String` extension/method already — that's existing
 * shared code, not something built in this session, so it should already be present in your `feature/map` module
 * untouched by this data loss.
 */
@Composable
fun DesktopSitePlannerHost(
    initialParams: SitePlannerParams,
    onDismiss: () -> Unit,
    onImport: (name: String, geoJson: String, latitude: Double, longitude: Double) -> Unit,
    onUseNodeLocation: (() -> Pair<Double, Double>)? = null,
) {
    var params by remember(initialParams) { mutableStateOf(initialParams) }
    var running by remember { mutableStateOf<SitePlannerParams?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val runner = remember { SitePlannerCoverageRunner() }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) { onDispose { runner.dispose() } }

    val current = running
    if (current == null) {
        SitePlannerSheet(
            initial = params,
            onSubmit = {
                errorMessage = null
                running = it
            },
            onDismiss = onDismiss,
            onUseNodeLocation =
            onUseNodeLocation?.let { node ->
                {
                    val (lat, lon) = node()
                    params = params.copy(latitude = lat, longitude = lon)
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = { running = null },
            title = { Text("Estimating coverage…") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { running = null }) { Text("Cancel") } },
        )

        LaunchedEffect(current) {
            val url = current.toQueryUrl(SITE_PLANNER_BASE_URL)
            withContext(Dispatchers.IO) {
                runner.start(
                    url = url,
                    onResult = { geoJson ->
                        // Fires on a native CEF callback thread — hop back to the main thread before touching
                        // Compose state or the caller's Koin-backed manager.
                        scope.launch(Dispatchers.Main) {
                            onImport(current.name, geoJson, current.latitude, current.longitude)
                            runner.dispose()
                            onDismiss()
                        }
                    },
                    onError = { detail ->
                        Logger.withTag(TAG).e { "Coverage estimate failed: $detail" }
                        scope.launch(Dispatchers.Main) { errorMessage = detail }
                    },
                )
            }
            delay(SITE_PLANNER_TIMEOUT_MS)
            if (running == current) {
                Logger.withTag(TAG).w { "Coverage estimate timed out after ${SITE_PLANNER_TIMEOUT_MS}ms" }
                errorMessage = "Timed out"
                runner.dispose()
            }
        }
    }
}
