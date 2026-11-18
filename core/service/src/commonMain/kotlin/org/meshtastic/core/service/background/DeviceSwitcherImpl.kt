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
package org.meshtastic.core.service.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.DeviceSwitcher
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.RadioPrefs
import org.meshtastic.core.repository.RadioSlotsPrefs

@Single
class DeviceSwitcherImpl(
    private val slotsPrefs: RadioSlotsPrefs,
    private val radioPrefs: RadioPrefs,
    private val radioController: RadioController,
    dispatchers: CoroutineDispatchers,
) : DeviceSwitcher {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override fun switchTo(address: String): Boolean {
        val slot = slotsPrefs.slots.value.firstOrNull { it.address == address } ?: return false
        if (address == radioPrefs.devAddr.value) return true
        scope.launch {
            slotsPrefs.setOff(slot.id, false)
            slotsPrefs.select(slot.id)
            radioController.setDeviceAddress(slot.address)
        }
        return true
    }
}
