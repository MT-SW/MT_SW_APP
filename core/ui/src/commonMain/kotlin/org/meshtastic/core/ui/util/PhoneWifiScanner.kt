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
 * One Wi-Fi network seen by this phone or computer. [is24Ghz] is null when the band is not known, [secured] is null
 * when it is not known whether the network needs a password.
 */
data class PhoneWifiNetwork(
    val ssid: String,
    val rssi: Int?,
    val is24Ghz: Boolean? = null,
    val secured: Boolean? = null,
)

/**
 * Looks at the Wi-Fi networks around this phone or computer, so the radio's network name can be picked from a list
 * instead of typed. It never reads saved passwords: the password is always typed by hand or scanned from a QR code.
 */
interface PhoneWifiScanner {
    /** The network this phone or computer is on right now, or null when unknown. */
    suspend fun currentSsid(): String?

    /** Networks in range, strongest first, one entry per name. Empty when scanning is not possible. */
    suspend fun scan(): List<PhoneWifiNetwork>
}

/** The scanner of this platform, or null where scanning is not available. */
@Composable expect fun rememberPhoneWifiScanner(): PhoneWifiScanner?

/** Keeps one entry per name (the strongest), drops empty names, strongest first. */
fun List<PhoneWifiNetwork>.tidy(): List<PhoneWifiNetwork> = filter { it.ssid.isNotBlank() }
    .groupBy { it.ssid }
    .map { (_, group) -> group.maxByOrNull { it.rssi ?: Int.MIN_VALUE }!! }
    .sortedByDescending { it.rssi ?: Int.MIN_VALUE }
