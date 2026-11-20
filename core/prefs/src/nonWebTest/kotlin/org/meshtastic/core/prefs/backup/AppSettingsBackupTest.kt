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
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.FileSystem
import okio.Path
import org.meshtastic.core.datastore.di.asCorePreferencesDataStore
import org.meshtastic.core.prefs.di.asAppDataStore
import org.meshtastic.core.prefs.di.asCustomEmojiDataStore
import org.meshtastic.core.prefs.di.asFilterDataStore
import org.meshtastic.core.prefs.di.asHomoglyphEncodingDataStore
import org.meshtastic.core.prefs.di.asMapDataStore
import org.meshtastic.core.prefs.di.asMapTileProviderDataStore
import org.meshtastic.core.prefs.di.asMeshLogDataStore
import org.meshtastic.core.prefs.di.asUiDataStore
import org.meshtastic.core.prefs.store.asPrefsStore
import org.meshtastic.core.repository.AppDataBundle
import org.meshtastic.core.repository.SettingsBackupScope
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

/** The settings file has to be the same on every platform, so it is checked here, in common code. */
class AppSettingsBackupTest {
    private lateinit var tmpDir: Path
    private lateinit var scope: TestScope

    @BeforeTest
    fun setup() {
        scope = TestScope(UnconfinedTestDispatcher())
        tmpDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "settingsBackupTest-${Uuid.random()}"
        FileSystem.SYSTEM.createDirectories(tmpDir)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        FileSystem.SYSTEM.deleteRecursively(tmpDir)
    }

    private class Stores(
        val ui: DataStore<Preferences>,
        val map: DataStore<Preferences>,
        val backup: AppSettingsBackupImpl,
    )

    private fun stores(tag: String): Stores {
        fun store(name: String): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
            scope = scope,
            produceFile = { tmpDir / "$tag-$name.preferences_pb" },
        )
        val ui = store("ui")
        val map = store("map")
        return Stores(
            ui = ui,
            map = map,
            backup =
            AppSettingsBackupImpl(
                ui = ui.asPrefsStore().asUiDataStore(),
                app = store("app").asPrefsStore().asAppDataStore(),
                map = map.asPrefsStore().asMapDataStore(),
                mapTiles = store("tiles").asPrefsStore().asMapTileProviderDataStore(),
                filter = store("filter").asPrefsStore().asFilterDataStore(),
                homoglyph = store("homoglyph").asPrefsStore().asHomoglyphEncodingDataStore(),
                emoji = store("emoji").asPrefsStore().asCustomEmojiDataStore(),
                meshLog = store("meshlog").asPrefsStore().asMeshLogDataStore(),
                core = store("core").asCorePreferencesDataStore(),
            ),
        )
    }

    private suspend fun Stores.export(backupScope: SettingsBackupScope): ByteArray =
        Buffer().also { backup.export(backupScope, it) }.readByteArray()

    @Test
    fun `every kind of value survives a round trip`() = scope.runTest {
        val from = stores("from")
        from.ui.edit {
            it[booleanPreferencesKey("floating-nav-bar")] = true
            it[intPreferencesKey("ui-option-navbar.look")] = 3
            it[stringPreferencesKey("font-family-name")] = "Inter"
            it[longPreferencesKey("some-long")] = 1_234_567_890_123L
        }
        from.map.edit {
            it[doublePreferencesKey("camera_zoom")] = 12.5
            it[stringSetPreferencesKey("map_excluded_roles")] = setOf("a", "b")
        }

        val to = stores("to")
        val restored = to.backup.import(SettingsBackupScope.All, Buffer().write(from.export(SettingsBackupScope.All)))

        assertEquals(6, restored)
        val ui = to.ui.data.first()
        assertEquals(true, ui[booleanPreferencesKey("floating-nav-bar")])
        assertEquals(3, ui[intPreferencesKey("ui-option-navbar.look")])
        assertEquals("Inter", ui[stringPreferencesKey("font-family-name")])
        assertEquals(1_234_567_890_123L, ui[longPreferencesKey("some-long")])
        val map = to.map.data.first()
        assertEquals(12.5, map[doublePreferencesKey("camera_zoom")])
        assertEquals(setOf("a", "b"), map[stringSetPreferencesKey("map_excluded_roles")])
    }

    @Test
    fun `settings that belong to one installation are not saved`() = scope.runTest {
        val from = stores("from")
        from.ui.edit {
            it[booleanPreferencesKey("app_intro_completed")] = true
            it[stringPreferencesKey("assistant-model-id")] = "model"
            it[intPreferencesKey("ui-scale")] = 5
        }

        val json = from.export(SettingsBackupScope.All).decodeToString()

        assertFalse("app_intro_completed" in json)
        assertFalse("assistant-model-id" in json)
        assertEquals(true, "ui-scale" in json)
    }

    @Test
    fun `restoring the look touches only the look and clears what the file does not have`() = scope.runTest {
        val from = stores("from")
        from.ui.edit {
            it[intPreferencesKey("ui-option-cards.look")] = 2
            it[booleanPreferencesKey("show-quick-chat")] = true
        }
        val file = from.export(SettingsBackupScope.Look)

        val to = stores("to")
        to.ui.edit {
            it[intPreferencesKey("ui-option-navbar.look")] = 1
            it[booleanPreferencesKey("show-quick-chat")] = false
        }
        val restored = to.backup.import(SettingsBackupScope.Look, Buffer().write(file))

        assertEquals(1, restored)
        val ui = to.ui.data.first()
        assertEquals(2, ui[intPreferencesKey("ui-option-cards.look")])
        assertNull(ui[intPreferencesKey("ui-option-navbar.look")])
        assertEquals(false, ui[booleanPreferencesKey("show-quick-chat")])
    }

    @Test
    fun `a file that is not a settings backup changes nothing`() = scope.runTest {
        val to = stores("to")
        to.ui.edit { it[intPreferencesKey("ui-scale")] = 4 }

        assertNull(to.backup.import(SettingsBackupScope.All, Buffer().writeUtf8("not json")))
        assertNull(to.backup.import(SettingsBackupScope.All, Buffer().writeUtf8("""{"format":"other"}""")))

        assertEquals(4, to.ui.data.first()[intPreferencesKey("ui-scale")])
    }

    @Test
    fun `the data bundle keeps the settings and leaves the database behind them`() {
        val database = byteArrayOf(83, 81, 76, 105, 116, 101)
        val settings = """{"format":"mt-sw-settings"}""".encodeToByteArray()
        val file = Buffer()
        AppDataBundle.writeHeader(file, settings)
        file.write(database)

        assertContentEquals(settings, AppDataBundle.readHeader(file))
        assertContentEquals(database, file.readByteArray())
    }

    @Test
    fun `a plain database file without a header is left untouched`() {
        val file = Buffer().write("SQLite format 3".encodeToByteArray())

        assertNull(AppDataBundle.readHeader(file))
        assertNotNull(file.size.takeIf { it == 15L })
    }
}
