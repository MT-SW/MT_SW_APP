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
package org.meshtastic.core.prefs.remote

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ApplicationCoroutineScope
import org.meshtastic.core.prefs.di.MeshDataStore
import org.meshtastic.core.prefs.store.stringSetPrefsKey
import org.meshtastic.core.repository.RemoteAdminKnownPrefs

@Single
class RemoteAdminKnownPrefsImpl(private val dataStore: MeshDataStore, private val scope: ApplicationCoroutineScope) :
    RemoteAdminKnownPrefs {

    override val known: StateFlow<Set<Int>> =
        dataStore.data
            .map { prefs -> (prefs[KEY_KNOWN] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet() }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override fun markKnown(nodeNum: Int) {
        if (nodeNum in known.value) return
        scope.launch {
            dataStore.edit { prefs -> prefs[KEY_KNOWN] = (prefs[KEY_KNOWN] ?: emptySet()) + nodeNum.toString() }
            Logger.i { "Zdalne zarządzanie: urządzenie ${nodeNum.toUInt()} potwierdzone jako własne" }
        }
    }

    companion object {
        val KEY_KNOWN = stringSetPrefsKey("remote_admin_known_nodes")
    }
}
