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

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.meshtastic.core.datastore.di.CorePreferencesDataStore

/**
 * Plain holder for the fw+ `local_stats_extended` fields. Kept local to `core:datastore` (instead of reusing
 * `core.model.util.LocalStatsExtended`) to avoid adding a new inter-module dependency for 7 ints — the mapping to the
 * domain type happens in `core:data` where both modules are already available.
 */
data class LocalStatsExtendedPrefs(
    val memoryFreeCheap: Int = 0,
    val memoryTotal: Int = 0,
    val cpuUsagePercent: Int = 0,
    val flashUsedBytes: Int = 0,
    val flashTotalBytes: Int = 0,
    val memoryPsramFree: Int = 0,
    val memoryPsramTotal: Int = 0,
)

/**
 * Persists the latest fw+ `local_stats_extended` telemetry snapshot in the shared [CorePreferencesDataStore] — the
 * same store already used by [BootloaderWarningDataSource] and others — rather than a new dedicated DataStore file.
 */
@Single
open class LocalStatsExtendedDataSource(private val dataStore: CorePreferencesDataStore) {

    private object PreferencesKeys {
        val MEMORY_FREE_CHEAP = intPreferencesKey("local-stats-extended-memory-free-cheap")
        val MEMORY_TOTAL = intPreferencesKey("local-stats-extended-memory-total")
        val CPU_USAGE_PERCENT = intPreferencesKey("local-stats-extended-cpu-usage-percent")
        val FLASH_USED_BYTES = intPreferencesKey("local-stats-extended-flash-used-bytes")
        val FLASH_TOTAL_BYTES = intPreferencesKey("local-stats-extended-flash-total-bytes")
        val MEMORY_PSRAM_FREE = intPreferencesKey("local-stats-extended-memory-psram-free")
        val MEMORY_PSRAM_TOTAL = intPreferencesKey("local-stats-extended-memory-psram-total")
    }

    open val localStatsExtendedFlow: Flow<LocalStatsExtendedPrefs> =
        dataStore.data.map { prefs ->
            LocalStatsExtendedPrefs(
                memoryFreeCheap = prefs[PreferencesKeys.MEMORY_FREE_CHEAP] ?: 0,
                memoryTotal = prefs[PreferencesKeys.MEMORY_TOTAL] ?: 0,
                cpuUsagePercent = prefs[PreferencesKeys.CPU_USAGE_PERCENT] ?: 0,
                flashUsedBytes = prefs[PreferencesKeys.FLASH_USED_BYTES] ?: 0,
                flashTotalBytes = prefs[PreferencesKeys.FLASH_TOTAL_BYTES] ?: 0,
                memoryPsramFree = prefs[PreferencesKeys.MEMORY_PSRAM_FREE] ?: 0,
                memoryPsramTotal = prefs[PreferencesKeys.MEMORY_PSRAM_TOTAL] ?: 0,
            )
        }

    open suspend fun setLocalStatsExtended(stats: LocalStatsExtendedPrefs) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.MEMORY_FREE_CHEAP] = stats.memoryFreeCheap
            prefs[PreferencesKeys.MEMORY_TOTAL] = stats.memoryTotal
            prefs[PreferencesKeys.CPU_USAGE_PERCENT] = stats.cpuUsagePercent
            prefs[PreferencesKeys.FLASH_USED_BYTES] = stats.flashUsedBytes
            prefs[PreferencesKeys.FLASH_TOTAL_BYTES] = stats.flashTotalBytes
            prefs[PreferencesKeys.MEMORY_PSRAM_FREE] = stats.memoryPsramFree
            prefs[PreferencesKeys.MEMORY_PSRAM_TOTAL] = stats.memoryPsramTotal
        }
    }
}