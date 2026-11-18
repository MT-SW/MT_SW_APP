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
package org.meshtastic.core.prefs.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okio.BufferedSink
import okio.BufferedSource
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.safeCatching
import org.meshtastic.core.datastore.di.CorePreferencesDataStore
import org.meshtastic.core.prefs.di.AppDataStore
import org.meshtastic.core.prefs.di.CustomEmojiDataStore
import org.meshtastic.core.prefs.di.FilterDataStore
import org.meshtastic.core.prefs.di.HomoglyphEncodingDataStore
import org.meshtastic.core.prefs.di.MapDataStore
import org.meshtastic.core.prefs.di.MapTileProviderDataStore
import org.meshtastic.core.prefs.di.MeshLogDataStore
import org.meshtastic.core.prefs.di.UiDataStore
import org.meshtastic.core.repository.AppSettingsBackupOperations
import org.meshtastic.core.repository.SettingsBackupScope

/**
 * Reads every preference store the user's settings live in and writes it as JSON (store → key → typed value); the
 * reverse restores it. The radio, mesh and analytics stores (the chosen device, this installation's id) are never
 * included, and a few one-time marks and the AI model choices are skipped, because they only make sense on the
 * installation that wrote them.
 */
@Suppress("LongParameterList")
@Single
class AppSettingsBackupImpl(
    ui: UiDataStore,
    app: AppDataStore,
    map: MapDataStore,
    mapTiles: MapTileProviderDataStore,
    filter: FilterDataStore,
    homoglyph: HomoglyphEncodingDataStore,
    emoji: CustomEmojiDataStore,
    meshLog: MeshLogDataStore,
    core: CorePreferencesDataStore,
) : AppSettingsBackupOperations {

    private val stores: Map<String, DataStore<Preferences>> =
        linkedMapOf(
            STORE_UI to ui,
            STORE_APP to app,
            STORE_MAP to map,
            STORE_MAP_TILES to mapTiles,
            STORE_FILTER to filter,
            STORE_HOMOGLYPH to homoglyph,
            STORE_EMOJI to emoji,
            STORE_MESH_LOG to meshLog,
            STORE_CORE to core,
        )

    override suspend fun export(scope: SettingsBackupScope, sink: BufferedSink) {
        val root = buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            put("scope", scope.name.lowercase())
            putJsonObject("stores") {
                for ((name, store) in stores) {
                    val entries =
                        store.data
                            .first()
                            .asMap()
                            .entries
                            .filter { included(scope, name, it.key.name) }
                            .sortedBy { it.key.name }
                    if (entries.isEmpty()) continue
                    putJsonObject(name) { entries.forEach { (key, value) -> encode(value)?.let { put(key.name, it) } } }
                }
            }
        }
        sink.writeUtf8(PRETTY.encodeToString(JsonObject.serializer(), root))
    }

    override suspend fun import(scope: SettingsBackupScope, source: BufferedSource): Int? {
        val fileStores = readStores(source) ?: return null
        // Decode first, so a damaged file changes nothing.
        val decoded = decodeStores(scope, fileStores)
        for ((name, values) in decoded) {
            stores.getValue(name).edit { prefs ->
                // A look restore brings back the whole look: values the file does not have return to the default.
                if (scope == SettingsBackupScope.Look) {
                    prefs.asMap().keys.filter { isLookKey(it.name) }.forEach { prefs.remove(it) }
                }
                values.forEach { (key, value) -> prefs.putAny(key, value) }
            }
        }
        return decoded.values.sumOf { it.size }
    }

    /** The "stores" part of a settings backup, or null when [source] is not one. */
    private fun readStores(source: BufferedSource): JsonObject? = safeCatching {
        val root = Json.parseToJsonElement(source.readUtf8()).jsonObject
        if (root["format"]?.jsonPrimitive?.contentOrNull == FORMAT) root["stores"]?.jsonObject else null
    }
        .getOrNull()

    private fun decodeStores(scope: SettingsBackupScope, fileStores: JsonObject): Map<String, List<Pair<String, Any>>> {
        val decoded = linkedMapOf<String, List<Pair<String, Any>>>()
        for (name in stores.keys) {
            val entries = safeCatching { fileStores[name]?.jsonObject }.getOrNull() ?: continue
            val values =
                entries.entries.mapNotNull { (key, element) ->
                    if (included(scope, name, key)) decode(element)?.let { key to it } else null
                }
            if (values.isNotEmpty()) decoded[name] = values
        }
        return decoded
    }

    private fun included(scope: SettingsBackupScope, store: String, key: String): Boolean = when (scope) {
        SettingsBackupScope.Look -> store == STORE_UI && isLookKey(key)
        SettingsBackupScope.All -> key !in EXCLUDED_KEYS && EXCLUDED_PREFIXES.none { key.startsWith(it) }
    }

    private fun isLookKey(key: String): Boolean = key in LOOK_KEYS || LOOK_PREFIXES.any { key.startsWith(it) }

    private fun encode(value: Any): JsonObject? {
        val (type, json) =
            when (value) {
                is Boolean -> TYPE_BOOLEAN to JsonPrimitive(value)

                is Int -> TYPE_INT to JsonPrimitive(value)

                is Long -> TYPE_LONG to JsonPrimitive(value)

                is Float -> TYPE_FLOAT to JsonPrimitive(value)

                is Double -> TYPE_DOUBLE to JsonPrimitive(value)

                is String -> TYPE_STRING to JsonPrimitive(value)

                is Set<*> ->
                    TYPE_STRING_SET to JsonArray(value.filterIsInstance<String>().sorted().map { JsonPrimitive(it) })

                else -> return null
            }
        return buildJsonObject {
            put("t", type)
            put("v", json)
        }
    }

    private fun decode(element: JsonElement): Any? = safeCatching {
        val entry = element.jsonObject
        val value = entry["v"] ?: return@safeCatching null
        when (entry["t"]?.jsonPrimitive?.contentOrNull) {
            TYPE_BOOLEAN -> value.jsonPrimitive.booleanOrNull
            TYPE_INT -> value.jsonPrimitive.intOrNull
            TYPE_LONG -> value.jsonPrimitive.longOrNull
            TYPE_FLOAT -> value.jsonPrimitive.floatOrNull
            TYPE_DOUBLE -> value.jsonPrimitive.doubleOrNull
            TYPE_STRING -> value.jsonPrimitive.contentOrNull
            TYPE_STRING_SET -> value.jsonArray.map { it.jsonPrimitive.content }.toSet()
            else -> null
        }
    }
        .getOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun MutablePreferences.putAny(name: String, value: Any) {
        when (value) {
            is Boolean -> this[booleanPreferencesKey(name)] = value
            is Int -> this[intPreferencesKey(name)] = value
            is Long -> this[longPreferencesKey(name)] = value
            is Float -> this[floatPreferencesKey(name)] = value
            is Double -> this[doublePreferencesKey(name)] = value
            is String -> this[stringPreferencesKey(name)] = value
            is Set<*> -> this[stringSetPreferencesKey(name)] = value as Set<String>
        }
    }

    private companion object {
        const val FORMAT = "mt-sw-settings"
        const val VERSION = 1

        const val STORE_UI = "ui"
        const val STORE_APP = "app"
        const val STORE_MAP = "map"
        const val STORE_MAP_TILES = "mapTiles"
        const val STORE_FILTER = "filter"
        const val STORE_HOMOGLYPH = "homoglyph"
        const val STORE_EMOJI = "emoji"
        const val STORE_MESH_LOG = "meshLog"
        const val STORE_CORE = "core"

        const val TYPE_BOOLEAN = "b"
        const val TYPE_INT = "i"
        const val TYPE_LONG = "l"
        const val TYPE_FLOAT = "f"
        const val TYPE_DOUBLE = "d"
        const val TYPE_STRING = "s"
        const val TYPE_STRING_SET = "ss"

        val PRETTY = Json { prettyPrint = true }

        /** The settings that make up the look: "ui-option-" ones, node list layout, theme, fonts, icon. */
        val LOOK_KEYS =
            setOf(
                "theme",
                "theme_default_applied",
                "ui-scale",
                "font-family-name",
                "font-weight-shift",
                "font-size-percent",
                "app-icon-style",
                "floating-nav-bar",
                "import-menu-in-top-bar",
                "show-full-message-timestamps",
            )
        val LOOK_PREFIXES = listOf("ui-option-", "node-layout-")

        /** Snapshots of the connected radio and pending recoveries: they belong to the radio, not to the settings. */
        val EXCLUDED_PREFIXES =
            listOf("local-stats-extended-", "pending-firmware-", "last-ble-node-recovery", "dismissed-bootloader")

        /** One-time marks, warning timers, the chosen connection and AI model choices stay with their installation. */
        val EXCLUDED_KEYS =
            setOf(
                "whats_new_seen_build",
                "whats_new_seen_stable",
                "whats_new_full_pending",
                "data_notice_accepted_at",
                "data_notice_version",
                "app_intro_completed",
                "has_shown_not_paired_warning",
                "selected-connection-transport",
                "firmware-update-notification-keys",
                "app-update-last-tag",
                "app-update-last-millis",
                "last-narrow-band-warning-shown",
                "next-narrow-band-warning",
                "next-region-warning",
                "last-region-warning-shown",
                "was-inside-swietokrzyskie-region",
                "radio-setup-offered-nodes",
                "auto-clean-nodes-last-run",
                "translator-model-id",
                "assistant-model-id",
                "dictation-model-id",
            )
    }
}
