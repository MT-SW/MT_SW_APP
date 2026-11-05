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
@file:Suppress("EmptyFunctionBlock")

package org.meshtastic.core.ble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

/**
 * iPhone Bluetooth state. The system asks the user for Bluetooth permission by itself at the first scan or connection
 * and pairs (with the PIN prompt) during the connection, so the app only reports "ready" here.
 */
@Single
class IosBluetoothRepository : BluetoothRepository {
    private val _state = MutableStateFlow(BluetoothState(hasPermissions = true, enabled = true))
    override val state: StateFlow<BluetoothState> = _state

    override fun refreshState() {}

    override fun isValid(bleAddress: String): Boolean = bleAddress.isNotEmpty()

    override fun isBonded(address: String): Boolean = false

    override suspend fun bond(device: BleDevice, pin: String?) {}
}
