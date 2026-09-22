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
package org.meshtastic.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.meshtastic.core.datastore.di.CorePreferencesDataStore

/**
 * Which sniffer (if any) is currently feeding the combined Sniffer Log panel (`feature/settings/sniffer`). Only one can
 * be active at a time -- selecting one turns the other off, matching the single-broker-session/single-connected- device
 * constraints each sniffer already has on its own.
 */
enum class SnifferSource {
    OFF,
    RADIO,
    MQTT,
}

/** File format offered when saving -- or loading back -- a Sniffer Log export. */
enum class SnifferLogFormat {
    TXT,
    JSON,
    CSV,
}

/**
 * Persisted settings for the combined Sniffer Log panel. The Radio and MQTT sniffers were merged into a single screen
 * reached from one entry point; these preferences apply to whichever source [activeSource] currently selects, so
 * switching sources keeps the same grouping/auto-scroll/decrypt/export choices rather than resetting them.
 */
@Single
open class SnifferLogPrefs(private val dataStore: CorePreferencesDataStore) {
    private object PreferencesKeys {
        val ACTIVE_SOURCE = stringPreferencesKey("sniffer-active-source")
        val GROUP_BY_GATEWAY = booleanPreferencesKey("sniffer-group-by-gateway")
        val AUTO_SCROLL = booleanPreferencesKey("sniffer-auto-scroll")
        val DECRYPT_PAYLOADS = booleanPreferencesKey("sniffer-decrypt-payloads")
        val EXPORT_FORMAT = stringPreferencesKey("sniffer-export-format")
    }

    open val activeSource: Flow<SnifferSource> =
        dataStore.data.map { prefs ->
            prefs[PreferencesKeys.ACTIVE_SOURCE]?.let { raw -> runCatching { SnifferSource.valueOf(raw) }.getOrNull() }
                ?: SnifferSource.OFF
        }

    open suspend fun setActiveSource(source: SnifferSource) {
        dataStore.edit { it[PreferencesKeys.ACTIVE_SOURCE] = source.name }
    }

    /** Merge packets seen from several gateways/nodes for the same over-the-air transmission into one entry. */
    open val groupByGateway: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.GROUP_BY_GATEWAY] ?: true }

    open suspend fun setGroupByGateway(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.GROUP_BY_GATEWAY] = enabled }
    }

    /** Keep the list scrolled to the newest packet as new ones arrive. */
    open val autoScroll: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.AUTO_SCROLL] ?: true }

    open suspend fun setAutoScroll(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_SCROLL] = enabled }
    }

    /** Attempt to decode/decrypt payloads with already-known channel keys. Off shows raw hex for every packet. */
    open val decryptPayloads: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DECRYPT_PAYLOADS] ?: true }

    open suspend fun setDecryptPayloads(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DECRYPT_PAYLOADS] = enabled }
    }

    open val exportFormat: Flow<SnifferLogFormat> =
        dataStore.data.map { prefs ->
            prefs[PreferencesKeys.EXPORT_FORMAT]?.let { raw ->
                runCatching { SnifferLogFormat.valueOf(raw) }.getOrNull()
            } ?: SnifferLogFormat.TXT
        }

    open suspend fun setExportFormat(format: SnifferLogFormat) {
        dataStore.edit { it[PreferencesKeys.EXPORT_FORMAT] = format.name }
    }
}
