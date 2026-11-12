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
package org.meshtastic.core.prefs.notification

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.prefs.di.UiDataStore
import org.meshtastic.core.repository.NotificationPrefs
import org.meshtastic.core.repository.QuietHours

@Single
@Suppress("TooManyFunctions")
class NotificationPrefsImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) : NotificationPrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val messagesEnabled: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_MESSAGES_ENABLED] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setMessagesEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_MESSAGES_ENABLED] = enabled } }
    }

    override val nodeEventsEnabled: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_NODE_EVENTS_ENABLED] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setNodeEventsEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_NODE_EVENTS_ENABLED] = enabled } }
    }

    override val nodeEventsAutoDisabledForEvent: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_NODE_EVENTS_AUTO_DISABLED] ?: false }.stateIn(scope, SharingStarted.Eagerly, false)

    override fun setNodeEventsAutoDisabledForEvent(disabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_NODE_EVENTS_AUTO_DISABLED] = disabled } }
    }

    override fun applyEventFirmwareNodeEventDefault(isEventFirmware: Boolean) {
        // One edit block: DataStore serializes edits, so the values read here are the same snapshot written below.
        // Reading the StateFlows and calling the setters instead would race — they lag these writes.
        scope.launch {
            dataStore.edit { prefs ->
                val enabled = prefs[KEY_NODE_EVENTS_ENABLED] ?: true
                val autoDisabled = prefs[KEY_NODE_EVENTS_AUTO_DISABLED] ?: false
                when {
                    // Only claim the restore if node events were actually on — otherwise the vanilla branch would
                    // later enable a preference the user turned off themselves.
                    isEventFirmware && !autoDisabled && enabled -> {
                        prefs[KEY_NODE_EVENTS_ENABLED] = false
                        prefs[KEY_NODE_EVENTS_AUTO_DISABLED] = true
                    }

                    !isEventFirmware && autoDisabled -> {
                        prefs[KEY_NODE_EVENTS_ENABLED] = true
                        prefs[KEY_NODE_EVENTS_AUTO_DISABLED] = false
                    }
                }
            }
        }
    }

    override val lowBatteryEnabled: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_LOW_BATTERY_ENABLED] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setLowBatteryEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_LOW_BATTERY_ENABLED] = enabled } }
    }

    override val soundWhileOpen: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_SOUND_WHILE_OPEN] ?: false }.stateIn(scope, SharingStarted.Eagerly, false)

    override fun setSoundWhileOpen(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_SOUND_WHILE_OPEN] = enabled } }
    }

    override val reactionsEnabled: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_REACTIONS_ENABLED] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setReactionsEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_REACTIONS_ENABLED] = enabled } }
    }

    override suspend fun loadReactionsEnabled(): Boolean = dataStore.data.first()[KEY_REACTIONS_ENABLED] ?: true

    override val newWaypointsEnabled: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_NEW_WAYPOINTS_ENABLED] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setNewWaypointsEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_NEW_WAYPOINTS_ENABLED] = enabled } }
    }

    override suspend fun loadNewWaypointsEnabled(): Boolean = dataStore.data.first()[KEY_NEW_WAYPOINTS_ENABLED] ?: true

    override val alertsEnabled: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_ALERTS_ENABLED] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setAlertsEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_ALERTS_ENABLED] = enabled } }
    }

    override val radioNoticesEnabled: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_RADIO_NOTICES_ENABLED] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setRadioNoticesEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_RADIO_NOTICES_ENABLED] = enabled } }
    }

    override val updatesEnabled: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_UPDATES_ENABLED] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setUpdatesEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_UPDATES_ENABLED] = enabled } }
    }

    override val nodeSilenceEnabled: StateFlow<Boolean> =
        dataStore.data.map { it[KEY_NODE_SILENCE_ENABLED] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    override fun setNodeSilenceEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_NODE_SILENCE_ENABLED] = enabled } }
    }

    // Stored as an insertion-ordered CSV of ints (DataStore has no int-set key, and a plain set can't express age).
    // Ordering lets us evict the oldest opt-in once capped; non-numeric leftovers are ignored on read.
    override val geofenceAlertOptIns: StateFlow<Set<Int>> =
        dataStore.data
            .map { prefs -> parseOptInIds(prefs[KEY_GEOFENCE_ALERT_OPT_INS]).toSet() }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override fun setGeofenceAlertOptIn(waypointId: Int, enabled: Boolean) {
        scope.launch {
            dataStore.edit { prefs ->
                val ids = parseOptInIds(prefs[KEY_GEOFENCE_ALERT_OPT_INS]).toMutableList()
                ids.remove(waypointId) // re-toggling moves it to the most-recent slot
                if (enabled) {
                    ids.add(waypointId)
                    // Bound growth: opt-ins accumulate as waypoints churn, so keep only the most-recent MAX.
                    while (ids.size > MAX_GEOFENCE_OPT_INS) ids.removeAt(0)
                }
                prefs[KEY_GEOFENCE_ALERT_OPT_INS] = ids.joinToString(",")
            }
        }
    }

    // Stored as "node:minutes" pairs separated by commas.
    override val nodeSilenceAlerts: StateFlow<Map<Int, Int>> =
        dataStore.data
            .map { prefs -> parseSilenceAlerts(prefs[KEY_NODE_SILENCE_ALERTS]) }
            .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    override fun setNodeSilenceAlert(nodeNum: Int, minutes: Int?) {
        scope.launch {
            dataStore.edit { prefs ->
                val alerts = parseSilenceAlerts(prefs[KEY_NODE_SILENCE_ALERTS]).toMutableMap()
                if (minutes == null || minutes <= 0) alerts.remove(nodeNum) else alerts[nodeNum] = minutes
                prefs[KEY_NODE_SILENCE_ALERTS] = alerts.entries.joinToString(",") { "${it.key}:${it.value}" }
            }
        }
    }

    // Stored as node numbers separated by commas.
    override val nodeSilenceCountRelayed: StateFlow<Set<Int>> =
        dataStore.data
            .map { prefs -> parseOptInIds(prefs[KEY_NODE_SILENCE_RELAYED]).toSet() }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override fun setNodeSilenceCountRelayed(nodeNum: Int, enabled: Boolean) {
        scope.launch {
            dataStore.edit { prefs ->
                val nodes = parseOptInIds(prefs[KEY_NODE_SILENCE_RELAYED]).toMutableSet()
                if (enabled) nodes.add(nodeNum) else nodes.remove(nodeNum)
                prefs[KEY_NODE_SILENCE_RELAYED] = nodes.joinToString(",")
            }
        }
    }

    // Stored as "start-end" hours, or empty for none.
    override val nodeSilenceQuietHours: StateFlow<QuietHours?> =
        dataStore.data
            .map { prefs -> parseQuietHours(prefs[KEY_NODE_SILENCE_QUIET]) }
            .stateIn(scope, SharingStarted.Eagerly, null)

    override fun setNodeSilenceQuietHours(hours: QuietHours?) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_NODE_SILENCE_QUIET] = hours?.let { "${it.startHour}-${it.endHour}" }.orEmpty()
            }
        }
    }

    companion object {
        /** Cap on remembered foreign-geofence opt-ins; far above realistic manual use, oldest evicted past it. */
        const val MAX_GEOFENCE_OPT_INS = 100

        private fun parseOptInIds(csv: String?): List<Int> =
            csv?.split(',')?.mapNotNull { it.toIntOrNull() }?.distinct() ?: emptyList()

        private fun parseSilenceAlerts(csv: String?): Map<Int, Int> = csv?.split(',')
            ?.mapNotNull { pair ->
                val (node, minutes) = pair.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                val nodeNum = node.toIntOrNull() ?: return@mapNotNull null
                val value = minutes.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                nodeNum to value
            }
            ?.toMap() ?: emptyMap()

        @Suppress("ReturnCount")
        private fun parseQuietHours(text: String?): QuietHours? {
            val (start, end) = text?.split('-')?.takeIf { it.size == 2 } ?: return null
            @Suppress("MagicNumber")
            val startHour = start.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
            @Suppress("MagicNumber")
            val endHour = end.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
            return QuietHours(startHour, endHour)
        }

        private val KEY_NODE_SILENCE_QUIET = stringPreferencesKey("notif_node_silence_quiet")
        private val KEY_NODE_SILENCE_ALERTS = stringPreferencesKey("notif_node_silence_alerts")
        private val KEY_NODE_SILENCE_RELAYED = stringPreferencesKey("notif_node_silence_relayed")
        private val KEY_MESSAGES_ENABLED = booleanPreferencesKey("notif_messages_enabled")
        private val KEY_NODE_EVENTS_ENABLED = booleanPreferencesKey("notif_node_events_enabled")
        private val KEY_NODE_EVENTS_AUTO_DISABLED = booleanPreferencesKey("notif_node_events_auto_disabled_event")
        private val KEY_LOW_BATTERY_ENABLED = booleanPreferencesKey("notif_low_battery_enabled")
        private val KEY_REACTIONS_ENABLED = booleanPreferencesKey("notif_reactions_enabled")
        private val KEY_NEW_WAYPOINTS_ENABLED = booleanPreferencesKey("notif_new_waypoints_enabled")
        private val KEY_ALERTS_ENABLED = booleanPreferencesKey("notif_alerts_enabled")
        private val KEY_RADIO_NOTICES_ENABLED = booleanPreferencesKey("notif_radio_notices_enabled")
        private val KEY_UPDATES_ENABLED = booleanPreferencesKey("notif_updates_enabled")
        private val KEY_NODE_SILENCE_ENABLED = booleanPreferencesKey("notif_node_silence_enabled")
        private val KEY_SOUND_WHILE_OPEN = booleanPreferencesKey("notif_sound_while_open")
        private val KEY_GEOFENCE_ALERT_OPT_INS = stringPreferencesKey("notif_geofence_alert_opt_ins")
    }
}
