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
package org.meshtastic.core.prefs.sos

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.prefs.di.UiDataStore
import org.meshtastic.core.repository.SosHistory
import org.meshtastic.core.repository.SosHistoryCodec
import org.meshtastic.core.repository.SosHistoryEntry

/** The history of received SOS, kept in the same store as the other SOS settings. */
@Single
class SosHistoryImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) : SosHistory {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val entries: StateFlow<List<SosHistoryEntry>> =
        dataStore.data
            .map { SosHistoryCodec.decode(it[KEY_HISTORY]) }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun add(entry: SosHistoryEntry) {
        scope.launch {
            dataStore.edit {
                val all = listOf(entry) + SosHistoryCodec.decode(it[KEY_HISTORY])
                it[KEY_HISTORY] = SosHistoryCodec.encode(all.take(SosHistoryCodec.MAX_ENTRIES))
            }
        }
    }

    override fun clear() {
        scope.launch { dataStore.edit { it[KEY_HISTORY] = "" } }
    }

    private companion object {
        val KEY_HISTORY = stringPreferencesKey("sos_history")
    }
}
