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
package org.meshtastic.core.repository

import okio.BufferedSink
import okio.BufferedSource

/** What a settings backup covers: every app setting, or only the look of the app ("Wygląd"). */
enum class SettingsBackupScope {
    All,
    Look,
}

/**
 * MT_SW: saves the app settings to a small JSON file and reads them back. The same file format is used on Android,
 * computer and iPhone/iPad. Settings tied to one radio or one installation (the chosen device, the one-time notices,
 * the downloaded AI models) are left out, so a restore on another phone does not break anything.
 */
interface AppSettingsBackupOperations {
    /** Writes the settings of [scope] to [sink]. */
    suspend fun export(scope: SettingsBackupScope, sink: BufferedSink)

    /**
     * Applies the settings of [scope] found in [source]. Returns how many values were restored, or null when the file
     * is not a settings backup. A "look" restore first puts the look back to the defaults, then applies the file.
     */
    suspend fun import(scope: SettingsBackupScope, source: BufferedSource): Int?

    companion object {
        const val MIME_TYPE: String = "application/json"
    }
}
