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
package org.meshtastic.core.prefs.radio

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.prefs.di.RadioDataStore
import org.meshtastic.core.repository.RadioSlot
import org.meshtastic.core.repository.RadioSlotsPrefs

/**
 * Saved radios of the connection center. They are not part of the settings backup: an address is tied to the device
 * that reached the radio (a Bluetooth address on one phone means nothing on another), so a restored list would point
 * nowhere. The radio saved before this existed is read as radio 1 until the list is first written.
 */
@Single
class RadioSlotsPrefsImpl(private val dataStore: RadioDataStore, dispatchers: CoroutineDispatchers) :
    RadioSlotsPrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private val state: StateFlow<Pair<List<RadioSlot>, Int?>> =
        dataStore.data
            .map { prefs ->
                val slots = current(prefs)
                val selected = prefs[KEY_SELECTED]?.takeIf { id -> slots.any { it.id == id } }
                slots to (selected ?: slots.firstOrNull()?.id)
            }
            .stateIn(scope, SharingStarted.Eagerly, emptyList<RadioSlot>() to null)

    override val slots: StateFlow<List<RadioSlot>> =
        state.map { it.first }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val selectedId: StateFlow<Int?> = state.map { it.second }.stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun put(address: String, name: String?, id: Int?): Int? {
        var used: Int? = null
        dataStore.edit { prefs ->
            val current = current(prefs)
            val target = id ?: current.firstOrNull { it.address == address }?.id ?: freeId(current)
            if (target == null || target !in 1..RadioSlotsPrefs.MAX_SLOTS) return@edit
            val next = (current.filterNot { it.id == target } + RadioSlot(target, address, name)).sortedBy { it.id }
            prefs[KEY_SLOTS] = RadioSlotsCodec.encode(next)
            if (prefs[KEY_SELECTED] == null || current.isEmpty()) prefs[KEY_SELECTED] = target
            used = target
        }
        return used
    }

    override suspend fun remove(id: Int) {
        dataStore.edit { prefs ->
            val current = current(prefs)
            val next = current.filterNot { it.id == id }
            // Written even when empty, so an emptied list is not read as "never written" and migrated back.
            prefs[KEY_SLOTS] = RadioSlotsCodec.encode(next)
            if (prefs[KEY_SELECTED] == id || next.none { it.id == prefs[KEY_SELECTED] }) {
                next.firstOrNull()?.let { prefs[KEY_SELECTED] = it.id } ?: prefs.remove(KEY_SELECTED)
            }
        }
    }

    override suspend fun select(id: Int) {
        dataStore.edit { prefs ->
            if (current(prefs).any { it.id == id }) prefs[KEY_SELECTED] = id
        }
    }

    private fun current(prefs: Preferences): List<RadioSlot> {
        val stored = prefs[KEY_SLOTS]
        return if (stored != null) {
            RadioSlotsCodec.decode(stored)
        } else {
            RadioSlotsCodec.effective(
                stored = emptyList(),
                legacyAddress = prefs[RadioPrefsImpl.KEY_DEV_ADDR_PREF],
                legacyName = prefs[RadioPrefsImpl.KEY_DEV_NAME_PREF],
            )
        }
    }

    private fun freeId(slots: List<RadioSlot>): Int? =
        (1..RadioSlotsPrefs.MAX_SLOTS).firstOrNull { id -> slots.none { it.id == id } }

    companion object {
        val KEY_SLOTS = stringPreferencesKey("radio-slots")
        val KEY_SELECTED = intPreferencesKey("radio-slot-selected")
    }
}
