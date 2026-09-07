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
package org.meshtastic.core.model.util

import org.meshtastic.proto.Telemetry

/**
 * Local device mesh statistics extension (heap, CPU, flash, PSRAM) beyond the standard [org.meshtastic.proto.LocalStats].
 *
 * Mirrors the `LocalStatsExtended` message (`local_stats_extended`, tag 20 in `Telemetry.variant`) sent by the fw+
 * firmware fork. This field is now formally declared in the shared MT_SW_PROTOBUFS schema, so Wire decodes it
 * natively into [org.meshtastic.proto.Telemetry.local_stats_extended] instead of leaving it in `unknownFields`.
 * [decodeLocalStatsExtended] just maps that generated type to this app's own snake_case-free shape, used throughout
 * the existing UI/history code.
 */
data class LocalStatsExtended(
    val memoryFreeCheap: Int = 0,
    val memoryTotal: Int = 0,
    val cpuUsagePercent: Int = 0,
    val flashUsedBytes: Int = 0,
    val flashTotalBytes: Int = 0,
    val memoryPsramFree: Int = 0,
    val memoryPsramTotal: Int = 0,
)

/**
 * Maps this [Telemetry] packet's natively-decoded `local_stats_extended` oneof variant (tag 20) to our own
 * [LocalStatsExtended] shape. Returns null when this packet's `variant` oneof is something else (device_metrics,
 * local_stats, host_metrics, etc.) — fw+ sends `local_stats` (heap) and `local_stats_extended` (CPU/flash/PSRAM) as
 * separate packets, not combined in one.
 */
fun Telemetry.decodeLocalStatsExtended(): LocalStatsExtended? = local_stats_extended?.let { ext ->
    LocalStatsExtended(
        memoryFreeCheap = ext.memory_free_cheap,
        memoryTotal = ext.memory_total,
        cpuUsagePercent = ext.cpu_usage_percent,
        flashUsedBytes = ext.flash_used_bytes,
        flashTotalBytes = ext.flash_total_bytes,
        memoryPsramFree = ext.memory_psram_free,
        memoryPsramTotal = ext.memory_psram_total,
    )
}