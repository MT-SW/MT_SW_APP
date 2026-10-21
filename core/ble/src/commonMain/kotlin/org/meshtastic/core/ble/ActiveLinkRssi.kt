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
package org.meshtastic.core.ble

import com.juul.kable.ExperimentalKableApi

/**
 * Signal strength (dBm) of the Bluetooth link to the radio that is connected right now, or null when there is no such
 * link or the platform cannot read it. Only read on demand; nothing is stored.
 */
@OptIn(ExperimentalKableApi::class)
suspend fun readActiveBleRssi(): Int? =
    runCatching { ActiveBleConnection.active?.peripheral?.rssi() }.getOrNull()?.takeIf { it < 0 }
