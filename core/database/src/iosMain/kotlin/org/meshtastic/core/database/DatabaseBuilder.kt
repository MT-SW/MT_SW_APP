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
package org.meshtastic.core.database

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.meshtastic.core.database.MeshtasticDatabase.Companion.configureCommon
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Returns a [RoomDatabase.Builder] configured for iOS with the given [dbName]. */
@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(dbName: String): RoomDatabase.Builder<MeshtasticDatabase> {
    val dbFilePath = documentDirectory() + "/$dbName.db"
    ensureWriteAheadLogging(dbFilePath)
    return Room.databaseBuilder<MeshtasticDatabase>(
        name = dbFilePath,
        factory = { MeshtasticDatabaseConstructor.initialize() },
    )
        .configureCommon()
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .setDriver(BusyTimeoutSQLiteDriver(BundledSQLiteDriver()))
}

/**
 * Puts an existing database file in write-ahead-log mode before Room opens it. On the iPhone every write took over a
 * second because the file was in the older rollback-journal mode, where each commit waits for the disk several times;
 * the faster `synchronous = NORMAL` setting of [BusyTimeoutSQLiteDriver] only applies to a file already in WAL mode,
 * and the mode stays in the file once set. Nothing else has the file open at this point, so the change cannot clash
 * with another connection. A failure is only logged: Room then opens the file as it did before.
 */
private fun ensureWriteAheadLogging(path: String) {
    if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return
    runCatching {
        val connection = BundledSQLiteDriver().open(path)
        try {
            connection.execSQL("PRAGMA busy_timeout = ${BusyTimeoutSQLiteDriver.DEFAULT_BUSY_TIMEOUT_MS}")
            val mode = connection.prepare("PRAGMA journal_mode = WAL").use { if (it.step()) it.getText(0) else "" }
            Logger.i { "Database $path: journal mode is $mode" }
        } finally {
            connection.close()
        }
    }
        .onFailure { Logger.w(it) { "Database $path: could not switch to WAL mode" } }
}

/** Returns a [RoomDatabase.Builder] configured for an in-memory iOS database. */
actual fun getInMemoryDatabaseBuilder(): RoomDatabase.Builder<MeshtasticDatabase> =
    Room.inMemoryDatabaseBuilder<MeshtasticDatabase>(factory = { MeshtasticDatabaseConstructor.initialize() })
        .configureCommon()
        .setDriver(BusyTimeoutSQLiteDriver(BundledSQLiteDriver()))

/** Returns the iOS directory where database files are stored. */
actual fun getDatabaseDirectory(): Path = documentDirectory().toPath()

/** Returns the iOS path of the main database file for [dbName]. */
actual fun getDatabaseFile(dbName: String): Path = (documentDirectory() + "/$dbName.db").toPath()

/** Deletes the database and its Room-associated files on iOS. */
@OptIn(ExperimentalForeignApi::class)
actual fun deleteDatabase(dbName: String) {
    val dir = documentDirectory()
    NSFileManager.defaultManager.removeItemAtPath(dir + "/$dbName.db", null)
    NSFileManager.defaultManager.removeItemAtPath(dir + "/$dbName.db-wal", null)
    NSFileManager.defaultManager.removeItemAtPath(dir + "/$dbName.db-shm", null)
}

/** Returns the system FileSystem for iOS. */
actual fun getFileSystem(): FileSystem = FileSystem.SYSTEM

/** Creates an iOS DataStore for database preferences. */
@OptIn(ExperimentalForeignApi::class)
actual fun createDatabaseDataStore(name: String): DataStore<Preferences> {
    val dir = documentDirectory() + "/datastore"
    NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
    return PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() }),
        produceFile = { (dir + "/$name.preferences_pb").toPath() },
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(documentDirectory?.path)
}

/** The folder of the app's data on iOS (the counterpart of `desktopDataDir()`). */
fun iosDataDir(): String = documentDirectory()
