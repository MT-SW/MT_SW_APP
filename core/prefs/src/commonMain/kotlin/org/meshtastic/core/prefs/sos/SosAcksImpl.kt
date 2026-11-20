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
import org.meshtastic.core.repository.SosAckCodec
import org.meshtastic.core.repository.SosAckEntry
import org.meshtastic.core.repository.SosAcks
import org.meshtastic.core.prefs.store.stringPrefsKey

/** The answers of recipients to the SOS we sent last, kept in the same store as the other SOS settings. */
@Single
class SosAcksImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) : SosAcks {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val entries: StateFlow<List<SosAckEntry>> =
        dataStore.data.map { SosAckCodec.decode(it[KEY_ACKS]) }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun add(entry: SosAckEntry) {
        scope.launch {
            dataStore.edit {
                val all = listOf(entry) + SosAckCodec.decode(it[KEY_ACKS])
                it[KEY_ACKS] = SosAckCodec.encode(all.take(SosAckCodec.MAX_ENTRIES))
            }
        }
    }

    override fun clear() {
        scope.launch { dataStore.edit { it[KEY_ACKS] = "" } }
    }

    private companion object {
        val KEY_ACKS = stringPrefsKey("sos_acks")
    }
}
