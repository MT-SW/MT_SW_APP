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
import org.meshtastic.core.repository.RemoteLastPicks
import org.meshtastic.core.repository.RemotePickKind

@Single
class RemoteLastPicksImpl(private val dataStore: MeshDataStore, private val scope: ApplicationCoroutineScope) :
    RemoteLastPicks {

    private fun key(kind: RemotePickKind) = when (kind) {
        RemotePickKind.FAVORITE -> KEY_FAVORITE
        RemotePickKind.IGNORE -> KEY_IGNORE
        RemotePickKind.CONTACT -> KEY_CONTACT
    }

    override val picks: StateFlow<Map<RemotePickKind, Set<Int>>> =
        dataStore.data
            .map { prefs ->
                RemotePickKind.entries.associateWith { kind ->
                    (prefs[key(kind)] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, RemotePickKind.entries.associateWith { emptySet() })

    override fun remember(kind: RemotePickKind, nums: Collection<Int>) {
        if (nums.isEmpty()) return
        scope.launch {
            dataStore.edit { prefs -> prefs[key(kind)] = nums.map { it.toString() }.toSet() }
            Logger.i { "Zdalne listy: zapamiętano ${nums.size} wybranych węzłów (${kind.name})" }
        }
    }

    override fun clear(kind: RemotePickKind) {
        scope.launch {
            dataStore.edit { prefs -> prefs.remove(key(kind)) }
            Logger.i { "Zdalne listy: wyczyszczono zapamiętane węzły (${kind.name})" }
        }
    }

    companion object {
        val KEY_FAVORITE = stringSetPrefsKey("remote_last_picks_favorite")
        val KEY_IGNORE = stringSetPrefsKey("remote_last_picks_ignore")
        val KEY_CONTACT = stringSetPrefsKey("remote_last_picks_contact")
    }
}
