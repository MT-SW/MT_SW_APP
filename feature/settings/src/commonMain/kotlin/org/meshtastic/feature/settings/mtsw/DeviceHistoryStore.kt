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
@file:Suppress("TooGenericExceptionCaught")

package org.meshtastic.feature.settings.mtsw

import co.touchlab.kermit.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.Path
import org.koin.core.annotation.Single
import org.meshtastic.core.repository.DeviceSnapshot
import org.meshtastic.feature.map.layers.mapLayerFileSystem
import org.meshtastic.feature.map.layers.mapLayersDirectory

/** One remembered reading of a device. */
@Serializable
data class DeviceHistoryEntry(
    val timeMillis: Long,
    val batteryLevel: Int? = null,
    val channelUtilization: Float? = null,
    val airUtilTx: Float? = null,
    val nodeCount: Int = 0,
    val onlineCount: Int = 0,
)

/**
 * MT_SW: the history of the device comparison. Each reading is kept (at most one every [MIN_GAP_MS] and [MAX_ENTRIES]
 * in all) in a small file for each device, on this device only.
 */
@Single
class DeviceHistoryStore {
    private val fs = mapLayerFileSystem()
    private val root: Path = (mapLayersDirectory().parent ?: mapLayersDirectory()) / "device_history"
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(DeviceHistoryEntry.serializer())

    private fun file(address: String): Path =
        root / (address.filter { it.isLetterOrDigit() }.ifEmpty { "device" } + ".json")

    fun history(address: String): List<DeviceHistoryEntry> =
        try {
            val f = file(address)
            if (fs.exists(f)) json.decodeFromString(serializer, fs.read(f) { readUtf8() }) else emptyList()
        } catch (e: Exception) {
            Logger.w { "Historia urządzeń: odczyt nie udał się: ${e.message}" }
            emptyList()
        }

    /** Adds the reading unless the last one is too recent; returns the history. */
    fun record(address: String, snapshot: DeviceSnapshot, nowMillis: Long): List<DeviceHistoryEntry> {
        val old = history(address)
        if (old.isNotEmpty() && nowMillis - old.last().timeMillis < MIN_GAP_MS) return old
        val entry =
            DeviceHistoryEntry(
                nowMillis,
                snapshot.batteryLevel,
                snapshot.channelUtilization,
                snapshot.airUtilTx,
                snapshot.nodeCount,
                snapshot.onlineCount,
            )
        val all = (old + entry).takeLast(MAX_ENTRIES)
        try {
            fs.createDirectories(root)
            fs.write(file(address)) { writeUtf8(json.encodeToString(serializer, all)) }
            Logger.i { "Historia urządzeń: zapisano odczyt, razem ${all.size}" }
        } catch (e: Exception) {
            Logger.w { "Historia urządzeń: zapis nie udał się: ${e.message}" }
        }
        return all
    }

    private companion object {
        const val MIN_GAP_MS = 10 * 60_000L
        const val MAX_ENTRIES = 600
    }
}
