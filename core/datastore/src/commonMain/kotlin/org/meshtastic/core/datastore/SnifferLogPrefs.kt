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
import androidx.datastore.preferences.core.longPreferencesKey
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
 * What happens once a sniffer's live packet list reaches [SnifferLogPrefs.MAX_BUFFERED_PACKETS]: [STOP] freezes on
 * whatever was captured first (matches "off" semantics -- nothing further is buffered until the trash icon clears the
 * panel), [OVERWRITE] keeps collecting and drops the oldest entries first, like a ring buffer.
 */
enum class SnifferBufferOverflowPolicy {
    STOP,
    OVERWRITE,
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
        val LAST_REAL_SOURCE = stringPreferencesKey("sniffer-last-real-source")
        val CLEARED_AT_MILLIS = longPreferencesKey("sniffer-cleared-at-millis")
        val GROUP_BY_GATEWAY = booleanPreferencesKey("sniffer-group-by-gateway")
        val AUTO_SCROLL = booleanPreferencesKey("sniffer-auto-scroll")
        val DECRYPT_PAYLOADS = booleanPreferencesKey("sniffer-decrypt-payloads")
        val EXPORT_FORMAT = stringPreferencesKey("sniffer-export-format")
        val BUFFER_OVERFLOW_POLICY = stringPreferencesKey("sniffer-buffer-overflow-policy")
        val HIDE_ONDEMAND_CHANNEL0 = booleanPreferencesKey("sniffer-hide-ondemand-channel0")
    }

    companion object {
        /** Shared cap for both live sniffer packet lists (Radio and MQTT) -- matches the HA panel's own buffer cap. */
        const val MAX_BUFFERED_PACKETS = 5000
    }

    open val activeSource: Flow<SnifferSource> =
        dataStore.data.map { prefs ->
            prefs[PreferencesKeys.ACTIVE_SOURCE]?.let { raw -> runCatching { SnifferSource.valueOf(raw) }.getOrNull() }
                ?: SnifferSource.OFF
        }

    open suspend fun setActiveSource(source: SnifferSource) {
        dataStore.edit { it[PreferencesKeys.ACTIVE_SOURCE] = source.name }
    }

    /**
     * The last RADIO/MQTT source a real selection was made for -- never Off. Lets a freshly created
     * [org.meshtastic.feature.settings.sniffer.SnifferPanelViewModel] (the panel is recreated every time the Sniffer
     * screen is reopened) keep showing that source's history while [activeSource] is currently Off, instead of the log
     * appearing to vanish just from navigating away and back. Null only when no source has ever been selected.
     */
    open val lastRealSource: Flow<SnifferSource?> =
        dataStore.data.map { prefs ->
            prefs[PreferencesKeys.LAST_REAL_SOURCE]?.let { raw ->
                runCatching { SnifferSource.valueOf(raw) }.getOrNull()
            }
        }

    open suspend fun setLastRealSource(source: SnifferSource) {
        dataStore.edit { it[PreferencesKeys.LAST_REAL_SOURCE] = source.name }
    }

    /**
     * Watermark set by the trash icon
     * ([org.meshtastic.feature.settings.sniffer.SnifferPanelViewModel.clearDisplayedLogs]): packets timestamped at or
     * before this are hidden from the panel. Persisted for the same reason as [lastRealSource] -- a cleared log must
     * stay cleared across a revisit, not reappear because the panel's ViewModel was recreated.
     *
     * Also set the first time a real source is ever selected while still at the 0L default (see
     * [org.meshtastic.feature.settings.sniffer.SnifferPanelViewModel]'s init) -- so a sniffing run only ever shows
     * packets it actually captured, never the shared MeshLog table's full unrelated history from before that run
     * started. [org.meshtastic.feature.settings.sniffer.SnifferLogViewModel.sniffedPackets] queries from this watermark
     * directly, treating 0L (nothing captured yet) as "show nothing" rather than "show everything".
     */
    open val clearedAtMillis: Flow<Long> = dataStore.data.map { it[PreferencesKeys.CLEARED_AT_MILLIS] ?: 0L }

    open suspend fun setClearedAtMillis(millis: Long) {
        dataStore.edit { it[PreferencesKeys.CLEARED_AT_MILLIS] = millis }
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

    /**
     * What to do once a live sniffer packet list reaches [MAX_BUFFERED_PACKETS]. Defaults to
     * [SnifferBufferOverflowPolicy.STOP].
     */
    open val bufferOverflowPolicy: Flow<SnifferBufferOverflowPolicy> =
        dataStore.data.map { prefs ->
            prefs[PreferencesKeys.BUFFER_OVERFLOW_POLICY]?.let { raw ->
                runCatching { SnifferBufferOverflowPolicy.valueOf(raw) }.getOrNull()
            } ?: SnifferBufferOverflowPolicy.STOP
        }

    open suspend fun setBufferOverflowPolicy(policy: SnifferBufferOverflowPolicy) {
        dataStore.edit { it[PreferencesKeys.BUFFER_OVERFLOW_POLICY] = policy.name }
    }

    /**
     * Hide OnDemand (port 354, see `ON_DEMAND_PORT_NUM`) packets sent on the primary channel (index 0) from the Radio
     * sniffer -- that traffic is the phone app's own OnDemand/diagnostic chatter with the connected node, not mesh
     * traffic, and on a busy channel 0 it can crowd out everything else in the log. Defaults to hidden; the toggle
     * exists for anyone who specifically wants to watch that traffic.
     */
    open val hideOnDemandChannel0: Flow<Boolean> =
        dataStore.data.map { it[PreferencesKeys.HIDE_ONDEMAND_CHANNEL0] ?: true }

    open suspend fun setHideOnDemandChannel0(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.HIDE_ONDEMAND_CHANNEL0] = enabled }
    }
}
