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
package org.meshtastic.core.prefs.archive

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
import org.meshtastic.core.repository.MessageArchiveConfig
import org.meshtastic.core.repository.MessageArchivePrefs

@Single
class MessageArchivePrefsImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) :
    MessageArchivePrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val config: StateFlow<MessageArchiveConfig> =
        dataStore.data
            .map { MessageArchiveConfig.decode(it[KEY_ARCHIVE]) }
            .stateIn(scope, SharingStarted.Eagerly, MessageArchiveConfig())

    override fun setConfig(config: MessageArchiveConfig) {
        scope.launch { dataStore.edit { it[KEY_ARCHIVE] = config.encode() } }
    }

    private companion object {
        val KEY_ARCHIVE = stringPreferencesKey("message_archive_config")
    }
}
