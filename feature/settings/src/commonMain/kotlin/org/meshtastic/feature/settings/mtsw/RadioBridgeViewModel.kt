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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.repository.RadioBridgeConfig
import org.meshtastic.core.repository.RadioBridgePrefs
import org.meshtastic.core.repository.RadioSlotsPrefs

/** The bridge settings of the radio chosen in the top bar. [slotId] is null while no radio is saved. */
data class RadioBridgeState(
    val slotId: Int? = null,
    val name: String? = null,
    val config: RadioBridgeConfig = RadioBridgeConfig(),
)

/** The settings of the TCP bridge (see [RadioBridgeConfig]) for the radio that is chosen now. */
@KoinViewModel
class RadioBridgeViewModel(slotsPrefs: RadioSlotsPrefs, private val bridgePrefs: RadioBridgePrefs) : ViewModel() {
    val state: StateFlow<RadioBridgeState> =
        combine(slotsPrefs.slots, slotsPrefs.selectedId, bridgePrefs.configs) { slots, selected, configs ->
            val slot = slots.firstOrNull { it.id == selected }
            val name = slot?.name?.takeIf { it.isNotBlank() } ?: slot?.address
            RadioBridgeState(slot?.id, name, configs[slot?.id] ?: RadioBridgeConfig())
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RadioBridgeState())

    fun update(change: (RadioBridgeConfig) -> RadioBridgeConfig) {
        val current = state.value
        val id = current.slotId ?: return
        bridgePrefs.setConfig(id, change(current.config))
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
