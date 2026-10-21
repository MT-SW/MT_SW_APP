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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

private const val SCAN_WAIT_MILLIS = 3_000L
private const val BAND_24_MIN_MHZ = 2400
private const val BAND_24_MAX_MHZ = 2500

@Composable
actual fun rememberPhoneWifiScanner(): PhoneWifiScanner? {
    val context = LocalContext.current
    val pending = remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pending.value?.complete(granted)
            pending.value = null
        }
    return remember(context, launcher) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifi == null) {
            null
        } else {
            AndroidPhoneWifiScanner(context, wifi) {
                val answer = CompletableDeferred<Boolean>()
                pending.value = answer
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                answer.await()
            }
        }
    }
}

private class AndroidPhoneWifiScanner(
    private val context: Context,
    private val wifi: WifiManager,
    private val askPermission: suspend () -> Boolean,
) : PhoneWifiScanner {

    // Android shows network names only to apps that may see the location.
    private suspend fun allowed(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            askPermission()

    @Suppress("DEPRECATION")
    override suspend fun currentSsid(): String? {
        if (!allowed()) return null
        val raw = runCatching { wifi.connectionInfo?.ssid }.getOrNull() ?: return null
        return raw.removeSurrounding("\"").takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    }

    @Suppress("DEPRECATION")
    override suspend fun scan(): List<PhoneWifiNetwork> {
        if (!allowed()) return emptyList()
        runCatching { wifi.startScan() }
        delay(SCAN_WAIT_MILLIS)
        val results = runCatching { wifi.scanResults }.getOrNull().orEmpty()
        return results
            .map { PhoneWifiNetwork(it.SSID.orEmpty(), it.level, it.frequency in BAND_24_MIN_MHZ..BAND_24_MAX_MHZ) }
            .tidy()
    }
}
