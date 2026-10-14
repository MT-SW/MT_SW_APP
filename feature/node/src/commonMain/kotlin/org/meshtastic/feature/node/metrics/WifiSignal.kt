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
package org.meshtastic.feature.node.metrics

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.util.LocalStatsExtended
import org.meshtastic.core.model.util.decodeLocalStatsExtended
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.wifi_reason_auth_expired
import org.meshtastic.core.resources.wifi_reason_handshake
import org.meshtastic.core.resources.wifi_reason_no_network
import org.meshtastic.core.resources.wifi_reason_other
import org.meshtastic.core.resources.wifi_reason_self
import org.meshtastic.core.resources.wifi_reason_signal_lost
import org.meshtastic.proto.Telemetry

/** One sample of the device's own WiFi link (fw+ extended stats), taken at [time] (seconds). */
internal data class WifiSample(val time: Int, val stats: LocalStatsExtended) {
    /** The device reports a WiFi signal only while it is connected. */
    val isConnected: Boolean
        get() = stats.wifiRssi != 0
}

// ESP-IDF disconnect reasons the firmware most often reports at the edge of coverage.
private const val REASON_AUTH_EXPIRE = 2
private const val REASON_DISCONNECTED_BY_DEVICE = 8
private const val REASON_HANDSHAKE_TIMEOUT = 15
private const val REASON_BEACON_TIMEOUT = 200
private const val REASON_NO_AP_FOUND = 201
private const val REASON_HANDSHAKE_FAILED = 204

/** Plain-language name of an ESP-IDF WiFi disconnect [code], with the code itself kept in brackets. */
@Composable
internal fun wifiDropReasonText(code: Int): String = when (code) {
    REASON_BEACON_TIMEOUT -> stringResource(Res.string.wifi_reason_signal_lost, code)
    REASON_NO_AP_FOUND -> stringResource(Res.string.wifi_reason_no_network, code)
    REASON_HANDSHAKE_TIMEOUT,
    REASON_HANDSHAKE_FAILED,
    -> stringResource(Res.string.wifi_reason_handshake, code)
    REASON_AUTH_EXPIRE -> stringResource(Res.string.wifi_reason_auth_expired, code)
    REASON_DISCONNECTED_BY_DEVICE -> stringResource(Res.string.wifi_reason_self, code)
    else -> stringResource(Res.string.wifi_reason_other, code)
}

/** Keeps only the telemetry that carries WiFi data, decoded once, oldest to newest as given. */
internal fun List<Telemetry>.toWifiSamples(sinceSeconds: Long): List<WifiSample> = mapNotNull { t ->
    if (t.time.toLong() < sinceSeconds) return@mapNotNull null
    t.decodeLocalStatsExtended()?.takeIf { it.hasWifi }?.let { WifiSample(t.time, it) }
}
