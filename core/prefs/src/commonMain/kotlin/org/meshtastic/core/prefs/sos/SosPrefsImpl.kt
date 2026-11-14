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

import androidx.datastore.preferences.core.booleanPreferencesKey
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
import org.meshtastic.core.repository.SosAlarmStyle
import org.meshtastic.core.repository.SosBarScreens
import org.meshtastic.core.repository.SosConfig
import org.meshtastic.core.repository.SosPrefs
import org.meshtastic.core.repository.SosQuietHours

@Single
class SosPrefsImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) : SosPrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val config: StateFlow<SosConfig> =
        dataStore.data.map { SosConfig.decode(it[KEY_SOS]) }.stateIn(scope, SharingStarted.Eagerly, SosConfig())

    override fun setConfig(config: SosConfig) {
        scope.launch { dataStore.edit { it[KEY_SOS] = config.encode() } }
    }

    // Written as the keys joined by commas; an empty text means none (the button is off), no value means all.
    override val barScreens: StateFlow<Set<String>> =
        dataStore.data
            .map { prefs ->
                prefs[KEY_BAR]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
                    ?: SosBarScreens.ALL.toSet()
            }
            .stateIn(scope, SharingStarted.Eagerly, SosBarScreens.ALL.toSet())

    override fun setBarScreens(screens: Set<String>) {
        scope.launch { dataStore.edit { it[KEY_BAR] = screens.joinToString(",") } }
    }

    override val receiveAlarm: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_RECEIVE] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setReceiveAlarm(on: Boolean) {
        scope.launch { dataStore.edit { it[KEY_RECEIVE] = on } }
    }

    override val alarmFlash: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_FLASH] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setAlarmFlash(on: Boolean) {
        scope.launch { dataStore.edit { it[KEY_FLASH] = on } }
    }

    override val confirmToSender: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_CONFIRM] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setConfirmToSender(on: Boolean) {
        scope.launch { dataStore.edit { it[KEY_CONFIRM] = on } }
    }

    // Written as the node numbers joined by commas; no value or an empty text means none.
    override val alarmNodes: StateFlow<Set<Int>> =
        dataStore.data
            .map { prefs ->
                prefs[KEY_ALARM_NODES]?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.toSet().orEmpty()
            }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override fun setAlarmNodes(nodes: Set<Int>) {
        scope.launch { dataStore.edit { it[KEY_ALARM_NODES] = nodes.joinToString(",") } }
    }

    override val alarmOnlyChosen: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_ONLY_CHOSEN] ?: false }.stateIn(scope, SharingStarted.Eagerly, false)

    override fun setAlarmOnlyChosen(on: Boolean) {
        scope.launch { dataStore.edit { it[KEY_ONLY_CHOSEN] = on } }
    }

    override val quietHours: StateFlow<SosQuietHours> =
        dataStore.data
            .map { SosQuietHours.decode(it[KEY_QUIET]) }
            .stateIn(scope, SharingStarted.Eagerly, SosQuietHours())

    override fun setQuietHours(hours: SosQuietHours) {
        scope.launch { dataStore.edit { it[KEY_QUIET] = hours.encode() } }
    }

    override val quietExceptions: StateFlow<Set<Int>> =
        dataStore.data
            .map { prefs ->
                prefs[KEY_QUIET_NODES]?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.toSet().orEmpty()
            }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override fun setQuietExceptions(nodes: Set<Int>) {
        scope.launch { dataStore.edit { it[KEY_QUIET_NODES] = nodes.joinToString(",") } }
    }

    override val alarmStyles: StateFlow<Map<Int, SosAlarmStyle>> =
        dataStore.data
            .map { SosAlarmStyle.decodeMap(it[KEY_STYLES]) }
            .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    override fun setAlarmStyles(styles: Map<Int, SosAlarmStyle>) {
        scope.launch { dataStore.edit { it[KEY_STYLES] = SosAlarmStyle.encodeMap(styles) } }
    }

    private companion object {
        val KEY_QUIET_NODES = stringPreferencesKey("sos_quiet_exceptions")
        val KEY_STYLES = stringPreferencesKey("sos_alarm_styles")
        val KEY_ALARM_NODES = stringPreferencesKey("sos_alarm_nodes")
        val KEY_ONLY_CHOSEN = booleanPreferencesKey("sos_alarm_only_chosen")
        val KEY_QUIET = stringPreferencesKey("sos_quiet_hours")
        val KEY_FLASH = booleanPreferencesKey("sos_alarm_flash")
        val KEY_CONFIRM = booleanPreferencesKey("sos_confirm_to_sender")
        val KEY_RECEIVE = booleanPreferencesKey("sos_receive_alarm")
        val KEY_SOS = stringPreferencesKey("sos_config")
        val KEY_BAR = stringPreferencesKey("sos_bar_screens")
    }
}
