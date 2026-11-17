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
package org.meshtastic.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.isValidDeviceAddress
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.RadioPrefs
import org.meshtastic.core.repository.RadioSlot
import org.meshtastic.core.repository.RadioSlotsPrefs
import org.meshtastic.core.repository.ServiceRepository

/** What a saved radio is doing in the chooser of the top bar. */
enum class RadioStatus {
    Connected,
    Connecting,
    Disconnected,
}

/** One row of the radio chooser: a saved radio and its state. */
data class RadioChoice(
    val id: Int,
    val name: String?,
    val address: String,
    val selected: Boolean,
    val status: RadioStatus,
)

/**
 * The radio chooser of the top bar: up to [RadioSlotsPrefs.MAX_SLOTS] saved radios, the number of connected ones and
 * whether the selected radio dropped. [dropped] is true after a connected radio went away by itself, until it is
 * connected again (a radio the user disconnected or replaced, or one never connected, is not "dropped").
 */
data class RadioChooserState(
    val radios: List<RadioChoice> = emptyList(),
    val connectedCount: Int = 0,
    val dropped: Boolean = false,
    val canAdd: Boolean = true,
)

/**
 * Keeps the saved radios in step with the address the app connects to, and carries out the actions of the chooser
 * (choose, connect, disconnect, remove). Only the chosen radio is connected at a time; choosing another one switches
 * the connection (the databases of the radios stay separate, as before).
 */
@KoinViewModel
class RadioChooserViewModel(
    private val slotsPrefs: RadioSlotsPrefs,
    private val radioPrefs: RadioPrefs,
    private val radioController: RadioController,
    serviceRepository: ServiceRepository,
) : ViewModel() {

    // True after a change the user asked for (disconnect, another radio, removal): the disconnection that follows is
    // expected and is not a dropped radio. Cleared when a radio is connected again.
    private var expectedChange = false

    // True from the moment a connected radio drops by itself until it is connected again, or the list is opened.
    private val dropped = MutableStateFlow(false)

    val state: StateFlow<RadioChooserState> =
        combine(
            slotsPrefs.slots,
            slotsPrefs.selectedId,
            radioPrefs.devAddr,
            serviceRepository.connectionState,
            dropped,
        ) { slots, selectedId, address, connection, droppedByItself ->
            val activeId = slots.firstOrNull { it.address == address }?.id ?: selectedId
            val choices =
                slots.map { slot ->
                    val active = slot.id == activeId && isValidDeviceAddress(address)
                    RadioChoice(
                        id = slot.id,
                        name = slot.name,
                        address = slot.address,
                        selected = slot.id == selectedId,
                        status = if (active) connection.toStatus() else RadioStatus.Disconnected,
                    )
                }
            RadioChooserState(
                radios = choices,
                connectedCount = choices.count { it.status == RadioStatus.Connected },
                dropped = droppedByItself && isValidDeviceAddress(address),
                canAdd = slots.size < RadioSlotsPrefs.MAX_SLOTS,
            )
        }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RadioChooserState())

    init {
        // Every radio the app connects to is remembered; the one in use is the selected one. A radio that is already
        // saved only gets its name refreshed; when all places are taken, the selected radio's place is reused.
        viewModelScope.launch {
            combine(radioPrefs.devAddr, radioPrefs.devName) { address, name -> address to name }
                .distinctUntilChanged()
                .collect { (address, name) ->
                    if (!isValidDeviceAddress(address)) return@collect
                    remember(address!!, name)
                }
        }
        // A radio that was connected and then went away by itself is a dropped one. One that was never connected (the
        // app just started, the radio is off) or that the user disconnected or replaced is not: no signal for those.
        viewModelScope.launch {
            var wasConnected = false
            serviceRepository.connectionState.collect { connection ->
                when (connection) {
                    is ConnectionState.Connected -> {
                        wasConnected = true
                        expectedChange = false
                        dropped.value = false
                    }
                    is ConnectionState.Disconnected -> {
                        if (wasConnected && !expectedChange) dropped.value = true
                        wasConnected = false
                    }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun remember(address: String, name: String?) {
        val known = slotsPrefs.slots.value.firstOrNull { it.address == address }
        val id =
            if (known != null) {
                slotsPrefs.put(address, name ?: known.name, known.id)
            } else {
                slotsPrefs.put(address, name) ?: slotsPrefs.selectedId.value?.let { slotsPrefs.put(address, name, it) }
            }
        id?.let { slotsPrefs.select(it) }
    }

    /** Switches the connection to the saved radio [id]. */
    fun choose(id: Int) {
        val slot = slotsPrefs.slots.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            slotsPrefs.select(slot.id)
            if (slot.address != radioPrefs.devAddr.value) expectChange()
            radioController.setDeviceAddress(slot.address)
        }
    }

    /** Disconnects the radio in use; it stays saved. */
    fun disconnect() {
        viewModelScope.launch {
            expectChange()
            radioController.setDeviceAddress(NO_DEVICE)
        }
    }

    /** Forgets the saved radio [id]; a radio in use is disconnected first. */
    fun remove(id: Int) {
        val slot = slotsPrefs.slots.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            if (slot.address == radioPrefs.devAddr.value) {
                expectChange()
                radioController.setDeviceAddress(NO_DEVICE)
            }
            slotsPrefs.remove(slot.id)
        }
    }

    /** The user has seen the signal (opened the list): it stops until the next time a radio drops. */
    fun acknowledgeDrop() {
        dropped.value = false
    }

    private fun expectChange() {
        expectedChange = true
        dropped.value = false
    }

    private fun ConnectionState.toStatus(): RadioStatus = when (this) {
        is ConnectionState.Connected -> RadioStatus.Connected
        is ConnectionState.Connecting -> RadioStatus.Connecting
        else -> RadioStatus.Disconnected
    }

    private companion object {
        const val NO_DEVICE = "n"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
