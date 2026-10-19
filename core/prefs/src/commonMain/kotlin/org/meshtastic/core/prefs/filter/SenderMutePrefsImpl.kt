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
package org.meshtastic.core.prefs.filter

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.prefs.di.FilterDataStore
import org.meshtastic.core.repository.SenderMutePrefs

/** Stores sender mutes as "key=until" strings next to the other message-filter settings. */
@Single
class SenderMutePrefsImpl(private val dataStore: FilterDataStore, dispatchers: CoroutineDispatchers) :
    SenderMutePrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val mutes: StateFlow<Map<String, Long>> =
        dataStore.data
            .map { decode(it[KEY_SENDER_MUTES] ?: emptySet()) }
            .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    override fun setMuteUntil(contactKey: String, senderNum: Int, untilMillis: Long) {
        val key = SenderMutePrefs.muteKey(contactKey, senderNum)
        scope.launch {
            dataStore.edit { prefs ->
                val now = nowMillis
                // Expired entries are dropped on every write so the set cannot grow without bound.
                val current = decode(prefs[KEY_SENDER_MUTES] ?: emptySet()).filterValues { it > now }.toMutableMap()
                if (untilMillis > now) current[key] = untilMillis else current.remove(key)
                prefs[KEY_SENDER_MUTES] = current.map { (k, v) -> "$k=$v" }.toSet()
            }
        }
    }

    private fun decode(raw: Set<String>): Map<String, Long> = raw.mapNotNull { entry ->
        val split = entry.lastIndexOf('=')
        val until = if (split > 0) entry.substring(split + 1).toLongOrNull() else null
        until?.let { entry.substring(0, split) to it }
    }.toMap()

    companion object {
        val KEY_SENDER_MUTES = stringSetPreferencesKey("sender_mutes")
    }
}
