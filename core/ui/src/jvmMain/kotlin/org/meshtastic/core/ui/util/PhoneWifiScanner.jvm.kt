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
import androidx.compose.runtime.remember
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val COMMAND_TIMEOUT_SECONDS = 20L
private const val BAND_24_MAX_MHZ = 2500
private const val MAC_AIRPORT =
    "/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport"

@Composable actual fun rememberPhoneWifiScanner(): PhoneWifiScanner? = remember { DesktopPhoneWifiScanner() }

/** Best effort: uses the tools each system already has. Anything that fails simply gives an empty list. */
private class DesktopPhoneWifiScanner : PhoneWifiScanner {
    private val os = System.getProperty("os.name").orEmpty().lowercase()

    private fun run(vararg command: String): String? = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) process.destroyForcibly()
        text
    }
        .getOrNull()

    override suspend fun currentSsid(): String? = withContext(Dispatchers.IO) {
        runCatching {
            when {
                "win" in os ->
                    run("netsh", "wlan", "show", "interfaces")
                        ?.lines()
                        ?.firstNotNullOfOrNull { Regex("^\\s*SSID\\s*:\\s*(.+)$").find(it)?.groupValues?.get(1)?.trim() }

                "mac" in os && File(MAC_AIRPORT).exists() ->
                    run(MAC_AIRPORT, "-I")
                        ?.lines()
                        ?.firstNotNullOfOrNull { Regex("^\\s*SSID:\\s*(.+)$").find(it)?.groupValues?.get(1)?.trim() }

                "mac" in os ->
                    run("networksetup", "-getairportnetwork", "en0")
                        ?.substringAfter("Network:", "")
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }

                else ->
                    run("nmcli", "-t", "-f", "ACTIVE,SSID", "dev", "wifi")
                        ?.lines()
                        ?.firstOrNull { it.startsWith("yes:") }
                        ?.removePrefix("yes:")
                        ?.trim()
            }
        }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    override suspend fun scan(): List<PhoneWifiNetwork> = withContext(Dispatchers.IO) {
        runCatching {
            when {
                "win" in os -> parseWindows(run("netsh", "wlan", "show", "networks", "mode=bssid").orEmpty())
                "mac" in os && File(MAC_AIRPORT).exists() -> parseMac(run(MAC_AIRPORT, "-s").orEmpty())
                "mac" in os -> emptyList()
                else -> parseLinux(run("nmcli", "-t", "-f", "SSID,SIGNAL,FREQ", "dev", "wifi", "list").orEmpty())
            }
        }
            .getOrDefault(emptyList())
            .tidy()
    }

    // "SSID 1 : name" ... "Signal : 87%"; the band is not read, since the words are translated.
    private fun parseWindows(text: String): List<PhoneWifiNetwork> {
        val result = mutableListOf<PhoneWifiNetwork>()
        var name: String? = null
        var signal: Int? = null
        fun flush() {
            name?.let { result += PhoneWifiNetwork(it, signal?.let { s -> s / 2 - 100 }, null) }
            name = null
            signal = null
        }
        text.lines().forEach { line ->
            val ssid = Regex("^\\s*SSID\\s+\\d+\\s*:\\s*(.*)$").find(line)
            if (ssid != null) {
                flush()
                name = ssid.groupValues[1].trim()
            } else if (name != null && signal == null) {
                Regex("^\\s*[^:\\d]+:\\s*(\\d{1,3})%\\s*$").find(line)?.let { signal = it.groupValues[1].toInt() }
            }
        }
        flush()
        return result
    }

    // Columns: SSID BSSID RSSI CHANNEL ...; the name may contain spaces, so anchor on the MAC address.
    private fun parseMac(text: String): List<PhoneWifiNetwork> = text.lines().mapNotNull { line ->
        val match = Regex("^\\s*(.+?)\\s+([0-9a-fA-F]{2}(?::[0-9a-fA-F]{2}){5})\\s+(-?\\d+)\\s+(\\d+)").find(line)
        match?.let {
            val channel = it.groupValues[4].toInt()
            PhoneWifiNetwork(it.groupValues[1], it.groupValues[3].toInt(), channel in 1..14)
        }
    }

    // "name:signal:2412 MHz" with ':' inside the name written as '\:'.
    private fun parseLinux(text: String): List<PhoneWifiNetwork> = text.lines().mapNotNull { line ->
        val parts = line.replace("\\:", "\u0000").split(':')
        if (parts.size < 3) return@mapNotNull null
        val freq = parts.last().filter { it.isDigit() }.toIntOrNull()
        val signal = parts[parts.size - 2].toIntOrNull()
        val ssid = parts.dropLast(2).joinToString(":").replace("\u0000", ":")
        PhoneWifiNetwork(ssid, signal?.let { it / 2 - 100 }, freq?.let { it < BAND_24_MAX_MHZ })
    }
}
