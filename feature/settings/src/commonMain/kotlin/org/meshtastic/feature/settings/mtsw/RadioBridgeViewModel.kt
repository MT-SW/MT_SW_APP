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
import org.meshtastic.core.repository.RadioBackgroundMonitor
import org.meshtastic.core.repository.RadioBridgeConfig
import org.meshtastic.core.repository.RadioBridgeMonitor
import org.meshtastic.core.repository.RadioBridgePrefs
import org.meshtastic.core.repository.RadioSlot
import org.meshtastic.core.repository.RadioSlotsPrefs
import org.meshtastic.core.repository.badgeFor

/** The bridge settings of the radio chosen in the top bar. [slotId] is null while no radio is saved. */
data class RadioBridgeState(
    val slotId: Int? = null,
    val name: String? = null,
    val config: RadioBridgeConfig = RadioBridgeConfig(),
    val hasSavedDevices: Boolean = false,
)

/**
 * The bridge settings of one saved device. [name] is null while the device is not connected (its name is not shown).
 */
data class RadioBridgeDevice(val slotId: Int, val name: String?, val nodeNum: Int?, val config: RadioBridgeConfig)

/** The settings of the TCP bridges (see [RadioBridgeConfig]): one set per saved device, and the chosen device. */
@KoinViewModel
class RadioBridgeViewModel(
    slotsPrefs: RadioSlotsPrefs,
    private val bridgePrefs: RadioBridgePrefs,
    background: RadioBackgroundMonitor,
    private val bridge: RadioBridgeMonitor,
) : ViewModel() {
    private var savedSlots: List<RadioSlot> = emptyList()

    val devices: StateFlow<List<RadioBridgeDevice>> =
        combine(slotsPrefs.slots, bridgePrefs.configs, background.connectedAddresses, background.badges) {
                slots,
                configs,
                connected,
                badges,
            ->
            savedSlots = slots
            slots.map { slot ->
                // Only the short name of the device's node is shown.
                val name = badges.badgeFor(slot.address)?.shortName?.takeIf { it.isNotBlank() }
                RadioBridgeDevice(
                    slotId = slot.id,
                    name = name.takeIf { slot.address in connected },
                    nodeNum = badges.badgeFor(slot.address)?.nodeNum?.takeIf { slot.address in connected },
                    config = configs[slot.id] ?: RadioBridgeConfig(),
                )
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val state: StateFlow<RadioBridgeState> =
        combine(slotsPrefs.slots, slotsPrefs.selectedId, bridgePrefs.configs) { slots, selected, configs ->
            // With one saved device there is nothing to choose: it is the one shown.
            val slot = slots.firstOrNull { it.id == selected } ?: slots.singleOrNull()
            val name = slot?.name?.takeIf { it.isNotBlank() } ?: slot?.address
            RadioBridgeState(
                slot?.id,
                name,
                configs[slot?.id] ?: RadioBridgeConfig(),
                hasSavedDevices = slots.isNotEmpty(),
            )
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RadioBridgeState())

    /** Changes the bridge settings of the device with the saved id [slotId]. */
    fun update(slotId: Int, change: (RadioBridgeConfig) -> RadioBridgeConfig) {
        val current = devices.value.firstOrNull { it.slotId == slotId }?.config ?: RadioBridgeConfig()
        bridgePrefs.setConfig(slotId, change(current))
    }

    /** Asks the device with the saved id [slotId] for its configuration again (the copy for clients is renewed). */
    fun refreshConfiguration(slotId: Int) {
        savedSlots.firstOrNull { it.id == slotId }?.let { bridge.refreshConfiguration(it.address) }
    }

    /** Changes the bridge settings of the device shown in the app (used by the console). */
    fun update(change: (RadioBridgeConfig) -> RadioBridgeConfig) {
        val current = state.value
        val id = current.slotId ?: return
        bridgePrefs.setConfig(id, change(current.config))
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
