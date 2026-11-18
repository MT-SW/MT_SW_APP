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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.isValidDeviceAddress
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioBackgroundMonitor
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.RadioPrefs
import org.meshtastic.core.repository.RadioSlotsPrefs
import org.meshtastic.core.repository.ServiceRepository

/** What a saved radio is doing in the chooser of the top bar. */
enum class RadioStatus {
    Connected,
    Connecting,
    Disconnected,
}

/**
 * One saved radio and its state. [active] is the radio the screens show (the one in use); a radio that is connected but
 * not [active] is kept connected in the background. [off] is true after the user disconnected it.
 */
data class RadioChoice(
    val id: Int,
    val name: String?,
    val address: String,
    val selected: Boolean,
    val status: RadioStatus,
    val active: Boolean = false,
    val off: Boolean = false,
    /** Short name and node number of the device, for its coloured plate (null while not known yet). */
    val shortName: String? = null,
    val nodeNum: Int? = null,
    /** New messages that reached this device in the background since it was last shown. */
    val unread: Int = 0,
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
    /** New messages on the devices that are not shown; the circle near the icon of the top bar. */
    val unreadTotal: Int = 0,
)

/**
 * Keeps the saved radios in step with the address the app connects to, and carries out the actions of the chooser and
 * of the connection card (choose, connect, disconnect, remove). One radio is in use (the screens show it); the other
 * saved radios stay connected in the background while that is allowed, so choosing another radio does not disconnect
 * the first one. Their databases stay separate.
 */
@KoinViewModel
class RadioChooserViewModel(
    private val slotsPrefs: RadioSlotsPrefs,
    private val radioPrefs: RadioPrefs,
    private val radioController: RadioController,
    serviceRepository: ServiceRepository,
    private val background: RadioBackgroundMonitor,
    nodeRepository: NodeRepository,
) : ViewModel() {

    // True after a change the user asked for (disconnect, another radio, removal): the disconnection that follows is
    // expected and is not a dropped radio. Cleared when a radio is connected again.
    private var expectedChange = false

    // True from the moment a connected radio drops by itself until it is connected again, or the list is opened.
    private val dropped = MutableStateFlow(false)

    // The slot that the next device picked from the list is saved in (null: a free one). Used once.
    private var targetSlot: Int? = null

    private val plates =
        combine(background.badges, background.unreadByAddress, nodeRepository.ourNodeInfo) { badges, unread, ours ->
            Triple(badges, unread, ours)
        }

    val state: StateFlow<RadioChooserState> =
        combine(
            combine(slotsPrefs.slots, slotsPrefs.selectedId, slotsPrefs.offIds) { slots, selectedId, off ->
                Triple(slots, selectedId, off)
            },
            radioPrefs.devAddr,
            serviceRepository.connectionState,
            background.connectedAddresses,
            combine(dropped, plates) { d, p -> d to p },
        ) { (slots, selectedId, off), address, connection, quiet, (droppedByItself, plate) ->
            val (badges, unread, ours) = plate
            val activeId = slots.firstOrNull { it.address == address }?.id ?: selectedId
            val choices = slots.map { slot ->
                val active = slot.id == activeId && isValidDeviceAddress(address)
                RadioChoice(
                    id = slot.id,
                    name = slot.name,
                    address = slot.address,
                    selected = slot.id == selectedId,
                    status =
                    when {
                        active -> connection.toStatus()
                        slot.address in quiet -> RadioStatus.Connected
                        else -> RadioStatus.Disconnected
                    },
                    active = active,
                    off = slot.id in off,
                    shortName =
                    if (active && ours != null) ours.user.short_name else badges[slot.address]?.shortName,
                    nodeNum = if (active && ours != null) ours.num else badges[slot.address]?.nodeNum,
                    unread = if (active) 0 else unread[slot.address] ?: 0,
                )
            }
            RadioChooserState(
                radios = choices,
                connectedCount = choices.count { it.status == RadioStatus.Connected },
                dropped = droppedByItself && isValidDeviceAddress(address),
                canAdd = slots.size < RadioSlotsPrefs.MAX_SLOTS,
                unreadTotal = choices.sumOf { it.unread },
            )
        }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RadioChooserState())

    init {
        // Every radio the app connects to is remembered; the one in use is the selected one. A radio that is already
        // saved only gets its name refreshed; when all places are taken, the selected radio's place is reused.
        // Devices saved earlier with the same name (the old switch wrote the name of the device shown before): a
        // network device gets its address back as the name, any other duplicate loses the wrong name.
        viewModelScope.launch {
            val slots = slotsPrefs.slots.first()
            val seen = mutableSetOf<String>()
            for (slot in slots) {
                val name = slot.name?.takeIf { it.isNotBlank() }
                if (name != null && !seen.add(name)) {
                    val fixed = if (slot.address.startsWith(NETWORK_PREFIX)) slot.address.drop(1) else null
                    slotsPrefs.put(slot.address, fixed, slot.id)
                }
            }
        }
        viewModelScope.launch {
            var lastAddress: String? = null
            combine(radioPrefs.devAddr, radioPrefs.devName) { address, name -> address to name }
                .distinctUntilChanged()
                .collect { (address, name) ->
                    // The name is written just before the address, so a change of the name alone still belongs to
                    // the radio that was in use: it must not rename that radio.
                    val changed = address != lastAddress
                    lastAddress = address
                    if (changed && address != null && isValidDeviceAddress(address)) {
                        // The device is shown now: its count of new messages starts again.
                        background.clearUnread(address)
                        remember(address, name)
                    }
                }
        }
        // A radio that was connected and then went away by itself is a dropped one. One that was never connected (the
        // app just started, the radio is off) or that the user disconnected or replaced is not: no signal for those.
        viewModelScope.launch {
            var wasConnected = false
            var connectedAddress: String? = null
            serviceRepository.connectionState.collect { connection ->
                when (connection) {
                    is ConnectionState.Connected -> {
                        wasConnected = true
                        connectedAddress = radioPrefs.devAddr.value
                        expectedChange = false
                        dropped.value = false
                    }

                    is ConnectionState.Disconnected -> {
                        // A switch to another device (also from a notification) is not a drop.
                        val switched = radioPrefs.devAddr.value != connectedAddress
                        if (wasConnected && !expectedChange && !switched) dropped.value = true
                        wasConnected = false
                    }

                    else -> Unit
                }
            }
        }
    }

    private suspend fun remember(address: String, name: String?) {
        val known = slotsPrefs.slots.value.firstOrNull { it.address == address }
        val target = targetSlot
        targetSlot = null
        val id =
            when {
                // The same radio is never saved twice, whatever place it was picked for.
                // A saved device keeps its name: when the app only switches to it, the stored name is still the one of
                // the device that was shown before.
                known != null -> slotsPrefs.put(address, known.name?.takeIf { it.isNotBlank() } ?: name, known.id)

                target != null -> slotsPrefs.put(address, name, target)

                else ->
                    slotsPrefs.put(address, name)
                        ?: slotsPrefs.selectedId.value?.let { slotsPrefs.put(address, name, it) }
            }
        id?.let {
            slotsPrefs.setOff(it, false)
            slotsPrefs.select(it)
        }
    }

    /**
     * Says which place the next radio picked from the list goes to: [id] when that place is free or all places are
     * taken, otherwise a free place. Called by the connection card just before a device is picked.
     */
    fun targetForNextPick(id: Int) {
        val slots = slotsPrefs.slots.value
        val taken = slots.any { it.id == id }
        targetSlot = if (!taken || slots.size >= RadioSlotsPrefs.MAX_SLOTS) id else null
    }

    /** Makes the saved radio [id] the one in use. The radio that was in use stays connected in the background. */
    fun choose(id: Int) {
        val slot = slotsPrefs.slots.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            slotsPrefs.setOff(slot.id, false)
            slotsPrefs.select(slot.id)
            if (slot.address != radioPrefs.devAddr.value) expectChange()
            radioController.setDeviceAddress(slot.address)
        }
    }

    /** Disconnects the saved radio [id] (in use or in the background); it stays saved and stays off until connected. */
    fun disconnect(id: Int) {
        val slot = slotsPrefs.slots.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            slotsPrefs.setOff(slot.id, true)
            if (slot.address == radioPrefs.devAddr.value) {
                expectChange()
                radioPrefs.setDevName(null)
                radioController.setDeviceAddress(NO_DEVICE)
            }
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
        const val NETWORK_PREFIX = "t"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
