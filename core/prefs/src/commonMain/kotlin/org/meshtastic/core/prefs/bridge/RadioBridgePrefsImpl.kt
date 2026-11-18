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
package org.meshtastic.core.prefs.bridge

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.prefs.di.UiDataStore
import org.meshtastic.core.repository.RadioBridgeConfig
import org.meshtastic.core.repository.RadioBridgePrefs
import org.meshtastic.core.repository.RadioSlotsPrefs

/**
 * The bridge settings, one entry per saved radio, in the settings store of the app (so they are part of the settings
 * backup and work the same on every platform).
 */
@Single
class RadioBridgePrefsImpl(private val dataStore: UiDataStore, dispatchers: CoroutineDispatchers) : RadioBridgePrefs {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val configs: StateFlow<Map<Int, RadioBridgeConfig>> =
        dataStore.data.map { decode(it[KEY_BRIDGES]) }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    override fun setConfig(slotId: Int, config: RadioBridgeConfig) {
        scope.launch {
            dataStore.edit { prefs ->
                val all = decode(prefs[KEY_BRIDGES]) + (slotId to config)
                prefs[KEY_BRIDGES] = encode(all)
            }
        }
    }

    companion object {
        val KEY_BRIDGES = stringPreferencesKey("radio-bridge-configs")

        internal fun encode(all: Map<Int, RadioBridgeConfig>): String = JsonObject(
            all.mapKeys { it.key.toString() }
                .mapValues { (_, c) ->
                    JsonObject(
                        mapOf(
                            "enabled" to JsonPrimitive(c.enabled),
                            "port" to JsonPrimitive(c.port),
                            "lan" to JsonPrimitive(c.lanAccess),
                            "readOnly" to JsonPrimitive(c.readOnly),
                            "blockAdmin" to JsonPrimitive(c.blockAdmin),
                            "blockMessages" to JsonPrimitive(c.blockMessages),
                            "maxClients" to JsonPrimitive(c.maxClients),
                        ),
                    )
                },
        )
            .toString()

        /** A damaged text reads as no settings (the bridge stays off). */
        internal fun decode(text: String?): Map<Int, RadioBridgeConfig> {
            if (text.isNullOrBlank()) return emptyMap()
            return runCatching {
                Json.parseToJsonElement(text)
                    .jsonObject
                    .mapNotNull { (key, value) ->
                        val id =
                            key.toIntOrNull()?.takeIf { it in 1..RadioSlotsPrefs.MAX_SLOTS }
                                ?: return@mapNotNull null
                        val o = value.jsonObject
                        val defaults = RadioBridgeConfig()
                        id to
                            RadioBridgeConfig(
                                enabled = o["enabled"]?.jsonPrimitive?.boolean ?: defaults.enabled,
                                port = o["port"]?.jsonPrimitive?.int ?: defaults.port,
                                lanAccess = o["lan"]?.jsonPrimitive?.boolean ?: defaults.lanAccess,
                                readOnly = o["readOnly"]?.jsonPrimitive?.boolean ?: defaults.readOnly,
                                blockAdmin = o["blockAdmin"]?.jsonPrimitive?.boolean ?: defaults.blockAdmin,
                                blockMessages =
                                o["blockMessages"]?.jsonPrimitive?.boolean ?: defaults.blockMessages,
                                maxClients = o["maxClients"]?.jsonPrimitive?.int ?: defaults.maxClients,
                            )
                    }
                    .toMap()
            }
                .getOrDefault(emptyMap())
        }
    }
}
