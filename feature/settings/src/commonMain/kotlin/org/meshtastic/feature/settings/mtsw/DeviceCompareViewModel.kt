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
package org.meshtastic.feature.settings.mtsw

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.repository.DeviceSnapshot
import org.meshtastic.core.repository.RadioBackgroundMonitor
import org.meshtastic.core.repository.RadioSlotsPrefs

/** One saved device in the side-by-side comparison (MT_SW). */
data class DeviceCompareRow(
    val slotId: Int,
    val title: String,
    val connected: Boolean,
    val unread: Int,
    val snapshot: DeviceSnapshot?,
)

/** Reads the last reported state of every saved device, to show them side by side. */
@KoinViewModel
class DeviceCompareViewModel(
    private val slotsPrefs: RadioSlotsPrefs,
    private val background: RadioBackgroundMonitor,
) : ViewModel() {
    private val _rows = MutableStateFlow<List<DeviceCompareRow>>(emptyList())
    val rows: StateFlow<List<DeviceCompareRow>> = _rows.asStateFlow()

    /** Reads every saved device again from its own database. */
    fun refresh() {
        viewModelScope.launch {
            val slots = slotsPrefs.slots.value
            val connected = background.connectedAddresses.value
            val unread = background.unreadByAddress.value
            Logger.i { "Porównanie urządzeń: odczyt ${slots.size} urządzeń" }
            _rows.value =
                slots.map { slot ->
                    val snapshot = background.readSnapshot(slot.address)
                    DeviceCompareRow(
                        slotId = slot.id,
                        title = snapshot?.shortName?.takeIf { it.isNotBlank() } ?: slot.name ?: slot.address,
                        connected = slot.address in connected,
                        unread = unread[slot.address] ?: 0,
                        snapshot = snapshot,
                    )
                }
        }
    }
}
